/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.inject.cdi.se.injector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Providers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjecteeImpl;
import org.glassfish.jersey.internal.inject.InjectionResolver;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests Jersey Proxy Resolver.
 */
public class JerseyProxyResolverTest {

    private static Field[] FIELDS = StubForFields.class.getDeclaredFields();

    @Test
    public void testSignletonIsProxiable() {
        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setInjecteeClass(TestSingleton.class);
        injectee.setParentClassScope(Singleton.class);

        JerseyProxyResolver resolver = new JerseyProxyResolver();
        assertTrue(resolver.isPrixiable(injectee));
    }

    @Test
    public void testApplicationScopeIsProxiable() {
        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setInjecteeClass(TestApplicationScope.class);
        injectee.setParentClassScope(ApplicationScoped.class);

        JerseyProxyResolver resolver = new JerseyProxyResolver();
        assertTrue(resolver.isPrixiable(injectee));
    }

    @Test
    public void testRequestScopeFromNonAnnotatedIsNotProxiable() {
        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setInjecteeClass(TestNonAnnotatedRequestScope.class);
        injectee.setParentClassScope(RequestScoped.class);

        JerseyProxyResolver resolver = new JerseyProxyResolver();
        assertFalse(resolver.isPrixiable(injectee));
    }

    @Test
    public void testRequestScopeIsNotProxiable() {
        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setInjecteeClass(TestRequestScope.class);
        injectee.setParentClassScope(RequestScoped.class);

        JerseyProxyResolver resolver = new JerseyProxyResolver();
        assertFalse(resolver.isPrixiable(injectee));
    }

    @Test
    public void testApplicationIsNotProxiable() {
        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setRequiredType(Application.class);

        JerseyProxyResolver resolver = new JerseyProxyResolver();
        assertFalse(resolver.isPrixiable(injectee));
    }

    @Test
    public void testProxyCreated() {
        MyInjectionResolver injectionResolver = new MyInjectionResolver(new JaxrsProviders());
        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setRequiredType(Providers.class);
        injectee.setParent(FIELDS[0]);

        JerseyProxyResolver resolver = new JerseyProxyResolver();
        Object proxy = resolver.proxy(injectee, injectionResolver);
        assertTrue(proxy.getClass().getName().contains("Proxy"));
    }

    @Test
    public void testProxyCached() {
        MyInjectionResolver injectionResolver = new MyInjectionResolver(new JaxrsProviders());
        InjecteeImpl injectee1 = new InjecteeImpl();
        injectee1.setRequiredType(Providers.class);
        injectee1.setParent(FIELDS[0]);

        InjecteeImpl injectee2 = new InjecteeImpl();
        injectee2.setRequiredType(Providers.class);
        injectee2.setParent(FIELDS[1]);

        JerseyProxyResolver resolver = new JerseyProxyResolver();
        Object proxy1 = resolver.proxy(injectee1, injectionResolver);
        Object proxy2 = resolver.proxy(injectee2, injectionResolver);
        assertSame(proxy1.getClass(), proxy2.getClass());
    }

    @Test
    public void testProxyCacheNotMismatched() {
        MyInjectionResolver injectionResolver1 = new MyInjectionResolver(new JaxrsProviders());
        InjecteeImpl injectee1 = new InjecteeImpl();
        injectee1.setRequiredType(Providers.class);
        injectee1.setParent(FIELDS[0]);

        MyInjectionResolver injectionResolver2 = new MyInjectionResolver(new ArrayList<>());
        InjecteeImpl injectee2 = new InjecteeImpl();
        injectee2.setRequiredType(List.class);
        injectee2.setParent(FIELDS[1]);

        JerseyProxyResolver resolver = new JerseyProxyResolver();
        Object proxy1 = resolver.proxy(injectee1, injectionResolver1);
        Object proxy2 = resolver.proxy(injectee2, injectionResolver2);
        assertNotSame(proxy1.getClass(), proxy2.getClass());
    }

    private static class StubForFields {
        private Object field1;
        private Object field2;
    }

    private static class MyInjectionResolver implements InjectionResolver {

        private final Object instance;

        private MyInjectionResolver(Object instance) {
            this.instance = instance;
        }

        @Override
        public Object resolve(Injectee injectee) {
            return instance;
        }

        @Override
        public boolean isConstructorParameterIndicator() {
            return true;
        }

        @Override
        public boolean isMethodParameterIndicator() {
            return false;
        }

        @Override
        public Class getAnnotation() {
            return Context.class;
        }
    }

    private static class TestNonAnnotatedRequestScope {
    }

    @RequestScoped
    private static class TestRequestScope {
    }

    @Singleton
    private static class TestSingleton {
    }

    @ApplicationScoped
    private static class TestApplicationScope {
    }
}
