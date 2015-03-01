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
import static org.junit.Assert.assertTrue;

/**
 * Taken from Jersey 1: jersey-tests:com.sun.jersey.impl.resource.AcceptTest.java
 *
 * @author Paul Sandoz
 */
public class AcceptTest {

    ApplicationHandler application;

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    public static class Resource {

        @Produces("application/foo")
        @GET
        public String doGetFoo() {
            return "foo";
        }

        @Produces("application/bar")
        @GET
        public String doGetBar() {
            return "bar";
        }

        @Produces("application/baz")
        @GET
        public String doGetBaz() {
            return "baz";
        }

        @Produces("*/*")
        @GET
        public Response doGetWildCard() {
            return Response.ok("wildcard", "application/wildcard").build();
        }
    }

    @Test
    public void testAcceptGet() throws Exception {
        ApplicationHandler app = createApplication(Resource.class);

        String s = (String) app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo").build()).get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=0.1").build()).get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo", "application/bar;q=0.4", "application/baz;q=0.2").build())
                .get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar", "application/baz;q=0.2").build())
                .get().getEntity();
        assertEquals("bar", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar;q=0.2", "application/baz").build())
                .get().getEntity();
        assertEquals("baz", s);
    }

    @Test
    public void testAcceptGetWildCard() throws Exception {
        ApplicationHandler app = createApplication(Resource.class);

        ContainerResponse response = app
                .apply(RequestContextBuilder.from("/", "GET").accept("application/wildcard", "application/foo;q=0.6",
                        "application/bar;q=0.4", "application/baz;q=0.2").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);

        String s = (String) response.getEntity();
        assertEquals("wildcard", s);
    }

    @Test
    public void testQualityErrorGreaterThanOne() throws Exception {
        ApplicationHandler app = createApplication(Resource.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=1.1").build())
                .get();
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testQualityErrorMoreThanThreeDigits() throws Exception {
        ApplicationHandler app = createApplication(Resource.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=0.1234").build())
                .get();
        assertEquals(400, response.getStatus());
    }

    @Path("/")
    public static class MultipleResource {

        @Produces({"application/foo", "application/bar"})
        @GET
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testAcceptMultiple() throws Exception {
        ApplicationHandler app = createApplication(MultipleResource.class);

        MediaType foo = MediaType.valueOf("application/foo");
        MediaType bar = MediaType.valueOf("application/bar");

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept(foo).build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(foo, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept(bar).build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(bar, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("*/*").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(foo, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/*").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(foo, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=0.1", "application/bar").build())
                .get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(bar, response.getMediaType());

        response = app
                .apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=0.5", "application/bar;q=0.1").build())
                .get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(foo, response.getMediaType());
    }

    @Test
    public void testAcceptMultiple2() throws Exception {
        ApplicationHandler app = createApplication(MultipleResource.class);

        MediaType foo = MediaType.valueOf("application/foo");

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("*/*").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(foo, response.getMediaType());
    }

    @Path("/")
    public static class SubTypeResource {

        @Produces("text/*")
        @GET
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testAcceptSubType() throws Exception {
        ApplicationHandler app = createApplication(SubTypeResource.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("image/png, text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
    }

    @Path("/")
    public static class NoProducesResource {

        @GET
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testAcceptNoProduces() throws Exception {
        ApplicationHandler app = createApplication(NoProducesResource.class);

        // media type order in the accept header does not impose output media type!
        ContainerResponse response = app
                .apply(RequestContextBuilder.from("/", "GET").accept("image/png, text/plain;q=0.9").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.valueOf("image/png"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
    }

    @Path("/")
    public static class ProducesOneMethodFooBarResource {

        @GET
        @Produces({"application/foo", "application/bar"})
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testProducesOneMethodFooBarResource() throws Exception {
        test(ProducesOneMethodFooBarResource.class);
    }

    @Path("/")
    public static class ProducesTwoMethodsFooBarResource {

        @GET
        @Produces("application/foo")
        public String getFoo() {
            return "GET";
        }

        @GET
        @Produces("application/bar")
        public String getBar() {
            return "GET";
        }
    }

    @Test
    public void testProducesTwoMethodsFooBarResource() throws Exception {
        test(ProducesTwoMethodsFooBarResource.class);
    }

    @Path("/")
    public static class ProducesTwoMethodsBarFooResource {

        @GET
        @Produces("application/bar")
        public String getBar() {
            return "GET";
        }

        @GET
        @Produces("application/foo")
        public String getFoo() {
            return "GET";
        }
    }

    @Test
    public void testProducesTwoMethodsBarFooResource() throws Exception {
        test(ProducesTwoMethodsBarFooResource.class);
    }

    private void test(Class<?> c) throws Exception {
        ApplicationHandler app = createApplication(c);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.valueOf("application/foo"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/bar").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.valueOf("application/bar"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo", "application/bar").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.valueOf("application/foo"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/bar", "application/foo").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.valueOf("application/bar"), response.getMediaType());
    }
}
