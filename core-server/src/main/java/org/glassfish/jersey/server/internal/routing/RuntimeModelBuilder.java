/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.routing.RouterBinder.RootRouteBuilder;
import org.glassfish.jersey.server.internal.routing.RouterBinder.RouteBuilder;
import org.glassfish.jersey.server.internal.routing.RouterBinder.RouteToPathBuilder;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.server.model.RuntimeResourceModel;
import org.glassfish.jersey.uri.PathPattern;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

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
    private final PushMethodHandlerRouter.Builder pushHandlerAcceptorBuilder;
    private final MethodSelectingRouter.Builder methodSelectingAcceptorBuilder;
    private final MessageBodyWorkers workers;
    private final PushMatchedMethodResourceRouter.Builder pushedMatchedMethodResourceBuilder;
    private final PushMatchedRuntimeResourceRouter.Builder pushedMatchedRuntimeResourceBuilder;
    private ProcessingProviders processingProviders;

    /**
     * Injection constructor.
     *
     * @param rootBuilder root router builder.
     * @param resourceMethodInvokerBuilder method invoker builder.
     * @param locator HK2 service locator.
     * @param pushHandlerAcceptorBuilder push handler acceptor builder.
     * @param methodSelectingAcceptorBuilder method selecting acceptor builder.
     * @param pushedMatchedMethodResourceBuilder push matched method and resource builder.
     * @param pushedMatchedRuntimeResourceBuilder push matched runtime resource builder.
     * @param workers message body workers.
     */
    @Inject
    public RuntimeModelBuilder(
            final RootRouteBuilder<PathPattern> rootBuilder,
            final ResourceMethodInvoker.Builder resourceMethodInvokerBuilder,
            final ServiceLocator locator,
            final PushMethodHandlerRouter.Builder pushHandlerAcceptorBuilder,
            final MethodSelectingRouter.Builder methodSelectingAcceptorBuilder,
            final PushMatchedMethodResourceRouter.Builder pushedMatchedMethodResourceBuilder,
            final PushMatchedRuntimeResourceRouter.Builder pushedMatchedRuntimeResourceBuilder,
            final MessageBodyWorkers workers) {
        this.rootBuilder = rootBuilder;
        this.resourceMethodInvokerBuilder = resourceMethodInvokerBuilder;
        this.locator = locator;
        this.pushHandlerAcceptorBuilder = pushHandlerAcceptorBuilder;
        this.methodSelectingAcceptorBuilder = methodSelectingAcceptorBuilder;
        this.workers = workers;
        this.pushedMatchedMethodResourceBuilder = pushedMatchedMethodResourceBuilder;
        this.pushedMatchedRuntimeResourceBuilder = pushedMatchedRuntimeResourceBuilder;
    }

    private Router createSingleMethodAcceptor(final ResourceMethod resourceMethod) {
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

        return pushHandlerAcceptorBuilder.build(resourceMethod.getInvocable().getHandler(), methodAcceptor);
    }


    private Inflector<ContainerRequest, ContainerResponse> createInflector(final ResourceMethod method) {

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
            routingRoot = Routers.acceptingTree(new Function<ContainerRequest, ContainerRequest>() {

                @Override
                public ContainerRequest apply(ContainerRequest input) {
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
     * @param resourceModel Resource model from which the runtime model should be built.
     * @param subResourceMode True if the {@code resourceModel} is a sub resource model returned from sub resource locator.
     * @return Root router of the router structure representing the resource model.
     */
    public Router buildModel(RuntimeResourceModel resourceModel, final boolean subResourceMode) {
        final List<RuntimeResource> runtimeResources = resourceModel.getRuntimeResources();

        final PushMatchedUriRouter uriPushingRouter = locator.createAndInitialize(PushMatchedUriRouter.class);
        RouteToPathBuilder<PathPattern> lastRoutedBuilder = null;

        // route methods
        for (RuntimeResource resource : runtimeResources) {
            PushMatchedRuntimeResourceRouter resourcePushingRouter = pushedMatchedRuntimeResourceBuilder.build(resource);

            // resource methods
            if (resource.getResourceMethods().size() > 0) {
                final List<MethodAcceptorPair> methodAcceptors = createAcceptors(resource);
                final PathPattern resourceClosedPattern =
                        (subResourceMode) ? PathPattern.END_OF_PATH_PATTERN : PathPattern.asClosed(resource.getPathPattern());

                lastRoutedBuilder = routeMethodAcceptor(
                        lastRoutedBuilder,
                        resourceClosedPattern,
                        uriPushingRouter,
                        resourcePushingRouter,
                        methodSelectingAcceptorBuilder.build(workers, methodAcceptors), subResourceMode);
            }

            RouteToPathBuilder<PathPattern> srRoutedBuilder = null;
            if (resource.getChildRuntimeResources().size() > 0) {
                for (RuntimeResource child : resource.getChildRuntimeResources()) {
                    final PathPattern childOpenPattern = child.getPathPattern();
                    final PathPattern childClosedPattern = PathPattern.asClosed(childOpenPattern);
                    PushMatchedRuntimeResourceRouter childResourcePushingRouter =
                            pushedMatchedRuntimeResourceBuilder.build(child);

                    // sub resource methods
                    if (child.getResourceMethods().size() > 0) {
                        final List<MethodAcceptorPair> childMethodAcceptors = createAcceptors(child);

                        srRoutedBuilder = routedBuilder(srRoutedBuilder)
                                .route(childClosedPattern)
                                .to(uriPushingRouter)
                                .to(childResourcePushingRouter)
                                .to(methodSelectingAcceptorBuilder.build(workers, childMethodAcceptors));
                    }

                    // sub resource locator
                    if (child.getResourceLocator() != null) {
                        srRoutedBuilder = routedBuilder(srRoutedBuilder)
                                .route(childOpenPattern)
                                .to(uriPushingRouter)
                                .to(childResourcePushingRouter)
                                .to(createSingleMethodAcceptor(child.getResourceLocator()));
                    }
                }
            }

            // resource locator with empty path
            if (resource.getResourceLocator() != null) {
                srRoutedBuilder = routedBuilder(srRoutedBuilder)
                        .route(PathPattern.OPEN_ROOT_PATH_PATTERN)
                        .to(uriPushingRouter)
                        .to(createSingleMethodAcceptor(resource.getResourceLocator()));
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

    private List<MethodAcceptorPair> createAcceptors(RuntimeResource runtimeResource) {
        List<MethodAcceptorPair> acceptorPairList = Lists.newArrayList();
        for (Resource resource : runtimeResource.getResources()) {
            for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
                acceptorPairList.add(new MethodAcceptorPair(resourceMethod,
                        pushedMatchedMethodResourceBuilder.build(resource, resourceMethod),
                        createSingleMethodAcceptor(resourceMethod)));
            }

        }
        return acceptorPairList;
    }

    private RouteBuilder<PathPattern> routedBuilder(RouteToPathBuilder<PathPattern> lastRoutedBuilder) {
        return lastRoutedBuilder == null ? rootBuilder : lastRoutedBuilder;
    }

    /**
     * Set {@link ProcessingProviders processing providers}.
     * @param processingProviders processing providers.
     */
    public void setProcessingProviders(ProcessingProviders processingProviders) {
        this.processingProviders = processingProviders;
    }
}
