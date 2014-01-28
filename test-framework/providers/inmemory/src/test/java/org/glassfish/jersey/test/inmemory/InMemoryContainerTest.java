/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.inmemory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for {@link InMemoryConnector}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InMemoryContainerTest extends JerseyTest {

    /**
     * Creates new instance.
     */
    public InMemoryContainerTest() {
        super(new InMemoryTestContainerFactory());
    }

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(TestResource.class, Resource1956.class, Resource2091.class, Resource2030.class);
    }

    /**
     * Test resource class.
     */
    @Path("test")
    public static class TestResource {

        /**
         * Test resource method.
         *
         * @return Test simple string response.
         */
        @GET
        public String getSomething() {
            return "get";
        }

        @POST
        public String post(String entity) {
            return entity + "-post";
        }

        @GET
        @Path("sub")
        public String getSubResource() {
            return "sub";
        }
    }

    @Test
    public void testInMemoryConnectorGet() {
        final Response response = target("test").request().get();

        assertTrue(response.getStatus() == 200);
    }

    @Test
    public void testGetSub() {
        final String response = target("test").path("sub").request().get(String.class);
        assertEquals("sub", response);
    }

    @Test
    public void testInMemoryConnnectorPost() {
        final Response response = target("test").request().post(
                Entity.entity("entity", MediaType.TEXT_PLAIN_TYPE));

        assertTrue(response.getStatus() == 200);
        assertEquals("entity-post", response.readEntity(String.class));
    }

    /**
     * Reproducer resource for JERSEY-1956.
     */
    @Path("1956")
    public static class Resource1956 {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("get-json")
        public String getJson(@Context HttpHeaders headers) throws Exception {
            String agent = headers.getHeaderString(HttpHeaders.USER_AGENT);
            return "{\"agent\": \"" + agent + "\"}";
        }
    }

    /**
     * Reproducer resource for JERSEY-2091.
     */
    @Path("2091")
    public static class Resource2091 {
        @POST
        @Produces(MediaType.TEXT_PLAIN)
        @Path("post-dummy-header")
        public String postHeader(@Context HttpHeaders headers) throws Exception {
            return "post-" + headers.getHeaderString("dummy-header");
        }
    }

    /**
     * Reproducer resource for JERSEY-2030.
     */
    @Path("2030")
    public static class Resource2030 {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public void asyncGet(@Suspended final AsyncResponse asyncResponse) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    asyncResponse.resume("resumed");
                }
            }).start();
        }
    }

    /**
     * Reproducer for JERSEY-1956.
     */
    @Test
    public void testAcceptNotStripped() {
        Response response;
        response = target("1956/get-json").request(MediaType.TEXT_PLAIN).get();
        assertThat(response.getStatus(), equalTo(Response.Status.NOT_ACCEPTABLE.getStatusCode()));

        response = target("1956/get-json").request(MediaType.APPLICATION_JSON).header(HttpHeaders.USER_AGENT, "test").get();
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        assertThat(response.readEntity(String.class), equalTo("{\"agent\": \"test\"}"));
    }

    /**
     * Reproducer for JERSEY-2091.
     */
    @Test
    public void testHeadersMakeItThroughForEntityLessRequest() {
        Response response =
                target("2091/post-dummy-header").request(MediaType.TEXT_PLAIN).header("dummy-header", "bummer").post(null);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        assertThat(response.readEntity(String.class), equalTo("post-bummer"));
    }

    /**
     * Reproducer for JERSEY-2030.
     */
    @Test
    public void testAsyncMethodsNotSupported() {
        try {
            target("2030").request(MediaType.TEXT_PLAIN).get();
            fail("ProcessingException expected.");
        } catch (ProcessingException ex) {
            assertThat(ex.getStackTrace()[0].getClassName(),
                    equalTo(InMemoryConnector.InMemoryResponseWriter.class.getName()));
        }
    }
}
