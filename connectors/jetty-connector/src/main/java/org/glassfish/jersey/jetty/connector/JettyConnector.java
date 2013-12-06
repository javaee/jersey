/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieStore;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.ByteBufferInputStream;
import org.glassfish.jersey.internal.util.collection.NonBlockingInputStream;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.ProxyConfiguration;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

/**
 * A {@link Connector} that utilizes the Jetty HTTP Client to send and receive
 * HTTP request and responses.
 * <p/>
 * The following properties are only supported at construction of this class:
 * <ul>
 * <li>{@link ClientProperties#ASYNC_THREADPOOL_SIZE}</li>
 * <li>{@link ClientProperties#CONNECT_TIMEOUT}</li>
 * <li>{@link ClientProperties#FOLLOW_REDIRECTS}</li>
 * <li>{@link ClientProperties#PROXY_URI}</li>
 * <li>{@link ClientProperties#PROXY_USERNAME}</li>
 * <li>{@link ClientProperties#PROXY_PASSWORD}</li>
 * <li>{@link ClientProperties#PROXY_PASSWORD}</li>
 * <li>{@link JettyClientProperties#SSL_CONFIG}</li>
 * <li>{@link JettyClientProperties#PREEMPTIVE_BASIC_AUTHENTICATION}</li>
 * <li>{@link JettyClientProperties#DISABLE_COOKIES}</li>
 * </ul>
 * <p/>
 * This transport supports both synchronous and asynchronous processing of client requests.
 * The following methods are supported: GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT and MOVE.
 * <p/>
 * Typical usage:
 * <p/>
 * <pre>
 * {@code
 * ClientConfig config = new ClientConfig();
 * Connector connector = new JettyConnector(config);
 * config.connector(connector);
 * Client client = ClientBuilder.newClient(config);
 *
 * // async request
 * WebTarget target = client.target("http://localhost:8080");
 * Future<Response> future = target.path("resource").request().async().get();
 *
 * // wait for 3 seconds
 * Response response = future.get(3, TimeUnit.SECONDS);
 * String entity = response.readEntity(String.class);
 * client.close();
 * }
 * </pre>
 * <p>
 * This connector supports only {@link org.glassfish.jersey.client.RequestEntityProcessing#BUFFERED entity buffering}.
 * Defining the property {@link ClientProperties#REQUEST_ENTITY_PROCESSING} has no effect on this connector.
 * </p>
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JettyConnector implements Connector {

    private static final Logger LOGGER = Logger.getLogger(JettyConnector.class.getName());

    private final HttpClient client;
    private final CookieStore cookieStore;

    /**
     * Create the new Jetty client connector.
     *
     * @param config client configuration.
     */
    JettyConnector(Configuration config) {
        SslConfigurator sslConfig = null;
        if (config != null) {
            sslConfig = PropertiesHelper.getValue(config.getProperties(), JettyClientProperties.SSL_CONFIG, SslConfigurator.class);
        }
        if (sslConfig != null) {
            final SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setSslContext(sslConfig.createSSLContext());
            this.client = new HttpClient(sslContextFactory);
        } else {
            this.client = new HttpClient();
        }

        if (config != null) {
            final Object connectTimeout = config.getProperties().get(ClientProperties.CONNECT_TIMEOUT);
            if (connectTimeout != null && connectTimeout instanceof Integer && (Integer) connectTimeout > 0) {
                client.setConnectTimeout((Integer) connectTimeout);
            }
            final Object threadPoolSize = config.getProperties().get(ClientProperties.ASYNC_THREADPOOL_SIZE);
            if (threadPoolSize != null && threadPoolSize instanceof Integer && (Integer) threadPoolSize > 0) {
                final String name = HttpClient.class.getSimpleName() + "@" + hashCode();
                final QueuedThreadPool threadPool = new QueuedThreadPool((Integer) threadPoolSize);
                threadPool.setName(name);
                client.setExecutor(threadPool);
            }
            Boolean disableCookies = (Boolean) config.getProperties().get(JettyClientProperties.DISABLE_COOKIES);
            disableCookies = (disableCookies != null) ? disableCookies : false;

            AuthenticationStore auth = client.getAuthenticationStore();
            Object basicAuthProvider = config.getProperty(JettyClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION);
            if (basicAuthProvider != null && (basicAuthProvider instanceof BasicAuthentication)) {
                auth.addAuthentication((BasicAuthentication) basicAuthProvider);
            }

            final Object proxyUri = config.getProperties().get(ClientProperties.PROXY_URI);
            if (proxyUri != null) {
                final URI u = getProxyUri(proxyUri);
                ProxyConfiguration proxyConfig = new ProxyConfiguration(u.getHost(), u.getPort());
                client.setProxyConfiguration(proxyConfig);
            }

            if (disableCookies) {
                client.setCookieStore(new HttpCookieStore.Empty());
            }

        }

        try {
            client.start();
        } catch (Exception e) {
            throw new ProcessingException("Failed to start the client.", e);
        }
        this.cookieStore = client.getCookieStore();
    }

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
     * Get the {@link HttpClient}.
     *
     * @return the {@link HttpClient}.
     */
    public HttpClient getHttpClient() {
        return client;
    }

    /**
     * Get the {@link CookieStore}.
     *
     * @return the {@link CookieStore} instance or null when
     *         JettyClientProperties.DISABLE_COOKIES set to true.
     */
    public CookieStore getCookieStore() {
        return cookieStore;
    }

    @Override
    public ClientResponse apply(final ClientRequest jerseyRequest) throws ProcessingException {
        final Request jettyRequest = translateRequest(jerseyRequest);
        final ContentProvider entity = getBytesProvider(jerseyRequest);
        if (entity != null) {
            jettyRequest.content(entity);
        }

        try {
            final ContentResponse jettyResponse = jettyRequest.send();
            final javax.ws.rs.core.Response.StatusType status = jettyResponse.getReason() == null ?
                    Statuses.from(jettyResponse.getStatus()) :
                    Statuses.from(jettyResponse.getStatus(), jettyResponse.getReason());

            final ClientResponse jerseyResponse = new ClientResponse(status, jerseyRequest);
            processResponseHeaders(jettyResponse.getHeaders(), jerseyResponse);
            try {
                jerseyResponse.setEntityStream(new HttpClientResponseInputStream(jettyResponse));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, null, e);
            }

            return jerseyResponse;
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private void processResponseHeaders(final HttpFields respHeaders, final ClientResponse jerseyResponse) {
        for (HttpField header : respHeaders) {
            final String headerName = header.getName();
            final MultivaluedMap<String, String> headers = jerseyResponse.getHeaders();
            List<String> list = headers.get(headerName);
            if (list == null) {
                list = new ArrayList<String>();
            }
            list.add(header.getValue());
            headers.put(headerName, list);
        }
    }

    private static final class HttpClientResponseInputStream extends FilterInputStream {
        HttpClientResponseInputStream(final ContentResponse jettyResponse) throws IOException {
            super(getInputStream(jettyResponse));
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }

    private static InputStream getInputStream(final ContentResponse response) throws IOException {
        return new ByteArrayInputStream(response.getContent());
    }

    private Request translateRequest(final ClientRequest clientRequest) {
        final HttpMethod method = HttpMethod.fromString(clientRequest.getMethod());
        if (method == null) {
            throw new ProcessingException(LocalizationMessages.METHOD_NOT_SUPPORTED(clientRequest.getMethod()));
        }
        final URI uri = clientRequest.getUri();
        Request request = client.newRequest(uri);
        request.method(method);

        request.followRedirects(clientRequest.resolveProperty(ClientProperties.FOLLOW_REDIRECTS, true));
        final Object readTimeout = clientRequest.getConfiguration().getProperties().get(ClientProperties.READ_TIMEOUT);
        if (readTimeout != null && readTimeout instanceof Integer && (Integer) readTimeout > 0) {
            request.timeout((Integer) readTimeout, TimeUnit.MILLISECONDS);
        }
        writeOutBoundHeaders(clientRequest.getHeaders(), request);
        return request;
    }

    private static void writeOutBoundHeaders(final MultivaluedMap<String, Object> headers, final Request request) {
        for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
            List<Object> vs = e.getValue();
            if (vs.size() == 1) {
                request.getHeaders().add(e.getKey(), vs.get(0).toString());
            } else {
                StringBuilder b = new StringBuilder();
                for (Object v : e.getValue()) {
                    if (b.length() > 0) {
                        b.append(',');
                    }
                    b.append(v);
                }
                request.getHeaders().add(e.getKey(), b.toString());
            }
        }
    }

    private ContentProvider getBytesProvider(final ClientRequest clientRequest) {
        final Object entity = clientRequest.getEntity();

        if (entity == null) {
            return null;
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        clientRequest.setStreamProvider(new OutboundMessageContext.StreamProvider() {
            @Override
            public OutputStream getOutputStream(int contentLength) throws IOException {
                return outputStream;
            }
        });

        try {
            clientRequest.writeEntity();
        } catch (IOException e) {
            throw new ProcessingException("Failed to write request entity.", e);
        }
        return new BytesContentProvider(outputStream.toByteArray());
    }

    private ContentProvider getStreamProvider(final ClientRequest clientRequest) {
        final Object entity = clientRequest.getEntity();

        if (entity == null) {
            return null;
        }

        final OutputStreamContentProvider streamContentProvider = new OutputStreamContentProvider();
        clientRequest.setStreamProvider(new OutboundMessageContext.StreamProvider() {
            @Override
            public OutputStream getOutputStream(int contentLength) throws IOException {
                return streamContentProvider.getOutputStream();
            }
        });

        try {
            clientRequest.writeEntity();
        } catch (IOException e) {
            throw new ProcessingException("Failed to write request entity.", e);
        }
        return streamContentProvider;
    }

    @Override
    public Future<?> apply(final ClientRequest jerseyRequest, final AsyncConnectorCallback callback) {
        final Request jettyRequest = translateRequest(jerseyRequest);
        final ContentProvider entity = getStreamProvider(jerseyRequest);
        if (entity != null) {
            jettyRequest.content(entity);
        }
        final AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        Throwable failure;
        try {
            final SettableFuture<ClientResponse> responseFuture = SettableFuture.create();
            Futures.addCallback(responseFuture, new FutureCallback<ClientResponse>() {
                @Override
                public void onSuccess(ClientResponse result) {
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t instanceof CancellationException) {
                        // take care of future cancellation
                        jettyRequest.abort(t);
                    }
                }
            });
            final AtomicReference<ClientResponse> jerseyResponse = new AtomicReference<ClientResponse>();
            final ByteBufferInputStream entityStream = new ByteBufferInputStream();
            buildAsyncRequest(jettyRequest)
                    .send(new Response.Listener.Empty() {

                        @Override
                        public void onHeaders(Response jettyResponse) {
                            if (responseFuture.isDone())
                                if (!callbackInvoked.compareAndSet(false, true)) {
                                    return;
                                }
                            final ClientResponse response = translateResponse(jerseyRequest, jettyResponse, entityStream);
                            jerseyResponse.set(response);
                            callback.response(response);
                        }

                        @Override
                        public void onContent(Response jettyResponse, ByteBuffer content) {
                            try {
                                entityStream.put(content);
                            } catch (InterruptedException ex) {
                                final ProcessingException pe = new ProcessingException(ex);
                                entityStream.closeQueue(pe);
                                // try to complete the future with an exception
                                responseFuture.setException(pe);
                                Thread.currentThread().interrupt();
                            }
                        }

                        @Override
                        public void onComplete(Result result) {
                            entityStream.closeQueue();
                            // try to complete the future with the response only once truly done
                            responseFuture.set(jerseyResponse.get());
                        }

                        @Override
                        public void onFailure(Response response, Throwable t) {
                            entityStream.closeQueue(t);
                            // try to complete the future with an exception
                            responseFuture.setException(t);
                            if (callbackInvoked.compareAndSet(false, true)) {
                                callback.failure(t);
                            }
                        }
                    });
            return responseFuture;
        } catch (Throwable t) {
            failure = t;
        }

        if (callbackInvoked.compareAndSet(false, true)) {
            callback.failure(failure);
        }
        return Futures.immediateFailedFuture(failure);
    }

    private Request buildAsyncRequest(final Request jettyRequest) {
        final Request request = client.newRequest(jettyRequest.getURI())
                .method(jettyRequest.method())
                .content(jettyRequest.getContent())
                .followRedirects(jettyRequest.isFollowRedirects());

        for (HttpField header : jettyRequest.getHeaders()) {
            request.getHeaders().add(header.getName(), header.getValue());
        }

        return request;
    }

    private ClientResponse translateResponse(final ClientRequest jerseyRequest,
                                             final org.eclipse.jetty.client.api.Response jettyResponse,
                                             final NonBlockingInputStream entityStream) {
        final ClientResponse jerseyResponse = new ClientResponse(Statuses.from(jettyResponse.getStatus()), jerseyRequest);
        processResponseHeaders(jettyResponse.getHeaders(), jerseyResponse);
        jerseyResponse.setEntityStream(entityStream);
        return jerseyResponse;
    }

    @Override
    public String getName() {
        return "Jetty HttpClient " + Jetty.VERSION;
    }

    @Override
    public void close() {
        try {
            client.stop();
        } catch (Exception e) {
            throw new ProcessingException("Failed to stop the client.", e);
        }
    }
}
