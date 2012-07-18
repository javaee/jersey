/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicBinder;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.InvokerBuilder;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ServerBinder;
import org.glassfish.jersey.server.internal.routing.RuntimeModelBuilder;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

/**
 * Runtime model builder test.
 *
 * @author Jakub Podlesak
 */
public class RMBuilderTest {

    private static final URI BASE_URI = URI.create("http://localhost:8080/base/");

    @Path("/helloworld")
    public static class HelloWorldResource {

        @GET
        @Produces("text/plain")
        public String getHello() {
            return "hello";
        }

        @OPTIONS
        @Produces("text/plain")
        public String getOptions() {
            return "GET";
        }

        @GET
        @Path("another/{b}")
        @Produces("text/plain")
        public String getAnother() {
            return "another";
        }
    }
    private RequestInvoker<ContainerRequest, ContainerResponse> invoker; // will be manually injected in the setupApplication()
    private RequestScope requestScope; // will be manually injected in the setupApplication()

    @Before
    public void setupApplication() {
        ServiceLocator locator = Injections.createLocator(new ServerBinder(), new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(HelloWorldResource.class);
            }
        });

        final Ref<MessageBodyWorkers> workers = locator.getService((new TypeLiteral<Ref<MessageBodyWorkers>>(){}).getType());
        workers.set(new MessageBodyFactory(locator));
        final Ref<ExceptionMappers> mappers = locator.getService((new TypeLiteral<Ref<ExceptionMappers>>(){}).getType());
        mappers.set(new ExceptionMapperFactory(locator));

        locator.inject(this);

        final RuntimeModelBuilder runtimeModelBuilder = locator.getService(RuntimeModelBuilder.class);
        runtimeModelBuilder.setGlobalInterceptors(new HashSet<ReaderInterceptor>(), new HashSet<WriterInterceptor>());
        runtimeModelBuilder.setBoundProviders(
                new MultivaluedHashMap<Class<? extends Annotation>, ContainerRequestFilter>(),
                new MultivaluedHashMap<Class<? extends Annotation>, ContainerResponseFilter>(),
                new MultivaluedHashMap<Class<? extends Annotation>, ReaderInterceptor>(),
                new MultivaluedHashMap<Class<? extends Annotation>, WriterInterceptor>(),
                Collections.<DynamicBinder>emptyList()
        );
        runtimeModelBuilder.process(Resource.builder(HelloWorldResource.class, new LinkedList<ResourceModelIssue>()).build(), false);
        final InvokerBuilder invokerBuilder = locator.createAndInitialize(InvokerBuilder.class);

        this.invoker = invokerBuilder.build(runtimeModelBuilder.buildModel(false));
        this.requestScope = locator.createAndInitialize(RequestScope.class);
    }

    @Test
    public void testHelloWorld() throws Exception {
        final ContainerRequest req = RequestContextBuilder.from(BASE_URI, URI.create(BASE_URI.getPath() + "helloworld"), "GET").build();

        Future<ContainerResponse> res = requestScope.runInScope(new Callable<Future<ContainerResponse>>() {

            @Override
            public Future<ContainerResponse> call() throws Exception {
                return invoker.apply(req);
            }
        });

        assertEquals("hello", res.get().getEntity());
    }

    @Test
    public void testOptions() throws Exception {
        final ContainerRequest req = RequestContextBuilder.from(BASE_URI, URI.create(BASE_URI.getPath() + "helloworld"), "OPTIONS").build();
        Future<ContainerResponse> res = requestScope.runInScope(new Callable<Future<ContainerResponse>>() {

            @Override
            public Future<ContainerResponse> call() throws Exception {
                return invoker.apply(req);
            }
        });

        assertEquals("GET", res.get().getEntity());
    }

    @Test
    public void testSubResMethod() throws Exception {
        final ContainerRequest req2 = RequestContextBuilder.from(BASE_URI, URI.create(BASE_URI.getPath() + "helloworld/another/b"), "GET").build();

        Future<ContainerResponse> res2 = requestScope.runInScope(new Callable<Future<ContainerResponse>>() {

            @Override
            public Future<ContainerResponse> call() throws Exception {

                return invoker.apply(req2);
            }
        });
        assertEquals("another", res2.get().getEntity());
    }
}
