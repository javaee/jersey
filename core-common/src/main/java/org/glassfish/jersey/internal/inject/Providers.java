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

package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.spi.Contract;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Utility class providing a set of utility methods for easier and more type-safe
 * interaction with HK2 injection layer.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
public final class Providers {

    private static final Logger LOGGER = Logger.getLogger(Providers.class.getName());

    /**
     * Map of all standard JAX-RS providers and their run-time affinity.
     */
    private static final Map<Class<?>, ProviderRuntime> JAX_RS_PROVIDER_INTERFACE_WHITELIST =
            getJaxRsProviderInterfaces();

    private static Map<Class<?>, ProviderRuntime> getJaxRsProviderInterfaces() {
        final Map<Class<?>, ProviderRuntime> interfaces = new HashMap<Class<?>, ProviderRuntime>();

        interfaces.put(javax.ws.rs.ext.ContextResolver.class, ProviderRuntime.BOTH);
        interfaces.put(javax.ws.rs.ext.ExceptionMapper.class, ProviderRuntime.BOTH);
        interfaces.put(javax.ws.rs.ext.MessageBodyReader.class, ProviderRuntime.BOTH);
        interfaces.put(javax.ws.rs.ext.MessageBodyWriter.class, ProviderRuntime.BOTH);
        interfaces.put(javax.ws.rs.ext.ReaderInterceptor.class, ProviderRuntime.BOTH);
        interfaces.put(javax.ws.rs.ext.WriterInterceptor.class, ProviderRuntime.BOTH);
        interfaces.put(javax.ws.rs.ext.ParamConverterProvider.class, ProviderRuntime.BOTH);

        interfaces.put(javax.ws.rs.container.ContainerRequestFilter.class, ProviderRuntime.SERVER);
        interfaces.put(javax.ws.rs.container.ContainerResponseFilter.class, ProviderRuntime.SERVER);
        interfaces.put(javax.ws.rs.container.DynamicFeature.class, ProviderRuntime.SERVER);

        interfaces.put(javax.ws.rs.client.ClientResponseFilter.class, ProviderRuntime.CLIENT);
        interfaces.put(javax.ws.rs.client.ClientRequestFilter.class, ProviderRuntime.CLIENT);
        interfaces.put(javax.ws.rs.client.RxInvokerProvider.class, ProviderRuntime.CLIENT);

        return interfaces;
    }

    /**
     * Map of all supported external (i.e. non-Jersey) contracts and their run-time affinity.
     */
    private static final Map<Class<?>, ProviderRuntime> EXTERNAL_PROVIDER_INTERFACE_WHITELIST =
            getExternalProviderInterfaces();

    private static Map<Class<?>, ProviderRuntime> getExternalProviderInterfaces() {
        final Map<Class<?>, ProviderRuntime> interfaces = new HashMap<Class<?>, ProviderRuntime>();

        // JAX-RS
        interfaces.putAll(JAX_RS_PROVIDER_INTERFACE_WHITELIST);
        interfaces.put(javax.ws.rs.core.Feature.class, ProviderRuntime.BOTH);

        // HK2
        interfaces.put(org.glassfish.hk2.utilities.Binder.class, ProviderRuntime.BOTH);

        return interfaces;
    }

    private enum ProviderRuntime {

        BOTH(null), SERVER(RuntimeType.SERVER), CLIENT(RuntimeType.CLIENT);

        private final RuntimeType runtime;

        private ProviderRuntime(final RuntimeType runtime) {
            this.runtime = runtime;
        }

        public RuntimeType getRuntime() {
            return runtime;
        }
    }

    private Providers() {
    }

    /**
     * Wrap an instance into a HK2 service factory.
     *
     * @param <T>      Java type if the contract produced by the provider and factory.
     * @param instance instance to be wrapped into (and provided by) the factory.
     * @return HK2 service factory wrapping and providing the instance.
     */
    public static <T> Factory<T> factoryOf(final T instance) {
        return new Factory<T>() {

            @Override
            public T provide() {
                return instance;
            }

            @Override
            public void dispose(final T instance) {
                //not used
            }
        };
    }

