/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
package org.glassfish.jersey.jdk.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class ProxyFilter extends Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> {

    private final ConnectorConfiguration.ProxyConfiguration proxyConfiguration;
    private final ProxyDigestAuthenticator proxyDigestAuthenticator = new ProxyDigestAuthenticator();
    private volatile State state = State.CONNECTING;
    private volatile InetSocketAddress originalDestinationAddress;

    /**
     * Constructor.
     *
     * @param downstreamFilter downstream filter. Accessible directly as {@link #downstreamFilter} protected field.
     */
    ProxyFilter(final Filter<HttpRequest, HttpResponse, ?, ?> downstreamFilter,
                ConnectorConfiguration.ProxyConfiguration proxyConfiguration) {
        super(downstreamFilter);
        this.proxyConfiguration = proxyConfiguration;
    }

    @Override
    void connect(final SocketAddress address, final Filter<?, ?, HttpRequest, HttpResponse> upstreamFilter) {
        this.upstreamFilter = upstreamFilter;
        this.originalDestinationAddress = (InetSocketAddress) address;
        downstreamFilter.connect(new InetSocketAddress(proxyConfiguration.getHost(), proxyConfiguration.getPort()), this);
    }

    @Override
    void onConnect() {
        HttpRequest connect = createConnectRequest();
        downstreamFilter.write(connect, new CompletionHandler<HttpRequest>() {
            @Override
            public void failed(final Throwable throwable) {
                upstreamFilter.processError(throwable);
            }
        });
    }

    @Override
    boolean processRead(HttpResponse httpResponse) {
        if (state == State.CONNECTED) {
            // if we have stop the connection phase, just pass through
            return true;
        }

        switch (httpResponse.getStatusCode()) {

            case 200: {
                state = State.CONNECTED;
                upstreamFilter.onConnect();
                break;
            }

            case 407: {
                if (state == State.AUTHENTICATED) {
                    upstreamFilter.onError(new ProxyAuthenticationException(LocalizationMessages.PROXY_407_TWICE()));
                    return false;
                }

                try {
                    state = State.AUTHENTICATED;
                    HttpRequest authenticatingRequest = createAuthenticatingRequest(httpResponse);
                    downstreamFilter.write(authenticatingRequest, new CompletionHandler<HttpRequest>() {
                        @Override
                        public void failed(final Throwable throwable) {
                            upstreamFilter.processError(throwable);
                        }
                    });
                } catch (ProxyAuthenticationException e) {
                    handleError(e);
                    return false;
                }

                break;
            }

            default: {
                handleError(new IOException(LocalizationMessages.PROXY_CONNECT_FAIL(httpResponse.getStatusCode())));
            }
        }

        return false;
    }

    @Override
    void write(final HttpRequest data, final CompletionHandler<HttpRequest> completionHandler) {
        downstreamFilter.write(data, completionHandler);
    }

    private void handleError(Throwable t) {
        upstreamFilter.onError(t);
    }

    private HttpRequest createAuthenticatingRequest(HttpResponse httpResponse) throws ProxyAuthenticationException {
        String authenticateHeader = null;
        final List<String> authHeader = httpResponse.getHeader(Constants.PROXY_AUTHENTICATE);
        if (authHeader != null && !authHeader.isEmpty()) {
            authenticateHeader = authHeader.get(0);
        }

        if (authenticateHeader == null || authenticateHeader.equals("")) {
            throw new ProxyAuthenticationException(LocalizationMessages.PROXY_MISSING_AUTH_HEADER());
        }

        final String[] tokens = authenticateHeader.trim().split("\\s+", 2);
        final String scheme = tokens[0];

        String authorizationHeader;
        if (Constants.BASIC.equals(scheme)) {
            authorizationHeader = ProxyBasicAuthenticator
                    .generateAuthorizationHeader(proxyConfiguration.getUserName(), proxyConfiguration.getPassword());
        } else if (Constants.DIGEST.equals(scheme)) {
            String originalDestinationUri = getOriginalDestinationUri();
            URI uri = URI.create(originalDestinationUri);
            authorizationHeader = proxyDigestAuthenticator
                    .generateAuthorizationHeader(uri, Constants.CONNECT, authenticateHeader,
                            proxyConfiguration.getUserName(), proxyConfiguration.getPassword());
        } else {
            throw new ProxyAuthenticationException(LocalizationMessages.PROXY_UNSUPPORTED_SCHEME(scheme));
        }

        HttpRequest connectRequest = createConnectRequest();
        connectRequest.addHeaderIfNotPresent(Constants.PROXY_AUTHORIZATION, authorizationHeader);
        return connectRequest;
    }

    private HttpRequest createConnectRequest() {
        String originalDestinationUri = getOriginalDestinationUri();
        URI uri = URI.create(originalDestinationUri);
        HttpRequest connect = HttpRequest.createBodyless(Constants.CONNECT, uri);
        connect.addHeaderIfNotPresent(Constants.HOST, originalDestinationUri);
        connect.addHeaderIfNotPresent(Constants.PROXY_CONNECTION, Constants.KEEP_ALIVE);
        return connect;
    }

    private String getOriginalDestinationUri() {
        return String.format("%s:%d", originalDestinationAddress.getHostString(), originalDestinationAddress.getPort());
    }

    enum State {
        CONNECTING,
        AUTHENTICATED,
        CONNECTED
    }
}
