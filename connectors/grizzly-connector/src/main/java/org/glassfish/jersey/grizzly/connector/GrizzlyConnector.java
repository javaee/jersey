/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.grizzly.connector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.Version;
import org.glassfish.jersey.internal.util.collection.ByteBufferInputStream;
import org.glassfish.jersey.internal.util.collection.NonBlockingInputStream;
import org.glassfish.jersey.message.internal.HeaderUtils;
import org.glassfish.jersey.message.internal.OutboundMessageContext;

import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ProxyServerSelector;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.providers.grizzly.FeedableBodyGenerator;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import com.ning.http.util.ProxyUtils;

/**
 * The transport using the AsyncHttpClient.
 *
 * @author Stepan Kopriva
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class GrizzlyConnector implements Connector {

    private final AsyncHttpClient grizzlyClient;

    /**
     * Create new connector based on Grizzly asynchronous client library.
     *
     * @param client                Jersey client instance to create the connector for.
     * @param config                Jersey client runtime configuration to be used to configure the connector parameters.
     * @param asyncClientCustomizer Async HTTP Client configuration builder customizer.
     */
    GrizzlyConnector(final Client client,
                     final Configuration config,
                     final GrizzlyConnectorProvider.AsyncClientCustomizer asyncClientCustomizer) {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

        ExecutorService executorService;
        if (config != null) {
            final Object threadPoolSize = config.getProperties().get(ClientProperties.ASYNC_THREADPOOL_SIZE);

            if (threadPoolSize != null && threadPoolSize instanceof Integer && (Integer) threadPoolSize > 0) {
                executorService = Executors.newFixedThreadPool((Integer) threadPoolSize);
            } else {
                executorService = Executors.newCachedThreadPool();
            }

            builder = builder.setExecutorService(executorService);

            builder.setConnectTimeout(ClientProperties.getValue(config.getProperties(),
                                                                ClientProperties.CONNECT_TIMEOUT, 0));

            builder.setRequestTimeout(ClientProperties.getValue(config.getProperties(),
                                                                ClientProperties.READ_TIMEOUT, 0));

            Object proxyUri;
            proxyUri = config.getProperty(ClientProperties.PROXY_URI);
            if (proxyUri != null) {
                final URI u = getProxyUri(proxyUri);
                final Properties proxyProperties = new Properties();
                proxyProperties.setProperty(ProxyUtils.PROXY_PROTOCOL, u.getScheme());
                proxyProperties.setProperty(ProxyUtils.PROXY_HOST, u.getHost());
                proxyProperties.setProperty(ProxyUtils.PROXY_PORT, String.valueOf(u.getPort()));

                final String userName = ClientProperties.getValue(
                        config.getProperties(), ClientProperties.PROXY_USERNAME, String.class);
                if (userName != null) {
                    proxyProperties.setProperty(ProxyUtils.PROXY_USER, userName);

                    final String password = ClientProperties.getValue(
                            config.getProperties(), ClientProperties.PROXY_PASSWORD, String.class);
                    if (password != null) {
                        proxyProperties.setProperty(ProxyUtils.PROXY_PASSWORD, password);
                    }
                }
                ProxyServerSelector proxyServerSelector = ProxyUtils.createProxyServerSelector(proxyProperties);
                builder.setProxyServerSelector(proxyServerSelector);
            }
        } else {
            executorService = Executors.newCachedThreadPool();
            builder.setExecutorService(executorService);
        }

        builder.setAllowPoolingConnections(true);
        if (client.getSslContext() != null) {
            builder.setSSLContext(client.getSslContext());
        }
        if (client.getHostnameVerifier() != null) {
            builder.setHostnameVerifier(client.getHostnameVerifier());
        }

        if (asyncClientCustomizer != null) {
            builder = asyncClientCustomizer.customize(client, config, builder);
        }

        AsyncHttpClientConfig asyncClientConfig = builder.build();

        this.grizzlyClient = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(asyncClientConfig), asyncClientConfig);
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static URI getProxyUri(final Object proxy) {
        if (proxy instanceof URI) {
            return (URI) proxy;
        } else if (proxy instanceof String) {
            return URI.create((String) proxy);
        } else {
            throw new ProcessingException(LocalizationMessages.WRONG_PROXY_URI_TYPE(ClientProperties.PROXY_URI));
        }
    }

    /**
     * Get the underlying Grizzly {@link com.ning.http.client.AsyncHttpClient} instance.
     *
     * @return underlying Grizzly {@link com.ning.http.client.AsyncHttpClient} instance.
     */
    public AsyncHttpClient getGrizzlyClient() {
        return grizzlyClient;
    }

    /**
     * Sends the {@link javax.ws.rs.core.Request} via Grizzly transport and returns the {@link javax.ws.rs.core.Response}.
     *
     * @param request Jersey client request to be sent.
     * @return received response.
     */
    @Override
    public ClientResponse apply(final ClientRequest request) {
        final Request connectorRequest = translate(request);
        final Map<String, String> clientHeadersSnapshot = writeOutBoundHeaders(request.getHeaders(), connectorRequest);

        final CompletableFuture<ClientResponse> responseFuture = new CompletableFuture<>();
        final ByteBufferInputStream entityStream = new ByteBufferInputStream();
        final AtomicBoolean futureSet = new AtomicBoolean(false);

        try {
            grizzlyClient.executeRequest(connectorRequest, new AsyncHandler<Void>() {
                private volatile HttpResponseStatus status = null;

                @Override
                public STATE onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
                    status = responseStatus;
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                    if (!futureSet.compareAndSet(false, true)) {
                        return STATE.ABORT;
                    }

                    HeaderUtils.checkHeaderChanges(clientHeadersSnapshot, request.getHeaders(),
                                                   GrizzlyConnector.this.getClass().getName());

                    responseFuture.complete(translate(request, this.status, headers, entityStream));
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                    entityStream.put(bodyPart.getBodyByteBuffer());
                    return STATE.CONTINUE;
                }

                @Override
                public Void onCompleted() throws Exception {
                    entityStream.closeQueue();
                    return null;
                }

                @Override
                public void onThrowable(Throwable t) {
                    entityStream.closeQueue(t);

                    if (futureSet.compareAndSet(false, true)) {
                        t = t instanceof IOException ? new ProcessingException(t.getMessage(), t) : t;
                        responseFuture.completeExceptionally(t);
                    }
                }
            });

            return responseFuture.get();
        } catch (ExecutionException ex) {
            Throwable e = ex.getCause() == null ? ex : ex.getCause();
            throw new ProcessingException(e.getMessage(), e);
        } catch (InterruptedException ex) {
            throw new ProcessingException(ex.getMessage(), ex);
        }
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        final Request connectorRequest = translate(request);
        final Map<String, String> clientHeadersSnapshot = writeOutBoundHeaders(request.getHeaders(), connectorRequest);
        final ByteBufferInputStream entityStream = new ByteBufferInputStream();
        final AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        Throwable failure;
        try {
            return grizzlyClient.executeRequest(connectorRequest, new AsyncHandler<Void>() {
                private volatile HttpResponseStatus status = null;

                @Override
                public STATE onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
                    status = responseStatus;
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                    if (!callbackInvoked.compareAndSet(false, true)) {
                        return STATE.ABORT;
                    }

                    HeaderUtils.checkHeaderChanges(clientHeadersSnapshot, request.getHeaders(),
                            GrizzlyConnector.this.getClass().getName());
                    // hand-off to grizzly's application thread pool for response processing
                    processResponse(new Runnable() {
                        @Override
                        public void run() {
                            callback.response(translate(request, status, headers, entityStream));
                        }
                    });
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                    entityStream.put(bodyPart.getBodyByteBuffer());
                    return STATE.CONTINUE;
                }

                @Override
                public Void onCompleted() throws Exception {
                    entityStream.closeQueue();
                    return null;
                }

                @Override
                public void onThrowable(Throwable t) {
                    entityStream.closeQueue(t);

                    if (callbackInvoked.compareAndSet(false, true)) {
                        t = t instanceof IOException ? new ProcessingException(t.getMessage(), t) : t;
                        callback.failure(t);
                    }
                }
            });
        } catch (Throwable t) {
            failure = t;
        }

        if (callbackInvoked.compareAndSet(false, true)) {
            callback.failure(failure);
        }
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(failure);
        return future;
    }

    @Override
    public void close() {
        grizzlyClient.close();
    }

    private ClientResponse translate(final ClientRequest requestContext,
                                     final HttpResponseStatus status,
                                     final HttpResponseHeaders headers,
                                     final NonBlockingInputStream entityStream) {

        final ClientResponse responseContext = new ClientResponse(new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return status.getStatusCode();
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.familyOf(status.getStatusCode());
            }

            @Override
            public String getReasonPhrase() {
                return status.getStatusText();
            }
        }, requestContext);

        for (Map.Entry<String, List<String>> entry : headers.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                responseContext.getHeaders().add(entry.getKey(), value);
            }
        }

        responseContext.setEntityStream(entityStream);

        return responseContext;
    }

    private com.ning.http.client.Request translate(final ClientRequest requestContext) {
        final String strMethod = requestContext.getMethod();
        final URI uri = requestContext.getUri();

        RequestBuilder builder = new RequestBuilder(strMethod).setUrl(uri.toString());

        builder.setFollowRedirects(requestContext.resolveProperty(ClientProperties.FOLLOW_REDIRECTS, true));

        if (requestContext.hasEntity()) {

            final RequestEntityProcessing entityProcessing =
                    requestContext.resolveProperty(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.class);

            if (entityProcessing == RequestEntityProcessing.BUFFERED) {
                byte[] entityBytes = bufferEntity(requestContext);
                builder = builder.setBody(entityBytes);
            } else {
                final FeedableBodyGenerator bodyGenerator = new FeedableBodyGenerator();
                final Integer chunkSize = requestContext.resolveProperty(
                        ClientProperties.CHUNKED_ENCODING_SIZE, ClientProperties.DEFAULT_CHUNK_SIZE);
                bodyGenerator.setMaxPendingBytes(chunkSize);
                final FeedableBodyGenerator.Feeder feeder = new FeedableBodyGenerator.SimpleFeeder(bodyGenerator) {
                    @Override
                    public void flush() throws IOException {
                        requestContext.writeEntity();
                    }
                };
                requestContext.setStreamProvider(new OutboundMessageContext.StreamProvider() {

                    @Override
                    public OutputStream getOutputStream(int contentLength) throws IOException {
                        return new FeederAdapter(feeder);
                    }
                });
                bodyGenerator.setFeeder(feeder);
                builder.setBody(bodyGenerator);
            }
        }

        final GrizzlyConnectorProvider.RequestCustomizer requestCustomizer = requestContext.resolveProperty(
                GrizzlyConnectorProvider.REQUEST_CUSTOMIZER,
                GrizzlyConnectorProvider.RequestCustomizer.class);
        if (requestCustomizer != null) {
            builder = requestCustomizer.customize(requestContext, builder);
        }

        return builder.build();
    }

    /**
     * Submits the response processing on Grizzly client's application thread pool.
     *
     * @param responseTask task to be processed on application thread pool.
     */
    private void processResponse(Runnable responseTask) {
        this.grizzlyClient.getConfig().executorService().submit(responseTask);
    }

    /**
     * Utility OutputStream implementation that can feed Grizzly chunk-encoded body generator.
     */
    private class FeederAdapter extends OutputStream {

        final FeedableBodyGenerator.Feeder delegate;

        /**
         * Get me a new adapter for given feeder.
         *
         * @param bodyFeeder adaptee to get fed as an output stream.
         */
        FeederAdapter(FeedableBodyGenerator.Feeder bodyFeeder) {
            this.delegate = bodyFeeder;
        }

        @Override
        public void write(int b) throws IOException {
            final byte[] buffer = new byte[1];
            buffer[0] = (byte) b;
            delegate.feed(Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, buffer), false);
        }

        @Override
        public void write(byte[] b) throws IOException {
            delegate.feed(Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, b), false);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.feed(Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, b, off, len), false);
        }

        @Override
        public void close() throws IOException {
            delegate.feed(Buffers.EMPTY_BUFFER, true);
        }
    }

    @SuppressWarnings("MagicNumber")
    private byte[] bufferEntity(ClientRequest requestContext) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        requestContext.setStreamProvider(new OutboundMessageContext.StreamProvider() {

            @Override
            public OutputStream getOutputStream(int contentLength) throws IOException {
                return baos;
            }
        });
        try {
            requestContext.writeEntity();
        } catch (IOException e) {
            throw new ProcessingException(LocalizationMessages.ERROR_BUFFERING_ENTITY(), e);
        }
        return baos.toByteArray();
    }

    private static Map<String, String> writeOutBoundHeaders(final MultivaluedMap<String, Object> headers,
                                                            final com.ning.http.client.Request request) {
        Map<String, String> stringHeaders = HeaderUtils.asStringHeadersSingleValue(headers);

        for (Map.Entry<String, String> e : stringHeaders.entrySet()) {
            request.getHeaders().add(e.getKey(), e.getValue());
        }
        return stringHeaders;
    }

    @Override
    public String getName() {
        return String.format("Async HTTP Grizzly Connector %s", Version.getVersion());
    }
}
