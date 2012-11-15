/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;

import org.glassfish.jersey.uri.PathPattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
 * resourceBuilder.addMethod("GET")
 *         .path("world")
 *         .produces("text/plain")
 *         .handledBy(new Inflector&lt;Request, String&gt;() {
 *                 &#64;Override
 *                 public String apply(Request request) {
 *                     return "Hello World!";
 *                 }
 *         });
 *
 * // Register the new programmatic resource in the application's configuration.
 * resourceConfig.addResources(resourceBuilder.build());
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
 */
public final class Resource implements Routed, ResourceModelComponent {

    /**
     * Resource model component builder.
     */
    public static final class Builder {

        private List<String> names;
        private String path;
        private boolean isRoot;

        private final Set<ResourceMethod.Builder> methodBuilders;

        private final List<ResourceMethod> resourceMethods;
        private final List<ResourceMethod> subResourceMethods;
        private final List<ResourceMethod> locators;

        private final Set<Class<?>> handlerClasses;
        private final Set<Object> handlerInstances;

        private Builder() {
            this.methodBuilders = Sets.newIdentityHashSet();

            this.resourceMethods = Lists.newLinkedList();
            this.subResourceMethods = Lists.newLinkedList();
            this.locators = Lists.newLinkedList();

            this.handlerClasses = Sets.newIdentityHashSet();
            this.handlerInstances = Sets.newIdentityHashSet();

            name("[unnamed]");
        }

        private Builder(final String path) {
            this();
            path(path);
        }

        private boolean isEmpty() {
            return !isRoot &&
                    methodBuilders.isEmpty() &&
                    resourceMethods.isEmpty() &&
                    subResourceMethods.isEmpty() &&
                    locators.isEmpty();
        }

