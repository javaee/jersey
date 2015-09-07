/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;

import org.glassfish.jersey.Severity;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.internal.ModelHelper;
import org.glassfish.jersey.uri.PathPattern;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Model of a single resource component.
 * <p>
 * Resource component model represents a collection of {@link ResourceMethod methods}
 * grouped under the same parent request path template. {@code Resource} class is also
 * the main entry point to the programmatic resource modeling API that provides ability
 * to programmatically extend the existing JAX-RS annotated resource classes or build
 * new resource models that may be utilized by Jersey runtime.
 * </p>
 * <p>
 * For example:
 * <pre>
 * &#64;Path("hello")
 * public class HelloResource {
 *      &#64;GET
 *      &#64;Produces("text/plain")
 *      public String sayHello() {
 *          return "Hello!";
 *      }
 * }
 *
 * ...
 *
 * // Register the annotated resource.
 * ResourceConfig resourceConfig = new ResourceConfig(HelloResource.class);
 *
 * // Add new "hello2" resource using the annotated resource class
 * // and overriding the resource path.
 * Resource.Builder resourceBuilder =
 *         Resource.builder(HelloResource.class, new LinkedList&lt;ResourceModelIssue&gt;())
 *         .path("hello2");
 *
 * // Add a new (virtual) sub-resource method to the "hello2" resource.
 * resourceBuilder.addChildResource("world").addMethod("GET")
 *         .produces("text/plain")
 *         .handledBy(new Inflector&lt;Request, String&gt;() {
 *                 &#64;Override
 *                 public String apply(Request request) {
 *                     return "Hello World!";
 *                 }
 *         });
 *
 * // Register the new programmatic resource in the application's configuration.
 * resourceConfig.registerResources(resourceBuilder.build());
 * </pre>
 * The following table illustrates the supported requests and provided responses
 * for the application configured in the example above.
 * <table>
 * <tr>
 * <th>Request</th><th>Response</th><th>Method invoked</th>
 * </tr>
 * <tr>
 * <td>{@code "GET /hello"}</td><td>{@code "Hello!"}</td><td>{@code HelloResource.sayHello()}</td>
 * </tr>
 * <tr>
 * <td>{@code "GET /hello2"}</td><td>{@code "Hello!"}</td><td>{@code HelloResource.sayHello()}</td>
 * </tr>
 * <tr>
 * <td>{@code "GET /hello2/world"}</td><td>{@code "Hello World!"}</td><td>{@code Inflector.apply()}</td>
 * </tr>
 * </table>
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
public final class Resource implements Routed, ResourceModelComponent {

    /**
     * Immutable resource data holder.
     */
    private static class Data {

        private final List<String> names;
        private final String path;
        private final PathPattern pathPattern;

        private final List<ResourceMethod.Data> resourceMethods;
        private final ResourceMethod.Data subResourceLocator;
        private final List<Resource.Data> childResources;

        private final Set<Class<?>> handlerClasses;
        private final Set<Object> handlerInstances;

        private final boolean extended;

        /**
         * Create a new immutable resource data holder from the supplied parameters.
         *
         * @param names            resource names.
         * @param path             resource path.
         * @param resourceMethods  child resource methods.
         * @param subResourceLocator          child resource locator.
         * @param childResources   child sub-resources.
         * @param handlerClasses   handler classes handling the resource methods.
         * @param handlerInstances handler instances handling the resource methods.
         * @param extended         flag indicating whether the resource is extended
         */
        private Data(
                final List<String> names,
                final String path,
                final List<ResourceMethod.Data> resourceMethods,
                final ResourceMethod.Data subResourceLocator,
                final List<Data> childResources,
                final Set<Class<?>> handlerClasses,
                final Set<Object> handlerInstances,
                boolean extended) {

            this.extended = extended;

            this.names = Resource.immutableCopy(names);
            this.path = path;
            this.pathPattern = (path == null || path.isEmpty())
                    ? PathPattern.OPEN_ROOT_PATH_PATTERN
                    : new PathPattern(path, PathPattern.RightHandPath.capturingZeroOrMoreSegments);
            this.resourceMethods = Resource.immutableCopy(resourceMethods);
            this.subResourceLocator = subResourceLocator;
            this.childResources = Collections.unmodifiableList(childResources); // no need to deep-copy the list

            this.handlerClasses = Resource.immutableCopy(handlerClasses);
            this.handlerInstances = Resource.immutableCopy(handlerInstances);
        }

