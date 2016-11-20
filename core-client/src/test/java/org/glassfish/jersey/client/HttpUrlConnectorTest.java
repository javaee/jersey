/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import org.glassfish.jersey.client.internal.HttpUrlConnector;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Various tests for the default client connector.
 *
 * @author Carlo Pellegrini
 * @author Miroslav Fuksa
 * @author Marek Potociar
 * @author Jakub Podlesak
 */
public class HttpUrlConnectorTest {

    // there is about 30 ms overhead on my laptop, 500 ms should be safe
    private final int TimeoutBASE = 500;

    /**
     * Reproducer for JERSEY-1984.
     * TODO: fix and re-enable the test, it could give java.net.NoRouteToHostException in certain environments instead of timeout exception
     */
    @Test
    @Ignore
    public void testConnectionTimeoutWithEntity() {
        _testInvocationTimeout(createNonRoutableTarget().request().buildPost(Entity.text("does not matter")));
    }

    /**
     * Additional test case for JERSEY-1984 to ensure, that the error occurs only when sending an entity.
     * TODO: see above, rewrite server part, the "non-routable" target solution is fragile
     */
    @Test
    @Ignore
    public void testConnectionTimeoutNoEntity() {
        _testInvocationTimeout(createNonRoutableTarget().request().buildGet());
    }

    /**
     * Reproducer for JERSEY-1611.
     */
    @Test
    public void testResolvedRequestUri() {
        HttpUrlConnectorProvider.ConnectionFactory factory = new HttpUrlConnectorProvider.ConnectionFactory() {
            @Override
            public HttpURLConnection getConnection(URL endpointUrl) throws IOException {
                HttpURLConnection result = (HttpURLConnection) endpointUrl.openConnection();
                return wrapRedirectedHttp(result);
            }
        };
        JerseyClient client = new JerseyClientBuilder().build();

        ClientRequest request = client.target("http://localhost:8080").request().buildGet().request();
        final HttpUrlConnectorProvider connectorProvider = new HttpUrlConnectorProvider().connectionFactory(factory);
        HttpUrlConnector connector = (HttpUrlConnector) connectorProvider.getConnector(client, client.getConfiguration());
        ClientResponse res = connector.apply(request);
        assertEquals(URI.create("http://localhost:8080"), res.getRequestContext().getUri());
        assertEquals(URI.create("http://redirected.org:8080/redirected"), res.getResolvedRequestUri());


        res.getHeaders().putSingle(HttpHeaders.LINK, Link.fromPath("action").rel("test").build().toString());
        assertEquals(URI.create("http://redirected.org:8080/action"), res.getLink("test").getUri());
    }

