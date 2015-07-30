/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.RuntimeType;

import javax.inject.Singleton;

import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.ComponentBag;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AliasDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.binding.ScopedBindingBuilder;

import jersey.repackaged.com.google.common.base.Predicate;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Class used for registration of the custom providers into HK2 service locator.
 * <p>
 * Custom providers are classes that implements specific JAX-RS or Jersey
 * SPI interfaces (e.g. {@link javax.ws.rs.ext.MessageBodyReader}) and are
 * supplied by the user. These providers will be bound into the HK2 service locator
 * annotated by a {@link Custom &#64;Custom} qualifier annotation.
 * </p>
 * <p>
 * Use the {@code &#64;Custom} qualifier annotation to retrieve these providers
 * from HK2 service locator. You may also use a one of the provider accessor utility
 * method defined in {@link Providers} class.
 * </p>
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Michal Gajdos
 */
public class ProviderBinder {

    private final ServiceLocator locator;

    /**
     * Create new provider binder instance.
     *
     * @param locator HK2 service locator the binder will use to bind the
     *                providers into.
     */
    public ProviderBinder(final ServiceLocator locator) {
        this.locator = locator;
    }

    /**
     * Register/bind custom provider instances. Registered providers will be handled
     * always as Singletons.
     *
     * @param instances custom provider instances.
     */
    public void bindInstances(final Iterable<Object> instances) {
        final DynamicConfiguration dc = Injections.getConfiguration(locator);
        for (final Object instance : instances) {
            bindInstance(instance, dc);
        }
        dc.commit();
    }

    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(final Class<?>... classes) {
        if (classes != null && classes.length > 0) {
            final DynamicConfiguration dc = Injections.getConfiguration(locator);
            for (final Class<?> clazz : classes) {
                bindClass(clazz, locator, dc, false);
            }
            dc.commit();
        }
    }

    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(final Iterable<Class<?>> classes) {
        bindClasses(classes, false);
    }

    /**
     * Register/bind custom provider classes that may also be resources. Registered
     * providers/resources will be handled always as Singletons unless annotated by
     * {@link PerLookup}.
     *
     * <p>
     * If {@code bindAsResources} is set to {@code true}, the providers will also be bound
     * as resources.
     * </p>
     *
     * @param classes       custom provider classes.
     * @param bindResources if {@code true}, the provider classes will also be bound as
     *                      resources.
     */
    public void bindClasses(final Iterable<Class<?>> classes, final boolean bindResources) {
        if (classes == null || !classes.iterator().hasNext()) {
            return;
        }

        final DynamicConfiguration dc = Injections.getConfiguration(locator);
        for (final Class<?> clazz : classes) {
            bindClass(clazz, locator, dc, bindResources);
        }
        dc.commit();
    }

    /**
     * Bind contract provider model to a provider class using the supplied HK2 dynamic configuration.
     *
     * @param providerClass provider class.
     * @param model      contract provider model.
     * @param dc            HK2 dynamic service locator configuration.
     */
    public static void bindProvider(
            final Class<?> providerClass, final ContractProvider model, final DynamicConfiguration dc) {

        for (final Class contract : model.getContracts()) {
            final ScopedBindingBuilder bindingBuilder = Injections.newBinder(providerClass)
                    .in(model.getScope())
                    .qualifiedBy(CustomAnnotationLiteral.INSTANCE);

            //noinspection unchecked
            bindingBuilder.to(contract);

            final int priority = model.getPriority(contract);
            if (priority > ContractProvider.NO_PRIORITY) {
                bindingBuilder.ranked(priority);
            }

            Injections.addBinding(bindingBuilder, dc);
        }
    }

    /**
     * Bind contract provider model to a provider instance using the supplied
     * HK2 dynamic configuration.
     *
     * Scope value specified in the {@link ContractProvider contract provider model}
     * is ignored as instances can only be bound as "singletons".
     *
     * @param providerInstance provider instance.
     * @param model         contract provider model.
     * @param dc               HK2 dynamic service locator configuration.
     */
    public static void bindProvider(
            final Object providerInstance, final ContractProvider model, final DynamicConfiguration dc) {

        for (final Class contract : model.getContracts()) {
            final ScopedBindingBuilder bindingBuilder = Injections
                    .newBinder(providerInstance).qualifiedBy(CustomAnnotationLiteral.INSTANCE);

            //noinspection unchecked
            bindingBuilder.to(contract);

            final int priority = model.getPriority(contract);
            if (priority > ContractProvider.NO_PRIORITY) {
                bindingBuilder.ranked(priority);
            }

            Injections.addBinding(bindingBuilder, dc);
        }
    }

    /**
     * Bind all providers contained in {@code providerBag} (classes and instances) using HK2 service locator. Configuration is
     * also committed.
     *
     * @param componentBag bag of provider classes and instances.
     * @param locator      HK2 service locator the binder will use to bind the providers into.
     */
    public static void bindProviders(final ComponentBag componentBag, final ServiceLocator locator) {
        bindProviders(componentBag, null, Collections.<Class<?>>emptySet(), locator);
    }

    /**
     * Bind all providers contained in {@code providerBag} (classes and instances) using HK2 service locator. Configuration is
     * also committed.
     *
     * @param componentBag      bag of provider classes and instances.
     * @param constrainedTo     current runtime (client or server).
     * @param registeredClasses classes which are manually registered by the user (not found by the classpath scanning).
     * @param locator           HK2 service locator the binder will use to bind the providers into.
     */
    public static void bindProviders(final ComponentBag componentBag,
                                     final RuntimeType constrainedTo,
                                     final Set<Class<?>> registeredClasses,
                                     final ServiceLocator locator) {
        final DynamicConfiguration dc = Injections.getConfiguration(locator);
        bindProviders(componentBag, constrainedTo, registeredClasses, dc);
        dc.commit();
    }

    /**
     * Bind all providers contained in {@code providerBag} (classes and instances) using HK2 service locator. Configuration is
     * not committed.
     *
     * @param componentBag         bag of provider classes and instances.
     * @param constrainedTo        current runtime (client or server).
     * @param registeredClasses    classes which are manually registered by the user (not found by the classpath scanning).
     * @param dynamicConfiguration HK2 dynamic service locator configuration.
     */
    public static void bindProviders(final ComponentBag componentBag,
                                     final RuntimeType constrainedTo,
                                     final Set<Class<?>> registeredClasses,
                                     final DynamicConfiguration dynamicConfiguration) {
        final Predicate<ContractProvider> filter = new Predicate<ContractProvider>() {
            @Override
            public boolean apply(final ContractProvider input) {
                return ComponentBag.EXCLUDE_EMPTY.apply(input) && ComponentBag.EXCLUDE_META_PROVIDERS.apply(input);
            }
        };

        // Bind provider classes except for pure meta-providers and providers with empty contract models (e.g. resources)
        Set<Class<?>> classes = Sets.newLinkedHashSet(componentBag.getClasses(filter));
        if (constrainedTo != null) {
            classes = Sets.filter(classes, new Predicate<Class<?>>() {
                @Override
                public boolean apply(final Class<?> componentClass) {
                    return Providers.checkProviderRuntime(
                            componentClass,
                            componentBag.getModel(componentClass),
                            constrainedTo,
                            registeredClasses == null || !registeredClasses.contains(componentClass),
                            false);
                }
            });
        }
        for (final Class<?> providerClass : classes) {
            final ContractProvider model = componentBag.getModel(providerClass);
            ProviderBinder.bindProvider(providerClass, model, dynamicConfiguration);
        }

        // Bind pure provider instances except for pure meta-providers and providers with empty contract models (e.g. resources)
        Set<Object> instances = componentBag.getInstances(filter);
        if (constrainedTo != null) {
            instances = Sets.filter(instances, new Predicate<Object>() {
                @Override
                public boolean apply(final Object component) {
                    final Class<?> componentClass = component.getClass();
                    return Providers.checkProviderRuntime(
                            componentClass,
                            componentBag.getModel(componentClass),
                            constrainedTo,
                            registeredClasses == null || !registeredClasses.contains(componentClass),
                            false);
                }
            });
        }
        for (final Object provider : instances) {
            final ContractProvider model = componentBag.getModel(provider.getClass());
            ProviderBinder.bindProvider(provider, model, dynamicConfiguration);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void bindInstance(final T instance, final DynamicConfiguration dc) {
        for (final Class contract : Providers.getProviderContracts(instance.getClass())) {
            Injections.addBinding(Injections.newBinder(instance).to(contract).qualifiedBy(CustomAnnotationLiteral.INSTANCE), dc);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void bindClass(final Class<T> clazz,
                               final ServiceLocator locator,
                               final DynamicConfiguration dc,
                               final boolean isResource) {
        final Class<? extends Annotation> scope = getProviderScope(clazz);

        if (isResource) {
            final ActiveDescriptor<?> descriptor = dc.bind(BuilderHelper.activeLink(clazz).to(clazz).in(scope).build());

            for (final Class contract : Providers.getProviderContracts(clazz)) {
                final AliasDescriptor aliasDescriptor = new AliasDescriptor(locator, descriptor, contract.getName(), null);
                aliasDescriptor.setScope(scope.getName());
                aliasDescriptor.addQualifierAnnotation(CustomAnnotationLiteral.INSTANCE);

                dc.bind(aliasDescriptor);
            }
        } else {
            final ScopedBindingBuilder<T> bindingBuilder =
                    Injections.newBinder(clazz).in(scope).qualifiedBy(CustomAnnotationLiteral.INSTANCE);
            for (final Class contract : Providers.getProviderContracts(clazz)) {
                bindingBuilder.to(contract);
            }
            Injections.addBinding(bindingBuilder, dc);
        }
    }

    private Class<? extends Annotation> getProviderScope(final Class<?> clazz) {
        Class<? extends Annotation> hk2Scope = Singleton.class;
        if (clazz.isAnnotationPresent(PerLookup.class)) {
            hk2Scope = PerLookup.class;
        }
        return hk2Scope;
    }
}
