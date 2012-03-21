/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Request.RequestBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.JerseyApplication;
import org.glassfish.jersey.server.JerseyApplication.Builder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jakub Podlesak
 */
public class AcceptQsTest {

    private static class StringReturningInflector implements Inflector<Request, Response> {

        String entity;

        StringReturningInflector(String entity) {
            this.entity = entity;
        }

        @Override
        public Response apply(Request data) {
                return Response.ok(entity).build();
        }
    }

    private JerseyApplication createApplication(Class<?>... rc) {
        final ResourceConfig resourceConfig = ResourceConfig.builder().addClasses(rc).build();

        return JerseyApplication.builder(resourceConfig).build();
    }

    private Inflector<Request, Response> stringResponse(String s) {
        return new StringReturningInflector(s);
    }

    @Path("/")
    public static class Resource {
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
        _testAcceptGet(createApplication(Resource.class));
    }

    @Test
    public void testAcceptGetProgrammatic() throws Exception {
        final JerseyApplication.Builder appBuilder = JerseyApplication.builder();

        appBuilder.bind("/").produces(MediaType.valueOf("application/foo;qs=0.4")).method("GET").to(stringResponse("foo"));
        appBuilder.bind("/").produces(MediaType.valueOf("application/bar;qs=0.5")).method("GET").to(stringResponse("bar"));
        appBuilder.bind("/").produces(MediaType.valueOf("application/baz")).method("GET").to(stringResponse("baz"));

        _testAcceptGet(appBuilder.build());
    }

    private void _testAcceptGet(JerseyApplication app) throws Exception {

        String s = app.apply(Requests.from("/","GET").accept("application/foo").build()).get().readEntity(String.class);
        assertEquals("foo", s);

        s = app.apply(getRequest().accept("application/foo;q=0.1").build()).get().readEntity(String.class);
        assertEquals("foo", s);

        s = app.apply(Requests.from("/","GET").accept("application/foo", "application/bar;q=0.4", "application/baz;q=0.2").build())
                .get().readEntity(String.class);
        assertEquals("foo", s);

        s = app.apply(Requests.from("/","GET").accept("application/foo;q=0.4", "application/bar;q=0.4", "application/baz;q=0.2").build())
                .get().readEntity(String.class);
        assertEquals("bar", s);

        s = app.apply(Requests.from("/","GET").accept("application/foo", "application/bar", "application/baz;q=0.6").build())
                .get().readEntity(String.class);
        assertEquals("bar", s);

        s = app.apply(Requests.from("/","GET").accept("application/foo;q=0.4", "application/bar", "application/baz;q=0.2").build())
                .get().readEntity(String.class);
        assertEquals("bar", s);

        s = app.apply(Requests.from("/","GET").accept("application/foo;q=0.4", "application/bar;q=0.2", "application/baz").build())
                .get().readEntity(String.class);
        assertEquals("baz", s);

        s = app.apply(Requests.from("/","GET").accept("application/foo;q=0.4", "application/bar;q=0.2", "application/baz;q=0.4").build())
                .get().readEntity(String.class);
        assertEquals("baz", s);
    }

    private RequestBuilder getRequest() {
        return Requests.from("/","GET");
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
        _testAcceptMultiple(createApplication(MultipleResource.class));
    }

    @Test
    public void testAcceptMultipleProgrammatic() throws Exception {
        final Builder appBuilder = JerseyApplication.builder();
        appBuilder.bind("/")
                .produces(MediaType.valueOf("application/foo;qs=0.5"), MediaType.valueOf("application/bar"))
                .method("GET").to(stringResponse("GET"));
        _testAcceptMultiple(appBuilder.build());
    }

    private void _testAcceptMultiple(JerseyApplication app) throws Exception {

        MediaType foo = MediaType.valueOf("application/foo");
        MediaType bar = MediaType.valueOf("application/bar");

        Response response = app.apply(Requests.from("/","GET").accept(foo).build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.readEntity(String.class));
        assertEquals(foo, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept(bar).build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.readEntity(String.class));
        assertEquals(bar, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("*/*").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.readEntity(String.class));
        assertEquals(bar, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("application/*").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.readEntity(String.class));
        assertEquals(bar, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("application/foo;q=0.1","application/bar").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.readEntity(String.class));
        assertEquals(bar, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("application/foo;q=0.5","application/bar;q=0.1").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.readEntity(String.class));
        assertEquals(foo, response.getHeaders().getMediaType());
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
        _testAcceptSubType(createApplication(SubTypeResource.class));
    }

    @Test
    public void testAcceptSubTypeProgrammatic() throws Exception {
        final Builder appBuilder = JerseyApplication.builder();

        appBuilder.bind("/").produces(MediaType.valueOf("text/*;qs=0.5")).method("GET").to(stringResponse("*"));
        appBuilder.bind("/").produces(MediaType.valueOf("text/plain;qs=0.6")).method("GET").to(stringResponse("plain"));
        appBuilder.bind("/").produces(MediaType.valueOf("text/html;qs=0.7")).method("GET").to(stringResponse("html"));

        _testAcceptSubType(appBuilder.build());
    }

