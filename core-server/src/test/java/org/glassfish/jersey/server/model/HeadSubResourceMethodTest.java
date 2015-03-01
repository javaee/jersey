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
package org.glassfish.jersey.server.model;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class HeadSubResourceMethodTest {

    private ApplicationHandler app;

    private void initiateWebApplication(Class<?>... classes) {
        app = new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    public static class ResourceGetNoHead {

        @Path("sub")
        @GET
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testGetNoHead() throws Exception {
        initiateWebApplication(ResourceGetNoHead.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub", "HEAD").build()).get();

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
        assertFalse(response.hasEntity());
    }

    @Path("/")
    public static class ResourceGetWithHead {

        @Path("sub")
        @HEAD
        public Response head() {
            return Response.ok().header("X-TEST", "HEAD").build();
        }

        @Path("sub")
        @GET
        public Response get() {
            return Response.ok("GET").header("X-TEST", "GET").build();
        }
    }

    @Test
    public void testGetWithHead() throws Exception {
        initiateWebApplication(ResourceGetWithHead.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub", "HEAD").build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals("HEAD", response.getHeaders().getFirst("X-TEST"));
    }

    @Path("/")
    public static class ResourceGetWithProduceNoHead {

        @Path("sub")
        @GET
        @Produces("application/foo")
        public String getFoo() {
            return "FOO";
        }

        @Path("sub")
        @GET
        @Produces("application/bar")
        public String getBar() {
            return "BAR";
        }
    }

    @Test
    public void testGetWithProduceNoHead() throws Exception {
        initiateWebApplication(ResourceGetWithProduceNoHead.class);

        MediaType foo = MediaType.valueOf("application/foo");
        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub", "HEAD").accept(foo).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(foo, response.getMediaType());

        MediaType bar = MediaType.valueOf("application/bar");
        response = app.apply(RequestContextBuilder.from("/sub", "HEAD").accept(bar).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(bar, response.getMediaType());
    }

    @Path("/")
    public static class ResourceGetWithProduceWithHead {

        @Path("sub")
        @HEAD
        @Produces("application/foo")
        public Response headFoo() {
            return Response.ok().header("X-TEST", "FOO-HEAD").build();
        }

        @Path("sub")
        @GET
        @Produces("application/foo")
        public Response getFoo() {
            return Response.ok("GET", "application/foo").header("X-TEST", "FOO-GET").build();
        }

        @Path("sub")
        @HEAD
        @Produces("application/bar")
        public Response headBar() {
            return Response.ok().header("X-TEST", "BAR-HEAD").build();
        }

        @Path("sub")
        @GET
        @Produces("application/bar")
        public Response getBar() {
            return Response.ok("GET").header("X-TEST", "BAR-GET").build();
        }
    }

    @Test
    public void testGetWithProduceWithHead() throws Exception {
        initiateWebApplication(ResourceGetWithProduceWithHead.class);

        MediaType foo = MediaType.valueOf("application/foo");
        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub", "HEAD").accept(foo).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(foo, response.getMediaType());
        assertEquals("FOO-HEAD", response.getHeaders().getFirst("X-TEST").toString());

        MediaType bar = MediaType.valueOf("application/bar");
        response = app.apply(RequestContextBuilder.from("/sub", "HEAD").accept(bar).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(bar, response.getMediaType());
        assertEquals("BAR-HEAD", response.getHeaders().getFirst("X-TEST").toString());
    }

    @Path("/")
    public static class ResourceGetWithProduceNoHeadDifferentSub {

        @Path("sub1")
        @GET
        @Produces("application/foo")
        public String getFoo() {
            return "FOO";
        }

        @Path("sub2")
        @GET
        @Produces("application/bar")
        public String getBar() {
            return "BAR";
        }
    }

    @Test
    public void testGetWithProduceNoHeadDifferentSub() throws Exception {
        initiateWebApplication(ResourceGetWithProduceNoHeadDifferentSub.class);

        MediaType foo = MediaType.valueOf("application/foo");
        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub1", "HEAD").accept(foo).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(foo, response.getMediaType());

        MediaType bar = MediaType.valueOf("application/bar");
        response = app.apply(RequestContextBuilder.from("/sub2", "HEAD").accept(bar).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(bar, response.getMediaType());
    }
}
