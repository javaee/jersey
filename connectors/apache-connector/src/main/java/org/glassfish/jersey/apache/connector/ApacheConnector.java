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

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.HttpClientParamConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.*;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.message.internal.Statuses;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.params.HttpParams;
import org.apache.http.util.VersionInfo;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * A {@link Connector} that utilizes the Apache HTTP Client to send and receive
 * HTTP request and responses.
 * <p/>
 * The following properties are only supported at construction of this class:
 * <ul>
 * <li>{@link ApacheClientProperties#CONNECTION_MANAGER}</li>
 * <li>{@link ApacheClientProperties#REQUEST_CONFIG} (or {@link ApacheClientProperties#HTTP_PARAMS})</li>
 * <li>{@link ApacheClientProperties#CREDENTIALS_PROVIDER}</li>
 * <li>{@link ApacheClientProperties#DISABLE_COOKIES}</li>
 * <li>{@link ClientProperties#PROXY_URI} (or {@link ApacheClientProperties#PROXY_URI})</li>
 * <li>{@link ClientProperties#PROXY_USERNAME} (or {@link ApacheClientProperties#PROXY_USERNAME})</li>
 * <li>{@link ClientProperties#PROXY_PASSWORD} (or {@link ApacheClientProperties#PROXY_PASSWORD})</li>
 * <li>{@link ApacheClientProperties#PREEMPTIVE_BASIC_AUTHENTICATION}</li>
 * <li>{@link ApacheClientProperties#SSL_CONFIG}</li>
 * </ul>
 * <p/>
 * By default a request entity is buffered and repeatable such that
 * authorization may be performed automatically in response to a 401 response.
 * <p/>
 * If the property {@link org.glassfish.jersey.client.ClientProperties#CHUNKED_ENCODING_SIZE} size
 * is set to a value greater than 0 then chunked encoding will be enabled
 * and the request entity (if present) will not be buffered and is not
 * repeatable. For authorization to work in such scenarios the property
 * {@link org.glassfish.jersey.apache.connector.ApacheClientProperties#PREEMPTIVE_BASIC_AUTHENTICATION} must
 * be set to {@code true}.
 * <p/>
 * If a {@link org.glassfish.jersey.client.ClientResponse} is obtained and an
 * entity is not read from the response then
 * {@link org.glassfish.jersey.client.ClientResponse#close()} MUST be called
 * after processing the response to release connection-based resources.
 * <p/>
 * Client operations are thread safe, the HTTP connection may
 * be shared between different threads.
 * <p/>
 * If a response entity is obtained that is an instance of {@link Closeable}
 * then the instance MUST be closed after processing the entity to release
 * connection-based resources.
 * <p/>
 * The following methods are currently supported: HEAD, GET, POST, PUT, DELETE, OPTIONS, PATCH and TRACE.
 *
 * @author jorgeluisw@mac.com
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @see ApacheClientProperties#CONNECTION_MANAGER
 */
@SuppressWarnings("deprecation")
public class ApacheConnector implements Connector {

    private final static Logger LOGGER = Logger.getLogger(ApacheConnector.class.getName());

    private final CloseableHttpClient client;
    private CookieStore cookieStore = null;
    private boolean preemptiveBasicAuth = false;
    private final RequestConfig requestConfig;

    private static final VersionInfo vi;
    private static final String release;

    public final static String CLIENT_CONTEXT = "CLIENT_CONTEXT";

    static {
        vi = VersionInfo.loadVersionInfo("org.apache.http.client", HttpClientBuilder.class.getClassLoader());
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
        Object reqConfig = null;

        if (config != null) {
            connectionManager = config.getProperties().get(ApacheClientProperties.CONNECTION_MANAGER);

            if (connectionManager != null) {
                if (!(connectionManager instanceof HttpClientConnectionManager)) {
                    LOGGER.log(
                            Level.WARNING,
                            LocalizationMessages.IGNORING_VALUE_OF_PROPERTY(
                                    ApacheClientProperties.CONNECTION_MANAGER,
                                    connectionManager.getClass().getName(),
                                    HttpClientConnectionManager.class.getName())
                    );
                    connectionManager = null;
                }
            }

            reqConfig = config.getProperties().get(ApacheClientProperties.REQUEST_CONFIG);
            if (reqConfig == null) {
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
            } else {
                if (!(reqConfig instanceof RequestConfig)) {
                    LOGGER.log(
                            Level.WARNING,
                            LocalizationMessages.IGNORING_VALUE_OF_PROPERTY(
                                    ApacheClientProperties.REQUEST_CONFIG,
                                    reqConfig.getClass().getName(),
                                    RequestConfig.class.getName())
                    );
                    reqConfig = null;
                }
            }
        }

        SslConfigurator sslConfig = null;
        if (config != null) {
            sslConfig = PropertiesHelper.getValue(config.getProperties(), ApacheClientProperties.SSL_CONFIG, SslConfigurator.class);
        }

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setConnectionManager((HttpClientConnectionManager) connectionManager);

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

        if (sslConfig != null) {
            clientBuilder.setSslcontext(sslConfig.createSSLContext());
        }
        int connectTimeout = 0, socketTimeout = 0;
        boolean ignoreCookies = false;
        if (config != null) {
            connectTimeout = PropertiesHelper.getValue(config.getProperties(), ClientProperties.CONNECT_TIMEOUT, 0);
            socketTimeout = PropertiesHelper.getValue(config.getProperties(), ClientProperties.READ_TIMEOUT, 0);
            ignoreCookies = PropertiesHelper.isProperty(config.getProperties(), ApacheClientProperties.DISABLE_COOKIES);

            Object credentialsProvider = config.getProperty(ApacheClientProperties.CREDENTIALS_PROVIDER);
            if (credentialsProvider != null && (credentialsProvider instanceof CredentialsProvider)) {
                clientBuilder.setDefaultCredentialsProvider((CredentialsProvider) credentialsProvider);
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
                userName = PropertiesHelper.getValue( config.getProperties(), ClientProperties.PROXY_USERNAME, String.class);
                if (userName == null) {
                    userName = PropertiesHelper.getValue(config.getProperties(), ApacheClientProperties.PROXY_USERNAME, String.class);
                }
                if (userName != null) {
                    String password;
                    password = PropertiesHelper.getValue( config.getProperties(), ClientProperties.PROXY_PASSWORD, String.class);
                    if (password == null) {
                        password = PropertiesHelper.getValue(config.getProperties(), ApacheClientProperties.PROXY_PASSWORD, String.class);
                    }

                    if (password != null) {
                        CredentialsProvider credsProvider = new BasicCredentialsProvider();
                        credsProvider.setCredentials(
                                new AuthScope(u.getHost(), u.getPort()),
                                new UsernamePasswordCredentials(userName, password)
                        );
                        clientBuilder.setDefaultCredentialsProvider(credsProvider);
                    }
                }
                clientBuilder.setProxy(proxy);
            }

            Boolean preemptiveBasicAuthProperty = (Boolean) config.getProperties()
                    .get(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION);
            this.preemptiveBasicAuth = (preemptiveBasicAuthProperty != null) ? preemptiveBasicAuthProperty : false;
        }

        if (httpParams != null) {
            RequestConfig.Builder reqConfigBuilder = RequestConfig.copy(HttpClientParamConfig.getRequestConfig((HttpParams) httpParams));
            if (connectTimeout > 0) {
                reqConfigBuilder.setConnectTimeout(connectTimeout);
            }
            if (socketTimeout > 0) {
                reqConfigBuilder.setSocketTimeout(socketTimeout);
            }
            if (ignoreCookies) {
                reqConfigBuilder.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
            }
            requestConfig = reqConfigBuilder.build();
        } else {
            if (reqConfig != null) {
                RequestConfig.Builder reqConfigBuilder = RequestConfig.copy((RequestConfig) reqConfig);
                if (connectTimeout > 0) {
                    reqConfigBuilder.setConnectTimeout(connectTimeout);
                }
                if (socketTimeout > 0) {
                    reqConfigBuilder.setSocketTimeout(socketTimeout);
                }
                if (ignoreCookies) {
                    reqConfigBuilder.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
                }
                requestConfig = reqConfigBuilder.build();
            } else {
                requestConfigBuilder.setConnectTimeout(connectTimeout);
                requestConfigBuilder.setSocketTimeout(socketTimeout);
                if (ignoreCookies) {
                    requestConfigBuilder.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
                }
                requestConfig = requestConfigBuilder.build();
            }
        }
        if (requestConfig.getCookieSpec() == null || !requestConfig.getCookieSpec().equals(CookieSpecs.IGNORE_COOKIES)) {
            this.cookieStore = new BasicCookieStore();
            clientBuilder.setDefaultCookieStore(cookieStore);
        }
        clientBuilder.setDefaultRequestConfig(requestConfig);
        this.client = clientBuilder.build();
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
            final CloseableHttpResponse response;
            final HttpClientContext context = HttpClientContext.create();
            if (preemptiveBasicAuth) {
                AuthCache authCache = new BasicAuthCache();
                BasicScheme basicScheme = new BasicScheme();
                authCache.put(getHost(request), basicScheme);
                context.setAuthCache(authCache);
                response = client.execute(getHost(request), request, context);
            } else {
                response = client.execute(getHost(request), request, context);
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
            responseContext.getRequestContext().setProperty(CLIENT_CONTEXT, context);

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
        try {
            client.close();
        } catch (IOException e) {
            throw new ProcessingException("Failed to stop the client.", e);
        }
    }

    private HttpHost getHost(final HttpUriRequest request) {
        return new HttpHost(request.getURI().getHost(), request.getURI().getPort(), request.getURI().getScheme());
    }

    private HttpUriRequest getUriHttpRequest(final ClientRequest clientRequest) {
        final String strMethod = clientRequest.getMethod();
        final URI uri = clientRequest.getUri();
        final HttpEntity entity = getHttpEntity(clientRequest);
        final RequestBuilder requestBuilder = RequestBuilder.create(strMethod).setUri(uri);
        requestBuilder.setConfig(RequestConfig.copy(requestConfig).setRedirectsEnabled(clientRequest.resolveProperty(ClientProperties.FOLLOW_REDIRECTS, requestConfig.isRedirectsEnabled())).build());
        if (entity != null) {
            requestBuilder.setEntity(entity);
        }
        return requestBuilder.build();
    }

    private HttpEntity getHttpEntity(final ClientRequest clientRequest) {
        final Object entity = clientRequest.getEntity();

        if (entity == null) {
            return null;
        }

        return new AbstractHttpEntity() {
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
                return null;
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

        HttpClientResponseInputStream(final CloseableHttpResponse response) throws IOException {
            super(getInputStream(response));
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }

    private static InputStream getInputStream(final CloseableHttpResponse response) throws IOException {

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