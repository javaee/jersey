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
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicBinder;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.InvocationContext;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.routing.RoutingContext;
import org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

/**
 * Server-side request-response {@link Inflector inflector} for invoking methods
 * of annotation-based resource classes.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class ResourceMethodInvoker implements Inflector<ContainerRequest, ContainerResponse>, ResourceInfo {

    private final Factory<RoutingContext> routingContextFactory;
    private final Factory<InvocationContext> invocationContextFactory;
    private final ResourceMethod method;
    private final ResourceMethodDispatcher dispatcher;
    private final Method resourceMethod;
    private final Class<?> resourceClass;
    private final Collection<ContainerRequestFilter> requestFilters = new HashSet<ContainerRequestFilter>();
    private final Collection<ContainerResponseFilter> responseFilters = new HashSet<ContainerResponseFilter>();
    private final Collection<ReaderInterceptor> readerInterceptors = new HashSet<ReaderInterceptor>();
    private final Collection<WriterInterceptor> writerInterceptors = new HashSet<WriterInterceptor>();

    /**
     * Resource method invoker "assisted" injection helper.
     *
     * The injectable builder API provides means for constructing a properly
     * injected {@link ResourceMethodInvoker resource method invoker} instances.
     */
    public static class Builder {

        @Inject
        private Factory<RoutingContext> routingContextFactory;
        @Inject
        private Factory<InvocationContext> invocationContextFactory;
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
         * @param dynamicBinders dynamic binders.
         * @return new resource method invoker instance.
         */
        public ResourceMethodInvoker build(ResourceMethod method,
            MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters,
            MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters,
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
                    dynamicBinders);
        }
    }

    private ResourceMethodInvoker(
            Factory<RoutingContext> routingContextFactory,
            Factory<InvocationContext> invocationContextFactory,
            ResourceMethodDispatcher.Provider dispatcherProvider,
            ResourceMethodInvocationHandlerProvider invocationHandlerProvider,
            ResourceMethod method,
            MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters,
            MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters,
            Collection<DynamicBinder> dynamicBinders) {
        this.routingContextFactory = routingContextFactory;
        this.invocationContextFactory = invocationContextFactory;

        this.method = method;
        final Invocable invocable = method.getInvocable();
        this.dispatcher = dispatcherProvider.create(invocable, invocationHandlerProvider.create(invocable));

        this.resourceMethod = invocable.getHandlingMethod();
        this.resourceClass = invocable.getHandler().getHandlerClass();

        for (DynamicBinder dynamicBinder : dynamicBinders) {
            Object boundProvider = dynamicBinder.getBoundProvider(this);

            // TODO: should be based on the type arg. value rather than instanceof?
            if (boundProvider instanceof WriterInterceptor) {
                writerInterceptors.add((WriterInterceptor) boundProvider);
            }

            if (boundProvider instanceof ReaderInterceptor) {
                readerInterceptors.add((ReaderInterceptor) boundProvider);
            }

            if (boundProvider instanceof ContainerRequestFilter) {
                this.requestFilters.add((ContainerRequestFilter) boundProvider);
            }

            if (boundProvider instanceof ContainerResponseFilter) {
                this.responseFilters.add((ContainerResponseFilter) boundProvider);
            }
        }

        if (resourceMethod != null) {
            addNameBoundFilters(nameBoundRequestFilters, nameBoundResponseFilters, method);
        }
        if (resourceClass != null) {
            addNameBoundFilters(nameBoundRequestFilters, nameBoundResponseFilters, method);
        }
    }

    private void addNameBoundFilters(
            MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters,
            MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters,
            ResourceMethod method
    ) {
        for (Class<? extends Annotation> nameBinding : method.getNameBindings()) {
            List<ContainerRequestFilter> reqF = nameBoundRequestFilters.get(nameBinding);
            if (reqF != null) {
                this.requestFilters.addAll(reqF);
            }
            List<ContainerResponseFilter> resF = nameBoundResponseFilters.get(nameBinding);
            if (resF != null) {
                this.responseFilters.addAll(resF);
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

        final InvocationContext invocationCtx = invocationContextFactory.get();
        if (method.isSuspendDeclared()) {
            invocationCtx.setSuspendTimeout(method.getSuspendTimeout(), method.getSuspendTimeoutUnit());
        }

        final Response response = dispatcher.dispatch(resource, requestContext);

        if (method.isSuspendDeclared()) {
            invocationCtx.setResponse(resource);
            invocationCtx.trySuspend();
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
     * Get all <b>dynamically</b> bound reader interceptors applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     * Note, this method does not return name-bound interceptors.
     *
     * @return All dynamically bound reader interceptors applicable to the {@link #getResourceMethod() resource method}.
     */
    public Collection<WriterInterceptor> getWriterInterceptors() {
        return writerInterceptors;
    }

    /**
     * Get all <b>dynamically</b> bound writer interceptors applicable to the {@link #getResourceMethod() resource method}
     * wrapped by this invoker.
     * Note, this method does not return name-bound interceptors.
     *
     * @return All dynamically bound writer interceptors applicable to the {@link #getResourceMethod() resource method}.
     */
    public Collection<ReaderInterceptor> getReaderInterceptors() {
        return readerInterceptors;
    }

    @Override
    public String toString() {
        return method.getInvocable().getHandlingMethod().toString();
    }
}
