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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.server.model.Resource;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A container for application resource models used during the {@link ApplicationHandler}
 * initialization.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class ResourceBag {
    /**
     * Resource bag builder.
     */
    public static final class Builder {

        private final Set<Class<?>> classes = Sets.newIdentityHashSet();
        private final Set<Object> instances = Sets.newIdentityHashSet();

        private final List<Resource> models = new LinkedList<Resource>();
        private final Map<String, Resource> rootResourceMap = new HashMap<String, Resource>();

        /**
         * Contract provider models.
         */
        private final Map<Class<?>, ContractProvider> contractProviders = Maps.newIdentityHashMap();

        /**
         * Register a new resource model created from a specific resource class.
         *
         * @param resourceClass introspected resource class.
         * @param resourceModel resource model for the class.
         * @param contractProvider contract provider for the resource.
         */
        void registerResource(Class<?> resourceClass, Resource resourceModel, ContractProvider contractProvider) {
            registerModel(resourceModel);
            registerContractProvider(resourceClass, contractProvider);
            classes.add(resourceClass);
        }

        private void registerContractProvider(final Class<?> resourceClass, final ContractProvider contractProvider) {
            if (contractProvider != null) {
                contractProviders.put(resourceClass, contractProvider);
            } else {
                contractProviders.put(resourceClass, ContractProvider.from(resourceClass));
            }
        }

        /**
         * Register a new resource model created from a specific resource instance.
         *
         * @param resourceInstance introspected resource instance.
         * @param resourceModel resource model for the instance.
         * @param contractProvider contract provider for the resource.
         */
        void registerResource(Object resourceInstance, Resource resourceModel, ContractProvider contractProvider) {
            registerModel(resourceModel);
            registerContractProvider(resourceInstance.getClass(), contractProvider);
            instances.add(resourceInstance);
        }

        /**
         * Register a new programmatically created resource model.
         *
         * @param resourceModel programmatically created resource model.
         */
        void registerProgrammaticResource(Resource resourceModel) {
            registerModel(resourceModel);
            classes.addAll(resourceModel.getHandlerClasses());
            instances.addAll(resourceModel.getHandlerInstances());
        }

        private void registerModel(Resource resourceModel) {
            final String path = resourceModel.getPath();
            if (resourceModel.isRootResource() && path != null) {
                Resource existing = rootResourceMap.get(path);
                if (existing != null) {
                    // merge resources
                    existing = Resource.builder(existing).mergeWith(resourceModel).build();
                    rootResourceMap.put(path, existing);
                } else {
                    rootResourceMap.put(path, resourceModel);
                }
            } else {
                models.add(resourceModel);
            }
        }

        /**
         * Build a resource bag.
         *
         * @return new resource bag initialized with the content of the resource bag builder.
         */
        ResourceBag build() {
            models.addAll(rootResourceMap.values());
            return new ResourceBag(classes, instances, models, contractProviders);
        }
    }

    /**
     * Resource handler classes for the models in this resource bag.
     */
    final Set<Class<?>> classes;
    /**
     * Resource handler instance for the models in this resource bag.
     */
    final Set<Object> instances;
    /**
     * Resource models.
     */
    final List<Resource> models;
    /**
     * Contract provider models.
     */
    final Map<Class<?>, ContractProvider> contractProviders;

    private ResourceBag(Set<Class<?>> classes, Set<Object> instances, List<Resource> models, Map<Class<?>,
            ContractProvider> contractProviders) {
        this.classes = classes;
        this.instances = instances;
        this.models = models;
        this.contractProviders = contractProviders;
    }


    /**
     * Returns list of root resources.
     * @return {@link Resource#isRoot Root} resources.
     */
    List<Resource> getRootResources() {
        List<Resource> rootResources = new ArrayList<Resource>();
        for (Resource resource : models) {
            if (resource.isRootResource()) {
                rootResources.add(resource);
            }
        }
        return rootResources;
    }
}
