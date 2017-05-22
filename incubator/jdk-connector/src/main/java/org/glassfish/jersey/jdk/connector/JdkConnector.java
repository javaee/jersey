/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2016 Oracle and/or its affiliates. All rights reserved.
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
import java.net.CookieManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class JdkConnector implements Connector {

    private final HttpConnectionPool httpConnectionPool;
    private final ConnectorConfiguration connectorConfiguration;

    JdkConnector(Client client, Configuration config) {
        connectorConfiguration = new ConnectorConfiguration(client, config);
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(connectorConfiguration.getCookiePolicy());
        httpConnectionPool = new HttpConnectionPool(connectorConfiguration, cookieManager);
    }

    @Override
    public ClientResponse apply(ClientRequest request) {

        // TODO lovely; Can't we do better?
        Future<?> future = apply(request, new AsyncConnectorCallback() {
            @Override
            public void response(ClientResponse response) {

            }

            @Override
            public void failure(Throwable failure) {

            }
        });

        try {
            return (ClientResponse) future.get();
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        final CompletableFuture<ClientResponse> responseFuture = new CompletableFuture<>();
        // just so we don't have to drag around both the future and callback
        final AsyncConnectorCallback internalCallback = new AsyncConnectorCallback() {
            @Override
            public void response(ClientResponse response) {
                callback.response(response);
                responseFuture.complete(response);
            }

            @Override
            public void failure(Throwable failure) {
                callback.failure(failure);
                responseFuture.completeExceptionally(failure);
            }
        };

        final HttpRequest httpRequest = createHttpRequest(request);

        if (httpRequest.getBodyMode() == HttpRequest.BodyMode.BUFFERED) {
            writeBufferedEntity(request, httpRequest, internalCallback);
        }

        if (httpRequest.getBodyMode() == HttpRequest.BodyMode.BUFFERED
                || httpRequest.getBodyMode() == HttpRequest.BodyMode.NONE) {
            send(request, httpRequest, internalCallback);
        }

        if (httpRequest.getBodyMode() == HttpRequest.BodyMode.CHUNKED) {

            /* We wait with sending the request header until the body stream has been touched.
             This is because of javax.ws.rs.ext.MessageBodyWriter, which says:

             "The message header map is mutable but any changes must be made before writing to the output stream since
              the headers will be flushed prior to writing the message body"

              This means that the headers can change until body output stream is used.
              */
            final InterceptingOutputStream bodyStream = new InterceptingOutputStream(httpRequest.getBodyStream(),
                    // send the prepared request when the stream is touched for the first time
                    () -> send(request, httpRequest, internalCallback));

            request.setStreamProvider(contentLength -> bodyStream);
            try {
                request.writeEntity();
            } catch (IOException e) {
                internalCallback.failure(e);
            }
        }

        return responseFuture;
    }

    private void writeBufferedEntity(ClientRequest request, final HttpRequest httpRequest, AsyncConnectorCallback callback) {
        request.setStreamProvider(contentLength -> httpRequest.getBodyStream());
        try {
            request.writeEntity();
        } catch (IOException e) {
            callback.failure(e);
        }
    }

    private void send(final ClientRequest request, final HttpRequest httpRequest, final AsyncConnectorCallback callback) {
        translateHeaders(request, httpRequest);
        final RedirectHandler redirectHandler = new RedirectHandler(httpConnectionPool, httpRequest, connectorConfiguration);
        httpConnectionPool.send(httpRequest, new CompletionHandler<HttpResponse>() {

            @Override
            public void failed(Throwable throwable) {
                callback.failure(throwable);
            }

            @Override
            public void completed(HttpResponse result) {
                redirectHandler.handleRedirects(result, new CompletionHandler<HttpResponse>() {
                    @Override
                    public void failed(Throwable throwable) {
                        callback.failure(throwable);
                    }

                    @Override
                    public void completed(HttpResponse result) {
                        ClientResponse response = translateResponse(request, result, redirectHandler.getLastRequestUri());
                        callback.response(response);
                    }
                });
            }
        });
    }

    private HttpRequest createHttpRequest(ClientRequest request) {
        Object entity = request.getEntity();

        if (entity == null) {
            return HttpRequest.createBodyless(request.getMethod(), request.getUri());
        }

        RequestEntityProcessing entityProcessing = request.resolveProperty(
                ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.class);

        HttpRequest httpRequest;
        if (entityProcessing != null && entityProcessing == RequestEntityProcessing.CHUNKED) {
            httpRequest = HttpRequest.createChunked(request.getMethod(), request.getUri(), connectorConfiguration.getChunkSize());
        } else {
            httpRequest = HttpRequest.createBuffered(request.getMethod(), request.getUri());
        }

        return httpRequest;
    }

    private Map<String, List<String>> translateHeaders(ClientRequest clientRequest, HttpRequest httpRequest) {
        Map<String, List<String>> headers = httpRequest.getHeaders();
        for (Map.Entry<String, List<String>> header : clientRequest.getStringHeaders().entrySet()) {
            List<String> values = new ArrayList<>(header.getValue());
            headers.put(header.getKey(), values);
        }

        return headers;
    }

    private ClientResponse translateResponse(final ClientRequest requestContext,
                                             final HttpResponse httpResponse,
                                             URI requestUri) {

        Response.StatusType statusType = new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return httpResponse.getStatusCode();
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.familyOf(httpResponse.getStatusCode());
            }

            @Override
            public String getReasonPhrase() {
                return httpResponse.getReasonPhrase();
            }
        };

        ClientResponse responseContext = new ClientResponse(statusType, requestContext, requestUri);

        Map<String, List<String>> headers = httpResponse.getHeaders();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                responseContext.getHeaders().add(entry.getKey(), value);
            }
        }

        responseContext.setEntityStream(httpResponse.getBodyStream());
        return responseContext;
    }

    @Override
    public String getName() {
        return "JDK connector";
    }

    @Override
    public void close() {
        httpConnectionPool.close();
    }
}
