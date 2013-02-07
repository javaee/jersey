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
package org.glassfish.jersey.apache.connector;

import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.Assert.*;

/**
 * @author Paul.Sandoz@Sun.Com
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class HttpMethodTest extends AbstractGrizzlyServerTester {
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

    protected Client createClient() {
        ClientConfig cc = new ClientConfig();
        return ClientFactory.newClient(cc.connector(new ApacheConnector(cc.getConfiguration())));
    }

    protected Client createPoolingClient() {
        ClientConfig cc = new ClientConfig();
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(100);
        cc.setProperty(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        return ClientFactory.newClient(cc.connector(new ApacheConnector(cc.getConfiguration())));
    }

    @Test
    public void testHead() {
        startServer(HttpMethodResource.class);

        WebTarget r = createClient().target(getUri().path("test").build());
        Response cr = r.request().head();
        assertFalse(cr.hasEntity());
    }

    @Test
    public void testOptions() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());
        Response cr = r.request().options();
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testGet() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());
        assertEquals("GET", r.request().get(String.class));

        Response cr = r.request().get();
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPost() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());
        assertEquals("POST", r.request().post(Entity.text("POST"), String.class));

        Response cr = r.request().post(Entity.text("POST"));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPostChunked() {
        ResourceConfig rc = new ResourceConfig(HttpMethodResource.class);
        startServer(rc);

        ClientConfig cc = new ClientConfig();
        Client c = ClientFactory.newClient(cc);
        Client client = ClientFactory.newClient(new ClientConfig().setProperty(ClientProperties.CHUNKED_ENCODING_SIZE, 1024).connector(new ApacheConnector(c.getConfiguration())));
        WebTarget r = client.target(getUri().path("test").build());

        assertEquals("POST", r.request().post(Entity.text("POST"), String.class));

        Response cr = r.request().post(Entity.text("POST"));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPostVoid() {
        startServer(HttpMethodResource.class);
        WebTarget r = createPoolingClient().target(getUri().path("test").build());

        // This test will lock up if ClientResponse is not closed by WebResource.
        // TODO need a better way to detect this.
        for (int i = 0; i < 100; i++) {
            r.request().post(Entity.text("POST"));
        }
    }

    @Test
    public void testPostNoProduce() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());
        assertEquals(204, r.path("noproduce").request().post(Entity.text("POST")).getStatus());

        Response cr = r.path("noproduce").request().post(Entity.text("POST"));
        assertFalse(cr.hasEntity());
        cr.close();
    }


    @Test
    public void testPostNoConsumeProduce() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());
        assertEquals(204, r.path("noconsumeproduce").request().post(null).getStatus());

        Response cr = r.path("noconsumeproduce").request().post(Entity.text("POST"));
        assertFalse(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPut() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());
        assertEquals("PUT", r.request().put(Entity.text("PUT"), String.class));

        Response cr = r.request().put(Entity.text("PUT"));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testDelete() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());
        assertEquals("DELETE", r.request().delete(String.class));

        Response cr = r.request().delete();
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPatch() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());
        assertEquals("PATCH", r.request().method("PATCH", Entity.text("PATCH"), String.class));

        Response cr = r.request().method("PATCH", Entity.text("PATCH"));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testAll() {
        startServer(HttpMethodResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());

        assertEquals("GET", r.request().get(String.class));

        assertEquals("POST", r.request().post(Entity.text("POST"), String.class));

        assertEquals(204, r.path("noproduce").request().post(Entity.text("POST")).getStatus());

        assertEquals(204, r.path("noconsumeproduce").request().post(null).getStatus());

        assertEquals("PUT", r.request().post(Entity.text("PUT"), String.class));

        assertEquals("DELETE", r.request().delete(String.class));
    }


    @Path("/test")
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
        startServer(ErrorResource.class);
        WebTarget r = createClient().target(getUri().path("test").build());

        // This test will lock up if ClientResponse is not closed by WebResource.
        // TODO need a better way to detect this.
        for (int i = 0; i < 100; i++) {
            try {
                r.request().post(Entity.text("POST"));
            } catch (ClientErrorException ex) {
            }
        }
    }

    @Test
    public void testPostErrorWithEntity() {
        startServer(ErrorResource.class);
        WebTarget r = createPoolingClient().target(getUri().path("test/entity").build());

        // This test will lock up if ClientResponse is not closed by WebResource.
        // TODO need a better way to detect this.
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