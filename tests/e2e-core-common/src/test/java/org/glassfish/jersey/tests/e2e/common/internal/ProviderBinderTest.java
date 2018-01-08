/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.RuntimeDelegate;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.CompositeBinder;
import org.glassfish.jersey.internal.inject.CustomAnnotationLiteral;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.tests.e2e.common.TestRuntimeDelegate;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ServiceProviders unit test.
 *
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class ProviderBinderTest {

    private static class MyProvider implements MessageBodyReader, MessageBodyWriter {

        @Override
        public boolean isReadable(Class type, Type genericType, Annotation[] annotations,
                                  MediaType mediaType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isWriteable(Class type, Type genericType, Annotation[] annotations,
                                   MediaType mediaType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getSize(Object t, Class type, Type genericType, Annotation[] annotations,
                            MediaType mediaType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void writeTo(Object t, Class type, Type genericType, Annotation[] annotations,
                            MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static Binder initBinders(Binder... binders) {
        List<Binder> binderList = Arrays.stream(binders).collect(Collectors.toList());
        binderList.add(new MessagingBinders.MessageBodyProviders(null, RuntimeType.SERVER));
        return CompositeBinder.wrap(binderList);
    }

    public ProviderBinderTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testServicesNotEmpty() {
        InjectionManager injectionManager = Injections.createInjectionManager(initBinders());
        injectionManager.completeRegistration();
        Set<MessageBodyReader> providers = Providers.getProviders(injectionManager, MessageBodyReader.class);
        assertTrue(providers.size() > 0);
    }

    @Test
    public void testServicesMbr() {
        InjectionManager injectionManager = Injections.createInjectionManager(initBinders());
        injectionManager.completeRegistration();
        Set<MessageBodyReader> providers = Providers.getProviders(injectionManager, MessageBodyReader.class);
        assertTrue(providers.size() > 0);
    }

    @Test
    public void testServicesMbw() {
        InjectionManager injectionManager = Injections.createInjectionManager(initBinders());
        injectionManager.completeRegistration();
        Set<MessageBodyWriter> providers = Providers.getProviders(injectionManager, MessageBodyWriter.class);
        assertTrue(providers.size() > 0);
    }

    @Test
    public void testProvidersMbr() {
        InjectionManager injectionManager = Injections.createInjectionManager(initBinders());
        ProviderBinder providerBinder = new ProviderBinder(injectionManager);
        providerBinder.bindClasses(Collections.singleton(MyProvider.class));

        injectionManager.completeRegistration();
        Set<MessageBodyReader> providers = Providers.getCustomProviders(injectionManager, MessageBodyReader.class);
        assertEquals(1, instancesOfType(MyProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbw() {
        InjectionManager injectionManager = Injections.createInjectionManager(initBinders());
        ProviderBinder providerBinder = new ProviderBinder(injectionManager);
        providerBinder.bindClasses(Collections.singleton(MyProvider.class));

        injectionManager.completeRegistration();
        Set<MessageBodyWriter> providers = Providers.getCustomProviders(injectionManager, MessageBodyWriter.class);
        final Collection<MyProvider> myProviders = instancesOfType(MyProvider.class, providers);
        assertEquals(1, myProviders.size());
    }

    @Test
    public void testProvidersMbrInstance() {
        InjectionManager injectionManager = Injections.createInjectionManager(initBinders());
        ProviderBinder providerBinder = new ProviderBinder(injectionManager);
        providerBinder.bindInstances(Collections.singleton(new MyProvider()));

        injectionManager.completeRegistration();
        Set<MessageBodyReader> providers = Providers.getCustomProviders(injectionManager, MessageBodyReader.class);
        assertEquals(1, instancesOfType(MyProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbwInstance() {
        InjectionManager injectionManager = Injections.createInjectionManager(initBinders());
        ProviderBinder providerBinder = new ProviderBinder(injectionManager);
        providerBinder.bindInstances(Collections.singleton(new MyProvider()));

        injectionManager.completeRegistration();
        Set<MessageBodyWriter> providers = Providers.getCustomProviders(injectionManager, MessageBodyWriter.class);
        assertEquals(instancesOfType(MyProvider.class, providers).size(), 1);
    }

    private <T> Collection<T> instancesOfType(final Class<T> c, Collection<?> collection) {

        return collection.stream()
                .filter((java.util.function.Predicate<Object>) o -> o.getClass() == c)
                .map((java.util.function.Function<Object, T>) c::cast)
                .collect(Collectors.toList());
    }


    @Test
    public void testCustomRegistration() {
        InjectionManager injectionManager = Injections.createInjectionManager();

        ProviderBinder providerBinder = new ProviderBinder(injectionManager);
        providerBinder.bindClasses(Child.class);
        providerBinder.bindClasses(NotFilterChild.class);
        injectionManager.completeRegistration();

        ContainerRequestFilter requestFilter = getRequestFilter(injectionManager);
        ContainerRequestFilter requestFilter2 = getRequestFilter(injectionManager);
        assertEquals(requestFilter, requestFilter2);


        ContainerResponseFilter responseFilter = getResponseFilter(injectionManager);
        ContainerResponseFilter responseFilter2 = getResponseFilter(injectionManager);
        assertTrue(responseFilter == responseFilter2);

        assertTrue(responseFilter == requestFilter);

        // only one filter should be registered
        Collection<ContainerResponseFilter> filters =
                Providers.getCustomProviders(injectionManager, ContainerResponseFilter.class);
        assertEquals(1, filters.size());

        Child child = injectionManager.getInstance(Child.class);
        Child child2 = injectionManager.getInstance(Child.class);

        assertTrue(child != responseFilter);

        assertTrue(child == child2);
    }

    private ContainerResponseFilter getResponseFilter(InjectionManager injectionManager) {
        ContainerResponseFilter responseFilter =
                injectionManager.getInstance(ContainerResponseFilter.class, CustomAnnotationLiteral.INSTANCE);
        assertEquals(Child.class, responseFilter.getClass());
        return responseFilter;
    }

    private ContainerRequestFilter getRequestFilter(InjectionManager injectionManager) {
        ContainerRequestFilter requestFilter =
                injectionManager.getInstance(ContainerRequestFilter.class, CustomAnnotationLiteral.INSTANCE);
        assertEquals(Child.class, requestFilter.getClass());
        return requestFilter;
    }

    interface ParentInterface {
    }

    interface ChildInterface extends ChildSuperInterface {
    }


    interface SecondChildInterface {
    }

    interface ChildSuperInterface extends ContainerResponseFilter {
    }

    @Singleton
    public static class Parent implements ParentInterface, ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }
    }

    @Singleton
    public static class Child extends Parent implements ChildInterface, SecondChildInterface {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        }
    }

    private static class NotFilterChild implements ParentInterface {
    }
}
