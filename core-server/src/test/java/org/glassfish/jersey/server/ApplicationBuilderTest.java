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

import org.glassfish.hk2.Services;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestInvoker.InvocationContext;
import org.junit.Test;
import org.jvnet.hk2.annotations.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for creating an application with asynchronously handled request processing
 * via {@link ApplicationBuilder}'s programmatic API.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ApplicationBuilderTest {

    private static final URI BASE_URI = URI.create("http://localhost:8080/base/");

    private static class AsyncInflector implements Inflector<Request, Response> {

        @Inject
        private InvocationContext invocationContext;
        @Inject
        Services services;
        private final String responseContent;

        public AsyncInflector() {
            this.responseContent = "DEFAULT";
        }

        public AsyncInflector(String responseContent) {
            this.responseContent = responseContent;
        }

        @Override
        public Response apply(final Request req) {
            // Suspend current request
            invocationContext.suspend();

            Executors.newSingleThreadExecutor().submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace(System.err);
                    }

                    // Returning will enter the suspended request
                    invocationContext.resume(Responses.from(200, req).entity(responseContent).build());
                }
            });

            return null;
        }
    }

    public Application setupApplication1() {
        final Application.Builder af = Application.builder();
        af.bind("a/b/c").method("GET").to(new AsyncInflector("A-B-C"));
        af.bind("a/b/d").method("GET").to(new AsyncInflector("A-B-D"));
        return af.build();
    }

//    @Test
    public void testAsyncApp1() throws InterruptedException, ExecutionException {
        Request req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath() + "a/b/c"), "GET").build();

        Future<Response> res = setupApplication1().apply(req);

        assertEquals("A-B-C", res.get().getEntity());
    }

//    @Test
    public void testAsyncApp2() throws InterruptedException, ExecutionException {
        Request req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath() + "a/b/d"), "GET").build();

        Future<Response> res = setupApplication1().apply(req);

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
        final ResourceConfig resourceConfig = ResourceConfig.builder().addClasses(ResourceA.class).build();
        final Application application = Application.builder(resourceConfig).build();

        Request req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath()), "GET").build();

        assertEquals("get!", application.apply(req).get().getEntity());
    }

//    @Test
    public void testappBuilderJaxRsApplication() throws InterruptedException, ExecutionException {

        javax.ws.rs.core.Application jaxRsApplication = new javax.ws.rs.core.Application() {

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

        final ResourceConfig resourceConfig = ResourceConfig.from(jaxRsApplication);
        final Application application = Application.builder(resourceConfig).build();

        Request req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath()), "GET").build();

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
        public ResourceA readFrom(Class<ResourceA> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            return null;
        }
    }

//    @Test
    public void testJaxrsApplicationInjection() throws InterruptedException, ExecutionException {
        final ResourceConfig resourceConfig = ResourceConfig.builder()
                .addClasses(ResourceB.class)
                .addSingletons(new ResourceAReader())
                .build();

        final Application application = Application.builder(resourceConfig).build();

        Request req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath()), "GET").build();

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

//    @Test
    public void testDeploymentFailsForAmbiguousResource() {
        final ResourceConfig resourceConfig = ResourceConfig.builder()
                .addClasses(ErrornousResource.class)
                .build();
        try {
            Application.builder(resourceConfig).build();
            assertTrue("application builder should have failed", false);
        } catch (Exception e) {
        }
    }
}
