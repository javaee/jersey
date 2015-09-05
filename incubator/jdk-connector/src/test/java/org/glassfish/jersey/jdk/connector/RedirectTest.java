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

package org.glassfish.jersey.jdk.connector;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class RedirectTest extends JerseyTest {

    private static String TARGET_GET_MSG = "You have reached the target";

    @Override
    protected Application configure() {
        return new ResourceConfig(RedirectingResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new JdkConnectorProvider());
    }

    @Test
    public void testDisableRedirect() {
        Response response = target("redirecting/303").property(ClientProperties.FOLLOW_REDIRECTS, false).request().get();
        assertEquals(303, response.getStatus());
    }

    @Test
    public void testGet303() {
        Response response = target("redirecting/303").request().get();
        assertEquals(200, response.getStatus());
        assertEquals(TARGET_GET_MSG, response.readEntity(String.class));
    }

    @Test
    public void testPost303() {
        Response response = target("redirecting/303").request().post(Entity.entity("My awesome message", MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(TARGET_GET_MSG, response.readEntity(String.class));
    }

    @Test
    public void testHead303() {
        Response response = target("redirecting/303").request().head();
        assertEquals(200, response.getStatus());
        assertTrue(response.readEntity(String.class).isEmpty());
    }

    // in this implementation; 301, 307 and 308 work exactly the same
    @Test
    public void testGet307() {
        Response response = target("redirecting/307").request().get();
        assertEquals(200, response.getStatus());
        assertEquals(TARGET_GET_MSG, response.readEntity(String.class));
    }

    // in this implementation; 301, 307 and 308 work exactly the same
    @Test
    public void testPost307() {
        Response response = target("redirecting/307").request().post(Entity.entity("My awesome message", MediaType.TEXT_PLAIN));
        assertEquals(307, response.getStatus());
    }

    // in this implementation; 301, 307 and 308 work exactly the same
    @Test
    public void testHead307() {
        Response response = target("redirecting/307").request().head();
        assertEquals(200, response.getStatus());
        assertTrue(response.readEntity(String.class).isEmpty());
    }

    @Test
    public void testCycle() {
        try {
            target("redirecting/cycle").request().get();
            fail();
        } catch (Throwable t) {
            assertEquals(RedirectException.class.getName(), t.getCause().getCause().getClass().getName());
        }
    }

    @Test
    public void testMaxRedirectsSuccess() {
        Response response = target("redirecting/maxRedirect").property(JdkConnectorProvider.MAX_REDIRECTS, 2).request().get();
        assertEquals(200, response.getStatus());
        assertEquals(TARGET_GET_MSG, response.readEntity(String.class));
    }

    @Test
    public void testMaxRedirectsFail() {
        try {
            target("redirecting/maxRedirect").property(JdkConnectorProvider.MAX_REDIRECTS, 1).request().get();
            fail();
        } catch (Throwable t) {
            assertEquals(RedirectException.class.getName(), t.getCause().getCause().getClass().getName());
        }
    }

    @Path("/redirecting")
    public static class RedirectingResource {

        private Response get303RedirectToTarget() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("target").build()).build();
        }

        private Response get307RedirectToTarget() {
            return Response.temporaryRedirect(UriBuilder.fromResource(RedirectingResource.class).path("target").build()).build();
        }

        @Path("303")
        @HEAD
        public Response head303() {
            return get303RedirectToTarget();
        }

        @Path("303")
        @GET
        public Response get303() {
            return get303RedirectToTarget();
        }

        @Path("303")
        @POST
        public Response post303(String entity) {
            return get303RedirectToTarget();
        }

        @Path("307")
        @HEAD
        public Response head307() {
            return get307RedirectToTarget();
        }

        @Path("307")
        @GET
        public Response get307() {
            return get307RedirectToTarget();
        }

        @Path("307")
        @POST
        public Response post307(String entity) {
            return get307RedirectToTarget();
        }

        @Path("target")
        @GET
        public String target() {
            return TARGET_GET_MSG;
        }

        @Path("target")
        @POST
        public String target(String entity) {
            return entity;
        }

        @Path("cycle")
        @GET
        public Response cycle() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("cycleNode2").build()).build();
        }

        @Path("cycleNode2")
        @GET
        public Response cycleNode2() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("cycleNode3").build()).build();
        }

        @Path("cycleNode3")
        @GET
        public Response cycleNode3() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("cycle").build()).build();
        }

        @Path("maxRedirect")
        @GET
        public Response maxRedirect() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("maxRedirectNode2").build()).build();
        }

        @Path("maxRedirectNode2")
        @GET
        public Response maxRedirectNode2() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("target").build()).build();
        }
    }
}