        @Override
        public String toString() {
            return "Resource{"
                    + ((path == null) ? "[unbound], " : "\"" + path + "\", ")
                    + childResources.size() + " child resources, "
                    + resourceMethods.size() + " resource methods, "
                    + (subResourceLocator == null ? "0" : "1") + " sub-resource locator, "
                    + handlerClasses.size() + " method handler classes, "
                    + handlerInstances.size() + " method handler instances"
                    + '}';
        }
    }

    /**
     * Resource model component builder.
     */
    public static final class Builder {

        private List<String> names;
        private String path;

        private final Set<ResourceMethod.Builder> methodBuilders;
        private final Set<Resource.Builder> childResourceBuilders;
        private final List<Resource.Data> childResources;

        private final List<ResourceMethod.Data> resourceMethods;
        private ResourceMethod.Data subResourceLocator;

        private final Set<Class<?>> handlerClasses;
        private final Set<Object> handlerInstances;

        private final Resource.Builder parentResource;

        private boolean extended;

        private Builder(final Resource.Builder parentResource) {
            this.methodBuilders = Sets.newLinkedHashSet();
            this.childResourceBuilders = Sets.newLinkedHashSet();
            this.childResources = Lists.newLinkedList();
            this.resourceMethods = Lists.newLinkedList();
            this.handlerClasses = Sets.newIdentityHashSet();
            this.handlerInstances = Sets.newIdentityHashSet();
            this.parentResource = parentResource;

            name("[unnamed]");
        }

        private Builder(final String path) {
            this((Resource.Builder) null);
            path(path);
        }

        private Builder(final String path, final Resource.Builder parentResource) {
            this(parentResource);
            this.path = path;
        }

        private Builder() {
            this((Resource.Builder) null);
        }

        private boolean isEmpty() {
            return this.path == null
                    && methodBuilders.isEmpty()
                    && childResourceBuilders.isEmpty()
                    && resourceMethods.isEmpty()
                    && childResources.isEmpty()
                    && subResourceLocator == null;
        }

        /**
         * Define a new name of the built resource.
         * <p/>
         * The resource model name is typically used for reporting
         * purposes (e.g. validation etc.).
         *
         * @param name new name of the resource.
         * @return updated builder object.
         * @see org.glassfish.jersey.server.model.Resource#getName()
         */
        public Builder name(final String name) {
            this.names = Lists.newArrayList(name);
            return this;
        }

        /**
         * Define a new path for the built resource.
         * <p/>
         * NOTE: Invoking this method marks a resource as a root resource.
         *
         * @param path new path for the resource.
         * @return updated builder object.
         */
        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        /**
         * Add a new method model to the resource for processing requests of
         * the specified HTTP method.
         * <p/>
         * The returned builder is automatically bound to the the resource. It is
         * not necessary to invoke the {@link ResourceMethod.Builder#build() build()}
         * method on the method builder after setting all the data. This will be
         * done automatically when the resource is built.
         *
         * @param httpMethod HTTP method that will be processed by the method.
         * @return a new resource method builder.
         */
        public ResourceMethod.Builder addMethod(final String httpMethod) {
            ResourceMethod.Builder builder = new ResourceMethod.Builder(this);
            methodBuilders.add(builder);
            return builder.httpMethod(httpMethod);
        }

        /**
         * Add a new arbitrary method model to the resource.
         * <p/>
         * The returned builder is automatically bound to the the resource. It is
         * not necessary to invoke the {@link ResourceMethod.Builder#build() build()}
         * method on the method builder after setting all the data. This will be
         * done automatically when the resource is built.
         *
         * @return a new resource method builder.
         */
        public ResourceMethod.Builder addMethod() {
            ResourceMethod.Builder builder = new ResourceMethod.Builder(this);
            methodBuilders.add(builder);
            return builder;
        }

