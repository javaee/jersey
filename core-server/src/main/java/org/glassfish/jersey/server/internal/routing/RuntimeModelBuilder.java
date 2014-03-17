/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import javax.inject.Inject;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.internal.routing.Routers.RootRouteBuilder;
import org.glassfish.jersey.server.internal.routing.Routers.RouteBuilder;
import org.glassfish.jersey.server.internal.routing.Routers.RouteToPathBuilder;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.server.model.RuntimeResourceModel;
import org.glassfish.jersey.uri.PathPattern;
import org.glassfish.jersey.uri.UriTemplate;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * This is a common base for root resource and sub-resource runtime model
 * builder.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public final class RuntimeModelBuilder {
    private final RootRouteBuilder<PathPattern> rootBuilder;
    private final ResourceMethodInvoker.Builder resourceMethodInvokerBuilder;
    private final ServiceLocator locator;
    private final MessageBodyWorkers workers;
    private ProcessingProviders processingProviders;

    /**
     * Injection constructor.
     *
     * @param resourceMethodInvokerBuilder   method invoker builder.
     * @param locator                        HK2 service locator.
     * @param workers                        message body workers.
     */
    @Inject
    public RuntimeModelBuilder(
            final ResourceMethodInvoker.Builder resourceMethodInvokerBuilder,
            final ServiceLocator locator,
            final MessageBodyWorkers workers) {
        this.resourceMethodInvokerBuilder = resourceMethodInvokerBuilder;
        this.locator = locator;
        this.workers = workers;
        this.rootBuilder = new PathPatternRouteBuilder(locator);
    }

    private Router createMethodAcceptor(final ResourceMethod resourceMethod) {
        Router methodAcceptor = null;
        switch (resourceMethod.getType()) {
            case RESOURCE_METHOD:
            case SUB_RESOURCE_METHOD:
                methodAcceptor = Routers.asTreeAcceptor(createInflector(resourceMethod));
                break;
            case SUB_RESOURCE_LOCATOR:
                methodAcceptor = new SubResourceLocatorRouter(locator, this, resourceMethod);
                break;
        }

        return new PushMethodHandlerRouter(resourceMethod.getInvocable().getHandler(), methodAcceptor);
    }


    private Inflector<RequestProcessingContext, ContainerResponse> createInflector(final ResourceMethod method) {

        return resourceMethodInvokerBuilder.build(
                method,
                processingProviders
        );
    }

    private Router createRootTreeAcceptor(RouteToPathBuilder<PathPattern> lastRoutedBuilder, boolean subResourceMode) {
        final Router routingRoot;
        if (lastRoutedBuilder != null) {
            routingRoot = lastRoutedBuilder.build();
        } else {
            /**
             * Create an empty routing root that accepts any request, does not do
             * anything and does not return any inflector. This will cause 404 being
             * returned for every request.
             */
            routingRoot = Routers.acceptingTree(new Function<RequestProcessingContext, RequestProcessingContext>() {

                @Override
                public RequestProcessingContext apply(RequestProcessingContext input) {
                    return input;
                }

            }).build();
        }

        if (subResourceMode) {
            return routingRoot;
        } else {
            return rootBuilder.root(routingRoot);
        }
    }

    private RouteToPathBuilder<PathPattern> routeMethodAcceptor(
            final RouteToPathBuilder<PathPattern> lastRoutedBuilder,
            final PathPattern pathPattern,
            final PushMatchedUriRouter uriPushingAcceptor,
            final PushMatchedRuntimeResourceRouter resourcePushingRouter,
            final Router methodAcceptor, boolean subResourceMode) {

        if (subResourceMode) {
            return routedBuilder(lastRoutedBuilder).route(pathPattern).to(resourcePushingRouter)
                    .to(methodAcceptor);
        } else {

            return routedBuilder(lastRoutedBuilder).route(pathPattern)
                    .to(uriPushingAcceptor)
                    .to(resourcePushingRouter)
                    .to(methodAcceptor);
        }
    }

    /**
     * Build a runtime model of routers based on the {@code resourceModel}.
     *
     * @param resourceModel   Resource model from which the runtime model should be built.
     * @param subResourceMode True if the {@code resourceModel} is a sub resource model returned from sub resource locator.
     * @return Root router of the router structure representing the resource model.
     */
    public Router buildModel(RuntimeResourceModel resourceModel, final boolean subResourceMode) {
        final List<RuntimeResource> runtimeResources = resourceModel.getRuntimeResources();

        final PushMatchedUriRouter uriPushingRouter = new PushMatchedUriRouter();
        RouteToPathBuilder<PathPattern> lastRoutedBuilder = null;

        // route methods
        for (RuntimeResource resource : runtimeResources) {
            PushMatchedRuntimeResourceRouter resourcePushingRouter = new PushMatchedRuntimeResourceRouter(resource);

            // resource methods
            if (!resource.getResourceMethods().isEmpty()) {
                final List<MethodAcceptorPair> methodAcceptors = createAcceptors(resource, subResourceMode);
                final PathPattern resourceClosedPattern =
                        (subResourceMode) ? PathPattern.END_OF_PATH_PATTERN : PathPattern.asClosed(resource.getPathPattern());

                lastRoutedBuilder = routeMethodAcceptor(
                        lastRoutedBuilder,
                        resourceClosedPattern,
                        uriPushingRouter,
                        resourcePushingRouter,
                        new MethodSelectingRouter(workers, methodAcceptors),
                        subResourceMode);
            }

            RouteToPathBuilder<PathPattern> srRoutedBuilder = null;
            if (!resource.getChildRuntimeResources().isEmpty()) {
                for (RuntimeResource child : resource.getChildRuntimeResources()) {
                    final PathPattern childOpenPattern = child.getPathPattern();
                    final PathPattern childClosedPattern = PathPattern.asClosed(childOpenPattern);
                    PushMatchedRuntimeResourceRouter childResourcePushingRouter =
                            new PushMatchedRuntimeResourceRouter(child);

                    // sub resource methods
                    if (!child.getResourceMethods().isEmpty()) {
                        final List<MethodAcceptorPair> childMethodAcceptors = createAcceptors(child, subResourceMode);

                        srRoutedBuilder = routedBuilder(srRoutedBuilder)
                                .route(childClosedPattern)
                                .to(uriPushingRouter)
                                .to(childResourcePushingRouter)
                                .to(new MethodSelectingRouter(workers, childMethodAcceptors));
                    }

                    // sub resource locator
                    if (child.getResourceLocator() != null) {
                        PushMatchedTemplateRouter locTemplateRouter = getTemplateRouterForChildLocator(subResourceMode, child);

                        srRoutedBuilder = routedBuilder(srRoutedBuilder)
                                .route(childOpenPattern)
                                .to(uriPushingRouter)
                                .to(locTemplateRouter)
                                .to(childResourcePushingRouter)
                                .to(new PushMatchedMethodRouter(child.getResourceLocator()))
                                .to(createMethodAcceptor(child.getResourceLocator()));
                    }
                }
            }

            // resource locator with empty path
            if (resource.getResourceLocator() != null) {
                final PushMatchedTemplateRouter resourceTemplateRouter = getTemplateRouter(subResourceMode,
                        getLocatorResource(resource).getPathPattern().getTemplate(),
                        PathPattern.OPEN_ROOT_PATH_PATTERN.getTemplate());

                srRoutedBuilder = routedBuilder(srRoutedBuilder)
                        .route(PathPattern.OPEN_ROOT_PATH_PATTERN)
                        .to(uriPushingRouter)
                        .to(resourceTemplateRouter)
                        .to(new PushMatchedMethodRouter(resource.getResourceLocator()))
                        .to(createMethodAcceptor(resource.getResourceLocator()));
            }

            if (srRoutedBuilder != null) {
                final PathPattern resourceOpenPattern =
                        (subResourceMode) ? PathPattern.OPEN_ROOT_PATH_PATTERN : resource.getPathPattern();

                lastRoutedBuilder = routeMethodAcceptor(
                        lastRoutedBuilder, resourceOpenPattern, uriPushingRouter, resourcePushingRouter,
                        srRoutedBuilder.build(), subResourceMode);
            }
        }
        return createRootTreeAcceptor(lastRoutedBuilder, subResourceMode);
    }

    private PushMatchedTemplateRouter getTemplateRouterForChildLocator(boolean subResourceMode, RuntimeResource child) {
        int i = 0;
        for (Resource res : child.getResources()) {
            if (res.getResourceLocator() != null) {
                return getTemplateRouter(subResourceMode,
                        child.getParentResources().get(i).getPathPattern().getTemplate(),
                        res.getPathPattern().getTemplate());
            }
            i++;
        }
        return null;
    }


    private PushMatchedTemplateRouter getTemplateRouter(boolean subResourceMode, UriTemplate parentTemplate,
                                                        UriTemplate childTemplate) {
        if (childTemplate != null) {
            return new PushMatchedTemplateRouter(
                    subResourceMode ? PathPattern.OPEN_ROOT_PATH_PATTERN.getTemplate() : parentTemplate,
                    childTemplate);
        } else {
            return new PushMatchedTemplateRouter(
                    subResourceMode ? PathPattern.END_OF_PATH_PATTERN.getTemplate() : parentTemplate);
        }
    }


    private Resource getLocatorResource(RuntimeResource resource) {
        for (Resource res : resource.getResources()) {
            if (res.getResourceLocator() != null) {
                return res;
            }
        }
        return null;
    }


    private List<MethodAcceptorPair> createAcceptors(RuntimeResource runtimeResource, boolean subResourceMode) {
        List<MethodAcceptorPair> acceptorPairList = Lists.newArrayList();
        int i = 0;
        for (Resource resource : runtimeResource.getResources()) {

            final Resource parentResource = runtimeResource.getParent() == null
                    ? null : runtimeResource.getParentResources().get(i++);

            final UriTemplate template = resource.getPathPattern().getTemplate();

            final PushMatchedTemplateRouter templateRouter = parentResource == null ?
                    getTemplateRouter(subResourceMode, template, null)
                    : getTemplateRouter(subResourceMode, parentResource.getPathPattern().getTemplate(), template);

            for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
                acceptorPairList.add(new MethodAcceptorPair(resourceMethod,
                        templateRouter,
                        new PushMatchedMethodRouter(resourceMethod),
                        createMethodAcceptor(resourceMethod)));
            }
        }
        return acceptorPairList;
    }

    private RouteBuilder<PathPattern> routedBuilder(RouteToPathBuilder<PathPattern> lastRoutedBuilder) {
        return lastRoutedBuilder == null ? rootBuilder : lastRoutedBuilder;
    }

    /**
     * Set {@link ProcessingProviders processing providers}.
     *
     * @param processingProviders processing providers.
     */
    public void setProcessingProviders(ProcessingProviders processingProviders) {
        this.processingProviders = processingProviders;
    }
}
