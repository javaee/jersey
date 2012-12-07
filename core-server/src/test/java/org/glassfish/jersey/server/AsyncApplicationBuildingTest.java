/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.internal.process.AsyncContext;
import org.glassfish.jersey.server.model.Resource;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for creating an application with asynchronously handled request processing
 * via {@link Resource}'s programmatic API.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class AsyncApplicationBuildingTest {

    private static final String BASE_URI = "http://localhost:8080/base/";

    private static class AsyncInflector implements Inflector<ContainerRequestContext, Response> {

        @Inject
        private Provider<AsyncContext> asyncContextProvider;
        private final String responseContent;

        public AsyncInflector() {
            this.responseContent = "DEFAULT";
        }

        public AsyncInflector(String responseContent) {
            this.responseContent = responseContent;
        }

        @Override
        public Response apply(final ContainerRequestContext req) {
            // Suspend current request
            final AsyncContext asyncContext = asyncContextProvider.get();
            asyncContext.suspend();

            Executors.newSingleThreadExecutor().submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace(System.err);
                    }

                    // Returning will enter the suspended request
                    asyncContext.resume(Response.ok().entity(responseContent).build());
                }
            });

            return null;
        }
    }

    public ApplicationHandler setupApplication1() {
        final ResourceConfig rc = new ResourceConfig();

        Resource.Builder rb;

        rb = Resource.builder("a/b/c");
        rb.addMethod("GET").handledBy(new AsyncInflector("A-B-C"));
        rc.registerResources(rb.build());

        rb = Resource.builder("a/b/d");
        rb.addMethod("GET").handledBy(new AsyncInflector("A-B-D"));
        rc.registerResources(rb.build());

        return new ApplicationHandler(rc);
    }

    @Test
    public void testAsyncApp1() throws InterruptedException, ExecutionException {
        ContainerRequest req = RequestContextBuilder.from(
                BASE_URI, BASE_URI + "a/b/c", "GET").build();

        Future<ContainerResponse> res = setupApplication1().apply(req);

        assertEquals("A-B-C", res.get().getEntity());
    }

    @Test
    public void testAsyncApp2() throws InterruptedException, ExecutionException {
        ContainerRequest req = RequestContextBuilder.from(
                BASE_URI, BASE_URI + "a/b/d", "GET").build();

        Future<ContainerResponse> res = setupApplication1().apply(req);

        assertEquals("A-B-D", res.get().getEntity());
    }

    @Path("/")
    public static class ResourceA {

        @GET
        public String get() {
            return "get!";
        }
    }

    @Test
    public void testappBuilderClasses() throws InterruptedException, ExecutionException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        ContainerRequest req = RequestContextBuilder.from(BASE_URI, BASE_URI, "GET").build();

        assertEquals("get!", application.apply(req).get().getEntity());
    }

    @Test
    public void testEmptyAppCreationPasses() throws InterruptedException, ExecutionException {
        final ResourceConfig resourceConfig = new ResourceConfig();
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        ContainerRequest req = RequestContextBuilder.from(BASE_URI, BASE_URI, "GET").build();

        assertEquals(404, application.apply(req).get().getStatus());
    }

    @Test
    public void testAppBuilderJaxRsApplication() throws InterruptedException, ExecutionException {
        Application jaxRsApplication = new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                HashSet<Class<?>> set = new HashSet<Class<?>>();
                set.add(ResourceA.class);
                return set;
            }

            @Override
            public Set<Object> getSingletons() {
                return super.getSingletons();
            }
        };

        final ApplicationHandler application = new ApplicationHandler(jaxRsApplication);

        ContainerRequest req = RequestContextBuilder.from(BASE_URI, BASE_URI, "GET").build();

        assertEquals("get!", application.apply(req).get().getEntity());
    }

    @Path("/")
    public static class ResourceB {

        @Context
        javax.ws.rs.core.Application application;

        @GET
        public String get() {
            assertTrue(application != null);
            assertTrue(application.getClasses().contains(ResourceB.class));
            assertTrue(application.getSingletons().size() > 0);
            assertTrue(application.getSingletons().iterator().next().getClass().equals(ResourceAReader.class));
            return "get!";
        }
    }

    public static class ResourceAReader implements MessageBodyReader<ResourceA> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return false;
        }

        @Override
        public ResourceA readFrom(Class<ResourceA> type, Type genericType, Annotation[] annotations,
                MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            return null;
        }
    }

    @Test
    public void testJaxrsApplicationInjection() throws InterruptedException, ExecutionException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceB.class)
                .registerInstances(new ResourceAReader());

        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        ContainerRequest req = RequestContextBuilder.from(BASE_URI, BASE_URI, "GET").build();

        assertEquals("get!", application.apply(req).get().getEntity());
    }

    @Path("/")
    @Consumes("text/plain")
    public static class ErrornousResource {

        @POST
        @Produces("text/plain")
        public String postOne(String s) {
            return "One";
        }

        @POST
        @Produces("text/plain")
        public String postTwo(String s) {
            return "Two";
        }
    }

    @Test
    public void testDeploymentFailsForAmbiguousResource() {
        final ResourceConfig resourceConfig = new ResourceConfig(ErrornousResource.class);
        try {
            ApplicationHandler server = new ApplicationHandler(resourceConfig);
            assertTrue("Jersey server initialization should have failed: " + server, false);
        } catch (Exception e) {
        }
    }
}
