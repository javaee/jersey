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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Configuration;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.Config;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
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
public class ClientConfig implements Configuration, Config, Configurable {
    /**
     * Internal configuration state.
     */
    private State state;

    /**
     * Default encapsulation of the internal configuration state.
     */
    private static class State implements Configuration, Config, Configurable {

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

        private volatile StateChangeStrategy strategy;
        private final Map<String, Object> properties;
        private final Map<String, Object> immutablePropertiesView;
        private final Set<Class<?>> providerClasses;

        private final Set<Class<?>> immutableClassesView;
        private final Set<Object> providerInstances;
        private final Set<Object> immutableInstancesView;
        private final Set<Feature> immutableFeatureSetView;

        private final BiMap<Class<? extends Feature>, Feature> features;
        private final List<Binder> binders;

        private final JerseyClient client;
        private Connector connector;

        private Value<Runtime> runtime = Values.lazy(new Value<Runtime>() {
            @Override
            public Runtime get() {
                return initRuntime();
            }
        });

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
             *         invoked configuration state mutator method.
             */
            public State onChange(final State state);
        }

        /**
         * Default configuration state constructor with {@link StateChangeStrategy "identity"}
         * state change strategy.
         *
         * @param client bound parent Jersey client.
         */
        State(JerseyClient client) {
            this.client = client;
            this.strategy = IDENTITY;

            this.properties = new HashMap<String, Object>();
            this.providerClasses = new LinkedHashSet<Class<?>>();
            this.providerInstances = new LinkedHashSet<Object>();
            this.features = HashBiMap.create();

            this.immutablePropertiesView = Collections.unmodifiableMap(properties);
            this.immutableClassesView = Collections.unmodifiableSet(providerClasses);
            this.immutableInstancesView = Collections.unmodifiableSet(providerInstances);
            this.immutableFeatureSetView = Collections.unmodifiableSet(features.values());

            this.binders = Lists.newLinkedList();
            this.connector = null;
        }

        /**
         * Copy the original configuration state while using the default state change
         * strategy.
         *
         * @param client   new Jersey client parent for the state.
         * @param original configuration strategy to be copied.
         */
        private State(JerseyClient client, State original) {
            this.client = client;
            this.strategy = IDENTITY;

            this.properties = new HashMap<String, Object>(original.properties);
            this.providerClasses = new LinkedHashSet<Class<?>>(original.providerClasses);
            this.providerInstances = new LinkedHashSet<Object>(original.providerInstances);
            this.features = HashBiMap.create(original.features);


            this.immutablePropertiesView = Collections.unmodifiableMap(properties);
            this.immutableClassesView = Collections.unmodifiableSet(providerClasses);
            this.immutableInstancesView = Collections.unmodifiableSet(providerInstances);
            this.immutableFeatureSetView = Collections.unmodifiableSet(this.features.values());

            this.binders = Lists.newLinkedList(original.binders);
            this.connector = original.connector;
        }

        /**
         * Create a copy of the configuration state within the same parent Jersey
         * client instance scope.
         *
         * @return configuration state copy.
         */
        State copy() {
            return new State(this.client, this);
        }

        /**
         * Create a copy of the configuration state in a scope of the given
         * parent Jersey client instance.
         *
         * @param client parent Jersey client instance.
         * @return configuration state copy.
         */
        State copy(JerseyClient client) {
            return new State(client, this);
        }

        void markAsShared() {
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

        @Override
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
            return immutableFeatureSetView;
        }

        public boolean isEnabled(final Class<? extends Feature> featureClass) {
            return features.containsKey(featureClass);
        }

        @Override
        public Set<Class<?>> getProviderClasses() {
            return immutableClassesView;
        }

        @Override
        public Set<Object> getProviderInstances() {
            return immutableInstancesView;
        }

