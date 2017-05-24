/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.RuntimeType;

import javax.inject.Singleton;

import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.ComponentBag;

/**
 * Class used for registration of the custom providers into injection manager.
 * <p>
 * Custom providers are classes that implements specific JAX-RS or Jersey
 * SPI interfaces (e.g. {@link javax.ws.rs.ext.MessageBodyReader}) and are
 * supplied by the user. These providers will be bound into the injection manager
 * annotated by a {@link Custom &#64;Custom} qualifier annotation.
 * </p>
 * <p>
 * Use the {@code &#64;Custom} qualifier annotation to retrieve these providers
 * from injection manager. You may also use a one of the provider accessor utility
 * method defined in {@link Providers} class.
 * </p>
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Michal Gajdos
 */
public class ProviderBinder {

    private final InjectionManager injectionManager;

    /**
     * Create new provider binder instance.
     *
     * @param injectionManager the binder will use to bind the providers into.
     */
    public ProviderBinder(final InjectionManager injectionManager) {
        this.injectionManager = injectionManager;
    }

    /**
     * Bind contract provider model to a provider class using the supplied injection manager.
     *
     * @param providerClass provider class.
     * @param model         contract provider model.
     */
    public static void bindProvider(Class<?> providerClass, ContractProvider model, InjectionManager injectionManager) {
        injectionManager.register(CompositeBinder.wrap(createProviderBinders(providerClass, model)));
    }

    private static Collection<Binder> createProviderBinders(Class<?> providerClass, ContractProvider model) {
        /* Create a Binder of the Provider with the concrete contract. */
        Function<Class, Binder> binderFunction = contract -> new AbstractBinder() {
            @Override
            @SuppressWarnings("unchecked")
            protected void configure() {
                ClassBinding builder = bind(providerClass)
                        .in(model.getScope())
                        .qualifiedBy(CustomAnnotationLiteral.INSTANCE)
                        .to(contract);

                int priority = model.getPriority(contract);
                if (priority > ContractProvider.NO_PRIORITY) {
                    builder.ranked(priority);
                }
            }
        };

        /* Create Binders with all contracts and return their collection. */
        return model.getContracts().stream()
                .map(binderFunction)
                .collect(Collectors.toList());
    }

    /**
     * Bind contract provider model to a provider instance using the supplied injection manager.
     * <p>
     * Scope value specified in the {@link ContractProvider contract provider model}
     * is ignored as instances can only be bound as "singletons".
     *
     * @param providerInstance provider instance.
     * @param model            contract provider model.
     */
    public static void bindProvider(Object providerInstance, ContractProvider model, InjectionManager injectionManager) {
        injectionManager.register(CompositeBinder.wrap(createProviderBinders(providerInstance, model)));
    }

    private static Collection<Binder> createProviderBinders(Object providerInstance, ContractProvider model) {
        /* Create a Binder of the Provider with the concrete contract. */
        Function<Class, Binder> binderFunction = contract -> new AbstractBinder() {
            @Override
            @SuppressWarnings("unchecked")
            protected void configure() {
                InstanceBinding builder = bind(providerInstance)
                        .qualifiedBy(CustomAnnotationLiteral.INSTANCE)
                        .to(contract);

                int priority = model.getPriority(contract);
                if (priority > ContractProvider.NO_PRIORITY) {
                    builder.ranked(priority);
                }
            }
        };

        /* Create Binders with all contracts and return their collection. */
        return model.getContracts().stream()
                .map(binderFunction)
                .collect(Collectors.toList());
    }

    /**
     * Bind all providers contained in {@code providerBag} (classes and instances) using injection manager. Configuration is
     * also committed.
     *
     * @param componentBag     bag of provider classes and instances.
     * @param injectionManager injection manager the binder will use to bind the providers into.
     */
    public static void bindProviders(final ComponentBag componentBag, final InjectionManager injectionManager) {
        bindProviders(componentBag, null, Collections.emptySet(), injectionManager);
    }

