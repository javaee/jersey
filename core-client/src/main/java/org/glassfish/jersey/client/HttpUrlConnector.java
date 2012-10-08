/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.MultivaluedMap;

import javax.net.ssl.HttpsURLConnection;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Default client transport connector using {@link HttpURLConnection}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class HttpUrlConnector extends RequestWriter implements Connector {
    private final ConnectionFactory connectionFactory;

    /**
     * A factory for {@link HttpURLConnection} instances.
     * <p>
     * A factory may be used to create a {@link HttpURLConnection} and configure
     * it in a custom manner that is not possible using the Client API.
     * <p>
     * A factory instance may be registered with the constructor
     * {@link HttpUrlConnector#HttpUrlConnector(HttpUrlConnector.ConnectionFactory)}.
     * Then the {@link HttpUrlConnector} instance may be registered with a {@link JerseyClient}
     * or {@link JerseyWebTarget} configuration via
     * {@link ClientConfig#connector(org.glassfish.jersey.client.spi.Connector)}.
     */
    public interface ConnectionFactory {

        /**
         * Get a {@link HttpURLConnection} for a given URL.
         * <p>
         * Implementation of the method MUST be thread-safe and MUST ensure that
         * a dedicated {@link HttpURLConnection} instance is returned for concurrent
         * requests.
         *
         * @param url the endpoint URL.
         * @return the {@link HttpURLConnection}.
         * @throws java.io.IOException in case the connection cannot be provided.
         */
        public HttpURLConnection getConnection(URL url) throws IOException;
    }

    /**
     * Create default {@link HttpURLConnection}-based Jersey client {@link Connector connector}.
     */
    public HttpUrlConnector() {
        connectionFactory = null;
    }

    /**
     * Create default {@link HttpURLConnection}-based Jersey client {@link Connector connector}.
     *
     * @param connectionFactory {@link HttpURLConnection} instance factory.
     */
    public HttpUrlConnector(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private static InputStream getInputStream(HttpURLConnection uc) throws IOException {
        if (uc.getResponseCode() < 300) {
            return uc.getInputStream();
        } else {
            InputStream ein = uc.getErrorStream();
            return (ein != null) ? ein : new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    public ClientResponse apply(ClientRequest request) {
        try {
            return _apply(request);
        } catch (IOException ex) {
            throw new ClientException(ex);
        }
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        return MoreExecutors.sameThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.response(_apply(request));
                } catch (IOException ex) {
                    callback.failure(new ClientException(ex));
                } catch (Throwable t) {
                    callback.failure(t);
                }
            }
        });
    }

    @Override
    public void close() {
        // do nothing
    }

    private ClientResponse _apply(final ClientRequest request) throws IOException {
        final Map<String, Object> configurationProperties = request.getConfiguration().getProperties();

        final HttpURLConnection uc;

        final URL endpointUrl = request.getUri().toURL();
        if (this.connectionFactory == null) {
            uc = (HttpURLConnection) endpointUrl.openConnection();
        } else {
            uc = this.connectionFactory.getConnection(endpointUrl);
        }
        uc.setDoInput(true);

        final String httpMethod = request.getMethod();
        if (PropertiesHelper.getValue(configurationProperties,
                ClientProperties.HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, false)) {
            setRequestMethodViaJreBugWorkaround(uc, httpMethod);
        } else {
            uc.setRequestMethod(httpMethod);
        }

        uc.setInstanceFollowRedirects(PropertiesHelper.getValue(configurationProperties,
                ClientProperties.FOLLOW_REDIRECTS, true));

        uc.setConnectTimeout(PropertiesHelper.getValue(configurationProperties,
                ClientProperties.CONNECT_TIMEOUT, 0));

        uc.setReadTimeout(PropertiesHelper.getValue(configurationProperties,
                ClientProperties.READ_TIMEOUT, 0));

        if (uc instanceof HttpsURLConnection) {
            HttpsURLConnection suc = (HttpsURLConnection) uc;
            SslConfig sslConfig = PropertiesHelper.getValue(configurationProperties, ClientProperties.SSL_CONFIG, SslConfig.class);
            if (sslConfig.isHostnameVerifierSet()) {
                suc.setHostnameVerifier(sslConfig.getHostnameVerifier());
            }
            suc.setSSLSocketFactory(sslConfig.getSSLContext().getSocketFactory());
        }

        final Object entity = request.getEntity();
        if (entity != null) {
            uc.setDoOutput(true);

            if (httpMethod.equalsIgnoreCase("GET")) {
                final Logger logger = Logger.getLogger(HttpUrlConnector.class.getName());
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, LocalizationMessages.HTTPURLCONNECTION_REPLACES_GET_WITH_ENTITY());
                }
            }

            writeRequestEntity(request, new RequestEntityWriterListener() {
                @Override
                public void onRequestEntitySize(long size) {
                    if (size != -1 && size < Integer.MAX_VALUE) {
                        // HttpURLConnection uses the int type for content length

                        // this just does not work for several consecutive requests.
                        // another day wasted on HttpUrlConnection bug :/
                        // uc.setFixedLengthStreamingMode((int)size);
                    } else {
                        // TODO (copied from Jersey 1.x) it appears HttpURLConnection has some bugs in
                        // chunked encoding
                        // uc.setChunkedStreamingMode(0);

                        // TODO deal with chunked encoding
//                        Integer chunkedEncodingSize = (Integer)request.getProperties().get(
//                                ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE);
//                        if (chunkedEncodingSize != null) {
//                            uc.setChunkedStreamingMode(chunkedEncodingSize);
//                        }
                    }
                }

                @Override
                public OutboundMessageContext.StreamProvider onGetStreamProvider() throws IOException {
                    return new OutboundMessageContext.StreamProvider() {
                        @Override
                        public OutputStream getOutputStream() throws IOException {
                            return uc.getOutputStream();
                        }

                        @Override
                        public void commit() throws IOException {
                            writeOutBoundHeaders(request.getStringHeaders(), uc);
                        }
                    };
                }

            });
        } else {
            writeOutBoundHeaders(request.getStringHeaders(), uc);
        }

        ClientResponse responseContext = new ClientResponse(
                Statuses.from(uc.getResponseCode()), request);
        responseContext.setEntityStream(getInputStream(uc));
        responseContext.headers(Maps.<String, List<String>>filterKeys(uc.getHeaderFields(), Predicates.notNull()));

        return responseContext;
    }

    private void writeOutBoundHeaders(MultivaluedMap<String, String> headers, HttpURLConnection uc) {
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            List<String> headerValues = header.getValue();
            if (headerValues.size() == 1) {
                uc.setRequestProperty(header.getKey(), headerValues.get(0));
            } else {
                StringBuilder b = new StringBuilder();
                boolean add = false;
                for (Object value : headerValues) {
                    if (add) {
                        b.append(',');
                    }
                    add = true;
                    b.append(value);
                }
                uc.setRequestProperty(header.getKey(), b.toString());
            }
        }
    }

    /**
     * Workaround for a bug in {@code HttpURLConnection.setRequestMethod(String)}
     * The implementation of Sun/Oracle is throwing a {@code ProtocolException}
     * when the method is other than the HTTP/1.1 default methods. So to use {@code PROPFIND}
     * and others, we must apply this workaround.
     *
     * See issue http://java.net/jira/browse/JERSEY-639
     */
    private static void setRequestMethodViaJreBugWorkaround(final HttpURLConnection httpURLConnection, final String method) {
        try {
            httpURLConnection.setRequestMethod(method); // Check whether we are running on a buggy JRE
        } catch (final ProtocolException pe) {
            try {
                final Class<?> httpURLConnectionClass = httpURLConnection.getClass();
                final Field methodField = httpURLConnectionClass.getSuperclass().getDeclaredField("method");
                methodField.setAccessible(true);
                methodField.set(httpURLConnection, method);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getName() {
        return "HttpUrlConnection " + System.getProperty("java.version");
    }
}