        @Override
        public State updateFrom(Configurable configuration) {
            return ((ClientConfig) configuration).state.copy(this.client);
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

        @Override
        public State register(Class<?> providerClass, int bindingPriority) {
            final State state = strategy.onChange(this);
            // TODO: implement method.
            return state;
        }

        @Override
        public <T> State register(Class<T> providerClass, Class<? super T>... contracts) {
            final State state = strategy.onChange(this);
            // TODO: implement method.
            return state;
        }

        @Override
        public <T> State register(Class<T> providerClass, int bindingPriority, Class<? super T>... contracts) {
            final State state = strategy.onChange(this);
            // TODO: implement method.
            return state;
        }

        @Override
        public State register(Object provider, int bindingPriority) {
            final State state = strategy.onChange(this);
            // TODO: implement method.
            return state;
        }

        @Override
        public <T> State register(Object provider, Class<? super T>... contracts) {
            final State state = strategy.onChange(this);
            // TODO: implement method.
            return state;
        }

        @Override
        public <T> State register(Object provider, int bindingPriority, Class<? super T>... contracts) {
            final State state = strategy.onChange(this);
            // TODO: implement method.
            return state;
        }

        public State enable(final Feature feature) {
            final Class<? extends Feature> featureClass = feature.getClass();
            if (features.containsKey(featureClass)) {
                throw new IllegalStateException(String.format("Feature [%s] has already been enabled.", featureClass));
            }

            final State state = strategy.onChange(this);
            boolean success = feature.configure(state);
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

        public State binders(Binder... binders) {
            if (binders != null && binders.length > 0) {
                final State state = strategy.onChange(this);
                Collections.addAll(state.binders, binders);
                return state;
            }
            return this;
        }

        public State setConnector(Connector connector) {
            final State state = strategy.onChange(this);
            state.connector = connector;
            return state;
        }

        Connector getConnector() {
            return connector;
        }

        JerseyClient getClient() {
            return client;
        }

        /**
         * Initialize the newly constructed client instance.
         */
        private Runtime initRuntime() {
            /**
             * Ensure that any attempt to add a new provider, feature, binder or modify the connector
             * will cause a copy of the current state.
             */
            markAsShared();

            final AbstractBinder configBinder = new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(State.this).to(Configuration.class).to(Config.class);
                }
            };
            final ServiceLocator locator;
            if (binders.isEmpty()) {
                locator = Injections.createLocator(configBinder, new ClientBinder());
            } else {
                final ArrayList<Binder> allBinders = new ArrayList<Binder>(binders.size() + 2);
                allBinders.add(configBinder);
                allBinders.add(new ClientBinder());
                allBinders.addAll(binders);
                locator = Injections.createLocator(allBinders.toArray(new Binder[allBinders.size()]));
            }

            final ProviderBinder providerBinder = new ProviderBinder(locator);
            providerBinder.bindClasses(getProviderClasses());
            providerBinder.bindInstances(getProviderInstances());

            final Runtime runtime = new Runtime(connector, locator);
            client.addListener(new JerseyClient.LifecycleListener() {
                @Override
                public void onClose() {
                    try {
                        runtime.close();
                    } finally {
                        ServiceLocatorFactory.getInstance().destroy(locator.getName());
                    }
                }
            });

            return runtime;
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
     * Construct a new Jersey configuration instance with the default features
     * and property values.
     */
    public ClientConfig() {
        this.state = new State(null);
    }

    /**
     * Construct a new Jersey configuration instance and register the provided list of provider classes.
     *
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
     *
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
     * @param parent parent Jersey client instance.
     */
    ClientConfig(JerseyClient parent) {
        this.state = new State(parent);
        this.state.setConnector(new HttpUrlConnector());
    }

    /**
     * Construct a new Jersey configuration instance with the features as well as
     * property values copied from the supplied JAX-RS configuration instance.
     *
     * @param parent parent Jersey client instance.
     * @param that   original {@link javax.ws.rs.client.Configuration}.
     */
    ClientConfig(JerseyClient parent, Configurable that) {
        if (that instanceof ClientConfig) {
            state = ((ClientConfig) that).state.copy(parent);
            if (state.getConnector() == null) {
                state.setConnector(new HttpUrlConnector());
            }
        } else {
            state = new State(parent);
            state.setConnector(new HttpUrlConnector());

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

    /**
     * Check if the given feature is enabled or not.
     *
     * @param feature tested feature.
     * @return {@code true} in case
     */
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
    public ClientConfig updateFrom(Configurable configuration) {
        state = state.updateFrom(configuration);
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
    public ClientConfig register(Class<?> providerClass, int bindingPriority) {
        state = state.register(providerClass, bindingPriority);
        return this;
    }

    @Override
    public <T> ClientConfig register(Class<T> providerClass, Class<? super T>... contracts) {
        state = state.register(providerClass, contracts);
        return this;
    }

    @Override
    public <T> ClientConfig register(Class<T> providerClass, int bindingPriority, Class<? super T>... contracts) {
        state = state.register(providerClass, bindingPriority, contracts);
        return this;
    }

    @Override
    public ClientConfig register(Object provider, int bindingPriority) {
        state = state.register(provider, bindingPriority);
        return this;
    }

    @Override
    public <T> ClientConfig register(Object provider, Class<? super T>... contracts) {
        state = state.register(provider, contracts);
        return this;
    }

    @Override
    public <T> ClientConfig register(Object provider, int bindingPriority, Class<? super T>... contracts) {
        state = state.register(provider, bindingPriority, contracts);
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
    public ClientConfig connector(Connector connector) {
        state = state.setConnector(connector);
        return this;
    }

    /**
     * Register custom HK2 binders.
     *
     * @param binders custom HK2 binders to be registered with the Jersey client.
     * @return this client config instance.
     */
    public ClientConfig binders(Binder... binders) {
        state = state.binders(binders);
        return this;
    }

    /**
     * Get the client transport connector.
     *
     * May return {@code null} if no connector has been set.
     *
     * @return client transport connector or {code null} if not set.
     */
    public Connector getConnector() {
        return state.getConnector();
    }

    /**
     * Get the configured runtime.
     *
     * @return configured runtime.
     */
    Runtime getRuntime() {
        return state.runtime.get();
    }

    /**
     * Get the parent Jersey client this configuration is bound to.
     *
     * May return {@code null} if no parent client has been bound.
     *
     * @return bound parent Jersey client or {@code null} if not bound.
     */
    public JerseyClient getClient() {
        return state.getClient();
    }

    /**
     * Check that the configuration instance has a parent client set.
     *
     * @throws IllegalStateException in case no parent Jersey client has been
     *                               bound to the configuration instance yet.
     */
    void checkClient() throws IllegalStateException {
        if (getClient() == null) {
            throw new IllegalStateException("Client configuration does not contain a parent client instance.");
        }
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
