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
package org.glassfish.jersey.server.internal.routing;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Inject;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.routing.RouterBinder.RootRouteBuilder;
import org.glassfish.jersey.server.internal.routing.RouterBinder.RouteBuilder;
import org.glassfish.jersey.server.internal.routing.RouterBinder.RouteToPathBuilder;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.uri.PathPattern;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import deprecated.javax.ws.rs.DynamicBinder;

/**
 * This is a common base for root resource and sub-resource runtime model
 * builder.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class RuntimeModelBuilder {

    @Inject
    private RootRouteBuilder<PathPattern> rootBuilder;
    @Inject
    private ResourceMethodInvoker.Builder resourceMethodInvokerBuilder;
    @Inject
    private ServiceLocator locator;
    @Inject
    private PushMethodHandlerRouter.Builder pushHandlerAcceptorBuilder;
    @Inject
    private MethodSelectingRouter.Builder methodSelectingAcceptorBuilder;
    @Inject
    private MessageBodyWorkers workers;
    private MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters;
    private MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters;
    private Collection<ReaderInterceptor> globalReaderInterceptors;
    private Collection<WriterInterceptor> globalWriterInterceptors;
    private MultivaluedMap<Class<? extends Annotation>, ReaderInterceptor> nameBoundReaderInterceptors;
    private MultivaluedMap<Class<? extends Annotation>, WriterInterceptor> nameBoundWriterInterceptors;
    private List<DynamicBinder> dynamicBinders;

    /**
     * A sorted map of closed resource path patterns to the list of (root) resource method
     * acceptors registered for the patterns.
     */
    private TreeMap<PathPattern, List<MethodAcceptorPair>> rootAcceptors =
            Maps.newTreeMap(PathPattern.COMPARATOR);

    /**
     * A sorted map of open resource path patterns to the sub-resource method and locator
     * acceptors registered under the resource path patterns.
     * <p/>
     * The sub-resource method and locator acceptors are represented also by a
     * common sorted map of open method path pattern to the list of method acceptors
     * registered on that open pattern.
     * <p/>
     * Note that also sub-resource methods are mapped under an open pattern so that
     * the method path pattern (map key) comparison can work properly. The open
     * paths on the sub-resource methods are replaced with the correct closed ones
     * during the runtime creation model, before the method acceptors are routed.
     */
    private TreeMap<PathPattern, TreeMap<PathPattern, List<MethodAcceptorPair>>> subResourceAcceptors =
            Maps.newTreeMap(PathPattern.COMPARATOR);

    /**
     * Process a single resource model and add it to the currently build runtime
     * routing and accepting model.
     *
     * @param resource resource model to be processed.
     * @param subResourceMode if {@code true}, all resources will be processed as sub-resources.
     */
    public void process(final Resource resource, final boolean subResourceMode) {
        if (!(resource.isRootResource() || subResourceMode)) {
            // ignore sub-resources if not in a sub-resource modelling mode.
            return;
        }
        // prepare & add resource method acceptors
        if (!resource.getResourceMethods().isEmpty()) {
            final PathPattern closedResourcePathPattern =
                    (subResourceMode) ? PathPattern.END_OF_PATH_PATTERN : PathPattern.asClosed(resource.getPathPattern());

            List<MethodAcceptorPair> sameResourcePathList = rootAcceptors.get(closedResourcePathPattern);
            if (sameResourcePathList == null) {
                sameResourcePathList = Lists.newLinkedList();
                rootAcceptors.put(closedResourcePathPattern, sameResourcePathList);
            }

            sameResourcePathList.addAll(Lists.transform(resource.getResourceMethods(),
                    new Function<ResourceMethod, MethodAcceptorPair>() {
                        @Override
                        public MethodAcceptorPair apply(ResourceMethod methodModel) {
                            return new MethodAcceptorPair(methodModel, createSingleMethodAcceptor(methodModel, subResourceMode));
                        }
                    }));
        }

        // prepare & add sub-resource method and locator acceptors.
        if (resource.getSubResourceMethods().size() + resource.getSubResourceLocators().size() > 0) {
            final PathPattern openResourcePathPattern =
                    (subResourceMode) ? PathPattern.OPEN_ROOT_PATH_PATTERN : resource.getPathPattern();

            TreeMap<PathPattern, List<MethodAcceptorPair>> sameResourcePathMap =
                    subResourceAcceptors.get(openResourcePathPattern);
            if (sameResourcePathMap == null) {
                sameResourcePathMap = Maps.newTreeMap(PathPattern.COMPARATOR);
                subResourceAcceptors.put(openResourcePathPattern, sameResourcePathMap);
            }

            for (ResourceMethod methodModel : resource.getSubResourceMethods()) {
                updateSubResourceMethodMap(sameResourcePathMap, methodModel, subResourceMode);
            }
            for (ResourceMethod methodModel : resource.getSubResourceLocators()) {
                updateSubResourceMethodMap(sameResourcePathMap, methodModel, subResourceMode);
            }
        }
    }

    private void updateSubResourceMethodMap(
            final TreeMap<PathPattern, List<MethodAcceptorPair>> subResourceMethodMap, final ResourceMethod methodModel, boolean subResourceMode) {

        PathPattern openMethodPattern = new PathPattern(methodModel.getPath());

        List<MethodAcceptorPair> samePathMethodAcceptorPairs = subResourceMethodMap.get(openMethodPattern);
        if (samePathMethodAcceptorPairs == null) {
            samePathMethodAcceptorPairs = Lists.newLinkedList();
            subResourceMethodMap.put(openMethodPattern, samePathMethodAcceptorPairs);
        }
        samePathMethodAcceptorPairs.add(new MethodAcceptorPair(methodModel, createSingleMethodAcceptor(methodModel, subResourceMode)));
    }

    private Router createSingleMethodAcceptor(final ResourceMethod resourceMethod, boolean subResourceMode) {
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

        // TODO: solve this via instance-based method handler model?
        if (subResourceMode) {
            return methodAcceptor;
        } else {
            return pushHandlerAcceptorBuilder.build(resourceMethod.getInvocable().getHandler(), methodAcceptor);
        }
    }


    private Inflector<ContainerRequest, ContainerResponse> createInflector(
            final ResourceMethod method) {

        return resourceMethodInvokerBuilder.build(
                method,
                nameBoundRequestFilters,
                nameBoundResponseFilters,
                globalReaderInterceptors,
                globalWriterInterceptors,
                nameBoundReaderInterceptors,
                nameBoundWriterInterceptors,
                dynamicBinders
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
            final Router uriPushingAcceptor,
            final Router methodAcceptor, boolean subResourceMode) {

        if (subResourceMode) {
            return routedBuilder(lastRoutedBuilder).route(pathPattern)
                    .to(methodAcceptor);
        } else {
            return routedBuilder(lastRoutedBuilder).route(pathPattern)
                    .to(uriPushingAcceptor)
                    .to(methodAcceptor);
        }
    }

    /**
     * Build a runtime model.
     *
     * @param subResourceMode if {@code true}, all resources will be processed as sub-resources.
     * @return runtime request routing root.
     */
    public Router buildModel(boolean subResourceMode) {
        final PushMatchedUriRouter uriPushingRouter = locator.createAndInitialize(PushMatchedUriRouter.class);
        RouteToPathBuilder<PathPattern> lastRoutedBuilder = null;

        // route resource method acceptors
        if (!rootAcceptors.isEmpty()) {
            for (Map.Entry<PathPattern, List<MethodAcceptorPair>> entry : rootAcceptors.entrySet()) {
                final PathPattern closedResourcePathPattern = entry.getKey();
                List<MethodAcceptorPair> methodAcceptorPairs = entry.getValue();

                lastRoutedBuilder = routeMethodAcceptor(
                        lastRoutedBuilder,
                        closedResourcePathPattern,
                        uriPushingRouter,
                        methodSelectingAcceptorBuilder.build(workers, methodAcceptorPairs), subResourceMode);
            }
            rootAcceptors.clear();
        }

        // route sub-resource method and locator acceptors
        if (!subResourceAcceptors.isEmpty()) {
            for (Map.Entry<PathPattern, TreeMap<PathPattern, List<MethodAcceptorPair>>> singleResourcePathEntry
                    : subResourceAcceptors.entrySet()) {

                RouteToPathBuilder<PathPattern> srRoutedBuilder = null;
                for (Map.Entry<PathPattern, List<MethodAcceptorPair>> singlePathEntry :
                        singleResourcePathEntry.getValue().entrySet()) {

                    // there can be multiple sub-resource methods on the same path
                    // but only a single sub-resource locator.
                    List<MethodAcceptorPair> subResourceMethods = Lists.newLinkedList();
                    MethodAcceptorPair locator = null;

                    for (MethodAcceptorPair methodAcceptorPair : singlePathEntry.getValue()) {
                        if (methodAcceptorPair.model.getType() == ResourceMethod.JaxrsType.SUB_RESOURCE_METHOD) {
                            subResourceMethods.add(methodAcceptorPair);
                        } else {
                            locator = methodAcceptorPair;
                        }
                    }
                    if (!subResourceMethods.isEmpty()) {
                        final PathPattern subResourceMethodPath = PathPattern.asClosed(singlePathEntry.getKey());
                        srRoutedBuilder = routedBuilder(srRoutedBuilder).route(subResourceMethodPath)
                                .to(uriPushingRouter)
                                .to(methodSelectingAcceptorBuilder.build(workers, subResourceMethods));
                    }
                    if (locator != null) {
                        srRoutedBuilder = routedBuilder(srRoutedBuilder).route(singlePathEntry.getKey())
                                .to(uriPushingRouter)
                                .to(locator.router);
                    }
                }
                assert srRoutedBuilder != null;
                lastRoutedBuilder = routeMethodAcceptor(
                        lastRoutedBuilder, singleResourcePathEntry.getKey(), uriPushingRouter, srRoutedBuilder.build(), subResourceMode);
            }
            subResourceAcceptors.clear();
        }
        return createRootTreeAcceptor(lastRoutedBuilder, subResourceMode);
    }

    private RouteBuilder<PathPattern> routedBuilder(RouteToPathBuilder<PathPattern> lastRoutedBuilder) {
        return lastRoutedBuilder == null ? rootBuilder : lastRoutedBuilder;
    }

    /**
     * Set global reader and writer interceptors.
     *
     * @param readerInterceptors global reader interceptors.
     * @param writerInterceptors global writer interceptors.
     */
    public void setGlobalInterceptors (Collection<ReaderInterceptor> readerInterceptors, Collection<WriterInterceptor> writerInterceptors) {
        this.globalReaderInterceptors = readerInterceptors;
        this.globalWriterInterceptors = writerInterceptors;
    }

    /**
     * Set the name bound filters and dynamic binders.
     *
     * @param nameBoundRequestFilters name bound request filters.
     * @param nameBoundResponseFilters name bound response filters.
     * @param nameBoundReaderInterceptors name bound reader interceptors.
     * @param nameBoundWriterInterceptors name bound writer interceptors.
     * @param dynamicBinders dynamic binders.
     */
    public void setBoundProviders(
            MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters,
            MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters,
            MultivaluedMap<Class<? extends Annotation>, ReaderInterceptor> nameBoundReaderInterceptors,
            MultivaluedMap<Class<? extends Annotation>, WriterInterceptor> nameBoundWriterInterceptors,
            List<DynamicBinder> dynamicBinders
    ) {
        this.nameBoundRequestFilters = nameBoundRequestFilters;
        this.nameBoundResponseFilters = nameBoundResponseFilters;
        this.nameBoundReaderInterceptors = nameBoundReaderInterceptors;
        this.nameBoundWriterInterceptors = nameBoundWriterInterceptors;
        this.dynamicBinders = dynamicBinders;
    }
}
