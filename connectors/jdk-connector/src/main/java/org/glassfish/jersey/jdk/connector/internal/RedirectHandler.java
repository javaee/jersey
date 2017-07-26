/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.jdk.connector.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
class RedirectHandler {

    private static final Set<Integer> REDIRECT_STATUS_CODES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(301, 302, 303, 307, 308)));

    private final int maxRedirects;
    private final boolean followRedirects;
    private final Set<URI> redirectUriHistory;
    private final HttpConnectionPool httpConnectionPool;
    private final HttpRequest originalHttpRequest;

    private volatile URI lastRequestUri = null;

    RedirectHandler(HttpConnectionPool httpConnectionPool, HttpRequest originalHttpRequest,
                    ConnectorConfiguration connectorConfiguration) {
        this.followRedirects = connectorConfiguration.getFollowRedirects();
        this.maxRedirects = connectorConfiguration.getMaxRedirects();
        this.httpConnectionPool = httpConnectionPool;
        this.originalHttpRequest = originalHttpRequest;
        this.redirectUriHistory = new HashSet<>(maxRedirects);
        this.lastRequestUri = originalHttpRequest.getUri();
    }

    void handleRedirects(final HttpResponse httpResponse, final CompletionHandler<HttpResponse> completionHandler) {
        if (!followRedirects) {
            completionHandler.completed(httpResponse);
            return;
        }

        if (!REDIRECT_STATUS_CODES.contains(httpResponse.getStatusCode())) {
            completionHandler.completed(httpResponse);
            return;
        }

        if (httpResponse.getStatusCode() != 303) {
            // we support other methods than GET and HEAD only with 303
            if (!Constants.HEAD.equals(originalHttpRequest.getMethod()) && !Constants.GET
                    .equals(originalHttpRequest.getMethod())) {
                completionHandler.completed(httpResponse);
                return;
            }
        }

        // reading the body is not necessary, but if we wait until the entire body has arrived, we can reuse the same connection
        consumeBodyIfPresent(httpResponse, new CompletionHandler<Void>() {
            @Override
            public void failed(Throwable throwable) {
                completionHandler.failed(throwable);
            }

            @Override
            public void completed(Void r) {
                doRedirect(httpResponse, new CompletionHandler<HttpResponse>() {
                    @Override
                    public void failed(Throwable throwable) {
                        completionHandler.failed(throwable);
                    }

                    @Override
                    public void completed(HttpResponse result) {
                        handleRedirects(result, completionHandler);
                    }
                });
            }
        });
    }

    private void doRedirect(final HttpResponse httpResponse, final CompletionHandler<HttpResponse> completionHandler) {

        // get location header
        String locationString = null;
        final List<String> locationHeader = httpResponse.getHeader("Location");
        if (locationHeader != null && !locationHeader.isEmpty()) {
            locationString = locationHeader.get(0);
        }

        if (locationString == null || locationString.isEmpty()) {
            completionHandler.failed(new RedirectException(LocalizationMessages.REDIRECT_NO_LOCATION()));
            return;
        }

        URI location;
        try {
            location = new URI(locationString);

            if (!location.isAbsolute()) {
                // location is not absolute, we need to resolve it.
                URI baseUri = lastRequestUri;
                location = baseUri.resolve(location.normalize());
            }
        } catch (URISyntaxException e) {
            completionHandler.failed(new RedirectException(LocalizationMessages.REDIRECT_ERROR_DETERMINING_LOCATION(), e));
            return;
        }

        // infinite loop detection
        boolean alreadyRequested = !redirectUriHistory.add(location);
        if (alreadyRequested) {
            completionHandler.failed(new RedirectException(LocalizationMessages.REDIRECT_INFINITE_LOOP()));
            return;
        }

        // maximal number of redirection
        if (redirectUriHistory.size() > maxRedirects) {
            completionHandler.failed(new RedirectException(LocalizationMessages.REDIRECT_LIMIT_REACHED(maxRedirects)));
            return;
        }

        String method = originalHttpRequest.getMethod();
        Map<String, List<String>> headers = originalHttpRequest.getHeaders();
        if (httpResponse.getStatusCode() == 303 && !method.equals(Constants.HEAD)) {
            // in case of 303 we rewrite every method except HEAD to GET
            method = Constants.GET;
            // remove entity-transport headers if present
            headers.remove(Constants.CONTENT_LENGTH);
            headers.remove(Constants.TRANSFER_ENCODING_HEADER);
        }

        HttpRequest httpRequest = HttpRequest.createBodyless(method, location);
        httpRequest.getHeaders().putAll(headers);
        lastRequestUri = location;

        httpConnectionPool.send(httpRequest, completionHandler);
    }

    private void consumeBodyIfPresent(HttpResponse response, final CompletionHandler<Void> completionHandler) {
        final AsynchronousBodyInputStream bodyStream = response.getBodyStream();
        bodyStream.setReadListener(new ReadListener() {
            @Override
            public void onDataAvailable() {
                while (bodyStream.isReady()) {
                    try {
                        bodyStream.read();
                    } catch (IOException e) {
                        completionHandler.failed(e);
                    }
                }
            }

            @Override
            public void onAllDataRead() {
                completionHandler.completed(null);
            }

            @Override
            public void onError(Throwable t) {
                completionHandler.failed(t);
            }
        });
    }

    URI getLastRequestUri() {
        return lastRequestUri;
    }
}
