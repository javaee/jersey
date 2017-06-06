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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Feature;

import javax.annotation.Priority;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.spi.Contract;

/**
 * Utility class providing a set of utility methods for easier and more type-safe
 * interaction with an injection layer.
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
    /**
     * Map of all supported external (i.e. non-Jersey) contracts and their run-time affinity.
     */
    private static final Map<Class<?>, ProviderRuntime> EXTERNAL_PROVIDER_INTERFACE_WHITELIST =
            getExternalProviderInterfaces();

    private Providers() {
    }

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

    private static Map<Class<?>, ProviderRuntime> getExternalProviderInterfaces() {
        final Map<Class<?>, ProviderRuntime> interfaces = new HashMap<Class<?>, ProviderRuntime>();

        // JAX-RS
        interfaces.putAll(JAX_RS_PROVIDER_INTERFACE_WHITELIST);
        interfaces.put(javax.ws.rs.core.Feature.class, ProviderRuntime.BOTH);
        interfaces.put(Binder.class, ProviderRuntime.BOTH);
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

    /**
     * Get the set of default providers registered for the given service provider contract
     * in the underlying {@link InjectionManager injection manager} container.
     *
     * @param <T>             service provider contract Java type.
     * @param injectionManager underlying injection manager.
     * @param contract        service provider contract.
     * @return set of all available default service provider instances for the contract.
     */
    public static <T> Set<T> getProviders(InjectionManager injectionManager, Class<T> contract) {
        Collection<ServiceHolder<T>> providers = getServiceHolders(injectionManager, contract);
        return getProviderClasses(providers);
    }

    /**
     * Get the set of all custom providers registered for the given service provider contract
     * in the underlying {@link InjectionManager injection manager} container.
     * <p>
     * Returned providers are sorted based on {@link Priority} (lower {@code Priority} value is higher priority,
     * see {@link Priorities}.
     *
     * @param <T>              service provider contract Java type.
     * @param injectionManager underlying injection manager.
     * @param contract         service provider contract.
     * @return set of all available service provider instances for the contract.
     */
    public static <T> Set<T> getCustomProviders(InjectionManager injectionManager, Class<T> contract) {

        List<ServiceHolder<T>> providers = getServiceHolders(injectionManager,
                                                             contract,
                                                             Comparator.comparingInt(Providers::getPriority),
                                                             CustomAnnotationLiteral.INSTANCE);

        return getProviderClasses(providers);
    }

    /**
     * Get the iterable of all providers (custom and default) registered for the given service provider contract
     * in the underlying {@link InjectionManager injection manager} container.
     *
     * @param <T>             service provider contract Java type.
     * @param injectionManager underlying injection manager.
     * @param contract        service provider contract.
     * @return iterable of all available service provider instances for the contract. Return value is never null.
     */
    public static <T> Iterable<T> getAllProviders(InjectionManager injectionManager, Class<T> contract) {
        return getAllProviders(injectionManager, contract, (Comparator<T>) null);
    }

    /**
     * Get the iterable of all {@link RankedProvider providers} (custom and default) registered for the given service provider
     * contract in the underlying {@link InjectionManager injection manager} container.
     *
     * @param <T>             service provider contract Java type.
     * @param injectionManager underlying injection manager.
     * @param contract        service provider contract.
     * @return iterable of all available ranked service providers for the contract. Return value is never {@code null}.
     */
    public static <T> Iterable<RankedProvider<T>> getAllRankedProviders(InjectionManager injectionManager, Class<T> contract) {
        List<ServiceHolder<T>> providers = getServiceHolders(injectionManager, contract, CustomAnnotationLiteral.INSTANCE);
        providers.addAll(getServiceHolders(injectionManager, contract));

        LinkedHashMap<ServiceHolder<T>, RankedProvider<T>> providerMap = new LinkedHashMap<>();

        for (ServiceHolder<T> provider : providers) {
            if (!providerMap.containsKey(provider)) {
                Set<Type> contractTypes = provider.getContractTypes();
                Class<?> implementationClass = provider.getImplementationClass();
                boolean proxyGenerated = true;
                for (Type ct : contractTypes) {
                    if (((Class<?>) ct).isAssignableFrom(implementationClass)) {
                        proxyGenerated = false;
                        break;
                    }
                }
                Set<Type> contracts = proxyGenerated ? contractTypes : null;
                providerMap.put(provider, new RankedProvider<>(provider.getInstance(), provider.getRank(), contracts));
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
     * The returned value is never {@code null}.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static <T> Iterable<T> sortRankedProviders(final RankedComparator<T> comparator,
                                                      final Iterable<RankedProvider<T>> providers) {

        return StreamSupport.stream(providers.spliterator(), false)
                .sorted(comparator)
                .map(RankedProvider::getProvider)
                .collect(Collectors.toList());
    }

    /**
     * Get the iterable of all providers (custom and default) registered for the given service provider contract in the underlying
     * {@link InjectionManager injection manager} container and automatically sorted using
     * {@link RankedComparator ranked comparator}.
     *
     * @param <T>             service provider contract Java type.
     * @param injectionManager underlying injection manager.
     * @param contract        service provider contract.
     * @return iterable of all available service providers for the contract. Return value is never {@code null}.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static <T> Iterable<T> getAllRankedSortedProviders(InjectionManager injectionManager, Class<T> contract) {
        Iterable<RankedProvider<T>> allRankedProviders = Providers.getAllRankedProviders(injectionManager, contract);
        return Providers.sortRankedProviders(new RankedComparator<>(), allRankedProviders);
    }

    /**
     * Merge and sort given providers with {@link RankedComparator ranked comparator}.
     *
     * @param comparator        comparator to sort the providers with.
     * @param providerIterables providers to be sorted.
     * @param <T>               service provider contract Java type.
     * @return merged and sorted {@link Iterable iterable} instance containing given providers.
     * The returned value is never {@code null}.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static <T> Iterable<T> mergeAndSortRankedProviders(final RankedComparator<T> comparator,
                                                              final Iterable<Iterable<RankedProvider<T>>> providerIterables) {

        return StreamSupport.stream(providerIterables.spliterator(), false)
                .flatMap(rankedProviders -> StreamSupport.stream(rankedProviders.spliterator(), false))
                .sorted(comparator)
                .map(RankedProvider::getProvider)
                .collect(Collectors.toList());
    }

    /**
     * Get the sorted iterable of all {@link RankedProvider providers} (custom and default) registered for the given service
     * provider contract in the underlying {@link InjectionManager injection manager} container.
     *
     * @param <T>             service provider contract Java type.
     * @param injectionManager underlying injection manager.
     * @param contract        service provider contract.
     * @param comparator      comparator to sort the providers with.
     * @return set of all available ranked service providers for the contract. Return value is never null.
     */
    public static <T> Iterable<T> getAllProviders(
            InjectionManager injectionManager, Class<T> contract, RankedComparator<T> comparator) {
        //noinspection unchecked
        return sortRankedProviders(comparator, getAllRankedProviders(injectionManager, contract));
    }

    /**
     * Get collection of all {@link ServiceHolder}s bound for providers (custom and default) registered for the given service
     * provider contract in the underlying {@link InjectionManager injection manager} container.
     *
     * @param <T>             service provider contract Java type.
     * @param injectionManager underlying injection manager.
     * @param contract        service provider contract.
     * @return set of all available service provider instances for the contract
     */
    public static <T> Collection<ServiceHolder<T>> getAllServiceHolders(InjectionManager injectionManager, Class<T> contract) {
        List<ServiceHolder<T>> providers = getServiceHolders(injectionManager,
                                                             contract,
                                                             Comparator.comparingInt(Providers::getPriority),
                                                             CustomAnnotationLiteral.INSTANCE);
        providers.addAll(getServiceHolders(injectionManager, contract));

        LinkedHashSet<ServiceHolder<T>> providersSet = new LinkedHashSet<>();
        for (ServiceHolder<T> provider : providers) {
            if (!providersSet.contains(provider)) {
                providersSet.add(provider);
            }
        }

        return providersSet;
    }

    private static <T> List<ServiceHolder<T>> getServiceHolders(
            InjectionManager bm, Class<T> contract, Annotation... qualifiers) {
        return bm.getAllServiceHolders(contract, qualifiers);
    }

    private static <T> List<ServiceHolder<T>> getServiceHolders(InjectionManager injectionManager,
                                                                Class<T> contract,
                                                                Comparator<Class<?>> objectComparator,
                                                                Annotation... qualifiers) {

        List<ServiceHolder<T>> serviceHolders = injectionManager.getAllServiceHolders(contract, qualifiers);
        serviceHolders.sort((o1, o2) -> objectComparator.compare(o1.getImplementationClass(), o2.getImplementationClass()));
        return serviceHolders;
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
     * Get the iterable of all providers (custom and default) registered for the given service provider contract
     * in the underlying {@link InjectionManager injection manager} container ordered based on the given {@code comparator}.
     *
     * @param <T>              service provider contract Java type.
     * @param injectionManager underlying injection manager.
     * @param contract         service provider contract.
     * @param comparator       comparator to be used for sorting the returned providers.
     * @return set of all available service provider instances for the contract ordered using the given
     * {@link Comparator comparator}.
     */
    public static <T> Iterable<T> getAllProviders(
            InjectionManager injectionManager, Class<T> contract, Comparator<T> comparator) {
        List<T> providerList = new ArrayList<>(getProviderClasses(getAllServiceHolders(injectionManager, contract)));
        if (comparator != null) {
            providerList.sort(comparator);
        }
        return providerList;
    }

    private static <T> Set<T> getProviderClasses(final Collection<ServiceHolder<T>> providers) {
        return providers.stream()
                        .map(Providers::holder2service)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <T> T holder2service(ServiceHolder<T> holder) {
        return (holder != null) ? holder.getInstance() : null;
    }

    private static int getPriority(Class<?> serviceClass) {
        Priority annotation = serviceClass.getAnnotation(Priority.class);
        if (annotation != null) {
            return annotation.value();
        }

        // default priority
        return Priorities.USER;
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
        final Set<Class<?>> contracts = Collections.newSetFromMap(new IdentityHashMap<>());
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
     * <p>
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
            /*
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
        final Collection<Class<?>> list = new LinkedList<>();

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