        /**
         * Add a new method model that is a copy of the given {@code resourceMethod}.
         * <p/>
         * The returned builder is automatically bound to the the resource. It is
         * not necessary to invoke the {@link ResourceMethod.Builder#build() build()}
         * method on the method builder after setting all the data. This will be
         * done automatically when the resource is built.
         *
         * @param resourceMethod The resource method based on which the new method builder
         *                       should be created.
         * @return a new resource method builder.
         */
        public ResourceMethod.Builder addMethod(ResourceMethod resourceMethod) {
            ResourceMethod.Builder builder = new ResourceMethod.Builder(this, resourceMethod);
            methodBuilders.add(builder);
            return builder;
        }

        /**
         * Get a method builder for an existing resource method.
         * <p/>
         * The original method is removed from the resource and a builder based on it is added.
         *
         * @param resourceMethod The resource method to be replaced by a builder.
         * @return a new resource method builder.
         * @throws java.lang.IllegalArgumentException in case the method to be updated is not registered
         *                                            in this resource builder.
         * @since 2.18
         */
        public ResourceMethod.Builder updateMethod(ResourceMethod resourceMethod) {
            final boolean removed = resourceMethods.remove(resourceMethod.getData());
            if (!removed) {
                throw new IllegalArgumentException(
                        LocalizationMessages.RESOURCE_UPDATED_METHOD_DOES_NOT_EXIST(resourceMethod.toString()));
            }
            ResourceMethod.Builder builder = new ResourceMethod.Builder(this, resourceMethod);
            methodBuilders.add(builder);
            return builder;
        }

        /**
         * Add a new child resource to the resource.
         * <p/>
         * The returned builder is automatically bound to the the resource. It is
         * not necessary to invoke the {@link Resource.Builder#build() build()}
         * method on the resource builder after setting all the data. This will be
         * done automatically when the resource is built.
         *
         * @param relativePath The path of the new child resource relative to this resource.
         * @return child resource builder.
         * @throws java.lang.IllegalStateException in case the child resource for this path exists already.
         */
        public Builder addChildResource(String relativePath) {
            if (this.parentResource != null) {
                throw new IllegalStateException(LocalizationMessages.RESOURCE_ADD_CHILD_ALREADY_CHILD());
            }
            final Builder resourceBuilder = new Builder(relativePath, this);

            childResourceBuilders.add(resourceBuilder);
            return resourceBuilder;
        }

        /**
         * Add an existing Resource as a child resource of current resource.
         *
         * @param resource Resource to be added as child resource.
         */
        public void addChildResource(Resource resource) {
            this.childResources.add(resource.data);
        }

        /**
         * Replace an existing child resource with a newly supplied one.
         *
         * @param replacedResource old resource to be replaced.
         * @param newResource      new resource to add.
         * @throws java.lang.IllegalArgumentException in case there is no such child resource to be replaced.
         * @since 2.18
         */
        public void replaceChildResource(Resource replacedResource, Resource newResource) {
            final boolean removed = this.childResources.remove(replacedResource.data);
            if (!removed) {
                throw new IllegalArgumentException(
                        LocalizationMessages.RESOURCE_REPLACED_CHILD_DOES_NOT_EXIST(replacedResource.toString()));
            }
            addChildResource(newResource);
        }

        /**
         * Merge methods from a given resource model into this resource model builder.
         *
         * @param resource to be merged into this resource model builder.
         * @return updated builder object.
         */
        public Builder mergeWith(final Resource resource) {
            mergeWith(resource.data);
            return this;
        }