    /**
     * Get the set of default providers registered for the given service provider contract
     * in the underlying {@link ServiceLocator HK2 service locator} container.
     *
     * @param <T>      service provider contract Java type.
     * @param locator  underlying HK2 service locator.
     * @param contract service provider contract.
     * @return set of all available default service provider instances for the contract.
     */
    public static <T> Set<T> getProviders(final ServiceLocator locator, final Class<T> contract) {
        final Collection<ServiceHandle<T>> hk2Providers = getServiceHandles(locator, contract);
        return getClasses(hk2Providers);
    }

    /**
     * Get the set of all custom providers registered for the given service provider contract
     * in the underlying {@link ServiceLocator HK2 service locator} container.
     *
     * @param <T>      service provider contract Java type.
     * @param locator  underlying HK2 service locator.
     * @param contract service provider contract.
     * @return set of all available service provider instances for the contract.
     */
    public static <T> Set<T> getCustomProviders(final ServiceLocator locator, final Class<T> contract) {
        final Collection<ServiceHandle<T>> hk2Providers = getServiceHandles(locator, contract, CustomAnnotationLiteral.INSTANCE);
        return getClasses(hk2Providers);
    }

    /**
     * Get the iterable of all providers (custom and default) registered for the given service provider contract
     * in the underlying {@link ServiceLocator HK2 service locator} container.
     *
     * @param <T>      service provider contract Java type.
     * @param locator  underlying HK2 service locator.
     * @param contract service provider contract.
     * @return iterable of all available service provider instances for the contract. Return value is never null.
     */
    public static <T> Iterable<T> getAllProviders(final ServiceLocator locator, final Class<T> contract) {
        return getAllProviders(locator, contract, (Comparator<T>) null);
    }

    /**
     * Get the iterable of all {@link RankedProvider providers} (custom and default) registered for the given service provider
     * contract in the underlying {@link ServiceLocator HK2 service locator} container.
     *
     * @param <T>      service provider contract Java type.
     * @param locator  underlying HK2 service locator.
     * @param contract service provider contract.
     * @return iterable of all available ranked service providers for the contract. Return value is never null.
     */
    public static <T> Iterable<RankedProvider<T>> getAllRankedProviders(final ServiceLocator locator, final Class<T> contract) {
        final List<ServiceHandle<T>> providers = getServiceHandles(locator, contract, CustomAnnotationLiteral.INSTANCE);
        providers.addAll(getServiceHandles(locator, contract));

        final LinkedHashMap<ActiveDescriptor<T>, RankedProvider<T>> providerMap =
                new LinkedHashMap<ActiveDescriptor<T>, RankedProvider<T>>();

        for (final ServiceHandle<T> provider : providers) {
            final ActiveDescriptor<T> key = provider.getActiveDescriptor();
            if (!providerMap.containsKey(key)) {
                final Set<Type> contractTypes = key.getContractTypes();
                final Class<?> implementationClass = key.getImplementationClass();
                boolean proxyGenerated = true;
                for (Type ct : contractTypes) {
                    if (((Class<?>) ct).isAssignableFrom(implementationClass)) {
                        proxyGenerated = false;
                        break;
                    }
                }
                providerMap.put(key,
                        new RankedProvider<T>(provider.getService(), key.getRanking(), proxyGenerated ? contractTypes : null));
            }
        }

        return providerMap.values();
    }

