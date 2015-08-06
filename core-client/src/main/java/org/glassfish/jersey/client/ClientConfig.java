/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.ExtendedConfig;
import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.JerseyClassAnalyzer;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.model.internal.CommonConfig;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.process.internal.ExecutorProviders;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Jersey externalized implementation of client-side JAX-RS {@link javax.ws.rs.core.Configurable
 * configurable} contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class ClientConfig implements Configurable<ClientConfig>, ExtendedConfig {
    /**
     * Internal configuration state.
     */
    private State state;

    /**
     * Default encapsulation of the internal configuration state.
     */
    private static class State implements Configurable<State>, ExtendedConfig {

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
        private final CommonConfig commonConfig;
        private final JerseyClient client;
        private volatile ConnectorProvider connectorProvider;


        private final LazyValue<ClientRuntime> runtime = Values.lazy(new Value<ClientRuntime>() {
            @Override
            public ClientRuntime get() {
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
             * invoked configuration state mutator method.
             */
            public State onChange(final State state);
        }

        /**
         * Default configuration state constructor with {@link StateChangeStrategy "identity"}
         * state change strategy.
         *
         * @param client bound parent Jersey client.
         */
        State(final JerseyClient client) {
            this.strategy = IDENTITY;
            this.commonConfig = new CommonConfig(RuntimeType.CLIENT, ComponentBag.EXCLUDE_EMPTY);
            this.client = client;
            final Iterator<ConnectorProvider> iterator = ServiceFinder.find(ConnectorProvider.class).iterator();
            if (iterator.hasNext()) {
                this.connectorProvider = iterator.next();
            } else {
                this.connectorProvider = new HttpUrlConnectorProvider();
            }
        }

        /**
         * Copy the original configuration state while using the default state change
         * strategy.
         *
         * @param client   new Jersey client parent for the state.
         * @param original configuration strategy to be copied.
         */
        private State(final JerseyClient client, final State original) {
            this.strategy = IDENTITY;
            this.client = client;
            this.commonConfig = new CommonConfig(original.commonConfig);
            this.connectorProvider = original.connectorProvider;
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
        State copy(final JerseyClient client) {
            return new State(client, this);
        }

        void markAsShared() {
            strategy = COPY_ON_CHANGE;
        }

        State preInitialize() {
            final State state = strategy.onChange(this);
            state.strategy = COPY_ON_CHANGE;
            state.runtime.get().preInitialize();
            return state;

        }

        @Override
        public State property(final String name, final Object value) {
            final State state = strategy.onChange(this);
            state.commonConfig.property(name, value);
            return state;
        }

        public State loadFrom(final Configuration config) {
            final State state = strategy.onChange(this);
            state.commonConfig.loadFrom(config);
            return state;
        }

        @Override
        public State register(final Class<?> providerClass) {
            final State state = strategy.onChange(this);
            state.commonConfig.register(providerClass);
            return state;
        }

        @Override
        public State register(final Object provider) {
            final State state = strategy.onChange(this);
            state.commonConfig.register(provider);
            return state;
        }

        @Override
        public State register(final Class<?> providerClass, final int bindingPriority) {
            final State state = strategy.onChange(this);
            state.commonConfig.register(providerClass, bindingPriority);
            return state;
        }

        @Override
        public State register(final Class<?> providerClass, final Class<?>... contracts) {
            final State state = strategy.onChange(this);
            state.commonConfig.register(providerClass, contracts);
            return state;
        }

        @Override
        public State register(final Class<?> providerClass, final Map<Class<?>, Integer> contracts) {
            final State state = strategy.onChange(this);
            state.commonConfig.register(providerClass, contracts);
            return state;
        }

        @Override
        public State register(final Object provider, final int bindingPriority) {
            final State state = strategy.onChange(this);
            state.commonConfig.register(provider, bindingPriority);
            return state;
        }

        @Override
        public State register(final Object provider, final Class<?>... contracts) {
            final State state = strategy.onChange(this);
            state.commonConfig.register(provider, contracts);
            return state;
        }

        @Override
        public State register(final Object provider, final Map<Class<?>, Integer> contracts) {
            final State state = strategy.onChange(this);
            state.commonConfig.register(provider, contracts);
            return state;
        }

        State connectorProvider(final ConnectorProvider provider) {
            if (provider == null) {
                throw new NullPointerException(LocalizationMessages.NULL_CONNECTOR_PROVIDER());
            }
            final State state = strategy.onChange(this);
            state.connectorProvider = provider;
            return state;
        }

        Connector getConnector() {
            // Get the connector only if the runtime has been initialized.
            return (runtime.isInitialized()) ? runtime.get().getConnector() : null;
        }

        ConnectorProvider getConnectorProvider() {
            return connectorProvider;
        }

        JerseyClient getClient() {
            return client;
        }

        @Override
        public State getConfiguration() {
            return this;
        }

        @Override
        public RuntimeType getRuntimeType() {
            return commonConfig.getConfiguration().getRuntimeType();
        }

        @Override
        public Map<String, Object> getProperties() {
            return commonConfig.getConfiguration().getProperties();
        }

        @Override
        public Object getProperty(final String name) {
            return commonConfig.getConfiguration().getProperty(name);
        }

        @Override
        public Collection<String> getPropertyNames() {
            return commonConfig.getConfiguration().getPropertyNames();
        }

        @Override
        public boolean isProperty(final String name) {
            return commonConfig.getConfiguration().isProperty(name);
        }

        @Override
        public boolean isEnabled(final Feature feature) {
            return commonConfig.getConfiguration().isEnabled(feature);
        }

        @Override
        public boolean isEnabled(final Class<? extends Feature> featureClass) {
            return commonConfig.getConfiguration().isEnabled(featureClass);
        }

        @Override
        public boolean isRegistered(final Object component) {
            return commonConfig.getConfiguration().isRegistered(component);
        }

        @Override
        public boolean isRegistered(final Class<?> componentClass) {
            return commonConfig.getConfiguration().isRegistered(componentClass);
        }

        @Override
        public Map<Class<?>, Integer> getContracts(final Class<?> componentClass) {
            return commonConfig.getConfiguration().getContracts(componentClass);
        }

        @Override
        public Set<Class<?>> getClasses() {
            return commonConfig.getConfiguration().getClasses();
        }

        @Override
        public Set<Object> getInstances() {
            return commonConfig.getConfiguration().getInstances();
        }

        public void configureAutoDiscoverableProviders(final ServiceLocator locator) {
            commonConfig.configureAutoDiscoverableProviders(locator, false);
        }

        public void configureForcedAutoDiscoverableProviders(final ServiceLocator locator) {
            commonConfig.configureAutoDiscoverableProviders(locator, true);
        }

        public void configureMetaProviders(final ServiceLocator locator) {
            commonConfig.configureMetaProviders(locator);
        }

        public ComponentBag getComponentBag() {
            return commonConfig.getComponentBag();
        }

        /**
         * Initialize the newly constructed client instance.
         */
        @SuppressWarnings("MethodOnlyUsedFromInnerClass")
        private ClientRuntime initRuntime() {
            /**
             * Ensure that any attempt to add a new provider, feature, binder or modify the connector
             * will cause a copy of the current state.
             */
            markAsShared();

            final State runtimeCfgState = this.copy();
            runtimeCfgState.markAsShared();

            final ServiceLocator locator = Injections.createLocator(new ClientBinder(runtimeCfgState.getProperties()));
            locator.setDefaultClassAnalyzerName(JerseyClassAnalyzer.NAME);

            // AutoDiscoverable.
            if (!CommonProperties.getValue(runtimeCfgState.getProperties(), RuntimeType.CLIENT,
                    CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.FALSE, Boolean.class)) {
                runtimeCfgState.configureAutoDiscoverableProviders(locator);
            } else {
                runtimeCfgState.configureForcedAutoDiscoverableProviders(locator);
            }

            // Configure binders and features.
            runtimeCfgState.configureMetaProviders(locator);

            // Bind configuration.
            final AbstractBinder configBinder = new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(runtimeCfgState).to(Configuration.class);
                }
            };
            final DynamicConfiguration dc = Injections.getConfiguration(locator);
            configBinder.bind(dc);
            dc.commit();

            // Bind providers.
            ProviderBinder.bindProviders(runtimeCfgState.getComponentBag(), RuntimeType.CLIENT, null, locator);

            // Bind executors.
            ExecutorProviders.createInjectionBindings(locator);

            final ClientConfig configuration = new ClientConfig(runtimeCfgState);
            final Connector connector = connectorProvider.getConnector(client, configuration);
            final ClientRuntime crt = new ClientRuntime(configuration, connector, locator);

            client.registerShutdownHook(crt);

            return crt;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final State state = (State) o;

            if (client != null ? !client.equals(state.client) : state.client != null) {
                return false;
            }
            if (!commonConfig.equals(state.commonConfig)) {
                return false;
            }
            return connectorProvider == null ? state.connectorProvider == null
                    : connectorProvider.equals(state.connectorProvider);
        }

        @Override
        public int hashCode() {
            int result = commonConfig.hashCode();
            result = 31 * result + (client != null ? client.hashCode() : 0);
            result = 31 * result + (connectorProvider != null ? connectorProvider.hashCode() : 0);
            return result;
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
    public ClientConfig(final Class<?>... providerClasses) {
        this();
        for (final Class<?> providerClass : providerClasses) {
            state.register(providerClass);
        }
    }

    /**
     * Construct a new Jersey configuration instance and register the provided list of provider instances.
     *
     * @param providers provider instances to be registered with this client configuration.
     */
    public ClientConfig(final Object... providers) {
        this();
        for (final Object provider : providers) {
            state.register(provider);
        }
    }

    /**
     * Construct a new Jersey configuration instance with the features as well as
     * property values copied from the supplied JAX-RS configuration instance.
     *
     * @param parent parent Jersey client instance.
     */
    ClientConfig(final JerseyClient parent) {
        this.state = new State(parent);
    }

    /**
     * Construct a new Jersey configuration instance with the features as well as
     * property values copied from the supplied JAX-RS configuration instance.
     *
     * @param parent parent Jersey client instance.
     * @param that   original {@link javax.ws.rs.core.Configuration}.
     */
    ClientConfig(final JerseyClient parent, final Configuration that) {
        if (that instanceof ClientConfig) {
            state = ((ClientConfig) that).state.copy(parent);
        } else {
            state = new State(parent);
            state.loadFrom(that);
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

    /**
     * Load the internal configuration state from an externally provided configuration state.
     *
     * Calling this method effectively replaces existing configuration state of the instance
     * with the state represented by the externally provided configuration.
     *
     * @param config external configuration state to replace the configuration of this configurable
     *               instance.
     * @return the updated client configuration instance.
     */
    public ClientConfig loadFrom(final Configuration config) {
        if (config instanceof ClientConfig) {
            state = ((ClientConfig) config).state.copy();
        } else {
            state.loadFrom(config);
        }
        return this;
    }

    @Override
    public ClientConfig register(final Class<?> providerClass) {
        state = state.register(providerClass);
        return this;
    }

    @Override
    public ClientConfig register(final Object provider) {
        state = state.register(provider);
        return this;
    }

    @Override
    public ClientConfig register(final Class<?> providerClass, final int bindingPriority) {
        state = state.register(providerClass, bindingPriority);
        return this;
    }

    @Override
    public ClientConfig register(final Class<?> providerClass, final Class<?>... contracts) {
        state = state.register(providerClass, contracts);
        return this;
    }

    @Override
    public ClientConfig register(final Class<?> providerClass, final Map<Class<?>, Integer> contracts) {
        state = state.register(providerClass, contracts);
        return this;
    }

    @Override
    public ClientConfig register(final Object provider, final int bindingPriority) {
        state = state.register(provider, bindingPriority);
        return this;
    }

    @Override
    public ClientConfig register(final Object provider, final Class<?>... contracts) {
        state = state.register(provider, contracts);
        return this;
    }

    @Override
    public ClientConfig register(final Object provider, final Map<Class<?>, Integer> contracts) {
        state = state.register(provider, contracts);
        return this;
    }

    @Override
    public ClientConfig property(final String name, final Object value) {
        state = state.property(name, value);
        return this;
    }

    @Override
    public ClientConfig getConfiguration() {
        return this;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return state.getRuntimeType();
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
    public Collection<String> getPropertyNames() {
        return state.getPropertyNames();
    }

    @Override
    public boolean isProperty(final String name) {
        return state.isProperty(name);
    }

    @Override
    public boolean isEnabled(final Feature feature) {
        return state.isEnabled(feature);
    }

    @Override
    public boolean isEnabled(final Class<? extends Feature> featureClass) {
        return state.isEnabled(featureClass);
    }

    @Override
    public boolean isRegistered(final Object component) {
        return state.isRegistered(component);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(final Class<?> componentClass) {
        return state.getContracts(componentClass);
    }

    @Override
    public boolean isRegistered(final Class<?> componentClass) {
        return state.isRegistered(componentClass);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return state.getClasses();
    }

    @Override
    public Set<Object> getInstances() {
        return state.getInstances();
    }

    /**
     * Register a custom Jersey client connector provider.
     * <p>
     * The registered {@code ConnectorProvider} instance will provide a
     * Jersey client {@link org.glassfish.jersey.client.spi.Connector}
     * for the {@link org.glassfish.jersey.client.JerseyClient} instance
     * created with this client configuration.
     * </p>
     *
     * @param connectorProvider custom connector provider. Must not be {@code null}.
     * @return this client config instance.
     * @throws java.lang.NullPointerException in case the {@code connectorProvider} is {@code null}.
     * @since 2.5
     */
    public ClientConfig connectorProvider(final ConnectorProvider connectorProvider) {
        state = state.connectorProvider(connectorProvider);
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
     * Get the client transport connector provider.
     *
     * If no custom connector provider has been set,
     * {@link org.glassfish.jersey.client.HttpUrlConnectorProvider default connector provider}
     * instance is returned.
     *
     * @return configured client transport connector provider.
     * @since 2.5
     */
    public ConnectorProvider getConnectorProvider() {
        return state.getConnectorProvider();
    }

    /**
     * Get the configured runtime.
     *
     * @return configured runtime.
     */
    ClientRuntime getRuntime() {
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
     * Pre initializes this configuration by initializing {@link ClientRuntime client runtime}
     * including {@link org.glassfish.jersey.message.MessageBodyWorkers message body workers}.
     * Once this method is called no other method implementing {@link Configurable} should be called
     * on this pre initialized configuration otherwise configuration will change back to uninitialized.
     * <p/>
     * Note that this method must be called only when configuration is attached to the client.
     *
     * @return Client configuration.
     */
    ClientConfig preInitialize() {
        state = state.preInitialize();
        return this;
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
