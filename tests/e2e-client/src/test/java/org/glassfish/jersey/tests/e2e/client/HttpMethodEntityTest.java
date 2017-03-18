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

import java.util.concurrent.Future;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Tests HTTP methods and entity presence.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class HttpMethodEntityTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testGet() {
        _test("GET", true, true);
        _test("GET", false, false);
    }

    @Test
    public void testPost() {
        _test("POST", true, false);
        _test("POST", false, false);
    }

    @Test
    public void testPut() {
        _test("PUT", true, false);
        _test("PUT", false, true);
    }

    @Test
    public void testDelete() {
        _test("DELETE", true, true);
        _test("DELETE", false, false);
    }

    @Test
    public void testHead() {
        _test("HEAD", true, true);
        _test("HEAD", false, false);
    }

    @Test
    public void testOptions() {
        _test("OPTIONS", true, true);
        _test("OPTIONS", false, false);
    }

    /**
     * Reproducer for JERSEY-2370: Sending POST without body.
     */
    @Test
    public void testEmptyPostWithoutContentType() {
        final WebTarget resource = target().path("resource");
        try {
            final Future<Response> future = resource.request().async().post(null);
            assertEquals(200, future.get().getStatus());

            final Response response = resource.request().post(null);
            assertEquals(200, response.getStatus());
        } catch (Exception e) {
            fail("Sending POST method without entity should not fail.");
        }
    }

    /**
     * Reproducer for JERSEY-2370: Sending POST without body.
     */
    @Test
    public void testEmptyPostWithContentType() {
        final WebTarget resource = target().path("resource");
        try {
            final Future<Response> future = resource.request().async().post(Entity.entity(null, "text/plain"));
            assertEquals(200, future.get().getStatus());

            final Response response = resource.request().post(Entity.entity(null, "text/plain"));
            assertEquals(200, response.getStatus());
        } catch (Exception e) {
            fail("Sending POST method without entity should not fail.");
        }
    }

    public void _test(String method, boolean entityPresent, boolean shouldFail) {
        Entity entity = entityPresent ? Entity.entity("entity", MediaType.TEXT_PLAIN_TYPE) : null;
        _testSync(method, entity, shouldFail);
        _testAsync(method, entity, shouldFail);
    }

    public void _testAsync(String method, Entity entity, boolean shouldFail) {
        try {
            final Future<Response> future = target().path("resource").request().async().method(method, entity);
            if (shouldFail) {
                fail("The method should fail.");
            }
            assertEquals(200, future.get().getStatus());
        } catch (Exception e) {
            if (!shouldFail) {
                fail("The method " + method + " with entity=" + (entity != null) + " should not fail.");
            }
        }
    }

    public void _testSync(String method, Entity entity, boolean shouldFail) {
        try {
            final Response response = target().path("resource").request().method(method, entity);
            assertEquals(200, response.getStatus());
            if (shouldFail) {
                fail("The method should fail.");
            }
        } catch (Exception e) {
            if (!shouldFail) {
                fail("The method " + method + " with entityPresent=" + (entity != null) + " should not fail.");
            }
        }
    }

    @Path("resource")
    public static class Resource {

        @Context
        HttpHeaders httpHeaders;

        @GET
        public String get() {
            return "get";
        }

        @POST
        public String post(String str) {
            // See JERSEY-1455
            assertFalse(httpHeaders.getRequestHeaders().containsKey(HttpHeaders.CONTENT_ENCODING));
            assertFalse(httpHeaders.getRequestHeaders().containsKey(HttpHeaders.CONTENT_LANGUAGE));

            return "post";
        }

        @PUT
        public String put(String str) {
            return "put";
        }

        @HEAD
        public String head() {
            return "head";
        }

        @DELETE
        public String delete() {
            return "delete";
        }
    }
}
