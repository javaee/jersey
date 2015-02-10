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

package org.glassfish.jersey.server.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;


/**
 * Resource model of the deployed application which contains set of root resources. As it implements {@link
 * ResourceModelComponent} it can be validated by {@link ComponentModelValidator component model validator} which will perform
 * validation of the entire resource model including all sub components ({@link Resource resources},
 * {@link ResourceMethod resource methods} ...).
 *
 * @author Miroslav Fuksa
 */
public class ResourceModel implements ResourceModelComponent {

    /**
     * Builder used to create {@link ResourceModel resource model} instances.
     */
    public static class Builder {
        private final List<Resource> resources;
        private final boolean subResourceModel;


        /**
         * Create new builder pre initialized with {@code resourceModel}.
         *
         * @param resourceModel    Resource model.
         * @param subResourceModel {@code true} if resource model created by this builder will be sub resource model,
         *                         {@code false} if it is a application root resource model.
         */
        public Builder(ResourceModel resourceModel, boolean subResourceModel) {
            this.resources = resourceModel.getResources();
            this.subResourceModel = subResourceModel;
        }

        /**
         * Create new builder pre initialized with {@code resource}.
         *
         * @param resources        Resources (root and non root).
         * @param subResourceModel {@code true} if resource model created by this builder will be sub resource model,
         *                         {@code false} if it is a application root resource model.
         */
        public Builder(List<Resource> resources, boolean subResourceModel) {
            this.resources = resources;
            this.subResourceModel = subResourceModel;
        }

        /**
         * Create new builder with empty resources.
         *
         * @param subResourceModel {@code true} if resource model created by this builder will be sub resource model,
         *                         {@code false} if it is a application root resource model.
         */
        public Builder(boolean subResourceModel) {
            this.resources = Lists.newArrayList();
            this.subResourceModel = subResourceModel;
        }


        /**
         * Add a resource to the builder.
         *
         * @param resource Resource to be added to the builder (root or non root resource).
         * @return Current builder.
         */
        public Builder addResource(Resource resource) {
            this.resources.add(resource);
            return this;
        }

        /**
         * Build the {@link ResourceModel resource model}. Resources with the same path are merged.
         *
         * @return Resource model.
         */
        public ResourceModel build() {
            Map<String, Resource> resourceMap = Maps.newLinkedHashMap();
            final Set<Resource> separateResources = Sets.newIdentityHashSet(); // resource with no path that should not be merged

            for (Resource resource : resources) {
                final String path = resource.getPath();
                if (path == null && !subResourceModel) {
                    separateResources.add(resource);
                } else {
                    final Resource fromMap = resourceMap.get(path);
                    if (fromMap == null) {
                        resourceMap.put(path, resource);
                    } else {
                        resourceMap.put(path, Resource.builder(fromMap).mergeWith(resource).build());
                    }
                }
            }
            List<Resource> rootResources = Lists.newArrayList();
            List<Resource> allResources = Lists.newArrayList();

            for (Map.Entry<String, Resource> entry : resourceMap.entrySet()) {
                if (entry.getKey() != null) {
                    rootResources.add(entry.getValue());
                }
                allResources.add(entry.getValue());
            }
            if (!subResourceModel) {
                allResources.addAll(separateResources);
            }

            return new ResourceModel(rootResources, allResources);
        }
    }

    private final List<Resource> rootResources;
    private final List<Resource> resources;

    private final Value<RuntimeResourceModel> runtimeRootResourceModelValue;

    /**
     * Creates new instance from root allResources.
     *
     * @param allResources Root resource of the resource model.
     */
    private ResourceModel(List<Resource> rootResources, List<Resource> allResources) {
        this.resources = allResources;
        this.rootResources = rootResources;
        this.runtimeRootResourceModelValue = Values.lazy(new Value<RuntimeResourceModel>() {
            @Override
            public RuntimeResourceModel get() {
                return new RuntimeResourceModel(ResourceModel.this.resources);
            }
        });
    }


    /**
     * Return root resources from this resource model.
     *
     * @return List of root resources.
     */
    public List<Resource> getRootResources() {
        return rootResources;
    }

    /**
     * Return all resources from this resource model.
     *
     * @return List of all resources (root and non root resources).
     */
    public List<Resource> getResources() {
        return resources;
    }

    @Override
    public void accept(ResourceModelVisitor visitor) {
        visitor.visitResourceModel(this);
    }

    @Override
    public List<? extends ResourceModelComponent> getComponents() {
        List<ResourceModelComponent> components = Lists.newArrayList();

        components.addAll(resources);
        components.addAll(getRuntimeResourceModel().getRuntimeResources());
        return components;
    }

    /**
     * Return {@link RuntimeResourceModel runtime resource model} based on this this resource model.
     *
     * @return Runtime resource model created from this resource model.
     */
    public RuntimeResourceModel getRuntimeResourceModel() {
        return runtimeRootResourceModelValue.get();
    }
}