        /**
         * Set the flag indicating whether the resource is extended or is a core of exposed RESTful API.
         * <p/>
         * The method defines the flag available at {@link org.glassfish.jersey.server.model.Resource#isExtended()}.
         * <p>
         * Extended resource model components are helper components that are not considered as a core of a
         * RESTful API. These can be for example {@code OPTIONS} {@link ResourceMethod resource methods}
         * added by {@link org.glassfish.jersey.server.model.ModelProcessor model processors}
         * or {@code application.wadl} resource producing the WADL. Both resource are rather supportive
         * than the core of RESTful API.
         * </p>
         * <p>
         * If not set the resource will not be defined as extended by default.
         * </p>
         *
         * @param extended If {@code true} then resource is marked as extended.
         * @return updated builder object.
         * @see org.glassfish.jersey.server.model.ExtendedResource
         * @since 2.5.1
         */
        public Builder extended(boolean extended) {
            this.extended = extended;
            return this;
        }

        /**
         * Get the flag indicating whether the resource method is extended or is a core of exposed RESTful API.
         *
         * @return {@code true} if the method is extended.
         */
        /* package */ boolean isExtended() {
            return extended;
        }

        private Builder mergeWith(final Resource.Data resourceData) {
            this.resourceMethods.addAll(resourceData.resourceMethods);
            this.childResources.addAll(resourceData.childResources);

            if (subResourceLocator != null && resourceData.subResourceLocator != null) {
                Errors.processWithException(new Runnable() {
                    @Override
                    public void run() {
                        Errors.error(
                                this,
                                LocalizationMessages.RESOURCE_MERGE_CONFLICT_LOCATORS(Builder.this, resourceData, path),
                                Severity.FATAL);
                    }
                });
            } else if (resourceData.subResourceLocator != null) {
                this.subResourceLocator = resourceData.subResourceLocator;
            }

            this.handlerClasses.addAll(resourceData.handlerClasses);
            this.handlerInstances.addAll(resourceData.handlerInstances);

            this.names.addAll(resourceData.names);

            return this;
        }

        /**
         * Merge methods from a given resource model builder into this resource model
         * builder.
         * <p>
         * NOTE: Any "open" method builders in the supplied {@code resourceBuilder} that have
         * not been {@link org.glassfish.jersey.server.model.ResourceMethod.Builder#build()
         * explicitly converted to method models} will be closed as part of this merge operation
         * before merging the resource builder instances.
         * </p>
         *
         * @param resourceBuilder to be merged into this resource model builder.
         * @return updated builder object.
         */
        public Builder mergeWith(final Builder resourceBuilder) {
            resourceBuilder.processMethodBuilders();

            this.resourceMethods.addAll(resourceBuilder.resourceMethods);
            this.childResources.addAll(resourceBuilder.childResources);
            if (Resource.Builder.this.subResourceLocator != null && resourceBuilder.subResourceLocator != null) {
                Errors.processWithException(new Runnable() {
                    @Override
                    public void run() {
                        Errors.warning(this, LocalizationMessages.RESOURCE_MERGE_CONFLICT_LOCATORS(Resource.Builder.this,
                                resourceBuilder, path));
                    }
                });
            } else if (resourceBuilder.subResourceLocator != null) {
                this.subResourceLocator = resourceBuilder.subResourceLocator;
            }
            this.handlerClasses.addAll(resourceBuilder.handlerClasses);
            this.handlerInstances.addAll(resourceBuilder.handlerInstances);
            this.names.addAll(resourceBuilder.names);
            return this;
        }