    /**
     * Bind all providers contained in {@code providerBag} (classes and instances) using injection manager. Configuration is
     * also committed.
     *
     * @param componentBag      bag of provider classes and instances.
     * @param constrainedTo     current runtime (client or server).
     * @param registeredClasses classes which are manually registered by the user (not found by the classpath scanning).
     * @param injectionManager  injection manager the binder will use to bind the providers into.
     */
    public static void bindProviders(ComponentBag componentBag,
                                     RuntimeType constrainedTo,
                                     Set<Class<?>> registeredClasses,
                                     InjectionManager injectionManager) {
        Predicate<ContractProvider> filter = ComponentBag.EXCLUDE_EMPTY
                .and(ComponentBag.excludeMetaProviders(injectionManager));

        /*
         * Check the {@code component} whether it is correctly configured for client or server {@link RuntimeType runtime}.
         */
        Predicate<Class<?>> correctlyConfigured =
                componentClass -> Providers.checkProviderRuntime(
                        componentClass,
                        componentBag.getModel(componentClass),
                        constrainedTo,
                        registeredClasses == null || !registeredClasses.contains(componentClass),
                        false);

        /*
         * These binder will be registered to InjectionManager at the end of method because of a bulk registration to avoid a
         * registration each binder alone.
         */
        Collection<Binder> binderToRegister = new ArrayList<>();

        // Bind provider classes except for pure meta-providers and providers with empty contract models (e.g. resources)
        Set<Class<?>> classes = new LinkedHashSet<>(componentBag.getClasses(filter));
        if (constrainedTo != null) {
            classes = classes.stream()
                    .filter(correctlyConfigured)
                    .collect(Collectors.toSet());
        }
        for (final Class<?> providerClass : classes) {
            final ContractProvider model = componentBag.getModel(providerClass);
            binderToRegister.addAll(createProviderBinders(providerClass, model));
        }

        // Bind provider instances except for pure meta-providers and providers with empty contract models (e.g. resources)
        Set<Object> instances = componentBag.getInstances(filter);
        if (constrainedTo != null) {
            instances = instances.stream()
                    .filter(component -> correctlyConfigured.test(component.getClass()))
                    .collect(Collectors.toSet());
        }
        for (final Object provider : instances) {
            final ContractProvider model = componentBag.getModel(provider.getClass());
            binderToRegister.addAll(createProviderBinders(provider, model));
        }

        injectionManager.register(CompositeBinder.wrap(binderToRegister));
    }

    @SuppressWarnings("unchecked")
    private static <T> Collection<Binder> createInstanceBinders(T instance) {
        Function<Class, Binder> binderFunction = contract ->
                new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(instance).to(contract).qualifiedBy(CustomAnnotationLiteral.INSTANCE);
                    }
                };

        return Providers.getProviderContracts(instance.getClass()).stream()
                .map(binderFunction)
                .collect(Collectors.toList());
    }

    /**
     * Register/bind custom provider instances. Registered providers will be handled
     * always as Singletons.
     *
     * @param instances custom provider instances.
     */
    public void bindInstances(final Iterable<Object> instances) {
        List<Object> instancesList = new ArrayList<>();
        instances.forEach(instancesList::add);
        bindInstances(instancesList);
    }

    /**
     * Register/bind custom provider instances. Registered providers will be handled
     * always as Singletons.
     *
     * @param instances custom provider instances.
     */
    public void bindInstances(final Collection<Object> instances) {
        List<Binder> binders = instances.stream()
                .map(ProviderBinder::createInstanceBinders)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        injectionManager.register(CompositeBinder.wrap(binders));
    }

    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(final Class<?>... classes) {
        bindClasses(Arrays.asList(classes), false);
    }

    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(final Iterable<Class<?>> classes) {
        List<Class<?>> classesList = new ArrayList<>();
        classes.forEach(classesList::add);
        bindClasses(classesList, false);
    }

    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(final Collection<Class<?>> classes) {
        bindClasses(classes, false);
    }

    /**
     * Register/bind custom provider classes that may also be resources. Registered
     * providers/resources will be handled always as Singletons unless annotated by
     * {@link PerLookup}.
     * <p>
     * <p>
     * If {@code bindAsResources} is set to {@code true}, the providers will also be bound
     * as resources.
     * </p>
     *
     * @param classes       custom provider classes.
     * @param bindResources if {@code true}, the provider classes will also be bound as
     *                      resources.
     */
    public void bindClasses(Collection<Class<?>> classes, boolean bindResources) {
        List<Binder> binders = classes.stream()
                .map(clazz -> createClassBinders(clazz, bindResources))
                .collect(Collectors.toList());

        injectionManager.register(CompositeBinder.wrap(binders));
    }

    @SuppressWarnings("unchecked")
    private <T> Binder createClassBinders(Class<T> clazz, boolean isResource) {
        final Class<? extends Annotation> scope = getProviderScope(clazz);

        if (isResource) {
            return new AbstractBinder() {
                @Override
                protected void configure() {
                    ClassBinding<T> descriptor = bindAsContract(clazz).in(scope);

                    for (Class contract : Providers.getProviderContracts(clazz)) {
                        descriptor.addAlias(contract)
                                .in(scope.getName())
                                .qualifiedBy(CustomAnnotationLiteral.INSTANCE);
                    }
                }
            };
        } else {
            return new AbstractBinder() {
                @Override
                protected void configure() {
                    ClassBinding<T> builder = bind(clazz).in(scope).qualifiedBy(CustomAnnotationLiteral.INSTANCE);
                    Providers.getProviderContracts(clazz).forEach(contract -> builder.to((Class<? super T>) contract));
                }
            };
        }
    }

    private Class<? extends Annotation> getProviderScope(final Class<?> clazz) {
        Class<? extends Annotation> scope = Singleton.class;
        if (clazz.isAnnotationPresent(PerLookup.class)) {
            scope = PerLookup.class;
        }
        return scope;
    }
}
