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

package org.glassfish.jersey.server.mvc.internal;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.server.model.internal.ModelHelper;
import org.glassfish.jersey.server.model.internal.ModelProcessorUtil;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.ResolvedViewable;

import com.google.common.collect.Lists;

/**
 * {@link ModelProcessor Model processor} enhancing (sub-)resources with {@value HttpMethod#GET} methods responsible of producing
 * implicit {@link Viewable viewables}.
 * <p/>
 * Note: Resource classes has to be annotated with {@link Template} annotation in order to be enhanced by this model processor.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 * @see Template
 */
class TemplateModelProcessor implements ModelProcessor {

    /**
     * Path parameter representing implicit template name.
     */
    private static final String IMPLICIT_VIEW_PATH_PARAMETER = "implicit-view-path-parameter";
    private static final String IMPLICIT_VIEW_PATH_PARAMETER_TEMPLATE = "{" + IMPLICIT_VIEW_PATH_PARAMETER + "}";

    private final ResourceContext resourceContext;

    private final Provider<ExtendedUriInfo> extendedUriInfoProvider;

    /**
     * Inflector producing response with {@link ResolvedViewable resolved viewable} where model is the resource class annotated
     * with {@link Template} or 404 as its status code.
     */
    private class TemplateInflector implements Inflector<ContainerRequestContext, Response> {

        private final String templateName;
        private final Class<?> resolvingClass;

        private final Class<?> resourceClass;
        private final Object resourceInstance;

        /**
         * Create enhancing template {@link Inflector inflector} method.
         *
         * @param templateName template name for the produced {@link Viewable viewable}.
         * @param resolvingClass resolving class of for the produced {@link Viewable viewable}
         * @param resourceClass model class for the produced {@link Viewable viewable}. Should not be {@code null}.
         * @param resourceInstance model for the produced {@link Viewable viewable}. May be {@code null}.
         */
        private TemplateInflector(final String templateName, final Class<?> resolvingClass,
                                  final Class<?> resourceClass, final Object resourceInstance) {
            this.templateName = templateName;
            this.resolvingClass = resolvingClass;

            this.resourceClass = resourceClass;
            this.resourceInstance = resourceInstance;
        }

        @Override
        public Response apply(ContainerRequestContext requestContext) {
            final ExtendedUriInfo extendedUriInfo = extendedUriInfoProvider.get();

            final List<String> templateNames = getTemplateNames(requestContext);

            final Object model = getModel(extendedUriInfo);
            final Class<?> resolvingClass = Object.class.equals(this.resolvingClass) || this.resolvingClass == null
                    ? resourceClass : this.resolvingClass;

            return Response.ok().entity(new ImplicitViewable(templateNames, model, resolvingClass)).build();
        }

        /**
         * Obtains a model object for a viewable.
         *
         * @param extendedUriInfo uri info to obtain last matched resource from.
         * @return a model object.
         */
        private Object getModel(final ExtendedUriInfo extendedUriInfo) {
            final List<Object> matchedResources = extendedUriInfo.getMatchedResources();

            if (resourceInstance != null) {
                return resourceInstance;
            } else if (matchedResources.size() > 1) {
                return matchedResources.get(1);
            } else {
                return resourceContext.getResource(resourceClass);
            }
        }

        /**
         * Returns a list of template names to be considered as candidates for resolving {@link Viewable viewable} into {@link
         * ResolvedViewable resolved viewable}.
         * <p/>
         * Order of template names to be resolved is as follows:
         * <ul>
         *     <li>{{@value #IMPLICIT_VIEW_PATH_PARAMETER}} value</li>
         *     <li>{@link org.glassfish.jersey.server.mvc.Template#name()}</li>
         *     <li>last sub-resource locator path</li>
         *     <li>index</li>
         * </ul>
         *
         * @param requestContext request context to obtain {@link #IMPLICIT_VIEW_PATH_PARAMETER} value from.
         * @return a non-empty list of template names.
         */
        private List<String> getTemplateNames(final ContainerRequestContext requestContext) {
            final List<String> templateNames = Lists.newArrayList();

            // Template name extracted from path param.
            final String pathTemplate = requestContext.getUriInfo().getPathParameters().getFirst(IMPLICIT_VIEW_PATH_PARAMETER);
            if (pathTemplate != null) {
                templateNames.add(pathTemplate);
            }

            // Annotation.
            if (this.templateName != null && !"".equals(this.templateName)) {
                templateNames.add(this.templateName);
            }

            // Sub-resource path.
            final ExtendedUriInfo uriInfo = extendedUriInfoProvider.get();
            final List<RuntimeResource> matchedRuntimeResources = uriInfo.getMatchedRuntimeResources();
            if (matchedRuntimeResources.size() > 1) {
                // > 1 to check that we matched sub-resource
                final RuntimeResource lastMatchedRuntimeResource = matchedRuntimeResources.get(0);
                final Resource lastMatchedResource = lastMatchedRuntimeResource.getResources().get(0);

                String path = lastMatchedResource.getPath();

                if (path != null && !IMPLICIT_VIEW_PATH_PARAMETER_TEMPLATE.equals(path)) {
                    path = path.charAt(0) == '/' ? path.substring(1, path.length()) : path;
                    templateNames.add(path);
                }
            }

            // Index.
            if (templateNames.isEmpty()) {
                templateNames.add("index");
            }

            return templateNames;
        }
    }

