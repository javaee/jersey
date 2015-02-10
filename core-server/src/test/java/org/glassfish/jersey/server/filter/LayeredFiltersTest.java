/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import javax.annotation.Priority;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests layering of filters applied on appropriate methods (using named bindings) on resource method, sub-method,
 * sub-resource locator, sub-resource method. Jersey 2 does not support full functionality of Jersey 1 speaking about
 * filter layering. Jersey 2 implementation is JAX-RS compliant.
 * <p/>
 * But it could be implemented as Jersey specific extension - JERSEY-2414.
 * Please un-ignore tests whenever JERSEY-2414 fixed.
 *
 * @author Paul Sandoz
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class LayeredFiltersTest {

    @Path("/")
    public static class ResourceWithSubresourceLocator {
        @Path("sub")
        @One
        public Object get() {
            return new ResourceWithMethod();
        }
    }

    @Path("/")
    public static class ResourceWithMethod {
        @GET
        @Two
        public String get(@Context HttpHeaders hh) {
            List<String> xTest = hh.getRequestHeaders().get("X-TEST");
            assertEquals(2, xTest.size());
            return xTest.get(0) + xTest.get(1);
        }

        @GET
        @Path("submethod")
        @Two
        public String getSubmethod(@Context HttpHeaders hh) {
            List<String> xTest = hh.getRequestHeaders().get("X-TEST");
            assertEquals(2, xTest.size());
            return xTest.get(0) + xTest.get(1);
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    public @interface One {
    }

    @One
    @Priority(Priorities.USER + 1)
    public static class FilterOne implements ContainerRequestFilter, ContainerResponseFilter {
        public void filter(ContainerRequestContext requestContext) throws IOException {
            List<String> xTest = requestContext.getHeaders().get("X-TEST");
            assertNull(xTest);

            requestContext.getHeaders().add("X-TEST", "one");
        }

        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            List<Object> xTest = responseContext.getHeaders().get("X-TEST");
            assertEquals(1, xTest.size());
            assertEquals("two", xTest.get(0));

            responseContext.getHeaders().add("X-TEST", "one");
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Two {
    }

    @Two
    @Priority(Priorities.USER + 2)
    public static class FilterTwo implements ContainerRequestFilter, ContainerResponseFilter {
        public void filter(ContainerRequestContext requestContext) throws IOException {
            List<String> xTest = requestContext.getHeaders().get("X-TEST");
            assertNotNull("FilterOne not called", xTest);
            assertEquals(1, xTest.size());
            assertEquals("one", xTest.get(0));

            requestContext.getHeaders().add("X-TEST", "two");
        }

        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            List<Object> xTest = responseContext.getHeaders().get("X-TEST");
            assertNull(xTest);

            responseContext.getHeaders().add("X-TEST", "two");
        }
    }

    @Test
    @Ignore("JERSEY-2414 - not yet implemented")
    public void testResourceMethod() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceWithSubresourceLocator.class)
                .register(FilterOne.class).register(FilterTwo.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/sub", "GET").build()).get();

        assertEquals(200, response.getStatus());
        assertEquals("onetwo", response.getEntity());
        List<Object> xTest = response.getHeaders().get("X-TEST");
        assertEquals(2, xTest.size());
        assertEquals("two", xTest.get(0));
        assertEquals("one", xTest.get(1));
    }

    @Test
    @Ignore("JERSEY-2414 - not yet implemented")
    public void testResourceSubresourceMethod() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceWithSubresourceLocator.class)
                .register(FilterOne.class).register(FilterTwo.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/sub/submethod", "GET")
                .build()).get();

        assertEquals(200, response.getStatus());
        assertEquals("onetwo", response.getEntity());
        List<Object> xTest = response.getHeaders().get("X-TEST");
        assertEquals(2, xTest.size());
        assertEquals("two", xTest.get(0));
        assertEquals("one", xTest.get(1));
    }


    @Path("/")
    @One
    public static class ResourceWithSubresourceLocatorOnClass {
        @Path("sub")
        public Object get() {
            return new ResourceWithMethodOnClass();
        }
    }

    @Path("/")
    @Two
    public static class ResourceWithMethodOnClass {
        @GET
        public String get(@Context HttpHeaders hh) {
            List<String> xTest = hh.getRequestHeaders().get("X-TEST");
            assertEquals(2, xTest.size());
            return xTest.get(0) + xTest.get(1);
        }

        @GET
        @Path("submethod")
        public String getSubmethod(@Context HttpHeaders hh) {
            List<String> xTest = hh.getRequestHeaders().get("X-TEST");
            assertEquals(2, xTest.size());
            return xTest.get(0) + xTest.get(1);
        }
    }

    @Test
    @Ignore("JERSEY-2414 - not yet implemented")
    public void testResourceMethodOnClass() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceWithSubresourceLocatorOnClass.class)
                .register(FilterOne.class).register(FilterTwo.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/sub", "GET").build()).get();

        assertEquals(200, response.getStatus());
        assertEquals("onetwo", response.getEntity());
        List<Object> xTest = response.getHeaders().get("X-TEST");
        assertEquals(2, xTest.size());
        assertEquals("two", xTest.get(0));
        assertEquals("one", xTest.get(1));
    }

    @Test
    @Ignore("JERSEY-2414 - not yet implemented")
    public void testResourceSubresourceMethodOnClass() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceWithSubresourceLocatorOnClass.class)
                .register(FilterOne.class).register(FilterTwo.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/sub/submethod", "GET").build()).get();

        assertEquals(200, response.getStatus());
        assertEquals("onetwo", response.getEntity());
        List<Object> xTest = response.getHeaders().get("X-TEST");
        assertEquals(2, xTest.size());
        assertEquals("two", xTest.get(0));
        assertEquals("one", xTest.get(1));
    }

    @Path("/")
    public static class ResourceWithMethodMultiple {
        @GET
        @One
        @Two
        public String get(@Context HttpHeaders hh) {
            List<String> xTest = hh.getRequestHeaders().get("X-TEST");
            assertEquals(2, xTest.size());
            return xTest.get(0) + xTest.get(1);
        }

        @GET
        @Path("submethod")
        @One
        @Two
        public String getSubmethod(@Context HttpHeaders hh) {
            List<String> xTest = hh.getRequestHeaders().get("X-TEST");
            assertEquals(2, xTest.size());
            return xTest.get(0) + xTest.get(1);
        }
    }

    @Test
    public void testResourceMethodMultiple() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceWithMethodMultiple.class)
                .register(FilterOne.class).register(FilterTwo.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/", "GET").build()).get();

        assertEquals(200, response.getStatus());
        assertEquals("onetwo", response.getEntity());
        List<Object> xTest = response.getHeaders().get("X-TEST");
        assertEquals(2, xTest.size());
        assertEquals("two", xTest.get(0));
        assertEquals("one", xTest.get(1));
    }

    @Test
    public void testResourceSubresourceMethodMultiple() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceWithMethodMultiple.class)
                .register(FilterOne.class).register(FilterTwo.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/submethod", "GET").build()).get();

        assertEquals(200, response.getStatus());
        assertEquals("onetwo", response.getEntity());
        List<Object> xTest = response.getHeaders().get("X-TEST");
        assertEquals(2, xTest.size());
        assertEquals("two", xTest.get(0));
        assertEquals("one", xTest.get(1));
    }

}
