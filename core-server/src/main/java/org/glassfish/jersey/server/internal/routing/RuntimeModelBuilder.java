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
package org.glassfish.jersey.server.internal.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.process.Endpoint;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.server.model.RuntimeResourceModel;
import org.glassfish.jersey.uri.PathPattern;
import org.glassfish.jersey.uri.UriTemplate;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * This is a common base for root resource and sub-resource runtime model
 * builder.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
final class RuntimeModelBuilder {

    private final ResourceMethodInvoker.Builder resourceMethodInvokerBuilder;
    private final MessageBodyWorkers workers;
    private final ProcessingProviders processingProviders;

    // SubResourceLocator Model Builder.
    private final Value<RuntimeLocatorModelBuilder> locatorBuilder;

    /**
     * Create a new instance of the runtime model builder.
     *
     * @param locator                      HK2 service locator.
     * @param resourceContext              Jersey resource context.
     * @param config                       configuration of the application.
     * @param workers                      message body workers.
     * @param processingProviders          processing providers.
     * @param resourceMethodInvokerBuilder method invoker builder.
     */
    public RuntimeModelBuilder(
            final ServiceLocator locator,
            final JerseyResourceContext resourceContext,
            final Configuration config,
            final MessageBodyWorkers workers,
            final ProcessingProviders processingProviders,
            final ResourceMethodInvoker.Builder resourceMethodInvokerBuilder) {

        this.resourceMethodInvokerBuilder = resourceMethodInvokerBuilder;
        this.workers = workers;
        this.processingProviders = processingProviders;

        this.locatorBuilder = Values.lazy(new Value<RuntimeLocatorModelBuilder>() {
            @Override
            public RuntimeLocatorModelBuilder get() {
                return new RuntimeLocatorModelBuilder(locator, config, resourceContext, RuntimeModelBuilder.this);
            }
        });
    }

    private Router createMethodRouter(final ResourceMethod resourceMethod) {
        Router methodAcceptor = null;
        switch (resourceMethod.getType()) {
            case RESOURCE_METHOD:
            case SUB_RESOURCE_METHOD:
                methodAcceptor = Routers.endpoint(createInflector(resourceMethod));
                break;
            case SUB_RESOURCE_LOCATOR:
                methodAcceptor = locatorBuilder.get().getRouter(resourceMethod);
                break;
        }

        return new PushMethodHandlerRouter(resourceMethod.getInvocable().getHandler(), methodAcceptor);
    }


    private Endpoint createInflector(final ResourceMethod method) {

        return resourceMethodInvokerBuilder.build(
                method,
                processingProviders
        );
    }

    private Router createRootRouter(final PathMatchingRouterBuilder lastRoutedBuilder, final boolean subResourceMode) {
        final Router routingRoot;
        if (lastRoutedBuilder != null) {
            routingRoot = lastRoutedBuilder.build();
        } else {
            /**
             * Create an empty routing root that accepts any request, does not do
             * anything and does not return any inflector. This will cause 404 being
             * returned for every request.
             */
            routingRoot = Routers.noop();
        }

        if (subResourceMode) {
            return routingRoot;
        } else {
            return new MatchResultInitializerRouter(routingRoot);
        }
    }

