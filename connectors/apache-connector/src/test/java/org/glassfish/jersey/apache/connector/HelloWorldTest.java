/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.net.ssl.SSLSession;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class HelloWorldTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(HelloWorldTest.class.getName());
    private static final String ROOT_PATH = "helloworld";

    @Path("helloworld")
    public static class HelloWorldResource {

        public static final String CLICHED_MESSAGE = "Hello World!";

        @GET
        @Produces("text/plain")
        public String getHello() {
            return CLICHED_MESSAGE;
        }

        @GET
        @Produces("text/plain")
        @Path("error")
        public Response getError() {
            return Response.serverError().entity("Error.").build();
        }

        @GET
        @Produces("text/plain")
        @Path("error2")
        public Response getError2() {
            return Response.serverError().entity("Error2.").build();
        }

    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(HelloWorldResource.class);
        config.register(new LoggingFeature(LOGGER, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY,
                LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new ApacheConnectorProvider());
    }

    @Test
    public void testConnection() {
        Response response = target().path(ROOT_PATH).request("text/plain").get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testClientStringResponse() {
        String s = target().path(ROOT_PATH).request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
    }

    @Test
    public void testConnectionPoolSharingEnabled() throws Exception {
        _testConnectionPoolSharing(true);
    }

    @Test
    public void testConnectionPoolSharingDisabled() throws Exception {
        _testConnectionPoolSharing(false);
    }

    public void _testConnectionPoolSharing(final boolean sharingEnabled) throws Exception {

        final HttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        final ClientConfig cc = new ClientConfig();
        cc.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        cc.property(ApacheClientProperties.CONNECTION_MANAGER_SHARED, sharingEnabled);
        cc.connectorProvider(new ApacheConnectorProvider());

        final Client clientOne = ClientBuilder.newClient(cc);
        WebTarget target = clientOne.target(getBaseUri()).path(ROOT_PATH);
        target.request().get();
        clientOne.close();

        final boolean exceptionExpected = !sharingEnabled;

        final Client clientTwo = ClientBuilder.newClient(cc);
        target = clientTwo.target(getBaseUri()).path(ROOT_PATH);
        try {
            target.request().get();
            if (exceptionExpected) {
                Assert.fail("Exception expected");
            }
        } catch (Exception e) {
            if (!exceptionExpected) {
                Assert.fail("Exception not expected");
            }
        } finally {
            clientTwo.close();
        }

        if (sharingEnabled) {
            connectionManager.shutdown();
        }
    }

    @Test
    public void testAsyncClientRequests() throws InterruptedException {
        HttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        ClientConfig cc = new ClientConfig();
        cc.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        cc.connectorProvider(new ApacheConnectorProvider());
        Client client = ClientBuilder.newClient(cc);
        WebTarget target = client.target(getBaseUri());
        final int REQUESTS = 20;
        final CountDownLatch latch = new CountDownLatch(REQUESTS);
        final long tic = System.currentTimeMillis();
        final Map<Integer, String> results = new ConcurrentHashMap<Integer, String>();
        for (int i = 0; i < REQUESTS; i++) {
            final int id = i;
            target.path(ROOT_PATH).request().async().get(new InvocationCallback<Response>() {
                @Override
                public void completed(Response response) {
                    try {
                        final String result = response.readEntity(String.class);
                        results.put(id, result);
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void failed(Throwable error) {
                    Logger.getLogger(HelloWorldTest.class.getName()).log(Level.SEVERE, "Failed on throwable", error);
                    results.put(id, "error: " + error.getMessage());
                    latch.countDown();
                }
            });
        }
        assertTrue(latch.await(10 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS));
        final long toc = System.currentTimeMillis();
        Logger.getLogger(HelloWorldTest.class.getName()).info("Executed in: " + (toc - tic));

        StringBuilder resultInfo = new StringBuilder("Results:\n");
        for (int i = 0; i < REQUESTS; i++) {
            String result = results.get(i);
            resultInfo.append(i).append(": ").append(result).append('\n');
        }
        Logger.getLogger(HelloWorldTest.class.getName()).info(resultInfo.toString());

        for (int i = 0; i < REQUESTS; i++) {
            String result = results.get(i);
            assertEquals(HelloWorldResource.CLICHED_MESSAGE, result);
        }
    }

    @Test
    public void testHead() {
        Response response = target().path(ROOT_PATH).request().head();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
    }

    @Test
    public void testFooBarOptions() {
        Response response = target().path(ROOT_PATH).request().header("Accept", "foo/bar").options();
        assertEquals(200, response.getStatus());
        final String allowHeader = response.getHeaderString("Allow");
        _checkAllowContent(allowHeader);
        assertEquals("foo/bar", response.getMediaType().toString());
        assertEquals(0, response.getLength());
    }

    @Test
    public void testTextPlainOptions() {
        Response response = target().path(ROOT_PATH).request().header("Accept", MediaType.TEXT_PLAIN).options();
        assertEquals(200, response.getStatus());
        final String allowHeader = response.getHeaderString("Allow");
        _checkAllowContent(allowHeader);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
        final String responseBody = response.readEntity(String.class);
        _checkAllowContent(responseBody);
    }

    private void _checkAllowContent(final String content) {
        assertTrue(content.contains("GET"));
        assertTrue(content.contains("HEAD"));
        assertTrue(content.contains("OPTIONS"));
    }

    @Test
    public void testMissingResourceNotFound() {
        Response response;

        response = target().path(ROOT_PATH + "arbitrary").request().get();
        assertEquals(404, response.getStatus());
        response.close();

        response = target().path(ROOT_PATH).path("arbitrary").request().get();
        assertEquals(404, response.getStatus());
        response.close();
    }

    @Test
    public void testLoggingFilterClientClass() {
        Client client = client();
        client.register(CustomLoggingFilter.class).property("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target().path(ROOT_PATH).request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testLoggingFilterClientInstance() {
        Client client = client();
        client.register(new CustomLoggingFilter()).property("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target().path(ROOT_PATH).request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testLoggingFilterTargetClass() {
        WebTarget target = target().path(ROOT_PATH);
        target.register(CustomLoggingFilter.class).property("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target.request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testLoggingFilterTargetInstance() {
        WebTarget target = target().path(ROOT_PATH);
        target.register(new CustomLoggingFilter()).property("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target.request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testConfigurationUpdate() {
        Client client1 = client();
        client1.register(CustomLoggingFilter.class).property("foo", "bar");

        Client client = ClientBuilder.newClient(client1.getConfiguration());
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = client.target(getBaseUri()).path(ROOT_PATH).request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    /**
     * JERSEY-2157 reproducer.
     * <p>
     * The test ensures that entities of the error responses which cause
     * WebApplicationException being thrown by a JAX-RS client are buffered
     * and that the underlying input connections are automatically released
     * in such case.
     */
    @Test
    public void testConnectionClosingOnExceptionsForErrorResponses() {
        final BasicClientConnectionManager cm = new BasicClientConnectionManager();
        final AtomicInteger connectionCounter = new AtomicInteger(0);

        final ClientConfig config = new ClientConfig().property(ApacheClientProperties.CONNECTION_MANAGER,
                new ClientConnectionManager() {
                    @Override
                    public SchemeRegistry getSchemeRegistry() {
                        return cm.getSchemeRegistry();
                    }

                    @Override
                    public ClientConnectionRequest requestConnection(final HttpRoute route, final Object state) {
                        connectionCounter.incrementAndGet();

                        final ClientConnectionRequest wrappedRequest = cm.requestConnection(route, state);

                        /**
                         * To explain the following long piece of code:
                         *
                         * All the code does is to just create a wrapper implementations
                         * for the AHC connection management interfaces.
                         *
                         * The only really important piece of code is the
                         * {@link org.apache.http.conn.ManagedClientConnection#releaseConnection()} implementation,
                         * where the connectionCounter is decremented when a managed connection instance
                         * is released by AHC runtime. In our test, this is expected to happen
                         * as soon as the exception is created for an error response
                         * (as the error response entity gets buffered in
                         * {@link org.glassfish.jersey.client.JerseyInvocation#convertToException(javax.ws.rs.core.Response)}).
                         */
                        return new ClientConnectionRequest() {
                            @Override
                            public ManagedClientConnection getConnection(long timeout, TimeUnit tunit)
                                    throws InterruptedException, ConnectionPoolTimeoutException {

                                final ManagedClientConnection wrappedConnection = wrappedRequest.getConnection(timeout, tunit);

                                return new ManagedClientConnection() {
                                    @Override
                                    public boolean isSecure() {
                                        return wrappedConnection.isSecure();
                                    }

                                    @Override
                                    public HttpRoute getRoute() {
                                        return wrappedConnection.getRoute();
                                    }

                                    @Override
                                    public SSLSession getSSLSession() {
                                        return wrappedConnection.getSSLSession();
                                    }

                                    @Override
                                    public void open(HttpRoute route, HttpContext context, HttpParams params) throws IOException {
                                        wrappedConnection.open(route, context, params);
                                    }

                                    @Override
                                    public void tunnelTarget(boolean secure, HttpParams params) throws IOException {
                                        wrappedConnection.tunnelTarget(secure, params);
                                    }

                                    @Override
                                    public void tunnelProxy(HttpHost next, boolean secure, HttpParams params) throws IOException {
                                        wrappedConnection.tunnelProxy(next, secure, params);
                                    }

                                    @Override
                                    public void layerProtocol(HttpContext context, HttpParams params) throws IOException {
                                        wrappedConnection.layerProtocol(context, params);
                                    }

                                    @Override
                                    public void markReusable() {
                                        wrappedConnection.markReusable();
                                    }

                                    @Override
                                    public void unmarkReusable() {
                                        wrappedConnection.unmarkReusable();
                                    }

                                    @Override
                                    public boolean isMarkedReusable() {
                                        return wrappedConnection.isMarkedReusable();
                                    }

                                    @Override
                                    public void setState(Object state) {
                                        wrappedConnection.setState(state);
                                    }

                                    @Override
                                    public Object getState() {
                                        return wrappedConnection.getState();
                                    }

                                    @Override
                                    public void setIdleDuration(long duration, TimeUnit unit) {
                                        wrappedConnection.setIdleDuration(duration, unit);
                                    }

                                    @Override
                                    public boolean isResponseAvailable(int timeout) throws IOException {
                                        return wrappedConnection.isResponseAvailable(timeout);
                                    }

                                    @Override
                                    public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
                                        wrappedConnection.sendRequestHeader(request);
                                    }

                                    @Override
                                    public void sendRequestEntity(HttpEntityEnclosingRequest request)
                                            throws HttpException, IOException {
                                        wrappedConnection.sendRequestEntity(request);
                                    }

                                    @Override
                                    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
                                        return wrappedConnection.receiveResponseHeader();
                                    }

                                    @Override
                                    public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException {
                                        wrappedConnection.receiveResponseEntity(response);
                                    }

                                    @Override
                                    public void flush() throws IOException {
                                        wrappedConnection.flush();
                                    }

                                    @Override
                                    public void close() throws IOException {
                                        wrappedConnection.close();
                                    }

                                    @Override
                                    public boolean isOpen() {
                                        return wrappedConnection.isOpen();
                                    }

                                    @Override
                                    public boolean isStale() {
                                        return wrappedConnection.isStale();
                                    }

                                    @Override
                                    public void setSocketTimeout(int timeout) {
                                        wrappedConnection.setSocketTimeout(timeout);
                                    }

                                    @Override
                                    public int getSocketTimeout() {
                                        return wrappedConnection.getSocketTimeout();
                                    }

                                    @Override
                                    public void shutdown() throws IOException {
                                        wrappedConnection.shutdown();
                                    }

                                    @Override
                                    public HttpConnectionMetrics getMetrics() {
                                        return wrappedConnection.getMetrics();
                                    }

                                    @Override
                                    public InetAddress getLocalAddress() {
                                        return wrappedConnection.getLocalAddress();
                                    }

                                    @Override
                                    public int getLocalPort() {
                                        return wrappedConnection.getLocalPort();
                                    }

                                    @Override
                                    public InetAddress getRemoteAddress() {
                                        return wrappedConnection.getRemoteAddress();
                                    }

                                    @Override
                                    public int getRemotePort() {
                                        return wrappedConnection.getRemotePort();
                                    }

                                    @Override
                                    public void releaseConnection() throws IOException {
                                        connectionCounter.decrementAndGet();
                                        wrappedConnection.releaseConnection();
                                    }

                                    @Override
                                    public void abortConnection() throws IOException {
                                        wrappedConnection.abortConnection();
                                    }

                                    @Override
                                    public String getId() {
                                        return wrappedConnection.getId();
                                    }

                                    @Override
                                    public void bind(Socket socket) throws IOException {
                                        wrappedConnection.bind(socket);
                                    }

                                    @Override
                                    public Socket getSocket() {
                                        return wrappedConnection.getSocket();
                                    }
                                };
                            }

                            @Override
                            public void abortRequest() {
                                wrappedRequest.abortRequest();
                            }
                        };
                    }

                    @Override
                    public void releaseConnection(ManagedClientConnection conn, long keepalive, TimeUnit tunit) {
                        cm.releaseConnection(conn, keepalive, tunit);
                    }

                    @Override
                    public void closeExpiredConnections() {
                        cm.closeExpiredConnections();
                    }

                    @Override
                    public void closeIdleConnections(long idletime, TimeUnit tunit) {
                        cm.closeIdleConnections(idletime, tunit);
                    }

                    @Override
                    public void shutdown() {
                        cm.shutdown();
                    }
                });
        config.connectorProvider(new ApacheConnectorProvider());

        final Client client = ClientBuilder.newClient(config);
        final WebTarget rootTarget = client.target(getBaseUri()).path(ROOT_PATH);

        // Test that connection is getting closed properly for error responses.
        try {
            final String response = rootTarget.path("error").request().get(String.class);
            fail("Exception expected. Received: " + response);
        } catch (InternalServerErrorException isee) {
            // do nothing - connection should be closed properly by now
        }

        // Fail if the previous connection has not been closed automatically.
        assertEquals(0, connectionCounter.get());

        try {
            final String response = rootTarget.path("error2").request().get(String.class);
            fail("Exception expected. Received: " + response);
        } catch (InternalServerErrorException isee) {
            assertEquals("Received unexpected data.", "Error2.", isee.getResponse().readEntity(String.class));
            // Test buffering:
            // second read would fail if entity was not buffered
            assertEquals("Unexpected data in the entity buffer.", "Error2.", isee.getResponse().readEntity(String.class));
        }

        assertEquals(0, connectionCounter.get());
    }
}