    private HttpURLConnection wrapRedirectedHttp(final HttpURLConnection connection) {
        if (connection instanceof HttpsURLConnection) {
            return connection;
        }

        return new HttpURLConnection(connection.getURL()) {

            @Override
            public URL getURL() {
                return url;
            }

            @Override
            public int getResponseCode() throws IOException {
                // Pretend we redirected for testRedirection
                url = new URL("http://redirected.org:8080/redirected");
                // and fake the status code to prevent actual connection
                return Response.Status.NO_CONTENT.getStatusCode();
            }

            @Override
            public String getResponseMessage() throws IOException {
                return Response.Status.NO_CONTENT.getReasonPhrase();
            }

            // Ignored for compatibility on java6
            // @Override
            public long getContentLengthLong() {
                return connection.getContentLength();
                // Ignored for compatibility on java6
                // return delegate.getContentLengthLong();
            }

            // Ignored for compatibility on java6
            // @Override
            public long getHeaderFieldLong(String name, long Default) {
                return connection.getHeaderFieldInt(name, (int) Default);
                // Ignored for compatibility on java6
                // return delegate.getHeaderFieldLong(name, Default);
            }

            // Ignored for compatibility on java6
            // @Override
            public void setFixedLengthStreamingMode(long contentLength) {
                // Ignored for compatibility on java6
                // delegate.setFixedLengthStreamingMode(contentLength);
            }

            @Override
            public void setInstanceFollowRedirects(boolean followRedirects) {
                connection.setInstanceFollowRedirects(followRedirects);
            }

            @Override
            public boolean getInstanceFollowRedirects() {
                return connection.getInstanceFollowRedirects();
            }

            @Override
            public void setRequestMethod(String method) throws ProtocolException {
                connection.setRequestMethod(method);
            }

            @Override
            public String getRequestMethod() {
                return connection.getRequestMethod();
            }

            @Override
            public long getHeaderFieldDate(String name, long Default) {
                return connection.getHeaderFieldDate(name, Default);
            }

            @Override
            public void disconnect() {
                connection.disconnect();
            }

            @Override
            public boolean usingProxy() {
                return connection.usingProxy();
            }

            @Override
            public Permission getPermission() throws IOException {
                return connection.getPermission();
            }

            @Override
            public InputStream getErrorStream() {
                return connection.getErrorStream();
            }

            @Override
            public String getHeaderField(int n) {
                return connection.getHeaderField(n);
            }

            @Override
            public void setChunkedStreamingMode(int chunklen) {
                connection.setChunkedStreamingMode(chunklen);
            }

            @Override
            public void setFixedLengthStreamingMode(int contentLength) {
                connection.setFixedLengthStreamingMode(contentLength);
            }

            @Override
            public String getHeaderFieldKey(int n) {
                return connection.getHeaderFieldKey(n);
            }

            @Override
            public void connect() throws IOException {
                connection.connect();
            }

            @Override
            public void setConnectTimeout(int timeout) {
                connection.setConnectTimeout(timeout);
            }

            @Override
            public int getConnectTimeout() {
                return connection.getConnectTimeout();
            }

            @Override
            public void setReadTimeout(int timeout) {
                connection.setReadTimeout(timeout);
            }

            @Override
            public int getReadTimeout() {
                return connection.getReadTimeout();
            }

            @Override
            public int getContentLength() {
                return connection.getContentLength();
            }

            @Override
            public String getContentType() {
                return connection.getContentType();
            }

            @Override
            public String getContentEncoding() {
                return connection.getContentEncoding();
            }

            @Override
            public long getExpiration() {
                return connection.getExpiration();
            }

            @Override
            public long getDate() {
                return connection.getDate();
            }

            @Override
            public long getLastModified() {
                return connection.getLastModified();
            }

            @Override
            public String getHeaderField(String name) {
                return connection.getHeaderField(name);
            }

            @Override
            public Map<String, List<String>> getHeaderFields() {
                return connection.getHeaderFields();
            }

            @Override
            public int getHeaderFieldInt(String name, int Default) {
                return connection.getHeaderFieldInt(name, Default);
            }

            @Override
            public Object getContent() throws IOException {
                return connection.getContent();
            }

            @Override
            public Object getContent(Class[] classes) throws IOException {
                return connection.getContent(classes);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return connection.getInputStream();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return connection.getOutputStream();
            }

            @Override
            public String toString() {
                return connection.toString();
            }

            @Override
            public void setDoInput(boolean doinput) {
                connection.setDoInput(doinput);
            }

            @Override
            public boolean getDoInput() {
                return connection.getDoInput();
            }

            @Override
            public void setDoOutput(boolean dooutput) {
                connection.setDoOutput(dooutput);
            }

            @Override
            public boolean getDoOutput() {
                return connection.getDoOutput();
            }

            @Override
            public void setAllowUserInteraction(boolean allowuserinteraction) {
                connection.setAllowUserInteraction(allowuserinteraction);
            }

            @Override
            public boolean getAllowUserInteraction() {
                return connection.getAllowUserInteraction();
            }

            @Override
            public void setUseCaches(boolean usecaches) {
                connection.setUseCaches(usecaches);
            }

            @Override
            public boolean getUseCaches() {
                return connection.getUseCaches();
            }

            @Override
            public void setIfModifiedSince(long ifmodifiedsince) {
                connection.setIfModifiedSince(ifmodifiedsince);
            }

            @Override
            public long getIfModifiedSince() {
                return connection.getIfModifiedSince();
            }

            @Override
            public boolean getDefaultUseCaches() {
                return connection.getDefaultUseCaches();
            }

            @Override
            public void setDefaultUseCaches(boolean defaultusecaches) {
                connection.setDefaultUseCaches(defaultusecaches);
            }

            @Override
            public void setRequestProperty(String key, String value) {
                connection.setRequestProperty(key, value);
            }

            @Override
            public void addRequestProperty(String key, String value) {
                connection.addRequestProperty(key, value);
            }

            @Override
            public String getRequestProperty(String key) {
                return connection.getRequestProperty(key);
            }

            @Override
            public Map<String, List<String>> getRequestProperties() {
                return connection.getRequestProperties();
            }
        };

    }