    /**
     * Sort given providers with {@link RankedComparator ranked comparator}.
     *
     * @param comparator comparator to sort the providers with.
     * @param providers  providers to be sorted.
     * @param <T>        service provider contract Java type.
     * @return sorted {@link Iterable iterable} instance containing given providers.
     *         The returned value is never {@code null}.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static <T> Iterable<T> sortRankedProviders(final RankedComparator<T> comparator,
                                                      final Iterable<RankedProvider<T>> providers) {
        final List<RankedProvider<T>> rankedProviders = Lists.newArrayList(providers);

        Collections.sort(rankedProviders, comparator);

        return Collections2.transform(rankedProviders, new Function<RankedProvider<T>, T>() {
            @Override
            public T apply(final RankedProvider<T> input) {
                return input.getProvider();
            }
        });
    }

    /**
     * Merge and sort given providers with {@link RankedComparator ranked comparator}.
     *
     * @param comparator        comparator to sort the providers with.
     * @param providerIterables providers to be sorted.
     * @param <T>               service provider contract Java type.
     * @return merged and sorted {@link Iterable iterable} instance containing given providers.
     *         The returned value is never {@code null}.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static <T> Iterable<T> mergeAndSortRankedProviders(final RankedComparator<T> comparator,
                                                              final Iterable<Iterable<RankedProvider<T>>> providerIterables) {
        final List<RankedProvider<T>> rankedProviders = Lists.newArrayList();

        for (final Iterable<RankedProvider<T>> providers : providerIterables) {
            rankedProviders.addAll(Lists.<RankedProvider<T>>newLinkedList(providers));
        }

        Collections.sort(rankedProviders, comparator);

        return Collections2.transform(rankedProviders, new Function<RankedProvider<T>, T>() {
            @Override
            public T apply(final RankedProvider<T> input) {
                return input.getProvider();
            }
        });
    }

    /**
     * Get the sorted iterable of all {@link RankedProvider providers} (custom and default) registered for the given service
     * provider contract in the underlying {@link ServiceLocator HK2 service locator} container.
     *
     * @param <T>        service provider contract Java type.
     * @param locator    underlying HK2 service locator.
     * @param contract   service provider contract.
     * @param comparator comparator to sort the providers with.
     * @return set of all available ranked service providers for the contract. Return value is never null.
     */
    public static <T> Iterable<T> getAllProviders(final ServiceLocator locator,
                                                  final Class<T> contract,
                                                  final RankedComparator<T> comparator) {
        //noinspection unchecked
        return sortRankedProviders(comparator, getAllRankedProviders(locator, contract));
    }

    /**
     * Get collection of all {@link ServiceHandle}s bound for providers (custom and default) registered for the given service
     * provider contract in the underlying {@link ServiceLocator HK2 service locator} container.
     *
     * @param <T>        service provider contract Java type.
     * @param locator    underlying HK2 service locator.
     * @param contract   service provider contract.
     * @return set of all available service provider instances for the contract
     */
    public static <T> Collection<ServiceHandle<T>> getAllServiceHandles(final ServiceLocator locator, final Class<T> contract) {
        final List<ServiceHandle<T>> providers = getServiceHandles(locator, contract, CustomAnnotationLiteral.INSTANCE);
        providers.addAll(getServiceHandles(locator, contract));

        final LinkedHashMap<ActiveDescriptor, ServiceHandle<T>> providerMap =
                new LinkedHashMap<ActiveDescriptor, ServiceHandle<T>>();

        for (final ServiceHandle<T> provider : providers) {
            final ActiveDescriptor key = provider.getActiveDescriptor();
            if (!providerMap.containsKey(key)) {
                providerMap.put(key, provider);
            }
        }

        return providerMap.values();
    }

    private static <T> List<ServiceHandle<T>> getServiceHandles(final ServiceLocator locator, final Class<T> contract,
                                                                final Annotation... qualifiers) {

        final List<ServiceHandle<T>> allServiceHandles = qualifiers == null
                ? locator.getAllServiceHandles(contract)
                : locator.getAllServiceHandles(contract, qualifiers);

        final ArrayList<ServiceHandle<T>> serviceHandles = new ArrayList<ServiceHandle<T>>();
        for (final ServiceHandle handle : allServiceHandles) {
            //noinspection unchecked
            serviceHandles.add((ServiceHandle<T>) handle);
        }
        return serviceHandles;
    }

