/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.message.internal.ReaderInterceptorExecutor;
import org.glassfish.jersey.message.internal.WriterInterceptorExecutor;
import org.glassfish.jersey.model.NameBound;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.PriorityComparator;
import org.glassfish.jersey.server.internal.process.ProcessingContext;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.routing.RoutingContext;
import org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import deprecated.javax.ws.rs.DynamicBinder;

/**
 * Server-side request-response {@link Inflector inflector} for invoking methods
 * of annotation-based resource classes.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class ResourceMethodInvoker implements Inflector<ContainerRequest, ContainerResponse>, ResourceInfo {

    private final Provider<RoutingContext> routingContextFactory;
    private final Provider<ProcessingContext> invocationContextFactory;
    private final ResourceMethod method;
    private final ResourceMethodDispatcher dispatcher;
    private final Method resourceMethod;
    private final Class<?> resourceClass;
    private final Collection<ContainerRequestFilter> requestFilters = new HashSet<ContainerRequestFilter>();
    private final Collection<ContainerResponseFilter> responseFilters = new HashSet<ContainerResponseFilter>();
    private final List<ReaderInterceptor> readerInterceptors;
    private final List<WriterInterceptor> writerInterceptors;

    /**
     * Resource method invoker "assisted" injection helper.
     *
     * The injectable builder API provides means for constructing a properly
     * injected {@link ResourceMethodInvoker resource method invoker} instances.
     */
    public static class Builder {

        @Inject
        private Provider<RoutingContext> routingContextFactory;
        @Inject
        private Provider<ProcessingContext> invocationContextFactory;
        @Inject
        private ResourceMethodDispatcherFactory dispatcherProviderFactory;
        @Inject
        private ResourceMethodInvocationHandlerFactory invocationHandlerProviderFactory;

        /**
         * Build a new resource method invoker instance.
         *
         * @param method resource method model.
         * @param nameBoundRequestFilters name bound request filters.
         * @param nameBoundResponseFilters name bound response filters.
         * @param globalReaderInterceptors global reader interceptors.
         * @param globalWriterInterceptors global writer interceptors.
         * @param nameBoundReaderInterceptors name-bound reader interceptors.
         * @param nameBoundWriterInterceptors name-bound writer interceptors.
         * @param dynamicBinders dynamic binders.
         * @return new resource method invoker instance.
         */
        public ResourceMethodInvoker build(ResourceMethod method,
            MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters,
            MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters,
            Collection<ReaderInterceptor> globalReaderInterceptors,
            Collection<WriterInterceptor> globalWriterInterceptors,
            MultivaluedMap<Class<? extends Annotation>, ReaderInterceptor> nameBoundReaderInterceptors,
            MultivaluedMap<Class<? extends Annotation>, WriterInterceptor> nameBoundWriterInterceptors,
            Collection<DynamicBinder> dynamicBinders
        ) {
            return new ResourceMethodInvoker(
                    routingContextFactory,
                    invocationContextFactory,
                    dispatcherProviderFactory,
                    invocationHandlerProviderFactory,
                    method,
                    nameBoundRequestFilters,
                    nameBoundResponseFilters,
                    globalReaderInterceptors,
                    globalWriterInterceptors,
                    nameBoundReaderInterceptors,
                    nameBoundWriterInterceptors,
                    dynamicBinders);
        }
    }

    private ResourceMethodInvoker(
            Provider<RoutingContext> routingContextFactory,
            Provider<ProcessingContext> invocationContextFactory,
            ResourceMethodDispatcher.Provider dispatcherProvider,
            ResourceMethodInvocationHandlerProvider invocationHandlerProvider,
            ResourceMethod method,
            MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters,
            MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters,
            Collection<ReaderInterceptor> globalReaderInterceptors,
            Collection<WriterInterceptor> globalWriterInterceptors,
            MultivaluedMap<Class<? extends Annotation>, ReaderInterceptor> nameBoundReaderInterceptors,
            MultivaluedMap<Class<? extends Annotation>, WriterInterceptor> nameBoundWriterInterceptors,
            Collection<DynamicBinder> dynamicBinders) {

        this.routingContextFactory = routingContextFactory;
        this.invocationContextFactory = invocationContextFactory;

        this.method = method;
        final Invocable invocable = method.getInvocable();
        this.dispatcher = dispatcherProvider.create(invocable, invocationHandlerProvider.create(invocable));

        this.resourceMethod = invocable.getHandlingMethod();
        this.resourceClass = invocable.getHandler().getHandlerClass();

        List<ReaderInterceptor> _readerInterceptors = new LinkedList<ReaderInterceptor>();
        List<WriterInterceptor> _writerInterceptors = new LinkedList<WriterInterceptor>();

        for (DynamicBinder dynamicBinder : dynamicBinders) {
            Object boundProvider = dynamicBinder.getBoundProvider(this);

            // TODO: should be based on the type arg. value rather than instanceof?
            if (boundProvider instanceof WriterInterceptor) {
                _writerInterceptors.add((WriterInterceptor) boundProvider);
            }

            if (boundProvider instanceof ReaderInterceptor) {
                _readerInterceptors.add((ReaderInterceptor) boundProvider);
            }

            if (boundProvider instanceof ContainerRequestFilter) {
                this.requestFilters.add((ContainerRequestFilter) boundProvider);
            }

            if (boundProvider instanceof ContainerResponseFilter) {
                this.responseFilters.add((ContainerResponseFilter) boundProvider);
            }
        }

        _readerInterceptors.addAll(globalReaderInterceptors);
        _writerInterceptors.addAll(globalWriterInterceptors);

        if (resourceMethod != null) {
            addNameBoundFiltersAndInterceptors(
                    nameBoundRequestFilters, nameBoundResponseFilters, nameBoundReaderInterceptors, nameBoundWriterInterceptors,
                    this.requestFilters, this.responseFilters, _readerInterceptors, _writerInterceptors,
                    method);
        }

        Collections.sort(_readerInterceptors, new PriorityComparator<ReaderInterceptor>(PriorityComparator.Order.ASCENDING));
        Collections.sort(_writerInterceptors, new PriorityComparator<WriterInterceptor>(PriorityComparator.Order.ASCENDING));

        this.readerInterceptors = Collections.unmodifiableList(_readerInterceptors);
        this.writerInterceptors = Collections.unmodifiableList(_writerInterceptors);
    }

    private void addNameBoundFiltersAndInterceptors(
            final MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters,
            final MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters,
            final MultivaluedMap<Class<? extends Annotation>, ReaderInterceptor> nameBoundReaderInterceptors,
            final MultivaluedMap<Class<? extends Annotation>, WriterInterceptor> nameBoundWriterInterceptors,
            final Collection<ContainerRequestFilter> targetRequestFilters,
            final Collection<ContainerResponseFilter> targetResponseFilters,
            final Collection<ReaderInterceptor> targetReaderInterceptors,
            final Collection<WriterInterceptor> targetWriterInterceptors,
            final NameBound target
    ) {
        for (Class<? extends Annotation> nameBinding : target.getNameBindings()) {
            List<ContainerRequestFilter> reqF = nameBoundRequestFilters.get(nameBinding);
            if (reqF != null) {
                targetRequestFilters.addAll(reqF);
            }
            List<ContainerResponseFilter> resF = nameBoundResponseFilters.get(nameBinding);
            if (resF != null) {
                targetResponseFilters.addAll(resF);
            }
            List<ReaderInterceptor> _readerInterceptors = nameBoundReaderInterceptors.get(nameBinding);
            if (_readerInterceptors != null) {
                targetReaderInterceptors.addAll(_readerInterceptors);
            }
            List<WriterInterceptor> _writerInterceptors = nameBoundWriterInterceptors.get(nameBinding);
            if (_writerInterceptors != null) {
                targetWriterInterceptors.addAll(_writerInterceptors);
            }
        }
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
    public ContainerResponse apply(final ContainerRequest requestContext) {
        final Object resource = routingContextFactory.get().peekMatchedResource();

        final ProcessingContext processingCtx = invocationContextFactory.get();
        if (method.isSuspendDeclared()) {
            processingCtx.setTimeout(method.getSuspendTimeout(), method.getSuspendTimeoutUnit());
        }
        requestContext.setProperty(ReaderInterceptorExecutor.INTERCEPTORS, getReaderInterceptors());
        requestContext.setProperty(WriterInterceptorExecutor.INTERCEPTORS, getWriterInterceptors());
        final Response response = dispatcher.dispatch(resource, requestContext);

        if (method.isSuspendDeclared()) {
            processingCtx.setResponse(resource);
            processingCtx.trySuspend();
        }

        final ContainerResponse responseContext = new ContainerResponse(requestContext, response);
        final Invocable invocable = method.getInvocable();
        responseContext.setEntityAnnotations(invocable.getHandlingMethod().getDeclaredAnnotations());

        if (responseContext.hasEntity() && !(responseContext.getEntityType() instanceof ParameterizedType)) {
            Type invocableType = invocable.getResponseType();
            if (invocableType != null && Void.TYPE != invocableType && Void.class != invocableType && invocableType != Response.class) {
                responseContext.setEntityType(invocableType);
            }
        }

        return responseContext;
    }

    /**
     * Get all bound request filters applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     *
     * @return All bound (dynamically or by name) request filters applicable to the {@link #getResourceMethod() resource
     * method}.
     */
    public Collection<ContainerRequestFilter> getRequestFilters() {
        return requestFilters;
    }

    /**
     * Get all bound response filters applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     *
     * @return All bound (dynamically or by name) response filters applicable to the {@link #getResourceMethod() resource
     * method}.
     */
    public Collection<ContainerResponseFilter> getResponseFilters() {
        return responseFilters;
    }

    /**
     * Get all reader interceptors applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     *
     * @return All reader interceptors applicable to the {@link #getResourceMethod() resource method}.
     */
    public List<WriterInterceptor> getWriterInterceptors() {
        return writerInterceptors;
    }

    /**
     * Get all writer interceptors applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     *
     * @return All writer interceptors applicable to the {@link #getResourceMethod() resource method}.
     */
    public List<ReaderInterceptor> getReaderInterceptors() {
        return readerInterceptors;
    }

    @Override
    public String toString() {
        return method.getInvocable().getHandlingMethod().toString();
    }
}
