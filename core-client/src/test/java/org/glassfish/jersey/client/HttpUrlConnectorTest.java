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
package org.glassfish.jersey.client;

import static org.junit.Assert.assertEquals;

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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

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

    @Test
    @Ignore
    // Does not seem to work on all operating systems - sometimes NoRouteToHostException is thrown
    // rather than the expected SocketTimeoutException
    public void testConnectionTimeoutNoEntity() {
        _testInvocationTimeout(createNonRoutableTarget().request().buildGet());
    }

    // reproducer for JERSEY-1984
    @Ignore
    @Test
    public void testConnectionTimeoutWithEntity() {
        _testInvocationTimeout(createNonRoutableTarget().request().buildPost(Entity.text("does not matter")));
    }

    @Test
    public void testRedirection() throws Exception {
        JerseyClient client = (JerseyClient) ClientBuilder.newClient();
        ClientRequest request = client.target("https://localhost:8080/").request().buildGet().request();
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
        assertEquals(URI.create("https://localhost:8080/"), res.getRequestContext().getUri());
        assertEquals(URI.create("https://localhost:8080/redirected/here"), res.getUri());
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

    @Test
    public void testSSLConnection() {
        JerseyClient client = (JerseyClient) ClientBuilder.newClient();
        ClientRequest request = client.target("https://localhost:8080/").request().buildGet().request();
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

    protected HttpURLConnection wrapNoContentHttps(final HttpURLConnection result) {
        if (result instanceof HttpsURLConnection) {
            return new HttpsURLConnection(result.getURL()) {
                private final HttpsURLConnection delegate = (HttpsURLConnection) result;

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

                public long getContentLengthLong() {
                    return delegate.getContentLength();
                    // java6 compatibility
                    // return delegate.getContentLengthLong();
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

                public long getHeaderFieldLong(String name, long Default) {
                    return delegate.getHeaderFieldInt(name, (int) Default);
                    // java6 compatibility
                    // return delegate.getHeaderFieldLong(name, Default);
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

                public void setFixedLengthStreamingMode(long contentLength) {
                    // Ignored for compatibility on java6
                    // delegate.setFixedLengthStreamingMode(contentLength);
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
                public int getResponseCode() throws IOException {
                    // Pretend we redirected for testRedirection
                    url = new URL("https://localhost:8080/redirected/here");
                    // and fake the status code to prevent actual connection
                    return Response.Status.NO_CONTENT.getStatusCode();
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
                    // used by testRedirection
                    return url;
                }

                @Override
                public String getResponseMessage() throws IOException {
                    return Response.Status.NO_CONTENT.getReasonPhrase();
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
