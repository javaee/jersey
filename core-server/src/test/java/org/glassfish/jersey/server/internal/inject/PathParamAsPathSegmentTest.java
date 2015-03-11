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

package org.glassfish.jersey.server.internal.inject;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.PathSegment;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Taken from Jersey-1: jersey-tests: com.sun.jersey.impl.methodparams.PathParamAsPathSegmentTest
 *
 * @author Paul Sandoz
 */
public class PathParamAsPathSegmentTest {

    ApplicationHandler app;

    private void initiateWebApplication(Class<?>... classes) {
        app = new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/{arg1}/{arg2}/{arg3}")
    public static class Resource {

        @GET
        public String doGet(
                @PathParam("arg1") PathSegment arg1,
                @PathParam("arg2") PathSegment arg2,
                @PathParam("arg3") PathSegment arg3) {
            assertEquals("a", arg1.getPath());
            assertEquals("b", arg2.getPath());
            assertEquals("c", arg3.getPath());
            return "content";
        }
    }

    @Test
    public void testStringArgsGet() throws Exception {
        initiateWebApplication(Resource.class);
        assertEquals("content", app.apply(RequestContextBuilder.from("/a/b/c", "GET").build()).get().getEntity());
    }

    @Path("/{id}")
    public static class Duplicate {

        @GET
        public String get(@PathParam("id") PathSegment id) {
            return id.getPath();
        }

        @GET
        @Path("/{id}")
        public String getSub(@PathParam("id") PathSegment id) {
            return id.getPath();
        }
    }

    @Test
    public void testDuplicate() throws Exception {
        initiateWebApplication(Duplicate.class);

        assertEquals("foo", app.apply(RequestContextBuilder.from("/foo", "GET").build()).get().getEntity());
        assertEquals("bar", app.apply(RequestContextBuilder.from("/foo/bar", "GET").build()).get().getEntity());
    }

    @Path("/{a}/{b}/{c}")
    public static class Root {

        @Path("/{x}/{y}/{z}")
        public Sub getSub() {
            return new Sub();
        }
    }

    public static class Sub {

        @Path("{foo}")
        @GET
        public String get(
                @PathParam("a") PathSegment a,
                @PathParam("b") PathSegment b,
                @PathParam("c") PathSegment c,
                @PathParam("x") PathSegment x,
                @PathParam("y") PathSegment y,
                @PathParam("z") PathSegment z,
                @PathParam("foo") PathSegment foo) {
            return acc(a, b, c, x, y, z, foo);
        }

        String acc(PathSegment... ps) {
            String s = "";
            for (PathSegment p : ps) {
                s += p.getPath();
            }

            return s;
        }
    }

    @Test
    public void testSubResources() throws Exception {
        initiateWebApplication(Root.class);

        assertEquals("1234567", app.apply(RequestContextBuilder.from("/1/2/3/4/5/6/7", "GET").build()).get().getEntity());
    }

    @Path("/{a}-{b}/{c}-{d}")
    public static class PathSeg {

        @GET
        public String doGet(
                @PathParam("a") PathSegment a,
                @PathParam("b") PathSegment b,
                @PathParam("c") PathSegment c,
                @PathParam("d") PathSegment d) {
            assertEquals(a.getPath(), b.getPath());
            assertEquals(c.getPath(), d.getPath());
            return "content";
        }

        @Path("{e}-{f}")
        @GET
        public String doGetSub(
                @PathParam("a") PathSegment a,
                @PathParam("b") PathSegment b,
                @PathParam("c") PathSegment c,
                @PathParam("d") PathSegment d,
                @PathParam("e") PathSegment e,
                @PathParam("f") PathSegment f) {
            assertEquals(a.getPath(), b.getPath());
            assertEquals(c.getPath(), d.getPath());
            assertEquals(e.getPath(), f.getPath());
            return "sub-content";
        }
    }

    @Test
    public void testPathSeg() throws Exception {
        initiateWebApplication(PathSeg.class);

        assertEquals("content", app.apply(RequestContextBuilder.from("/a-b/c-d", "GET").build()).get().getEntity());
        assertEquals("sub-content", app.apply(RequestContextBuilder.from("/a-b/c-d/e-f", "GET").build()).get().getEntity());
    }

    @Path("/{a: .+}/edit/{b}")
    public static class PathSegs {

        @GET
        public String doGet(
                @PathParam("a") PathSegment a,
                @PathParam("b") PathSegment b) {
            return a.getPath() + "-" + b.getPath();
        }
    }

    @Test
    public void testPathSegs() throws Exception {
        initiateWebApplication(PathSegs.class);

        assertEquals("z-b", app.apply(RequestContextBuilder.from("/x/y/z/edit/b", "GET").build()).get().getEntity());
        assertEquals("z-b", app.apply(RequestContextBuilder.from("///x/y/z/edit/b", "GET").build()).get().getEntity());
    }

    @Path("/{a: .+}")
    public static class PathSegsEnd {

        @GET
        public String doGet(
                @PathParam("a") PathSegment a) {
            return a.getPath();
        }
    }

    @Test
    public void testPathSegsEnd() throws Exception {
        initiateWebApplication(PathSegsEnd.class);

        assertEquals("z", app.apply(RequestContextBuilder.from("/x/y/z", "GET").build()).get().getEntity());
        assertEquals("", app.apply(RequestContextBuilder.from("/x/y/z/", "GET").build()).get().getEntity());
    }

    @Path("/{a: .+}/edit/{b}")
    public static class PathSegsList {

        @GET
        public String doGet(
                @PathParam("a") List<PathSegment> a,
                @PathParam("b") List<PathSegment> b) {
            StringBuilder s = new StringBuilder();
            for (PathSegment p : a) {
                if (p.getPath().isEmpty()) {
                    s.append('/');
                } else {
                    s.append(p.getPath());
                }
            }
            s.append('-');
            for (PathSegment p : b) {
                if (p.getPath().isEmpty()) {
                    s.append('/');
                } else {
                    s.append(p.getPath());
                }
            }

            return s.toString();
        }
    }

    @Test
    public void testPathSegsList() throws Exception {
        initiateWebApplication(PathSegsList.class);

        assertEquals("xyz-b", app.apply(RequestContextBuilder.from("/x/y/z/edit/b", "GET").build()).get().getEntity());
        assertEquals("//xyz-b", app.apply(RequestContextBuilder.from(
                "http://localhost/", "http://localhost///x/y/z/edit/b", "GET").build()).get().getEntity());
    }

    @Path("/{a: .+}")
    public static class PathSegsEndList {

        @GET
        public String doGet(
                @PathParam("a") List<PathSegment> a) {
            StringBuilder s = new StringBuilder();
            for (PathSegment p : a) {
                if (p.getPath().length() == 0) {
                    s.append('/');
                } else {
                    s.append(p.getPath());
                }
            }
            return s.toString();
        }
    }

    @Test
    public void testPathSegsEndList() throws Exception {
        initiateWebApplication(PathSegsEndList.class);

        assertEquals("xyz", app.apply(RequestContextBuilder.from("/x/y/z", "GET").build()).get().getEntity());
        assertEquals("xyz/", app.apply(RequestContextBuilder.from("/x/y/z/", "GET").build()).get().getEntity());
    }

    @Path("/")
    public static class PathSegOnSubResource {

        PathSegment ps;

        public PathSegOnSubResource() {
        }

        public PathSegOnSubResource(PathSegment ps) {
            this.ps = ps;
        }

        @GET
        public String get() {
            return ps.getPath();
        }

        @Path("{id}")
        public PathSegOnSubResource getSunResource(@PathParam("id") PathSegment ps) {
            return new PathSegOnSubResource(ps);
        }
    }

    @Test
    public void testPathSegOnSubResource() throws Exception {
        initiateWebApplication(PathSegOnSubResource.class);

        assertEquals("x", app.apply(RequestContextBuilder.from("/x", "GET").build()).get().getEntity());
    }
}
