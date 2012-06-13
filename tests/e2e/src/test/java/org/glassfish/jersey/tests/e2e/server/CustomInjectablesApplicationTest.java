/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import javax.inject.Qualifier;

import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerException;

import org.glassfish.hk2.BinderFactory;
import org.glassfish.hk2.DynamicBinderFactory;
import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class CustomInjectablesApplicationTest extends JerseyTest {

    public static class MyApplication extends Application {

        public static Set<Class<?>> classes = new HashSet<Class<?>>() {{
            add(Resource.class);
        }};

        public MyApplication(@Inject Services services) {
            System.out.println("Registering injectables...");

            final DynamicBinderFactory dynamicBinderFactory = services.bindDynamically();

            // request scope binding
            dynamicBinderFactory.bind(MyInjectablePerRequest.class).to(MyInjectablePerRequest.class).in(RequestScope.class);
            // singleton binding
            dynamicBinderFactory.bind().to(MyInjectableSingleton.class).in(org.glassfish.hk2.scopes.Singleton.class);
            // singleton instance binding
            dynamicBinderFactory.bind().toInstance(new MyInjectableSingleton());
            // request scope binding with specified custom annotation
            dynamicBinderFactory.bind().annotatedWith(MyAnnotation.class).to(MyInjectablePerRequest.class).in(RequestScope.class);

            // commits changes
            dynamicBinderFactory.commit();
        }

        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }
    }

    public static class MyInjectablePerRequest {
        public int i = 0;
    }

    @Scoped(Singleton.class)
    public static class MyInjectableSingleton {
        public int i = 0;
    }

    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @Qualifier
    public static @interface MyAnnotation {

    }

    @Path("/")
    public static class Resource {
        @Inject
        MyInjectablePerRequest myInjectablePerRequest;

        @Inject
        MyInjectableSingleton myInjectableSingleton;

        @MyAnnotation
        MyInjectablePerRequest myInjectablePerRequest2;

        @GET
        @Path("/perrequest")
        public String getAndIncPerRequest() {
            return Integer.valueOf(++myInjectablePerRequest.i).toString();
        }

        @GET
        @Path("/perrequestCustomAnnotation")
        public String getAndIncPerRequest2() {
            return Integer.valueOf(++myInjectablePerRequest2.i).toString();
        }

        @GET
        @Path("/singleton")
        @Produces("text/plain")
        public String getAndIncSingleton() {
            System.out.println(myInjectableSingleton);
            return Integer.valueOf(++myInjectableSingleton.i).toString();
        }
    }

    public CustomInjectablesApplicationTest() throws TestContainerException {
        super(MyApplication.class);
    }

    @Test
    public void testPerRequest() throws Exception {
        final javax.ws.rs.client.Target perrequest = target().path("perrequest");

        assertEquals("1", perrequest.request().get(String.class));
        assertEquals("1", perrequest.request().get(String.class));
        assertEquals("1", perrequest.request().get(String.class));
    }

    @Test
    public void testSingleton() throws Exception {
        final javax.ws.rs.client.Target perrequest = target().path("singleton");

        assertEquals("1", perrequest.request().get(String.class));
        assertEquals("2", perrequest.request().get(String.class));
        assertEquals("3", perrequest.request().get(String.class));
    }

    @Test
    @Ignore
    public void testCustomAnnotation() throws Exception {
        final javax.ws.rs.client.Target perrequestCustomAnnotation = target().path("perrequestCustomAnnotation");

        assertEquals("1", perrequestCustomAnnotation.request().get(String.class));
        assertEquals("1", perrequestCustomAnnotation.request().get(String.class));
        assertEquals("1", perrequestCustomAnnotation.request().get(String.class));
    }

    @Test
    public void plainHK2Test() throws Exception {
        final Services services = HK2.get().create(null, new RequestScope.Module(), new Module() {
            @Override
            public void configure(BinderFactory binderFactory) {
                binderFactory.bind(MyInjectablePerRequest.class).to(MyInjectablePerRequest.class).in(RequestScope.class);
                binderFactory.bind().to(MyInjectableSingleton.class).in(org.glassfish.hk2.scopes.Singleton.class);
            }
        });

        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        final MyInjectableSingleton myInjectableSingleton = services.forContract(MyInjectableSingleton.class).get();
        assertEquals(myInjectableSingleton, services.forContract(MyInjectableSingleton.class).get());

        final MyInjectablePerRequest myInjectablePerRequest = requestScope.runInScope(new Callable<MyInjectablePerRequest>() {

            @Override
            public MyInjectablePerRequest call() throws Exception {
                final MyInjectablePerRequest myInjectablePerRequest = services.forContract(MyInjectablePerRequest.class).get();
                assertEquals(myInjectablePerRequest, services.forContract(MyInjectablePerRequest.class).get());
                return myInjectablePerRequest;
            }
        });

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {
                assertNotSame(myInjectablePerRequest, services.forContract(MyInjectablePerRequest.class).get());
            }
        });

    }

    @Test
    public void plainHK2DynamicTest() throws Exception {
        final Services services = HK2.get().create(null, new RequestScope.Module());

        final DynamicBinderFactory dynamicBinderFactory = services.bindDynamically();
        dynamicBinderFactory.bind(MyInjectablePerRequest.class).to(MyInjectablePerRequest.class).in(RequestScope.class);
        dynamicBinderFactory.bind().to(MyInjectableSingleton.class).in(org.glassfish.hk2.scopes.Singleton.class);
        dynamicBinderFactory.commit();

        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        final MyInjectableSingleton myInjectableSingleton = services.forContract(MyInjectableSingleton.class).get();
        assertEquals(myInjectableSingleton, services.forContract(MyInjectableSingleton.class).get());

        final MyInjectablePerRequest myInjectablePerRequest = requestScope.runInScope(new Callable<MyInjectablePerRequest>() {
            @Override
            public MyInjectablePerRequest call() throws Exception {
                final MyInjectablePerRequest myInjectablePerRequest = services.forContract(MyInjectablePerRequest.class).get();
                assertEquals(myInjectablePerRequest, services.forContract(MyInjectablePerRequest.class).get());
                return myInjectablePerRequest;
            }
        });

        requestScope.runInScope(new Runnable() {
            @Override
            public void run() {
                assertNotSame(myInjectablePerRequest, services.forContract(MyInjectablePerRequest.class).get());
            }
        });
    }
}
