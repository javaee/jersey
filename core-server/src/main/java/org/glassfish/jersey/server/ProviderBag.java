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

package org.glassfish.jersey.server;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.model.ContractProvider;

import com.google.common.collect.Sets;

/**
 * A container for contract provider models used during the {@link org.glassfish.jersey.server.ApplicationHandler}
 * initialization.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class ProviderBag {
    /**
     * Provider bag builder.
     */
    final static class Builder {

        private final Set<Class<?>> classes = Sets.newIdentityHashSet();
        private final Set<Object> instances = Sets.newIdentityHashSet();
        private final Map<Class<?>, ContractProvider> models = new IdentityHashMap<Class<?>, ContractProvider>();

        /**
         * Register a class as a contract provider.
         *
         * @param providerClass class to be introspected as a contract provider and registered.
         * @param isResource if {@code true} the class is a treated as a resource class, i.e. it
         *                   will not be added to the set of provider classes.
         * @return {@code true} if the class was successfully registered as provider.
         */
        boolean register(final Class<?> providerClass, final boolean isResource) {
            if (!models.containsKey(providerClass)) {
                final ContractProvider model = ContractProvider.from(providerClass);
                if (model == null) {
                    return false;
                }
                models.put(providerClass, model);
            }
            return isResource || classes.add(providerClass);
        }

        /**
         * Register an instance as a contract provider.
         *
         * @param provider instance to be introspected as a contract provider and registered.
         * @param isResource if {@code true} the instance is a treated as a resource class, i.e. it
         *                   will not be added to the set of provider instance.
         * @return {@code true} if the instance was successfully registered as provider.
         */
        boolean register(final Object provider, final boolean isResource) {
            final Class<?> providerClass = provider.getClass();
            if (!models.containsKey(providerClass)) {
                final ContractProvider model = ContractProvider.from(provider);
                if (model == null) {
                    return false;
                }
                models.put(providerClass, model);
            }
            return isResource || instances.add(provider);
        }

        /**
         * Build a resource bag.
         *
         * @return new resource bag initialized with the content of the resource bag builder.
         */
        ProviderBag build() {
            return new ProviderBag(classes, instances, models);
        }
    }

    /**
     * Contract provider classes.
     */
    final Set<Class<?>> classes;
    /**
     * Contract provider instances.
     */
    final Set<Object> instances;
    /**
     * Contract provider models.
     */
    final Map<Class<?>, ContractProvider> models;

    private ProviderBag(Set<Class<?>> classes, Set<Object> instances, Map<Class<?>, ContractProvider> models) {
        this.classes = classes;
        this.instances = instances;
        this.models = models;
    }
}
