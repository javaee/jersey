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
package org.glassfish.jersey.internal;

import java.util.Set;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.Custom;
import org.glassfish.jersey.internal.inject.Providers;

import org.glassfish.hk2.BinderFactory;
import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.DynamicBinderFactory;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Scope;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Class used for registration of the custom providers into HK2 services.
 * <p>
 * Custom providers are classes that implements specific JAX-RS or Jersey
 * SPI interfaces (e.g. {@link javax.ws.rs.ext.MessageBodyReader} and are
 * supplied by the user. These providers will be bound into the HK2 services
 * annotated by a {@link Custom &#64;Custom} qualifier annotation.
 * </p>
 * <p>
 * Use the {@code &#64;Custom} qualifier annotation to retrieve these providers
 * from HK2 services. For example:
 * </p>
 * <pre>
 *  Collection&lt;Provider&lt;MessageBodyReader&gt;&gt; hk2Providers =
 *          services.forContract(MessageBodyReader.class)
 *                  .annotatedWith(Custom.class)
 *                  .all();
 * </pre>
 * <p>
 * You may also use a one of the provider accessor utility method defined in
 * {@link Providers} class.
 * </p>
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ProviderBinder {
    @Inject
    Services services;
    @Inject
    Injector injector;

    /**
     * Register/bind custom provider instances. Registered providers will be handled
     * always as Singletons.
     *
     * @param instances custom provider instances.
     */
    public <T> void bindInstances(T... instances) {
        if (instances != null && instances.length > 0) {
            DynamicBinderFactory binderFactory = services.bindDynamically();
            for (T instance : instances) {
                bindInstance(instance, binderFactory);
            }
            binderFactory.commit();
        }
    }

    /**
     * Register/bind custom provider instances. Registered providers will be handled
     * always as Singletons.
     *
     * @param instances custom provider instances.
     */
    public void bindInstances(Set<Object> instances) {
        DynamicBinderFactory binderFactory = services.bindDynamically();
        for (Object instance : instances) {
            bindInstance(instance, binderFactory);
        }
        binderFactory.commit();
    }


    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link org.glassfish.jersey.spi.PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(Class<?>... classes) {
        if (classes != null && classes.length > 0) {
            DynamicBinderFactory binderFactory = services.bindDynamically();
            for (Class<?> clazz : classes) {
                bindClass(clazz, binderFactory, false);
            }
            binderFactory.commit();
        }
    }

    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link org.glassfish.jersey.spi.PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(Iterable<Class<?>> classes) {
        bindClasses(classes, false);
    }

    /**
     * Register/bind custom provider classes that may also be resources. Registered
     * providers/resources will be handled always as Singletons unless annotated by
     * {@link org.glassfish.jersey.spi.PerLookup}.
     *
     * <p>
     * If {@code bindAsResources} is set to {@code true}, the providers will also be bound
     * as resources.
     * </p>
     *
     * @param classes         custom provider classes.
     * @param bindAsResources if {@code true}, the provider classes will also be bound as
     *                        resources.
     */
    public void bindClasses(Iterable<Class<?>> classes, boolean bindAsResources) {
        if (classes == null || !classes.iterator().hasNext()) {
            return;
        }

        DynamicBinderFactory binderFactory = services.bindDynamically();
        for (Class<?> clazz : classes) {
            bindClass(clazz, binderFactory, bindAsResources);
        }
        binderFactory.commit();
    }

    @SuppressWarnings("unchecked")
    private <T> void bindInstance(T instance, BinderFactory binderFactory) {
        for (Class contract : Providers.getProviderContracts(instance.getClass())) {
            binderFactory.bind(contract).annotatedWith(Custom.class).toInstance(instance);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void bindClass(Class<T> clazz, BinderFactory binderFactory, boolean alsoResources) {
        Class<? extends Scope> scope = getProviderScope(clazz);

        binderFactory.bind().annotatedWith(Custom.class).to(clazz).in(scope);
        if (scope == Singleton.class) {
            SingletonFactory factory = new SingletonFactory(clazz, injector);
            for (Class contract : Providers.getProviderContracts(clazz)) {
                binderFactory.bind(contract).annotatedWith(Custom.class).toFactory(factory).in(scope);
            }
            if (alsoResources) {
                binderFactory.bind(clazz).toFactory(factory).in(scope);
            }
        } else {
            for (Class contract : Providers.getProviderContracts(clazz)) {
                binderFactory.bind(contract).annotatedWith(Custom.class).to(clazz).in(scope);
            }
            if (alsoResources) {
                binderFactory.bind(clazz).to(clazz).in(scope);
            }
        }
    }

    private Class<? extends Scope> getProviderScope(Class<?> clazz) {
        Class<? extends Scope> hk2Scope = Singleton.class;
        if (clazz.isAnnotationPresent(org.glassfish.jersey.spi.PerLookup.class)) {
            hk2Scope = PerLookup.class;
        }
        return hk2Scope;
    }

    private static class InstanceSupplier<T> implements Supplier<T> {
        private Injector injector;

        private Class<T> rawType;

        @Override
        public T get() throws ComponentException {
            return injector.inject(rawType);
        }

        public InstanceSupplier(Class<T> rawType, Injector injector) {
            this.rawType = rawType;
            this.injector = injector;
        }
    }

    private static class SingletonFactory<T> implements Factory<T> {
        Supplier<T> supplier;

        @Override
        public T get() throws ComponentException {
            return supplier.get();
        }

        public SingletonFactory(Class<T> rawType, Injector injector) {
            supplier = Suppliers.memoize(new InstanceSupplier<T>(rawType, injector));
        }
    }

    /**
     * Module which registers {@link ProviderBinder} into the the HK2 services.
     */
    public static class ProviderBinderModule extends AbstractModule {

        @Override
        protected void configure() {
//            bind(ProviderBinder.class).to(ProviderBinder.class).in(PerLookup.class);
            bind().to(ProviderBinder.class).in(PerLookup.class);
        }
    }
}
