/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.client;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test no successful (3XX, 4XX, 5XX) responses with no empty body.
 *
 * @author Ballesi Ezequiel (ezequielballesi at gmail.com)
 */
public class NonSuccessfulResponseTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }

    @Path("resource")
    public static class TestResource {

        @GET
        @Path("/{status}")
        public Response getXXX(@PathParam("status") int status) {
            return Response.status(status).entity("get").build();
        }

        @POST
        @Path("/{status}")
        public Response postXXX(@PathParam("status") int status, String post) {
            return Response.status(status).entity(post).build();
        }

    }

    @Test
    public void testGet3XX() {
        generalTestGet(302);
    }

    @Test
    public void testPost3XX() {
        generalTestPost(302);
    }

    @Test
    public void testGet4XX() {
        generalTestGet(401);
    }

    @Test
    public void testPost4XX() {
        generalTestPost(401);
    }

    @Test
    public void testGet5XX() {
        generalTestGet(500);
    }

    @Test
    public void testPost5XX() {
        generalTestPost(500);
    }

    private void generalTestGet(int status) {
        WebTarget target = target("resource").path(Integer.toString(status));
        SyncInvoker sync = target.request();
        Response response = sync.get(Response.class);
        Assert.assertEquals(status, response.getStatus());
        Assert.assertEquals("get", response.readEntity(String.class));
    }

    private void generalTestPost(int status) {
        Entity<String> entity = Entity.entity("entity", MediaType.WILDCARD_TYPE);
        WebTarget target = target("resource").path(Integer.toString(status));
        SyncInvoker sync = target.request();
        Response response = sync.post(entity, Response.class);
        Assert.assertEquals(status, response.getStatus());
        Assert.assertEquals("entity", response.readEntity(String.class));
    }

}
