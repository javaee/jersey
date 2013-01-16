/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test matching of resources with ambiguous templates.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class AmbiguousTemplateTest {

    @Path("{abc}")
    public static class ResourceABC {
        @PathParam("abc")
        String param;

        @Path("a")
        @GET
        public String get() {
            return "abc:" + param;
        }
    }

    @Path("{xyz}")
    public static class ResourceXYZ {
        @PathParam("xyz")
        String param;


        @Path("x")
        @GET
        public String get() {
            return "xyz:" + param;
        }
    }

    @Test
    public void testPathParamOnAmbiguousTemplate() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu/a", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("abc:uuu", response.getEntity());
    }

    @Test
    @Ignore("JERSEY-1669: does not work with ambiguous templates.")
    public void testPathParamOnAmbiguousTemplate2() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/test/x", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("xyz:test", response.getEntity());
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
        Assert.assertEquals("POST, GET, OPTIONS, HEAD", containerResponse.getEntity());
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
        Assert.assertEquals("GET, OPTIONS, HEAD, PUT", containerResponse.getEntity());
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