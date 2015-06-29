/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdkhttp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import com.google.common.io.ByteStreams;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Jdk Https Server tests.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class JdkHttpsServerTest extends AbstractJdkHttpServerTester {

    private static final String TRUSTSTORE_CLIENT_FILE = "./truststore_client";
    private static final String TRUSTSTORE_CLIENT_PWD = "asdfgh";
    private static final String KEYSTORE_CLIENT_FILE = "./keystore_client";
    private static final String KEYSTORE_CLIENT_PWD = "asdfgh";

    private static final String KEYSTORE_SERVER_FILE = "./keystore_server";
    private static final String KEYSTORE_SERVER_PWD = "asdfgh";
    private static final String TRUSTSTORE_SERVER_FILE = "./truststore_server";
    private static final String TRUSTSTORE_SERVER_PWD = "asdfgh";

    private HttpServer server;
    private final URI httpsUri = UriBuilder.fromUri("https://localhost/").port(getPort()).build();
    private final URI httpUri = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
    private final ResourceConfig rc = new ResourceConfig(TestResource.class);

    @Path("/testHttps")
    public static class TestResource {
        @GET
        public String get() {
            return "test";
        }
    }

    /**
     * Test, that {@link HttpsServer} instance is returned when providing empty SSLContext (but not starting).
     * @throws Exception
     */
    @Test
    public void testCreateHttpsServerNoSslContext() throws Exception {
        server = JdkHttpServerFactory.createHttpServer(httpsUri, rc, null, false);
        assertThat(server, instanceOf(HttpsServer.class));
    }

    /**
     * Test, that exception is thrown when attempting to start a {@link HttpsServer} with empty SSLContext.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testStartHttpServerNoSslContext() throws Exception {
        server = JdkHttpServerFactory.createHttpServer(httpsUri, rc, null, true);
    }

    /**
     * Test, that {@link javax.net.ssl.SSLHandshakeException} is thrown when attepmting to connect to server with client
     * not configured correctly.
     * @throws Exception
     */
    @Test(expected = SSLHandshakeException.class)
    public void testCreateHttpsServerDefaultSslContext() throws Throwable {
        server = JdkHttpServerFactory.createHttpServer(httpsUri, rc, SSLContext.getDefault(), true);
        assertThat(server, instanceOf(HttpsServer.class));

        // access the https server with not configured client
        final Client client = ClientBuilder.newBuilder().newClient();
        try {
            client.target(httpsUri).path("testHttps").request().get(String.class);
        } catch (final ProcessingException e) {
            throw e.getCause();
        }
    }

    /**
     * Test, that {@link HttpsServer} can be manually started even with (empty) SSLContext, but will throw an exception
     * on request.
     * @throws Exception
     */
    @Test(expected = IOException.class)
    public void testHttpsServerNoSslContextDelayedStart() throws Throwable {
        server = JdkHttpServerFactory.createHttpServer(httpsUri, rc, null, false);
        assertThat(server, instanceOf(HttpsServer.class));
        server.start();

        final Client client = ClientBuilder.newBuilder().newClient();
        try {
            client.target(httpsUri).path("testHttps").request().get(String.class);
        } catch (final ProcessingException e) {
            throw e.getCause();
        }
    }

    /**
     * Test, that {@link HttpsServer} cannot be configured with {@link HttpsConfigurator} after it has started.
     * @throws Exception
     */
    @Test(expected = IllegalStateException.class)
    public void testConfigureSslContextAfterStart() throws Throwable {
        server = JdkHttpServerFactory.createHttpServer(httpsUri, rc, null, false);
        assertThat(server, instanceOf(HttpsServer.class));
        server.start();
        ((HttpsServer) server).setHttpsConfigurator(new HttpsConfigurator(getServerSslContext()));
    }

    /**
     * Tests a client to server roundtrip with correctly configured SSL on both sides.
     * @throws IOException
     */
    @Test
    public void testCreateHttpsServerRoundTrip() throws IOException {
        final SSLContext serverSslContext = getServerSslContext();

        server = JdkHttpServerFactory.createHttpServer(httpsUri, rc, serverSslContext, true);

        final SSLContext foundContext = ((HttpsServer) server).getHttpsConfigurator().getSSLContext();
        assertEquals(serverSslContext, foundContext);

        final SSLContext clientSslContext = getClientSslContext();
        final Client client = ClientBuilder.newBuilder().sslContext(clientSslContext).build();
        final String response = client.target(httpsUri).path("testHttps").request().get(String.class);

        assertEquals("test", response);
    }

    /**
     * Test, that if URI uses http scheme instead of https, SSLContext is ignored.
     * @throws IOException
     */
    @Test
    public void testHttpWithSsl() throws IOException {
        server = JdkHttpServerFactory.createHttpServer(httpUri, rc, getServerSslContext(), true);
        assertThat(server, instanceOf(HttpServer.class));
        assertThat(server, not(instanceOf(HttpsServer.class)));
    }

    private SSLContext getClientSslContext() throws IOException {
        final InputStream trustStore = JdkHttpsServerTest.class.getResourceAsStream(TRUSTSTORE_CLIENT_FILE);
        final InputStream keyStore = JdkHttpsServerTest.class.getResourceAsStream(KEYSTORE_CLIENT_FILE);


        final SslConfigurator sslConfigClient = SslConfigurator.newInstance()
                .trustStoreBytes(ByteStreams.toByteArray(trustStore))
                .trustStorePassword(TRUSTSTORE_CLIENT_PWD)
                .keyStoreBytes(ByteStreams.toByteArray(keyStore))
                .keyPassword(KEYSTORE_CLIENT_PWD);

        return sslConfigClient.createSSLContext();
    }

    private SSLContext getServerSslContext() throws IOException {
        final InputStream trustStore = JdkHttpsServerTest.class.getResourceAsStream(TRUSTSTORE_SERVER_FILE);
        final InputStream keyStore = JdkHttpsServerTest.class.getResourceAsStream(KEYSTORE_SERVER_FILE);

        final SslConfigurator sslConfigServer = SslConfigurator.newInstance()
                .keyStoreBytes(ByteStreams.toByteArray(keyStore))
                .keyPassword(KEYSTORE_SERVER_PWD)
                .trustStoreBytes(ByteStreams.toByteArray(trustStore))
                .trustStorePassword(TRUSTSTORE_SERVER_PWD);

        return sslConfigServer.createSSLContext();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
