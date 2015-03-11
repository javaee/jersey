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

package org.glassfish.jersey.server.internal.inject;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.glassfish.jersey.server.ContainerResponse;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Miroslav Fuksa
 *
 */
public class PathParamDefaultValueTest extends AbstractTest {


    @Test
    public void testStandardPathParamValueFoo() throws ExecutionException, InterruptedException {
        initiateWebApplication(FooResource.class);

        ContainerResponse response = getResponseContext("/foo/bar/test");
        assertEquals("test", response.getEntity());
    }

    @Test
    public void testDefaultPathParamValueFoo() throws ExecutionException, InterruptedException {
        initiateWebApplication(FooResource.class);

        ContainerResponse response = getResponseContext("/foo");
        assertEquals("default-id", response.getEntity());
    }

    @Test
    public void testDefaultPathParamValueOnResource1() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource.class);

        ContainerResponse response = getResponseContext("/foo");
        assertEquals("default-id", response.getEntity());
    }


    @Test
    public void testDefaultPathParamValueOnResource2() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource.class);

        ContainerResponse response = getResponseContext("/foo/bar/aaa");
        assertEquals("aaa", response.getEntity());
    }

    @Test
    public void testCallWithMissingPathParam404() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource.class);

        ContainerResponse response = getResponseContext("/foo/bar");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDefaultPathParamInSubResource() throws ExecutionException, InterruptedException {
        initiateWebApplication(FooResource.class);

        ContainerResponse response = getResponseContext("/foo/baz/sub");
        assertEquals(200, response.getStatus());
        assertEquals("default-id", response.getEntity());
    }


    @Test
    public void testParamInSubResource() throws ExecutionException, InterruptedException {
        initiateWebApplication(FooResource.class);

        ContainerResponse response = getResponseContext("/foo/baz/iddd");
        assertEquals(200, response.getStatus());
        assertEquals("iddd", response.getEntity());
    }


    @Test
    public void testDefaultPathParamValueOnAnotherResource1() throws ExecutionException, InterruptedException {
        initiateWebApplication(AnotherResource.class);

        ContainerResponse response = getResponseContext("/foo/test/bar/barrr");
        assertEquals("test:barrr", response.getEntity());
    }


    @Test
    public void testDefaultPathParamValueOnAnotherResource2() throws ExecutionException, InterruptedException {
        initiateWebApplication(AnotherResource.class);

        ContainerResponse response = getResponseContext("/foo");
        assertEquals("default-id:default-bar", response.getEntity());
    }


    @Test
    public void testDefaultPathParamValueOnAnotherResource3() throws ExecutionException, InterruptedException {
        initiateWebApplication(AnotherResource.class);

        ContainerResponse response = getResponseContext("/foo/test");
        assertEquals("test:default-bar", response.getEntity());
    }

    @Path("foo")
    public static class FooResource {
        @PathParam("id")
        @DefaultValue("default-id")
        String id;

        @GET
        public String getFoo() {
            return id;
        }

        @GET
        @Path("bar/{id}")
        public String getBar() {
            return id;
        }

        @Path("baz/{id}")
        public Resource getResource() {
            return new Resource();
        }

        @Path("baz/sub")
        public Resource getResource2() {
            return new Resource();
        }

    }

    @Path("foo")
    public static class Resource {
        @GET
        public String getFoo(@PathParam("id") @DefaultValue("default-id") String id) {
            return id;
        }

        @GET
        @Path("bar/{id}")
        public String getBar(@PathParam("id") @DefaultValue("default-id") String id) {
            return id;
        }
    }

    @Path("foo")
    public static class AnotherResource {
        @PathParam("bar")
        @DefaultValue("default-bar")
        String bar;

        @GET
        public String getFoo(@PathParam("id") @DefaultValue("default-id") String id) {
            return id + ":" + bar;
        }

        @GET
        @Path("{id}")
        public String getBar(@PathParam("id") @DefaultValue("default-id") String id) {
            return id + ":" + bar;
        }

        @GET
        @Path("{id}/bar/{bar}")
        public String getBarBar(@PathParam("id") @DefaultValue("default-id") String id) {
            return id + ":" + bar;
        }
    }
}