        /**
         * Define a new name of the built resource.
         *
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
            // we need to maintain a separate flag because the entered path value
            // can be null.
            this.isRoot = true;
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
         * Merge methods from a given resource model into this resource model builder.
         *
         * @param resource to be merged into this resource model builder.
         * @return updated builder object.
         */
        public Builder mergeWith(final Resource resource) {
            this.resourceMethods.addAll(resource.getResourceMethods());
            this.subResourceMethods.addAll(resource.getSubResourceMethods());
            this.locators.addAll(resource.getSubResourceLocators());

            this.handlerClasses.addAll(resource.getHandlerClasses());
            this.handlerInstances.addAll(resource.getHandlerInstances());

            this.names.addAll(resource.names);

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
        public Builder mergeWith(Builder resourceBuilder) {
            resourceBuilder.processMethodBuilders();

            this.resourceMethods.addAll(resourceBuilder.resourceMethods);
            this.subResourceMethods.addAll(resourceBuilder.subResourceMethods);
            this.locators.addAll(resourceBuilder.locators);

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
         * @param builder builder instance that built the method.
         * @param method  new resource, sub-resource or sub-resource locator
         */
        void onBuildMethod(ResourceMethod.Builder builder, ResourceMethod method) {
            Preconditions.checkState(methodBuilders.remove(builder),
                    "Resource.Builder.onBuildMethod() invoked from a resource method builder " +
                            "that is not registered in the resource builder instance.");

            switch (method.getType()) {
                case RESOURCE_METHOD:
                    resourceMethods.add(method);
                    break;
                case SUB_RESOURCE_METHOD:
                    subResourceMethods.add(method);
                    break;
                case SUB_RESOURCE_LOCATOR:
                    locators.add(method);
                    break;
            }

            final MethodHandler methodHandler = method.getInvocable().getHandler();
            if (methodHandler.isClassBased()) {
                handlerClasses.add(methodHandler.getHandlerClass());
            } else {
                handlerInstances.add(methodHandler.getHandlerInstance());
            }
        }

        /**
         * Build a new resource model.
         *
         * @return new (immutable) resource model.
         */
        public Resource build() {
            processMethodBuilders();

            return new Resource(
                    immutableCopy(names),
                    path,
                    isRoot,
                    immutableCopy(resourceMethods),
                    immutableCopy(subResourceMethods),
                    immutableCopy(locators),
                    immutableCopy(handlerClasses),
                    immutableCopy(handlerInstances));
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

        private void processMethodBuilders() {
            // We have to iterate the set this way to prevent ConcurrentModificationExceptions
            // caused by the nested invocation of Set.remove(...) in Resource.Builder.onBuildMethod(...).
            while (!methodBuilders.isEmpty()) {
                methodBuilders.iterator().next().build();
            }
        }
    }

    /**
     * Get a new unbound resource model builder.
     * <p/>
     * Note that a resource built from the returned builder is <i>NOT</i>
     * automatically marked to be a {@link #isRootResource() root resource}.
     * This can be changed by invoking the {@link Builder#path(java.lang.String)
     * path(...)} method on the returned builder.
     *
     * @return new unbound resource model builder.
     * @see Resource.Builder#path(java.lang.String)
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get a new resource model builder for a resource bound to a given path.
     * <p/>
     * Note that a resource built from the returned builder is automatically
     * marked to be a {@link #isRootResource() root resource}.
     *
     * @param path resource path.
     * @return new resource model builder.
     * @see Resource.Builder#path(java.lang.String)
     */
    public static Builder builder(final String path) {
        return new Builder(path);
    }

    /**
     * Create a resource model builder initialized by introspecting an annotated
     * JAX-RS resource class.
     * <p/>
     * Method performs an {@link #isAcceptable(java.lang.Class) acceptability} check,
     * on the resource class prior to the resource model creation.
     *
     * @param resourceClass resource class to be modelled.
     * @return resource model builder initialized by the class or {@code null} if the
     *         class does not represent a resource.
     * @throws IllegalArgumentException in case the class is not
     *                                  {@link #isAcceptable(java.lang.Class) acceptable}
     *                                  as a JAX-RS resource.
     */
    public static Builder builder(Class<?> resourceClass) throws IllegalArgumentException {
        final Builder builder = new IntrospectionModeller(resourceClass).createResourceBuilder(false);
        return builder.isEmpty() ? null : builder;
    }

    /**
     * Create a resource model builder initialized by introspecting an annotated
     * JAX-RS resource instance.
     * <p/>
     * Unlike {@link #builder(Class)}, this method does not perform
     * the {@link #isAcceptable(java.lang.Class) acceptability} check, since it is
     * assumed that the instance of the resource has already been created and is
     * acceptable.
     *
     * @param resource  resource instance to be modelled.
     * @return resource model builder initialized by instance or {@code null} if the
     *         instance does not represent a resource.
     */
    public static Builder builder(Object resource) {
        final Builder builder = new IntrospectionModeller(resource.getClass()).createResourceBuilder(true);
        return builder.isEmpty() ? null : builder;
    }

    /**
     * Create a resource model initialized by introspecting an annotated
     * JAX-RS resource class.
     * <p/>
     * Method performs an {@link #isAcceptable(java.lang.Class) acceptability} check,
     * on the resource class prior to the resource model creation.
     *
     * @param resourceClass resource class to be modelled.
     * @return resource model initialized by the class or {@code null} if the
     *         class does not represent a resource.
     * @throws IllegalArgumentException in case the class is not
     *                                  {@link #isAcceptable(java.lang.Class) acceptable}
     *                                  as a JAX-RS resource.
     */
    public static Resource from(Class<?> resourceClass) throws IllegalArgumentException {
        final Builder builder = new IntrospectionModeller(resourceClass).createResourceBuilder(false);
        return builder.isEmpty() ? null : builder.build();
    }

    /**
     * Create a resource model initialized by introspecting an annotated
     * JAX-RS resource instance.
     * <p/>
     * Unlike {@link #builder(Class)}, this method does not perform
     * the {@link #isAcceptable(java.lang.Class) acceptability} check, since it is
     * assumed that the instance of the resource has already been created and is
     * acceptable.
     *
     * @param resource  resource instance to be modelled.
     * @return resource model initialized by instance or {@code null} if the
     *         instance does not represent a resource.
     */
    public static Resource from(Object resource) {
        final Builder builder = new IntrospectionModeller(resource.getClass()).createResourceBuilder(true);
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
     *         resource, {@code false} otherwise.
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
     *         the class is a root resource class), or {@code null} otherwise.
     */
    public static Path getPath(Class<?> resourceClass) {
        return IntrospectionModeller.getAnnotatedResourceClass(resourceClass).getAnnotation(Path.class);
    }

    /**
     * Get a new resource model builder initialized from a given resource model.
     *
     * @param resource resource model initializing the resource builder.
     * @return new resource model builder.
     */
    public static Builder builder(Resource resource) {
        final Builder b = new Builder(resource.path);

        b.resourceMethods.addAll(resource.resourceMethods);
        b.subResourceMethods.addAll(resource.subResourceMethods);
        b.locators.addAll(resource.subResourceLocators);

        b.handlerClasses.addAll(resource.handlerClasses);
        b.handlerInstances.addAll(resource.handlerInstances);

        return b;
    }

    private final List<String> names;
    private transient String name;
    private final String path;
    private final PathPattern pathPattern;
    private final boolean isRoot;

    private final List<ResourceMethod> resourceMethods;
    private final List<ResourceMethod> subResourceMethods;
    private final List<ResourceMethod> subResourceLocators;

    private final Set<Class<?>> handlerClasses;
    private final Set<Object> handlerInstances;

    private Resource(
            final List<String> names,
            final String path,
            final boolean isRoot,
            final List<ResourceMethod> resourceMethods,
            final List<ResourceMethod> subResourceMethods,
            final List<ResourceMethod> subResourceLocators,
            final Set<Class<?>> handlerClasses,
            final Set<Object> handlerInstances) {

        this.names = names;
        this.path = path;
        this.isRoot = isRoot;

        this.pathPattern = (!isRoot || path == null || path.isEmpty()) ?
                PathPattern.OPEN_ROOT_PATH_PATTERN : new PathPattern(path, PathPattern.RightHandPath.capturingZeroOrMoreSegments);

        this.resourceMethods = resourceMethods;
        this.subResourceMethods = subResourceMethods;
        this.subResourceLocators = subResourceLocators;

        this.handlerClasses = handlerClasses;
        this.handlerInstances = handlerInstances;
    }

    /**
     * Check if this resource model models a JAX-RS root resource.
     *
     * @return {@code true}, if this is a model of a JAX-RS root resource,
     *         {@code false} otherwise.
     */
    public boolean isRootResource() {
        return isRoot;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public PathPattern getPathPattern() {
        return pathPattern;
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
        if (name == null) {
            if (names.size() == 1) {
                name = names.get(0);
            } else {
                // return merged name
                StringBuilder nameBuilder = new StringBuilder("Merge of ");
                nameBuilder.append(names.toString());
                name = nameBuilder.toString();
            }
        }

        return name;
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
     * Provides a non-null list of sub-resource methods available on the resource.
     *
     * @return non-null abstract sub-resource method list.
     */
    public List<ResourceMethod> getSubResourceMethods() {
        return subResourceMethods;
    }

    /**
     * Provides a non-null list of sub-resource locators available on the resource.
     *
     * @return non-null abstract sub-resource locator list.
     */
    public List<ResourceMethod> getSubResourceLocators() {
        return subResourceLocators;
    }

    /**
     * Get the method handler classes for the resource methods registered on the resource.
     *
     * @return resource method handler classes.
     */
    public Set<Class<?>> getHandlerClasses() {
        return handlerClasses;
    }

    /**
     * Get the method handler (singleton) instances for the resource methods registered
     * on the resource.
     *
     * @return resource method handler instances.
     */
    public Set<Object> getHandlerInstances() {
        return handlerInstances;
    }

    @Override
    public void accept(ResourceModelVisitor visitor) {
        visitor.visitResourceClass(this);
    }

    @Override
    public String toString() {
        return "Resource {"
                + ((path == null) ? "[unbound], " : "\"" + path + "\", ")
                + resourceMethods.size() + " resource methods, "
                + subResourceMethods.size() + " sub-resource methods, "
                + subResourceLocators.size() + " sub-resource locators, "
                + handlerClasses.size() + " method handler classes, "
                + handlerInstances.size() + " method handler instances"
                + '}';
    }

    @Override
    public List<? extends ResourceModelComponent> getComponents() {
        List<ResourceMethod> components = new LinkedList<ResourceMethod>();

        components.addAll(getResourceMethods());
        components.addAll(getSubResourceMethods());
        components.addAll(getSubResourceLocators());

        return components;
    }
}
