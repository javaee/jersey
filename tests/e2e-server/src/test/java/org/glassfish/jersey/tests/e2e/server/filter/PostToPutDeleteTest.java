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

package org.glassfish.jersey.tests.e2e.server.filter;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Martin Matula
 */
public class PostToPutDeleteTest extends JerseyTest {

    @Path("/")
    public static class Resource {

        @GET
        public String get(@QueryParam("a") String a) {
            return "GET: " + a;
        }

        @PUT
        public String put() {
            return "PUT";
        }

        @DELETE
        public String delete() {
            return "DELETE";
        }

        @POST
        public String post() {
            return "POST";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(HttpMethodOverrideFilter.class, Resource.class);
    }

    @Test
    public void testPut() {
        assertResponseEquals("PUT,PUT,PUT,", _test("PUT"));
    }

    @Test
    public void testDelete() {
        assertResponseEquals("DELETE,DELETE,DELETE,", _test("DELETE"));
    }

    @Test
    public void testGet() {
        assertResponseEquals("GET: test,GET: test,GET: test,", _test("GET"));
    }

    @Test
    public void testConflictingMethods() {
        Response cr = target("/").queryParam("_method", "PUT").request()
                .header("X-HTTP-Method-Override", "DELETE").post(Entity.text(""));
        assertEquals(400, cr.getStatus());
    }

    @Test
    public void testUnsupportedMethod() {
        assertResponseEquals("405,405,405,", _test("PATCH"));
    }

    @Test
    public void testGetWithQueryParam() {
        String result = target().queryParam("_method", "GET").queryParam("a", "test").request().post(null, String.class);
        assertEquals("GET: test", result);
    }

    @Test
    public void testGetWithOtherEntity() {
        String result = target().queryParam("_method", "GET").request().post(Entity.text("a=test"), String.class);
        assertEquals("GET: null", result);
    }

    @Test
    public void testPlainPost() {
        String result = target().request().post(null, String.class);
        assertEquals("POST", result);
    }

    public Response[] _test(String method) {
        Response[] result = new Response[3];
        WebTarget target = target();

        result[0] = target.request().header("X-HTTP-Method-Override", method)
                .post(Entity.form(new Form().param("a", "test")));
        result[1] = target.queryParam("_method", method).request()
                .post(Entity.form(new Form().param("a", "test")));
        result[2] = target.queryParam("_method", method).request().header("X-HTTP-Method-Override", method)
                .post(Entity.form(new Form().param("a", "test")));
        return result;
    }

    public void assertResponseEquals(String expected, Response[] responses) {
        StringBuilder result = new StringBuilder();

        for (Response r : responses) {
            if (r.getStatus() == Response.Status.OK.getStatusCode()) {
                result.append(r.readEntity(String.class));
            } else {
                result.append(r.getStatus());
            }
            result.append(",");
        }

        assertEquals(expected, result.toString());
    }
}
