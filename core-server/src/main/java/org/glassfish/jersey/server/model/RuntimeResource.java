/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.glassfish.jersey.uri.PathPattern;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Runtime resource is a group of {@link Resource resources} with the same {@link Resource#getPath() path}
 * regular expression. Runtime resource is constructed from {@link Resource resources} creating
 * the {@link ResourceModel resource model}.
 * <p/>
 * Runtime resource can have child runtime resources which are groups of child resources of all resources constructing this
 * runtime resource.
 * <p/>
 * The following example shows how Runtime resource structure is built from Resource model:
 * <pre>
 * &#064;Path("{foo}")
 * public class TemplateResourceFoo {
 *     &#064;GET
 *     &#064;Path("child")
 *     public String getFoo() {...}
 *
 *     &#064;Path("{x}")
 *     &#064;GET
 *     public String getX() {...}
 *
 *     &#064;Path("{y}")
 *     &#064;POST
 *     public String postY(String entity) {...}
 * }
 *
 * &#064;Path("{bar}")
 * public class TemplateResourceBar {
 *     &#064;Path("{z}")
 *     &#064;PUT
 *     public String putZ(String entity) {...}
 * }
 * </pre>
 *
 * Will be represented by RuntimeResources:
 * <table border="1">
 * <tr>
 * <th>line</th>
 * <th>RuntimeResource regex</th>
 * <th>Grouped Resources (paths)</th>
 * <th>Parent RuntimeResource (line)</th>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>"/([^/]+?)"</td>
 * <td>Resource("{foo}"), Resource("{bar}")</td>
 * <td>no parent</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>"child"</td>
 * <td>Child Resource("child")</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>"/([^/]+?)"</td>
 * <td>Child Resource("{x}"), Child Resource("{y}"), Child Resource("{z}")</td>
 * <td>1</td>
 * </tr>
 * </table>
 *
 * @author Miroslav Fuksa
 */
public class RuntimeResource implements ResourceModelComponent {
    /**
     * Runtime Resource builder.
     */
    static class Builder {
        private final List<Resource> resources;
        private final String regex;
        private final List<RuntimeResource.Builder> childRuntimeResourceBuilders;


        /**
         * Create new {@link RuntimeResource runtime resource} builder instance.
         *
         * @param resources                    List of resources with same regex that creates a RuntimeResource.
         * @param childRuntimeResourceBuilders List of builders of child runtime resources that belong runtime resource.
         * @param regex                        Path regular expression.
         */
        public Builder(List<Resource> resources, List<Builder> childRuntimeResourceBuilders, String regex) {
            this.childRuntimeResourceBuilders = childRuntimeResourceBuilders;
            this.resources = resources;
            this.regex = regex;
        }


        /**
         * Build new RuntimeResource from this builder.
         *
         * @param parent Parent runtime resource.
         * @return New RuntimeResource instance.
         */
        public RuntimeResource build(RuntimeResource parent) {
            return new RuntimeResource(resources, childRuntimeResourceBuilders, parent, regex);
        }
    }

    /**
     * Comparator of RuntimeResources based on rules respecting resource matching algorithm.
     */
    public static final Comparator<RuntimeResource> COMPARATOR = new Comparator<RuntimeResource>() {
        @Override
        public int compare(RuntimeResource o1, RuntimeResource o2) {
            return PathPattern.COMPARATOR.compare(o1.getPathPattern(), o2.getPathPattern());
        }
    };


    private final String regex;
    private final List<ResourceMethod> resourceMethods;
    private final List<ResourceMethod> resourceLocators;
    private final List<RuntimeResource> childRuntimeResources;
    private final List<Resource> resources;

    private final RuntimeResource parent;
    private final PathPattern pathPattern;


    private RuntimeResource(List<Resource> resources,
                            List<Builder> childRuntimeResourceBuilders,
                            RuntimeResource parent,
                            String regex) {
        this.parent = parent;
        this.pathPattern = resources.get(0).getPathPattern();

        this.resources = Lists.newArrayList(resources);

        this.regex = regex;
        this.resourceMethods = Lists.newArrayList();
        this.resourceLocators = Lists.newArrayList();
        this.childRuntimeResources = Lists.newArrayList();
        for (Builder childRuntimeResourceBuilder : childRuntimeResourceBuilders) {
            this.childRuntimeResources.add(childRuntimeResourceBuilder.build(this));
        }
        Collections.sort(this.childRuntimeResources, COMPARATOR);

        for (final Resource res : this.resources) {
            this.resourceMethods.addAll(res.getResourceMethods());

            final ResourceMethod resourceLocator = res.getResourceLocator();
            if (resourceLocator != null) {
                this.resourceLocators.add(resourceLocator);
            }
        }
    }

    /**
     * Get child runtime resources of this resource.
     *
     * @return List of child runtime resource.
     */
    public List<RuntimeResource> getChildRuntimeResources() {
        return childRuntimeResources;
    }

    /**
     * Get regular expression of path pattern of this runtime resource.
     *
     * @return Matching regular expression.
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Get resource methods (excluding resource locators) of all {@link Resource resources} of this runtime resource.
     *
     * @return List of resource methods.
     */
    public List<ResourceMethod> getResourceMethods() {
        return resourceMethods;
    }

    /**
     * Get resource locators of all {@link Resource resources} of this runtime resource.
     * <p/>
     * Note that valid RuntimeResource should have only one resource locator. This method is used for validation purposes.
     *
     * @return List of resource locators.
     */
    public List<ResourceMethod> getResourceLocators() {
        return resourceLocators;
    }

    /**
     * Return the resource locator of this resource.
     *
     * @return Resource locator of this runtime resource.
     */
    public ResourceMethod getResourceLocator() {
        if (resourceLocators.size() >= 1) {
            return resourceLocators.get(0);
        } else {
            return null;
        }
    }

    /**
     * Get parent of this runtime resource.
     *
     * @return Parent runtime resource if this runtime resource is a child resource, null otherwise.
     */
    public RuntimeResource getParent() {
        return parent;
    }

    /**
     * Get path pattern for matching purposes.
     *
     * @return Path pattern.
     */
    public PathPattern getPathPattern() {
        return pathPattern;
    }

    /**
     * Get full regular expression of this runtime resource prefixed by regular expression of parent if present.
     *
     * @return Full resource regular expression.
     */
    public String getFullPathRegex() {
        if (parent == null) {
            return regex;
        } else {
            return parent.getRegex() + regex;
        }
    }

    /**
     * Return parent {@link Resource resources} of {@link Resource resources} from this runtime resource. The returned list
     * is ordered so that the position of the parent resource in the returned list is the same as position of its child resource
     * in list returned by {@link #getResources()}. Simply said the order of lists returned
     * from {@code getParentResources()} and {@link #getResources()} from parent-child point of view is the same. If the resource
     * has no parent then the element {@code null} is in the list.
     *
     * @return Parent resource list with resources if this runtime resource is child resource or {@code null} elements if
     *         this runtime resource is the parent resource.
     */
    public List<Resource> getParentResources() {
        return Lists.transform(resources, new Function<Resource, Resource>() {
            @Override
            public Resource apply(Resource child) {
                return (child == null) ? null : child.getParent();
            }
        });
    }

    /**
     * Get resources creating this runtime resource.
     *
     * @return List of resources with same path regular expression which this resource is based on.
     */
    public List<Resource> getResources() {
        return resources;
    }

    @Override
    public void accept(ResourceModelVisitor visitor) {
        visitor.visitRuntimeResource(this);
    }

    @Override
    public List<? extends ResourceModelComponent> getComponents() {
        return getChildRuntimeResources();
    }
}

