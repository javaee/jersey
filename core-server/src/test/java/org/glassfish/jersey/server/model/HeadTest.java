/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNotNull;

/**
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class HeadTest {

    private ApplicationHandler app;

    private void initiateWebApplication(Class<?>... classes) {
        app = new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    static public class ResourceGetNoHead {

        @GET
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testGetNoHead() throws Exception {
        initiateWebApplication(ResourceGetNoHead.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "HEAD").build()).get();

        assertEquals(200, response.getStatus());
        String length = response.getHeaderString("Content-Length");
        assertNotNull(length);
        assertEquals(3, Integer.parseInt(length));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
        assertFalse(response.hasEntity());
    }

    @Path("/")
    static public class ResourceGetWithHead {

        @HEAD
        public Response head() {
            return Response.ok().header("X-TEST", "HEAD").build();
        }

        @GET
        public Response get() {
            return Response.ok("GET").header("X-TEST", "GET").build();
        }
    }

    @Test
    public void testGetWithHead() throws Exception {
        initiateWebApplication(ResourceGetWithHead.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "HEAD").build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals("HEAD", response.getHeaders().getFirst("X-TEST"));
    }

    @Path("/")
    static public class ResourceGetWithProduceNoHead {

        @GET
        @Produces("application/foo")
        public String getFoo() {
            return "FOO";
        }

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
        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "HEAD").accept(foo).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(foo, response.getMediaType());

        MediaType bar = MediaType.valueOf("application/bar");
        response = app.apply(RequestContextBuilder.from("/", "HEAD").accept(bar).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(bar, response.getMediaType());
    }

    @Path("/")
    static public class ResourceGetWithProduceWithHead {

        @HEAD
        @Produces("application/foo")
        public Response headFoo() {
            return Response.ok().header("X-TEST", "FOO-HEAD").build();
        }

        @GET
        @Produces("application/foo")
        public Response getFoo() {
            return Response.ok("GET", "application/foo").header("X-TEST", "FOO-GET").build();
        }

        @HEAD
        @Produces("application/bar")
        public Response headBar() {
            return Response.ok().header("X-TEST", "BAR-HEAD").build();
        }

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
        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "HEAD").accept(foo).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(foo, response.getMediaType());
        assertEquals("FOO-HEAD", response.getHeaders().getFirst("X-TEST").toString());

        MediaType bar = MediaType.valueOf("application/bar");
        response = app.apply(RequestContextBuilder.from("/", "HEAD").accept(bar).build()).get();
        assertEquals(200, response.getStatus());
        assertFalse(response.hasEntity());
        assertEquals(bar, response.getMediaType());
        assertEquals("BAR-HEAD", response.getHeaders().getFirst("X-TEST").toString());
    }

    @Path("/")
    static public class ResourceGetByteNoHead {

        @GET
        public byte[] get() {
            return "GET".getBytes();
        }
    }

    @Test
    public void testGetByteNoHead() throws Exception {
        initiateWebApplication(ResourceGetByteNoHead.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "HEAD").build()).get();
        assertEquals(200, response.getStatus());
        String length = response.getHeaderString("Content-Length");
        assertNotNull(length);
        assertEquals(3, Integer.parseInt(length));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, response.getMediaType());
        assertFalse(response.hasEntity());
    }

    @Path("/")
    static public class ResourceGetWithNoProduces {

        @GET
        public Response getPlain() {
            return Response.ok("text").header("x-value", "text").
                    build();
        }

        @GET
        @Produces("text/html")
        public Response getHtml() {
            return Response.ok("html").header("x-value", "html").
                    build();
        }
    }

    @Test
    public void testResourceXXX() throws Exception {
        initiateWebApplication(ResourceGetWithNoProduces.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "HEAD").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("text", response.getHeaderString("x-value"));

        response = app.apply(RequestContextBuilder.from("/", "HEAD").accept("text/html").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("html", response.getHeaderString("x-value"));
    }
}
