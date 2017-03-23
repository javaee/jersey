/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Testing encoding of {@Path path annotations}.
 *
 * @author Miroslav Fuksa
 */
public class PathEncodingTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(PercentEncodedTest.class, AsteriskResource.class);
    }

    @Test
    public void test1() {
        assertEquals("ok", target().path("test").path("[]").request().get(String.class));
    }

    @Test
    public void test2() {
        assertEquals("ok", target().path("test").path("%5b%5d").request().get(String.class));
    }

    @Test
    public void test3() {
        assertEquals("ok", target().path("test").path("%5b%5D").request().get(String.class));
    }

    @Test
    public void testComplex1() {
        assertEquals("a-ok", target().path("test").path("test/a/[]").request().get(String.class));
    }

    @Test
    public void testComplex2() {
        assertEquals("a-ok", target().path("test").path("test/a/%5b%5D").request().get(String.class));
    }

    @Test
    public void testComplex3() {
        final Response res = target().path("test").path("test/a/path/%5b%5d").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("a-ok", res.readEntity(String.class));
    }

    @Test
    public void testNotFound() {
        final Response res = target().path("test").path("test/a/path/%5ab").request().get();
        assertEquals(404, res.getStatus());
    }

    @Test
    public void testComplex4() {
        assertEquals("a-ok", target().path("test").path("test/a/path/[]").request().get(String.class));
    }

    @Test
    public void testSlash() {
        assertEquals("ok", target().path("test/slash/").request().get(String.class));
    }

    @Test
    public void testWithoutSlash() {
        assertEquals("ok", target().path("test/slash").request().get(String.class));
    }

    @Test
    public void testAsteriskInPath() {
        Response response = target().path("*").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("ok", response.readEntity(String.class));
    }

    @Path("*")
    public static class AsteriskResource {
        @GET
        public String get() {
            return "ok";
        }
    }

    @Path("test")
    public static class PercentEncodedTest {

        @GET
        @Path("[]")
        public String simple() {
            return "ok";
        }

        @GET
        @Path("slash/")
        public String slash(@Context UriInfo uri) {
            return "ok";
        }

        @GET
        @Path("test/{a : .* }/[]")
        public String complex(@PathParam("a") String a) {
            return a + "-ok";
        }

        @GET
        @Path("test/{a : .* }/path/%5b%5D")
        public String complex2(@PathParam("a") String a) {
            return a + "-ok";
        }


    }
}