        /**
         * Called when a new resource, sub-resource and sub-resource locator method
         * was built and should be registered with the resource builder.
         * <p>
         * This is a friend call-back API exposed for a use by a {@link ResourceMethod.Builder
         * ResourceMethod.Builder}.
         * </p>
         *
         * @param builder    builder instance that built the method.
         * @param methodData new resource, sub-resource or sub-resource locator
         */
        void onBuildMethod(ResourceMethod.Builder builder, ResourceMethod.Data methodData) {
            Preconditions.checkState(methodBuilders.remove(builder),
                    "Resource.Builder.onBuildMethod() invoked from a resource method builder "
                            + "that is not registered in the resource builder instance.");

            switch (methodData.getType()) {
                case RESOURCE_METHOD:
                    resourceMethods.add(methodData);
                    break;
                case SUB_RESOURCE_LOCATOR:
                    if (subResourceLocator != null) {
                        Errors.processWithException(new Runnable() {
                            @Override
                            public void run() {
                                Errors.error(
                                        this,
                                        LocalizationMessages.AMBIGUOUS_SRLS(this, path),
                                        Severity.FATAL);
                            }
                        });

                    }
                    subResourceLocator = methodData;
                    break;
            }

            final MethodHandler methodHandler = methodData.getInvocable().getHandler();
            if (methodHandler.isClassBased()) {
                handlerClasses.add(methodHandler.getHandlerClass());
            } else {
                handlerInstances.add(methodHandler.getHandlerInstance());
            }
        }

        private void onBuildChildResource(Builder childResourceBuilder, Resource.Data childResourceData) {
            Preconditions.checkState(childResourceBuilders.remove(childResourceBuilder),
                    "Resource.Builder.onBuildChildResource() invoked from a resource builder "
                            + "that is not registered in the resource builder instance as a child resource builder.");
            childResources.add(childResourceData);

        }

        private List<Resource.Data> mergeResources(List<Resource.Data> resources) {
            List<Resource.Data> mergedResources = Lists.newArrayList();
            for (int i = 0; i < resources.size(); i++) {
                Resource.Data outer = resources.get(i);
                Resource.Builder builder = null;

                for (int j = i + 1; j < resources.size(); j++) {
                    Resource.Data inner = resources.get(j);

                    if (outer.path.equals(inner.path)) {
                        if (builder == null) {
                            builder = Resource.builder(outer);
                        }
                        builder.mergeWith(inner);
                        resources.remove(j);
                        //noinspection AssignmentToForLoopParameter
                        j--;
                    }
                }
                if (builder == null) {
                    mergedResources.add(outer);
                } else {
                    mergedResources.add(builder.buildResourceData());
                }
            }
            return mergedResources;
        }

        private Data buildResourceData() {
            if (parentResource != null && parentResource.isExtended()) {
                this.extended = true;
            }

            processMethodBuilders();
            processChildResourceBuilders();

            final List<Data> mergedChildResources = mergeResources(childResources);
            Set<Class<?>> classes = Sets.newHashSet(handlerClasses);
            Set<Object> instances = Sets.newHashSet(handlerInstances);
            for (Data childResource : mergedChildResources) {
                classes.addAll(childResource.handlerClasses);
                instances.addAll(childResource.handlerInstances);
            }

            if (areAllMembersExtended(mergedChildResources)) {
                extended = true;
            }

            final Data resourceData = new Data(
                    names,
                    path,
                    resourceMethods,
                    subResourceLocator,
                    mergedChildResources,
                    classes,
                    instances,
                    extended);

            if (parentResource != null) {
                parentResource.onBuildChildResource(this, resourceData);
            }
            return resourceData;
        }

        private boolean areAllMembersExtended(List<Data> mergedChildResources) {
            boolean allExtended = true;
            for (ResourceMethod.Data resourceMethod : resourceMethods) {
                if (!resourceMethod.isExtended()) {
                    allExtended = false;
                }
            }
            if (subResourceLocator != null && !subResourceLocator.isExtended()) {
                allExtended = false;
            }

            for (Data childResource : mergedChildResources) {
                if (!childResource.extended) {
                    allExtended = false;
                }
            }
            return allExtended;
        }

        /**
         * Build a new resource model.
         *
         * @return new (immutable) resource model.
         */
        public Resource build() {
            final Data resourceData = buildResourceData();
            return new Resource(null, resourceData);
        }

        private void processMethodBuilders() {
            // We have to iterate the set this way to prevent ConcurrentModificationExceptions
            // caused by the nested invocation of Set.remove(...) in Resource.Builder.onBuildMethod(...).
            while (!methodBuilders.isEmpty()) {
                methodBuilders.iterator().next().build();
            }
        }

