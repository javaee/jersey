/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;

import jersey.repackaged.com.google.common.base.Predicates;
import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.util.concurrent.MoreExecutors;

/**
 * Default client transport connector using {@link HttpURLConnection}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class HttpUrlConnector implements Connector {

    private static final Logger LOGGER = Logger.getLogger(HttpUrlConnector.class.getName());
    private static final String ALLOW_RESTRICTED_HEADERS_SYSTEM_PROPERTY = "sun.net.http.allowRestrictedHeaders";
    // The list of restricted headers is extracted from sun.net.www.protocol.http.HttpURLConnection
    private static final String[] restrictedHeaders = {
            "Access-Control-Request-Headers",
            "Access-Control-Request-Method",
            "Connection", /* close is allowed */
            "Content-Length",
            "Content-Transfer-Encoding",
            "Host",
            "Keep-Alive",
            "Origin",
            "Trailer",
            "Transfer-Encoding",
            "Upgrade",
            "Via"
    };

    private static final Set<String> restrictedHeaderSet = new HashSet<String>(restrictedHeaders.length);

    static {
        for (String headerName : restrictedHeaders) {
            restrictedHeaderSet.add(headerName.toLowerCase());
        }
    }

    private final HttpUrlConnectorProvider.ConnectionFactory connectionFactory;
    private final int chunkSize;
    private final boolean fixLengthStreaming;
    private final boolean setMethodWorkaround;
    private final boolean isRestrictedHeaderPropertySet;
    private final LazyValue<SSLSocketFactory> sslSocketFactory;

    /**
     * Create new {@code HttpUrlConnector} instance.
     *
     * @param client              JAX-RS client instance for which the connector is being created.
     * @param connectionFactory   {@link javax.net.ssl.HttpsURLConnection} factory to be used when creating connections.
     * @param chunkSize           chunk size to use when using HTTP chunked transfer coding.
     * @param fixLengthStreaming  specify if the the {@link java.net.HttpURLConnection#setFixedLengthStreamingMode(int)
     *                            fixed-length streaming mode} on the underlying HTTP URL connection instances should be
     *                            used when sending requests.
     * @param setMethodWorkaround specify if the reflection workaround should be used to set HTTP URL connection method
     *                            name. See {@link HttpUrlConnectorProvider#SET_METHOD_WORKAROUND} for details.
     */
    public HttpUrlConnector(
            final Client client,
            final HttpUrlConnectorProvider.ConnectionFactory connectionFactory,
            final int chunkSize,
            final boolean fixLengthStreaming,
            final boolean setMethodWorkaround) {

        sslSocketFactory = Values.lazy(new Value<SSLSocketFactory>() {
            @Override
            public SSLSocketFactory get() {
                return client.getSslContext().getSocketFactory();
            }
        });

        this.connectionFactory = connectionFactory;
        this.chunkSize = chunkSize;
        this.fixLengthStreaming = fixLengthStreaming;
        this.setMethodWorkaround = setMethodWorkaround;

        // check if sun.net.http.allowRestrictedHeaders system property has been set and log the result
        // the property is being cached in the HttpURLConnection, so this is only informative - there might
        // already be some connection(s), that existed before the property was set/changed.
        isRestrictedHeaderPropertySet = Boolean.valueOf(AccessController.doPrivileged(
                PropertiesHelper.getSystemProperty(ALLOW_RESTRICTED_HEADERS_SYSTEM_PROPERTY, "false")
        ));

        LOGGER.config(isRestrictedHeaderPropertySet
                        ? LocalizationMessages.RESTRICTED_HEADER_PROPERTY_SETTING_TRUE(ALLOW_RESTRICTED_HEADERS_SYSTEM_PROPERTY)
                        : LocalizationMessages.RESTRICTED_HEADER_PROPERTY_SETTING_FALSE(ALLOW_RESTRICTED_HEADERS_SYSTEM_PROPERTY)
        );
    }

    private static InputStream getInputStream(final HttpURLConnection uc) throws IOException {
        return new InputStream() {
            private final UnsafeValue<InputStream, IOException> in = Values.lazy(new UnsafeValue<InputStream, IOException>() {
                @Override
                public InputStream get() throws IOException {
                    if (uc.getResponseCode() < Response.Status.BAD_REQUEST.getStatusCode()) {
                        return uc.getInputStream();
                    } else {
                        InputStream ein = uc.getErrorStream();
                        return (ein != null) ? ein : new ByteArrayInputStream(new byte[0]);
                    }
                }
            });

            private volatile boolean closed = false;

            /**
             * The motivation for this method is to straighten up a behaviour of {@link sun.net.www.http.KeepAliveStream} which
             * is used here as a backing {@link InputStream}. The problem is that its access methods (e.g., {@link
             * sun.net.www.http.KeepAliveStream#read()}) do not throw {@link IOException} if the stream is closed. This behaviour
             * contradicts with {@link InputStream} contract.
             * <p/>
             * This is a part of fix of JERSEY-2878
             * <p/>
             * Note that {@link java.io.FilterInputStream} also changes the contract of
             * {@link java.io.FilterInputStream#read(byte[], int, int)} as it doesn't state that closed stream causes an {@link
             * IOException} which might be questionable. Nevertheless, our contract is {@link InputStream} and as such, the
             * stream we're offering must comply with it.
             *
             * @throws IOException
             */
            private void throwIOExceptionIfClosed() throws IOException {
                if (closed) {
                    throw new IOException("Stream closed");
                }
            }

            @Override
            public int read() throws IOException {
                int result = in.get().read();
                throwIOExceptionIfClosed();
                return result;
            }

            @Override
            public int read(byte[] b) throws IOException {
                int result = in.get().read(b);
                throwIOExceptionIfClosed();
                return result;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int result = in.get().read(b, off, len);
                throwIOExceptionIfClosed();
                return result;
            }

            @Override
            public long skip(long n) throws IOException {
                long result = in.get().skip(n);
                throwIOExceptionIfClosed();
                return result;
            }

            @Override
            public int available() throws IOException {
                int result = in.get().available();
                throwIOExceptionIfClosed();
                return result;
            }

            @Override
            public void close() throws IOException {
                try {
                    in.get().close();
                } finally {
                    closed = true;
                }
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
                throwIOExceptionIfClosed();
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

    /**
     * Secure connection if necessary.
     * <p/>
     * Provided implementation sets {@link HostnameVerifier} and {@link SSLSocketFactory} to give connection, if that
     * is an instance of {@link HttpsURLConnection}.
     *
     * @param client client associated with this client runtime.
     * @param uc     http connection to be secured.
     */
    protected void secureConnection(final JerseyClient client, final HttpURLConnection uc) {
        if (uc instanceof HttpsURLConnection) {
            HttpsURLConnection suc = (HttpsURLConnection) uc;

            final HostnameVerifier verifier = client.getHostnameVerifier();
            if (verifier != null) {
                suc.setHostnameVerifier(verifier);
            }

            if (HttpsURLConnection.getDefaultSSLSocketFactory() == suc.getSSLSocketFactory()) {
                // indicates that the custom socket factory was not set
                suc.setSSLSocketFactory(sslSocketFactory.get());
            }
        }
    }

    private ClientResponse _apply(final ClientRequest request) throws IOException {
        final HttpURLConnection uc;

        uc = this.connectionFactory.getConnection(request.getUri().toURL());
        uc.setDoInput(true);

        final String httpMethod = request.getMethod();
        if (request.resolveProperty(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, setMethodWorkaround)) {
            setRequestMethodViaJreBugWorkaround(uc, httpMethod);
        } else {
            uc.setRequestMethod(httpMethod);
        }

        uc.setInstanceFollowRedirects(request.resolveProperty(ClientProperties.FOLLOW_REDIRECTS, true));

        uc.setConnectTimeout(request.resolveProperty(ClientProperties.CONNECT_TIMEOUT, uc.getConnectTimeout()));

        uc.setReadTimeout(request.resolveProperty(ClientProperties.READ_TIMEOUT, uc.getReadTimeout()));

        secureConnection(request.getClient(), uc);

        final Object entity = request.getEntity();
        if (entity != null) {
            RequestEntityProcessing entityProcessing = request.resolveProperty(
                    ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.class);

            if (entityProcessing == null || entityProcessing != RequestEntityProcessing.BUFFERED) {
                final long length = request.getLengthLong();
                if (fixLengthStreaming && length > 0) {
                    // uc.setFixedLengthStreamingMode(long) was introduced in JDK 1.7 and Jersey client supports 1.6+
                    if ("1.6".equals(Runtime.class.getPackage().getSpecificationVersion())) {
                        uc.setFixedLengthStreamingMode(request.getLength());
                    } else {
                        uc.setFixedLengthStreamingMode(length);
                    }
                } else if (entityProcessing == RequestEntityProcessing.CHUNKED) {
                    uc.setChunkedStreamingMode(chunkSize);
                }
            }
            uc.setDoOutput(true);

            if ("GET".equalsIgnoreCase(httpMethod)) {
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
        final Response.StatusType status =
                reasonPhrase == null ? Statuses.from(code) : Statuses.from(code, reasonPhrase);
        final URI resolvedRequestUri;
        try {
            resolvedRequestUri = uc.getURL().toURI();
        } catch (URISyntaxException e) {
            throw new ProcessingException(e);
        }

        ClientResponse responseContext = new ClientResponse(status, request, resolvedRequestUri);
        responseContext.headers(Maps.filterKeys(uc.getHeaderFields(), Predicates.notNull()));
        responseContext.setEntityStream(getInputStream(uc));

        return responseContext;
    }

    private void setOutboundHeaders(MultivaluedMap<String, String> headers, HttpURLConnection uc) {
        boolean restrictedSent = false;
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            String headerName = header.getKey();
            String headerValue;

            List<String> headerValues = header.getValue();
            if (headerValues.size() == 1) {
                headerValue = headerValues.get(0);
                uc.setRequestProperty(headerName, headerValue);
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
                headerValue = b.toString();
                uc.setRequestProperty(headerName, headerValue);
            }
            // if (at least one) restricted header was added and the allowRestrictedHeaders
            if (!isRestrictedHeaderPropertySet && !restrictedSent) {
                if (isHeaderRestricted(headerName, headerValue)) {
                    restrictedSent = true;
                }
            }
        }
        if (restrictedSent) {
            LOGGER.warning(LocalizationMessages.RESTRICTED_HEADER_POSSIBLY_IGNORED(ALLOW_RESTRICTED_HEADERS_SYSTEM_PROPERTY));
        }
    }

    private boolean isHeaderRestricted(String name, String value) {
        name = name.toLowerCase();
        return name.startsWith("sec-")
                || restrictedHeaderSet.contains(name)
                && !("connection".equalsIgnoreCase(name) && "close".equalsIgnoreCase(value));
    }

    /**
     * Workaround for a bug in {@code HttpURLConnection.setRequestMethod(String)}
     * The implementation of Sun/Oracle is throwing a {@code ProtocolException}
     * when the method is not in the list of the HTTP/1.1 default methods.
     * This means that to use e.g. {@code PROPFIND} and others, we must apply this workaround.
     * <p/>
     * See issue http://java.net/jira/browse/JERSEY-639
     */
    private static void setRequestMethodViaJreBugWorkaround(final HttpURLConnection httpURLConnection,
                                                            final String method) {
        try {
            httpURLConnection.setRequestMethod(method); // Check whether we are running on a buggy JRE
        } catch (final ProtocolException pe) {
            try {
                AccessController
                        .doPrivileged(new PrivilegedExceptionAction<Object>() {
                            @Override
                            public Object run() throws NoSuchFieldException,
                                    IllegalAccessException {
                                try {
                                    httpURLConnection.setRequestMethod(method);
                                    // Check whether we are running on a buggy
                                    // JRE
                                } catch (final ProtocolException pe) {
                                    Class<?> connectionClass = httpURLConnection
                                            .getClass();
                                    try {
                                        final Field delegateField = connectionClass.getDeclaredField("delegate");
                                        delegateField.setAccessible(true);

                                        HttpURLConnection delegateConnection =
                                                (HttpURLConnection) delegateField.get(httpURLConnection);
                                        setRequestMethodViaJreBugWorkaround(delegateConnection, method);
                                    } catch (NoSuchFieldException e) {
                                        // Ignore for now, keep going
                                    } catch (IllegalArgumentException e) {
                                        throw new RuntimeException(e);
                                    } catch (IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                    try {
                                        Field methodField;
                                        while (connectionClass != null) {
                                            try {
                                                methodField = connectionClass
                                                        .getDeclaredField("method");
                                            } catch (NoSuchFieldException e) {
                                                connectionClass = connectionClass
                                                        .getSuperclass();
                                                continue;
                                            }
                                            methodField.setAccessible(true);
                                            methodField.set(httpURLConnection, method);
                                            break;
                                        }
                                    } catch (final Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
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
