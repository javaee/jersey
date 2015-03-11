/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jakub Podlesak
 */
public class AcceptQsTest {

    private static class StringReturningInflector implements Inflector<ContainerRequestContext, Response> {

        String entity;

        StringReturningInflector(String entity) {
            this.entity = entity;
        }

        @Override
        public Response apply(ContainerRequestContext data) {
            return Response.ok(entity).build();
        }
    }

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }

    private Inflector<ContainerRequestContext, Response> stringResponse(String s) {
        return new StringReturningInflector(s);
    }

    @Path("/")
    public static class TestResource {

        @Produces("application/foo;qs=0.4")
        @GET
        public String doGetFoo() {
            return "foo";
        }

        @Produces("application/bar;qs=0.5")
        @GET
        public String doGetBar() {
            return "bar";
        }

        @Produces("application/baz")
        @GET
        public String doGetBaz() {
            return "baz";
        }
    }

    @Test
    public void testAcceptGetDeclarative() throws Exception {
        runTestAcceptGet(createApplication(TestResource.class));
    }

    @Test
    public void testAcceptGetProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.4")).handledBy(stringResponse("foo"));
        rb.addMethod("GET").produces(MediaType.valueOf("application/bar;qs=0.5")).handledBy(stringResponse("bar"));
        rb.addMethod("GET").produces(MediaType.valueOf("application/baz")).handledBy(stringResponse("baz"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptGet(new ApplicationHandler(rc));
    }

    private void runTestAcceptGet(ApplicationHandler app) throws Exception {

        String s = (String) app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo").build()).get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=0.1").build()).get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo", "application/bar;q=0.4", "application/baz;q=0.2").build())
                .get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar;q=0.4", "application/baz;q=0.2").build())
                .get().getEntity();
        assertEquals("bar", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo", "application/bar", "application/baz;q=0.6").build())
                .get().getEntity();
        assertEquals("bar", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar", "application/baz;q=0.2").build())
                .get().getEntity();
        assertEquals("bar", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar;q=0.2", "application/baz").build())
                .get().getEntity();
        assertEquals("baz", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar;q=0.2", "application/baz;q=0.4").build())
                .get().getEntity();
        assertEquals("baz", s);
    }

    @Path("/")
    public static class MultipleResource {

        @Produces({"application/foo;qs=0.5", "application/bar"})
        @GET
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testAcceptMultipleDeclarative() throws Exception {
        runTestAcceptMultiple(createApplication(MultipleResource.class));
    }

    @Test
    public void testAcceptMultipleProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");
        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.5"), MediaType.valueOf("application/bar"))
                .handledBy(stringResponse("GET"));
        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptMultiple(new ApplicationHandler(rc));
    }

    private void runTestAcceptMultiple(ApplicationHandler app) throws Exception {

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
        assertEquals(bar, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/*").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.getEntity());
        assertEquals(bar, response.getMediaType());

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

    @Path("/")
    public static class SubTypeResource {

        @Produces("text/*;qs=0.5")
        @GET
        public String getWildcard() {
            return "*";
        }

        @Produces("text/plain;qs=0.6")
        @GET
        public String getPlain() {
            return "plain";
        }

        @Produces("text/html;qs=0.7")
        @GET
        public String getXml() {
            return "html";
        }
    }

    @Test
    public void testAcceptSubTypeDeclarative() throws Exception {
        runTestAcceptSubType(createApplication(SubTypeResource.class));
    }

    @Test
    public void testAcceptSubTypeProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("text/*;qs=0.5")).handledBy(stringResponse("*"));
        rb.addMethod("GET").produces(MediaType.valueOf("text/plain;qs=0.6")).handledBy(stringResponse("plain"));
        rb.addMethod("GET").produces(MediaType.valueOf("text/html;qs=0.7")).handledBy(stringResponse("html"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptSubType(new ApplicationHandler(rc));
    }

    private void runTestAcceptSubType(ApplicationHandler app) throws Exception {

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("image/png, text/plain;q=0.4").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("html", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain, text/html;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/html;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("html", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/*;q=0.5, text/plain;q=0.6").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/gaga;q=0.6").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.valueOf("text/gaga"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/gaga, text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.valueOf("text/plain"), response.getMediaType());
    }

    @Path("/")
    public static class SubTypeResourceNotIntuitive {

        @Produces("text/*;qs=0.9")
        @GET
        public String getWildcard() {
            return "*";
        }

        @Produces("text/plain;qs=0.7")
        @GET
        public String getPlain() {
            return "plain";
        }

        @Produces("text/html;qs=0.5")
        @GET
        public String getXml() {
            return "html";
        }
    }

    @Test
    public void testAcceptSubTypeNotIntuitiveDeclarative() throws Exception {
        runTestAcceptSubTypeNotIntuitive(createApplication(SubTypeResourceNotIntuitive.class));
    }

    @Test
    public void testAcceptSubTypeNotIntuitiveProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("text/*;qs=0.9")).handledBy(stringResponse("*"));
        rb.addMethod("GET").produces(MediaType.valueOf("text/plain;qs=0.7")).handledBy(stringResponse("plain"));
        rb.addMethod("GET").produces(MediaType.valueOf("text/html;qs=0.5")).handledBy(stringResponse("html"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptSubTypeNotIntuitive(new ApplicationHandler(rc));
    }

    private void runTestAcceptSubTypeNotIntuitive(ApplicationHandler app) throws Exception {

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("image/png, text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain, text/html;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/html;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/*;q=0.5, text/plain;q=0.6").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/gaga;q=0.6").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.valueOf("text/gaga"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/gaga, text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.valueOf("text/gaga"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/*").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/*;q=0.5, text/html;q=0.1").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.getEntity());
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
    public void testAcceptNoProducesDeclarative() throws Exception {
        runTestAcceptNoProduces(createApplication(NoProducesResource.class));
    }

    @Test
    public void testAcceptNoProducesProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");
        rb.addMethod("GET").handledBy(stringResponse("GET"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptNoProduces(new ApplicationHandler(rc));
    }

    private void runTestAcceptNoProduces(ApplicationHandler app) throws Exception {

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
        @Produces({"application/foo;qs=0.1", "application/bar"})
        public String get() {
            return "FOOBAR";
        }
    }

    @Test
    public void testProducesOneMethodFooBarResourceDeclarative() throws Exception {
        runTestFooBar(createApplication(ProducesOneMethodFooBarResource.class), "FOOBAR", "FOOBAR");
    }

    @Test
    public void testProducesOneMethodFooBarResourceProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");
        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.1"), MediaType.valueOf("application/bar"))
                .handledBy(stringResponse("FOOBAR"));
        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestFooBar(new ApplicationHandler(rc), "FOOBAR", "FOOBAR");
    }

    @Path("/")
    public static class ProducesTwoMethodsFooBarResource {

        @GET
        @Produces("application/foo;qs=0.1")
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
    public void testProducesTwoMethodsFooBarResourceProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.1")).handledBy(stringResponse("FOO"));
        rb.addMethod("GET").produces(MediaType.valueOf("application/bar")).handledBy(stringResponse("BAR"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestFooBar(new ApplicationHandler(rc), "FOO", "BAR");
    }

    @Test
    public void testProducesTwoMethodsFooBarResourceDeclarative() throws Exception {
        runTestFooBar(createApplication(ProducesTwoMethodsFooBarResource.class), "FOO", "BAR");
    }

    @Path("/")
    public static class ProducesTwoMethodsBarFooResource {

        @GET
        @Produces("application/bar")
        public String getBar() {
            return "BAR";
        }

        @GET
        @Produces("application/foo;qs=0.1")
        public String getFoo() {
            return "FOO";
        }
    }

    @Test
    public void testProducesTwoMethodsBarFooResourceProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("application/bar")).handledBy(stringResponse("BAR"));
        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.1")).handledBy(stringResponse("FOO"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestFooBar(new ApplicationHandler(rc), "FOO", "BAR");
    }

    @Test
    public void testProducesTwoMethodsBarFooResourceDeclarative() throws Exception {
        runTestFooBar(createApplication(ProducesTwoMethodsBarFooResource.class), "FOO", "BAR");
    }

    private void runTestFooBar(ApplicationHandler app, String fooContent, String barContent) throws Exception {

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals(fooContent, response.getEntity());
        assertEquals(MediaType.valueOf("application/foo"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/bar").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals(barContent, response.getEntity());
        assertEquals(MediaType.valueOf("application/bar"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo", "application/bar;q=0.5").build())
                .get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals(fooContent, response.getEntity());
        assertEquals(MediaType.valueOf("application/foo"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/bar", "application/foo;q=0.5").build())
                .get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals(barContent, response.getEntity());
        assertEquals(MediaType.valueOf("application/bar"), response.getMediaType());
    }
}
