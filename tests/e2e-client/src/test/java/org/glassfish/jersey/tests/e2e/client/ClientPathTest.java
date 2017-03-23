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

package org.glassfish.jersey.tests.e2e.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test definition of path in client invocation.
 *
 * @author Miroslav Fuksa
 *
 */
public class ClientPathTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class, TestResourceWithPathParams.class);
    }

    /**
     * Test that {@link PathParam path parameters} can be passed to {@link Client#target(String)} method.
     */
    @Test
    public void pathParamInTargetTest() {

        Response response = client().target("http://localhost:" + getPort() + "/test/{beginBy}")
                .resolveTemplate("beginBy", "abc")
                .request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(200, response.getStatus());
        assertEquals("test-get,abc", response.readEntity(String.class));
    }

    /**
     * Tests path concatenation. (regression test for JERSEY-1114)
     */
    @Test
    public void pathConcatenationTest1() {
        Response response = client().target("http://localhost:" + getPort()).path("path").request(MediaType.TEXT_PLAIN_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("test-path", response.readEntity(String.class));
    }

    /**
     * Tests path concatenation. (regression test for JERSEY-1114)
     */
    @Test
    public void pathConcatenationTest2() {
        Response response = client().target("http://localhost:" + getPort()).path("/path").request(MediaType.TEXT_PLAIN_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("test-path", response.readEntity(String.class));
    }

    /**
     * Tests path concatenation. (regression test for JERSEY-1114)
     */
    @Test
    public void pathConcatenationTest3() {
        Response response = client().target("http://localhost:" + getPort()).path("/path/").path("/another/")
                .request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(200, response.getStatus());
        assertEquals("test-another-path", response.readEntity(String.class));
    }

    /**
     * Tests path concatenation. (regression test for JERSEY-1114)
     */
    @Test
    public void pathConcatenationTest4() {
        Response response = client().target("http://localhost:" + getPort()).path("/path").path("another/")
                .request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(200, response.getStatus());
        assertEquals("test-another-path", response.readEntity(String.class));
    }

    /**
     * Tests path concatenation. (regression test for JERSEY-1114)
     */
    @Test
    public void pathConcatenationTest6() {
        Response response = client().target("http://localhost:" + getPort() + "/").path("/path/another")
                .request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(200, response.getStatus());
        assertEquals("test-another-path", response.readEntity(String.class));
    }

    /**
     * Test resource class with {@link PathParam path parameters).
     *
     */
    @Path("test/{beginBy}")
    public static class TestResourceWithPathParams {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.TEXT_PLAIN)
        public String get(@PathParam(value = "beginBy") String param) {
            return "test-get," + param;
        }

    }

    /**
     * Test resource class.
     *
     */
    @Path("path")
    public static class TestResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.TEXT_PLAIN)
        public String get() {
            return "test-path";
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.TEXT_PLAIN)
        @Path("another")
        public String getAnother() {
            return "test-another-path";
        }

    }

}
