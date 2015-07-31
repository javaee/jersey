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

package org.glassfish.jersey.server.model.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.RuntimeResource;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Helper class with methods supporting processing resource model by {@link org.glassfish.jersey.server.model.ModelProcessor
 * model processors}.
 *
 * @author Miroslav Fuksa
 * @author Michal Gajdos
 */
public final class ModelProcessorUtil {

    private ModelProcessorUtil() {
        throw new AssertionError("Instantiation not allowed.");
    }

    /**
     * Return allowed methods for the given {@code resource}. OPTIONS and HEAD are always returned in the result.
     *
     * @param resource Resource for which resource methods should be found.
     * @return Set of resource methods that can be invoked on the given resource.
     */
    public static Set<String> getAllowedMethods(RuntimeResource resource) {
        boolean getFound = false;
        Set<String> allowedMethods = Sets.newHashSet();
        for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
            final String httpMethod = resourceMethod.getHttpMethod();
            allowedMethods.add(httpMethod);
            if (!getFound && httpMethod.equals(HttpMethod.GET)) {
                getFound = true;
            }
        }
        allowedMethods.add(HttpMethod.OPTIONS);
        if (getFound) {
            allowedMethods.add(HttpMethod.HEAD);
        }
        return allowedMethods;
    }


    private static boolean isMethodOverridden(ResourceMethod resourceMethod, String httpMethod, MediaType consumes,
                                              MediaType produces) {
        if (!resourceMethod.getHttpMethod().equals(httpMethod)) {
            return false;
        }
        final boolean consumesMatch = overrides(resourceMethod.getConsumedTypes(), consumes);
        final boolean producesMatch = overrides(resourceMethod.getProducedTypes(), produces);
        return consumesMatch && producesMatch;
    }


    private static boolean overrides(List<MediaType> mediaTypes, MediaType mediaType) {
        if (mediaTypes.isEmpty()) {
            return true;
        }
        for (MediaType mt : mediaTypes) {
            if (overrides(mt, mediaType)) {
                return true;
            }
        }
        return false;
    }


    private static boolean overrides(MediaType mt1, MediaType mt2) {
        return mt1.isWildcardType()
                || (mt1.getType().equals(mt2.getType()) && (mt1.isWildcardSubtype() || mt1.getSubtype().equals(mt2.getSubtype()
        )));
    }

    /**
     * Method bean containing basic information about enhancing resource method.
     */
    public static class Method {

        private final String httpMethod;
        private final String path;

        private final List<MediaType> consumes;
        private final List<MediaType> produces;

        private final Class<? extends Inflector<ContainerRequestContext, Response>> inflectorClass;
        private final Inflector<ContainerRequestContext, Response> inflector;

        /**
         * Create new method instance.
         *
         * @param path relative path of the method.
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media type.
         * @param produces Produces media type.
         * @param inflector Inflector handling the resource method.
         */
        public Method(String path, String httpMethod, MediaType consumes, MediaType produces,
                      Class<? extends Inflector<ContainerRequestContext, Response>> inflector) {
            this(path, httpMethod, Collections.singletonList(consumes), Collections.singletonList(produces), inflector);
        }

        /**
         * Create new method instance.
         *
         * @param path relative path of the method.
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media types.
         * @param produces Produces media types.
         * @param inflectorClass Inflector handling the resource method.
         */
        public Method(String path, String httpMethod, List<MediaType> consumes, List<MediaType> produces,
                      Class<? extends Inflector<ContainerRequestContext, Response>> inflectorClass) {
            this.path = path;
            this.httpMethod = httpMethod;
            this.consumes = consumes;
            this.produces = produces;
            this.inflectorClass = inflectorClass;
            this.inflector = null;
        }

        /**
         * Create new method instance.
         *
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media type.
         * @param produces Produces media type.
         * @param inflector Inflector handling the resource method.
         */
        public Method(String httpMethod, MediaType consumes, MediaType produces,
                      Class<? extends Inflector<ContainerRequestContext, Response>> inflector) {
            this(null, httpMethod, consumes, produces, inflector);
        }

        /**
         * Create new method instance.
         *
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media types.
         * @param produces Produces media types.
         * @param inflector Inflector handling the resource method.
         */
        public Method(String httpMethod, List<MediaType> consumes, List<MediaType> produces,
                      Class<? extends Inflector<ContainerRequestContext, Response>> inflector) {
            this(null, httpMethod, consumes, produces, inflector);
        }

        /**
         * Create new method instance.
         *
         * @param path relative path of the method.
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media types.
         * @param produces Produces media types.
         * @param inflector Inflector handling the resource method.
         */
        public Method(String path, String httpMethod, List<MediaType> consumes, List<MediaType> produces,
                      Inflector<ContainerRequestContext, Response> inflector) {
            this.path = path;
            this.httpMethod = httpMethod;
            this.consumes = consumes;
            this.produces = produces;
            this.inflectorClass = null;
            this.inflector = inflector;
        }

        /**
         * Create new method instance.
         *
         * @param path relative path of the method.
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media type.
         * @param produces Produces media type.
         * @param inflector Inflector handling the resource method.
         */
        public Method(String path, String httpMethod, MediaType consumes, MediaType produces,
                      Inflector<ContainerRequestContext, Response> inflector) {
            this(path, httpMethod, Collections.singletonList(consumes), Collections.singletonList(produces), inflector);
        }

        /**
         * Create new method instance.
         *
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media type.
         * @param produces Produces media type.
         * @param inflector Inflector handling the resource method.
         */
        public Method(String httpMethod, MediaType consumes, MediaType produces,
                      Inflector<ContainerRequestContext, Response> inflector) {
            this(null, httpMethod, consumes, produces, inflector);
        }

        /**
         * Create new method instance.
         *
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media types.
         * @param produces Produces media types.
         * @param inflector Inflector handling the resource method.
         */
        public Method(String httpMethod, List<MediaType> consumes, List<MediaType> produces,
                      Inflector<ContainerRequestContext, Response> inflector) {
            this(null, httpMethod, consumes, produces, inflector);
        }
    }

    /**
     * Enhance {@code resourceModel} with a list of additional methods.
     *
     * The {@code resourceModel} is traversed and for each available runtime resource URI in the model {@code methods} are added.
     * In case of method conflicts, the existing resource methods will be preserved and will not be 'overridden' by any new
     * method from the {@code methods} list. Overriding check takes into account media types of methods so
     * that new resource methods with same HTTP method can be defined only for a more more specific media type.
     *
     * @param resourceModel Resource model to be enhanced.
     * @param subResourceModel {@code true} if the {@code resourceModel} to be enhanced is a sub resource model, {@code false}
     *                                     if it is application resource model.
     * @param methods List of enhancing methods.
     * @param extendedFlag This flag will initialize the property
     *                  {@link org.glassfish.jersey.server.model.ResourceMethod#isExtended()}.
     *
     * @return New resource model builder enhanced by {@code methods}.
     */
    public static ResourceModel.Builder enhanceResourceModel(ResourceModel resourceModel, boolean subResourceModel,
                                                             List<Method> methods, boolean extendedFlag) {
        ResourceModel.Builder newModelBuilder = new ResourceModel.Builder(resourceModel, subResourceModel);

        for (RuntimeResource resource : resourceModel.getRuntimeResourceModel().getRuntimeResources()) {
            enhanceResource(resource, newModelBuilder, methods, extendedFlag);
        }
        return newModelBuilder;
    }

    /**
     * Enhance the runtime resource referenced by {@code resource} parameter with a list of additional methods.
     *
     * The new {@code methods} are added to the runtime resource. In case of method conflicts, the existing resource methods
     * will be preserved and will not be 'overridden' by any new method from the {@code methods} list.
     * Overriding check takes into account media types of methods so that new resource methods with same HTTP method
     * can be defined only for a more more specific media type.
     *
     * @param resource Runtime resource to be enhanced.
     * @param enhancedModelBuilder Builder for the enhanced resource model to be used.
     * @param methods List of enhancing methods.
     * @param extended This flag will initialize the property
     *                  {@link org.glassfish.jersey.server.model.ResourceMethod#isExtended()}.
     */
    public static void enhanceResource(RuntimeResource resource, ResourceModel.Builder enhancedModelBuilder,
                                       List<Method> methods, boolean extended) {
        final Resource firstResource = resource.getResources().get(0);

        if (methodsSuitableForResource(firstResource, methods)) {
            for (Method method : methods) {
                final Set<MediaType> produces = Sets.newHashSet(method.produces);

                for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
                    for (final MediaType produce : method.produces) {
                        if (ModelProcessorUtil.isMethodOverridden(
                                resourceMethod,
                                method.httpMethod,
                                method.consumes.get(0),
                                produce)) {
                            produces.remove(produce);
                        }
                    }
                }

                if (!produces.isEmpty()) {
                    final Resource parentResource = resource.getParentResources().get(0);
                    if (parentResource != null && method.path != null) {
                        continue;
                    }

                    final Resource.Builder resourceBuilder = Resource.builder(firstResource.getPath());
                    final Resource.Builder builder = method.path != null
                            ? resourceBuilder.addChildResource(method.path) : resourceBuilder;
                    final ResourceMethod.Builder methodBuilder = builder
                            .addMethod(method.httpMethod)
                            .consumes(method.consumes)
                            .produces(produces);

                    if (method.inflector != null) {
                        methodBuilder.handledBy(method.inflector);
                    } else {
                        methodBuilder.handledBy(method.inflectorClass);
                    }
                    methodBuilder.extended(extended);

                    final Resource newResource = resourceBuilder.build();
                    if (parentResource != null) {
                        final Resource.Builder parentBuilder = Resource.builder(parentResource.getPath());
                        parentBuilder.addChildResource(newResource);
                        enhancedModelBuilder.addResource(parentBuilder.build());
                    } else {
                        enhancedModelBuilder.addResource(newResource);
                    }
                }
            }
        }

        for (RuntimeResource child : resource.getChildRuntimeResources()) {
            enhanceResource(child, enhancedModelBuilder, methods, extended);
        }
    }

    /**
     * Determines whether the given methods can enhance the resource.
     *
     * @param resource resource to add the methods to.
     * @param methods methods to add.
     * @return {@code true} if methods can enhance the resource, {@code false} otherwise.
     */
    private static boolean methodsSuitableForResource(final Resource resource, final List<Method> methods) {
        if (!resource.getResourceMethods().isEmpty()) {
            return true;
        }

        // If there are no handler classes/instances we want to add only non-HEAD / non-OPTIONS methods.
        if (resource.getHandlerInstances().isEmpty() && resource.getHandlerClasses().isEmpty()) {
            for (final Method method : methods) {
                if (!HttpMethod.HEAD.equals(method.httpMethod) && !HttpMethod.OPTIONS.equals(method.httpMethod)) {
                    return true;
                }
            }
        }
        return false;
    }
}
