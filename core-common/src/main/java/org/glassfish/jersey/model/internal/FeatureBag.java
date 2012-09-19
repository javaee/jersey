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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A container for features to be enabled used during the server or client
 * initialization.
 *
 * <li>Stores the order in which all registered features should be enabled.</li>
 * <li>Knows about already enabled features.</li>
 * <li>Feature instances added to this bag takes precedence over added feature classes.</li>
 *
 * <p>
 * Note: Features are configured immediately before binding time of all providers and resources (on server).
 * </p>
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class FeatureBag {

    public static final class RegisteredFeature {

        private final Class<? extends Feature> featureClass;
        private final Feature feature;

        public RegisteredFeature(final Class<? extends Feature> featureClass, final Feature feature) {
            this.featureClass = featureClass;
            this.feature = feature;
        }

        public Class<? extends Feature> getFeatureClass() {
            return featureClass;
        }

        public Feature getFeature() {
            return feature;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof RegisteredFeature)) {
                return false;
            }
            RegisteredFeature other = (RegisteredFeature) obj;

            return this == other
                    || (featureClass == other.featureClass)
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
     * Feature bag builder.
     */
    public static final class Builder {

        private final List<RegisteredFeature> registered = Lists.newLinkedList();
        private final Set<RegisteredFeature> enabled = Sets.newIdentityHashSet();

        private final MultivaluedMap<Class<? extends Feature>, RegisteredFeature> registeredFeatures =
                new MultivaluedHashMap<Class<? extends Feature>, RegisteredFeature>();


        /**
         * Creates a new {@code FeatureBag} instance.
         */
        public Builder() {
        }

        /**
         * Copy constructor.
         *
         * @param other instance of {@code FeatureBag.Builder} to copy data from.
         */
        public Builder(final Builder other) {
            this.registered.addAll(other.registered);
            this.registeredFeatures.putAll(other.registeredFeatures);
            this.enabled.addAll(other.enabled);
        }

        /**
         * Copy constructor.
         *
         * @param other instance of {@code FeatureBag.Builder} to copy data from.
         */
        public Builder(final FeatureBag other) {
            this.registered.addAll(other.registered);

            for (final RegisteredFeature registeredFeature : other.registered) {
                this.registeredFeatures.add(registeredFeature.featureClass != null ? registeredFeature.featureClass :
                        registeredFeature.feature.getClass(), registeredFeature);
            }

            this.enabled.addAll(other.enabled);
        }

        /**
         * Adds a feature class to the list of features.
         *
         * @param featureClass feature class to be enabled.
         * @return {@code true} if the feature has been successfully added to the list of features,
         * {@code false} if a feature (class/instance) of this class is already present in this bag.
         */
        public boolean add(final Class<? extends Feature> featureClass) {
            if (!registeredFeatures.containsKey(featureClass)) {
                final RegisteredFeature featureClassToRegister = new RegisteredFeature(featureClass, null);
                registeredFeatures.add(featureClass, featureClassToRegister);
                registered.add(featureClassToRegister);

                return true;
            }

            return false;
        }

        /**
         * Adds a feature instance to the list of features.
         *
         * @param feature feature instance to be enabled.
         * @return {@code true} if the feature class has been successfully added to the list of features,
         * {@code false} if a feature instance is already present in this bag.
         */
        public boolean add(final Feature feature) {
            final Class<? extends Feature> featureClass = feature.getClass();
            final RegisteredFeature featureClassToRegister = new RegisteredFeature(featureClass, null);

            // Remove class?
            final List<RegisteredFeature> registeredFeatures = this.registeredFeatures.get(featureClass);
            if (registeredFeatures != null
                    && registeredFeatures.contains(featureClassToRegister)) {
                registeredFeatures.remove(featureClassToRegister);
                this.registered.remove(featureClassToRegister);
            }

            // Add instance.
            final RegisteredFeature featureToRegister = new RegisteredFeature(null, feature);
            this.registeredFeatures.add(feature.getClass(), featureToRegister);
            this.registered.add(featureToRegister);

            return true;
        }

        /**
         * Build a feature bag.
         *
         * @return new feature bag initialized with the content of the feature bag builder.
         */
        public FeatureBag build() {
            return new FeatureBag(registered, enabled);
        }
    }

    private final List<RegisteredFeature> registered;
    private final Set<RegisteredFeature> enabled = Sets.newIdentityHashSet();

    private FeatureBag(final List<RegisteredFeature> registered, final Set<RegisteredFeature> enabled) {
        this.registered = Collections.unmodifiableList(registered);
        this.enabled.addAll(enabled);
    }

    /**
     * Returns an unmodifiable list of un-configured features in order they were added.
     *
     * @return a non-null list of features.
     */
    public List<RegisteredFeature> getUnconfiguredFeatures() {
        final LinkedList<RegisteredFeature> registeredFeatures = Lists.newLinkedList(registered);
        for (final RegisteredFeature feature : enabled) {
            registeredFeatures.remove(feature);
        }

        return registeredFeatures;
    }

    /**
     * Returns a set of already enabled features.
     *
     * @return set of already enabled features.
     */
    public Set<Feature> getEnabledFeatures() {
        final Set<Feature> enabledFeatures = Sets.newIdentityHashSet();

        for (final RegisteredFeature feature : enabled) {
            if (feature.feature != null) {
                enabledFeatures.add(feature.feature);
            }
        }

        return Collections.unmodifiableSet(enabledFeatures);
    }

    /**
     * Indicates whether any feature of given {@code featureClass} is already enabled or not.
     *
     * @param featureClass feature class to check.
     * @return {@code true} if a feature of given class has been already enabled, {@code false} otherwise.
     */
    public boolean isEnabled(final Class<? extends Feature> featureClass) {
        return enabled.contains(new RegisteredFeature(featureClass, null));
    }

    /**
     * Indicates whether the given feature is already enabled or not.
     *
     * @param feature feature to check.
     * @return {@code true} if the feature has been already enabled, {@code false} otherwise.
     */
    public boolean isEnabled(final Feature feature) {
        return enabled.contains(new RegisteredFeature(null, feature));
    }

    public void setEnabled(final Feature feature) {
        enabled.add(new RegisteredFeature(feature.getClass(), null));
        enabled.add(new RegisteredFeature(null, feature));
    }

    public boolean isRegistered(final RegisteredFeature registeredFeature) {
        return registered.contains(registeredFeature);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof FeatureBag)) {
            return false;
        }
        FeatureBag other = (FeatureBag) obj;

        return this == other
                && (registered == other.registered || registered.equals(other.registered));
    }

    @Override
    public int hashCode() {
        int hash = 53;
        hash = 7 * hash + registered.hashCode();
        return hash;
    }
}
