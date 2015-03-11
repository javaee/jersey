/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.inject.Singleton;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test sub resource locators returning {@link Resource programmatic resources}.
 *
 * @author Miroslav Fuksa
 *
 */
public class SubResourceProgrammaticTest {

    @Path("root")
    public static class RootResource {

        @Path("locator")
        public Resource subResourceLocator() {
            return getResource();
        }

        private Resource getResource() {
            final Resource.Builder builder = Resource.builder();
            builder.addMethod("GET").produces(MediaType.TEXT_PLAIN_TYPE).handledBy(
                    new Inflector<ContainerRequestContext, String>() {
                        @Override
                        public String apply(ContainerRequestContext containerRequestContext) {
                            return "inflector";
                        }
                    });
            return builder.build();
        }

        @Path("singleton")
        public Resource subResourceSingleton() {
            return Resource.from(SingletonResource.class);
        }

        @Path("standard")
        public Resource subResourceStandard() {
            return Resource.from(StandardResource.class);
        }

        @Path("standard-instance")
        public StandardResource subResourceStandardInstance() {
            return new StandardResource();
        }

        @Path("complex")
        public Resource complexSubResource() throws NoSuchMethodException {
            Resource.Builder builder = Resource.builder();
            builder.addChildResource("singleton-method").addMethod("GET").handledBy(SingletonResource.class,
                    SingletonResource.class.getDeclaredMethod("getCounter"));

            builder.addChildResource(Resource.from(SingletonChild.class));
            builder.addChildResource(Resource.from(StandardChild.class));
            builder.addMethod("GET").produces(MediaType.TEXT_PLAIN_TYPE).handledBy(
                    new Inflector<ContainerRequestContext, String>() {
                        @Override
                        public String apply(ContainerRequestContext containerRequestContext) {
                            return "inflector";
                        }
                    });
            return builder.build();
        }
    }

    @Singleton
    @Path("root-singleton")
    public static class SingletonResource {

        int counter = 0;

        @GET
        public String getCounter() {
            return "singleton:" + counter++;
        }

        @Path("iteration")
        public Resource subResourceSingleton() {
            return Resource.from(SingletonResource.class);
        }

        @Path("iteration-class")
        public Class<SingletonResource> subResourceSingletonClass() {
            return SingletonResource.class;
        }

        @Path("iteration-instance")
        public SingletonResource subResourceSingletonInstance() {
            return new SingletonResource();
        }

        @Path("iteration-standard")
        public Resource subResourceStandard() {
            return Resource.from(StandardResource.class);
        }
    }

    @Path("root-standard")
    public static class StandardResource {

        int counter = 0;

        @GET
        public String getCounter() {
            return "standard:" + counter++;
        }

        @Path("iteration")
        public Resource subResourceStandard() {
            return Resource.from(StandardResource.class);
        }

        @Path("iteration-singleton")
        public Resource subResourceSingleton() {
            return Resource.from(SingletonResource.class);
        }

    }

    @Path("singleton")
    public static class SingletonChild {

        @Path("/")
        public Resource locator() {
            return Resource.from(SingletonResource.class);
        }
    }

    @Path("standard")
    public static class StandardChild {

        @Path("/")
        public Resource locator() {
            return Resource.from(StandardResource.class);
        }
    }

    @Test
    public void testInflectorBased() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(RootResource.class));
        _test(handler, "/root/locator", "inflector");
    }

    @Test
    public void testSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(RootResource.class));
        _test(handler, "/root/singleton", "singleton:0");
        _test(handler, "/root/singleton", "singleton:1");
        _test(handler, "/root/singleton/iteration", "singleton:2");
    }

    @Test
    public void testSingleton2() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(RootResource.class, SingletonResource.class));
        _test(handler, "/root-singleton", "singleton:0");
        _test(handler, "/root/singleton", "singleton:1");
        _test(handler, "/root/singleton/iteration", "singleton:2");
        _test(handler, "/root-singleton/iteration", "singleton:3");
        _test(handler, "/root-singleton/iteration-class", "singleton:4");
        _test(handler, "/root-singleton/iteration-instance", "singleton:0");
    }

    @Test
    public void testStandard() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(RootResource.class, StandardResource.class));
        _test(handler, "/root-standard", "standard:0");
        _test(handler, "/root/standard", "standard:0");
        _test(handler, "/root/standard/iteration", "standard:0");
        _test(handler, "/root-standard/iteration", "standard:0");
        _test(handler, "/root/standard-instance", "standard:0");
    }

    @Test
    public void testComplex() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(RootResource.class));
        int singletonCounter = 0;
        _test(handler, "/root/complex/", "inflector");
        _test(handler, "/root/complex/singleton", "singleton:" + singletonCounter++);
        _test(handler, "/root/complex/singleton/iteration", "singleton:" + singletonCounter++);
        _test(handler, "/root/complex/singleton/iteration/iteration", "singleton:" + singletonCounter++);
        _test(handler, "/root/complex/singleton/iteration-class", "singleton:" + singletonCounter++);
        _test(handler, "/root/complex/singleton/iteration-instance", "singleton:0");
        _test(handler, "/root/complex/standard", "standard:0");
        _test(handler, "/root/complex/standard/iteration", "standard:0");
        _test(handler, "/root/complex/singleton-method/", "singleton:" + singletonCounter++);

        _test(handler, "/root/complex/singleton/iteration/iteration-standard", "standard:0");
        _test(handler, "/root/complex/standard/iteration/iteration-singleton", "singleton:" + singletonCounter++);
        _test(handler, "/root/complex/standard/iteration/iteration-singleton/iteration/iteration-standard", "standard:0");
        _test(handler, "/root/complex/standard/iteration/iteration-singleton/iteration/iteration-standard/iteration",
                "standard:0");
        _test(handler, "/root/complex/standard/iteration/iteration-singleton/iteration/iteration-standard/iteration"
                + "/iteration-singleton/iteration-class", "singleton:" + singletonCounter++);
        _test(handler, "/root/complex/standard/iteration/iteration-singleton/iteration/iteration-standard/iteration"
                + "/iteration-singleton/iteration-class/iteration-instance", "singleton:0");
        _test(handler, "/root/complex/standard/iteration/iteration-singleton", "singleton:" + singletonCounter++);

        _test(handler, "/root/complex/singleton-method/", "singleton:" + singletonCounter++);
        _test(handler, "/root/complex/singleton", "singleton:" + singletonCounter++);
        _test(handler, "/root/complex", "inflector");
    }

    private void _test(ApplicationHandler handler, String requestUri, String expected)
            throws InterruptedException, ExecutionException {
        final ContainerResponse response = handler.apply(RequestContextBuilder.from(requestUri, "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals(expected, response.getEntity());
    }

    @Test
    public void testInvalidSubResource() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(new ResourceConfig(WrongResource.class));
        _test(handler, "/wrong", "ok");
        try {
            final ContainerResponse response = handler.apply(RequestContextBuilder.from("/wrong/locator", "GET").build()).get();
            assertEquals(500, response.getStatus());
            fail("Should throw exception caused by validation errors of Sub Resource.");
        } catch (Throwable e) {
            // ok - Should throw exception caused by validation errors of Sub Resource.
        }
    }

    @Path("wrong")
    public static class WrongResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "ok";
        }

        @Path("locator")
        public Resource locator() {
            return Resource.from(InvalidSubResource.class);
        }
    }

    /**
     * Invalid resource.
     */
    public static class InvalidSubResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get1() {
            return "get1";
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get2() {
            return "get2";
        }

    }
}
