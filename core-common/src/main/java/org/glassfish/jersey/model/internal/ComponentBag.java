/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.model.internal;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.NameBinding;
import javax.ws.rs.core.Feature;

import javax.annotation.Priority;
import javax.inject.Scope;

import org.glassfish.jersey.Severity;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;

/**
 * An internal Jersey container for custom component classes and instances.
 * <p/>
 * The component bag can automatically compute a {@link ContractProvider contract provider} model
 * for the registered component type and stores it with the component registration.
 * <p>
 * The rules for managing components inside a component bag are derived from the
 * rules of JAX-RS {@link javax.ws.rs.core.Configurable} API. In short:
 * <ul>
 * <li>The iteration order of registered components mirrors the registration order
 * of these components.</li>
 * <li>There can be only one registration for any given component type.</li>
 * <li>Existing registrations cannot be overridden (any attempt to override
 * an existing registration will be rejected).</li>
 * </ul>
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ComponentBag {
    /**
     * A filtering strategy that excludes all pure meta-provider models (i.e. models that only contain
     * recognized meta-provider contracts - {@link javax.ws.rs.core.Feature} and/or {@link Binder} and/or external meta-provider
     * from {@link org.glassfish.jersey.internal.inject.InjectionManager#isRegistrable(Class)}).
     * <p>
     * This filter predicate returns {@code false} for all {@link org.glassfish.jersey.model.ContractProvider contract provider models}
     * that represent a model containing only recognized meta-provider contracts.
     * </p>
     */
    private static final Predicate<ContractProvider> EXCLUDE_META_PROVIDERS = model -> {
        final Set<Class<?>> contracts = model.getContracts();
        if (contracts.isEmpty()) {
            return true;
        }

        byte count = 0;
        if (contracts.contains(Feature.class)) {
            count++;
        }
        if (contracts.contains(Binder.class)) {
            count++;
        }
        return contracts.size() > count;
    };


    private static final Function<Object, Binder> CAST_TO_BINDER = Binder.class::cast;

    /**
     * A method creates the {@link Predicate} which is able to filter all Jersey meta-providers along with the components which
     * is able to register the current used {@link InjectionManager}.
     *
     * @param injectionManager current injection manager.
     * @return {@code Predicate} excluding Jersey meta-providers and the specific ones for a current {@code InjectionManager}.
     */
    public static Predicate<ContractProvider> excludeMetaProviders(InjectionManager injectionManager) {
        return EXCLUDE_META_PROVIDERS.and(model -> !injectionManager.isRegistrable(model.getImplementationClass()));
    }

    /**
     * A filtering strategy that includes only models that contain contract registrable by
     * {@link InjectionManager}.
     * <p>
     * This filter predicate returns {@code true} for all {@link org.glassfish.jersey.model.ContractProvider contract provider models}
     * that represent an object which can be registered using specific {@link InjectionManager}
     * contract.
     * </p>
     */
    public static final BiPredicate<ContractProvider, InjectionManager> EXTERNAL_ONLY = (model, injectionManager) ->
            model.getImplementationClass() != null && injectionManager.isRegistrable(model.getImplementationClass());

    /**
     * A filtering strategy that includes only models that contain {@link Binder} provider contract.
     * <p>
     * This filter predicate returns {@code true} for all {@link org.glassfish.jersey.model.ContractProvider contract provider models}
     * that represent a provider registered to provide {@link Binder} contract.
     * </p>
     */
    public static final Predicate<ContractProvider> BINDERS_ONLY = model -> model.getContracts().contains(Binder.class);

    /**
     * A filtering strategy that includes only models that contain {@link ExecutorServiceProvider} provider contract.
     * <p>
     * This filter predicate returns {@code true} for all {@link org.glassfish.jersey.model.ContractProvider contract provider models}
     * that represent a provider registered to provide {@link ExecutorServiceProvider} contract.
     * </p>
     */
    public static final Predicate<ContractProvider> EXECUTOR_SERVICE_PROVIDER_ONLY =
            model -> model.getContracts().contains(ExecutorServiceProvider.class)
                    && !model.getContracts().contains(ScheduledExecutorServiceProvider.class);

    /**
     * A filtering strategy that includes only models that contain {@link ScheduledExecutorServiceProvider} provider contract.
     * <p>
     * This filter predicate returns {@code true} for all {@link org.glassfish.jersey.model.ContractProvider contract provider models}
     * that represent a provider registered to provide {@link ScheduledExecutorServiceProvider} contract.
     * </p>
     */
    public static final Predicate<ContractProvider> SCHEDULED_EXECUTOR_SERVICE_PROVIDER_ONLY =
            model -> model.getContracts().contains(ScheduledExecutorServiceProvider.class);

    /**
     * A filtering strategy that excludes models with no recognized contracts.
     * <p>
     * This filter predicate returns {@code false} for all {@link org.glassfish.jersey.model.ContractProvider contract provider models}
     * that are empty, i.e. do not contain any recognized contracts.
     * </p>
     */
    public static final Predicate<ContractProvider> EXCLUDE_EMPTY = model -> !model.getContracts().isEmpty();

    /**
     * A filtering strategy that accepts any contract provider model.
     * <p>
     * This filter predicate returns {@code true} for any contract provider model.
     * </p>
     */
    public static final Predicate<ContractProvider> INCLUDE_ALL = contractProvider -> true;

    /**
     * Contract provider model enhancer that builds a model as is, without any
     * modifications.
     */
    static final Inflector<ContractProvider.Builder, ContractProvider> AS_IS = ContractProvider.Builder::build;

    /**
     * Contract provider model registration strategy.
     */
    private final Predicate<ContractProvider> registrationStrategy;
    /**
     * Registered component classes collection and it's immutable view.
     */
    private final Set<Class<?>> classes;
    private final Set<Class<?>> classesView;
    /**
     * Registered component instances collection and it's immutable view.
     */
    private final Set<Object> instances;
    private final Set<Object> instancesView;
    /**
     * Map of contract provider models for the registered component classes and instances
     * it's immutable view.
     */
    private final Map<Class<?>, ContractProvider> models;
    private final Set<Class<?>> modelKeysView;

    /**
     * Create new empty component bag.
     *
     * @param registrationStrategy function driving the decision (based on the introspected
     *                             {@link org.glassfish.jersey.model.ContractProvider contract provider model}) whether
     *                             or not should the component class registration continue
     *                             towards a successful completion.
     * @return a new empty component bag.
     */
    public static ComponentBag newInstance(Predicate<ContractProvider> registrationStrategy) {
        return new ComponentBag(registrationStrategy);
    }

    /**
     * If {@code T} object is registered in {@link ComponentBag} using the {@link Binder}, {@code T} is not visible using the
     * methods for getting classes and instances {@link ComponentBag#getClasses(Predicate)} and
     * {@link ComponentBag#getInstances(Predicate)}.
     * <p>
     * Method selects all {@link org.glassfish.jersey.internal.inject.Binding bindings} and picks up the instances or creates
     * the instances from {@link ClassBinding} (injection does not work at this moment).
     *
     * @param injectionManager injection manager to create an object from {@code T} class.
     * @param componentBag     component bag which provides registered binders.
     * @return all instances/classes registered using binders.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getFromBinders(InjectionManager injectionManager, ComponentBag componentBag,
            Function<Object, T> cast, Predicate<Binding> filter) {

        Function<Binding, Object> bindingToObject = binding -> {
            if (binding instanceof ClassBinding) {
                ClassBinding classBinding = (ClassBinding) binding;
                return injectionManager.createAndInitialize(classBinding.getService());
            } else {
                InstanceBinding instanceBinding = (InstanceBinding) binding;
                return instanceBinding.getService();
            }
        };

        return componentBag.getInstances(ComponentBag.BINDERS_ONLY).stream()
                .map(CAST_TO_BINDER)
                .flatMap(binder -> Bindings.getBindings(injectionManager, binder).stream())
                .filter(filter)
                .map(bindingToObject)
                .map(cast)
                .collect(Collectors.toList());
    }

    private ComponentBag(Predicate<ContractProvider> registrationStrategy) {
        this.registrationStrategy = registrationStrategy;

        this.classes = new LinkedHashSet<>();
        this.instances = new LinkedHashSet<>();
        this.models = new IdentityHashMap<>();

        this.classesView = Collections.unmodifiableSet(classes);
        this.instancesView = Collections.unmodifiableSet(instances);
        this.modelKeysView = Collections.unmodifiableSet(models.keySet());
    }

    private ComponentBag(Predicate<ContractProvider> registrationStrategy,
                         Set<Class<?>> classes,
                         Set<Object> instances,
                         Map<Class<?>, ContractProvider> models) {
        this.registrationStrategy = registrationStrategy;

        this.classes = classes;
        this.instances = instances;
        this.models = models;

        this.classesView = Collections.unmodifiableSet(classes);
        this.instancesView = Collections.unmodifiableSet(instances);
        this.modelKeysView = Collections.unmodifiableSet(models.keySet());
    }

    /**
     * Register a component class using a given registration strategy.
     *
     * @param componentClass class to be introspected as a contract provider and registered, based
     *                       on the registration strategy decision.
     * @param modelEnhancer  custom contract provider model enhancer.
     * @return {@code true} if the component registration was successful.
     */
    public boolean register(Class<?> componentClass, Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        final boolean result = registerModel(componentClass, ContractProvider.NO_PRIORITY, null, modelEnhancer);
        if (result) {
            classes.add(componentClass);
        }
        return result;
    }

    /**
     * Register a component class as a contract provider with an explicitly specified binding priority.
     *
     * @param componentClass class to be introspected as a contract provider and registered.
     * @param priority       explicitly specified binding priority for the provider contracts implemented
     *                       by the component.
     * @param modelEnhancer  custom contract provider model enhancer.
     * @return {@code true} if the component registration was successful.
     */
    public boolean register(Class<?> componentClass,
                            int priority,
                            Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        final boolean result = registerModel(componentClass, priority, null, modelEnhancer);
        if (result) {
            classes.add(componentClass);
        }
        return result;
    }

    /**
     * Register a component class as a contract provider for the specified contracts.
     *
     * @param componentClass class to be introspected as a contract provider and registered.
     * @param contracts      contracts to bind the component class to.
     * @param modelEnhancer  custom contract provider model enhancer.
     * @return {@code true} if the component registration was successful.
     */
    public boolean register(Class<?> componentClass,
                            Set<Class<?>> contracts,
                            Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        final boolean result =
                registerModel(componentClass, ContractProvider.NO_PRIORITY, asMap(contracts), modelEnhancer);
        if (result) {
            classes.add(componentClass);
        }
        return result;
    }

    /**
     * Register a component class as a contract provider for the specified contracts.
     *
     * @param componentClass class to be introspected as a contract provider and registered.
     * @param contracts      contracts with their priorities to bind the component class to.
     * @param modelEnhancer  custom contract provider model enhancer.
     * @return {@code true} if the component registration was successful.
     */
    public boolean register(Class<?> componentClass,
                            Map<Class<?>, Integer> contracts,
                            Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        final boolean result =
                registerModel(componentClass, ContractProvider.NO_PRIORITY, contracts, modelEnhancer);
        if (result) {
            classes.add(componentClass);
        }
        return result;
    }

    /**
     * Register a component using a given registration strategy.
     *
     * @param component     instance to be introspected as a contract provider and registered, based
     *                      on the registration strategy decision.
     * @param modelEnhancer custom contract provider model enhancer.
     * @return {@code true} if the component registration was successful.
     */
    public boolean register(Object component, Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        final Class<?> componentClass = component.getClass();
        final boolean result = registerModel(componentClass, ContractProvider.NO_PRIORITY, null, modelEnhancer);
        if (result) {
            instances.add(component);
        }
        return result;
    }

    /**
     * Register a component as a contract provider with an explicitly specified binding priority.
     *
     * @param component     instance to be introspected as a contract provider and registered, based
     *                      on the registration strategy decision.
     * @param priority      explicitly specified binding priority for the provider contracts implemented
     *                      by the component.
     * @param modelEnhancer custom contract provider model enhancer.
     * @return {@code true} if the component registration was successful.
     */
    public boolean register(Object component,
                            int priority,
                            Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        final Class<?> componentClass = component.getClass();
        final boolean result = registerModel(componentClass, priority, null, modelEnhancer);
        if (result) {
            instances.add(component);
        }
        return result;
    }

    /**
     * Register a component as a contract provider for the specified contracts.
     *
     * @param component     instance to be introspected as a contract provider and registered, based
     *                      on the registration strategy decision.
     * @param contracts     contracts to bind the component to.
     * @param modelEnhancer custom contract provider model enhancer.
     * @return {@code true} if the component registration was successful.
     */
    public boolean register(Object component,
                            Set<Class<?>> contracts,
                            Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        final Class<?> componentClass = component.getClass();
        final boolean result =
                registerModel(componentClass, ContractProvider.NO_PRIORITY, asMap(contracts), modelEnhancer);
        if (result) {
            instances.add(component);
        }
        return result;
    }

    /**
     * Register a component as a contract provider for the specified contracts.
     *
     * @param component     instance to be introspected as a contract provider and registered, based
     *                      on the registration strategy decision.
     * @param contracts     contracts with their priorities to bind the component to.
     * @param modelEnhancer custom contract provider model enhancer.
     * @return {@code true} if the component registration was successful.
     */
    public boolean register(Object component,
                            Map<Class<?>, Integer> contracts,
                            Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        final Class<?> componentClass = component.getClass();
        final boolean result =
                registerModel(componentClass, ContractProvider.NO_PRIORITY, contracts, modelEnhancer);
        if (result) {
            instances.add(component);
        }
        return result;
    }

    /**
     * Register a {@link ContractProvider contract provider model} for a given  class.
     *
     * @param componentClass  registered component class.
     * @param defaultPriority default component priority. If {@value ContractProvider#NO_PRIORITY},
     *                        the value from the component class {@link javax.annotation.Priority} annotation will be used
     *                        (if any).
     * @param contractMap     map of contracts and their binding priorities. If {@code null}, the contracts will
     *                        gathered by introspecting the component class. Content of the contract map
     *                        may be modified during the registration processing.
     * @param modelEnhancer   custom contract provider model enhancer.
     * @return {@code true} upon successful registration of a contract provider model for a given component class,
     *         {@code false} otherwise.
     */
    private boolean registerModel(final Class<?> componentClass,
                                  final int defaultPriority,
                                  final Map<Class<?>, Integer> contractMap,
                                  final Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {

        return Errors.process(() -> {
            if (models.containsKey(componentClass)) {
                Errors.error(LocalizationMessages.COMPONENT_TYPE_ALREADY_REGISTERED(componentClass),
                             Severity.HINT);
                return false;
            }

            // Register contracts
            final ContractProvider model = modelFor(componentClass, defaultPriority, contractMap, modelEnhancer);

            // Apply registration strategy
            if (!registrationStrategy.test(model)) {
                return false;
            }

            models.put(componentClass, model);
            return true;
        });
    }

    /**
     * Create a contract provider model by introspecting a component class.
     *
     * @param componentClass component class to create contract provider model for.
     * @return contract provider model for the class.
     */
    public static ContractProvider modelFor(final Class<?> componentClass) {
        return modelFor(componentClass, ContractProvider.NO_PRIORITY, null, AS_IS);
    }

    /**
     * Create a contract provider for a given component class.
     *
     * @param componentClass  component class to create contract provider model for.
     * @param defaultPriority default component priority. If {@value ContractProvider#NO_PRIORITY},
     *                        the value from the component class {@link javax.annotation.Priority} annotation will be used
     *                        (if any).
     * @param contractMap     map of contracts and their binding priorities. If {@code null}, the contracts will
     *                        gathered by introspecting the component class. Content of the contract map
     *                        may be modified during the registration processing.
     * @param modelEnhancer   custom contract provider model enhancer.
     * @return contract provider model for the class.
     */
    private static ContractProvider modelFor(final Class<?> componentClass,
                                             final int defaultPriority,
                                             final Map<Class<?>, Integer> contractMap,
                                             final Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
        Map<Class<?>, Integer> contracts = contractMap;
        if (contracts == null) { // introspect
            contracts = asMap(Providers.getProviderContracts(componentClass));
        } else { // filter custom contracts
            final Iterator<Class<?>> it = contracts.keySet().iterator();
            while (it.hasNext()) {
                final Class<?> contract = it.next();
                if (contract == null) {
                    it.remove();
                    continue;
                }

                boolean failed = false;
                if (!Providers.isSupportedContract(contract)) {
                    Errors.error(LocalizationMessages.CONTRACT_NOT_SUPPORTED(contract, componentClass),
                                 Severity.WARNING);
                    failed = true;
                }
                if (!contract.isAssignableFrom(componentClass)) {
                    Errors.error(LocalizationMessages.CONTRACT_NOT_ASSIGNABLE(contract, componentClass),
                                 Severity.WARNING);
                    failed = true;
                }
                if (failed) {
                    it.remove();
                }
            }
        }
        final ContractProvider.Builder builder = ContractProvider.builder(componentClass)
                .addContracts(contracts)
                .defaultPriority(defaultPriority);

        // Process annotations (priority, name bindings, scope)
        final boolean useAnnotationPriority = defaultPriority == ContractProvider.NO_PRIORITY;
        for (Annotation annotation : componentClass.getAnnotations()) {
            if (annotation instanceof Priority) {
                if (useAnnotationPriority) {
                    builder.defaultPriority(((Priority) annotation).value());
                }
            } else {
                for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
                    if (metaAnnotation instanceof NameBinding) {
                        builder.addNameBinding(annotation.annotationType());
                    }
                    if (metaAnnotation instanceof Scope) {
                        builder.scope(annotation.annotationType());
                    }
                }
            }
        }

        return modelEnhancer.apply(builder);
    }

    private static Map<Class<?>, Integer> asMap(Set<Class<?>> contractSet) {
        Map<Class<?>, Integer> contracts = new IdentityHashMap<>();
        for (Class<?> contract : contractSet) {
            contracts.put(contract, ContractProvider.NO_PRIORITY);
        }
        return contracts;
    }

    /**
     * Get all registered component classes, including {@link javax.ws.rs.core.Feature features}
     * and {@link Binder binders} meta-providers.
     *
     * @return all registered component classes.
     */
    public Set<Class<?>> getClasses() {
        return classesView;
    }

    /**
     * Get all registered component instances, including {@link javax.ws.rs.core.Feature features}
     * and {@link Binder binders} meta-providers.
     *
     * @return all registered component instances.
     */
    public Set<Object> getInstances() {
        return instancesView;
    }

    /**
     * Get a subset of all registered component classes using the {@code filter} predicate
     * to determine for each component class based on it's contract provider class model whether
     * it should be kept or filtered out.
     *
     * @param filter function that decides whether a particular class should be returned
     *               or not.
     * @return filtered subset of registered component classes.
     */
    public Set<Class<?>> getClasses(final Predicate<ContractProvider> filter) {
        return classesView.stream()
                          .filter(input -> {
                              final ContractProvider model = getModel(input);
                              return filter.test(model);
                          })
                          .collect(Collectors.toSet());
    }

    /**
     * Get a subset of all registered component instances using the {@code filter} predicate
     * to determine for each component instance based on it's contract provider class model whether
     * it should be kept or filtered out.
     *
     * @param filter function that decides whether a particular class should be returned
     *               or not.
     * @return filtered subset of registered component instances.
     */
    public Set<Object> getInstances(final Predicate<ContractProvider> filter) {

        return instancesView.stream()
                            .filter(input -> {
                                final ContractProvider model = getModel(input.getClass());
                                return filter.test(model);
                            })
                            .collect(Collectors.toSet());
    }

    /**
     * Get an unmodifiable view of all component classes, for which a registration exists
     * (either class or instance based) in the component bag.
     *
     * @return set of classes of all component classes and instances registered in this
     *         component bag.
     */
    public Set<Class<?>> getRegistrations() {
        return modelKeysView;
    }

    /**
     * Get a model for a given component class, or {@code null} if no such component is registered
     * in the component bag.
     *
     * @param componentClass class of the registered component to retrieve the
     *                       contract provider model for.
     * @return model for a given component class, or {@code null} if no such component is registered.
     */
    public ContractProvider getModel(Class<?> componentClass) {
        return models.get(componentClass);
    }

    /**
     * Get a copy of this component bag.
     *
     * @return component bag copy.
     */
    public ComponentBag copy() {
        return new ComponentBag(
                registrationStrategy,
                new LinkedHashSet<>(classes),
                new LinkedHashSet<>(instances),
                new IdentityHashMap<>(models));
    }

    /**
     * Get immutable copy of a component bag.
     *
     * @return immutable view of a component bag.
     */
    public ComponentBag immutableCopy() {
        return new ImmutableComponentBag(this);
    }

    /**
     * Removes all the component registrations and resets the component bag instance to
     * a state as if it was create anew.
     */
    public void clear() {
        this.classes.clear();
        this.instances.clear();
        this.models.clear();
    }

    /**
     * Clear and initialize the component registrations from given bag instance.
     *
     * @param bag component bag to initialize this one with.
     */
    void loadFrom(final ComponentBag bag) {
        clear();

        this.classes.addAll(bag.classes);
        this.instances.addAll(bag.instances);
        this.models.putAll(bag.models);
    }

    /**
     * Immutable version of {@link org.glassfish.jersey.model.internal.ComponentBag}.
     *
     * @author Marek Potociar (marek.potociar at oracle.com)
     */
    private static class ImmutableComponentBag extends ComponentBag {
        ImmutableComponentBag(ComponentBag original) {
            super(original.registrationStrategy,
                  new LinkedHashSet<>(original.classes),
                  new LinkedHashSet<>(original.instances),
                  new IdentityHashMap<>(original.models));
        }

        @Override
        public boolean register(Class<?> componentClass, Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
            throw new IllegalStateException("This instance is read-only.");
        }

        @Override
        public boolean register(Class<?> componentClass,
                                int priority,
                                Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
            throw new IllegalStateException("This instance is read-only.");
        }

        @Override
        public boolean register(Class<?> componentClass,
                                Set<Class<?>> contracts,
                                Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
            throw new IllegalStateException("This instance is read-only.");
        }

        @Override
        public boolean register(Class<?> componentClass,
                                Map<Class<?>, Integer> contracts,
                                Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
            throw new IllegalStateException("This instance is read-only.");
        }

        @Override
        public boolean register(Object component, Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
            throw new IllegalStateException("This instance is read-only.");
        }

        @Override
        public boolean register(Object component,
                                int priority,
                                Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
            throw new IllegalStateException("This instance is read-only.");
        }

        @Override
        public boolean register(Object component,
                                Set<Class<?>> contracts,
                                Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
            throw new IllegalStateException("This instance is read-only.");
        }

        @Override
        public boolean register(Object component,
                                Map<Class<?>, Integer> contracts,
                                Inflector<ContractProvider.Builder, ContractProvider> modelEnhancer) {
            throw new IllegalStateException("This instance is read-only.");
        }

        @Override
        public ComponentBag copy() {
            // we're immutable => no need to copy
            return this;
        }

        @Override
        public ComponentBag immutableCopy() {
            // we're immutable => no need to copy
            return this;
        }

        @Override
        public void clear() {
            throw new IllegalStateException("This instance is read-only.");
        }
    }
}
