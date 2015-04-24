/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.client.connector.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.io.ByteStreams;

/**
 * SSL connector tests.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@RunWith(Parameterized.class)
public class SslConnectorConfigurationTest {

    private static final String CLIENT_TRUST_STORE = "truststore_client";
    private static final String CLIENT_KEY_STORE = "keystore_client";

    /**
     * Test parameters provider.
     *
     * @return test parameters.
     */
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> testData() {
        return Arrays.asList(new Object[][] {
                {new HttpUrlConnectorProvider()},
                {new GrizzlyConnectorProvider()},
                {new JettyConnectorProvider()},
                {new ApacheConnectorProvider()}
        });
    }

    @Parameterized.Parameter(0)
    public ConnectorProvider connectorProvider;

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

    private static SSLContext getSslContext() throws IOException {
        final InputStream trustStore = SslConnectorConfigurationTest.class.getResourceAsStream(CLIENT_TRUST_STORE);
        final InputStream keyStore = SslConnectorConfigurationTest.class.getResourceAsStream(CLIENT_KEY_STORE);
        return SslConfigurator.newInstance()
                .trustStoreBytes(ByteStreams.toByteArray(trustStore))
                .trustStorePassword("asdfgh")
                .keyStoreBytes(ByteStreams.toByteArray(keyStore))
                .keyPassword("asdfgh")
                .createSSLContext();
    }

    /**
     * Test to see that the correct Http status is returned.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testSSLWithAuth() throws Exception {
        final SSLContext sslContext = getSslContext();

        final ClientConfig cc = new ClientConfig().connectorProvider(connectorProvider);
        final Client client = ClientBuilder.newBuilder()
                .withConfig(cc)
                .sslContext(sslContext)
                .build();

        // client basic auth demonstration
        client.register(HttpAuthenticationFeature.basic("user", "password"));
        final WebTarget target = client.target(Server.BASE_URI).register(new LoggingFilter());

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(200, response.getStatus());
    }

    /**
     * Test to see that HTTP 401 is returned when client tries to GET without
     * proper credentials.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testHTTPBasicAuth1() throws Exception {
        final SSLContext sslContext = getSslContext();

        final ClientConfig cc = new ClientConfig().connectorProvider(new ApacheConnectorProvider());
        final Client client = ClientBuilder.newBuilder()
                .withConfig(cc)
                .sslContext(sslContext)
                .build();

        final WebTarget target = client.target(Server.BASE_URI).register(new LoggingFilter());

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(401, response.getStatus());
    }

    /**
     * Test to see that SSLHandshakeException is thrown when client don't have
     * trusted key.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testSSLAuth1() throws Exception {
        final SSLContext sslContext = getSslContext();

        final ClientConfig cc = new ClientConfig().connectorProvider(new ApacheConnectorProvider());
        final Client client = ClientBuilder.newBuilder()
                .withConfig(cc)
                .sslContext(sslContext)
                .build();

        WebTarget target = client.target(Server.BASE_URI).register(new LoggingFilter());

        boolean caught = false;
        try {
            target.path("/").request().get(String.class);
        } catch (Exception e) {
            caught = true;
        }

        assertTrue(caught);
    }
}
