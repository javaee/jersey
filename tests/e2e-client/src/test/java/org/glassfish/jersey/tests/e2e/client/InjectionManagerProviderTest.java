/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.inject.Inject;

import org.glassfish.jersey.InjectionManagerProvider;
import org.glassfish.jersey.client.InjectionManagerClientProvider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link InjectionManagerClientProvider}.
 *
 * @author Miroslav Fuksa
 */
public class InjectionManagerProviderTest extends JerseyTest {

    @Path("resource")
    public static class TestResource {
        @POST
        public String echo(String entity) {
            return entity;
        }
    }


    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }


    public static class MyInjectedService {
        public final String name;

        public MyInjectedService(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class BinderFeature implements Feature {

        public final String name;

        public BinderFeature(String name) {
            this.name = name;
        }

        @Override
        public boolean configure(FeatureContext context) {
            context.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(new MyInjectedService(name)).to(MyInjectedService.class);
                }
            });
            return true;
        }
    }


    public static class MyRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            final InjectionManager injectionManager = InjectionManagerClientProvider.getInjectionManager(requestContext);
            final MyInjectedService service = injectionManager.getInstance(MyInjectedService.class);
            final String name = service.getName();
            requestContext.setEntity(name);
        }
    }


    @Test
    public void testRequestFilterInstance() {
        final Response response = target().path("resource")
                .register(new BinderFeature("hello"))
                .register(new MyRequestFilter())
                .request().post(Entity.entity("will-be-overwritten", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("hello", response.readEntity(String.class));
    }

    @Test
    public void testRequestFilterClass() {
        final Response response = target().path("resource")
                .register(new BinderFeature("hello"))
                .register(MyRequestFilter.class)
                .request().post(Entity.entity("will-be-overwritten", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("hello", response.readEntity(String.class));
    }


    public static class MyResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {

            final InjectionManager locator = InjectionManagerClientProvider.getInjectionManager(responseContext);
            final MyInjectedService service = locator.getInstance(MyInjectedService.class);
            final String name = service.getName();
            responseContext.setEntityStream(new ByteArrayInputStream(name.getBytes()));
        }
    }

    @Test
    public void testResponseFilterInstance() {
        final Response response = target().path("resource")
                .register(new BinderFeature("world"))
                .register(new MyResponseFilter())
                .request().post(Entity.entity("will-be-overwritten", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("world", response.readEntity(String.class));
    }

    @Test
    public void testResponseFilterClass() {
        final Response response = target().path("resource")
                .register(new BinderFeature("world"))
                .register(MyResponseFilter.class)
                .request().post(Entity.entity("will-be-overwritten", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("world", response.readEntity(String.class));
    }

    public static class MyFeature implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(MyFeatureInterceptor.class);
            return true;
        }

        public static class MyFeatureInterceptor implements WriterInterceptor {
            private final String name;

            @Inject
            public MyFeatureInterceptor(MyInjectedService injectedService) {
                this.name = injectedService.getName();
            }

            @Override
            public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
                context.setEntity(context.getEntity() + "-interceptor-" + name);
                context.proceed();
            }
        }
    }

    @Test
    public void testFeatureInstance() {
        final Response response = target().path("resource")
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(new MyInjectedService("feature")).to(MyInjectedService.class);
                    }
                })
                .register(new MyFeature())
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-interceptor-feature", response.readEntity(String.class));
    }

    @Test
    public void testFeatureClass() {
        final Response response = target().path("resource")
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(new MyInjectedService("feature")).to(MyInjectedService.class);
                    }
                })
                .register(MyFeature.class)
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-interceptor-feature", response.readEntity(String.class));
    }


    public static class MyWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final InjectionManager serviceLocator = InjectionManagerProvider.getInjectionManager(context);
            final MyInjectedService service = serviceLocator.getInstance(MyInjectedService.class);
            context.setEntity(((String) context.getEntity()) + "-writer-interceptor-" + service.getName());
            context.proceed();
        }
    }

    @Test
    public void testWriterInterceptorInstance() {
        final Response response = target().path("resource")
                .register(new BinderFeature("universe"))
                .register(new MyWriterInterceptor())
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-writer-interceptor-universe", response.readEntity(String.class));
    }

    @Test
    public void testWriterInterceptorClass() {
        final Response response = target().path("resource")
                .register(new BinderFeature("universe"))
                .register(MyWriterInterceptor.class)
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-writer-interceptor-universe", response.readEntity(String.class));
    }


    public static class MyReaderInterceptor implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final Object entity = context.proceed();
            if (!(entity instanceof String)) {
                return entity;
            }
            final String stringEntity = (String) entity;
            final InjectionManager serviceLocator = InjectionManagerProvider.getInjectionManager(context);
            final MyInjectedService service = serviceLocator.getInstance(MyInjectedService.class);
            return stringEntity + "-reader-interceptor-" + service.getName();
        }
    }


    @Test
    public void testReaderInterceptorInstance() {
        final Response response = target().path("resource")
                .register(new BinderFeature("universe"))
                .register(new MyReaderInterceptor())
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-reader-interceptor-universe", response.readEntity(String.class));
    }

    @Test
    public void testReaderInterceptorClass() {
        final Response response = target().path("resource")
                .register(new BinderFeature("universe"))
                .register(MyReaderInterceptor.class)
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-reader-interceptor-universe", response.readEntity(String.class));
    }
}
