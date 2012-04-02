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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.JerseyApplication;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Taken from Jersey-1: jersey-tests:com.sun.jersey.impl.subresources.SubResourceHttpMethodsTest
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class SubResourceHttpMethodsTest {

    JerseyApplication app;

    private JerseyApplication.Builder createApplicationBuilder(Class<?>... classes) {
        final ResourceConfig resourceConfig = new ResourceConfig(classes);
        return JerseyApplication.builder(resourceConfig);
    }

    @Path("/")
    static public class SubResourceMethods {
        @GET
        public String getMe() {
            return "/";
        }

        @Path("sub")
        @GET
        public String getMeSub() {
            return "/sub";
        }

        @Path("sub/sub")
        @GET
        public String getMeSubSub() {
            return "/sub/sub";
        }
    }

    @Test
    public void testSubResourceMethods() throws Exception {
        app = createApplicationBuilder(SubResourceMethods.class).build();

        assertEquals("/", app.apply(Requests.from("/","GET").build()).get().readEntity(String.class));
        assertEquals("/sub", app.apply(Requests.from("/sub","GET").build()).get().readEntity(String.class));
        assertEquals("/sub/sub", app.apply(Requests.from("/sub/sub","GET").build()).get().readEntity(String.class));
    }

    @Path("/")
    static public class SubResourceMethodsWithTemplates {
        @GET
        public String getMe() {
            return "/";
        }

        @Path("sub{t}")
        @GET
        public String getMeSub(@PathParam("t") String t) {
            return t;
        }

        @Path("sub/{t}")
        @GET
        public String getMeSubSub(@PathParam("t") String t) {
            return t;
        }

        @Path("subunlimited{t: .*}")
        @GET
        public String getMeSubUnlimited(@PathParam("t") String t) {
            return t;
        }

        @Path("subunlimited/{t: .*}")
        @GET
        public String getMeSubSubUnlimited(@PathParam("t") String t) {
            return t;
        }
    }

    @Test
    public void testSubResourceMethodsWithTemplates() throws Exception {
        app = createApplicationBuilder(SubResourceMethodsWithTemplates.class).build();

        assertEquals("/", app.apply(Requests.from("/","GET").build()).get().readEntity(String.class));

        assertEquals("value", app.apply(Requests.from("/subvalue","GET").build()).get().readEntity(String.class));
        assertEquals("a", app.apply(Requests.from("/sub/a","GET").build()).get().readEntity(String.class));

        assertEquals("value/a", app.apply(Requests.from("/subunlimitedvalue/a","GET").build()).get().readEntity(String.class));
        assertEquals("a/b/c/d", app.apply(Requests.from("/subunlimited/a/b/c/d","GET").build()).get().readEntity(String.class));
    }

    @Path("/")
    static public class SubResourceMethodsWithDifferentTemplates {
        @Path("{foo}")
        @GET
        public String getFoo(@PathParam("foo") String foo) {
            return foo;
        }

        // TODO: was bar in the @Path and @PathParam annotations bellow, shall it work?
//        @Path("{bar}")
        @Path("{foo}")
        @POST
        public String postBar(@PathParam("foo") String bar) {
            return bar;
        }
    }

    @Test
    public void testSubResourceMethodsWithDifferentTemplates() throws Exception {
        app = createApplicationBuilder(SubResourceMethodsWithDifferentTemplates.class).build();

        assertEquals("foo", app.apply(Requests.from("/foo","GET").build()).get().readEntity(String.class));
        assertEquals("bar", app.apply(Requests.from("/bar", "POST").build()).get().readEntity(String.class));
    }

    @Path("/{p}/")
    static public class SubResourceMethodWithLimitedTemplate {
        @GET
        public String getMe(@PathParam("p") String p, @QueryParam("id") String id) {
            return p + id;
        }

        @GET
        @Path("{id: .*}")
        public String getUnmatchedPath(
                @PathParam("p") String p,
                @PathParam("id") String path) {
          return path;
        }
    }

    @Test
    public void testSubResourceMethodWithLimitedTemplate() throws Exception {
        app = createApplicationBuilder(SubResourceMethodWithLimitedTemplate.class).build();

        assertEquals("topone", app.apply(Requests.from("/top/?id=one","GET").build()).get().readEntity(String.class));
        assertEquals("a/b/c/d", app.apply(Requests.from("/top/a/b/c/d","GET").build()).get().readEntity(String.class));
    }

    @Path("/{p}")
    static public class SubResourceNoSlashMethodWithLimitedTemplate {
        @GET
        public String getMe(@PathParam("p") String p, @QueryParam("id") String id) {
            System.out.println(id);
            return p + id;
        }

        @GET
        @Path(value="{id: .*}")
        public String getUnmatchedPath(
                @PathParam("p") String p,
                @PathParam("id") String path) {
          return path;
        }
    }

    @Test
    public void testSubResourceNoSlashMethodWithLimitedTemplate() throws Exception {
        app = createApplicationBuilder(SubResourceNoSlashMethodWithLimitedTemplate.class).build();

        assertEquals("topone", app.apply(Requests.from("/top?id=one","GET").build()).get().readEntity(String.class));
        assertEquals("a/b/c/d", app.apply(Requests.from("/top/a/b/c/d","GET").build()).get().readEntity(String.class));
    }

    @Path("/")
    static public class SubResourceWithSameTemplate {
        public static class SubResource {
            @GET
            @Path("bar")
            public String get() {
                return "BAR";
            }
        }

        @GET
        @Path("foo")
        public String get() {
            return "FOO";
        }

        @Path("foo")
        public SubResource getUnmatchedPath() {
            return new SubResource();
        }
    }

    @Test
    public void testSubResourceMethodWithSameTemplate() throws Exception {
        app = createApplicationBuilder(SubResourceWithSameTemplate.class).build();

        assertEquals("FOO", app.apply(Requests.from("/foo","GET").build()).get().readEntity(String.class));
        assertEquals("BAR", app.apply(Requests.from("/foo/bar","GET").build()).get().readEntity(String.class));
    }

    @Path("/")
    static public class SubResourceExplicitRegex {
        @GET
        @Path("{id}")
        public String getSegment(@PathParam("id") String id) {
            return "segment: " + id;
        }

        @GET
        @Path("{id: .+}")
        public String getSegments(@PathParam("id") String id) {
            return "segments: " + id;
        }

        @GET
        @Path("digit/{id: \\d+}")
        public String getDigit(@PathParam("id") int id) {
            return "digit: " + id;
        }

        @GET
        @Path("digit/{id}")
        public String getDigitAnything(@PathParam("id") String id) {
            return "anything: " + id;
        }
    }

    @Test
    public void testSubResource() throws Exception {
        app = createApplicationBuilder(SubResourceExplicitRegex.class).build();

        assertEquals("segments: foo", app.apply(Requests.from("/foo","GET").build()).get().readEntity(String.class));
        assertEquals("segments: foo/bar", app.apply(Requests.from("/foo/bar","GET").build()).get().readEntity(String.class));

        assertEquals("digit: 123", app.apply(Requests.from("/digit/123","GET").build()).get().readEntity(String.class));
        assertEquals("anything: foo", app.apply(Requests.from("/digit/foo","GET").build()).get().readEntity(String.class));
    }

    @Path("/")
    static public class SubResourceExplicitRegexCapturingGroups {
        @GET
        @Path("{a: (\\d)(\\d*)}")
        public String getSingle(@PathParam("a") int a) {
            return "" + a;
        }

        @GET
        @Path("{a: (\\d)(\\d*)}-{b: (\\d)(\\d*)}-{c: (\\d)(\\d*)}")
        public String getMultiple(
                @PathParam("a") int a,
                @PathParam("b") int b,
                @PathParam("c") int c) {
            return "" + a + "-" + b + "-" + c;
        }
    }

    @Test
    public void testSubResourceCapturingGroups() throws Exception {
        app = createApplicationBuilder(SubResourceExplicitRegexCapturingGroups.class).build();

        assertEquals("123", app.apply(Requests.from("/123","GET").build()).get().readEntity(String.class));
        assertEquals("123-456-789", app.apply(Requests.from("/123-456-789","GET").build()).get().readEntity(String.class));
    }


    @Path("/")
    static public class SubResourceXXX {
        @GET
        @Path("{id}/literal")
        public String getSegment(@PathParam("id") String id) {
            return id;
        }

        @GET
        @Path("{id1}/{id2}/{id3}")
        public String getSegments(
                @PathParam("id1") String id1,
                @PathParam("id2") String id2,
                @PathParam("id3") String id3
                ) {
            return id1 + id2 + id3;
        }
    }

    @Test
    public void testSubResourceXXX() throws Exception {
        app = createApplicationBuilder(SubResourceXXX.class).build();

        assertEquals("123", app.apply(Requests.from("/123/literal","GET").build()).get().readEntity(String.class));
        assertEquals("123literal789", app.apply(Requests.from("/123/literal/789","GET").build()).get().readEntity(String.class));
    }
}
