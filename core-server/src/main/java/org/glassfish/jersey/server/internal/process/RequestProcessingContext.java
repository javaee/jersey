/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.process;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.process.internal.ChainableStage;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.server.CloseableService;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.monitoring.RequestEventBuilder;
import org.glassfish.jersey.server.internal.routing.RoutingContext;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.base.Function;

/**
 * Request processing context.
 *
 * Serves as a hub for all request processing related information and is being passed between stages.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
// TODO replace also ContainerResponse in stages with this guy.
public final class RequestProcessingContext implements RespondingContext {

    private final ServiceLocator serviceLocator;

    private final ContainerRequest request;
    private final UriRoutingContext routingContext;
    private final RespondingContext respondingContext;
    private final CloseableService closeableService;

    private final RequestEventBuilder monitoringEventBuilder;
    private final RequestEventListener monitoringEventListener;

    private final Ref<Value<AsyncContext>> asyncContextValueRef;

    /**
     * Create new request processing context.
     *
     * @param serviceLocator          service locator / injector.
     * @param request                 container request.
     * @param routingContext          routing context.
     * @param monitoringEventBuilder  request monitoring event builder.
     * @param monitoringEventListener registered request monitoring event listener.
     */
    public RequestProcessingContext(
            final ServiceLocator serviceLocator,
            final ContainerRequest request,
            final UriRoutingContext routingContext,
            final RequestEventBuilder monitoringEventBuilder,
            final RequestEventListener monitoringEventListener) {
        this.serviceLocator = serviceLocator;

        this.request = request;
        this.routingContext = routingContext;
        this.respondingContext = new DefaultRespondingContext();
        this.closeableService = new DefaultCloseableService();

        this.monitoringEventBuilder = monitoringEventBuilder;
        this.monitoringEventListener = monitoringEventListener;

        this.asyncContextValueRef = Refs.threadSafe(Values.<AsyncContext>empty());
    }

    /**
     * Get the processed container request.
     *
     * @return processed container request.
     */
    public ContainerRequest request() {
        return request;
    }

    /**
     * Get the routing context for the processed container request.
     *
     * @return request routing context.
     */
    public RoutingContext routingContext() {
        return routingContext;
    }

    /**
     * Get the underlying {@link UriRoutingContext} instance for the processed
     * container request.
     * <p>
     * This instance is used  by {@link ServerProcessingBinder} to satisfy injection of multiple types, namely:
     * <ul>
     * <li>{@link javax.ws.rs.core.UriInfo}<li>
     * </li>{@link org.glassfish.jersey.server.ExtendedUriInfo}<li>
     * </li>{@link javax.ws.rs.container.ResourceInfo}</li>
     * </ul>
     * </p>
     *
     * @return request routing context.
     */
    UriRoutingContext uriRoutingContext() {
        return routingContext;
    }

    /**
     * Get closeable service associated with the request.
     *
     * @return closeable service associated with the request.
     */
    public CloseableService closeableService() {
        return closeableService;
    }


    /**
     * Lazily initialize {@link org.glassfish.jersey.server.internal.process.AsyncContext} for this
     * request processing context.
     * <p>
     * The {@code lazyContextValue} will be only invoked once during the first call to {@link #asyncContext()}.
     * As such, the asynchronous context for this request can be initialized lazily, on demand.
     * </p>
     *
     * @param lazyContextValue lazily initialized {@code AsyncContext} instance bound to this request processing context.
     */
    // TODO figure out how to make this package-private.
    public void initAsyncContext(Value<AsyncContext> lazyContextValue) {
        asyncContextValueRef.set(Values.lazy(lazyContextValue));
    }

    /**
     * Get the asynchronous context associated with this request processing context.
     *
     * May return {@code null} if no asynchronous context has been initialized in this request processing context yet.
     *
     * @return asynchronous context associated with this request processing context, or {@code null} if the
     * asynchronous context has not been initialized yet
     * (see {@link #initAsyncContext(org.glassfish.jersey.internal.util.collection.Value)}).
     */
    public AsyncContext asyncContext() {
        return asyncContextValueRef.get().get();
    }

    /**
     * Get a {@link Value} instance holding the asynchronous context associated with this request processing context.
     *
     * May return an empty value if no asynchronous context has been initialized in this request processing context yet.
     *
     * @return value instance holding the asynchronous context associated with this request processing context.
     * The returned value may be empty, if no asynchronous context has been initialized yet
     * (see {@link #initAsyncContext(org.glassfish.jersey.internal.util.collection.Value)}).
     */
    public Value<AsyncContext> asyncContextValue() {
        return asyncContextValueRef.get();
    }

    /**
     * Get service locator.
     *
     * The returned instance is application-scoped.
     *
     * @return application-scoped service locator.
     */
    public ServiceLocator serviceLocator() {
        return serviceLocator;
    }

    /**
     * Get request monitoring event builder.
     *
     * @return request monitoring event builder.
     */
    // TODO perhaps this method can be completely removed or replaced by setting values on the context directly?
    public RequestEventBuilder monitoringEventBuilder() {
        return monitoringEventBuilder;
    }

    /**
     * Trigger a new monitoring event for the currently processed request.
     *
     * @param eventType request event type.
     */
    public void triggerEvent(RequestEvent.Type eventType) {
        if (monitoringEventListener != null) {
            monitoringEventListener.onEvent(monitoringEventBuilder.build(eventType));
        }
    }

    @Override
    public void push(final Function<ContainerResponse, ContainerResponse> responseTransformation) {
        respondingContext.push(responseTransformation);
    }

    @Override
    public void push(final ChainableStage<ContainerResponse> stage) {
        respondingContext.push(stage);
    }

    @Override
    public Stage<ContainerResponse> createRespondingRoot() {
        return respondingContext.createRespondingRoot();
    }
}
