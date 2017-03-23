/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Sub-resource access/processing E2E tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
public class SubResourceTest extends JerseyTest {

    @Path("root/sub")
    public static class Resource {
        @Path("/")
        public SubResource getSubResourceLocator() {
            return new SubResource();
        }

        @Path("sub2")
        public SubResource getSubResourceLocator2() {
            return new SubResource();
        }

        static final String GET = "get";

        @Path("some/path")
        @GET
        public String get() {
            return GET;
        }

        @Path("empty-locator")
        public EmptySubResourceClass getEmptyLocator() {
            return new EmptySubResourceClass();
        }
    }

    public static class SubResource {
        public static final String MESSAGE = "Got it!";

        @GET
        public String getIt() {
            return MESSAGE;
        }


        @POST
        public String post(String str) {
            return str;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, LocatorAndMethodResource.class, EmptyRootResource.class);
    }

    /**
     * Test concurrent sub-resource access. (See JERSEY-1421).
     *
     * @throws Exception in case of test failure.
     */
    @Test
    public void testConcurrentSubResourceAccess() throws Exception {
        final WebTarget subResource = target("root/sub/sub2");

        final int MAX = 25;

        final List<Future<String>> results = new ArrayList<Future<String>>(MAX);
        for (int i = 0; i < MAX; i++) {
            results.add(subResource.request().async().get(String.class));
        }

        for (Future<String> resultFuture : results) {
            assertEquals(SubResource.MESSAGE, resultFuture.get());
        }
    }

    @Test
    public void subResourceTest() throws Exception {
        Response response = target("root/sub/sub2").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(SubResource.MESSAGE, response.readEntity(String.class));

        response = target("root/sub/sub2").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(SubResource.MESSAGE, response.readEntity(String.class));
    }

    @Test
    public void subResourceWithoutPathTest() throws Exception {
        Response response = target("root/sub").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(SubResource.MESSAGE, response.readEntity(String.class));
    }

    @Test
    public void testGet() throws Exception {
        Response response = target("root/sub/some/path").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(Resource.GET, response.readEntity(String.class));
    }

    @Test
    public void testPost() throws Exception {
        Response response = target("root/sub/sub2").request().post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("post", response.readEntity(String.class));
    }

    // this resource class will report warning during validation, but should be loaded
    @Path("locator-and-method")
    public static class LocatorAndMethodResource {
        @GET
        @Path("sub")
        public String getSub() {
            return "get";
        }

        @Path("sub")
        public PostSubResource getSubResourceSub() {
            return new PostSubResource();
        }

        @GET
        public String get() {
            return "get";
        }

        @Path("/")
        public PostSubResource getSubResource() {
            return new PostSubResource();
        }
    }

    public static class PostSubResource {
        @GET
        public String get() {
            return "fail: locator get should never be called !!!";
        }

        @POST
        public String post(String post) {
            return "fail: post should never be called !!!";
        }

        @GET
        @Path("inner")
        public String getInner() {
            return "inner";
        }
    }

    @Test
    public void testGetIsCalled() throws Exception {
        Response response = target("locator-and-method").request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("get", response.readEntity(String.class));
    }

    @Test
    public void testGetIsCalledInSub() throws Exception {
        Response response = target("locator-and-method/sub").request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("get", response.readEntity(String.class));
    }

    @Test
    public void testGetIsCalledInInner() throws Exception {
        Response response = target("locator-and-method/inner").request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("inner", response.readEntity(String.class));
    }

    @Test
    public void testGetIsCalledInSubInner() throws Exception {
        Response response = target("locator-and-method/sub/inner").request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("inner", response.readEntity(String.class));
    }

    @Test
    public void testPostShouldNeverBeCalled() throws Exception {
        Response response = target("locator-and-method").request().post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testPostShouldNeverBeCalledInSub() throws Exception {
        Response response = target("locator-and-method/sub").request().post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
    }

    @Path("empty-root")
    public static class EmptyRootResource {

    }

    @Test
    public void testCallEmptyResource() throws Exception {
        Response response = target("empty-root").request().get();
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    public static class EmptySubResourceClass {
        // empty
    }

    @Test
    public void testCallEmptySubResource() throws Exception {
        Response response = target("empty-locator").request().get();
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