    /**
     * Get the iterable of all providers (custom and default) registered for the given service provider contract
     * in the underlying {@link ServiceLocator HK2 service locator} container ordered based on the given {@code comparator}.
     *
     * @param <T>        service provider contract Java type.
     * @param locator    underlying HK2 service locator.
     * @param contract   service provider contract.
     * @param comparator comparator to be used for sorting the returned providers.
     * @return set of all available service provider instances for the contract ordered using the given
     * {@link Comparator comparator}.
     */
    public static <T> Iterable<T> getAllProviders(final ServiceLocator locator,
                                                  final Class<T> contract,
                                                  final Comparator<T> comparator) {

        final List<T> providerList = new ArrayList<T>(getClasses(getAllServiceHandles(locator, contract)));

        if (comparator != null) {
            Collections.sort(providerList, comparator);
        }

        return providerList;
    }

    private static <T> Set<T> getClasses(final Collection<ServiceHandle<T>> hk2Providers) {
        if (hk2Providers.isEmpty()) {
            return Sets.newLinkedHashSet();
        } else {
            return Sets.newLinkedHashSet(Collections2.transform(hk2Providers, new ProviderToService<T>()));
        }
    }

    /**
     * Get the set of all providers registered for the given service provider contract
     * in the underlying {@link ServiceLocator HK2 locator} container.
     *
     * @param <T>        service provider contract Java type.
     * @param locator    underlying HK2 service locator.
     * @param contract   service provider contract.
     * @param comparator contract comparator used for ordering contracts in the
     *                   set.
     * @return set of all available service provider instances for the contract.
     */
    public static <T> SortedSet<T> getProviders(final ServiceLocator locator,
                                                final Class<T> contract,
                                                final Comparator<T> comparator) {
        final Collection<ServiceHandle<T>> hk2Providers = getServiceHandles(locator, contract);
        if (hk2Providers.isEmpty()) {
            return Sets.newTreeSet(comparator);
        } else {
            final TreeSet<T> set = Sets.newTreeSet(comparator);
            set.addAll(Collections2.transform(hk2Providers, new ProviderToService<T>()));
            return set;
        }
    }

    /**
     * Returns provider contracts recognized by Jersey that are implemented by the {@code clazz}.
     * Recognized provider contracts include all JAX-RS providers as well as all Jersey SPI
     * components annotated with {@link Contract &#064;Contract} annotation.
     *
     * @param clazz class to extract the provider interfaces from.
     * @return set of provider contracts implemented by the given class.
     */
    public static Set<Class<?>> getProviderContracts(final Class<?> clazz) {
        final Set<Class<?>> contracts = Sets.newIdentityHashSet();
        computeProviderContracts(clazz, contracts);
        return contracts;
    }

    private static void computeProviderContracts(final Class<?> clazz, final Set<Class<?>> contracts) {
        for (final Class<?> contract : getImplementedContracts(clazz)) {
            if (isSupportedContract(contract)) {
                contracts.add(contract);
            }
            computeProviderContracts(contract, contracts);
        }
    }

