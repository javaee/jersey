/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;
import org.glassfish.jersey.internal.util.collection.Values;
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
public class HttpUrlConnector implements Connector {
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

    private static InputStream getInputStream(final HttpURLConnection uc) throws IOException {
        return new InputStream() {
            private final UnsafeValue<InputStream, IOException> in = Values.lazy(new UnsafeValue<InputStream, IOException>() {
                @Override
                public InputStream get() throws IOException {
                    if (uc.getResponseCode() < 400) {
                        return uc.getInputStream();
                    } else {
                        InputStream ein = uc.getErrorStream();
                        return (ein != null) ? ein : new ByteArrayInputStream(new byte[0]);
                    }
                }
            });

            @Override
            public int read() throws IOException {
                return in.get().read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return in.get().read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return in.get().read(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return in.get().skip(n);
            }

            @Override
            public int available() throws IOException {
                return in.get().available();
            }

            @Override
            public void close() throws IOException {
                in.get().close();
            }

            @Override
            public void mark(int readLimit) {
                try {
                    in.get().mark(readLimit);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to retrieve the underlying input stream.", e);
                }
            }

            @Override
            public void reset() throws IOException {
                in.get().reset();
            }

            @Override
            public boolean markSupported() {
                try {
                    return in.get().markSupported();
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to retrieve the underlying input stream.", e);
                }
            }
        };
    }

    @Override
    public ClientResponse apply(ClientRequest request) {
        try {
            return _apply(request);
        } catch (IOException ex) {
            throw new ProcessingException(ex);
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
                    callback.failure(new ProcessingException(ex));
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
                ClientProperties.CONNECT_TIMEOUT, uc.getConnectTimeout()));

        uc.setReadTimeout(PropertiesHelper.getValue(configurationProperties,
                ClientProperties.READ_TIMEOUT, uc.getReadTimeout()));

        if (uc instanceof HttpsURLConnection) {
            HttpsURLConnection suc = (HttpsURLConnection) uc;

            final JerseyClient client = request.getClient();
            final HostnameVerifier verifier = client.getHostnameVerifier();
            if (verifier != null) {
                suc.setHostnameVerifier(verifier);
            }
            suc.setSSLSocketFactory(client.getSslContext().getSocketFactory());
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

            request.setStreamProvider(new OutboundMessageContext.StreamProvider() {

                @Override
                public OutputStream getOutputStream(int contentLength) throws IOException {
                    setOutboundHeaders(request.getStringHeaders(), uc);
                    return uc.getOutputStream();
                }
            });
            request.writeEntity();

        } else {
            setOutboundHeaders(request.getStringHeaders(), uc);
        }

        final int code = uc.getResponseCode();
        final String reasonPhrase = uc.getResponseMessage();
        final Response.StatusType status = reasonPhrase == null ?
                Statuses.from(code) : Statuses.from(code, reasonPhrase);
        ClientResponse responseContext = new ClientResponse(
                status, request);
        responseContext.headers(Maps.<String, List<String>>filterKeys(uc.getHeaderFields(), Predicates.notNull()));
        responseContext.setEntityStream(getInputStream(uc));

        return responseContext;
    }

    private void setOutboundHeaders(MultivaluedMap<String, String> headers, HttpURLConnection uc) {
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
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws NoSuchFieldException, IllegalAccessException {
                        final Field methodField = httpURLConnectionClass.getSuperclass().getDeclaredField("method");
                        methodField.setAccessible(true);
                        methodField.set(httpURLConnection, method);
                        return null;
                    }
                });
            } catch (final PrivilegedActionException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "HttpUrlConnection " + AccessController.doPrivileged(PropertiesHelper.getSystemProperty("java.version"));
    }
}
