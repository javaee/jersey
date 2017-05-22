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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.NameBinding;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
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
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link InjectionManagerProvider}.
 *
 * @author Miroslav Fuksa
 */
public class InjectionManagerServerProviderTest extends JerseyTest {

    @Path("resource")
    public static class TestResource {
        @POST
        @Path("feature")
        @FeatureBound
        public String echoFeature(String entity) {
            return entity;
        }


        @POST
        @Path("reader-interceptor")
        @ReaderInterceptorBound
        public String echoReaderInterceptor(String entity) {
            return entity;
        }

        @POST
        @Path("writer-interceptor")
        @WriterInterceptorBound
        public String echoWriterInterceptor(String entity) {
            return entity;
        }
    }

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(TestResource.class);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new MyInjectedService("hello")).to(MyInjectedService.class);
            }
        });
        resourceConfig.register(new MyFeature());
        resourceConfig.register(new MyReaderInterceptor());
        resourceConfig.register(new MyWriterInterceptor());
        return resourceConfig;
    }


    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface FeatureBound {
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface ReaderInterceptorBound {
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface WriterInterceptorBound {
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


    public static class MyFeature implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(MyFeatureInterceptor.class);
            return true;
        }


        @FeatureBound
        public static class MyFeatureInterceptor implements WriterInterceptor {
            private final String name;

            @Inject
            public MyFeatureInterceptor(MyInjectedService injectedService) {
                this.name = injectedService.getName();
            }

            @Override
            public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
                context.setEntity(((String) context.getEntity()) + "-interceptorfeature-" + name);
                context.proceed();
            }
        }
    }

    @Test
    public void testFeature() {
        final Response response = target().path("resource/feature")
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-interceptorfeature-hello", response.readEntity(String.class));
    }

    @WriterInterceptorBound
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
    public void testWriterInterceptor() {
        final Response response = target().path("resource/writer-interceptor")
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-writer-interceptor-hello", response.readEntity(String.class));
    }


    @ReaderInterceptorBound
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
        final Response response = target().path("resource/reader-interceptor")
                .request().post(Entity.entity("will-be-extended-by", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("will-be-extended-by-reader-interceptor-hello", response.readEntity(String.class));
    }

}
