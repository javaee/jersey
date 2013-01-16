/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import com.google.common.collect.Sets;

/**
 * Helper class with methods supporting processing resource model by {@link org.glassfish.jersey.server.model.ModelProcessor
 * model processors}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ModelProcessorUtil {
    /**
     * Return allowed methods for the given {@code resource}. OPTIONS and HEAD are always returned in the result.
     *
     * @param resource Resource for which resource methods should be found.
     * @return Set of resource methods that can be invoked on the given resource.
     */
    public static Set<String> getAllowedMethods(RuntimeResource resource) {
        Set<String> allowedMethods = Sets.newHashSet();
        for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
            final String httpMethod = resourceMethod.getHttpMethod();
            allowedMethods.add(httpMethod);
        }
        allowedMethods.add(HttpMethod.OPTIONS);
        allowedMethods.add(HttpMethod.HEAD);
        return allowedMethods;
    }


    private static boolean isMethodOverriden(ResourceMethod resourceMethod, String httpMethod, MediaType consumes,
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
        private final MediaType consumes;
        private final MediaType produces;
        private final Class<? extends Inflector<ContainerRequestContext, Response>> inflector;


        /**
         * Create new method instance.
         * @param httpMethod HTTP method (eg. GET, POST, OPTIONS).
         * @param consumes Consumed media type.
         * @param produces Produces media type.
         * @param inflector Inflector handling the resource method.
         */
        public Method(String httpMethod, MediaType consumes, MediaType produces,
                      Class<? extends Inflector<ContainerRequestContext, Response>> inflector) {
            this.httpMethod = httpMethod;
            this.consumes = consumes;
            this.produces = produces;
            this.inflector = inflector;
        }
    }

    /**
     * Enhance {@code resourceModel} by list of methods. The {@code resourceModel} is traversed and for each available endpoint
     * URI in the model {@code methods} are added. In case of method conflicts currently existing methods will
     * never be 'overriden' by any method from {@code methods}. Overriding check takes into account media types of methods so
     * that new resource methods with same HTTP method can define only more specific media type.
     *
     * @param resourceModel Resource model to be enhanced.
     * @param subResourceModel {@code true} if the {@code resourceModel} to be enhanced is a sub resource model, {@code false}
     *                                     if it is application resource model.
     * @param methods List of enhancing methods.
     * @return New resource model builder enhanced by {@code methods}.
     */
    public static ResourceModel.Builder enhanceResourceModel(ResourceModel resourceModel, boolean subResourceModel,
                                                             List<Method> methods) {
        ResourceModel.Builder newModelBuilder = new ResourceModel.Builder(resourceModel, subResourceModel);

        for (RuntimeResource resource : resourceModel.getRuntimeResourceModel().getRuntimeResources()) {
            enhanceResource(resource, newModelBuilder, methods);
        }
        return newModelBuilder;
    }

    private static void enhanceResource(RuntimeResource resource, ResourceModel.Builder newModelBuilder, List<Method> methods) {

        if (resource.getResourceMethods().size() > 0) {
            for (Method method : methods) {
                boolean found = false;
                for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
                    if (ModelProcessorUtil.isMethodOverriden(resourceMethod, method.httpMethod, method.consumes,
                            method.produces)) {
                        found = true;
                    }
                }
                if (!found) {
                    final Resource firstResource = resource.getResources().get(0);
                    final Resource.Builder resourceBuilder = Resource.builder(firstResource.getPath());
                    resourceBuilder.addMethod(method.httpMethod).consumes(method.consumes).produces(method.produces)
                            .handledBy(method.inflector).build();

                    final Resource newResource = resourceBuilder.build();
                    final Resource parentResource = resource.getParentResources().get(0);
                    if (parentResource != null) {
                        final Resource.Builder parentBuilder = Resource.builder(parentResource.getPath());
                        parentBuilder.addChildResource(newResource);
                        newModelBuilder.addResource(parentBuilder.build());
                    } else {
                        newModelBuilder.addResource(newResource);
                    }
                }
            }
        }

        for (RuntimeResource child : resource.getChildRuntimeResources()) {
            enhanceResource(child, newModelBuilder, methods);
        }
    }
}
