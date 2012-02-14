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
package org.glassfish.jersey.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.message.internal.MessagingModules;
import org.glassfish.jersey.message.internal.StringMessageProvider;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.Services;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * ServiceProviders unit test.
 *
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ServiceProvidersTest {

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

    private static Module[] initModules(Module... modules) {
        List<Module> moduleList = Lists.newArrayList(modules);

        moduleList.add(new ContextInjectionResolver.Module());
        moduleList.add(new ServiceProvidersModule());
        moduleList.add(new MessagingModules.MessageBodyProviders());

        return moduleList.toArray(new Module[moduleList.size()]);
    }

    public ServiceProvidersTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testServicesNotEmpty() {
        Services services = HK2.get().create(null, initModules());
        ServiceProviders ps = services.byType(ServiceProviders.Builder.class).get().build();

        Set<MessageBodyReader> providers = ps.getDefault(MessageBodyReader.class);
        assertTrue(providers.size() > 0);
    }

    @Test
    public void testServicesMbr() {
        Services services = HK2.get().create(null, initModules());
        ServiceProviders ps = services.byType(ServiceProviders.Builder.class).get().build();

        Set<MessageBodyReader> providers = ps.getDefault(MessageBodyReader.class);
        assertEquals(1, instancesOfType(StringMessageProvider.class, providers).size());
    }

    @Test
    public void testServicesMbw() {
        Services services = HK2.get().create(null, initModules());
        ServiceProviders ps = services.byType(ServiceProviders.Builder.class).get().build();

        Set<MessageBodyWriter> providers = ps.getDefault(MessageBodyWriter.class);
        assertEquals(1, instancesOfType(StringMessageProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbr() {
        Services services = HK2.get().create(null, initModules());
        ServiceProviders ps = services.byType(ServiceProviders.Builder.class).get().setProviderClasses(Sets.<Class<?>>newHashSet(MyProvider.class)).build();
        Set<MessageBodyReader> providers = ps.getCustom(MessageBodyReader.class);
        assertEquals(1, instancesOfType(MyProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbw() {
        Services services = HK2.get().create(null, initModules());
        ServiceProviders ps = services.byType(ServiceProviders.Builder.class).get().setProviderClasses(Sets.<Class<?>>newHashSet(MyProvider.class)).build();

        Set<MessageBodyWriter> providers = ps.getCustom(MessageBodyWriter.class);
        final Collection<MyProvider> myProviders = instancesOfType(MyProvider.class, providers);
        assertEquals(1, myProviders.size());
    }

    @Test
    public void testProvidersMbrInstance() {
        Services services = HK2.get().create(null, initModules());
        ServiceProviders ps = services.byType(ServiceProviders.Builder.class).get().setProviderInstances(Sets.<Object>newHashSet(new MyProvider())).build();
        Set<MessageBodyReader> providers = ps.getCustom(MessageBodyReader.class);
        assertEquals(1, instancesOfType(MyProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbwInstance() {
        Services services = HK2.get().create(null, initModules());
        ServiceProviders ps = services.byType(ServiceProviders.Builder.class).get().setProviderInstances(Sets.newHashSet((Object) new MyProvider())).build();

        Set<MessageBodyWriter> providers = ps.getCustom(MessageBodyWriter.class);
        assertEquals(instancesOfType(MyProvider.class, providers).size(), 1);
    }

    private <T> Collection<T> instancesOfType(final Class<T> c, Collection<?> collection) {
        return Collections2.transform(Collections2.filter(collection, new Predicate<Object>() {

            @Override
            public boolean apply(Object input) {
                return input.getClass() == c;
            }
        }), new Function<Object, T>() {

            @Override
            public T apply(Object input) {
                return c.cast(input);
            }
        });
    }
}