        private void processChildResourceBuilders() {
            // We have to iterate the set this way to prevent ConcurrentModificationExceptions
            // caused by the nested invocation of Set.remove(...) in Resource.Builder.onBuildChildResource(...).
            while (!childResourceBuilders.isEmpty()) {
                childResourceBuilders.iterator().next().build();
            }
        }

        @Override
        public String toString() {
            return "Builder{"
                    + "names=" + names
                    + ", path='" + path + '\''
                    + ", methodBuilders=" + methodBuilders
                    + ", childResourceBuilders=" + childResourceBuilders
                    + ", childResources=" + childResources
                    + ", resourceMethods=" + resourceMethods
                    + ", subResourceLocator=" + subResourceLocator
                    + ", handlerClasses=" + handlerClasses
                    + ", handlerInstances=" + handlerInstances
                    + ", parentResource=" + ((parentResource == null) ? "<no parent>" : parentResource.shortToString())
                    + ", extended=" + extended
                    + '}';
        }

        private String shortToString() {
            return "Builder{names=" + names + ", path='" + path + "'}";
        }
    }

    /**
     * Get a new unbound resource model builder.
     *
     * @return new unbound resource model builder.
     * @see Resource.Builder#path(java.lang.String)
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get a new resource model builder for a resource bound to a given path.
     *
     * @param path resource path.
     * @return new resource model builder.
     * @see Resource.Builder#path(java.lang.String)
     */
    public static Builder builder(final String path) {
        return new Builder(path);
    }

    /**
     * Creates a {@link Builder resource builder} instance from the list of {@code resource} which can be merged
     * into a single resource. It must be possible to merge the {@code resources} into a single valid resource.
     * For example all resources must have the same {@link Resource#getPath() path}, they cannot have ambiguous methods
     * on the same path, etc.
     *
     * @param resources Resources with the same path.
     * @return Resource builder initialized from merged resources.
     */
    public static Builder builder(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) {
            return builder();
        }

        final Iterator<Resource> it = resources.iterator();
        Resource.Data resourceData = it.next().data;
        Builder builder = Resource.builder(resourceData);
        String path = resourceData.path;

        while (it.hasNext()) {
            resourceData = it.next().data;
            if ((resourceData.path == null && path == null) || (path != null && path.equals(resourceData.path))) {
                builder.mergeWith(resourceData);
            } else {
                throw new IllegalArgumentException(LocalizationMessages.ERROR_RESOURCES_CANNOT_MERGE());
            }
        }

