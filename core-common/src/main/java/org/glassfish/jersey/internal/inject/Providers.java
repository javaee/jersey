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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.container.DynamicBinder;

import org.glassfish.jersey.spi.Contract;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Provider;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;

import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

/**
 * Utility class providing a set of utility methods for easier and more type-safe
 * interaction with HK2 injection layer.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class Providers {

    private Providers() {
    }

    /**
     * Wrap HK2 service provider into a HK2 service factory.
     *
     * @param <T> Java type if the contract produced by the provider and factory.
     * @param provider HK2 service provider to be wrapped.
     * @return HK2 service factory wrapping the HK2 service provider.
     */
    public static <T> Factory<T> asFactory(final Provider<T> provider) {
        return new Factory<T>() {

            @Override
            public T get() {
                return provider.get();
            }
        };
    }

    /**
     * Wrap an instance into a HK2 service factory.
     *
     * @param <T> Java type if the contract produced by the provider and factory.
     * @param instance instance to be wrapped into (and provided by) the factory.
     * @return HK2 service factory wrapping and providing the instance.
     */
    public static <T> Factory<T> factoryOf(final T instance) {
        return new Factory<T>() {

            @Override
            public T get() {
                return instance;
            }
        };
    }

    private static void exploreType(Type type, StringBuilder builder) {
        if (type instanceof ParameterizedType) {
            builder.append(TypeLiteral.getRawType(type).getName());

            // we ignore wildcard types.
            Collection<Type> types = Arrays.asList(((ParameterizedType) type).getActualTypeArguments());
            Iterator<Type> typesEnum = types.iterator();
            List<Type> nonWildcards = new ArrayList<Type>();
            while (typesEnum.hasNext()) {
                Type genericType = typesEnum.next();
                if (!(genericType instanceof WildcardType)) {
                    nonWildcards.add(genericType);
                }
            }
            if (!nonWildcards.isEmpty()) {
                builder.append("<");
                Iterator<Type> typesItr = nonWildcards.iterator();
                while (typesItr.hasNext()) {
                    exploreType(typesItr.next(), builder);
                    if (typesItr.hasNext()) {
                        builder.append(",");
                    }
                }
                builder.append(">");
            }
        } else {
            builder.append(TypeLiteral.getRawType(type).getName());
        }
    }

    private static String exploreType(Type type) {
        StringBuilder builder = new StringBuilder();
        exploreType(type, builder);
        return builder.toString();
    }

    /**
     * Build a string definition for a generic contract.
     *
     * @param rawType raw generic type.
     * @param types actual generic type arguments.
     * @return string definition for a generic contract.
     */
    public static String contractStringFor(Class<?> rawType, Type... types) {
        StringBuilder builder = new StringBuilder(rawType.getName());

        if (types != null && types.length > 0) {
            builder.append('<').append(exploreType(types[0]));
            for (int i = 1; i < types.length; i++) {
                builder.append(',').append(exploreType(types[i]));
            }
            builder.append('>');
        }

        return builder.toString();
    }

    /**
     * Get a typed service instance for a contract specified as a string.
     *
     * @param <T> expected contract type.
     * @param services HK2 services.
     * @param contract string definition of the contract Java type.
     * @return typed service instance for the contract.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getContract(Services services, String contract) {
        return (T) services.forContract(contract).all();//get();
    }

    /**
     * Get a typed HK2 service provider for a contract specified as string.
     *
     * @param <T> expected contract type.
     * @param services HK2 services.
     * @param contract string definition of the contract Java type.
     * @return typed HK2 service provider for the contract.
     */
    @SuppressWarnings("unchecked")
    public static <T> Provider<T> getProvider(Services services, String contract) {
        return (Provider<T>) services.forContract(contract).getProvider();
    }

    /**
     * Get a typed HK2 service factory for a contract specified as string.
     *
     * @param <T> expected contract type.
     * @param services HK2 services.
     * @param contract string definition of the contract Java type.
     * @return typed HK2 service factory for the contract.
     */
    @SuppressWarnings("unchecked")
    public static <T> Factory<T> getFactory(Services services, String contract) {
        return (Factory<T>) asFactory(services.forContract(contract).getProvider());
    }

    /**
     * Get the set of default providers registered for the given service provider contract
     * in the underlying {@link Services HK2 services} container.
     *
     * @param <T> service provider contract Java type.
     * @param services underlying HK2 services container.
     * @param contract service provider contract.
     * @return set of all available default service provider instances for the contract.
     */
    public static <T> Set<T> getProviders(Services services, Class<T> contract) {
        final Collection<Provider<T>> hk2Providers = services.forContract(contract).all();
        return getClasses(hk2Providers);
    }


    /**
     * Get the set of all custom providers registered for the given service provider contract
     * in the underlying {@link Services HK2 services} container.
     *
     * @param <T> service provider contract Java type.
     * @param services underlying HK2 services container.
     * @param contract service provider contract.
     * @return set of all available service provider instances for the contract.
     */
    public static <T> Set<T> getCustomProviders(Services services, Class<T> contract) {
        final Collection<Provider<T>> hk2Providers = services.forContract(contract).annotatedWith(Custom.class).all();
        return getClasses(hk2Providers);
    }

    /**
     * Get the set of all providers (custom and default) registered for the given service provider contract
     * in the underlying {@link Services HK2 services} container.
     *
     * @param <T> service provider contract Java type.
     * @param services underlying HK2 services container.
     * @param contract service provider contract.
     * @return set of all available service provider instances for the contract.
     */
    public static <T> List<T> getAllProviders(Services services, Class<T> contract) {
        List<T> providers = new ArrayList<T>(getClasses(services.forContract(contract).annotatedWith(Custom.class).all()));
        providers.addAll(getClasses(services.forContract(contract).all()));
        return providers;
    }

    /**
     * Get the set of all providers (custom and default) registered for the given service provider contract
     * in the underlying {@link Services HK2 services} container ordered based on the given {@code comparator}.
     *
     * @param <T> service provider contract Java type.
     * @param services underlying HK2 services container.
     * @param contract service provider contract.
     * @return set of all available service provider instances for the contract ordered using the given
     * {@link Comparator comparator}.
     */
    public static <T> List<T> getAllProviders(Services services, Class<T> contract, Comparator<T> comparator) {
        List<T> providers = new ArrayList<T>(getClasses(services.forContract(contract).annotatedWith(Custom.class).all()));
        providers.addAll(getClasses(services.forContract(contract).all()));
        Collections.sort(providers, comparator);
        return providers;
    }


    private static <T> Set<T> getClasses(Collection<Provider<T>> hk2Providers) {
        if (hk2Providers.isEmpty()) {
            return Sets.newLinkedHashSet();
        } else {
            return Sets.newLinkedHashSet(Collections2.transform(hk2Providers, new ProviderToService<T>()));
        }
    }


    /**
     * Get the set of all providers registered for the given service provider contract
     * in the underlying {@link Services HK2 services} container.
     *
     * @param <T> service provider contract Java type.
     * @param services underlying HK2 services container.
     * @param contract service provider contract.
     * @param comparator contract comparator used for ordering contracts in the
     *     set.
     * @return set of all available service provider instances for the contract.
     */
    public static <T> SortedSet<T> getProviders(Services services, Class<T> contract, final Comparator<T> comparator) {
        final Collection<Provider<T>> hk2Providers = services.forContract(contract).all();
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


    private static boolean isProviderContract(Class clazz) {
        return (JAX_RS_PROVIDER_INTERFACE_WHITELIST.contains(clazz) || clazz.isAnnotationPresent(Contract.class));
    }

    private static final Set<Class<?>> JAX_RS_PROVIDER_INTERFACE_WHITELIST = getJaxRsProviderInterfaces();

    private static Set<Class<?>> getJaxRsProviderInterfaces() {
        Set<Class<?>> interfaces = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());
        interfaces.add(javax.ws.rs.ext.ContextResolver.class);
        interfaces.add(javax.ws.rs.ext.ExceptionMapper.class);
        interfaces.add(javax.ws.rs.ext.MessageBodyReader.class);
        interfaces.add(javax.ws.rs.ext.MessageBodyWriter.class);
        interfaces.add(javax.ws.rs.ext.Providers.class);
        interfaces.add(javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate.class);
        interfaces.add(javax.ws.rs.ext.ReaderInterceptor.class);
        interfaces.add(javax.ws.rs.ext.WriterInterceptor.class);

        interfaces.add(javax.ws.rs.container.ContainerRequestFilter.class);
        interfaces.add(javax.ws.rs.container.ContainerResponseFilter.class);
        interfaces.add(javax.ws.rs.client.ClientResponseFilter.class);
        interfaces.add(javax.ws.rs.client.ClientRequestFilter.class);

        interfaces.add(DynamicBinder.class);

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
        return !getProviderContracts(clazz).isEmpty();
    }
}
