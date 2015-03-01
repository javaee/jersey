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
package org.glassfish.jersey.server;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test UriInfo content.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class UriInfoMatchedUrisTest {

    private ApplicationHandler createApplication(Class<?>... rc) {
        final ResourceConfig resourceConfig = new ResourceConfig(rc);
        return new ApplicationHandler(resourceConfig);
    }

    @Path("/")
    public static class RootPathResource {

        @GET
        public String getRoot(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "");
            return "root";
        }

        @GET
        @Path("bar")
        public String getRootBar(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "bar", "");
            return "rootbar";
        }

        @Path("baz")
        public RootSubResource getRootBaz(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "baz", "");
            return new RootSubResource();
        }
    }

    public static class RootSubResource {

        @GET
        public String getRootBaz(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "baz", "");
            return "rootbaz";
        }

        @GET
        @Path("bar")
        public String getRootBazBar(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "baz/bar", "baz", "");
            return "rootbazbar";
        }
    }

    @Path("foo")
    public static class Resource {

        @GET
        public String getFoo(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "foo");
            return "foo";
        }

        @GET
        @Path("bar")
        public String getFooBar(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "foo/bar", "foo");
            return "foobar";
        }

        @Path("baz")
        public SubResource getFooBaz(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "foo/baz", "foo");
            return new SubResource();
        }
    }

    public static class SubResource {

        @GET
        public String getFooBaz(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "foo/baz", "foo");
            return "foobaz";
        }

        @GET
        @Path("bar")
        public String getFooBazBar(@Context UriInfo uriInfo) {
            assertMatchedUris(uriInfo, "foo/baz/bar", "foo/baz", "foo");
            return "foobazbar";
        }
    }

    @Test
    public void testMatchedUris() throws Exception {
        ApplicationHandler app = createApplication(Resource.class, RootPathResource.class);

        ContainerResponse responseContext;

        responseContext = app.apply(RequestContextBuilder.from("/", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("root", responseContext.getEntity());

        responseContext = app.apply(RequestContextBuilder.from("/bar", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("rootbar", responseContext.getEntity());

        responseContext = app.apply(RequestContextBuilder.from("/baz", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("rootbaz", responseContext.getEntity());

        responseContext = app.apply(RequestContextBuilder.from("/baz/bar", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("rootbazbar", responseContext.getEntity());

        responseContext = app.apply(RequestContextBuilder.from("/foo", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("foo", responseContext.getEntity());

        responseContext = app.apply(RequestContextBuilder.from("/foo/bar", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("foobar", responseContext.getEntity());

        responseContext = app.apply(RequestContextBuilder.from("/foo/baz", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("foobaz", responseContext.getEntity());

        responseContext = app.apply(RequestContextBuilder.from("/foo/baz/bar", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("foobazbar", responseContext.getEntity());
    }

    /**
     * Reproducer test for JERSEY-2071.
     *
     * @throws Exception in case of test error.
     */
    @Test
    public void testAbsoluteMatchedUris() throws Exception {
        ApplicationHandler app = createApplication(Resource.class, RootPathResource.class);

        ContainerResponse responseContext;
        responseContext = app.apply(RequestContextBuilder.from("http://localhost:8080/", "http://localhost:8080/", "GET").build())
                .get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("root", responseContext.getEntity());

        responseContext = app
                .apply(RequestContextBuilder.from("http://localhost:8080/", "http://localhost:8080/bar", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("rootbar", responseContext.getEntity());

        responseContext = app
                .apply(RequestContextBuilder.from("http://localhost:8080/", "http://localhost:8080/baz", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("rootbaz", responseContext.getEntity());

        responseContext = app
                .apply(RequestContextBuilder.from("http://localhost:8080/", "http://localhost:8080/baz/bar", "GET").build())
                .get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("rootbazbar", responseContext.getEntity());

        responseContext = app
                .apply(RequestContextBuilder.from("http://localhost:8080/", "http://localhost:8080/foo", "GET").build()).get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("foo", responseContext.getEntity());

        responseContext = app
                .apply(RequestContextBuilder.from("http://localhost:8080/", "http://localhost:8080/foo/bar", "GET").build())
                .get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("foobar", responseContext.getEntity());

        responseContext = app
                .apply(RequestContextBuilder.from("http://localhost:8080/", "http://localhost:8080/foo/baz", "GET").build())
                .get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("foobaz", responseContext.getEntity());

        responseContext = app
                .apply(RequestContextBuilder.from("http://localhost:8080/", "http://localhost:8080/foo/baz/bar", "GET").build())
                .get();
        assertEquals(200, responseContext.getStatus());
        assertEquals("foobazbar", responseContext.getEntity());
    }

    private static void assertMatchedUris(UriInfo uriInfo, String... expectedMatchedUris) {
        final List<String> uris = uriInfo.getMatchedURIs();

        assertEquals(expectedMatchedUris.length, uris.size());
        int i = 0;
        for (String uri : expectedMatchedUris) {
            assertEquals(uri, uris.get(i++));
        }
    }
}