    private void _testInvocationTimeout(Invocation invocation) {

        final long start = System.currentTimeMillis();

        try {
            invocation.invoke();

            Assert.fail("Timeout expected!");

        } catch (Exception ex) {

            Assert.assertTrue(String.format("Bad exception, %s, caught! Timeout expected.", ex.getCause()),
                    ex.getCause() instanceof SocketTimeoutException);

            final long stop = System.currentTimeMillis();
            long time = stop - start;

            Assert.assertTrue(
               String.format(
                    "Actual time, %d ms, should not be more than twice as longer as the original timeout, %d ms",
                                 time,                                                              TimeoutBASE),
               time < 2 * TimeoutBASE);
        }
    }

    /**
     * Test SSL connection.
     */
    @Test
    public void testSSLConnection() {
        JerseyClient client = new JerseyClientBuilder().build();
        ClientRequest request = client.target("https://localhost:8080").request().buildGet().request();
        HttpUrlConnectorProvider.ConnectionFactory factory = new HttpUrlConnectorProvider.ConnectionFactory() {
            @Override
            public HttpURLConnection getConnection(URL endpointUrl) throws IOException {
                HttpURLConnection result = (HttpURLConnection) endpointUrl.openConnection();
                return wrapNoContentHttps(result);
            }
        };
        final HttpUrlConnectorProvider connectorProvider = new HttpUrlConnectorProvider().connectionFactory(factory);
        HttpUrlConnector connector = (HttpUrlConnector) connectorProvider.getConnector(client, client.getConfiguration());
        ClientResponse res = connector.apply(request);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatusInfo().getStatusCode());
        assertEquals(Response.Status.NO_CONTENT.getReasonPhrase(), res.getStatusInfo().getReasonPhrase());
    }

    private HttpURLConnection wrapNoContentHttps(final HttpURLConnection result) {
        if (result instanceof HttpsURLConnection) {
            return new HttpsURLConnection(result.getURL()) {
                private final HttpsURLConnection delegate = (HttpsURLConnection) result;

                @Override
                public int getResponseCode() throws IOException {
                    return Response.Status.NO_CONTENT.getStatusCode();
                }

                @Override
                public String getResponseMessage() throws IOException {
                    return Response.Status.NO_CONTENT.getReasonPhrase();
                }

                // Ignored for compatibility on java6
                // @Override
                public long getContentLengthLong() {
                    return delegate.getContentLength();
                    // Ignored for compatibility on java6
                    // return delegate.getContentLengthLong();
                }

                // Ignored for compatibility on java6
                // @Override
                public long getHeaderFieldLong(String name, long Default) {
                    return delegate.getHeaderFieldInt(name, (int) Default);
                    // Ignored for compatibility on java6
                    // return delegate.getHeaderFieldLong(name, Default);
                }

                // Ignored for compatibility on java6
                // @Override
                public void setFixedLengthStreamingMode(long contentLength) {
                    // Ignored for compatibility on java6
                    // delegate.setFixedLengthStreamingMode(contentLength);
                }

                @Override
                public String getHeaderFieldKey(int n) {
                    return delegate.getHeaderFieldKey(n);
                }

                @Override
                public String getHeaderField(int n) {
                    return delegate.getHeaderField(n);
                }

                @Override
                public void connect() throws IOException {
                    delegate.connect();
                }

                @Override
                public boolean getInstanceFollowRedirects() {
                    return delegate.getInstanceFollowRedirects();
                }

                @Override
                public int getConnectTimeout() {
                    return delegate.getConnectTimeout();
                }

                @Override
                public int getContentLength() {
                    return delegate.getContentLength();
                }

                @Override
                public String getContentType() {
                    return delegate.getContentType();
                }

                @Override
                public long getHeaderFieldDate(String name, long Default) {
                    return delegate.getHeaderFieldDate(name, Default);
                }

                @Override
                public String getContentEncoding() {
                    return delegate.getContentEncoding();
                }

                @Override
                public void disconnect() {
                    delegate.disconnect();
                }

                @Override
                public long getExpiration() {
                    return delegate.getExpiration();
                }

                @Override
                public long getDate() {
                    return delegate.getDate();
                }

                @Override
                public InputStream getErrorStream() {
                    return delegate.getErrorStream();
                }

                @Override
                public long getLastModified() {
                    return delegate.getLastModified();
                }

                @Override
                public String getHeaderField(String name) {
                    return delegate.getHeaderField(name);
                }

                @Override
                public Map<String, List<String>> getHeaderFields() {
                    return delegate.getHeaderFields();
                }

                @Override
                public int getHeaderFieldInt(String name, int Default) {
                    return delegate.getHeaderFieldInt(name, Default);
                }

                @Override
                public Object getContent() throws IOException {
                    return delegate.getContent();
                }

                @Override
                public Object getContent(@SuppressWarnings("rawtypes") Class[] classes) throws IOException {
                    return delegate.getContent(classes);
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return delegate.getInputStream();
                }

                @Override
                public boolean getDoInput() {
                    return delegate.getDoInput();
                }

                @Override
                public boolean getDoOutput() {
                    return delegate.getDoOutput();
                }

                @Override
                public boolean getAllowUserInteraction() {
                    return delegate.getAllowUserInteraction();
                }

                @Override
                public void addRequestProperty(String key, String value) {
                    delegate.addRequestProperty(key, value);
                }

                @Override
                public String getCipherSuite() {
                    return delegate.getCipherSuite();
                }

                @Override
                public boolean getDefaultUseCaches() {
                    return delegate.getDefaultUseCaches();
                }

                @Override
                public HostnameVerifier getHostnameVerifier() {
                    return delegate.getHostnameVerifier();
                }

                @Override
                public long getIfModifiedSince() {
                    return delegate.getIfModifiedSince();
                }

                @Override
                public Certificate[] getLocalCertificates() {
                    return delegate.getLocalCertificates();
                }

                @Override
                public Principal getLocalPrincipal() {
                    return delegate.getLocalPrincipal();
                }

                @Override
                public void setFixedLengthStreamingMode(int contentLength) {
                    delegate.setFixedLengthStreamingMode(contentLength);
                }

                @Override
                public void setChunkedStreamingMode(int chunklen) {
                    delegate.setChunkedStreamingMode(chunklen);
                }

                @Override
                public void setConnectTimeout(int timeout) {
                    delegate.setConnectTimeout(timeout);
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    return delegate.getOutputStream();
                }

                @Override
                public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
                    return delegate.getPeerPrincipal();
                }

                @Override
                public void setInstanceFollowRedirects(boolean followRedirects) {
                    delegate.setInstanceFollowRedirects(followRedirects);
                }

                @Override
                public void setRequestMethod(String method) throws ProtocolException {
                    delegate.setRequestMethod(method);
                }

                @Override
                public String getRequestMethod() {
                    return delegate.getRequestMethod();
                }

                @Override
                public void setReadTimeout(int timeout) {
                    delegate.setReadTimeout(timeout);
                }

                @Override
                public int getReadTimeout() {
                    return delegate.getReadTimeout();
                }

                @Override
                public URL getURL() {
                    return delegate.getURL();
                }

                @Override
                public boolean usingProxy() {
                    return delegate.usingProxy();
                }

                @Override
                public Permission getPermission() throws IOException {
                    return delegate.getPermission();
                }

                @Override
                public void setDoInput(boolean doinput) {
                    delegate.setDoInput(doinput);
                }

                @Override
                public void setDoOutput(boolean dooutput) {
                    delegate.setDoOutput(dooutput);
                }

                @Override
                public void setAllowUserInteraction(boolean allowuserinteraction) {
                    delegate.setAllowUserInteraction(allowuserinteraction);
                }

                @Override
                public void setUseCaches(boolean usecaches) {
                    delegate.setUseCaches(usecaches);
                }

                @Override
                public boolean getUseCaches() {
                    return delegate.getUseCaches();
                }

                @Override
                public void setIfModifiedSince(long ifmodifiedsince) {
                    delegate.setIfModifiedSince(ifmodifiedsince);
                }

                @Override
                public void setDefaultUseCaches(boolean defaultusecaches) {
                    delegate.setDefaultUseCaches(defaultusecaches);
                }

                @Override
                public String getRequestProperty(String key) {
                    return delegate.getRequestProperty(key);
                }

                @Override
                public Map<String, List<String>> getRequestProperties() {
                    return delegate.getRequestProperties();
                }

                @Override
                public SSLSocketFactory getSSLSocketFactory() {
                    return delegate.getSSLSocketFactory();
                }

                @Override
                public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
                    return delegate.getServerCertificates();
                }

                @Override
                public void setHostnameVerifier(HostnameVerifier v) {
                    delegate.setHostnameVerifier(v);
                }

                @Override
                public void setRequestProperty(String key, String value) {
                    delegate.setRequestProperty(key, value);
                }

                @Override
                public void setSSLSocketFactory(SSLSocketFactory sf) {
                    delegate.setSSLSocketFactory(sf);
                }
            };
        }
        return result;
    }

    private WebTarget createNonRoutableTarget() {
        Client client = ClientBuilder.newClient();
        client.property(ClientProperties.CONNECT_TIMEOUT, TimeoutBASE);
        // the following address should not be routable, connections will timeout
        return client.target("http://10.255.255.254/");
    }
}