    private void _testAcceptSubType(JerseyApplication app) throws Exception {

        Response response = app.apply(Requests.from("/","GET").accept("text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("image/png, text/plain;q=0.4").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("html", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/plain, text/html;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/html;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("html", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/*;q=0.5, text/plain;q=0.6").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/plain;q=0.5, text/gaga;q=0.6").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.valueOf("text/gaga"), response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/gaga, text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.readEntity(String.class));
        assertEquals(MediaType.valueOf("text/plain"), response.getHeaders().getMediaType());
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
        _testAcceptSubTypeNotIntuitive(createApplication(SubTypeResourceNotIntuitive.class));
    }

    @Test
    public void testAcceptSubTypeNotIntuitiveProgramatic() throws Exception {
        final Builder appBuilder = JerseyApplication.builder();

        appBuilder.bind("/").produces(MediaType.valueOf("text/*;qs=0.9")).method("GET").to(stringResponse("*"));
        appBuilder.bind("/").produces(MediaType.valueOf("text/plain;qs=0.7")).method("GET").to(stringResponse("plain"));
        appBuilder.bind("/").produces(MediaType.valueOf("text/html;qs=0.5")).method("GET").to(stringResponse("html"));

        _testAcceptSubTypeNotIntuitive(appBuilder.build());
    }

    private void _testAcceptSubTypeNotIntuitive(JerseyApplication app) throws Exception {

        Response response = app.apply(Requests.from("/","GET").accept("text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("image/png, text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/plain, text/html;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/html;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/*;q=0.5, text/plain;q=0.6").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/plain;q=0.5, text/gaga;q=0.6").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.valueOf("text/gaga"), response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/gaga, text/plain").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("*", response.readEntity(String.class));
        assertEquals(MediaType.valueOf("text/gaga"), response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/*").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/*;q=0.5, text/html;q=0.1").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("plain", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());
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
        _testAcceptNoProduces(createApplication(NoProducesResource.class));
    }

    @Test
    public void testAcceptNoProducesProgrammatic() throws Exception {
        final Builder appBuilder = JerseyApplication.builder();
        appBuilder.bind("/").method("GET").to(stringResponse("GET"));
        _testAcceptNoProduces(appBuilder.build());
    }

    private void _testAcceptNoProduces(JerseyApplication app) throws Exception {

        // media type order in the accept header does not impose output media type!
        Response response = app.apply(Requests.from("/","GET").accept("image/png, text/plain;q=0.9").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.readEntity(String.class));
        assertEquals(MediaType.valueOf("image/png"), response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("GET", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getHeaders().getMediaType());
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
        _testFooBar(createApplication(ProducesOneMethodFooBarResource.class), "FOOBAR", "FOOBAR");
    }

    @Test
    public void testProducesOneMethodFooBarResourceProgrammatic() throws Exception {
        final Builder appBuilder = JerseyApplication.builder();
        appBuilder.bind("/").method("GET")
                    .produces(MediaType.valueOf("application/foo;qs=0.1"), MediaType.valueOf("application/bar"))
                    .to(stringResponse("FOOBAR"));
        _testFooBar(appBuilder.build(), "FOOBAR", "FOOBAR");
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
        final Builder appBuilder = JerseyApplication.builder();

        appBuilder.bind("/").produces(MediaType.valueOf("application/foo;qs=0.1")).method("GET").to(stringResponse("FOO"));
        appBuilder.bind("/").produces(MediaType.valueOf("application/bar")).method("GET").to(stringResponse("BAR"));

        _testFooBar(appBuilder.build(), "FOO", "BAR");
    }

    @Test
    public void testProducesTwoMethodsFooBarResourceDeclarative() throws Exception {
        _testFooBar(createApplication(ProducesTwoMethodsFooBarResource.class), "FOO", "BAR");
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
        final Builder appBuilder = JerseyApplication.builder();

        appBuilder.bind("/").produces(MediaType.valueOf("application/bar")).method("GET").to(stringResponse("BAR"));
        appBuilder.bind("/").produces(MediaType.valueOf("application/foo;qs=0.1")).method("GET").to(stringResponse("FOO"));

        _testFooBar(appBuilder.build(), "FOO", "BAR");
    }

    @Test
    public void testProducesTwoMethodsBarFooResourceDeclarative() throws Exception {
        _testFooBar(createApplication(ProducesTwoMethodsBarFooResource.class), "FOO", "BAR");
    }

    private void _testFooBar(JerseyApplication app, String fooContent, String barContent) throws Exception {

        Response response = app.apply(Requests.from("/","GET").accept("application/foo").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals(fooContent, response.readEntity(String.class));
        assertEquals(MediaType.valueOf("application/foo"), response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("application/bar").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals(barContent, response.readEntity(String.class));
        assertEquals(MediaType.valueOf("application/bar"), response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("application/foo", "application/bar;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals(fooContent, response.readEntity(String.class));
        assertEquals(MediaType.valueOf("application/foo"), response.getHeaders().getMediaType());

        response = app.apply(Requests.from("/","GET").accept("application/bar", "application/foo;q=0.5").build()).get();
        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals(barContent, response.readEntity(String.class));
        assertEquals(MediaType.valueOf("application/bar"), response.getHeaders().getMediaType());
    }
}
