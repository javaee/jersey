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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.NameBinding;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.model.NameBound;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.uri.PathPattern;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Model of a method available on a resource. Covers resource method, sub-resource
 * method and sub-resource locator.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ResourceMethod implements ResourceModelComponent, Producing, Consuming, Suspendable, NameBound {

    /**
     * Resource method classification based on the recognized JAX-RS
     * resource method types.
     */
    public static enum JaxrsType {
        /**
         * JAX-RS resource method.
         * <p/>
         * Does not have a path template assigned. Is assigned to a particular HTTP method.
         */
        RESOURCE_METHOD {
            @Override
            PathPattern createPatternFor(String pathTemplate) {
                // template is ignored.
                return PathPattern.END_OF_PATH_PATTERN;
            }
        },
        /**
         * JAX-RS sub-resource method.
         * <p/>
         * Has a sub-path template assigned and is assigned to a particular HTTP method.
         */
        SUB_RESOURCE_METHOD {
            @Override
            PathPattern createPatternFor(String pathTemplate) {
                return new PathPattern(pathTemplate, PathPattern.RightHandPath.capturingZeroSegments);
            }
        },
        /**
         * JAX-RS sub-resource locator.
         * <p/>
         * Has a sub-path template assigned but is not assigned to any particular HTTP method.
         * Instead it produces a sub-resource instance that should be further
         * used in the request URI matching.
         */
        SUB_RESOURCE_LOCATOR {
            @Override
            PathPattern createPatternFor(String pathTemplate) {
                return new PathPattern(pathTemplate, PathPattern.RightHandPath.capturingZeroOrMoreSegments);
            }
        };

        /**
         * Create a proper matching path pattern from the provided template for
         * the selected method type.
         *
         * @param pathTemplate method path template.
         * @return method matching path pattern.
         */
        abstract PathPattern createPatternFor(String pathTemplate);

        private static JaxrsType classify(String httpMethod) {
            if (httpMethod != null && !httpMethod.isEmpty()) {
                return RESOURCE_METHOD;
            } else {
                return SUB_RESOURCE_LOCATOR;
            }
        }
    }

    /**
     * Resource method model builder.
     */
    public static final class Builder {
        private final Resource.Builder parent;

        // HttpMethod
        private String httpMethod;
        // Consuming & Producing
        private final Set<MediaType> consumedTypes;
        private final Set<MediaType> producedTypes;
        // Suspendable
        private boolean managedAsync;
        private boolean suspended;
        private long suspendTimeout;
        private TimeUnit suspendTimeoutUnit;
        // Invocable
        private Class<?> handlerClass;
        private Object handlerInstance;
        private Method handlingMethod;
        private boolean encodedParams;
        // NameBound
        private final Collection<Class<? extends Annotation>> nameBindings;

        /**
         * Create a resource method builder.
         * <p>
         * The supplied parent resource model builder will be called to register
         * the newly created resource method instance as part of the {@link #build()}
         * method invocation.
         * </p>
         * <p>
         * Note that the {@link #build()} method does not have to be invoked manually
         * as the registration will happen automatically as part of the
         * {@link org.glassfish.jersey.server.model.Resource.Builder#build()} method
         * invocation.
         * </p>
         * @param parent parent resource model builder.
         */
        Builder(final Resource.Builder parent) {
            this.parent = parent;

            this.httpMethod = null;

            this.consumedTypes = Sets.newLinkedHashSet();
            this.producedTypes = Sets.newLinkedHashSet();

            this.suspended = false;
            this.suspendTimeout = AsyncResponse.NO_TIMEOUT;
            this.suspendTimeoutUnit = TimeUnit.MILLISECONDS;

            this.encodedParams = false;

            this.nameBindings = Sets.newLinkedHashSet();
        }

        /**
         * Set the associated HTTP method name.
         *
         * @param name HTTP method name.
         * @return updated builder object.
         */
        public Builder httpMethod(String name) {
            this.httpMethod = name;
            return this;
        }


        /**
         * Add produced media types supported by the component.
         *
         * @param types produced media types.
         * @return updated builder object.
         */
        public Builder produces(String... types) {
            return produces(MediaTypes.createFrom(types));
        }

        /**
         * Add produced media types supported by the component.
         *
         * @param types produced media types.
         * @return updated builder object.
         */
        public Builder produces(MediaType... types) {
            return produces(Arrays.asList(types));
        }

        /**
         * Add produced media types supported by the component.
         *
         * @param types produced media types.
         * @return updated builder object.
         */
        public Builder produces(Collection<MediaType> types) {
            this.producedTypes.addAll(types);
            return this;
        }

        /**
         * Add consumed media types supported by the component.
         *
         * @param types consumed media types.
         * @return updated builder object.
         */
        public Builder consumes(String... types) {
            return consumes(MediaTypes.createFrom(types));
        }

        /**
         * Add consumed media types supported by the component.
         *
         * @param types consumed media types.
         * @return updated builder object.
         */
        public Builder consumes(MediaType... types) {
            return consumes(Arrays.asList(types));
        }

        /**
         * Add consumed media types supported by the component.
         *
         * @param types consumed media types.
         * @return updated builder object.
         */
        public Builder consumes(Collection<MediaType> types) {
            this.consumedTypes.addAll(types);
            return this;
        }

        /**
         * Adds name bindings. The passed annotation types not annotated with {@link javax.ws.rs.NameBinding}
         * meta-annotation will be ignored.
         *
         * @param nameBindings collection of name binding annotation types.
         * @return updated builder object.
         */
        public Builder nameBindings(final Collection<Class<? extends Annotation>> nameBindings) {
            for (Class<? extends Annotation> nameBinding : nameBindings) {
                if (nameBinding.getAnnotation(NameBinding.class) != null) {
                    this.nameBindings.add(nameBinding);
                }
            }
            return this;
        }

        /**
         * Adds name bindings. The passed annotation types not annotated with {@link javax.ws.rs.NameBinding}
         * meta-annotation will be ignored.
         *
         * @param nameBindings name binding annotation types.
         * @return updated builder object.
         */
        public Builder nameBindings(final Class<? extends Annotation>... nameBindings) {
            return nameBindings(Arrays.asList(nameBindings));
        }

        /**
         * Adds name bindings. The passed annotations not annotated with {@link javax.ws.rs.NameBinding}
         * meta-annotation will be ignored.
         *
         * @param nameBindings name binding annotations.
         * @return updated builder object.
         */
        public Builder nameBindings(final Annotation... nameBindings) {
            return nameBindings(Collections2.transform(Arrays.asList(nameBindings),
                    new Function<Annotation, Class<? extends Annotation>>() {
                        @Override
                        public Class<? extends Annotation> apply(Annotation input) {
                            return input.annotationType();
                        }
                    })
            );
        }

        /**
         * Mark the component for suspending.
         * <p/>
         * An invocation of a component (resource or sub-resource method) marked
         * for suspending will be automatically suspended by the Jersey runtime.
         *
         * @param timeout suspend timeout value.
         * @param unit    suspend timeout time unit.
         * @return updated builder object.
         */
        public Builder suspended(long timeout, TimeUnit unit) {
            suspended = true;
            suspendTimeout = timeout;
            suspendTimeoutUnit = unit;

            return this;
        }

        public Builder managedAsync() {
            managedAsync = true;

            return this;
        }

        /**
         * If set to {@code true}, the parameter values will not be automatically
         * decoded.
         * <p/>
         * Defaults to {@code false}.
         *
         * @param value {@code true} if the automatic parameter decoding should be
         *              disabled, false otherwise.
         * @return updated builder object.
         * @see javax.ws.rs.Encoded
         */
        public Builder encodedParameters(boolean value) {
            encodedParams = value;
            return this;
        }

        /**
         * Define a resource method handler binding.
         *
         * @param handlerClass concrete resource method handler class.
         * @param method       handling method.
         * @return updated builder object.
         */
        public Builder handledBy(Class<?> handlerClass, Method method) {
            this.handlerInstance = null;

            this.handlerClass = handlerClass;
            this.handlingMethod = method;

            return this;
        }

        /**
         * Define a resource method handler binding.
         *
         * @param handlerInstance concrete resource method handler instance.
         * @param method          handling method.
         * @return updated builder object.
         */
        public Builder handledBy(Object handlerInstance, Method method) {
            this.handlerClass = null;

            this.handlerInstance = handlerInstance;
            this.handlingMethod = method;

            return this;
        }

        /**
         * Define an inflector-based resource method handler binding.
         *
         * @param inflector inflector handling the resource method.
         * @return updated builder object.
         */
        public Builder handledBy(Inflector<ContainerRequestContext, ?> inflector) {
            return handledBy(inflector, Invocable.APPLY_INFLECTOR_METHOD);
        }

        /**
         * Define an inflector-based resource method handler binding.
         *
         * @param inflectorClass class of the inflector handling the resource method.
         * @return updated builder object.
         */
        public Builder handledBy(Class<? extends Inflector> inflectorClass) {
            return handledBy(inflectorClass, Invocable.APPLY_INFLECTOR_METHOD);
        }

        /**
         * Build the resource method model and register it with the parent
         * {@link Resource.Builder Resource.Builder}.
         *
         * @return new resource method model.
         */
        public ResourceMethod build() {
            final Invocable invocable = createInvocable();

            ResourceMethod method = new ResourceMethod(
                    httpMethod,
                    consumedTypes,
                    producedTypes,
                    managedAsync,
                    suspended,
                    suspendTimeout,
                    suspendTimeoutUnit,
                    invocable,
                    nameBindings);

            parent.onBuildMethod(this, method);

            return method;
        }

        private Invocable createInvocable() {
            assert handlerClass != null || handlerInstance != null;

            final MethodHandler handler;
            if (handlerClass != null) {
                handler = MethodHandler.create(handlerClass, encodedParams);
            } else { // instance based
                handler = MethodHandler.create(handlerInstance);
            }

            return Invocable.create(handler, handlingMethod, encodedParams);
        }
    }

    // JAX-RS method type
    private final JaxrsType type;
    // HttpMethod
    private final String httpMethod;
    // Consuming & Producing
    private final List<MediaType> consumedTypes;
    private final List<MediaType> producedTypes;
    // SuspendableComponent
    private final boolean managedAsync;
    private final boolean suspended;
    private final long suspendTimeout;
    private final TimeUnit suspendTimeoutUnit;
    // Invocable
    private final Invocable invocable;
    // NameBound
    private final Collection<Class<? extends Annotation>> nameBindings;

    private ResourceMethod(final String httpMethod,
                           final Collection<MediaType> consumedTypes,
                           final Collection<MediaType> producedTypes,
                           boolean managedAsync, final boolean suspended,
                           final long suspendTimeout,
                           final TimeUnit suspendTimeoutUnit,
                           final Invocable invocable,
                           final Collection<Class<? extends Annotation>> nameBindings
    ) {
        this.managedAsync = managedAsync;
        this.type = JaxrsType.classify(httpMethod);

        this.httpMethod = (httpMethod == null) ? httpMethod : httpMethod.toUpperCase();

        this.consumedTypes = Collections.unmodifiableList(Lists.newArrayList(consumedTypes));
        this.producedTypes = Collections.unmodifiableList(Lists.newArrayList(producedTypes));
        this.invocable = invocable;
        this.suspended = suspended;
        this.suspendTimeout = suspendTimeout;
        this.suspendTimeoutUnit = suspendTimeoutUnit;

        this.nameBindings = Collections.unmodifiableCollection(Lists.newArrayList(nameBindings));
    }

    /**
     * Get the JAX-RS method type.
     *
     * @return the JAX-RS method type.
     */
    public JaxrsType getType() {
        return type;
    }

    /**
     * Get the associated HTTP method.
     * <p/>
     * May return {@code null} in case the method represents a sub-resource
     * locator.
     *
     * @return the associated HTTP method, or {@code null} in case this method
     *         represents a sub-resource locator.
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * Get the invocable method model.
     *
     * @return invocable method model.
     */
    public Invocable getInvocable() {
        return invocable;
    }


    // Consuming
    @Override
    public List<MediaType> getConsumedTypes() {
        return consumedTypes;
    }

    // Producing
    @Override
    public List<MediaType> getProducedTypes() {
        return producedTypes;
    }

    // Suspendable
    @Override
    public long getSuspendTimeout() {
        return suspendTimeout;
    }

    @Override
    public TimeUnit getSuspendTimeoutUnit() {
        return suspendTimeoutUnit;
    }

    @Override
    public boolean isSuspendDeclared() {
        return suspended;
    }

    @Override
    public boolean isManagedAsyncDeclared() {
        return managedAsync;
    }

    // ResourceModelComponent
    @Override
    public List<? extends ResourceModelComponent> getComponents() {
        return Arrays.asList(invocable);
    }

    @Override
    public void accept(ResourceModelVisitor visitor) {
        visitor.visitResourceMethod(this);
    }

    // NameBound
    @Override
    public boolean isNameBound() {
        return !nameBindings.isEmpty();
    }

    @Override
    public Collection<Class<? extends Annotation>> getNameBindings() {
        return nameBindings;
    }

    @Override
    public String toString() {
        return "ResourceMethod{" +
                "httpMethod=" + httpMethod +
                ", consumedTypes=" + consumedTypes +
                ", producedTypes=" + producedTypes +
                ", suspended=" + suspended +
                ", suspendTimeout=" + suspendTimeout +
                ", suspendTimeoutUnit=" + suspendTimeoutUnit +
                ", invocable=" + invocable +
                ", nameBindings=" + nameBindings + '}';
    }
}
