/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.apache.connector;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.message.internal.Statuses;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.VersionInfo;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * A {@link Connector} that utilizes the Apache HTTP Client to send and receive
 * HTTP request and responses.
 * <p/>
 * The following properties are only supported at construction of this class:
 * <ul>
 * <li>{@link ApacheClientProperties#CONNECTION_MANAGER}</li>
 * <li>{@link ApacheClientProperties#HTTP_PARAMS}</li>
 * <li>{@link ApacheClientProperties#CREDENTIALS_PROVIDER}</li>
 * <li>{@link ApacheClientProperties#DISABLE_COOKIES}</li>
 * <li>{@link ClientProperties#PROXY_URI} (or {@link ApacheClientProperties#PROXY_URI})</li>
 * <li>{@link ClientProperties#PROXY_USERNAME} (or {@link ApacheClientProperties#PROXY_USERNAME})</li>
 * <li>{@link ClientProperties#PROXY_PASSWORD} (or {@link ApacheClientProperties#PROXY_PASSWORD})</li>
 * <li>{@link ClientProperties#REQUEST_ENTITY_PROCESSING} - default value is {@link RequestEntityProcessing#CHUNKED}</li>
 * <li>{@link ApacheClientProperties#PREEMPTIVE_BASIC_AUTHENTICATION}</li>
 * <li>{@link ApacheClientProperties#SSL_CONFIG}</li>
 * </ul>
 * <p>
 * This connector uses {@link RequestEntityProcessing#CHUNKED chunked encoding} as a default setting. This can
 * be overriden by the {@link ClientProperties#REQUEST_ENTITY_PROCESSING}.
 * </p>
 * <p>
 * Using of authorization is dependent on the chunk encoding setting. If the entity
 * buffering is enabled, the entity is buffered and authorization can be performed
 * automatically in response to a 401 by sending the request again. When entity buffering
 * is disabled (chunked encoding is used) then the property
 * {@link org.glassfish.jersey.apache.connector.ApacheClientProperties#PREEMPTIVE_BASIC_AUTHENTICATION} must
 * be set to {@code true}.
 * </p>
 * <p>
 * If a {@link org.glassfish.jersey.client.ClientResponse} is obtained and an
 * entity is not read from the response then
 * {@link org.glassfish.jersey.client.ClientResponse#close()} MUST be called
 * after processing the response to release connection-based resources.
 * </p>
 * <p>
 * Client operations are thread safe, the HTTP connection may
 * be shared between different threads.
 * <p/>
 * <p>
 * If a response entity is obtained that is an instance of {@link Closeable}
 * then the instance MUST be closed after processing the entity to release
 * connection-based resources.
 * </p>
 * <p>
 * The following methods are currently supported: HEAD, GET, POST, PUT, DELETE and OPTIONS.
 * </p>
 *
 * @author jorgeluisw@mac.com
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @see ApacheClientProperties#CONNECTION_MANAGER
 */
public class ApacheConnector implements Connector {

    private final static Logger LOGGER = Logger.getLogger(ApacheConnector.class.getName());

    private final HttpClient client;
    private CookieStore cookieStore = null;
    private boolean preemptiveBasicAuth = false;

    private static final VersionInfo vi;
    private static final String release;

    static {
        vi = VersionInfo.loadVersionInfo("org.apache.http.client", DefaultHttpClient.class.getClassLoader());
        release = (vi != null) ? vi.getRelease() : VersionInfo.UNAVAILABLE;
    }

    /**
     * Create the new Apache HTTP Client connector.
     *
     * @param config client configuration.
     */
    public ApacheConnector(Configuration config) {
        Object connectionManager = null;
        Object httpParams = null;

        if (config != null) {
            connectionManager = config.getProperties().get(ApacheClientProperties.CONNECTION_MANAGER);

            if (connectionManager != null) {
                if (!(connectionManager instanceof ClientConnectionManager)) {
                    LOGGER.log(
                            Level.WARNING,
                            LocalizationMessages.IGNORING_VALUE_OF_PROPERTY(
                                    ApacheClientProperties.CONNECTION_MANAGER,
                                    connectionManager.getClass().getName(),
                                    ClientConnectionManager.class.getName())
                    );
                    connectionManager = null;
                }
            }

            httpParams = config.getProperties().get(ApacheClientProperties.HTTP_PARAMS);
            if (httpParams != null) {
                if (!(httpParams instanceof HttpParams)) {
                    LOGGER.log(
                            Level.WARNING,
                            LocalizationMessages.IGNORING_VALUE_OF_PROPERTY(
                                    ApacheClientProperties.HTTP_PARAMS,
                                    httpParams.getClass().getName(),
                                    HttpParams.class.getName())
                    );
                    httpParams = null;
                }
            }
        }

        SslConfigurator sslConfig = null;
        if (config != null) {
            sslConfig = PropertiesHelper.getValue(config.getProperties(), ApacheClientProperties.SSL_CONFIG, SslConfigurator.class);
        }

        this.client = new DefaultHttpClient((ClientConnectionManager) connectionManager, (HttpParams) httpParams);
        if (sslConfig != null) {
            SSLSocketFactory socketFactory = new SSLSocketFactory(sslConfig.createSSLContext());
            Scheme https = new Scheme("https", 443, socketFactory);
            client.getConnectionManager().getSchemeRegistry().register(https);
        }
        if (config != null) {
            client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
                    PropertiesHelper.getValue(config.getProperties(), ClientProperties.CONNECT_TIMEOUT, 0));
            client.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT,
                    PropertiesHelper.getValue(config.getProperties(), ClientProperties.READ_TIMEOUT, 0));

            for (Map.Entry<String, Object> entry : config.getProperties().entrySet()) {
                client.getParams().setParameter(entry.getKey(), entry.getValue());
            }

            if (PropertiesHelper.isProperty(config.getProperties(), ApacheClientProperties.DISABLE_COOKIES)) {
                client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
            }

            Object credentialsProvider = config.getProperty(ApacheClientProperties.CREDENTIALS_PROVIDER);
            if (credentialsProvider != null && (credentialsProvider instanceof CredentialsProvider)) {
                ((DefaultHttpClient) client).setCredentialsProvider((CredentialsProvider) credentialsProvider);
            }


            Object proxyUri;
            proxyUri = config.getProperty(ClientProperties.PROXY_URI);
            if (proxyUri == null) {
                proxyUri = config.getProperty(ApacheClientProperties.PROXY_URI);
            }
            if (proxyUri != null) {
                final URI u = getProxyUri(proxyUri);
                final HttpHost proxy = new HttpHost(u.getHost(), u.getPort(), u.getScheme());

                String userName;
                userName = PropertiesHelper.getValue(config.getProperties(), ClientProperties.PROXY_USERNAME, String.class);
                if (userName == null) {
                    userName = PropertiesHelper.getValue(
                            config.getProperties(), ApacheClientProperties.PROXY_USERNAME, String.class);
                }
                if (userName != null) {
                    String password;
                    password = PropertiesHelper.getValue(config.getProperties(), ClientProperties.PROXY_PASSWORD, String.class);
                    if (password == null) {
                        password = PropertiesHelper.getValue(
                                config.getProperties(), ApacheClientProperties.PROXY_PASSWORD, String.class);
                    }

                    if (password != null) {
                        ((DefaultHttpClient) client).getCredentialsProvider().setCredentials(
                                new AuthScope(u.getHost(), u.getPort()),
                                new UsernamePasswordCredentials(userName, password)
                        );
                    }
                }
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }

            Boolean preemptiveBasicAuthProperty = (Boolean) config.getProperties()
                    .get(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION);
            this.preemptiveBasicAuth = (preemptiveBasicAuthProperty != null) ? preemptiveBasicAuthProperty : false;
        }

        if (client.getParams().getParameter(ClientPNames.COOKIE_POLICY) == null
                || !client.getParams().getParameter(ClientPNames.COOKIE_POLICY).equals(CookiePolicy.IGNORE_COOKIES)) {
            this.cookieStore = new BasicCookieStore();
            ((DefaultHttpClient) client).setCookieStore(cookieStore);
        }
    }

    /**
     * Get the {@link HttpClient}.
     *
     * @return the {@link HttpClient}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public HttpClient getHttpClient() {
        return client;
    }

    /**
     * Get the {@link CookieStore}.
     *
     * @return the {@link CookieStore} instance or {@code null} when {@value ApacheClientProperties#DISABLE_COOKIES} set to
     *         {@code true}.
     */
    public CookieStore getCookieStore() {
        return cookieStore;
    }

    private static URI getProxyUri(final Object proxy) {
        if (proxy instanceof URI) {
            return (URI) proxy;
        } else if (proxy instanceof String) {
            return URI.create((String) proxy);
        } else {
            throw new ProcessingException(LocalizationMessages.WRONG_PROXY_URI_TYPE(ApacheClientProperties.PROXY_URI));
        }
    }

    @Override
    public ClientResponse apply(final ClientRequest clientRequest) throws ProcessingException {
        final HttpUriRequest request = getUriHttpRequest(clientRequest);

        writeOutBoundHeaders(clientRequest.getHeaders(), request);

        try {
            final HttpResponse response;

            if (preemptiveBasicAuth) {
                AuthCache authCache = new BasicAuthCache();
                BasicScheme basicScheme = new BasicScheme();
                authCache.put(getHost(request), basicScheme);
                BasicHttpContext localContext = new BasicHttpContext();
                localContext.setAttribute(ClientContext.AUTH_CACHE, authCache);

                response = client.execute(getHost(request), request, localContext);
            } else {
                response = client.execute(getHost(request), request);
            }

            final Response.StatusType status = response.getStatusLine().getReasonPhrase() == null ?
                    Statuses.from(response.getStatusLine().getStatusCode()) :
                    Statuses.from(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());

            final ClientResponse responseContext = new ClientResponse(status, clientRequest);

            final Header[] respHeaders = response.getAllHeaders();
            for (Header header : respHeaders) {
                final String headerName = header.getName();
                final MultivaluedMap<String, String> headers = responseContext.getHeaders();
                List<String> list = headers.get(headerName);
                if (list == null) {
                    list = new ArrayList<String>();
                }
                list.add(header.getValue());
                headers.put(headerName, list);
            }

            try {
                responseContext.setEntityStream(new HttpClientResponseInputStream(response));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, null, e);
            }

            return responseContext;
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        return MoreExecutors.sameThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.response(apply(request));
                } catch (ProcessingException ex) {
                    callback.failure(ex);
                } catch (Throwable t) {
                    callback.failure(t);
                }
            }
        });
    }

    @Override
    public String getName() {
        return "Apache HttpClient " + release;
    }

    @Override
    public void close() {
        client.getConnectionManager().shutdown();
    }

    private HttpHost getHost(final HttpUriRequest request) {
        return new HttpHost(request.getURI().getHost(), request.getURI().getPort(), request.getURI().getScheme());
    }

    private HttpUriRequest getUriHttpRequest(final ClientRequest clientRequest) {
        final String strMethod = clientRequest.getMethod();
        final URI uri = clientRequest.getUri();

        Boolean bufferingEnabled = clientRequest.resolveProperty(ClientProperties.REQUEST_ENTITY_PROCESSING,
                RequestEntityProcessing.class) == RequestEntityProcessing.BUFFERED;

        final HttpEntity entity = getHttpEntity(clientRequest, bufferingEnabled);
        final HttpUriRequest request;

        if (strMethod.equals(HttpGet.METHOD_NAME)) {
            request = new HttpGet(uri);
        } else if (strMethod.equals(HttpPost.METHOD_NAME)) {
            request = new HttpPost(uri);
        } else if (strMethod.equals(HttpPut.METHOD_NAME)) {
            request = new HttpPut(uri);
        } else if (strMethod.equals(HttpDelete.METHOD_NAME)) {
            request = new HttpDelete(uri);
        } else if (strMethod.equals(HttpHead.METHOD_NAME)) {
            request = new HttpHead(uri);
        } else if (strMethod.equals(HttpOptions.METHOD_NAME)) {
            request = new HttpOptions(uri);
        } else if (strMethod.equals(HttpPatch.METHOD_NAME)) {
            request = new HttpPatch(uri);
        } else if (strMethod.equals(HttpTrace.METHOD_NAME)) {
            request = new HttpTrace(uri);
        } else {
            request = new HttpEntityEnclosingRequestBase() {
                @Override
                public String getMethod() {
                    return strMethod;
                }

                @Override
                public URI getURI() {
                    return uri;
                }
            };
        }
        request.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS,
                clientRequest.resolveProperty(ClientProperties.FOLLOW_REDIRECTS, true));

        if (entity != null && request instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
        } else if (entity != null) {
            throw new ProcessingException(LocalizationMessages.ENTITY_NOT_SUPPORTED(clientRequest.getMethod()));
        }

        Integer chunkSize = clientRequest.resolveProperty(ClientProperties.CHUNKED_ENCODING_SIZE, Integer.class);
        if (chunkSize != null && !bufferingEnabled) {
            client.getParams().setIntParameter(CoreConnectionPNames.MIN_CHUNK_LIMIT, chunkSize);
        }
        return request;
    }


    private HttpEntity getHttpEntity(final ClientRequest clientRequest, final boolean bufferingEnabled) {
        final Object entity = clientRequest.getEntity();

        if (entity == null) {
            return null;
        }

        AbstractHttpEntity httpEntity = new AbstractHttpEntity() {
            @Override
            public boolean isRepeatable() {
                return false;
            }

            @Override
            public long getContentLength() {
                return -1;
            }

            @Override
            public InputStream getContent() throws IOException, IllegalStateException {
                if (bufferingEnabled) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);
                    writeTo(buffer);
                    return new ByteArrayInputStream(buffer.toByteArray());
                } else {
                    return null;
                }
            }

            @Override
            public void writeTo(final OutputStream outputStream) throws IOException {
                clientRequest.setStreamProvider(new OutboundMessageContext.StreamProvider() {
                    @Override
                    public OutputStream getOutputStream(int contentLength) throws IOException {
                        return outputStream;
                    }
                });
                clientRequest.writeEntity();
            }

            @Override
            public boolean isStreaming() {
                return false;
            }
        };

        if (bufferingEnabled) {
            try {
                return new BufferedHttpEntity(httpEntity);
            } catch (IOException e) {
                throw new ProcessingException(LocalizationMessages.ERROR_BUFFERING_ENTITY(), e);
            }
        } else {
            return httpEntity;
        }
    }

    private void writeOutBoundHeaders(final MultivaluedMap<String, Object> headers, final HttpUriRequest request) {
        for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
            List<Object> vs = e.getValue();
            if (vs.size() == 1) {
                request.addHeader(e.getKey(), vs.get(0).toString());
            } else {
                StringBuilder b = new StringBuilder();
                for (Object v : e.getValue()) {
                    if (b.length() > 0) {
                        b.append(',');
                    }
                    b.append(v);
                }
                request.addHeader(e.getKey(), b.toString());
            }
        }
    }

    private static final class HttpClientResponseInputStream extends FilterInputStream {

        HttpClientResponseInputStream(final HttpResponse response) throws IOException {
            super(getInputStream(response));
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }

    private static InputStream getInputStream(final HttpResponse response) throws IOException {

        if (response.getEntity() == null) {
            return new ByteArrayInputStream(new byte[0]);
        } else {
            final InputStream i = response.getEntity().getContent();
            if (i.markSupported()) {
                return i;
            }
            return new BufferedInputStream(i, ReaderWriter.BUFFER_SIZE);
        }
    }
}
