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

package org.glassfish.jersey.server.model;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Tests whether providers are correctly validated in the server runtime (for example if provider constrained to
 * client runtime is skipped on the server).
 * @author Miroslav Fuksa
 *
 */
public class ConstrainedToServerTest {


    @Test
    public void testFiltersAnnotated() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(MyServerFilter.class, MyClientFilter.class,
                MyServerWrongFilter.class, MyServerFilterWithoutConstraint.class, Resource.class);
        resourceConfig.registerInstances(new MyServerWrongFilter2(), new MyServerFilter2());
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals("called", response.getHeaderString("MyServerFilter"));
        assertEquals("called", response.getHeaderString("MyServerFilter2"));
        assertEquals("called", response.getHeaderString("MyServerFilterWithoutConstraint"));
    }

    @Test
    public void testMyClientUnConstrainedFilter() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(MyClientUnConstrainedFilter.class, Resource.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testResourceAndProviderConstrainedToClient() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceAndProviderConstrainedToClient.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource-and-provider",
                "GET").build()).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testResourceAndProviderConstrainedToServer() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceAndProviderConstrainedToServer.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource-and-provider-server",
                "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("called", response.getHeaderString("ResourceAndProviderConstrainedToServer"));
    }

    @Test
    public void testClientAndServerProvider() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, MyServerAndClientFilter.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("called", response.getHeaderString("MyServerAndClientFilter"));
    }

    @Test
    public void testClientAndServerProvider2() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, MyServerAndClientContrainedToClientFilter.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testSpecificApplication() throws ExecutionException, InterruptedException {
        Application app = new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                final HashSet<Class<?>> classes = Sets.newHashSet();
                classes.add(Resource.class);
                classes.add(MyClientFilter.class);
                classes.add(MyServerWrongFilter.class);
                return classes;
            }
        };
        ApplicationHandler handler = new ApplicationHandler(app);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
    }

    @ConstrainedTo(RuntimeType.SERVER)
    public static class MyServerFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("MyServerFilter", "called");
        }
    }

    public static class MyServerFilterWithoutConstraint implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("MyServerFilterWithoutConstraint", "called");
        }
    }

    @ConstrainedTo(RuntimeType.SERVER)
    public static class MyServerFilter2 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("MyServerFilter2", "called");
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class MyServerWrongFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            fail("This filter should never be called.");
        }
    }


    @ConstrainedTo(RuntimeType.SERVER)
    public static class MyServerAndClientFilter implements ContainerResponseFilter, ClientResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("MyServerAndClientFilter", "called");
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class MyServerAndClientContrainedToClientFilter implements ContainerResponseFilter, ClientResponseFilter,
            MessageBodyWriter<String> {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            fail("This MyServerAndClientContrainedToClientFilter filter should never be called.");
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return false;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return 0;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
                WebApplicationException {
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class MyServerWrongFilter2 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            fail("This filter should never be called.");
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class MyClientFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            fail("This filter should never be called.");
        }
    }


    public static class MyClientUnConstrainedFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            fail("This filter should never be called.");
        }
    }

    @Path("resource")
    public static class Resource {

        @GET
        public String get() {
            return "get";
        }
    }

    @Path("resource-and-provider")
    @ConstrainedTo(RuntimeType.CLIENT)
    public static class ResourceAndProviderConstrainedToClient implements ContainerResponseFilter {

        @GET
        public Response get() {
            return Response.ok().entity("ok").build();
        }

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            fail("This filter method should never be called.");
        }
    }

    @Path("resource-and-provider-server")
    @ConstrainedTo(RuntimeType.SERVER)
    public static class ResourceAndProviderConstrainedToServer implements ContainerResponseFilter {

        @GET
        public Response get() {
            return Response.ok().entity("ok").build();
        }


        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("ResourceAndProviderConstrainedToServer", "called");
        }
    }
}