    /**
     * Build a runtime model of routers based on the {@code resourceModel}.
     *
     * @param resourceModel   Resource model from which the runtime model should be built.
     * @param subResourceMode True if the {@code resourceModel} is a sub resource model returned from sub resource locator.
     * @return Root router of the router structure representing the resource model.
     */
    public Router buildModel(final RuntimeResourceModel resourceModel, final boolean subResourceMode) {
        final List<RuntimeResource> runtimeResources = resourceModel.getRuntimeResources();

        final PushMatchedUriRouter uriPushingRouter = new PushMatchedUriRouter();
        PathMatchingRouterBuilder currentRouterBuilder = null;

        // route methods
        for (final RuntimeResource resource : runtimeResources) {
            final PushMatchedRuntimeResourceRouter resourcePushingRouter = new PushMatchedRuntimeResourceRouter(resource);

            // resource methods
            if (!resource.getResourceMethods().isEmpty()) {
                final List<MethodRouting> methodRoutings = createResourceMethodRouters(resource, subResourceMode);
                final Router methodSelectingRouter = new MethodSelectingRouter(workers, methodRoutings);
                if (subResourceMode) {
                    currentRouterBuilder = startNextRoute(currentRouterBuilder, PathPattern.END_OF_PATH_PATTERN)
                            .to(resourcePushingRouter)
                            .to(methodSelectingRouter);
                } else {
                    currentRouterBuilder = startNextRoute(currentRouterBuilder, PathPattern.asClosed(resource.getPathPattern()))
                            .to(uriPushingRouter)
                            .to(resourcePushingRouter)
                            .to(methodSelectingRouter);
                }
            }

            PathMatchingRouterBuilder srRoutedBuilder = null;
            if (!resource.getChildRuntimeResources().isEmpty()) {
                for (final RuntimeResource childResource : resource.getChildRuntimeResources()) {
                    final PathPattern childOpenPattern = childResource.getPathPattern();
                    final PathPattern childClosedPattern = PathPattern.asClosed(childOpenPattern);
                    final PushMatchedRuntimeResourceRouter childResourcePushingRouter =
                            new PushMatchedRuntimeResourceRouter(childResource);

                    // sub resource methods
                    if (!childResource.getResourceMethods().isEmpty()) {
                        final List<MethodRouting> childMethodRoutings =
                                createResourceMethodRouters(childResource, subResourceMode);

                        srRoutedBuilder = startNextRoute(srRoutedBuilder, childClosedPattern)
                                .to(uriPushingRouter)
                                .to(childResourcePushingRouter)
                                .to(new MethodSelectingRouter(workers, childMethodRoutings));
                    }

                    // sub resource locator
                    if (childResource.getResourceLocator() != null) {
                        final PushMatchedTemplateRouter locTemplateRouter =
                                getTemplateRouterForChildLocator(subResourceMode, childResource);

                        srRoutedBuilder = startNextRoute(srRoutedBuilder, childOpenPattern)
                                .to(uriPushingRouter)
                                .to(locTemplateRouter)
                                .to(childResourcePushingRouter)
                                .to(new PushMatchedMethodRouter(childResource.getResourceLocator()))
                                .to(createMethodRouter(childResource.getResourceLocator()));
                    }
                }
            }

            // resource locator with empty path
            if (resource.getResourceLocator() != null) {
                final PushMatchedTemplateRouter resourceTemplateRouter = getTemplateRouter(subResourceMode,
                        getLocatorResource(resource).getPathPattern().getTemplate(),
                        PathPattern.OPEN_ROOT_PATH_PATTERN.getTemplate());

                srRoutedBuilder = startNextRoute(srRoutedBuilder, PathPattern.OPEN_ROOT_PATH_PATTERN)
                        .to(uriPushingRouter)
                        .to(resourceTemplateRouter)
                        .to(new PushMatchedMethodRouter(resource.getResourceLocator()))
                        .to(createMethodRouter(resource.getResourceLocator()));
            }

            if (srRoutedBuilder != null) {
                final Router methodRouter = srRoutedBuilder.build();

                if (subResourceMode) {
                    currentRouterBuilder = startNextRoute(currentRouterBuilder, PathPattern.OPEN_ROOT_PATH_PATTERN)
                            .to(resourcePushingRouter)
                            .to(methodRouter);
                } else {
                    currentRouterBuilder = startNextRoute(currentRouterBuilder, resource.getPathPattern())
                            .to(uriPushingRouter)
                            .to(resourcePushingRouter)
                            .to(methodRouter);
                }
            }
        }
        return createRootRouter(currentRouterBuilder, subResourceMode);
    }

    private PushMatchedTemplateRouter getTemplateRouterForChildLocator(final boolean subResourceMode,
                                                                       final RuntimeResource child) {
        int i = 0;
        for (final Resource res : child.getResources()) {
            if (res.getResourceLocator() != null) {
                return getTemplateRouter(subResourceMode,
                        child.getParentResources().get(i).getPathPattern().getTemplate(),
                        res.getPathPattern().getTemplate());
            }
            i++;
        }
        return null;
    }


    private PushMatchedTemplateRouter getTemplateRouter(final boolean subResourceMode, final UriTemplate parentTemplate,
                                                        final UriTemplate childTemplate) {
        if (childTemplate != null) {
            return new PushMatchedTemplateRouter(
                    subResourceMode ? PathPattern.OPEN_ROOT_PATH_PATTERN.getTemplate() : parentTemplate,
                    childTemplate);
        } else {
            return new PushMatchedTemplateRouter(
                    subResourceMode ? PathPattern.END_OF_PATH_PATTERN.getTemplate() : parentTemplate);
        }
    }


    private Resource getLocatorResource(final RuntimeResource resource) {
        for (final Resource res : resource.getResources()) {
            if (res.getResourceLocator() != null) {
                return res;
            }
        }
        return null;
    }

    private List<MethodRouting> createResourceMethodRouters(
            final RuntimeResource runtimeResource, final boolean subResourceMode) {

        final List<MethodRouting> methodRoutings = new ArrayList<>();
        int i = 0;
        for (final Resource resource : runtimeResource.getResources()) {

            final Resource parentResource = runtimeResource.getParent() == null
                    ? null : runtimeResource.getParentResources().get(i++);

            final UriTemplate template = resource.getPathPattern().getTemplate();

            final PushMatchedTemplateRouter templateRouter = parentResource == null
                    ? getTemplateRouter(subResourceMode, template, null)
                    : getTemplateRouter(subResourceMode, parentResource.getPathPattern().getTemplate(), template);

            for (final ResourceMethod resourceMethod : resource.getResourceMethods()) {
                methodRoutings.add(new MethodRouting(resourceMethod,
                        templateRouter,
                        new PushMatchedMethodRouter(resourceMethod),
                        createMethodRouter(resourceMethod)));
            }
        }
        return methodRoutings.isEmpty() ? Collections.<MethodRouting>emptyList() : methodRoutings;
    }

    private PathToRouterBuilder startNextRoute(final PathMatchingRouterBuilder currentRouterBuilder, PathPattern routingPattern) {
        return currentRouterBuilder == null
                ? PathMatchingRouterBuilder.newRoute(routingPattern) : currentRouterBuilder.route(routingPattern);
    }
}
