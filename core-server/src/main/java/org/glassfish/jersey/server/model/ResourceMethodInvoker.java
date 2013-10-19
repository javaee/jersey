/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.NameBound;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.process.AsyncContext;
import org.glassfish.jersey.server.internal.process.Endpoint;
import org.glassfish.jersey.server.internal.process.RespondingContext;
import org.glassfish.jersey.server.internal.routing.RoutingContext;
import org.glassfish.jersey.server.model.internal.ResourceMethodDispatcherFactory;
import org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.cache.Cache;
import org.glassfish.hk2.utilities.cache.Computable;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Server-side request-response {@link Inflector inflector} for invoking methods
 * of annotation-based resource classes.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class ResourceMethodInvoker implements Endpoint, ResourceInfo {

    private final Provider<RoutingContext> routingContextProvider;
    private final Provider<AsyncContext> asyncContextProvider;
    private final Provider<RespondingContext> respondingContextProvider;
    private final ResourceMethod method;
    private final ResourceMethodDispatcher dispatcher;
    private final Method resourceMethod;
    private final Class<?> resourceClass;
    private final List<RankedProvider<ContainerRequestFilter>> requestFilters = Lists.newArrayList();
    private final List<RankedProvider<ContainerResponseFilter>> responseFilters = Lists.newArrayList();
    private final Iterable<ReaderInterceptor> readerInterceptors;
    private final Iterable<WriterInterceptor> writerInterceptors;

    /**
     * Resource method invoker "assisted" injection helper.
     *
     * The injectable builder API provides means for constructing a properly
     * injected {@link ResourceMethodInvoker resource method invoker} instances.
     */
    public static class Builder {

        @Inject
        private Provider<RoutingContext> routingContextProvider;
        @Inject
        private Provider<AsyncContext> asyncContextProvider;
        @Inject
        private Provider<RespondingContext> respondingContextProvider;
        @Inject
        private ResourceMethodDispatcherFactory dispatcherProviderFactory;
        @Inject
        private ResourceMethodInvocationHandlerFactory invocationHandlerProviderFactory;
        @Inject
        private ServiceLocator locator;
        @Inject
        private Configuration globalConfig;

        /**
         * Build a new resource method invoker instance.
         *
         * @param method              resource method model.
         * @param processingProviders Processing providers.
         * @return new resource method invoker instance.
         */
        public ResourceMethodInvoker build(
                ResourceMethod method,
                ProcessingProviders processingProviders
        ) {
            return new ResourceMethodInvoker(
                    routingContextProvider,
                    asyncContextProvider,
                    respondingContextProvider,
                    dispatcherProviderFactory,
                    invocationHandlerProviderFactory,
                    method,
                    processingProviders,
                    locator,
                    globalConfig);
        }
    }

    private ResourceMethodInvoker(
            Provider<RoutingContext> routingContextProvider,
            Provider<AsyncContext> asyncContextProvider,
            Provider<RespondingContext> respondingContextProvider,
            ResourceMethodDispatcher.Provider dispatcherProvider,
            ResourceMethodInvocationHandlerProvider invocationHandlerProvider,
            final ResourceMethod method,
            ProcessingProviders processingProviders,
            ServiceLocator locator,
            Configuration globalConfig) {

        this.routingContextProvider = routingContextProvider;
        this.asyncContextProvider = asyncContextProvider;
        this.respondingContextProvider = respondingContextProvider;

        this.method = method;
        final Invocable invocable = method.getInvocable();
        this.dispatcher = dispatcherProvider.create(invocable,
                invocationHandlerProvider.create(invocable));

        this.resourceMethod = invocable.getHandlingMethod();
        this.resourceClass = invocable.getHandler().getHandlerClass();

        // Configure dynamic features.
        final ResourceMethodConfig config = new ResourceMethodConfig(globalConfig.getProperties());
        for (final DynamicFeature dynamicFeature : processingProviders.getDynamicFeatures()) {
            dynamicFeature.configure(this, config);
        }

        final ComponentBag componentBag = config.getComponentBag();
        final List<Object> providers = Lists.newArrayList(componentBag.getInstances(ComponentBag.EXCLUDE_META_PROVIDERS));

        // Get instances of providers.
        final Set<Class<?>> providerClasses = componentBag.getClasses(ComponentBag.EXCLUDE_META_PROVIDERS);
        if (!providerClasses.isEmpty()) {
            locator = Injections.createLocator(locator, new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(config).to(Configuration.class);
                }
            });

            for (final Class<?> providerClass : providerClasses) {
                providers.add(locator.createAndInitialize(providerClass));
            }
        }

        final List<RankedProvider<ReaderInterceptor>> _readerInterceptors = Lists.newLinkedList();
        final List<RankedProvider<WriterInterceptor>> _writerInterceptors = Lists.newLinkedList();
        final List<RankedProvider<ContainerRequestFilter>> _requestFilters = Lists.newLinkedList();
        final List<RankedProvider<ContainerResponseFilter>> _responseFilters = Lists.newLinkedList();

        for (final Object provider : providers) {
            final ContractProvider model = componentBag.getModel(provider.getClass());
            final Set<Class<?>> contracts = model.getContracts();

            if (contracts.contains(WriterInterceptor.class)) {
                _writerInterceptors.add(
                        new RankedProvider<WriterInterceptor>(
                                (WriterInterceptor) provider,
                                model.getPriority(WriterInterceptor.class)));
            }

            if (contracts.contains(ReaderInterceptor.class)) {
                _readerInterceptors.add(
                        new RankedProvider<ReaderInterceptor>(
                                (ReaderInterceptor) provider,
                                model.getPriority(ReaderInterceptor.class)));
            }

            if (contracts.contains(ContainerRequestFilter.class)) {
                _requestFilters.add(
                        new RankedProvider<ContainerRequestFilter>(
                                (ContainerRequestFilter) provider,
                                model.getPriority(ContainerRequestFilter.class)));
            }

            if (contracts.contains(ContainerResponseFilter.class)) {
                _responseFilters.add(
                        new RankedProvider<ContainerResponseFilter>(
                                (ContainerResponseFilter) provider,
                                model.getPriority(ContainerResponseFilter.class)));
            }
        }

        _readerInterceptors.addAll(Lists.newLinkedList(processingProviders.getGlobalReaderInterceptors()));
        _writerInterceptors.addAll(Lists.newLinkedList(processingProviders.getGlobalWriterInterceptors()));

        if (resourceMethod != null) {
            addNameBoundFiltersAndInterceptors(
                    processingProviders,
                    _requestFilters, _responseFilters, _readerInterceptors, _writerInterceptors,
                    method);
        }

        this.readerInterceptors = Collections.unmodifiableList(Lists.newArrayList(Providers.sortRankedProviders(
                new RankedComparator<ReaderInterceptor>(), _readerInterceptors)));
        this.writerInterceptors = Collections.unmodifiableList(Lists.newArrayList(Providers.sortRankedProviders(
                new RankedComparator<WriterInterceptor>(), _writerInterceptors)));
        this.requestFilters.addAll(_requestFilters);
        this.responseFilters.addAll(_responseFilters);
    }

    private <T> void addNameBoundProviders(Collection<RankedProvider<T>> targetCollection, final NameBound nameBound,
                                           final MultivaluedMap<Class<? extends Annotation>,
                                                   RankedProvider<T>> nameBoundProviders, final MultivaluedMap<RankedProvider<T>,
            Class<? extends Annotation>> nameBoundProvidersInverse) {
        MultivaluedMap<RankedProvider<T>, Class<? extends Annotation>> foundBindingsMap
                = new MultivaluedHashMap<RankedProvider<T>, Class<? extends Annotation>>();
        for (Class<? extends Annotation> nameBinding : nameBound.getNameBindings()) {
            Iterable<RankedProvider<T>> providers = nameBoundProviders.get(nameBinding);
            if (providers != null) {
                for (RankedProvider<T> provider : providers) {
                    foundBindingsMap.add(provider, nameBinding);
                }
            }
        }

        for (Map.Entry<RankedProvider<T>, List<Class<? extends Annotation>>> entry : foundBindingsMap.entrySet()) {
            final RankedProvider<T> provider = entry.getKey();
            final List<Class<? extends Annotation>> foundBindings = entry.getValue();
            final List<Class<? extends Annotation>> providerBindings = nameBoundProvidersInverse.get(provider);
            if (foundBindings.size() == providerBindings.size()) {
                targetCollection.add(provider);
            }
        }
    }

    private void addNameBoundFiltersAndInterceptors(
            final ProcessingProviders processingProviders,
            final Collection<RankedProvider<ContainerRequestFilter>> targetRequestFilters,
            final Collection<RankedProvider<ContainerResponseFilter>> targetResponseFilters,
            final Collection<RankedProvider<ReaderInterceptor>> targetReaderInterceptors,
            final Collection<RankedProvider<WriterInterceptor>> targetWriterInterceptors,
            final NameBound nameBound
    ) {
        addNameBoundProviders(targetRequestFilters, nameBound, processingProviders.getNameBoundRequestFilters(),
                processingProviders.getNameBoundRequestFiltersInverse());
        addNameBoundProviders(targetResponseFilters, nameBound, processingProviders.getNameBoundResponseFilters(),
                processingProviders.getNameBoundResponseFiltersInverse());
        addNameBoundProviders(targetReaderInterceptors, nameBound, processingProviders.getNameBoundReaderInterceptors(),
                processingProviders.getNameBoundReaderInterceptorsInverse());
        addNameBoundProviders(targetWriterInterceptors, nameBound, processingProviders.getNameBoundWriterInterceptors(),
                processingProviders.getNameBoundWriterInterceptorsInverse());
    }


    @Override
    public Method getResourceMethod() {
        return resourceMethod;
    }

    @Override
    public Class<?> getResourceClass() {
        return resourceClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ContainerResponse apply(final ContainerRequest requestContext) {
        final Object resource = routingContextProvider.get().peekMatchedResource();

        if (method.isSuspendDeclared() || method.isManagedAsyncDeclared()) {
            asyncContextProvider.get().suspend();
        }

        if (method.isManagedAsyncDeclared()) {
            asyncContextProvider.get().invokeManaged(new Producer<Response>() {
                @Override
                public Response call() {
                    final Response response = invoke(requestContext, resource);
                    if (method.isSuspendDeclared()) {
                        // we ignore any response returned from a method that injects AsyncResponse
                        return null;
                    }
                    return response;
                }
            });
            return null; // return null on current thread
        } else {
            return new ContainerResponse(requestContext, invoke(requestContext, resource));
        }
    }

    private static final Cache<Method, Annotation[]> methodAnnotationCache = new Cache<Method, Annotation[]>(new Computable<Method, Annotation[]>() {

        @Override
        public Annotation[] compute(Method m) {
            return m.getDeclaredAnnotations();
        }
    });

    private Response invoke(ContainerRequest requestContext, Object resource) {

        Response jaxrsResponse;
        requestContext.triggerEvent(RequestEvent.Type.RESOURCE_METHOD_START);

        try {
            jaxrsResponse = dispatcher.dispatch(resource, requestContext);
        } finally {
            requestContext.triggerEvent(RequestEvent.Type.RESOURCE_METHOD_FINISHED);
        }

        if (jaxrsResponse == null) {
            jaxrsResponse = Response.noContent().build();
        }

        respondingContextProvider.get().push(new Function<ContainerResponse, ContainerResponse>() {
            @Override
            public ContainerResponse apply(final ContainerResponse response) {
                if (response == null) {
                    return response;
                }

                final Invocable invocable = method.getInvocable();
                final Annotation[] entityAnn = response.getEntityAnnotations();
                final Annotation[] methodAnn = methodAnnotationCache.compute(invocable.getHandlingMethod());
                if (methodAnn.length > 0) {
                    if (entityAnn.length == 0) {
                        response.setEntityAnnotations(methodAnn);
                    } else {
                        Annotation[] mergedAnn = Arrays.copyOf(methodAnn, methodAnn.length + entityAnn.length);
                        System.arraycopy(entityAnn, 0, mergedAnn, methodAnn.length, entityAnn.length);
                        response.setEntityAnnotations(mergedAnn);
                    }
                }

                if (response.hasEntity() && !(response.getEntityType() instanceof ParameterizedType)) {
                    Type invocableType = invocable.getResponseType();
                    if (invocableType != null &&
                            Void.TYPE != invocableType &&
                            Void.class != invocableType &&
                            // Do NOT change the entity type for Response or it's subclasses.
                            (!(invocableType instanceof Class) || !Response.class.isAssignableFrom((Class) invocableType))) {
                        response.setEntityType(invocableType);
                    }
                }
                return response;

            }
        });

        return jaxrsResponse;
    }

    /**
     * Get all bound request filters applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     *
     * @return All bound (dynamically or by name) request filters applicable to the {@link #getResourceMethod() resource
     *         method}.
     */
    public Iterable<RankedProvider<ContainerRequestFilter>> getRequestFilters() {
        return requestFilters;
    }

    /**
     * Get all bound response filters applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     *
     * @return All bound (dynamically or by name) response filters applicable to the {@link #getResourceMethod() resource
     *         method}.
     */
    public Iterable<RankedProvider<ContainerResponseFilter>> getResponseFilters() {
        return responseFilters;
    }

    /**
     * Get all reader interceptors applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     *
     * @return All reader interceptors applicable to the {@link #getResourceMethod() resource method}.
     */
    public Iterable<WriterInterceptor> getWriterInterceptors() {
        return writerInterceptors;
    }

    /**
     * Get all writer interceptors applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     *
     * @return All writer interceptors applicable to the {@link #getResourceMethod() resource method}.
     */
    public Iterable<ReaderInterceptor> getReaderInterceptors() {
        return readerInterceptors;
    }

    @Override
    public String toString() {
        return method.getInvocable().getHandlingMethod().toString();
    }
}
