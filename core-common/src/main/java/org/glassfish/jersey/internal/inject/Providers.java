/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.internal.LocalizationMessages;

import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.spi.Contract;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Utility class providing a set of utility methods for easier and more type-safe
 * interaction with HK2 injection layer.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class Providers {

    private static final Logger LOGGER = Logger.getLogger(Providers.class.getName());


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
            public void dispose(T instance) {
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
    public static <T> Set<T> getProviders(ServiceLocator locator, Class<T> contract) {
        final Collection<ServiceHandle<T>> hk2Providers = getAllServiceHandles(locator, contract);
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
    public static <T> Set<T> getCustomProviders(ServiceLocator locator, Class<T> contract) {
        final Collection<ServiceHandle<T>> hk2Providers = getAllServiceHandles(locator, contract, new CustomAnnotationImpl());
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
    public static <T> Iterable<T> getAllProviders(ServiceLocator locator, Class<T> contract) {
        return getAllProviders(locator, contract, (Comparator<T>) null);
    }

    /**
     * Get the iterable of all {@link RankedProvider providers} (custom and default) registered for the given service provider
     * contract in the underlying {@link ServiceLocator HK2 service locator} container.
     *
     * @param <T> service provider contract Java type.
     * @param locator underlying HK2 service locator.
     * @param contract service provider contract.
     * @return iterable of all available ranked service providers for the contract. Return value is never null.
     */
    public static <T> Iterable<RankedProvider<T>> getAllRankedProviders(ServiceLocator locator, Class<T> contract) {
        List<ServiceHandle<T>> providers = getAllServiceHandles(locator, contract, new CustomAnnotationImpl());
        providers.addAll(getAllServiceHandles(locator, contract));

        LinkedHashMap<ActiveDescriptor<T>, RankedProvider<T>> providerMap = Maps.newLinkedHashMap();

        for (ServiceHandle<T> provider : providers) {
            ActiveDescriptor<T> key = provider.getActiveDescriptor();
            if (!providerMap.containsKey(key)) {
                providerMap.put(key, new RankedProvider<T>(provider.getService(), key.getRanking()));
            }
        }

        return providerMap.values();
    }

    /**
     * Sorts given providers with {@link RankedComparator ranked comparator}.
     *
     * @param comparator comparator to sort the providers with.
     * @param providerIterables providers to be sorted.
     * @param <T> service provider contract Java type.
     * @return sorted {@link Iterable iterable} instance containing given providers. Return value is never null.
     */
    public static <T> Iterable<T> sortRankedProviders(final RankedComparator<T> comparator,
                                                      final Iterable<RankedProvider<T>>... providerIterables) {
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
     * @param <T> service provider contract Java type.
     * @param locator underlying HK2 service locator.
     * @param contract service provider contract.
     * @param comparator comparator to sort the providers with.
     * @return set of all available ranked service providers for the contract. Return value is never null.
     */
    public static <T> Iterable<T> getAllProviders(final ServiceLocator locator,
                                                  final Class<T> contract,
                                                  final RankedComparator<T> comparator) {
        //noinspection unchecked
        return sortRankedProviders(comparator, getAllRankedProviders(locator, contract));
    }

    private static <T> List<ServiceHandle<T>> getAllServiceHandles(ServiceLocator locator, Class<T> contract,
                                                                   Annotation... qualifiers) {

        List<ServiceHandle<T>> allServiceHandles = qualifiers == null ?
                locator.getAllServiceHandles(contract) :
                locator.getAllServiceHandles(contract, qualifiers);

        ArrayList<ServiceHandle<T>> serviceHandles = new ArrayList<ServiceHandle<T>>();
        for (ServiceHandle handle : allServiceHandles) {
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
    public static <T> Iterable<T> getAllProviders(ServiceLocator locator, Class<T> contract, Comparator<T> comparator) {
        List<ServiceHandle<T>> providers = getAllServiceHandles(locator, contract, new CustomAnnotationImpl());
        providers.addAll(getAllServiceHandles(locator, contract));

        LinkedHashMap<ActiveDescriptor, ServiceHandle<T>> providerMap = Maps.newLinkedHashMap();

        for (ServiceHandle<T> provider : providers) {
            ActiveDescriptor key = provider.getActiveDescriptor();
            if (!providerMap.containsKey(key)) {
                providerMap.put(key, provider);
            }
        }

        final ArrayList<T> providerList = new ArrayList<T>(getClasses(providerMap.values()));

        if (comparator != null) {
            Collections.sort(providerList, comparator);
        }

        return providerList;
    }

    private static <T> Set<T> getClasses(Collection<ServiceHandle<T>> hk2Providers) {
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
    public static <T> SortedSet<T> getProviders(ServiceLocator locator, Class<T> contract, final Comparator<T> comparator) {
        final Collection<ServiceHandle<T>> hk2Providers = getAllServiceHandles(locator, contract);
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
    public static Set<Class<?>> getProviderContracts(Class<?> clazz) {
        Set<Class<?>> contracts = new HashSet<Class<?>>();
        computeProviderContracts(clazz, contracts);
        return contracts;
    }

    private static void computeProviderContracts(Class<?> clazz, Set<Class<?>> contracts) {
        for (Class<?> contract : getImplementedContracts(clazz)) {
            if (isProviderContract(contract)) {
                contracts.add(contract);
            }
            computeProviderContracts(contract, contracts);
        }
    }

    /**
     * Filter provider instances removing all providers not targeted to the {@code currentRuntime}.
     * The provider applicability to a runtime is determined based on the {@link ConstrainedTo &#64;ConstraintTo}
     * restriction placed on the contract interface as well as any {@code @ConstrainedTo} restrictions
     * placed on the contract implementation class.
     * <p>
     * A warning is logged for all providers that have been filtered out.
     * </p>
     *
     * @param providerInstances providers to be filtered.
     * @param currentRuntime    current runtime.
     * @param registeredClasses classes which were explicitly registered and not found by the classpath scanning
     *                          or any other kind of class scanning.
     * @return set of provider instances.
     */
    public static Set<Object> filterInstancesByConstraint(Set<Object> providerInstances, ConstrainedTo.Type currentRuntime,
                                                          Set<Class<?>> registeredClasses) {
        Set<Object> filteredInstances = new HashSet<Object>();
        for (Object provider : providerInstances) {
            final boolean valid = checkProviderRuntime(provider.getClass(), currentRuntime,
                    registeredClasses == null || !registeredClasses.contains(provider.getClass()), false);
            if (valid) {
                filteredInstances.add(provider);
            }
        }

        return filteredInstances;
    }

    /**
     * Filter provider instances removing all providers not targeted to the {@code currentRuntime}.
     * The provider applicability to a runtime is determined based on the {@link ConstrainedTo &#64;ConstraintTo}
     * restriction placed on the contract interface as well as any {@code @ConstrainedTo} restrictions
     * placed on the contract implementation class.
     * <p>
     * A warning is logged for all providers that have been filtered out.
     * </p>
     *
     * @param providers         providers to be filtered.
     * @param currentRuntime    current runtime.
     * @param registeredClasses classes which were explicitly registered and not found by the classpath scanning or any other
     *                          kind of class scanning.
     * @return set of provider classes.
     */
    public static Set<Class<?>> filterByConstraint(Set<Class<?>> providers, ConstrainedTo.Type currentRuntime,
                                                   Set<Class<?>> registeredClasses) {
        Set<Class<?>> filteredProviders = new HashSet<Class<?>>();
        for (Class<?> provider : providers) {
            final boolean valid = checkProviderRuntime(provider, currentRuntime, registeredClasses == null ||
                    !registeredClasses.contains(provider), false);
            if (valid) {
                filteredProviders.add(provider);
            }
        }
        return filteredProviders;
    }

    /**
     * Check the {@code provider} whether it is appropriate correctly configured for client or server
     * {@link ConstrainedTo.Type runtime}.
     *
     * If a problem occurs a warning is logged and if the provider is not usable at all in the current runtime
     * {@code false} is returned. For classes found during provider scanning (scanned=true) certain warnings are completely
     * ignored (e.g. providers {@link ConstrainedTo constrained to} the client runtime and found by server-side class path
     * scanning will be silently ignored and no warning will be logged).
     *
     * @param provider          the class of the provider being checked.
     * @param runtimeConstraint current runtime (client or server).
     * @param scanned           {@code false} if the class was explicitly registered; {@code true} if the class has been
     *                                       discovered
     *                          during any form of provider scanning.
     * @param isResource        {@code true} if the provider is also a resource class.
     * @return {@code true} if provider is fine and can be used {@code false} otherwise.
     */
    public static boolean checkProviderRuntime(Class<?> provider, ConstrainedTo.Type runtimeConstraint,
                                               boolean scanned, boolean isResource) {
        final Set<Class<?>> providerContracts = Providers.getProviderContracts(provider);
        final ConstrainedTo constrainedAnnotation = provider.getAnnotation(ConstrainedTo.class);
        ConstrainedTo.Type providerConstraint = constrainedAnnotation == null ? null : constrainedAnnotation.value();
        if (Feature.class.isAssignableFrom(provider)) {
            // TODO: solve after implementation
            return true;
        }

        final StringBuilder warnings = new StringBuilder();
        try {
            /**
             * Indicates that the provider implements at least one contract compatible
             * with it's implementation class constraint.
             */
            boolean foundProviderCompatible = providerConstraint == null;
            boolean foundRuntimeCompatibleContract = false;
            for (Class<?> providerContract : providerContracts) {
                // if the contract is common/not constrained, default to provider constraint
                final ConstrainedTo.Type contractConstraint = getContractConstraint(providerContract, providerConstraint);
                foundRuntimeCompatibleContract |= contractConstraint == null || contractConstraint == runtimeConstraint;

                if (providerConstraint != null) {
                    if (contractConstraint != providerConstraint) {
                        warnings.append(LocalizationMessages.WARNING_PROVIDER_CONSTRAINED_TO_WRONG_PACKAGE(
                                provider.getName(),
                                providerConstraint.name(),
                                providerContract.getName(),
                                contractConstraint.name()))
                                .append(" ");
                    } else {
                        foundProviderCompatible = true;
                    }
                }
            }

            if (!foundProviderCompatible) {
                warnings.append(LocalizationMessages.ERROR_PROVIDER_CONSTRAINED_TO_WRONG_PACKAGE(
                        provider.getName(),
                        providerConstraint.name()));
                logProviderSkipped(warnings, provider, isResource);
                return false;
            }

            boolean isProviderRuntimeCompatible;
            // runtimeConstraint vs. providerConstraint
            isProviderRuntimeCompatible = providerConstraint == null || providerConstraint == runtimeConstraint;
            if (!isProviderRuntimeCompatible && !scanned) {
                // log failure for manually registered providers
                warnings.append(LocalizationMessages.ERROR_PROVIDER_CONSTRAINED_TO_WRONG_RUNTIME(
                        provider.getName(),
                        providerConstraint.name(),
                        runtimeConstraint.name()));

                logProviderSkipped(warnings, provider, isResource);
            }

            // runtimeConstraint vs contractConstraint
            if (!foundRuntimeCompatibleContract && !scanned) {
                warnings.append(LocalizationMessages.ERROR_PROVIDER_REGISTERED_WRONG_RUNTIME(provider.getName(),
                        runtimeConstraint.name(), runtimeConstraint == ConstrainedTo.Type.SERVER ? "client" : "server"));
                logProviderSkipped(warnings, provider, isResource);
                return false;
            }

            return isProviderRuntimeCompatible && foundRuntimeCompatibleContract;
        } finally {
            if (warnings.length() > 0) {
                LOGGER.log(Level.WARNING, warnings.toString());
            }
        }
    }

    private static void logProviderSkipped(StringBuilder sb, Class<?> provider, boolean alsoResourceClass) {
        sb.append(alsoResourceClass ? LocalizationMessages.ERROR_PROVIDER_AND_RESOURCE_CONSTRAINED_TO_IGNORED
                (provider.getName()) : LocalizationMessages.ERROR_PROVIDER_CONSTRAINED_TO_IGNORED(provider.getName()));
    }

    private static boolean isProviderContract(Class clazz) {
        return (JAX_RS_PROVIDER_INTERFACE_WHITELIST.get(clazz) != null || clazz.isAnnotationPresent(Contract.class));
    }


    private static ConstrainedTo.Type getContractConstraint(Class<?> clazz, ConstrainedTo.Type defaultConstraint) {
        final ProviderRuntime jaxRsProvider = JAX_RS_PROVIDER_INTERFACE_WHITELIST.get(clazz);

        ConstrainedTo.Type result = null;
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

    private static final class ProviderRuntime {
        private final ConstrainedTo.Type runtime;

        private ProviderRuntime(ConstrainedTo.Type runtime) {
            this.runtime = runtime;
        }

        public ConstrainedTo.Type getRuntime() {
            return runtime;
        }
    }

    private static final Map<Class<?>, ProviderRuntime> JAX_RS_PROVIDER_INTERFACE_WHITELIST =
            getJaxRsProviderInterfaces();

    private static Map<Class<?>, ProviderRuntime> getJaxRsProviderInterfaces() {
        Map<Class<?>, ProviderRuntime> interfaces = new HashMap<Class<?>, ProviderRuntime>();

        interfaces.put(javax.ws.rs.ext.ContextResolver.class, new ProviderRuntime(null));
        interfaces.put(javax.ws.rs.ext.ExceptionMapper.class, new ProviderRuntime(null));
        interfaces.put(javax.ws.rs.ext.MessageBodyReader.class, new ProviderRuntime(null));
        interfaces.put(javax.ws.rs.ext.MessageBodyWriter.class, new ProviderRuntime(null));
        interfaces.put(javax.ws.rs.ext.ReaderInterceptor.class, new ProviderRuntime(null));
        interfaces.put(javax.ws.rs.ext.WriterInterceptor.class, new ProviderRuntime(null));

        interfaces.put(javax.ws.rs.core.Feature.class, new ProviderRuntime(null));

        interfaces.put(javax.ws.rs.container.ContainerRequestFilter.class, new ProviderRuntime(ConstrainedTo.Type.SERVER));
        interfaces.put(javax.ws.rs.container.ContainerResponseFilter.class, new ProviderRuntime(ConstrainedTo.Type.SERVER));
        interfaces.put(javax.ws.rs.client.ClientResponseFilter.class, new ProviderRuntime(ConstrainedTo.Type.CLIENT));
        interfaces.put(javax.ws.rs.client.ClientRequestFilter.class, new ProviderRuntime(ConstrainedTo.Type.CLIENT));

        interfaces.put(javax.ws.rs.ext.ParamConverterProvider.class, new ProviderRuntime(null));

        interfaces.put(javax.ws.rs.container.DynamicFeature.class, new ProviderRuntime(ConstrainedTo.Type.SERVER));


        return interfaces;
    }


    private static List<Class<?>> getImplementedContracts(Class<?> clazz) {
        List<Class<?>> list = new LinkedList<Class<?>>();

        Collections.addAll(list, clazz.getInterfaces());

        final Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            list.add(superclass);
        }

        return list;
    }

    /**
     * Returns {@code true} if the given class is a provider (implements specific interfaces).
     * See {@link #getProviderContracts}.
     *
     * @param clazz class to test.
     * @return {@code true} if the class is provider, {@code false} otherwise.
     */
    public static boolean isProvider(Class<?> clazz) {
        return findFirstProviderContract(clazz);
    }

    private static boolean findFirstProviderContract(Class<?> clazz) {
        for (Class<?> contract : getImplementedContracts(clazz)) {
            if (isProviderContract(contract)) {
                return true;
            }
            if (findFirstProviderContract(contract)) {
                return true;
            }
        }
        return false;
    }
}
