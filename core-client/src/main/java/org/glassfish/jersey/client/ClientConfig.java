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
package org.glassfish.jersey.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Feature;

import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.process.Inflector;

import org.glassfish.hk2.utilities.Binder;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

/**
 * Jersey implementation of {@link Configuration JAX-RS client
 * configuration} contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class ClientConfig implements Configuration, FeaturesAndProperties {

    /**
     * Default encapsulation of the internal configuration state.
     */
    private static class State implements Configuration {

        /**
         * Configuration state change strategy.
         */
        private static interface StateChangeStrategy {

            /**
             * Invoked whenever a mutator method is called in the given configuration
             * state.
             *
             * @param state configuration state to be mutated.
             * @return state instance that will be mutated and returned from the
             *     invoked configuration state mutator method.
             */
            public State onChange(final State state);
        }

        /**
         * Strategy that returns the same state instance.
         */
        private static final StateChangeStrategy IDENTITY = new StateChangeStrategy() {

            @Override
            public State onChange(final State state) {
                return state;
            }
        };
        /**
         * Strategy that returns a copy of the state instance.
         */
        private static final StateChangeStrategy COPY_ON_CHANGE = new StateChangeStrategy() {

            @Override
            public State onChange(final State state) {
                return state.copy();
            }
        };
        private transient StateChangeStrategy strategy;
        private final Map<String, Object> properties;
        private final Map<String, Object> immutablePropertiesView;
        private final Set<Class<?>> providerClasses;
        private final Set<Object> providerInstances;
        private final BiMap<Class<? extends Feature>, Feature> features;
        private final Set<Feature> featuresSetView;
        private final List<Binder> binders;
        private Inflector<ClientRequest, ClientResponse> connector;

        private volatile boolean locked = false;

        /**
         * Default configuration state constructor with {@link StateChangeStrategy "identity"}
         * state change strategy.
         */
        public State() {
            this.strategy = IDENTITY;

            this.properties = new HashMap<String, Object>();
            this.immutablePropertiesView = Collections.unmodifiableMap(properties);

            this.providerClasses = new LinkedHashSet<Class<?>>();
            this.providerInstances = new LinkedHashSet<Object>();

            this.binders = Lists.newLinkedList();

            this.features = HashBiMap.create();
            this.featuresSetView = Collections.unmodifiableSet(features.values());
        }

        /**
         * Copy the original configuration state while using the default state change
         * strategy.
         *
         * @param original configuration strategy to be copied.
         */
        private State(State original) {
            this.strategy = IDENTITY;

            this.properties = new HashMap<String, Object>(original.properties);
            this.immutablePropertiesView = Collections.unmodifiableMap(properties);

            this.providerClasses = new LinkedHashSet<Class<?>>(original.providerClasses);
            this.providerInstances = new LinkedHashSet<Object>(original.providerInstances);

            this.binders = Lists.newLinkedList(original.binders);

            this.features = HashBiMap.create(original.features);
            this.featuresSetView = Collections.unmodifiableSet(this.features.values());

            this.connector = original.connector;
        }

        private State copy() {
            return new State(this);
        }

        public void markAsShared() {
            strategy = COPY_ON_CHANGE;
        }

        @Override
        public Map<String, Object> getProperties() {
            return immutablePropertiesView;
        }

        @Override
        public Object getProperty(final String name) {
            return properties.get(name);
        }

        public boolean isProperty(final String name) {
            if (properties.containsKey(name)) {
                Object value = properties.get(name);
                if (value instanceof Boolean) {
                    return Boolean.class.cast(value);
                } else {
                    return Boolean.parseBoolean(value.toString());
                }
            }

            return false;
        }

        @Override
        public Set<Feature> getFeatures() {
            return featuresSetView;
        }

        public boolean isEnabled(final Class<? extends Feature> featureClass) {
            return features.containsKey(featureClass);
        }

        @Override
        public Set<Class<?>> getProviderClasses() {
            return providerClasses;
        }

        @Override
        public Set<Object> getProviderInstances() {
            return providerInstances;
        }

        @Override
        public State update(final javax.ws.rs.client.Configuration configuration) {
            return new State(((ClientConfig) configuration).state);
        }

        @Override
        public State register(final Class<?> providerClass) {
            final State state = strategy.onChange(this);
            state.providerClasses.add(providerClass);
            return state;
        }

        @Override
        public State register(final Object provider) {
            final State state = strategy.onChange(this);
            state.providerInstances.add(provider);
            return state;
        }

        public State enable(final Feature feature) {
            final Class<? extends Feature> featureClass = feature.getClass();
            if (features.containsKey(featureClass)) {
                throw new IllegalStateException(String.format("Feature [%s] has already been enabled.", featureClass));
            }

            final State state = strategy.onChange(this);
            boolean success = feature.onEnable(state);
            if (success) {
                state.features.put(featureClass, feature);
            }

            return state;
        }

        @Override
        public State setProperties(final Map<String, ?> properties) {
            final State state = strategy.onChange(this);
            state.properties.clear();
            state.properties.putAll(properties);
            return state;
        }

        @Override
        public State setProperty(final String name, final Object value) {
            final State state = strategy.onChange(this);
            state.properties.put(name, value);
            return state;
        }

        void binders(Binder... binders) {
            checkLocked();
            if (binders != null && binders.length > 0) {
                Collections.addAll(this.binders, binders);
            }
        }

        List<Binder> getCustomBinders() {
            return binders;
        }

        void setConnector(Inflector<ClientRequest, ClientResponse> connector) {
            checkLocked();
            this.connector = connector;
        }

        Inflector<ClientRequest, ClientResponse> getConnector() {
            return connector;
        }

        void lock() {
            locked = true;
        }

        private void checkLocked() {
            if (locked) {
                throw new IllegalStateException();
            }
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof State)) {
                return false;
            }
            State other = (State) obj;
            return this == other
                    || (properties == other.properties || properties.equals(other.properties))
                    && (providerClasses == other.providerClasses || providerClasses.equals(other.providerClasses))
                    && (providerInstances == other.providerInstances || providerInstances.equals(other.providerInstances))
                    && (binders == other.binders || binders.equals(other.binders))
                    && (connector == other.connector
                    || connector != null && connector.equals(other.connector))
                    ;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + this.properties.hashCode();
            hash = 41 * hash + this.providerClasses.hashCode();
            hash = 41 * hash + this.providerInstances.hashCode();
            hash = 41 * hash + this.binders.hashCode();
            hash = 41 * hash + (this.connector != null ? this.connector.hashCode() : 0);
            return hash;
        }
    }

    /**
     * Internal configuration state.
     */
    private State state;

    /**
     * Construct a new Jersey configuration instance with the default features
     * and property values.
     */
    public ClientConfig() {
        this.state = new State();
    }

    /**
     * Construct a new Jersey configuration instance and register the provided list of provider classes.
     * @param providerClasses provider classes to be registered with this client configuration.
     */
    public ClientConfig(Class<?>... providerClasses) {
        this();
        for (Class<?> providerClass : providerClasses) {
            state.register(providerClass);
        }
    }

    /**
     * Construct a new Jersey configuration instance and register the provided list of provider instances.
     * @param providers provider instances to be registered with this client configuration.
     */
    public ClientConfig(Object... providers) {
        this();
        for (Object provider : providers) {
            state.register(provider);
        }
    }

    /**
     * Construct a new Jersey configuration instance with the features as well as
     * property values copied from the supplied JAX-RS configuration instance.
     *
     * @param that original {@link javax.ws.rs.client.Configuration}.
     *
     */
    ClientConfig(javax.ws.rs.client.Configuration that) {
        this.state = new State();

        state = state.setProperties(that.getProperties());

        for (Object provider : that.getProviderInstances()) {
            state = state.register(provider);
        }
        for (Class<?> providerClass : that.getProviderClasses()) {
            state = state.register(providerClass);
        }


        for (Feature feature : that.getFeatures()) {
            state = state.enable(feature);
        }
    }

    /**
     * Construct a new Jersey configuration instance using the supplied state.
     *
     * @param state to be referenced from the new configuration instance.
     */
    private ClientConfig(final State state) {
        this.state = state;
    }

    /**
     * Take a snapshot of the current configuration and its internal state.
     * <p/>
     * The returned configuration object is an new instance different from the
     * original one, however the cloning of the internal configuration state is
     * lazily deferred until either original or the snapshot configuration is
     * modified for the first time since the snapshot was taken.
     *
     * @return snapshot of the current configuration.
     */
    ClientConfig snapshot() {
        state.markAsShared();
        return new ClientConfig(state);
    }

    @Override
    public Map<String, Object> getProperties() {
        return state.getProperties();
    }

    @Override
    public Object getProperty(final String name) {
        return state.getProperty(name);
    }

    @Override
    public boolean isProperty(final String name) {
        return state.isProperty(name);
    }

    @Override
    public Set<Feature> getFeatures() {
        return state.getFeatures();
    }

    public boolean isEnabled(final Class<? extends Feature> feature) {
        return state.isEnabled(feature);
    }

    @Override
    public Set<Class<?>> getProviderClasses() {
        return state.getProviderClasses();
    }

    @Override
    public Set<Object> getProviderInstances() {
        return state.getProviderInstances();
    }

    @Override
    public ClientConfig update(final javax.ws.rs.client.Configuration configuration) {
        state = state.update(configuration);
        return this;
    }

    @Override
    public ClientConfig register(final Class<?> providerClass) {
        // TODO features
        state = state.register(providerClass);
        return this;
    }

    @Override
    public ClientConfig register(final Object provider) {
        // TODO features properly
        if (provider instanceof Feature) {
            state = state.enable((Feature) provider);
        } else {
            state = state.register(provider);
        }
        return this;
    }

    @Override
    public ClientConfig setProperties(final Map<String, ?> properties) {
        state = state.setProperties(properties);
        return this;
    }

    @Override
    public ClientConfig setProperty(final String name, final Object value) {
        state = state.setProperty(name, value);
        return this;
    }

    /**
     * Set Jersey client transport connector.
     *
     * @param connector client transport connector.
     * @return this client config instance.
     */
    public ClientConfig connector(Inflector<ClientRequest, ClientResponse> connector) {
        state.setConnector(connector);
        return this;
    }

    /**
     * Register custom HK2 binders.
     *
     * @param binders custom HK2 binders to be registered with the Jersey client.
     * @return this client config instance.
     */
    public ClientConfig binders(Binder... binders) {
        state.binders(binders);
        return this;
    }

    void lock() {
        state.lock();
    }

    Inflector<ClientRequest, ClientResponse> getConnector() {
        return state.getConnector();
    }

    List<Binder> getCustomBinders() {
        return state.getCustomBinders();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ClientConfig other = (ClientConfig) obj;
        return this.state == other.state || (this.state != null && this.state.equals(other.state));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.state != null ? this.state.hashCode() : 0);
        return hash;
    }
}
