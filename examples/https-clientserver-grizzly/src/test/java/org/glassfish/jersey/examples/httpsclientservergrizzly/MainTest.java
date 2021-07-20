/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.httpsclientservergrizzly;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class starts the grizzly server and then client performs several SSL (https)
 * requests where different scenarios are tested (SSL Client authentication, missing truststore
 * configuration, etc.). Server is a Grizzly server configured for SSL support and client
 * uses both, {@link HttpUrlConnectorProvider} and {@link GrizzlyConnectorProvider}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class MainTest {

    private static final String TRUSTORE_CLIENT_FILE = "./truststore_client";
    private static final String TRUSTSTORE_CLIENT_PWD = "asdfgh";
    private static final String KEYSTORE_CLIENT_FILE = "./keystore_client";
    private static final String KEYSTORE_CLIENT_PWD = "asdfgh";

    private final Object serverGuard = new Object();
    private Server server = null;

    @Before
    public void setUp() throws Exception {
        synchronized (serverGuard) {
            if (server != null) {
                throw new IllegalStateException(
                        "Test run sync issue: Another instance of the SSL-secured HTTP test server has been already started.");
            }
            server = Server.start();
        }
    }

    @After
    public void tearDown() throws Exception {
        synchronized (serverGuard) {
            if (server == null) {
                throw new IllegalStateException("Test run sync issue: There is no SSL-secured HTTP test server to stop.");
            }
            server.stop();
            server = null;
        }
    }

    @Test
    public void testSSLWithBasicAndSSLAuthGrizzlyConnector() {
        final ClientConfig clientConfig = getGrizzlyConfig();
        _testSSLWithBasicAndSSLAuth(clientConfig);
    }

    private ClientConfig getGrizzlyConfig() {
        return new ClientConfig().connectorProvider(new GrizzlyConnectorProvider());
    }

    @Test
    public void testSSLWithBasicAndSSLAuthHttpUrlConnector() {
        final ClientConfig clientConfig = getHttpUrlConnectorConfig();
        _testSSLWithBasicAndSSLAuth(clientConfig);
    }

    private ClientConfig getHttpUrlConnectorConfig() {
        return new ClientConfig().connectorProvider(new HttpUrlConnectorProvider());
    }

    /**
     * Test to see that the correct Http status is returned.
     */
    private void _testSSLWithBasicAndSSLAuth(ClientConfig clientConfig) {
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreFile(TRUSTORE_CLIENT_FILE)
                .trustStorePassword(TRUSTSTORE_CLIENT_PWD)
                .keyStoreFile(KEYSTORE_CLIENT_FILE)
                .keyPassword(KEYSTORE_CLIENT_PWD);

        final SSLContext sslContext = sslConfig.createSSLContext();
        Client client = ClientBuilder.newBuilder().withConfig(clientConfig)
                .sslContext(sslContext).build();

        // client basic auth demonstration
        client.register(HttpAuthenticationFeature.basic("user", "password"));

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(200, response.getStatus());
    }


    @Test
    public void testWithoutBasicAuthHttpUrlConnector() {
        _testWithoutBasicAuth(getHttpUrlConnectorConfig());
    }

    @Test
    public void testWithoutBasicAuthGrizzlyConnector() {
        _testWithoutBasicAuth(getGrizzlyConfig());
    }

    /**
     * Test to see that HTTP 401 is returned when client tries to GET without
     * proper credentials.
     */
    private void _testWithoutBasicAuth(ClientConfig clientConfig) {
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreFile(TRUSTORE_CLIENT_FILE)
                .trustStorePassword(TRUSTSTORE_CLIENT_PWD)
                .keyStoreFile(KEYSTORE_CLIENT_FILE)
                .keyPassword(KEYSTORE_CLIENT_PWD);

        Client client = ClientBuilder.newBuilder().withConfig(clientConfig).sslContext(sslConfig
                .createSSLContext()).build();

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);
        target.register(LoggingFeature.class);

        Response response;

        try {
            response = target.path("/").request().get(Response.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        assertEquals(401, response.getStatus());
    }

    /**
     * Test to see that SSLHandshakeException is thrown when client don't have
     * trusted key.
     */
    private void _testWithoutSSLAuthentication(ClientConfig clientConfig) {
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreFile(TRUSTORE_CLIENT_FILE)
                .trustStorePassword(TRUSTSTORE_CLIENT_PWD);

        Client client = ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .sslContext(sslConfig.createSSLContext()).build();

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);
        target.register(LoggingFeature.class);

        boolean caught = false;

        try {
            target.path("/").request().get(String.class);
        } catch (Exception e) {
            caught = true;
        }

        assertTrue(caught);
        // solaris throws java.net.SocketException instead of SSLHandshakeException
        // assertTrue(msg.contains("SSLHandshakeException"));
    }

    @Test
    public void testWithoutSSLAuthenticationGrizzly() {
        _testWithoutSSLAuthentication(getGrizzlyConfig());
    }

    @Test
    public void testWithoutSSLAuthenticationHttpUrlConnector() {
        _testWithoutSSLAuthentication(getHttpUrlConnectorConfig());
    }
}
