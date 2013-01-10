/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.uri.PathPattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * Resource model of the deployed application which contains set of root resources. As it implements {@link
 * ResourceModelComponent} it can be validated by {@link ComponentModelValidator component model validator} which will perform
 * validation of the entire resource model including all sub components ({@link Resource resources},
 * {@link ResourceMethod resource methods} ...).
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class ResourceModel implements ResourceModelComponent {

    /**
     * Builder used to create {@link ResourceModel resource model} instances.
     */
    public static class Builder {
        private final List<Resource> resources;

        /**
         * Create new builder with empty resources.
         */
        public Builder() {
            this.resources = Lists.newArrayList();

        }

        /**
         * Create new builder pre initialized with {@code resource}.
         * @param resources Resources (root and non root resources).
         */
        public Builder(List<Resource> resources) {
            this.resources = resources;
        }

        /**
         * Add a resource to the builder.
         * @param resource Resource to be added to the builder (root or non root resource).
         * @return Current builder.
         */
        public Builder addResource(Resource resource) {
            this.resources.add(resource);
            return this;
        }

        /**
         * Build the {@link ResourceModel resource model}. Resources with the same path are merged.
         * @return Resource model.
         */
        public ResourceModel build() {
            List<Resource> allResource = Lists.newArrayList();

            Map<String, Resource> rootResourceMap = Maps.newLinkedHashMap();
            for (Resource resource : resources) {
                final String path = resource.getPath();

                if (resource.isRootResource()) {
                    final Resource fromMap = rootResourceMap.get(path);
                    if (fromMap == null) {
                        rootResourceMap.put(path, resource);
                    } else {
                        rootResourceMap.put(path, Resource.builder(fromMap).mergeWith(resource).build());
                    }
                } else {
                    allResource.add(resource);
                }
            }
            final List<Resource> rootResources = Lists.newArrayList(rootResourceMap.values());
            allResource.addAll(rootResources);
            return new ResourceModel(rootResources, resources);
        }
    }

    private final List<Resource> rootResources;
    private final List<Resource> resources;

    private final Map<PathPattern, List<Resource>> pathPatternChildResourceMap;


    /**
     * Creates new instance from root resources.
     * @param resources Root resource of the resource model.
     */
    private ResourceModel(List<Resource> rootResources, List<Resource> resources) {
        this.resources = resources;
        this.rootResources = rootResources;
        this.pathPatternChildResourceMap = Resource.groupResourcesByPathPattern(this.rootResources);
    }


    /**
     * Return map with root resources grouped by path pattern.
     * @return Map where key is the resource {@link PathPattern path pattern}
     * ({@link org.glassfish.jersey.server.model.Resource#getPathPattern()}) and value is list of resource
     * having this path pattern.
     */
    public Map<PathPattern, List<Resource>> getPathPatternToResources() {
        return pathPatternChildResourceMap;
    }

    /**
     * Return root resources from this {@link ResourceModel resource model}.
     * @return List of root resources.
     */
    public List<Resource> getRootResources() {
        return rootResources;
    }

    /**
     * Return all resources from this {@link ResourceModel resource model}.
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
        return resources;
    }
}