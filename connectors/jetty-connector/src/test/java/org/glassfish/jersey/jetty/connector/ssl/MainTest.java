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
package org.glassfish.jersey.jetty.connector.ssl;

import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jetty.connector.JettyClientProperties;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.io.ByteStreams;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class MainTest {

    @Before
    public void setUp() throws Exception {
        Server.startServer();
    }

    @After
    public void tearDown() throws Exception {
        Server.stopServer();
    }

    /**
     * Test to see that the correct Http status is returned.
     */
    @Test
    public void testSSLWithAuth() throws Exception {
        final InputStream trustStore = MainTest.class.getResourceAsStream("/truststore_client");
        final InputStream keyStore = MainTest.class.getResourceAsStream("/keystore_client");
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreBytes(ByteStreams.toByteArray(trustStore))
                .trustStorePassword("asdfgh")
                .keyStoreBytes(ByteStreams.toByteArray(keyStore))
                .keyPassword("asdfgh");

        ClientConfig config = new ClientConfig();
        config.property(JettyClientProperties.SSL_CONFIG, sslConfig);
        config.connectorProvider(new JettyConnectorProvider());

        Client client = ClientBuilder.newClient(config);

        // client basic auth demonstration
        client.register(new HttpBasicAuthFilter("user", "password"));

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);
        target.register(new LoggingFilter());

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(200, response.getStatus());

        client.close();
    }

    /**
     * Test to see that HTTP 401 is returned when client tries to GET without
     * proper credentials.
     */
    @Test
    public void testHTTPBasicAuth1() throws Exception {
        final InputStream trustStore = MainTest.class.getResourceAsStream("/truststore_client");
        final InputStream keyStore = MainTest.class.getResourceAsStream("/keystore_client");
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreBytes(ByteStreams.toByteArray(trustStore))
                .trustStorePassword("asdfgh")
                .keyStoreBytes(ByteStreams.toByteArray(keyStore))
                .keyPassword("asdfgh");


        ClientConfig cc = new ClientConfig();
        cc.property(JettyClientProperties.SSL_CONFIG, sslConfig);
        cc.connectorProvider(new JettyConnectorProvider());

        Client client = ClientBuilder.newClient(cc);

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);
        target.register(new LoggingFilter());

        Response response;

        response = target.path("/").request().get(Response.class);

        assertEquals(401, response.getStatus());

        client.close();
    }

    /**
     * Test to see that SSLHandshakeException is thrown when client don't have
     * trusted key. Jetty throws javax.ws.rs.NotAuthorizedException: HTTP 401 Unauthorized.
     */
    @Test
    public void testSSLAuth1() throws Exception {
        final InputStream trustStore = MainTest.class.getResourceAsStream("/truststore_client");
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreBytes(ByteStreams.toByteArray(trustStore))
                .trustStorePassword("asdfgh");

        ClientConfig cc = new ClientConfig();
        cc.property(JettyClientProperties.SSL_CONFIG, sslConfig);
        cc.connectorProvider(new JettyConnectorProvider());

        Client client = ClientBuilder.newClient(cc);

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);
        target.register(new LoggingFilter());

        boolean caught = false;

        try {
            target.path("/").request().get(String.class);
        } catch (Exception e) {
            caught = true;
        }

        assertTrue(caught);
        // solaris throws java.net.SocketException instead of SSLHandshakeException
        // assertTrue(msg.contains("SSLHandshakeException"));

        client.close();
    }
}
