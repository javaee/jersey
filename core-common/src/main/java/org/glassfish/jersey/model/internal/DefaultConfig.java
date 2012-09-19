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

package org.glassfish.jersey.model.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.ContractProvider;

/**
 * Common {@link Configurable} implementation for server and client.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class DefaultConfig implements Configurable {

    private ProviderBag providerBag;
    private FeatureBag featureBag;

    private final Map<String, Object> properties;
    private final Map<String, Object> immutablePropertiesView;

    private volatile boolean locked = false;

    /**
     * Creates a {@code DefaultConfig} instance;
     */
    public DefaultConfig() {
        this.providerBag = new ProviderBag.Builder().build();
        this.featureBag = new FeatureBag.Builder().build();

        this.properties = new HashMap<String, Object>();
        this.immutablePropertiesView = Collections.unmodifiableMap(properties);
    }

    /**
     * Copy constructor.
     *
     * @param configurable configurable to copy class properties from.
     */
    public DefaultConfig(final DefaultConfig configurable) {
        this(configurable.properties, configurable.providerBag, configurable.featureBag);
    }

    public DefaultConfig(final Map<String, Object> properties) {
        this();
        this.properties.putAll(properties);
    }

    public DefaultConfig(final Map<String, Object> properties, final ProviderBag providerBag, final FeatureBag featureBag) {
        this.providerBag = new ProviderBag.Builder(providerBag).build();
        this.featureBag = new FeatureBag.Builder(featureBag).build();

        this.properties = new HashMap<String, Object>(properties);
        this.immutablePropertiesView = Collections.unmodifiableMap(this.properties);
    }

    @Override
    public Map<String, Object> getProperties() {
        return immutablePropertiesView;
    }

    @Override
    public Object getProperty(final String name) {
        return properties.get(name);
    }

    @Override
    public Configurable setProperties(final Map<String, ?> properties) {
        if (locked) {
            throw new IllegalStateException(LocalizationMessages.CONFIGURABLE_NOT_MODIFIABLE());
        }

        this.properties.clear();

        if (properties != null) {
            this.properties.putAll(properties);
        }
        return this;
    }

    @Override
    public Configurable setProperty(final String name, final Object value) {
        if (locked) {
            throw new IllegalStateException(LocalizationMessages.CONFIGURABLE_NOT_MODIFIABLE());
        }

        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
        return this;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return featureBag.getEnabledFeatures();
    }

    @Override
    public Set<Class<?>> getProviderClasses() {
        return providerBag.getClasses();
    }

    @Override
    public Set<Object> getProviderInstances() {
        return providerBag.getInstances();
    }

    @Override
    public Configurable register(final Class<?> providerClass) {
        return this.register(providerClass, ContractProvider.NO_PRIORITY);
    }

    @Override
    public Configurable register(final Object provider) {
        return register(provider, ContractProvider.NO_PRIORITY);
    }

    @Override
    public Configurable register(final Class<?> providerClass, final int bindingPriority) {
        if (providerClass == null) {
            throw new IllegalArgumentException(LocalizationMessages.PROVIDER_CLASS_CANNOT_BE_NULL());
        }
        return register(providerClass, bindingPriority, Providers.getProviderContracts(providerClass));
    }

    @Override
    public <P> Configurable register(final Class<P> providerClass, final Class<? super P>... contracts) {
        return register(providerClass, ContractProvider.NO_PRIORITY, contracts);
    }

    @Override
    public <P> Configurable register(final Class<P> providerClass, final int bindingPriority, Class<? super P>... contracts) {
        return register(providerClass, bindingPriority, new HashSet<Class<?>>(Arrays.asList(contracts)));
    }

    @Override
    public Configurable register(final Object provider, final int bindingPriority) {
        if (provider == null) {
            throw new IllegalArgumentException(LocalizationMessages.PROVIDER_CANNOT_BE_NULL());
        }
        return register(provider, bindingPriority, Providers.getProviderContracts(provider.getClass()));
    }

    @Override
    public <P> Configurable register(final Object provider, final Class<? super P>... contracts) {
        return register(provider, ContractProvider.NO_PRIORITY, contracts);
    }

    @Override
    public <P> Configurable register(final Object provider, final int bindingPriority, final Class<? super P>... contracts) {
        return register(provider, bindingPriority, new HashSet<Class<?>>(Arrays.asList(contracts)));
    }

    /**
     * Checks whether a set of contracts is supported by this configuration. Method should log a warning message if a certain
     * contract is not allowed by this configuration and it returns (modified) set of allowed contracts.
     *
     * @param bindingContracts set of contracts to perform check on.
     * @return set of contracts allowed by this configuration.
     */
    protected Set<Class<?>> checkContracts(final Class<?> providerClass,
                                           final Set<Class<?>> allProviderContracts, final Set<Class<?>> bindingContracts) {
        bindingContracts.retainAll(allProviderContracts);
        return bindingContracts;
    }

    /**
     * Returns a {@link ProviderBag} instance.
     *
     * @return a non-null provider bag instance.
     */
    public ProviderBag getProviderBag() {
        return providerBag;
    }

    /**
     * Returns a {@link FeatureBag} instance.
     *
     * @return a non-null feature bag instance.
     */
    public FeatureBag getFeatureBag() {
        return featureBag;
    }

    /**
     * Indicates whether is the given feature enabled in this {@code Configurable} or not.
     *
     * @param featureClass feature class to check.
     * @return {@code true} if the feature is enabled in this {@code Configurable}, {@code false} otherwise.
     */
    public boolean isEnabled(final Class<? extends Feature> featureClass) {
        return getFeatureBag().isEnabled(featureClass);
    }

    private <P> Configurable register(final Class<P> providerClass, final int bindingPriority, Set<Class<?>> contracts) {
        return register(providerClass, null, bindingPriority, contracts);
    }

    private Configurable register(final Object provider, final int bindingPriority, Set<Class<?>> contracts) {
        if (provider == null) {
            throw new IllegalArgumentException(LocalizationMessages.PROVIDER_CANNOT_BE_NULL());
        }

        return register(provider.getClass(), provider, bindingPriority, contracts);
    }

    /**
     * Registers the given provider for future binding.
     *
     * @param providerClass provider class.
     * @param provider (optional) provider instance.
     * @param bindingPriority binding priority.
     * @param contracts contracts to bind the provider to.
     * @return updated {@code Configurable} instance.
     *
     * @see javax.ws.rs.BindingPriority
     */
    private <P> Configurable register(final Class<P> providerClass,
                                       final Object provider, final int bindingPriority, Set<Class<?>> contracts) {
        if (locked) {
            throw new IllegalStateException(LocalizationMessages.CONFIGURABLE_NOT_MODIFIABLE());
        }
        if (providerClass == null) {
            throw new IllegalArgumentException(LocalizationMessages.PROVIDER_CLASS_CANNOT_BE_NULL());
        }

        final Set<Class<?>> allProviderContracts = Providers.getProviderContracts(providerClass);

        contracts.remove(null);
        if (contracts.isEmpty()) {
            contracts = allProviderContracts;
        }

        contracts = checkContracts(providerClass, allProviderContracts, contracts);

        // Enable feature.
        if (Feature.class.isAssignableFrom(providerClass) && contracts.contains(Feature.class)) {
            final FeatureBag.Builder featureBagBuilder = new FeatureBag.Builder(featureBag);

            if (provider != null) {
                featureBagBuilder.add((Feature) provider);
            } else {
                //noinspection unchecked
                featureBagBuilder.add((Class<Feature>) providerClass);
            }

            featureBag = featureBagBuilder.build();
            contracts.remove(Feature.class);
        }

        // Register provider if there are contracts to register to.
        if (!contracts.isEmpty()) {
            final ProviderBag.Builder providerBagBuilder = new ProviderBag.Builder(providerBag);

            if (provider != null) {
                providerBagBuilder.register(provider, bindingPriority, contracts);
            } else {
                providerBagBuilder.register(providerClass, bindingPriority, contracts);
            }

            providerBag = providerBagBuilder.build();
        }

        return this;
    }

    /**
     * Locks this configuration so it cannot be modified.
     */
    public void lock() {
        this.locked = true;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DefaultConfig)) {
            return false;
        }
        DefaultConfig other = (DefaultConfig) obj;

        return this == other
                || (properties == other.properties || properties.equals(other.properties))
                && (providerBag == other.providerBag || providerBag.equals(other.providerBag))
                && (featureBag == other.featureBag || featureBag.equals(other.featureBag));
    }

    @Override
    public int hashCode() {
        int hash = 23;
        hash = 7 * hash + this.properties.hashCode();
        hash = 7 * hash + this.providerBag.hashCode();
        hash = 7 * hash + this.featureBag.hashCode();
        return hash;
    }
}
