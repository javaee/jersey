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

package org.glassfish.jersey.server.filter;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import javax.annotation.Priority;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests calling {@link ContainerRequestContext#setMethod(String)} in different request/response phases.
 *
 * @author Miroslav Fuksa
 */
public class FilterSetMethodTest {

    @Test
    public void testResponseFilter() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(Resource.class, ResponseFilter.class));
        ContainerResponse res = handler.apply(RequestContextBuilder.from("", "/resource/setMethod", "GET").build()).get();
        assertEquals(200, res.getStatus());
    }

    @Test
    public void testPreMatchingFilter() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(Resource.class, PreMatchFilter.class));
        ContainerResponse res = handler.apply(RequestContextBuilder.from("", "/resource/setMethod", "GET").build()).get();
        assertEquals(200, res.getStatus());
    }

    @Test
    public void testPostMatchingFilter() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(Resource.class, PostMatchFilter.class));
        ContainerResponse res = handler.apply(RequestContextBuilder.from("", "/resource/setMethod", "GET").build()).get();
        assertEquals(200, res.getStatus());
    }

    @Test
    public void testResource() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(Resource.class, PostMatchFilter.class));
        ContainerResponse res = handler.apply(RequestContextBuilder.from("", "/resource/setMethodInResource",
                "GET").build()).get();
        assertEquals(200, res.getStatus());
    }

    @Test
    public void testSubResourceLocator() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(AnotherResource.class));
        ContainerResponse res = handler.apply(RequestContextBuilder.from("", "/another/locator",
                "GET").build()).get();
        assertEquals(200, res.getStatus());
    }

    @Test
    public void testResourceUri() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(ResourceChangeUri.class,
                PreMatchChangingUriFilter.class));
        ContainerResponse res = handler.apply(RequestContextBuilder.from("", "/resourceChangeUri/first",
                "GET").build()).get();
        assertEquals(200, res.getStatus());
        assertEquals("ok", res.getEntity());
    }

    @Path("resourceChangeUri")
    public static class ResourceChangeUri {

        @Path("first")
        @GET
        public String first() {
            fail("should not be called.");
            return "fail";
        }

        @Path("first/a")
        @GET
        public String a() {
            return "ok";
        }
    }

    @Provider
    @Priority(500)
    @PreMatching
    public static class PreMatchChangingUriFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            final URI requestUri = requestContext.getUriInfo().getRequestUriBuilder().path("a").build();
            requestContext.setRequestUri(requestUri);
        }
    }


    @Provider
    @Priority(500)
    public static class ResponseFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            checkExceptionThrown(new SetMethodClosure(requestContext));
            checkExceptionThrown(new SetUriClosure(requestContext));
        }
    }


    @Provider
    @Priority(500)
    @PreMatching
    public static class PreMatchFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            new SetMethodClosure(requestContext).f();
            new SetUriClosure(requestContext).f();
            // Should not throw IllegalArgumentException exception in pre match filter.
        }
    }

    @Provider
    @Priority(500)
    public static class PostMatchFilter implements ContainerRequestFilter {
        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            checkExceptionThrown(new SetMethodClosure(requestContext));
            checkExceptionThrown(new SetUriClosure(requestContext));
        }
    }

    @Path("resource")
    public static class Resource {
        @GET
        @Path("setMethod")
        public Response setMethod() {
            Response response = Response.ok().build();
            return response;
        }

        @GET
        @Path("setMethodInResource")
        public Response setMethodInResource(@Context ContainerRequestContext request) {
            checkExceptionThrown(new SetMethodClosure(request));
            checkExceptionThrown(new SetUriClosure(request));
            return Response.ok().build();
        }


    }

    @Path("another")
    public static class AnotherResource {

        @Path("locator")
        public SubResource methodInSubResource(@Context ContainerRequestContext request) {
            checkExceptionThrown(new SetMethodClosure(request));
            checkExceptionThrown(new SetUriClosure(request));
            return new SubResource();
        }

        public static class SubResource {
            @GET
            public Response get(@Context ContainerRequestContext request) {
                checkExceptionThrown(new SetMethodClosure(request));
                checkExceptionThrown(new SetUriClosure(request));
                return Response.ok().build();
            }
        }
    }


    public static interface Closure {
        void f();
    }

    private static void checkExceptionThrown(Closure f) {
        try {
            f.f();
            fail("Should throw IllegalArgumentException exception.");
        } catch (IllegalStateException exception) {
            // ok - should throw IllegalArgumentException
        }
    }

    public static class SetMethodClosure implements Closure {
        final ContainerRequestContext requestContext;

        public SetMethodClosure(ContainerRequestContext requestContext) {
            this.requestContext = requestContext;
        }

        @Override
        public void f() {
            requestContext.setMethod("OPTIONS");
        }

    }

    public static class SetUriClosure implements Closure {
        final ContainerRequestContext requestContext;

        public SetUriClosure(ContainerRequestContext requestContext) {
            this.requestContext = requestContext;
        }

        @Override
        public void f() {
            requestContext.setRequestUri(requestContext.getUriInfo().getRequestUri());
        }

    }

}
