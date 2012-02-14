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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Sets;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.glassfish.hk2.Services;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.collect.Lists;

/**
 * Combines access to custom provider classes and instances
 * and providers registered via a HK2 module or Java Service Provider mechanism
 * ({@code META-INF/services}).
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ServiceProviders {

    /**
     * Service providers builder.
     */
    public static final class Builder {

        private final Services services;
        private Set<Class<?>> classes;
        private Set<Object> instances;

        /**
         * Injection constructor.
         *
         * @param services HK2 services to be used for service providers lookup.
         */
        public Builder(@Inject Services services) {
            this.services = services;
        }

        /**
         * Set custom service provider classes that will take precedence over the default
         * service providers provided by the underlying HK2 services.
         *
         * @param classes custom service classes.
         * @return the updated builder instance.
         */
        public Builder setProviderClasses(Set<Class<?>> classes) {
            this.classes = classes;
            return this;
        }

        /**
         * Set custom service provider instances that will take precedence over the default
         * service providers provided by the underlying HK2 services.
         *
         * @param instances custom service provider instances.
         * @return the updated builder instance.
         */
        public Builder setProviderInstances(Set<Object> instances) {
            this.instances = instances;
            return this;
        }

        public ServiceProviders build() {
            return new ServiceProviders(services, classes, instances);
        }
    }
    //
    private static final Logger LOGGER = Logger.getLogger(ServiceProviders.class.getName());
    //
    private final Services services;
    private final Set<Class<?>> providerClasses;
    private final Set<Object> providerInstances;

    private ServiceProviders(final Services services, final Set<Class<?>> classes, final Set<Object> instances) {
        this.services = services;
        this.providerClasses = Collections.unmodifiableSet(Sets.newHashSet(classes == null ? Collections.<Class<?>>emptySet() : classes));
        this.providerInstances = Collections.unmodifiableSet(Sets.newHashSet(instances == null ? Collections.<Object>emptySet() : instances));
    }

    /**
     * Get all provider instances of the requested provider type from the list of internally
     * registered provider instances and classes. This method does not perform
     * service provider lookup in {@code META-INF/services}.
     *
     * @param <T> provider Java type.
     * @param providerContract provider contract class.
     * @return instances of all internally registered providers of the given
     *     provider type.
     */
    public <T> Set<T> getCustom(Class<T> providerContract) {
        Set<T> result = new LinkedHashSet<T>(getCustomInstances(providerContract));

        for (Class<?> pc : providerClasses) {
            if (providerContract.isAssignableFrom(pc)) {
                Object o = getComponent(pc);
                if (o != null) {
                    result.add(providerContract.cast(o));
                }
            }
        }

        return result;
    }

    /**
     * Get all provider instances of the requested provider type from the list of internally
     * registered provider instances and classes. This method does not perform
     * service provider lookup in {@code META-INF/services}.
     * <p/>
     * The returned provider list, if not empty, is sorted using the supplied
     * {@link comparator} before it is returned.
     *
     * @param <T> provider Java type.
     * @param providerContract provider contract class.
     * @param comparator contract comparator used for ordering provider instances
     *     in the set.
     * @return sorted list of instances of all internally registered providers of
     *     the given provider type.
     */
    public <T> List<T> getCustom(Class<T> providerContract, final Comparator<T> comparator) {
        final List<T> providers = Lists.newArrayList(getDefault(providerContract));
        Collections.sort(providers, comparator);
        return providers;
    }

    /**
     * Get all provider instances of the requested provider type found by performing
     * service provider lookup in {@code META-INF/services}. This method ignores any
     * internally registered provider instances or classes.
     *
     * @param <T> provider Java type.
     * @param providerContract provider contract class.
     * @return instances of all providers of the given provider type found
     *     during the service provider lookup.
     */
    public <T> Set<T> getDefault(Class<T> providerContract) {
        return Providers.getProviders(services, providerContract);
    }

    /**
     * Get all provider instances of the requested provider type found by performing
     * service provider lookup in {@code META-INF/services}. This method ignores any
     * internally registered provider instances or classes.
     * <p/>
     * The returned provider list, if not empty, is sorted using the supplied
     * {@link comparator} before it is returned.
     *
     * @param <T> provider Java type.
     * @param providerContract provider contract class.
     * @param comparator contract comparator used for ordering provider instances
     *     in the set.
     * @return sorted list of instances of all providers of the given provider type
     *     found during the service provider lookup.
     */
    public <T> List<T> getDefault(Class<T> providerContract, final Comparator<T> comparator) {
        final List<T> providers = Lists.newArrayList(getDefault(providerContract));
        Collections.sort(providers, comparator);
        return providers;
    }

    /**
     * Get all provider instances of the requested provider type found both
     * in the internal storage as well as by performing service provider lookup
     * in {@code META-INF/services}. This method returns a result that is a
     * combination of the results returned by {@link #findProviders(java.lang.Class)}
     * and {@link #getDefault(java.lang.Class)}.
     *
     * @param <T> provider Java type.
     * @param providerContract provider contract class.
     * @return instances of all providers of the given provider type found both
     *     in the internal storage as well as during the service provider lookup.
     */
    public <T> Set<T> getAll(Class<T> providerContract) {
        Set<T> result = new LinkedHashSet<T>(getCustom(providerContract));
        result.addAll(getDefault(providerContract));
        return result;
    }

    /**
     * Get all provider instances of the requested provider type found both
     * in the internal storage as well as by performing service provider lookup
     * in {@code META-INF/services}. This method returns a result that is a
     * combination of the results returned by {@link #findProviders(java.lang.Class)}
     * and {@link #getDefault(java.lang.Class)}.
     * <p/>
     * The returned provider list, if not empty, is sorted using the supplied
     * {@link comparator} before it is returned.
     *
     * @param <T> provider Java type.
     * @param providerContract provider contract class.
     * @param comparator contract comparator used for ordering provider instances
     *     in the set.
     * @return sorted list of instances of all providers of the given provider type
     *     found both in the internal storage as well as during the service provider
     *     lookup.
     */
    public <T> List<T> getAll(Class<T> providerContract, final Comparator<T> comparator) {
        final List<T> providers = Lists.newArrayList(getAll(providerContract));
        Collections.sort(providers, comparator);
        return providers;
    }

    /**
     * Asynchronous callback interface for handling all the service providers of
     * a given contract type.
     *
     * @param <T>
     */
    public static interface ServiceListener<T> {

        /**
         * Invoked whenever a new provider instance of the given type has been found.
         *
         * @param service most recently found provider instance.
         */
        public void onAdd(T service);
    }

    /**
     * Get all provider instances of the requested provider type from the list of
     * internally registered provider instances and classes. This method does not
     * perform service provider lookup in {@code META-INF/services}.
     * <p />
     * Rather than returning the list of found provider instances, the method
     * {@link ServiceListener#onAdd(java.lang.Object) invokes} the supplied provider
     * listener for every provider instance that matches the requested provider
     * type.
     *
     * @param <T> provider Java type.
     * @param providerContract provider contract class.
     * @param listener provider listener invoked with every matched provider instance.
     */
    public <T> void getCustom(Class<T> providerContract, ServiceListener<T> listener) {
        for (T t : getCustom(providerContract)) {
            listener.onAdd(t);
        }
    }

    /**
     * Get all provider instances of the requested provider type found both
     * in the internal storage as well as by performing service provider lookup
     * in {@code META-INF/services}.
     * <p />
     * Rather than returning the list of found provider instances, the method
     * {@link ServiceListener#onAdd(java.lang.Object) invokes} the supplied provider
     * listener for every provider instance that matches the requested provider
     * type.
     *
     * @param <T> provider Java type.
     * @param providerContract provider contract class.
     * @param listener provider listener invoked with every matched provider instance.
     */
    public <T> void getAll(Class<T> providerContract, ServiceListener<T> listener) {
        for (T t : getCustom(providerContract)) {
            listener.onAdd(t);
        }

        for (T t : getDefault(providerContract)) {
            listener.onAdd(t);
        }
    }

    /**
     * Instantiate providers of the given type using the array of provider implementation
     * class names.
     * <p />
     * Note that this method does not fail in case any of the implementation classes
     * cannot be found or instantiated or if it does not match the requested provider
     * type. Instead, a {@link Level#SEVERE severe} log entry is recorded and the
     * failing implementation class is ignored and not included in the returned
     * list of provider instances.
     *
     * @param <T> requested provider Java type.
     * @param providerContract provider contract class.
     * @param classNames provider implementation class names.
     * @return provider instances instantiated from the supplied provider implementation
     *     class names.
     */
    public <T> List<T> instantiate(Class<T> providerContract, String[] classNames) {
        List<T> ps = new LinkedList<T>();
        for (String className : classNames) {
            try {
                Class<?> c = ReflectionHelper.classForNameWithException(className);
                if (providerContract.isAssignableFrom(c)) {
                    Object o = getComponent(c);
                    if (o != null) {
                        ps.add(providerContract.cast(o));
                    }
                } else {
                    LOGGER.log(Level.SEVERE,
                            "The class {0} is not assignable to the class {1}. This class is ignored.",
                            new Object[]{className, providerContract.getName()});
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "The class {0}"
                        + " could not be found"
                        + ". This class is ignored.", className);
            }
        }

        return ps;
    }

    /**
     * Instantiate providers of the given type using the array of provider implementation
     * classes.
     * <p />
     * Note that this method does not fail in case any of the implementation classes
     * cannot be instantiated or if it does not match the requested provider type.
     * Instead, a {@link Level#SEVERE severe} log entry is recorded and the failing
     * implementation class is ignored and not included in the returned list of
     * provider instances.
     *
     * @param <T> requested provider Java type.
     * @param providerContract provider contract class.
     * @param classes provider implementation classes.
     * @return provider instances instantiated from the supplied provider implementation
     *     class names.
     */
    public <T> List<T> instantiate(Class<T> providerContract, Class<? extends T>[] classes) {
        List<T> ps = new LinkedList<T>();
        for (Class<? extends T> c : classes) {
            Object o = getComponent(c);
            if (o != null) {
                ps.add(providerContract.cast(o));
            }
        }

        return ps;
    }

    private Object getComponent(Class<?> provider) {
        try {
            return services.byType(provider).get();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Component instantiation failed.", ex);
            return null;
        }
    }

    private <T> Set<T> getCustomInstances(final Class<T> contract) {
        Set<T> sp = new LinkedHashSet<T>();
        for (Object p : providerInstances) {
            if (contract.isInstance(p)) {
                sp.add(contract.cast(p));
            }
        }

        return sp;
    }
}