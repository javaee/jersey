/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import javax.ws.rs.container.ResumeCallback;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.util.Closure;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.process.internal.ExecutorsFactory;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.inject.ExtractorException;
import org.glassfish.jersey.server.internal.process.AsyncContext;
import org.glassfish.jersey.server.internal.process.Endpoint;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.internal.process.RespondingContext;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.spi.ExceptionMappers;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.COMPLETED;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.RESUMED;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.RUNNING;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.SUSPENDED;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Server-side request processing runtime.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class ServerRuntime {
    private final Stage<ContainerRequest> requestProcessingRoot;

    private final ServiceLocator locator;

    private final RequestScope requestScope;
    private final ExceptionMappers exceptionMappers;
    private final Provider<CloseableService> closeableServiceProvider;
    private final Provider<Ref<Value<AsyncContext>>> asyncContextFactoryProvider;
    private final ExecutorsFactory<ContainerRequest> asyncExecutorsFactory;

    /**
     * Server-side request processing runtime builder.
     */
    public static class Builder {
        @Inject
        private ServiceLocator locator;
        @Inject
        private RequestScope requestScope;
        @Inject
        private ExceptionMappers exceptionMappers;
        @Inject
        private Provider<CloseableService> closeableServiceProvider;
        @Inject
        private Provider<Ref<Value<AsyncContext>>> asyncContextRefProvider;
        @Inject
        private ExecutorsFactory<ContainerRequest> asyncExecutorsFactory;

        /**
         * Create new server-side request processing runtime.
         *
         * @param requestProcessingRoot application request processing root stage.
         * @return new server-side request processing runtime.
         */
        public ServerRuntime build(final Stage<ContainerRequest> requestProcessingRoot) {
            return new ServerRuntime(
                    requestProcessingRoot,
                    locator,
                    requestScope,
                    exceptionMappers,
                    closeableServiceProvider,
                    asyncContextRefProvider,
                    asyncExecutorsFactory);
        }
    }

    private ServerRuntime(Stage<ContainerRequest> requestProcessingRoot,
                          ServiceLocator locator,
                          RequestScope requestScope,
                          ExceptionMappers exceptionMappers,
                          Provider<CloseableService> closeableServiceProvider,
                          Provider<Ref<Value<AsyncContext>>> asyncContextFactoryProvider,
                          ExecutorsFactory<ContainerRequest> asyncExecutorsFactory) {
        this.requestProcessingRoot = requestProcessingRoot;
        this.locator = locator;
        this.requestScope = requestScope;
        this.exceptionMappers = exceptionMappers;
        this.closeableServiceProvider = closeableServiceProvider;
        this.asyncContextFactoryProvider = asyncContextFactoryProvider;
        this.asyncExecutorsFactory = asyncExecutorsFactory;
    }

    /**
     * Process a container request.
     *
     * @param request request to be processed.
     */
    public void process(final ContainerRequest request) {
        request.checkState();
        requestScope.runInScope(new Runnable() {
            @Override
            public void run() {
                final Responder responder = new Responder(
                        request,
                        locator.<RespondingContext>getService(RespondingContext.class),
                        exceptionMappers,
                        closeableServiceProvider);

                final AsyncResponderHolder asyncResponderHolder = new AsyncResponderHolder(
                        responder, locator, requestScope, requestScope.referenceCurrent(), asyncExecutorsFactory);


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
    }

    private static class AsyncResponderHolder implements Value<AsyncContext> {

        private final Responder responder;
        private final ServiceLocator locator;
        private final RequestScope requestScope;
        private final RequestScope.Instance scopeInstance;
        private final ExecutorsFactory<ContainerRequest> asyncExecutorsFactory;

        private volatile AsyncResponder asyncResponder;

        private AsyncResponderHolder(Responder responder,
                                     ServiceLocator locator,
                                     RequestScope requestScope,
                                     RequestScope.Instance scopeInstance,
                                     ExecutorsFactory<ContainerRequest> asyncExecutorsFactory) {
            this.responder = responder;
            this.locator = locator;
            this.requestScope = requestScope;
            this.scopeInstance = scopeInstance;
            this.asyncExecutorsFactory = asyncExecutorsFactory;
        }

        @Override
        public AsyncResponder get() {
            final AsyncResponder ar = new AsyncResponder(responder, locator, requestScope, scopeInstance, asyncExecutorsFactory);
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
        private final RespondingContext respondingCtx;
        private final ExceptionMappers exceptionMappers;
        private final Provider<CloseableService> closeableService;

        private final CompletionCallbackRunner completionCallbackRunner = new CompletionCallbackRunner();
        // TODO support connection callback
        private final ConnectionCallbackRunner connectionCallbackRunner = new ConnectionCallbackRunner();

        public Responder(final ContainerRequest request,
                         final RespondingContext respondingCtx,
                         final ExceptionMappers exceptionMappers,
                         final Provider<CloseableService> closeableService) {

            this.request = request;
            this.respondingCtx = respondingCtx;
            this.exceptionMappers = exceptionMappers;
            this.closeableService = closeableService;
        }


        public void process(ContainerResponse response) {
            Stage<ContainerResponse> respondingRoot = respondingCtx.createRespondingRoot();

            if (respondingRoot != null) {
                response = Stages.process(response, respondingRoot);
            }

            writeResponse(response);

            // no-exception zone
            // the methods below are guaranteed to not throw any exceptions
            completionCallbackRunner.onComplete();
            release(response);
        }

        public void process(Throwable throwable) {
            ContainerResponse response = null;
            try {
                response = convertResponse(mapException(throwable));
                process(response);
            } catch (Throwable error) {
                try {
                    request.getResponseWriter().failure(error);
                } finally {
                    completionCallbackRunner.onError(error);
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
        private Response mapException(Throwable throwable) throws Throwable {
            if (throwable instanceof MappableException) {
                // extract cause and continue with exception mapping
                throwable = throwable.getCause();
            }

            if (throwable instanceof ProcessingException) {
                // other processing exception => map to an error response
                Response.StatusType statusCode = Response.Status.INTERNAL_SERVER_ERROR;
                String message = throwable.getMessage();
                if (throwable instanceof HeaderValueException) {
                    statusCode = Response.Status.BAD_REQUEST;
                } else if (throwable instanceof ExtractorException) {
                    statusCode = Response.Status.BAD_REQUEST;
                }

                if (statusCode == Response.Status.INTERNAL_SERVER_ERROR) {
                    LOGGER.log(Level.SEVERE, message, throwable);
                } else {
                    LOGGER.log(Level.FINE, message, throwable);
                }

                return Response.status(statusCode).build();
            }

            Response response = null;
            if (throwable instanceof WebApplicationException) {
                response = ((WebApplicationException) throwable).getResponse();
            }
            if (response == null || !response.hasEntity()) {
                // try to map the WAE
                ExceptionMapper mapper = exceptionMappers.find(throwable.getClass());
                if (mapper != null) {
                    try {
                        response = mapper.toResponse(throwable);
                        if (response == null) {
                            return Response.noContent().build();
                        }
                    } catch (Throwable t) {
                        return Response.serverError().build();
                    }
                }
            }
            if (response != null) {
                return response;
            }

            // throwable was not mapped - has to be propagated to the container.
            throw throwable;
        }

        /**
         * Used to set proper Content-Length header to outgoing {@link Response}s.
         */
        private static class MessageBodySizeCallback implements MessageBodyWorkers.MessageBodySizeCallback {
            private long size = -1;

            @Override
            public void onRequestEntitySize(long size) throws IOException {
                this.size = size;
            }

            public long getSize() {
                return size;
            }
        }

        private ContainerResponse writeResponse(final ContainerResponse response) {
            final ContainerResponseWriter writer = request.getResponseWriter();
            final MessageBodySizeCallback messageBodySizeCallback = new MessageBodySizeCallback();


            if (!response.hasEntity()) {
                writer.writeResponseStatusAndHeaders(0, response);
                return response;
            }

            final Object entity = response.getEntity();
            boolean skipFinally = false;
            try {
                response.setStreamProvider(new OutboundMessageContext.StreamProvider() {
                    private OutputStream output;

                    @Override
                    public void commit() throws IOException {
                        final long size;
                        if (response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING) != null) {
                            size = -1;
                        } else {
                            size = messageBodySizeCallback.getSize();
                        }
                        output = writer.writeResponseStatusAndHeaders(size, response);
                    }

                    @Override
                    public OutputStream getOutputStream() throws IOException {
                        return output;
                    }
                });
                response.setEntityStream(request.getWorkers().writeTo(
                        entity,
                        entity.getClass(),
                        response.getEntityType(),
                        response.getEntityAnnotations(),
                        response.getMediaType(),
                        response.getHeaders(),
                        request.getPropertiesDelegate(),
                        response.getEntityStream(),
                        messageBodySizeCallback,
                        true,
                        !request.getMethod().equals(HttpMethod.HEAD)));
            } catch (Throwable ex) {
                if (response.isCommitted()) {
                    /**
                     * We're done with processing here. There's nothing we can do about the exception so
                     * let's just log it.
                     */
                    LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_WRITING_RESPONSE_ENTITY(), ex);
                } else {
                    skipFinally = true;
                    throw new MappableException(ex);
                }
            } finally {
                if (!skipFinally) {
                    response.commitStream();

                    if (response.isChunked()) {
                        try {
                            ((ChunkedOutput) entity).setContext(request, response);
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_WRITING_RESPONSE_ENTITY_CHUNK(), ex);
                        }
                        // suspend the writer
                        if (writer.suspend(0, TimeUnit.SECONDS, null)) {
                            // TODO already suspended - what to do? override the timeout value?
                        }
                    }
                }
            }

            return response;
        }

        private void release(ContainerResponse responseContext) {
            try {
                closeableService.get().close();
                // Commit the container response writer if not in chunked mode
                // responseContext may be null in case the request processing was cancelled.
                if (responseContext != null && !responseContext.isChunked()) {
                    responseContext.close();
                }
            } catch (Throwable throwable) {
                // TODO L10N
                LOGGER.log(Level.WARNING, "Attempt to release single request processing resources has failed.", throwable);
            }
        }
    }

    private static class AsyncResponder implements AsyncContext, ContainerResponseWriter.TimeoutHandler, CompletionCallback {
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
        private final ServiceLocator locator;
        private final RequestScope requestScope;
        private final RequestScope.Instance scopeInstance;
        private final ExecutorsFactory<ContainerRequest> asyncExecutorsFactory;

        private volatile TimeoutHandler timeoutHandler = DEFAULT_TIMEOUT_HANDLER;

        private final ResumeCallbackRunner resumeCallbackRunner = new ResumeCallbackRunner();
        private final List<AbstractCallbackRunner<?>> callbackRunners;

        public AsyncResponder(final Responder responder,
                              final ServiceLocator locator,
                              final RequestScope requestScope,
                              final RequestScope.Instance scopeInstance,
                              final ExecutorsFactory<ContainerRequest> asyncExecutorsFactory) {
            this.requestScope = requestScope;
            this.responder = responder;
            this.locator = locator;
            this.scopeInstance = scopeInstance;
            this.asyncExecutorsFactory = asyncExecutorsFactory;

            this.callbackRunners = Collections.unmodifiableList(Arrays.asList(
                    resumeCallbackRunner, responder.completionCallbackRunner, responder.connectionCallbackRunner));

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
        public void onComplete() {
            synchronized (stateLock) {
                state = COMPLETED;
            }
        }

        @Override
        public void onError(Throwable throwable) {
            synchronized (stateLock) {
                state = COMPLETED;
            }
        }

        @Override
        public void invokeManaged(final Producer<Response> producer) {
            asyncExecutorsFactory.getRequestingExecutor(responder.request).submit(new Runnable() {
                @Override
                public void run() {
                    requestScope.runInScope(scopeInstance, new Runnable() {
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
        public void resume(final Object response) throws IllegalStateException {
            resume(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Response jaxrsResponse = toJaxrsResponse(response);

                        resumeCallbackRunner.onResume(AsyncResponder.this, jaxrsResponse);

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
        public void resume(final Throwable error) throws IllegalStateException {
            resume(new Runnable() {
                @Override
                public void run() {
                    resumeCallbackRunner.onResume(AsyncResponder.this, error);
                    responder.process(error);
                }
            });
        }

        private void resume(Runnable handler) {
            synchronized (stateLock) {
                if (state != SUSPENDED) {
                    throw new IllegalStateException("Not suspended.");
                }
                state = RESUMED;
            }

            requestScope.runInScope(scopeInstance, handler);
        }

        @Override
        public void cancel() {
            cancel(new Value<Response>() {
                @Override
                public Response get() {
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
                }
            });
        }

        @Override
        public void cancel(final int retryAfter) {
            cancel(new Value<Response>() {
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
        public void cancel(final Date retryAfter) {
            cancel(new Value<Response>() {
                @Override
                public Response get() {
                    return Response
                            .status(Response.Status.SERVICE_UNAVAILABLE)
                            .header(HttpHeaders.RETRY_AFTER, retryAfter)
                            .build();
                }
            });
        }

        private void cancel(final Value<Response> responseValue) {
            synchronized (stateLock) {
                if (cancelled) {
                    return;
                }
                if (state != SUSPENDED) {
                    throw new IllegalStateException("Not suspended");
                }
                state = RESUMED;
                cancelled = true;
            }

            requestScope.runInScope(scopeInstance, new Runnable() {
                @Override
                public void run() {
                    try {
                        final Response response = responseValue.get();
                        resumeCallbackRunner.onResume(AsyncResponder.this, response);
                        responder.process(new ContainerResponse(responder.request, response));
                    } catch (Throwable t) {
                        responder.process(t);
                    }
                }
            });
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
        public void setTimeout(long time, TimeUnit unit) throws IllegalStateException {
            responder.request.getResponseWriter().setSuspendTimeout(time, unit);
        }

        @Override
        public void setTimeoutHandler(TimeoutHandler handler) {
            timeoutHandler = handler;
        }

        @Override
        public boolean register(final Class<?> callback) throws NullPointerException {
            return register(Injections.getOrCreate(locator, callback));
        }

        @Override
        public boolean[] register(Class<?> callback, Class<?>... callbacks) throws NullPointerException {
            final boolean[] results = new boolean[1 + ((callbacks == null) ? 0 : callbacks.length)];

            int i = 0;
            results[i++] = register(callback);

            if (callbacks != null) {
                for (Class<?> c : callbacks) {
                    results[i++] = register(c);
                }
            }

            return results;
        }

        @Override
        public boolean register(Object callback) throws NullPointerException {
            boolean result = false;
            for (AbstractCallbackRunner<?> runner : callbackRunners) {
                if (runner.supports(callback.getClass())) {
                    result |= runner.register(callback);
                }
            }

            return result;
        }

        @Override
        public boolean[] register(Object callback, Object... callbacks) throws NullPointerException {
            final boolean[] results = new boolean[1 + ((callbacks == null) ? 0 : callbacks.length)];

            int i = 0;
            results[i++] = register(callback);

            if (callbacks != null) {
                for (Object c : callbacks) {
                    results[i++] = register(c);
                }
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

        public abstract boolean supports(Class<?> callbackClass);

        @SuppressWarnings("unchecked")
        public boolean register(Object callback) {
            return callbacks.offer((T) callback);
        }

        protected final void executeCallbacks(Closure<T> invoker) {
            for (T callback : callbacks) {
                try {
                    invoker.invoke(callback);
                } catch (Throwable t) {
                    // TODO L10N
                    logger.log(Level.WARNING, String.format("Callback %s invocation failed.", callback.getClass().getName()), t);
                }
            }
        }
    }

    private static class ResumeCallbackRunner
            extends AbstractCallbackRunner<ResumeCallback> implements ResumeCallback {

        private ResumeCallbackRunner() {
            super(Logger.getLogger(ResumeCallbackRunner.class.getName()));
        }

        @Override
        public boolean supports(Class<?> callbackClass) {
            return ResumeCallback.class.isAssignableFrom(callbackClass);
        }

        @Override
        public void onResume(final AsyncResponse resuming, final Response response) {
            executeCallbacks(new Closure<ResumeCallback>() {
                @Override
                public void invoke(ResumeCallback callback) {
                    callback.onResume(resuming, response);
                }
            });
        }

        @Override
        public void onResume(final AsyncResponse resuming, final Throwable error) {
            executeCallbacks(new Closure<ResumeCallback>() {
                @Override
                public void invoke(ResumeCallback callback) {
                    callback.onResume(resuming, error);
                }
            });
        }
    }

    private static class CompletionCallbackRunner
            extends AbstractCallbackRunner<CompletionCallback> implements CompletionCallback {

        private CompletionCallbackRunner() {
            super(Logger.getLogger(CompletionCallbackRunner.class.getName()));
        }

        @Override
        public boolean supports(Class<?> callbackClass) {
            return CompletionCallback.class.isAssignableFrom(callbackClass);
        }

        @Override
        public void onComplete() {
            executeCallbacks(new Closure<CompletionCallback>() {
                @Override
                public void invoke(CompletionCallback callback) {
                    callback.onComplete();
                }
            });
        }

        @Override
        public void onError(final Throwable error) {
            executeCallbacks(new Closure<CompletionCallback>() {
                @Override
                public void invoke(CompletionCallback callback) {
                    callback.onError(error);
                }
            });
        }
    }

    private static class ConnectionCallbackRunner
            extends AbstractCallbackRunner<ConnectionCallback> implements ConnectionCallback {

        private ConnectionCallbackRunner() {
            super(Logger.getLogger(ConnectionCallbackRunner.class.getName()));
        }

        @Override
        public boolean supports(Class<?> callbackClass) {
            return ConnectionCallback.class.isAssignableFrom(callbackClass);
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
