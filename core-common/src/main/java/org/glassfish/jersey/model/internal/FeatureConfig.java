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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.ContractProvider;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class FeatureConfig implements Configurable {

    private final Configurable delegate;
    private final List<FeatureBag.RegisteredFeature> unprocessedRegisteredFeatures = Lists.newLinkedList();

    public FeatureConfig(final Configurable delegate) {
        this.delegate = delegate;
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public Object getProperty(final String name) {
        return delegate.getProperty(name);
    }

    @Override
    public Configurable setProperties(final Map<String, ?> properties) {
        return delegate.setProperties(properties);
    }

    @Override
    public Configurable setProperty(final String name, final Object value) {
        return delegate.setProperty(name, value);
    }

    @Override
    public Collection<Feature> getFeatures() {
        return delegate.getFeatures();
    }

    @Override
    public Set<Class<?>> getProviderClasses() {
        return delegate.getProviderClasses();
    }

    @Override
    public Set<Object> getProviderInstances() {
        return delegate.getProviderInstances();
    }

    @Override
    public Configurable register(final Class<?> providerClass) {
        return register(providerClass, ContractProvider.NO_PRIORITY, Providers.getProviderContracts(providerClass));
    }

    @Override
    public Configurable register(final Class<?> providerClass, final int bindingPriority) {
        return register(providerClass, bindingPriority, Providers.getProviderContracts(providerClass));
    }

    @Override
    public <T> Configurable register(final Class<T> providerClass, final Class<? super T>... contracts) {
        return register(providerClass, ContractProvider.NO_PRIORITY, Sets.newLinkedHashSet(Arrays.<Class<?>>asList(contracts)));
    }

    @Override
    public <T> Configurable register(final Class<T> providerClass, final int bindingPriority,
                                     final Class<? super T>... contracts) {
        return register(providerClass, bindingPriority, Sets.newLinkedHashSet(Arrays.<Class<?>>asList(contracts)));
    }

    @Override
    public Configurable register(final Object provider) {
        return register(provider, ContractProvider.NO_PRIORITY, Providers.getProviderContracts(provider.getClass()));
    }

    @Override
    public Configurable register(final Object provider, final int bindingPriority) {
        return register(provider, bindingPriority, Providers.getProviderContracts(provider.getClass()));
    }

    @Override
    public <T> Configurable register(final Object provider, final Class<? super T>... contracts) {
        return register(provider, ContractProvider.NO_PRIORITY, Sets.newLinkedHashSet(Arrays.<Class<?>>asList(contracts)));
    }

    @Override
    public <T> Configurable register(final Object provider, final int bindingPriority, final Class<? super T>... contracts) {
        return register(provider, bindingPriority, Sets.newLinkedHashSet(Arrays.<Class<?>>asList(contracts)));
    }

    @SuppressWarnings("unchecked")
    private Configurable register(final Object provider, final int bindingPriority, Set<Class<?>> contracts) {
        if (contracts.contains(Feature.class)) {
            if (provider instanceof Feature) {
                unprocessedRegisteredFeatures.add(new FeatureBag.RegisteredFeature(null, (Feature) provider));
            }
            contracts.remove(Feature.class);

            if (contracts.isEmpty()) {
                return this;
            }
        }
        return delegate.register(provider, bindingPriority, contracts.toArray(new Class[contracts.size()]));
    }

    @SuppressWarnings("unchecked")
    private <T> Configurable register(final Class<T> providerClass, final int bindingPriority, Set<Class<?>> contracts) {
        if (contracts.contains(Feature.class)) {
            if (Feature.class.isAssignableFrom(providerClass)) {
                unprocessedRegisteredFeatures.add(new FeatureBag.RegisteredFeature((Class<? extends Feature>)providerClass, null));
            }
            contracts.remove(Feature.class);

            if (contracts.isEmpty()) {
                return this;
            }
        }
        return delegate.register(providerClass, bindingPriority, contracts.toArray(new Class[contracts.size()]));
    }

    public Collection<FeatureBag.RegisteredFeature> getUnprocessedFeatures() {
        return unprocessedRegisteredFeatures;
    }
}
