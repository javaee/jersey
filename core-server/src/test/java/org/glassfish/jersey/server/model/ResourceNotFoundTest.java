/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ExecutionException;

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

/**
 * test for JERSEY-938
 *
 * @author Jakub Podlesak
 */
public class ResourceNotFoundTest {

    ApplicationHandler application;

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }


    public static class MyInflector implements Inflector<ContainerRequestContext, Response> {
        @Override
        public Response apply(ContainerRequestContext data) {
            return Response.ok("dynamic", MediaType.TEXT_PLAIN).build();
        }
    }

    @Path("/foo")
    public static class FooResource {
        @Produces("text/plain")
        @GET
        public String getFoo() {
            return "foo";
        }

        @Path("bar")
        @Produces("text/plain")
        @GET
        public String getBar() {
            return "bar";
        }

        @Path("content-type")
        @GET
        public Response getSpecialContentType() {
            return Response.status(Response.Status.NOT_FOUND).type("application/something").build();
        }

    }

    @Test
    public void testExistingDeclarativeResources() throws Exception {
        ApplicationHandler app = createApplication(FooResource.class);

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/foo", "GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("foo", response.getEntity());

        response = app.apply(RequestContextBuilder.from("/foo/bar", "GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.getEntity());
    }

    @Test
    public void testMissingDeclarativeResources() throws Exception {
        ApplicationHandler app = createApplication(FooResource.class);

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/foe", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(RequestContextBuilder.from("/fooe", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(RequestContextBuilder.from("/foo/baz", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(RequestContextBuilder.from("/foo/bar/baz", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());
    }

    private ApplicationHandler createMixedApp() {
        ResourceConfig rc = new ResourceConfig(FooResource.class);

        Resource.Builder rb;

        rb = Resource.builder("/dynamic");
        rb.addMethod("GET").handledBy(new MyInflector());
        rc.registerResources(rb.build());

        rb = Resource.builder("/foo/dynamic");
        rb.addMethod("GET").handledBy(new MyInflector());
        rc.registerResources(rb.build());

        return new ApplicationHandler(rc);
    }

    @Test
    public void testExistingMixedResources() throws Exception {

        ApplicationHandler app = createMixedApp();

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/foo", "GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("foo", response.getEntity());

        response = app.apply(RequestContextBuilder.from("/dynamic", "GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("dynamic", response.getEntity());

        response = app.apply(RequestContextBuilder.from("/foo/bar", "GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.getEntity());

        response = app.apply(RequestContextBuilder.from("/foo/dynamic", "GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("dynamic", response.getEntity());
    }


    @Test
    public void testMissingMixedResources() throws Exception {

        ApplicationHandler app = createMixedApp();

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/foe", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(RequestContextBuilder.from("/fooe", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(RequestContextBuilder.from("/dynamical", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(RequestContextBuilder.from("/foo/baz", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(RequestContextBuilder.from("/foo/bar/baz", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(RequestContextBuilder.from("/foo/dynamic/baz", "GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testCustomContentTypeAndNoEntity() throws ExecutionException, InterruptedException {
        ApplicationHandler app = createApplication(FooResource.class);
        final ContainerResponse response = app.apply(RequestContextBuilder.from("/foo/content-type", "GET")
                .build()).get();
        assertEquals(404, response.getStatus());
        assertEquals("application/something", response.getMediaType().toString());
    }
}