    /**
     * Check the {@code component} whether it is appropriate correctly configured for client or server
     * {@link RuntimeType runtime}.
     *
     * If a problem occurs a warning is logged and if the component is not usable at all in the current runtime
     * {@code false} is returned. For classes found during component scanning (scanned=true) certain warnings are
     * completely ignored (e.g. components {@link ConstrainedTo constrained to} the client runtime and found by
     * server-side class path scanning will be silently ignored and no warning will be logged).
     *
     * @param component         the class of the component being checked.
     * @param model             model of the component.
     * @param runtimeConstraint current runtime (client or server).
     * @param scanned           {@code false} if the component type has been registered explicitly;
     *                          {@code true} if the class has been discovered during any form of component scanning.
     * @param isResource        {@code true} if the component is also a resource class.
     * @return {@code true} if component is acceptable for use in the given runtime type, {@code false} otherwise.
     */
    public static boolean checkProviderRuntime(final Class<?> component,
                                               final ContractProvider model,
                                               final RuntimeType runtimeConstraint,
                                               final boolean scanned,
                                               final boolean isResource) {
        final Set<Class<?>> contracts = model.getContracts();
        final ConstrainedTo constrainedTo = component.getAnnotation(ConstrainedTo.class);
        final RuntimeType componentConstraint = constrainedTo == null ? null : constrainedTo.value();
        if (Feature.class.isAssignableFrom(component)) {
            return true;
        }

        final StringBuilder warnings = new StringBuilder();
        try {
            /**
             * Indicates that the provider implements at least one contract compatible
             * with it's implementation class constraint.
             */
            boolean foundComponentCompatible = componentConstraint == null;
            boolean foundRuntimeCompatibleContract = isResource && runtimeConstraint == RuntimeType.SERVER;
            for (final Class<?> contract : contracts) {
                // if the contract is common/not constrained, default to provider constraint
                final RuntimeType contractConstraint = getContractConstraint(contract, componentConstraint);
                foundRuntimeCompatibleContract |= contractConstraint == null || contractConstraint == runtimeConstraint;

                if (componentConstraint != null) {
                    if (contractConstraint != componentConstraint) {
                        //noinspection ConstantConditions
                        warnings.append(LocalizationMessages.WARNING_PROVIDER_CONSTRAINED_TO_WRONG_PACKAGE(
                                component.getName(),
                                componentConstraint.name(),
                                contract.getName(),
                                contractConstraint.name())) // is never null
                                .append(" ");
                    } else {
                        foundComponentCompatible = true;
                    }
                }
            }

            if (!foundComponentCompatible) {
                //noinspection ConstantConditions
                warnings.append(LocalizationMessages.ERROR_PROVIDER_CONSTRAINED_TO_WRONG_PACKAGE(
                        component.getName(),
                        componentConstraint.name())) // is never null
                        .append(" ");
                logProviderSkipped(warnings, component, isResource);
                return false;
            }

            final boolean isProviderRuntimeCompatible;
            // runtimeConstraint vs. providerConstraint
            isProviderRuntimeCompatible = componentConstraint == null || componentConstraint == runtimeConstraint;
            if (!isProviderRuntimeCompatible && !scanned) {
                // log failure for manually registered providers
                warnings.append(LocalizationMessages.ERROR_PROVIDER_CONSTRAINED_TO_WRONG_RUNTIME(
                        component.getName(),
                        componentConstraint.name(),
                        runtimeConstraint.name()))
                        .append(" ");

                logProviderSkipped(warnings, component, isResource);
            }

            // runtimeConstraint vs contractConstraint
            if (!foundRuntimeCompatibleContract && !scanned) {
                warnings.append(LocalizationMessages.ERROR_PROVIDER_REGISTERED_WRONG_RUNTIME(
                        component.getName(),
                        runtimeConstraint.name()))
                        .append(" ");
                logProviderSkipped(warnings, component, isResource);
                return false;
            }

            return isProviderRuntimeCompatible && foundRuntimeCompatibleContract;
        } finally {
            if (warnings.length() > 0) {
                LOGGER.log(Level.WARNING, warnings.toString());
            }
        }
    }

    private static void logProviderSkipped(final StringBuilder sb, final Class<?> provider, final boolean alsoResourceClass) {
        sb.append(alsoResourceClass
                ? LocalizationMessages.ERROR_PROVIDER_AND_RESOURCE_CONSTRAINED_TO_IGNORED(provider.getName())
                : LocalizationMessages.ERROR_PROVIDER_CONSTRAINED_TO_IGNORED(provider.getName())).append(" ");
    }

