/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.util.Closure;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.process.internal.ExecutorsFactory;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.BackgroundScheduler;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.monitoring.RequestEventBuilder;
import org.glassfish.jersey.server.internal.monitoring.RequestEventImpl;
import org.glassfish.jersey.server.internal.process.AsyncContext;
import org.glassfish.jersey.server.internal.process.Endpoint;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.internal.process.RespondingContext;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.spi.ExceptionMappers;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.COMPLETED;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.RESUMED;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.RUNNING;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.SUSPENDED;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.base.Preconditions;

/**
 * Server-side request processing runtime.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class ServerRuntime {
    private final Stage<ContainerRequest> requestProcessingRoot;

    private final ServiceLocator locator;

    private final ScheduledExecutorService backgroundScheduler;

    private final RequestScope requestScope;
    private final ExceptionMappers exceptionMappers;
    private final Provider<RespondingContext> respondingContextProvider;
    private final Provider<CloseableService> closeableServiceProvider;
    private final Provider<Ref<Value<AsyncContext>>> asyncContextFactoryProvider;
    private final Provider<AsyncContext> asyncContextProvider;
    private final Provider<UriRoutingContext> uriRoutingContextProvider;
    private final ExecutorsFactory<ContainerRequest> asyncExecutorsFactory;
    private final ApplicationEventListener applicationEventListener;
    private final Configuration configuration;

    /**
     * Server-side request processing runtime builder.
     */
    public static class Builder {
        @Inject
        private ServiceLocator locator;
        @Inject
        @BackgroundScheduler
        private ScheduledExecutorService backgroundScheduler;
        @Inject
        private RequestScope requestScope;
        @Inject
        private ExceptionMappers exceptionMappers;
        @Inject
        private Provider<RespondingContext> respondingContextProvider;
        @Inject
        private Provider<CloseableService> closeableServiceProvider;
        @Inject
        private Provider<Ref<Value<AsyncContext>>> asyncContextRefProvider;
        @Inject
        private Provider<AsyncContext> asyncContextProvider;
        @Inject
        private Provider<UriRoutingContext> uriRoutingContextProvider;
        @Inject
        private ExecutorsFactory<ContainerRequest> asyncExecutorsFactory;
        @Inject
        private Configuration configuration;

        /**
         * Create new server-side request processing runtime.
         *
         * @param requestProcessingRoot application request processing root stage.
         * @param eventListener         Application event listener registered for this runtime.
         * @return new server-side request processing runtime.
         */
        public ServerRuntime build(final Stage<ContainerRequest> requestProcessingRoot, ApplicationEventListener eventListener) {
            return new ServerRuntime(
                    requestProcessingRoot,
                    locator,
                    backgroundScheduler,
                    requestScope,
                    exceptionMappers,
                    respondingContextProvider,
                    closeableServiceProvider,
                    asyncContextRefProvider,
                    asyncContextProvider,
                    uriRoutingContextProvider,
                    asyncExecutorsFactory,
                    eventListener,
                    configuration);
        }
    }

    private ServerRuntime(Stage<ContainerRequest> requestProcessingRoot,
                          ServiceLocator locator,
                          ScheduledExecutorService backgroundScheduler,
                          RequestScope requestScope,
                          ExceptionMappers exceptionMappers,
                          Provider<RespondingContext> respondingContextProvider,
                          Provider<CloseableService> closeableServiceProvider,
                          Provider<Ref<Value<AsyncContext>>> asyncContextFactoryProvider,
                          Provider<AsyncContext> asyncContextProvider,
                          Provider<UriRoutingContext> uriRoutingContextProvider,
                          ExecutorsFactory<ContainerRequest> asyncExecutorsFactory,
                          ApplicationEventListener applicationEventListener,
                          Configuration configuration) {
        this.requestProcessingRoot = requestProcessingRoot;
        this.locator = locator;
        this.backgroundScheduler = backgroundScheduler;
        this.requestScope = requestScope;
        this.exceptionMappers = exceptionMappers;
        this.respondingContextProvider = respondingContextProvider;
        this.closeableServiceProvider = closeableServiceProvider;
        this.asyncContextFactoryProvider = asyncContextFactoryProvider;
        this.asyncContextProvider = asyncContextProvider;
        this.uriRoutingContextProvider = uriRoutingContextProvider;
        this.asyncExecutorsFactory = asyncExecutorsFactory;
        this.applicationEventListener = applicationEventListener;
        this.configuration = configuration;
    }

    /**
     * Process a container request.
     *
     * @param request request to be processed.
     */
    public void process(final ContainerRequest request) {
        initRequestEventListeners(request);

        try {
            request.checkState();
            requestScope.runInScope(new Runnable() {
                @Override
                public void run() {
                    final Responder responder = new Responder(request, ServerRuntime.this);
                    final AsyncResponderHolder asyncResponderHolder = new AsyncResponderHolder(
                            responder, requestScope.referenceCurrent());

                    try {
                        final Ref<Endpoint> endpointRef = Refs.emptyRef();
                        final ContainerRequest data = Stages.process(request, requestProcessingRoot, endpointRef);

                        final Endpoint endpoint = endpointRef.get();
                        if (endpoint == null) {
                            // not found
                            throw new NotFoundException();
                        }

                        asyncContextFactoryProvider.get().set(asyncResponderHolder);
                        final ContainerResponse response = endpoint.apply(data);

                        if (!asyncResponderHolder.isAsync()) {
                            responder.process(response);
                        }
                    } catch (Throwable throwable) {
                        responder.process(throwable);
                    } finally {
                        asyncResponderHolder.release();
                    }
                }
            });
        } finally {
            request.triggerEvent(RequestEvent.Type.FINISHED);
        }
    }

    private void initRequestEventListeners(ContainerRequest request) {
        if (applicationEventListener != null) {
            final RequestEventBuilder requestEventBuilder = new RequestEventImpl.Builder().setContainerRequest(request);
            final RequestEventListener requestEventEventListener =
                    applicationEventListener.onRequest(requestEventBuilder.build(RequestEvent.Type.START));

            if (requestEventEventListener != null) {
                request.setRequestEventListener(requestEventEventListener, requestEventBuilder);
            }
        }
    }

    /**
     * Get the Jersey server runtime background scheduler.
     *
     * @return server runtime background scheduler.
     * @see BackgroundScheduler
     */
    ScheduledExecutorService getBackgroundScheduler() {
        return backgroundScheduler;
    }

    private static class AsyncResponderHolder implements Value<AsyncContext> {

        private final Responder responder;
        private final RequestScope.Instance scopeInstance;

        private volatile AsyncResponder asyncResponder;

        private AsyncResponderHolder(Responder responder,
                                     RequestScope.Instance scopeInstance) {
            this.responder = responder;
            this.scopeInstance = scopeInstance;
        }

        @Override
        public AsyncResponder get() {
            final AsyncResponder ar = new AsyncResponder(responder, scopeInstance);
            asyncResponder = ar;
            return ar;
        }

        public boolean isAsync() {
            final AsyncResponder ar = asyncResponder;
            return ar != null && !ar.isRunning();
        }

        public void release() {
            if (asyncResponder == null) {
                scopeInstance.release();
            }
        }
    }

    private static class Responder {
        private static final Logger LOGGER = Logger.getLogger(Responder.class.getName());

        private final ContainerRequest request;
        private final ServerRuntime runtime;

        private final CompletionCallbackRunner completionCallbackRunner = new CompletionCallbackRunner();
        private final ConnectionCallbackRunner connectionCallbackRunner = new ConnectionCallbackRunner();


        public Responder(final ContainerRequest request, final ServerRuntime runtime) {
            this.request = request;
            this.runtime = runtime;
        }

        public void process(ContainerResponse response) {
            request.getRequestEventBuilder().setContainerResponse(response);
            response = processResponse(response);
            release(response);
        }

        private ContainerResponse processResponse(ContainerResponse response) {
            Stage<ContainerResponse> respondingRoot = runtime.respondingContextProvider.get().createRespondingRoot();

            if (respondingRoot != null) {
                response = Stages.process(response, respondingRoot);
            }
            writeResponse(response);

            // no-exception zone
            // the methods below are guaranteed to not throw any exceptions
            completionCallbackRunner.onComplete(null);
            return response;
        }

        public void process(Throwable throwable) {
            request.getRequestEventBuilder().setException(throwable, RequestEvent.ExceptionCause.ORIGINAL);
            request.triggerEvent(RequestEvent.Type.ON_EXCEPTION);

            ContainerResponse response = null;
            try {
                final Response exceptionResponse = mapException(throwable);
                try {
                    try {
                        response = convertResponse(exceptionResponse);
                        request.getRequestEventBuilder().setContainerResponse(response).setResponseSuccessfullyMapped(true);
                    } finally {
                        request.triggerEvent(RequestEvent.Type.EXCEPTION_MAPPING_FINISHED);
                    }

                    processResponse(response);
                } catch (Throwable respError) {
                    LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_PROCESSING_RESPONSE_FROM_ALREADY_MAPPED_EXCEPTION());
                    request.getRequestEventBuilder().setException(respError, RequestEvent.ExceptionCause.MAPPED_RESPONSE);
                    request.triggerEvent(RequestEvent.Type.ON_EXCEPTION);
                    throw respError;
                }
            } catch (Throwable responseError) {
                if (throwable != responseError
                        && !(throwable instanceof MappableException && throwable.getCause() == responseError)) {
                    LOGGER.log(Level.FINE, LocalizationMessages.ERROR_EXCEPTION_MAPPING_ORIGINAL_EXCEPTION(), throwable);
                }
                LOGGER.log(Level.FINE, LocalizationMessages.ERROR_EXCEPTION_MAPPING_THROWN_TO_CONTAINER(), responseError);

                try {
                    request.getResponseWriter().failure(responseError);
                } finally {
                    completionCallbackRunner.onComplete(responseError);
                }
            } finally {
                release(response);
            }
        }

        private ContainerResponse convertResponse(Response exceptionResponse) {
            final ContainerResponse containerResponse = new ContainerResponse(request, exceptionResponse);
            containerResponse.setMappedFromException(true);
            return containerResponse;
        }

        @SuppressWarnings("unchecked")
        private Response mapException(final Throwable originalThrowable) throws Throwable {
            Throwable throwable = originalThrowable;
            boolean inMappable = false;
            boolean mappingNotFound = false;

            do {
                if (throwable instanceof MappableException) {
                    inMappable = true;
                } else if (inMappable || throwable instanceof WebApplicationException) {
                    Response waeResponse = null;
                    if (throwable instanceof WebApplicationException) {
                        waeResponse = ((WebApplicationException) throwable).getResponse();
                        if (waeResponse.hasEntity()) {
                            return waeResponse;
                        }
                    }

                    ExceptionMapper mapper = runtime.exceptionMappers.findMapping(throwable);
                    if (mapper != null) {
                        request.getRequestEventBuilder().setExceptionMapper(mapper);
                        request.triggerEvent(RequestEvent.Type.EXCEPTION_MAPPER_FOUND);
                        try {
                            final Response mappedResponse = mapper.toResponse(throwable);
                            if (mappedResponse != null) {
                                // response successfully mapped
                                return mappedResponse;
                            } else {
                                return Response.noContent().build();
                            }
                        } catch (Throwable mapperThrowable) {
                            // spec: If the exception mapping provider throws an exception while creating a Response
                            // then return a server error (status code 500) response to the client.
                            LOGGER.log(Level.SEVERE, LocalizationMessages.EXCEPTION_MAPPER_THROWS_EXCEPTION(mapper.getClass()),
                                    mapperThrowable);
                            LOGGER.log(Level.SEVERE, LocalizationMessages.EXCEPTION_MAPPER_FAILED_FOR_EXCEPTION(), throwable);
                            return Response.serverError().build();
                        }
                    }

                    if (waeResponse != null) {
                        return waeResponse;
                    }

                    mappingNotFound = true;
                }
                // internal mapping
                if (throwable instanceof HeaderValueException) {
                    if (((HeaderValueException) throwable).getContext() == HeaderValueException.Context.INBOUND) {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                }

                if (!inMappable || mappingNotFound) {
                    // user failures (thrown from Resource methods or provider methods)

                    // spec: Unchecked exceptions and errors that have not been mapped MUST be re-thrown and allowed to
                    // propagate to the underlying container.

                    // not logged on this level.
                    throw throwable;
                }

                throwable = throwable.getCause();
            } while (throwable != null);
            // jersey failures (not thrown from Resource methods or provider methods) -> rethrow
            throw originalThrowable;
        }

        private ContainerResponse writeResponse(final ContainerResponse response) {
            final ContainerResponseWriter writer = request.getResponseWriter();

            if (!response.hasEntity()) {
                writer.writeResponseStatusAndHeaders(0, response);
                setWrittenResponse(response);
                return response;
            }

            final Object entity = response.getEntity();
            boolean skipFinally = false;

            final boolean isHead = request.getMethod().equals(HttpMethod.HEAD);


            try {

                response.setStreamProvider(new OutboundMessageContext.StreamProvider() {
                    @Override
                    public OutputStream getOutputStream(int contentLength) throws IOException {
                        final OutputStream outputStream = writer.writeResponseStatusAndHeaders(contentLength, response);
                        return isHead ? null : outputStream;
                    }
                });

                if ((writer.enableResponseBuffering() || isHead) && !response.isChunked()) {
                    response.enableBuffering(runtime.configuration);
                }

                try {
                    response.setEntityStream(request.getWorkers().writeTo(
                            entity,
                            entity.getClass(),
                            response.getEntityType(),
                            response.getEntityAnnotations(),
                            response.getMediaType(),
                            response.getHeaders(),
                            request.getPropertiesDelegate(),
                            response.getEntityStream(),
                            runtime.uriRoutingContextProvider.get().getBoundWriterInterceptors()));
                } catch (MappableException mpe) {
                    if (mpe.getCause() instanceof IOException) {
                        connectionCallbackRunner.onDisconnect(runtime.asyncContextProvider.get());
                    }
                    throw mpe;
                }
                setWrittenResponse(response);

            } catch (Throwable ex) {
                if (response.isCommitted()) {
                    /**
                     * We're done with processing here. There's nothing we can do about the exception so
                     * let's just log it.
                     */
                    LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_WRITING_RESPONSE_ENTITY(), ex);
                } else {
                    skipFinally = true;
                    if (ex instanceof RuntimeException) {
                        throw (RuntimeException) ex;
                    } else {
                        throw new MappableException(ex);
                    }
                }
            } finally {
                if (!skipFinally) {

                    if (response.isChunked()) {
                        try {
                            response.commitStream();
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_COMMITTING_OUTPUT_STREAM(), e);
                        }

                        try {
                            ((ChunkedOutput) entity).setContext(
                                    request,
                                    response,
                                    connectionCallbackRunner,
                                    runtime.asyncContextProvider,
                                    runtime.uriRoutingContextProvider.get());
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_WRITING_RESPONSE_ENTITY_CHUNK(), ex);
                        }
                        // suspend the writer indefinitely (passing null timeout handler is ok in such case).
                        writer.suspend(0, TimeUnit.SECONDS, null);
                        // TODO what to do if we detect that the writer has already been suspended? override the timeout value?
                    } else {
                        try {
                            // the response must be closed here instead of just flushed or committed. Some
                            // output streams writes out bytes only on close (for example GZipOutputStream).
                            response.close();
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_CLOSING_COMMIT_OUTPUT_STREAM(), e);
                        }
                    }
                }
            }

            return response;
        }

        private void setWrittenResponse(ContainerResponse response) {
            request.getRequestEventBuilder().setContainerResponse(response);
            request.getRequestEventBuilder().setSuccess(response.getStatus() < 400);
            request.getRequestEventBuilder().setResponseWritten(true);
        }

        private void release(ContainerResponse responseContext) {
            try {
                runtime.closeableServiceProvider.get().close();

                // Commit the container response writer if not in chunked mode
                // responseContext may be null in case the request processing was cancelled.
                if (responseContext != null && !responseContext.isChunked()) {
                    // responseContext.commitStream();
                    responseContext.close();
                }

            } catch (Throwable throwable) {
                // TODO L10N
                LOGGER.log(Level.WARNING, "Attempt to release single request processing resources has failed.", throwable);
            }
        }
    }

    private static class AsyncResponder implements AsyncContext, ContainerResponseWriter.TimeoutHandler, CompletionCallback {
        private static final Logger LOGGER = Logger.getLogger(AsyncResponder.class.getName());

        private static final TimeoutHandler DEFAULT_TIMEOUT_HANDLER = new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse asyncResponse) {
                throw new ServiceUnavailableException();
            }
        };

        private final Object stateLock = new Object();
        private State state = RUNNING;
        private boolean cancelled = false;

        private final Responder responder;
        private final RequestScope.Instance scopeInstance;

        private volatile TimeoutHandler timeoutHandler = DEFAULT_TIMEOUT_HANDLER;

        private final List<AbstractCallbackRunner<?>> callbackRunners;

        public AsyncResponder(final Responder responder,
                              final RequestScope.Instance scopeInstance) {
            this.responder = responder;
            this.scopeInstance = scopeInstance;

            this.callbackRunners = Collections.unmodifiableList(Arrays.asList(
                    responder.completionCallbackRunner, responder.connectionCallbackRunner));

            responder.completionCallbackRunner.register(this);
        }

        @Override
        public void onTimeout(ContainerResponseWriter responseWriter) {
            final TimeoutHandler handler = timeoutHandler;
            try {
                synchronized (stateLock) {
                    if (state == SUSPENDED) {
                        handler.handleTimeout(this);
                    }
                }
            } catch (Throwable throwable) {
                try {
                    resume(throwable);
                } catch (IllegalStateException ignored) {
                    // TODO remove the try-catch block once the resume API changes.
                    // ignore the exception - already resumed by someone else
                }
            }
        }

        @Override
        public void onComplete(final Throwable throwable) {
            synchronized (stateLock) {
                state = COMPLETED;
            }
        }

        @Override
        public void invokeManaged(final Producer<Response> producer) {
            responder.runtime.asyncExecutorsFactory.getRequestingExecutor(responder.request).submit(new Runnable() {
                @Override
                public void run() {
                    responder.runtime.requestScope.runInScope(scopeInstance, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final Response response = producer.call();
                                if (response != null) {
                                    resume(response);
                                }
                            } catch (Throwable t) {
                                resume(t);
                            }
                        }
                    });
                }
            });
        }

        @Override
        public boolean suspend() {
            synchronized (stateLock) {
                if (state == RUNNING) {
                    state = SUSPENDED;
                    responder.request.getResponseWriter().suspend(AsyncResponse.NO_TIMEOUT, TimeUnit.SECONDS, this);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean resume(final Object response) throws IllegalStateException {
            return resume(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Response jaxrsResponse = toJaxrsResponse(response);
                        responder.process(new ContainerResponse(responder.request, jaxrsResponse));
                    } catch (Throwable t) {
                        responder.process(t);
                    }
                }
            });
        }

        private Response toJaxrsResponse(final Object response) {
            if (response instanceof Response) {
                return (Response) response;
            } else {
                return Response.ok(response).build();
            }
        }

        @Override
        public boolean resume(final Throwable error) throws IllegalStateException {
            return resume(new Runnable() {
                @Override
                public void run() {
                    try {
                        responder.process(new MappableException(error));
                    } catch (final Throwable error) {
                        // Ignore the exception - already resumed but may be rethrown by ContainerResponseWriter#failure.
                    }
                }
            });
        }

        private boolean resume(Runnable handler) {
            synchronized (stateLock) {
                if (state != SUSPENDED) {
                    return false;
                }
                state = RESUMED;
            }

            responder.runtime.requestScope.runInScope(scopeInstance, handler);

            return true;
        }

        @Override
        public boolean cancel() {
            return cancel(new Value<Response>() {
                @Override
                public Response get() {
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
                }
            });
        }

        @Override
        public boolean cancel(final int retryAfter) {
            return cancel(new Value<Response>() {
                @Override
                public Response get() {
                    return Response
                            .status(Response.Status.SERVICE_UNAVAILABLE)
                            .header(HttpHeaders.RETRY_AFTER, retryAfter)
                            .build();
                }
            });
        }

        @Override
        public boolean cancel(final Date retryAfter) {
            return cancel(new Value<Response>() {
                @Override
                public Response get() {
                    return Response
                            .status(Response.Status.SERVICE_UNAVAILABLE)
                            .header(HttpHeaders.RETRY_AFTER, retryAfter)
                            .build();
                }
            });
        }

        private boolean cancel(final Value<Response> responseValue) {
            synchronized (stateLock) {
                if (cancelled) {
                    return true;
                }

                if (state != SUSPENDED) {
                    return false;
                }
                state = RESUMED;
                cancelled = true;
            }

            responder.runtime.requestScope.runInScope(scopeInstance, new Runnable() {
                @Override
                public void run() {
                    try {
                        final Response response = responseValue.get();
                        responder.process(new ContainerResponse(responder.request, response));
                    } catch (Throwable t) {
                        responder.process(t);
                    }
                }
            });
            return true;
        }

        public boolean isRunning() {
            synchronized (stateLock) {
                return state == RUNNING;
            }
        }

        @Override
        public boolean isSuspended() {
            synchronized (stateLock) {
                return state == SUSPENDED;
            }
        }

        @Override
        public boolean isCancelled() {
            synchronized (stateLock) {
                return cancelled;
            }
        }

        @Override
        public boolean isDone() {
            synchronized (stateLock) {
                return state == COMPLETED;
            }
        }

        @Override
        public boolean setTimeout(long time, TimeUnit unit) {
            try {
                responder.request.getResponseWriter().setSuspendTimeout(time, unit);
                return true;
            } catch (IllegalStateException ex) {
                LOGGER.log(Level.FINER, "Unable to set timeout on the AsyncResponse.", ex);
                return false;
            }
        }

        @Override
        public void setTimeoutHandler(TimeoutHandler handler) {
            timeoutHandler = handler;
        }

        @Override
        public Collection<Class<?>> register(final Class<?> callback) {
            Preconditions.checkNotNull(callback, LocalizationMessages.PARAM_NULL("callback"));

            return register(Injections.getOrCreate(responder.runtime.locator, callback));
        }

        @Override
        public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback, Class<?>... callbacks) {
            Preconditions.checkNotNull(callback, LocalizationMessages.PARAM_NULL("callback"));
            Preconditions.checkNotNull(callbacks, LocalizationMessages.CALLBACK_ARRAY_NULL());
            for (final Class<?> additionalCallback : callbacks) {
                Preconditions.checkNotNull(additionalCallback, LocalizationMessages.CALLBACK_ARRAY_ELEMENT_NULL());
            }

            final Map<Class<?>, Collection<Class<?>>> results = new HashMap<Class<?>, Collection<Class<?>>>();

            results.put(callback, register(callback));

            for (Class<?> c : callbacks) {
                results.put(c, register(c));
            }

            return results;
        }

        @Override
        public Collection<Class<?>> register(Object callback) {
            Preconditions.checkNotNull(callback, LocalizationMessages.PARAM_NULL("callback"));

            Collection<Class<?>> result = new LinkedList<Class<?>>();
            for (AbstractCallbackRunner<?> runner : callbackRunners) {
                if (runner.supports(callback.getClass())) {
                    if (runner.register(callback)) {
                        result.add(runner.getCallbackContract());
                    }
                }
            }

            return result;
        }

        @Override
        public Map<Class<?>, Collection<Class<?>>> register(Object callback, Object... callbacks) {
            Preconditions.checkNotNull(callback, LocalizationMessages.PARAM_NULL("callback"));
            Preconditions.checkNotNull(callbacks, LocalizationMessages.CALLBACK_ARRAY_NULL());
            for (final Object additionalCallback : callbacks) {
                Preconditions.checkNotNull(additionalCallback, LocalizationMessages.CALLBACK_ARRAY_ELEMENT_NULL());
            }

            final Map<Class<?>, Collection<Class<?>>> results = new HashMap<Class<?>, Collection<Class<?>>>();

            results.put(callback.getClass(), register(callback));

            for (Object c : callbacks) {
                results.put(c.getClass(), register(c));
            }

            return results;
        }
    }

    private static abstract class AbstractCallbackRunner<T> {
        private final Queue<T> callbacks = new ConcurrentLinkedQueue<T>();
        private final Logger logger;

        protected AbstractCallbackRunner(Logger logger) {
            this.logger = logger;
        }

        /**
         * Return true if this callback runner supports the {@code callbackClass}.
         *
         * @param callbackClass Callback to be checked.
         * @return True if this callback runner supports the {@code callbackClass}; false otherwise.
         */
        public final boolean supports(Class<?> callbackClass) {
            return getCallbackContract().isAssignableFrom(callbackClass);
        }

        /**
         * Get the callback contract supported by this callback runner.
         *
         * @return callback contract supported by this callback runner.
         */
        public abstract Class<?> getCallbackContract();

        @SuppressWarnings("unchecked")
        public boolean register(Object callback) {
            return callbacks.offer((T) callback);
        }

        protected final void executeCallbacks(Closure<T> invoker) {
            for (T callback : callbacks) {
                try {
                    invoker.invoke(callback);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, LocalizationMessages.ERROR_ASYNC_CALLBACK_FAILED(callback.getClass().getName()), t);
                }
            }
        }
    }

    private static class CompletionCallbackRunner
            extends AbstractCallbackRunner<CompletionCallback> implements CompletionCallback {

        private CompletionCallbackRunner() {
            super(Logger.getLogger(CompletionCallbackRunner.class.getName()));
        }

        @Override
        public Class<?> getCallbackContract() {
            return CompletionCallback.class;
        }

        @Override
        public void onComplete(final Throwable throwable) {
            executeCallbacks(new Closure<CompletionCallback>() {
                @Override
                public void invoke(CompletionCallback callback) {
                    callback.onComplete(throwable);
                }
            });
        }
    }

    /**
     * Executor of {@link ConnectionCallback connection callbacks}.
     */
    static class ConnectionCallbackRunner
            extends AbstractCallbackRunner<ConnectionCallback> implements ConnectionCallback {

        private ConnectionCallbackRunner() {
            super(Logger.getLogger(ConnectionCallbackRunner.class.getName()));
        }

        @Override
        public Class<?> getCallbackContract() {
            return ConnectionCallback.class;
        }

        @Override
        public void onDisconnect(final AsyncResponse disconnected) {
            executeCallbacks(new Closure<ConnectionCallback>() {
                @Override
                public void invoke(ConnectionCallback callback) {
                    callback.onDisconnect(disconnected);
                }
            });
        }


    }

}
