/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.inmemory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

/**
 * In-memory client connector.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class InMemoryConnector implements Connector {

    private static final Logger LOGGER = Logger.getLogger(InMemoryConnector.class.getName());

    private final URI baseUri;
    private final ApplicationHandler appHandler;

    /**
     * In-memory client connector provider.
     */
    static class Provider implements ConnectorProvider {

        private final URI baseUri;
        private final ApplicationHandler appHandler;

        /**
         * Create new in-memory connector provider.
         *
         * @param baseUri    application base URI.
         * @param appHandler RequestInvoker instance which represents application.
         */
        Provider(URI baseUri, ApplicationHandler appHandler) {
            this.baseUri = baseUri;
            this.appHandler = appHandler;
        }

        @Override
        public Connector getConnector(Client client, Configuration config) {
            return new InMemoryConnector(baseUri, appHandler);
        }
    }

    /**
     * Create new in-memory connector.
     *
     * @param baseUri    application base URI.
     * @param appHandler RequestInvoker instance which represents application.
     */
    private InMemoryConnector(final URI baseUri, final ApplicationHandler appHandler) {
        this.baseUri = baseUri;
        this.appHandler = appHandler;
    }

    /**
     * In memory container response writer.
     */
    public static class InMemoryResponseWriter implements ContainerResponseWriter {

        private MultivaluedMap<String, String> headers;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private boolean committed;
        private Response.StatusType statusInfo;

        @Override
        public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext) {
            List<Object> length = new ArrayList<>();
            length.add(String.valueOf(contentLength));

            responseContext.getHeaders().put(HttpHeaders.CONTENT_LENGTH, length);
            headers = responseContext.getStringHeaders();
            statusInfo = responseContext.getStatusInfo();
            return baos;
        }

        @Override
        public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
            LOGGER.warning("Asynchronous server side invocations are not supported by InMemoryContainer.");
            return false;
        }

        @Override
        public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) {
            throw new UnsupportedOperationException("Async server side invocations are not supported by InMemoryContainer.");
        }

        @Override
        public void commit() {
            committed = true;
        }

        @Override
        public void failure(Throwable error) {
            throw new ProcessingException("Server-side request processing failed with an error.", error);
        }

        @Override
        public boolean enableResponseBuffering() {
            return true;
        }

        /**
         * Get the written entity.
         *
         * @return Byte array which contains the entity written by the server.
         */
        public byte[] getEntity() {
            if (!committed) {
                throw new IllegalStateException("Response is not committed yet.");
            }
            return baos.toByteArray();
        }

        /**
         * Return response headers.
         *
         * @return headers.
         */
        public MultivaluedMap<String, String> getHeaders() {
            return headers;
        }

        /**
         * Returns response status info.
         *
         * @return status info.
         */
        public Response.StatusType getStatusInfo() {
            return statusInfo;
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Transforms client-side request to server-side and invokes it on provided application ({@link ApplicationHandler}
     * instance).
     *
     * @param clientRequest client side request to be invoked.
     */
    @Override
    public ClientResponse apply(final ClientRequest clientRequest) {
        PropertiesDelegate propertiesDelegate = new MapPropertiesDelegate();

        final ContainerRequest containerRequest = new ContainerRequest(baseUri,
                clientRequest.getUri(), clientRequest.getMethod(),
                null, propertiesDelegate);

        containerRequest.getHeaders().putAll(clientRequest.getStringHeaders());

        final ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
        if (clientRequest.getEntity() != null) {
            clientRequest.setStreamProvider(new OutboundMessageContext.StreamProvider() {
                @Override
                public OutputStream getOutputStream(int contentLength) throws IOException {
                    final MultivaluedMap<String, Object> clientHeaders = clientRequest.getHeaders();
                    if (contentLength != -1 && !clientHeaders.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                        containerRequest.getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
                    }
                    return clientOutput;
                }
            });
            clientRequest.enableBuffering();

            try {
                clientRequest.writeEntity();
            } catch (IOException e) {
                final String msg = "Error while writing entity to the output stream.";
                LOGGER.log(Level.SEVERE, msg, e);
                throw new ProcessingException(msg, e);
            }
        }

        containerRequest.setEntityStream(new ByteArrayInputStream(clientOutput.toByteArray()));

        boolean followRedirects = ClientProperties.getValue(clientRequest.getConfiguration().getProperties(),
                ClientProperties.FOLLOW_REDIRECTS, true);

        final InMemoryResponseWriter inMemoryResponseWriter = new InMemoryResponseWriter();
        containerRequest.setWriter(inMemoryResponseWriter);
        containerRequest.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        });
        appHandler.handle(containerRequest);

        return tryFollowRedirects(followRedirects,
                createClientResponse(
                        clientRequest,
                        inMemoryResponseWriter),
                new ClientRequest(clientRequest));

    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        CompletableFuture<ClientResponse> future = new CompletableFuture<>();
        try {
            ClientResponse response = apply(request);
            callback.response(response);
            future.complete(response);
        } catch (ProcessingException ex) {
            future.completeExceptionally(ex);
        } catch (Throwable t) {
            callback.failure(t);
            future.completeExceptionally(t);
        }
        return future;
    }

    @Override
    public void close() {
        // do nothing
    }

    private ClientResponse createClientResponse(final ClientRequest clientRequest,
                                                final InMemoryResponseWriter responseWriter) {
        final ClientResponse clientResponse = new ClientResponse(responseWriter.getStatusInfo(), clientRequest);
        clientResponse.getHeaders().putAll(responseWriter.getHeaders());
        clientResponse.setEntityStream(new ByteArrayInputStream(responseWriter.getEntity()));
        return clientResponse;
    }

    @SuppressWarnings("MagicNumber")
    private ClientResponse tryFollowRedirects(boolean followRedirects, ClientResponse response, ClientRequest request) {
        if (!followRedirects) {
            return response;
        }

        while (true) {
            switch (response.getStatus()) {
                case 303:
                case 302:
                case 307:
                    request = new ClientRequest(request);
                    request.setUri(response.getLocation());
                    if (response.getStatus() == 303) {
                        request.setMethod("GET");
                    }
                    response = apply(request);
                    break;
                default:
                    return response;
            }
        }
    }

    @Override
    public String getName() {
        return "Jersey InMemory Connector";
    }
}