    /**
     * Check if the given Java type is a Jersey-supported contract.
     *
     * @param type contract type.
     * @return {@code true} if given type is a Jersey-supported contract, {@code false} otherwise.
     */
    public static boolean isSupportedContract(final Class<?> type) {
        return (EXTERNAL_PROVIDER_INTERFACE_WHITELIST.get(type) != null || type.isAnnotationPresent(Contract.class));
    }

    private static RuntimeType getContractConstraint(final Class<?> clazz, final RuntimeType defaultConstraint) {
        final ProviderRuntime jaxRsProvider = EXTERNAL_PROVIDER_INTERFACE_WHITELIST.get(clazz);

        RuntimeType result = null;
        if (jaxRsProvider != null) {
            result = jaxRsProvider.getRuntime();
        } else if (clazz.getAnnotation(Contract.class) != null) {
            final ConstrainedTo constrainedToAnnotation = clazz.getAnnotation(ConstrainedTo.class);
            if (constrainedToAnnotation != null) {
                result = constrainedToAnnotation.value();
            }
        }

        return (result == null) ? defaultConstraint : result;
    }

    private static Iterable<Class<?>> getImplementedContracts(final Class<?> clazz) {
        final Collection<Class<?>> list = new LinkedList<Class<?>>();

        Collections.addAll(list, clazz.getInterfaces());

        final Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            list.add(superclass);
        }

        return list;
    }

    /**
     * Returns {@code true} if the given component class is a provider (implements specific interfaces).
     * See {@link #getProviderContracts}.
     *
     * @param clazz class to test.
     * @return {@code true} if the class is provider, {@code false} otherwise.
     */
    public static boolean isProvider(final Class<?> clazz) {
        return findFirstProviderContract(clazz);
    }

    /**
     * Returns {@code true} if given component class is a JAX-RS provider.
     *
     * @param clazz class to check.
     * @return {@code true} if the class is a JAX-RS provider, {@code false} otherwise.
     */
    public static boolean isJaxRsProvider(final Class<?> clazz) {
        for (final Class<?> providerType : JAX_RS_PROVIDER_INTERFACE_WHITELIST.keySet()) {
            if (providerType.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ensure the supplied implementation classes implement the expected contract.
     *
     * @param contract        contract that is expected to be implemented by the implementation classes.
     * @param implementations contract implementations.
     * @throws java.lang.IllegalArgumentException in case any of the implementation classes does not
     *                                            implement the expected contract.
     */
    public static void ensureContract(final Class<?> contract, final Class<?>... implementations) {
        if (implementations == null || implementations.length <= 0) {
            return;
        }

        final StringBuilder invalidClassNames = new StringBuilder();
        for (final Class<?> impl : implementations) {
            if (!contract.isAssignableFrom(impl)) {
                if (invalidClassNames.length() > 0) {
                    invalidClassNames.append(", ");
                }
                invalidClassNames.append(impl.getName());
            }
        }

        if (invalidClassNames.length() > 0) {
            throw new IllegalArgumentException(LocalizationMessages.INVALID_SPI_CLASSES(
                    contract.getName(),
                    invalidClassNames.toString()));
        }

    }

    /**
     * Inject {@code providerInstances}. The method iterates through {@code providerInstances}
     * and initializes injectable fields of each instance using {@code serviceLocator}.
     *
     * @param providerInstances Iterable of provider instances to be injected.
     * @param serviceLocator    Service locator.
     */
    public static <T> void injectProviders(final Iterable<T> providerInstances, final ServiceLocator serviceLocator) {
        for (final T providerInstance : providerInstances) {
            serviceLocator.inject(providerInstance);
        }

    }

    private static boolean findFirstProviderContract(final Class<?> clazz) {
        for (final Class<?> contract : getImplementedContracts(clazz)) {
            if (isSupportedContract(contract)) {
                return true;
            }
            if (findFirstProviderContract(contract)) {
                return true;
            }
        }
        return false;
    }
}
