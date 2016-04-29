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

package org.glassfish.jersey.jetty.connector;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul Sandoz
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class CookieTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(CookieTest.class.getName());

    @Path("/")
    public static class CookieResource {
        @GET
        public Response get(@Context HttpHeaders h) {
            Cookie c = h.getCookies().get("name");
            String e = (c == null) ? "NO-COOKIE" : c.getValue();
            return Response.ok(e)
                    .cookie(new NewCookie("name", "value")).build();
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(CookieResource.class);
        config.register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
        return config;
    }

    @Test
    public void testCookieResource() {
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new JettyConnectorProvider());
        Client client = ClientBuilder.newClient(config);
        WebTarget r = client.target(getBaseUri());


        assertEquals("NO-COOKIE", r.request().get(String.class));
        assertEquals("value", r.request().get(String.class));
        client.close();
    }

    @Test
    public void testDisabledCookies() {
        ClientConfig cc = new ClientConfig();
        cc.property(JettyClientProperties.DISABLE_COOKIES, true);
        cc.connectorProvider(new JettyConnectorProvider());
        JerseyClient client = JerseyClientBuilder.createClient(cc);
        WebTarget r = client.target(getBaseUri());

        assertEquals("NO-COOKIE", r.request().get(String.class));
        assertEquals("NO-COOKIE", r.request().get(String.class));

        final JettyConnector connector = (JettyConnector) client.getConfiguration().getConnector();
        if (connector.getCookieStore() != null) {
            assertTrue(connector.getCookieStore().getCookies().isEmpty());
        } else {
            assertNull(connector.getCookieStore());
        }
        client.close();
    }

    @Test
    public void testCookies() {
        ClientConfig cc = new ClientConfig();
        cc.connectorProvider(new JettyConnectorProvider());
        JerseyClient client = JerseyClientBuilder.createClient(cc);
        WebTarget r = client.target(getBaseUri());

        assertEquals("NO-COOKIE", r.request().get(String.class));
        assertEquals("value", r.request().get(String.class));

        final JettyConnector connector = (JettyConnector) client.getConfiguration().getConnector();
        assertNotNull(connector.getCookieStore().getCookies());
        assertEquals(1, connector.getCookieStore().getCookies().size());
        assertEquals("value", connector.getCookieStore().getCookies().get(0).getValue());
        client.close();
    }
}
