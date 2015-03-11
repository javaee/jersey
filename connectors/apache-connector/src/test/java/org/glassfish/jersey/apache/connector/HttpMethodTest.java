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

package org.glassfish.jersey.apache.connector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul Sandoz
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class HttpMethodTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(HttpMethodResource.class, ErrorResource.class);
    }

    protected Client createClient() {
        ClientConfig cc = new ClientConfig();
        cc.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newClient(cc);
    }

    protected Client createPoolingClient() {
        ClientConfig cc = new ClientConfig();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(100);
        cc.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        cc.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newClient(cc);
    }

    private WebTarget getWebTarget(final Client client) {
        return client.target(getBaseUri()).path("test");
    }

    private WebTarget getWebTarget() {
        return getWebTarget(createClient());
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("PATCH")
    public @interface PATCH {
    }

    @Path("/test")
    public static class HttpMethodResource {
        @GET
        public String get() {
            return "GET";
        }

        @POST
        public String post(String entity) {
            return entity;
        }

        @PUT
        public String put(String entity) {
            return entity;
        }

        @DELETE
        public String delete() {
            return "DELETE";
        }

        @DELETE
        @Path("withentity")
        public String delete(String entity) {
            return entity;
        }

        @POST
        @Path("noproduce")
        public void postNoProduce(String entity) {
        }

        @POST
        @Path("noconsumeproduce")
        public void postNoConsumeProduce() {
        }

        @PATCH
        public String patch(String entity) {
            return entity;
        }
    }

    @Test
    public void testHead() {
        WebTarget r = getWebTarget();
        Response cr = r.request().head();
        assertFalse(cr.hasEntity());
    }

    @Test
    public void testOptions() {
        WebTarget r = getWebTarget();
        Response cr = r.request().options();
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testGet() {
        WebTarget r = getWebTarget();
        assertEquals("GET", r.request().get(String.class));

        Response cr = r.request().get();
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPost() {
        WebTarget r = getWebTarget();
        assertEquals("POST", r.request().post(Entity.text("POST"), String.class));

        Response cr = r.request().post(Entity.text("POST"));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPostChunked() {
        ClientConfig cc = new ClientConfig()
                .property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024)
                .connectorProvider(new ApacheConnectorProvider());
        Client client = ClientBuilder.newClient(cc);
        WebTarget r = getWebTarget(client);

        assertEquals("POST", r.request().post(Entity.text("POST"), String.class));

        Response cr = r.request().post(Entity.text("POST"));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPostVoid() {
        WebTarget r = getWebTarget(createPoolingClient());

        for (int i = 0; i < 100; i++) {
            r.request().post(Entity.text("POST"));
        }
    }

    @Test
    public void testPostNoProduce() {
        WebTarget r = getWebTarget();
        assertEquals(204, r.path("noproduce").request().post(Entity.text("POST")).getStatus());

        Response cr = r.path("noproduce").request().post(Entity.text("POST"));
        assertFalse(cr.hasEntity());
        cr.close();
    }


    @Test
    public void testPostNoConsumeProduce() {
        WebTarget r = getWebTarget();
        assertEquals(204, r.path("noconsumeproduce").request().post(null).getStatus());

        Response cr = r.path("noconsumeproduce").request().post(Entity.text("POST"));
        assertFalse(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPut() {
        WebTarget r = getWebTarget();
        assertEquals("PUT", r.request().put(Entity.text("PUT"), String.class));

        Response cr = r.request().put(Entity.text("PUT"));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testDelete() {
        WebTarget r = getWebTarget();
        assertEquals("DELETE", r.request().delete(String.class));

        Response cr = r.request().delete();
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPatch() {
        WebTarget r = getWebTarget();
        assertEquals("PATCH", r.request().method("PATCH", Entity.text("PATCH"), String.class));

        Response cr = r.request().method("PATCH", Entity.text("PATCH"));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testAll() {
        WebTarget r = getWebTarget();

        assertEquals("GET", r.request().get(String.class));

        assertEquals("POST", r.request().post(Entity.text("POST"), String.class));

        assertEquals(204, r.path("noproduce").request().post(Entity.text("POST")).getStatus());

        assertEquals(204, r.path("noconsumeproduce").request().post(null).getStatus());

        assertEquals("PUT", r.request().post(Entity.text("PUT"), String.class));

        assertEquals("DELETE", r.request().delete(String.class));
    }


    @Path("/error")
    public static class ErrorResource {
        @POST
        public Response post(String entity) {
            return Response.serverError().build();
        }

        @Path("entity")
        @POST
        public Response postWithEntity(String entity) {
            return Response.serverError().entity("error").build();
        }
    }

    @Test
    public void testPostError() {
        WebTarget r = createClient().target(getBaseUri()).path("error");

        for (int i = 0; i < 100; i++) {
            try {
                final Response post = r.request().post(Entity.text("POST"));
                post.close();
            } catch (ClientErrorException ex) {
            }
        }
    }

    @Test
    public void testPostErrorWithEntity() {
        WebTarget r = createPoolingClient().target(getBaseUri()).path("error/entity");

        for (int i = 0; i < 100; i++) {
            try {
                r.request().post(Entity.text("POST"));
            } catch (ClientErrorException ex) {
                String s = ex.getResponse().readEntity(String.class);
                assertEquals("error", s);
            }
        }
    }
}
