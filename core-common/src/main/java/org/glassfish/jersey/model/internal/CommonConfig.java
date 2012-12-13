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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.ExtendedConfig;
import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.process.Inflector;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Common immutable {@link javax.ws.rs.core.Configuration} implementation for
 * server and client.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class CommonConfig implements FeatureContext, ExtendedConfig {
    private static final Logger LOGGER = Logger.getLogger(CommonConfig.class.getName());
    private static final Function<Object,Binder> CAST_TO_BINDER = new Function<Object, Binder>() {
        @Override
        public Binder apply(Object input) {
            return Binder.class.cast(input);
        }
    };

    /**
     * Configuration runtime type.
     */
    private final RuntimeType type;
    /**
     * Configuration properties collection and it's immutable views.
     */
    private final Map<String, Object> properties;
    private final Map<String, Object> immutablePropertiesView;
    private final Collection<String> immutablePropertyNames;
    /**
     * Configured providers, does not include features and binders.
     */
    private final ComponentBag componentBag;
    /**
     * Collection of unprocessed feature registrations.
     */
    private final List<FeatureRegistration> newFeatureRegistrations;
    /**
     * Collection of enabled feature classes.
     */
    private final Set<Class<? extends Feature>> enabledFeatureClasses;
    /**
     * Collection of enabled feature instances.
     */
    private final Set<Feature> enabledFeatures;

    /**
     * A single feature registration record.
     */
    private static final class FeatureRegistration {

        private final Class<? extends Feature> featureClass;
        private final Feature feature;

        private FeatureRegistration(final Class<? extends Feature> featureClass) {
            this.featureClass = featureClass;
            this.feature = null;
        }

        private FeatureRegistration(final Feature feature) {
            this.featureClass = feature.getClass();
            this.feature = feature;
        }

        /**
         * Get the registered feature class.
         *
         * @return registered feature class.
         */
        public Class<? extends Feature> getFeatureClass() {
            return featureClass;
        }

        /**
         * Get the registered feature instance or {@code null} if this is a
         * class based feature registration.
         *
         * @return the registered feature instance or {@code null} if this is a
         *         class based feature registration.
         */
        public Feature getFeature() {
            return feature;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FeatureRegistration)) {
                return false;
            }
            FeatureRegistration other = (FeatureRegistration) obj;

            return (featureClass == other.featureClass)
                    || (feature != null && (feature == other.feature || feature.equals(other.feature)));
        }

        @Override
        public int hashCode() {
            int hash = 47;
            hash = 13 * hash + (feature != null ? feature.hashCode() : 0);
            hash = 13 * hash + (featureClass != null ? featureClass.hashCode() : 0);
            return hash;
        }
    }

    /**
     * Create a new {@code RuntimeConfig} instance.
     * <p>
     * The constructor provides a way for defining a {@link ContractProvider contract
     * provider model} registration strategy. Once a registration model is built
     * for a newly registered contract, the provided registration strategy filter is
     * consulted whether the model should be registered or not.
     * </p>
     * <p>
     * Clients can use the method to cancel any contract provider model registration
     * that does not meet the criteria of a given configuration context, such as a model
     * that does not have any recognized contracts associated with it.
     * </p>
     *
     * @param type                 configuration runtime type.
     * @param registrationStrategy function driving the decision (based on the introspected
     *                             {@link ContractProvider contract provider model}) whether
     *                             or not should the component class registration continue
     *                             towards a successful completion.
     */
    public CommonConfig(RuntimeType type, Predicate<ContractProvider> registrationStrategy) {
        this.type = type;

        this.properties = new HashMap<String, Object>();
        this.immutablePropertiesView = Collections.unmodifiableMap(properties);
        this.immutablePropertyNames = Collections.unmodifiableCollection(properties.keySet());

        this.componentBag = ComponentBag.newInstance(registrationStrategy);

        this.newFeatureRegistrations = new LinkedList<FeatureRegistration>();

        this.enabledFeatureClasses = Sets.newIdentityHashSet();
        this.enabledFeatures = Sets.newHashSet();
    }

    /**
     * Copy constructor.
     *
     * @param config configurable to copy class properties from.
     */
    public CommonConfig(CommonConfig config) {
        this.type = config.type;

        this.properties = new HashMap<String, Object>(config.properties);
        this.immutablePropertiesView = Collections.unmodifiableMap(this.properties);
        this.immutablePropertyNames = Collections.unmodifiableCollection(this.properties.keySet());

        this.componentBag = config.componentBag.copy();

        this.newFeatureRegistrations = Lists.newLinkedList(config.newFeatureRegistrations);

        this.enabledFeatureClasses = Sets.newIdentityHashSet();
        this.enabledFeatureClasses.addAll(config.enabledFeatureClasses);

        this.enabledFeatures = Sets.newHashSet();
        this.enabledFeatures.addAll(config.enabledFeatures);
    }

    @Override
    public ExtendedConfig getConfiguration() {
        return this;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return type;
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
    public boolean isProperty(String name) {
        return PropertiesHelper.isProperty(getProperty(name));
    }

    @Override
    public Collection<String> getPropertyNames() {
        return immutablePropertyNames;
    }

    @Override
    public boolean isEnabled(final Class<? extends Feature> featureClass) {
        return enabledFeatureClasses.contains(featureClass);
    }

    @Override
    public boolean isEnabled(final Feature feature) {
        return enabledFeatures.contains(feature);
    }

    @Override
    public boolean isRegistered(Object component) {
        return componentBag.getInstances().contains(component);
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return componentBag.getRegistrations().contains(componentClass);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        final ContractProvider model = componentBag.getModel(componentClass);
        return (model == null) ? null : model.getContractMap();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return componentBag.getClasses();
    }

    @Override
    public Set<Object> getInstances() {
        return componentBag.getInstances();
    }

    /**
     * Returns a {@link ComponentBag} instance associated with the configuration.
     *
     * @return a non-null component bag instance.
     */
    public final ComponentBag getComponentBag() {
        return componentBag;
    }

    /**
     * An extension point that provides a way how to define a custom enhancement/update
     * operation of a contract provider model registration being produced for a given
     * component class.
     * Default implementation return an enhancer just builds the model.
     * <p>
     * Derived implementations may use this method to e.g. filter out all contracts not
     * applicable in the given configuration context or change the model scope. The returned
     * set of filtered contracts is then used for the actual provider registration.
     * </p>
     *
     * @param componentClass class of the component being registered.
     * @return filter for the contracts that being registered for a given component class.
     */
    protected Inflector<ContractProvider.Builder, ContractProvider> getModelEnhancer(final Class<?> componentClass) {
        return ComponentBag.AS_IS;
    }

    /**
     * Set the configured properties to the provided map of properties.
     *
     * @param properties new map of properties to be set.
     * @return updated configuration instance.
     */
    public CommonConfig setProperties(final Map<String, ?> properties) {
        this.properties.clear();

        if (properties != null) {
            this.properties.putAll(properties);
        }
        return this;
    }

    /**
     * Add properties to {@code ResourceConfig}.
     *
     * If any of the added properties exists already, he values of the existing
     * properties will be replaced with new values.
     *
     * @param properties properties to add.
     * @return updated configuration instance.
     */
    public CommonConfig addProperties(final Map<String, ?> properties) {
        if (properties != null) {
            this.properties.putAll(properties);
        }
        return this;
    }

    @Override
    public CommonConfig setProperty(final String name, final Object value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
        return this;
    }

    @Override
    public CommonConfig register(final Class<?> componentClass) {
        checkComponentClassNotNull(componentClass);
        if (componentBag.register(componentClass, getModelEnhancer(componentClass))) {
            processFeatureRegistration(null, componentClass);
        }

        return this;
    }

    @Override
    public CommonConfig register(final Class<?> componentClass, final int bindingPriority) {
        checkComponentClassNotNull(componentClass);
        if (componentBag.register(componentClass, bindingPriority, getModelEnhancer(componentClass))) {
            processFeatureRegistration(null, componentClass);
        }

        return this;
    }

    @Override
    public CommonConfig register(final Class<?> componentClass, final Class<?>... contracts) {
        checkComponentClassNotNull(componentClass);
        if (componentBag.register(componentClass, asNewIdentitySet(contracts), getModelEnhancer(componentClass))) {
            processFeatureRegistration(null, componentClass);
        }

        return this;
    }

    @Override
    public CommonConfig register(final Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        checkComponentClassNotNull(componentClass);
        if (componentBag.register(componentClass, contracts, getModelEnhancer(componentClass))) {
            processFeatureRegistration(null, componentClass);
        }

        return this;
    }

    @Override
    public CommonConfig register(final Object component) {
        checkProviderNotNull(component);

        final Class<?> componentClass = component.getClass();
        if (componentBag.register(component, getModelEnhancer(componentClass))) {
            processFeatureRegistration(component, componentClass);
        }

        return this;
    }

    @Override
    public CommonConfig register(final Object component, final int bindingPriority) {
        checkProviderNotNull(component);
        final Class<?> componentClass = component.getClass();
        if (componentBag.register(component, bindingPriority, getModelEnhancer(componentClass))) {
            processFeatureRegistration(component, componentClass);
        }

        return this;
    }

    @Override
    public CommonConfig register(final Object component, final Class<?>... contracts) {
        checkProviderNotNull(component);
        final Class<?> componentClass = component.getClass();
        if (componentBag.register(component, asNewIdentitySet(contracts), getModelEnhancer(componentClass))) {
            processFeatureRegistration(component, componentClass);
        }

        return this;
    }

    @Override
    public CommonConfig register(final Object component, Map<Class<?>, Integer> contracts) {
        checkProviderNotNull(component);
        final Class<?> componentClass = component.getClass();
        if (componentBag.register(component, contracts, getModelEnhancer(componentClass))) {
            processFeatureRegistration(component, componentClass);
        }

        return this;
    }

    private void processFeatureRegistration(Object component, Class<?> componentClass) {
        ContractProvider model = componentBag.getModel(componentClass);
        if (model.getContracts().contains(Feature.class)) {
            @SuppressWarnings("unchecked")
            final FeatureRegistration registration = (component != null) ?
                    new FeatureRegistration((Feature) component) :
                    new FeatureRegistration((Class<? extends Feature>) componentClass);
            newFeatureRegistrations.add(registration);
        }
    }

    @Override
    public CommonConfig replaceWith(Configuration config) {
        setProperties(config.getProperties());

        this.enabledFeatures.clear();
        this.enabledFeatureClasses.clear();

        componentBag.clear();
        resetRegistrations();

        for (Class<?> cls : config.getClasses()) {
            register(cls, config.getContracts(cls));
        }

        for (Object o : config.getInstances()) {
            register(o, config.getContracts(o.getClass()));
        }

        return this;
    }

    private Set<Class<?>> asNewIdentitySet(Class<?>... contracts) {
        Set<Class<?>> result = Sets.newIdentityHashSet();
        result.addAll(Arrays.asList(contracts));
        return result;
    }

    private void checkProviderNotNull(Object provider) {
        if (provider == null) {
            throw new IllegalArgumentException(LocalizationMessages.COMPONENT_CANNOT_BE_NULL());
        }
    }

    private void checkComponentClassNotNull(Class<?> componentClass) {
        if (componentClass == null) {
            throw new IllegalArgumentException(LocalizationMessages.COMPONENT_CLASS_CANNOT_BE_NULL());
        }
    }

    /**
     * Configure HK2 binders in the HK2 service locator and enable JAX-RS features.
     *
     * @param locator locator in which the binders and features should be configured.
     */
    public void configureMetaProviders(final ServiceLocator locator) {
        // First, configure existing binders
        final Set<Binder> configuredBinders = configureBinders(locator, Collections.<Binder>emptySet());

        // Next, configure all features
        configureFeatures(
                locator,
                new HashSet<FeatureRegistration>(),
                resetRegistrations());

        // At last, configure any new binders added by features
        configureBinders(locator, configuredBinders);
    }

    private Set<Binder> configureBinders(final ServiceLocator locator, final Set<Binder> configured) {
        Set<Binder> allConfigured = Sets.newIdentityHashSet();
        allConfigured.addAll(configured);

        final Collection<Binder> binders = getBinders(configured);
        if (!binders.isEmpty()) {
            final DynamicConfiguration dc = Injections.getConfiguration(locator);

            for (Binder binder : binders) {
                binder.bind(dc);
                allConfigured.add(binder);
            }
            dc.commit();
        }

        return allConfigured;
    }

    private Collection<Binder> getBinders(final Set<Binder> configured) {
        return Collections2.filter(
                Collections2.transform(componentBag.getInstances(ComponentBag.BINDERS_ONLY), CAST_TO_BINDER),
                new Predicate<Binder>() {
                    @Override
                    public boolean apply(Binder binder) {
                        return !configured.contains(binder);
                    }
                });
    }

    private void configureFeatures(final ServiceLocator locator,
                                   final Set<FeatureRegistration> processed,
                                   final List<FeatureRegistration> unprocessed) {
        for (FeatureRegistration registration : unprocessed) {
            if (processed.contains(registration)) {
                LOGGER.config(LocalizationMessages.FEATURE_HAS_ALREADY_BEEN_PROCESSED(registration.getFeatureClass()));
                continue;
            }

            Feature feature = registration.getFeature();
            if (feature == null) {
                feature = locator.create(registration.getFeatureClass());
            } else {
                locator.inject(feature);
            }

            if (enabledFeatures.contains(feature)) {
                LOGGER.config(LocalizationMessages.FEATURE_HAS_ALREADY_BEEN_PROCESSED(feature));
                continue;
            }

            boolean success = feature.configure(this);

            if (success) {
                processed.add(registration);

                configureFeatures(locator, processed, resetRegistrations());

                enabledFeatureClasses.add(registration.getFeatureClass());
                enabledFeatures.add(feature);
            }
        }
    }

    private List<FeatureRegistration> resetRegistrations() {
        List<FeatureRegistration> result = new ArrayList<FeatureRegistration>(newFeatureRegistrations);
        newFeatureRegistrations.clear();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommonConfig)) return false;

        CommonConfig that = (CommonConfig) o;

        if (type != that.type) return false;
        if (!properties.equals(that.properties)) return false;
        if (!componentBag.equals(that.componentBag)) return false;
        if (!enabledFeatureClasses.equals(that.enabledFeatureClasses)) return false;
        if (!enabledFeatures.equals(that.enabledFeatures)) return false;
        if (!newFeatureRegistrations.equals(that.newFeatureRegistrations)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + properties.hashCode();
        result = 31 * result + componentBag.hashCode();
        result = 31 * result + newFeatureRegistrations.hashCode();
        result = 31 * result + enabledFeatures.hashCode();
        result = 31 * result + enabledFeatureClasses.hashCode();
        return result;
    }
}