    /**
     * Create a {@code TemplateModelProcessor} instance.
     *
     * @param resourceContext (injected) resource context.
     * @param extendedUriInfoProvider (injected) extended uri info provider.
     */
    @Inject
    TemplateModelProcessor(final ResourceContext resourceContext, final Provider<ExtendedUriInfo> extendedUriInfoProvider) {
        this.resourceContext = resourceContext;
        this.extendedUriInfoProvider = extendedUriInfoProvider;
    }

    @Override
    public ResourceModel processResourceModel(final ResourceModel resourceModel, final Configuration configuration) {
        return processModel(resourceModel, false);
    }

    @Override
    public ResourceModel processSubResource(final ResourceModel subResourceModel, final Configuration configuration) {
        return processModel(subResourceModel, true);
    }

    /**
     * Enhance {@link RuntimeResource runtime resources} of given {@link ResourceModel resource model} with methods obtained with
     * {@link #getEnhancingMethods(org.glassfish.jersey.server.model.RuntimeResource)}.
     *
     * @param resourceModel resource model to enhance runtime resources of.
     * @param subResourceModel determines whether the resource model represents sub-resource.
     * @return enhanced resource model.
     */
    private ResourceModel processModel(final ResourceModel resourceModel, final boolean subResourceModel) {
        ResourceModel.Builder newModelBuilder = new ResourceModel.Builder(resourceModel, subResourceModel);

        for (RuntimeResource resource : resourceModel.getRuntimeResourceModel().getRuntimeResources()) {
            ModelProcessorUtil.enhanceResource(resource, newModelBuilder, getEnhancingMethods(resource));
        }
        return newModelBuilder.build();
    }

    /**
     * Returns a list of enhancing methods for a given {@link RuntimeResource runtime resource}.
     *
     * @param runtimeResource runtime resource to create enhancing methods for.
     * @return list of enhancing methods.
     */
    private List<ModelProcessorUtil.Method> getEnhancingMethods(final RuntimeResource runtimeResource) {
        final List<ModelProcessorUtil.Method> newMethods = Lists.newArrayList();

        for (final Resource resource : runtimeResource.getResources()) {
            // Handler classes.
            for (final Class<?> handlerClass : resource.getHandlerClasses()) {
                createEnhancingMethods(handlerClass, null, newMethods);
            }

            // Names - if there are no handler classes / instances.
            if (resource.getHandlerClasses().isEmpty() && resource.getHandlerInstances().isEmpty()) {
                for (String resourceName : resource.getNames()) {
                    final Class<Object> resourceClass = ReflectionHelper.classForName(resourceName);
                    if (resourceClass != null) {
                        createEnhancingMethods(resourceClass, null, newMethods);
                    }
                }
            }

            // Handler instances.
            Errors.process(new Producer<Void>() {
                @Override
                public Void call() {
                    for (final Object handlerInstance : resource.getHandlerInstances()) {
                        final Class<?> handlerInstanceClass = handlerInstance.getClass();

                        if (!resource.getHandlerClasses().contains(handlerInstanceClass)) {
                            createEnhancingMethods(handlerInstanceClass, handlerInstance, newMethods);
                        } else {
                            Errors.warning(resource, LocalizationMessages.TEMPLATE_HANDLER_ALREADY_ENHANCED(handlerInstanceClass));
                        }
                    }

                    return null;
                }
            });
        }

        return newMethods;
    }

    /**
     * Creates enhancing methods for given resource.
     *
     * @param resourceClass resource class for which enhancing methods should be created.
     * @param resourceInstance resource instance for which enhancing methods should be created. May be {@code null}.
     * @param newMethods list to store new methods into.
     */
    private void createEnhancingMethods(final Class<?> resourceClass, final Object resourceInstance,
                                        final List<ModelProcessorUtil.Method> newMethods) {
        final Template template = resourceClass.getAnnotation(Template.class);

        if (template != null) {
            final Class<?> annotatedResourceClass = ModelHelper.getAnnotatedResourceClass(resourceClass);

            final List<MediaType> produces = MediaTypes
                    .createQualitySourceMediaTypes(annotatedResourceClass.getAnnotation(Produces.class));
            final List<MediaType> consumes = MediaTypes.createFrom(annotatedResourceClass.getAnnotation(Consumes.class));

            final TemplateInflector inflector = new TemplateInflector(template.name(), template.resolvingClass(),
                    resourceClass, resourceInstance);

            newMethods.add(new ModelProcessorUtil.Method(HttpMethod.GET, consumes, produces, inflector));
            newMethods.add(new ModelProcessorUtil.Method(IMPLICIT_VIEW_PATH_PARAMETER_TEMPLATE, HttpMethod.GET,
                    consumes, produces, inflector));
        }
    }
}