        return builder;
    }

    /**
     * Create a resource model builder initialized by introspecting an annotated
     * JAX-RS resource class.
     *
     * @param resourceClass resource class to be modelled.
     * @return resource model builder initialized by the class or {@code null} if the
     * class does not represent a resource.
     */
    public static Builder builder(Class<?> resourceClass) {
        return builder(resourceClass, false);
    }

    /**
     * Create a resource model builder initialized by introspecting an annotated
     * JAX-RS resource class.
     *
     * @param resourceClass     resource class to be modelled.
     * @param disableValidation if set to {@code true}, then any model validation checks will be disabled.
     * @return resource model builder initialized by the class or {@code null} if the
     * class does not represent a resource.
     */
    public static Builder builder(Class<?> resourceClass, boolean disableValidation) {
        final Builder builder = new IntrospectionModeller(resourceClass, disableValidation).createResourceBuilder();
        return builder.isEmpty() ? null : builder;
    }

    /**
     * Create a resource model initialized by introspecting an annotated
     * JAX-RS resource class.
     *
     * @param resourceClass resource class to be modelled.
     * @return resource model initialized by the class or {@code null} if the
     * class does not represent a resource.
     */
    public static Resource from(Class<?> resourceClass) {
        return from(resourceClass, false);
    }

    /**
     * Create a resource model initialized by introspecting an annotated
     * JAX-RS resource class.
     *
     * @param resourceClass     resource class to be modelled.
     * @param disableValidation if set to {@code true}, then any model validation checks will be disabled.
     * @return resource model initialized by the class or {@code null} if the
     * class does not represent a resource.
     */
    public static Resource from(Class<?> resourceClass, boolean disableValidation) {
        final Builder builder = new IntrospectionModeller(resourceClass, disableValidation).createResourceBuilder();
        return builder.isEmpty() ? null : builder.build();
    }

    /**
     * Check if the class is acceptable as a JAX-RS provider or resource.
     * <p/>
     * Method returns {@code false} if the class is either
     * <ul>
     * <li>abstract</li>
     * <li>interface</li>
     * <li>annotation</li>
     * <li>primitive</li>
     * <li>local class</li>
     * <li>non-static member class</li>
     * </ul>
     *
     * @param c class to be checked.
     * @return {@code true} if the class is an acceptable JAX-RS provider or
     * resource, {@code false} otherwise.
     */
    public static boolean isAcceptable(Class<?> c) {
        return !((c.getModifiers() & Modifier.ABSTRACT) != 0
                         || c.isPrimitive()
                         || c.isAnnotation()
                         || c.isInterface()
                         || c.isLocalClass()
                         || (c.isMemberClass() && (c.getModifiers() & Modifier.STATIC) == 0));
    }

    /**
     * Get the resource class {@link Path @Path} annotation.
     * <p/>
     * May return {@code null} in case there is no {@code @Path} annotation on the resource.
     *
     * @param resourceClass resource class.
     * @return {@code @Path} annotation instance if present on the resource class (i.e.
     * the class is a root resource class), or {@code null} otherwise.
     */
    public static Path getPath(Class<?> resourceClass) {
        return ModelHelper.getAnnotatedResourceClass(resourceClass).getAnnotation(Path.class);
    }

    /**
     * Get a new resource model builder initialized from a given resource model.
     *
     * @param resource resource model initializing the resource builder.
     * @return new resource model builder.
     */
    public static Builder builder(Resource resource) {
        return builder(resource.data);
    }

    private static Builder builder(Resource.Data resourceData) {
        final Builder b;
        if (resourceData.path == null) {
            b = new Builder();
        } else {
            b = new Builder(resourceData.path);
        }

        b.resourceMethods.addAll(resourceData.resourceMethods);
        b.childResources.addAll(resourceData.childResources);
        b.subResourceLocator = resourceData.subResourceLocator;

        b.handlerClasses.addAll(resourceData.handlerClasses);
        b.handlerInstances.addAll(resourceData.handlerInstances);

        b.names.addAll(resourceData.names);

        return b;
    }

    private static List<Resource> transform(final Resource parent, final List<Data> list) {
        return Lists.transform(list, new Function<Data, Resource>() {
            @Override
            public Resource apply(Data data) {
                return new Resource(parent, data);
            }
        });
    }

    private static <T> List<T> immutableCopy(List<T> list) {
        return list.isEmpty() ? Collections.<T>emptyList() : Collections.unmodifiableList(Lists.newArrayList(list));
    }

    private static <T> Set<T> immutableCopy(Set<T> set) {
        if (set.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<T> result = Sets.newIdentityHashSet();
        result.addAll(set);
        return set;
    }

    private final Resource parent;
    private final Data data;

    private final Value<String> name;

    private final List<ResourceMethod> resourceMethods;
    private final ResourceMethod locator;
    private final List<Resource> childResources;

    private Resource(final Resource parent, final Data data) {
        this.parent = parent;
        this.data = data;

        this.name = Values.lazy(new Value<String>() {
            @Override
            public String get() {
                if (data.names.size() == 1) {
                    return data.names.get(0);
                } else {
                    // return merged name
                    return "Merge of " + data.names.toString();
                }
            }
        });

        this.resourceMethods = immutableCopy(ResourceMethod.transform(Resource.this, data.resourceMethods));
        this.locator = data.subResourceLocator == null ? null : new ResourceMethod(Resource.this, data.subResourceLocator);
        this.childResources = immutableCopy(Resource.transform(Resource.this, data.childResources));
    }

    @Override
    public String getPath() {
        return data.path;
    }

    @Override
    public PathPattern getPathPattern() {
        return data.pathPattern;
    }

    /**
     * Get the parent resource for this resource model or {@code null} in case this
     * resource is a top-level resource and does not have a parent.
     *
     * @return parent resource or {@code null} if the resource does not have a parent.
     * @since 2.1
     */
    public Resource getParent() {
        return parent;
    }

    /**
     * Get the resource name.
     * <p/>
     * If the resource was constructed from a JAX-RS annotated resource class,
     * the resource name will be set to the {@link Class#getName() fully-qualified name}
     * of the resource class.
     *
     * @return reference JAX-RS resource handler class.
     */
    public String getName() {
        return name.get();
    }

    /**
     * Return a list of resource names.
     *
     * @return a list of resource names.
     */
    public List<String> getNames() {
        return data.names;
    }

    /**
     * Provides a non-null list of resource methods available on the resource.
     *
     * @return non-null abstract resource method list.
     */
    public List<ResourceMethod> getResourceMethods() {
        return resourceMethods;
    }

    /**
     * Provides a resource locator available on the resource.
     *
     * @return Resource locator if it is present, null otherwise.
     */
    public ResourceMethod getResourceLocator() {
        return locator;
    }

    /**
     * Provides resource methods and resource locator are available on the resource. The list is ordered so that resource
     * methods are positioned first before resource locator.
     *
     * @return List of resource methods and resource locator.
     */
    public List<ResourceMethod> getAllMethods() {
        final LinkedList<ResourceMethod> methodsAndLocators = Lists.newLinkedList(getResourceMethods());
        final ResourceMethod loc = getResourceLocator();
        if (loc != null) {
            methodsAndLocators.add(loc);
        }
        return methodsAndLocators;
    }

    /**
     * Returns the list of child resources available on this resource.
     *
     * @return Non-null list of child resources (may be empty).
     */
    public List<Resource> getChildResources() {
        return childResources;
    }

    /**
     * Get the method handler classes for the resource methods registered on the resource.
     *
     * @return resource method handler classes.
     */
    public Set<Class<?>> getHandlerClasses() {
        return data.handlerClasses;
    }

    /**
     * Get the method handler (singleton) instances for the resource methods registered
     * on the resource.
     *
     * @return resource method handler instances.
     */
    public Set<Object> getHandlerInstances() {
        return data.handlerInstances;
    }

    @Override
    public void accept(ResourceModelVisitor visitor) {
        if (getParent() == null) {
            visitor.visitResource(this);
        } else {
            visitor.visitChildResource(this);
        }
    }

    /**
     * Get the flag indicating whether the resource is extended.
     * <p/>
     * I.e. it is not part of the resource set that forms the REST API explicitly defined by the application developer (resource
     * classes and instances returned from the {@link javax.ws.rs.core.Application} subclass getters).
     * <p>
     * Extended resource model components are helper components that are not part of the explicitly defined REST API of
     * a JAX-RS application, instead they are generated by Jersey runtime. For example, extended resource model components
     * include {@code OPTIONS} {@link ResourceMethod resource methods}
     * automatically generated by Jersey {@link org.glassfish.jersey.server.model.ModelProcessor resource model processors}
     * or {@code application.wadl} resource API that exposes the application WADL descriptor.
     * </p>
     *
     * @return {@code true} if the resource is part of the application's extended REST API, {@code false} otherwise.
     * @see org.glassfish.jersey.server.model.ExtendedResource
     * @since 2.5.1
     */
    public boolean isExtended() {
        return data.extended;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public List<? extends ResourceModelComponent> getComponents() {
        List<ResourceModelComponent> components = new LinkedList<>();

        components.addAll(getChildResources());
        components.addAll(getResourceMethods());

        final ResourceMethod resourceLocator = getResourceLocator();
        if (resourceLocator != null) {
            components.add(resourceLocator);
        }
        return components;
    }
}
