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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SSL connector tests.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@RunWith(Parameterized.class)
public class SslConnectorConfigurationTest extends AbstractConnectorServerTest {

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
        final WebTarget target = client.target(Server.BASE_URI).register(LoggingFeature.class);

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

        final WebTarget target = client.target(Server.BASE_URI).register(LoggingFeature.class);

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

        WebTarget target = client.target(Server.BASE_URI).register(LoggingFeature.class);

        boolean caught = false;
        try {
            target.path("/").request().get(String.class);
        } catch (Exception e) {
            caught = true;
        }

        assertTrue(caught);
    }

    /**
     * Test that a response to an authentication challenge has the same SSL configuration as the original request.
     */
    @Test
    public void testSSLWithNonPreemptiveAuth() throws Exception {
        final SSLContext sslContext = getSslContext();

        final ClientConfig cc = new ClientConfig().connectorProvider(connectorProvider);
        final Client client = ClientBuilder.newBuilder()
                .withConfig(cc)
                .sslContext(sslContext)
                .build();

        // client basic auth demonstration
        HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials("user", "password")
                .build();

        client.register(authFeature);
        final WebTarget target = client.target(Server.BASE_URI).register(LoggingFeature.class);

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(200, response.getStatus());
    }
}
