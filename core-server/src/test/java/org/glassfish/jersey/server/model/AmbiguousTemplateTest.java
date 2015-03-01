/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test matching of resources with ambiguous templates.
 *
 * @author Miroslav Fuksa
 *
 */
public class AmbiguousTemplateTest {

    @Path("{abc}")
    public static class ResourceABC {

        @PathParam("abc")
        String param;

        @Path("a")
        @GET
        public String getSub() {
            return "a-abc:" + param;
        }

        @GET
        public String get() {
            return "abc:" + param;
        }

    }

    @Path("{xyz}")
    public static class ResourceXYZ {

        @PathParam("xyz")
        String param;

        @POST
        public String post(String post) {
            return "xyz:" + param;
        }

        @Path("x")
        @GET
        public String get() {
            return "x-xyz:" + param;
        }

        @Path("{sub-x}")
        @GET
        public String get(@PathParam("sub-x") String subx) {
            return "subx-xyz:" + param + ":" + subx;
        }
    }

    @Test
    public void testPathParamOnAmbiguousTemplate() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu/a", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("a-abc:uuu", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplate2() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/test/x", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("x-xyz:test", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplate3() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("abc:uuu", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplate4() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/post", "POST")
                .entity("entity").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("xyz:post", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplate5() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/xxx/foo", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("subx-xyz:xxx:foo", response.getEntity());
    }

    @Path("locator")
    public static class SimpleLocator {

        @Path("{resource}")
        public Object locator(@PathParam("resource") String resource) {
            if ("xyz".equals(resource)) {
                return new ResourceXYZ();
            } else if ("abc".equals(resource)) {
                return new ResourceABC();
            }
            throw new WebApplicationException(404);
        }
    }

    @Test
    public void testPathParamOnAmbiguousTemplateTroughSubResourceLocator1() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(SimpleLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/locator/abc/uuu/a", "GET")
                .build()).get();
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void testPathParamOnAmbiguousTemplateTroughSubResourceLocator2() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(SimpleLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/locator/abc/a", "GET")
                .build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("a-abc:null", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplateTroughSubResourceLocator3() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(SimpleLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/locator/xyz/subxfoo", "GET")
                .build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("subx-xyz:null:subxfoo", response.getEntity());
    }

    @Path("{xyz}")
    public static class ResourceWithLocator {

        @PathParam("xyz")
        String param;

        @Path("/")
        public SubResource locator() {
            return new SubResource(param);
        }

        @Path("{path}")
        public SubResource subLocator(@PathParam("path") String path) {
            return new SubResource(param + ":" + path);
        }
    }

    public static class SubResource {

        private final String str;

        public SubResource(String str) {
            this.str = str;
        }

        @GET
        public String get() {
            return str;
        }
    }

    @Test
    public void testSubResourceLocatorWithPathParams() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceWithLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("uuu", response.getEntity());
    }

    @Test
    public void testSubResourceLocatorWithPathParams2() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceWithLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu/test", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("uuu:test", response.getEntity());
    }

    @Path("{templateA}")
    public static class ResourceA {

        @GET
        public String getA() {
            return "getA";
        }
    }

    @Path("{templateB}")
    public static class ResourceB {

        @POST
        public String postB(String entity) {
            return "postB";
        }
    }

    @Path("resq")
    public static class ResourceQ {

        @GET
        @Path("{path}")
        public String getA() {
            return "getA";
        }

        @PUT
        @Path("{temp}")
        public String put(String str) {
            return "getB";
        }
    }

    @Test
    public void testOptionsOnRoot() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class, ResourceB.class, ResourceQ.class);
        ApplicationHandler app = new ApplicationHandler(resourceConfig);
        final ContainerResponse containerResponse = app.apply(RequestContextBuilder.from("/aaa", "OPTIONS")
                .accept(MediaType.TEXT_PLAIN).build()).get();
        Assert.assertEquals(200, containerResponse.getStatus());

        final List<String> methods = Arrays.asList(containerResponse.getEntity().toString().split(", "));
        assertThat(methods, hasItems("POST", "GET", "OPTIONS", "HEAD"));
        assertThat(methods.size(), is(4));

    }

    @Test
    public void testGetOnRoot() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class, ResourceB.class, ResourceQ.class);
        ApplicationHandler app = new ApplicationHandler(resourceConfig);
        final ContainerResponse containerResponse = app.apply(RequestContextBuilder.from("/aaa", "GET")
                .accept(MediaType.TEXT_PLAIN).build()).get();
        Assert.assertEquals(200, containerResponse.getStatus());
        Assert.assertEquals("getA", containerResponse.getEntity());
    }

    @Test
    public void testOptionsOnChild() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class, ResourceB.class, ResourceQ.class);
        ApplicationHandler app = new ApplicationHandler(resourceConfig);
        final ContainerResponse containerResponse = app.apply(RequestContextBuilder.from("/resq/c", "OPTIONS")
                .accept(MediaType.TEXT_PLAIN).build()).get();
        Assert.assertEquals(200, containerResponse.getStatus());

        final List<String> methods = Arrays.asList(containerResponse.getEntity().toString().split(", "));
        assertThat(methods, hasItems("PUT", "GET", "OPTIONS", "HEAD"));
        assertThat(methods.size(), is(4));

    }

    @Test
    public void testGetOnChild() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class, ResourceB.class, ResourceQ.class);
        ApplicationHandler app = new ApplicationHandler(resourceConfig);
        final ContainerResponse containerResponse = app.apply(RequestContextBuilder.from("/resq/a", "GET").build()).get();
        Assert.assertEquals(200, containerResponse.getStatus());
        Assert.assertEquals("getA", containerResponse.getEntity());
    }
}
