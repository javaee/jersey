/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.net.URI;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.Closure;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.ServerTraceEvent;
import org.glassfish.jersey.server.internal.monitoring.EmptyRequestEventBuilder;
import org.glassfish.jersey.server.internal.monitoring.RequestEventBuilder;
import org.glassfish.jersey.server.internal.monitoring.RequestEventImpl;
import org.glassfish.jersey.server.internal.process.AsyncContext;
import org.glassfish.jersey.server.internal.process.Endpoint;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.ExternalRequestContext;
import org.glassfish.jersey.server.spi.ExternalRequestScope;
import org.glassfish.jersey.server.spi.ResponseErrorMapper;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.api.ServiceLocator;

import static org.glassfish.jersey.server.internal.process.AsyncContext.State.COMPLETED;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.RESUMED;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.RUNNING;
import static org.glassfish.jersey.server.internal.process.AsyncContext.State.SUSPENDED;

import jersey.repackaged.com.google.common.base.Preconditions;

/**
 * Server-side request processing runtime.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ServerRuntime {

    private final Stage<RequestProcessingContext> requestProcessingRoot;
    private final ProcessingProviders processingProviders;

    private final ServiceLocator locator;

    private final ScheduledExecutorService backgroundScheduler;
    private final Provider<ExecutorService> managedAsyncExecutor;

    private final RequestScope requestScope;
    private final ExceptionMappers exceptionMappers;
    private final ApplicationEventListener applicationEventListener;
    private final Configuration configuration;

    private final ExternalRequestScope externalRequestScope;

    private final TracingConfig tracingConfig;
    private final TracingLogger.Level tracingThreshold;

    private final boolean processResponseErrors;

    /** Do not resolve relative URIs in the {@code Location} header */
    private final boolean disableLocationHeaderRelativeUriResolution;
    /** Resolve relative URIs according to RFC7231 (not JAX-RS 2.0 compliant */
    private final boolean rfc7231LocationHeaderRelativeUriResolution;

    /*package */ static final ExternalRequestScope<Object> NOOP_EXTERNAL_REQ_SCOPE = new ExternalRequestScope<Object>() {

        @Override
        public ExternalRequestContext<Object> open(final ServiceLocator serviceLocator) {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public void suspend(final ExternalRequestContext<Object> o, final ServiceLocator serviceLocator) {
        }

        @Override
        public void resume(final ExternalRequestContext<Object> o, final ServiceLocator serviceLocator) {
        }
    };

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
        @ManagedAsyncExecutor
        private Provider<ExecutorService> asyncExecutorProvider;
        @Inject
        private RequestScope requestScope;
        @Inject
        private ExceptionMappers exceptionMappers;
        @Inject
        private Configuration configuration;
        @Inject
        private ExternalRequestScope externalRequestScope;

        /**
         * Create new server-side request processing runtime.
         *
         * @param processingRoot      application request processing root stage.
         * @param eventListener       application event listener registered for this runtime.
         * @param processingProviders application processing providers.
         * @return new server-side request processing runtime.
         */
        public ServerRuntime build(
                final Stage<RequestProcessingContext> processingRoot,
                final ApplicationEventListener eventListener,
                final ProcessingProviders processingProviders) {

            final ExternalRequestScope externalScope =
                    externalRequestScope != null ? externalRequestScope : NOOP_EXTERNAL_REQ_SCOPE;

            return new ServerRuntime(
                    processingRoot,
                    processingProviders,
                    locator,
                    backgroundScheduler,
                    asyncExecutorProvider,
                    requestScope,
                    exceptionMappers,
                    eventListener,
                    externalScope,
                    configuration);
        }
    }

    private ServerRuntime(final Stage<RequestProcessingContext> requestProcessingRoot,
                          final ProcessingProviders processingProviders,
                          final ServiceLocator locator,
                          final ScheduledExecutorService backgroundScheduler,
                          final Provider<ExecutorService> managedAsyncExecutorProvider,
                          final RequestScope requestScope,
                          final ExceptionMappers exceptionMappers,
                          final ApplicationEventListener applicationEventListener,
                          final ExternalRequestScope externalScope,
                          final Configuration configuration) {
        this.requestProcessingRoot = requestProcessingRoot;
        this.processingProviders = processingProviders;
        this.locator = locator;

        this.backgroundScheduler = backgroundScheduler;
        this.managedAsyncExecutor = managedAsyncExecutorProvider;

        this.requestScope = requestScope;
        this.exceptionMappers = exceptionMappers;
        this.applicationEventListener = applicationEventListener;
        this.externalRequestScope = externalScope;
        this.configuration = configuration;

        this.tracingConfig = TracingUtils.getTracingConfig(configuration);
        this.tracingThreshold = TracingUtils.getTracingThreshold(configuration);

        this.processResponseErrors = PropertiesHelper.isProperty(
                configuration.getProperty(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED));

        this.disableLocationHeaderRelativeUriResolution = ServerProperties.getValue(configuration.getProperties(),
                ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED,
                Boolean.FALSE, Boolean.class);

        this.rfc7231LocationHeaderRelativeUriResolution = ServerProperties.getValue(configuration.getProperties(),
                ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_RFC7231,
                Boolean.FALSE, Boolean.class);
    }

    /**
     * Process a container request.
     *
     * @param request container request to be processed.
     */
    public void process(final ContainerRequest request) {
        TracingUtils.initTracingSupport(tracingConfig, tracingThreshold, request);
        TracingUtils.logStart(request);

        final UriRoutingContext routingContext = request.getUriRoutingContext();

        RequestEventBuilder monitoringEventBuilder = EmptyRequestEventBuilder.INSTANCE;
        RequestEventListener monitoringEventListener = null;

        if (applicationEventListener != null) {
            monitoringEventBuilder = new RequestEventImpl.Builder()
                    .setContainerRequest(request)
                    .setExtendedUriInfo(routingContext);
            monitoringEventListener = applicationEventListener.onRequest(
                    monitoringEventBuilder.build(RequestEvent.Type.START));
        }

        request.setProcessingProviders(processingProviders);

        final RequestProcessingContext context = new RequestProcessingContext(
                locator,
                request,
                routingContext,
                monitoringEventBuilder,
                monitoringEventListener);

        request.checkState();
        final Responder responder = new Responder(context, ServerRuntime.this);
        final RequestScope.Instance requestScopeInstance = requestScope.createInstance();
        final AsyncResponderHolder asyncResponderHolder =
                new AsyncResponderHolder(responder, externalRequestScope,
                        requestScopeInstance, externalRequestScope.open(locator));
        context.initAsyncContext(asyncResponderHolder);

        requestScope.runInScope(requestScopeInstance, new Runnable() {
            @Override
            public void run() {
                try {
                    // set base URI into response builder thread-local variable
                    // for later resolving of relative location URIs
                    if (!disableLocationHeaderRelativeUriResolution) {
                        final URI uriToUse =
                                rfc7231LocationHeaderRelativeUriResolution ? request.getRequestUri() : request.getBaseUri();
                        OutboundJaxrsResponse.Builder.setBaseUri(uriToUse);
                    }

                    final Ref<Endpoint> endpointRef = Refs.emptyRef();
                    final RequestProcessingContext data = Stages.process(context, requestProcessingRoot, endpointRef);

                    final Endpoint endpoint = endpointRef.get();
                    if (endpoint == null) {
                        // not found
                        throw new NotFoundException();
                    }

                    final ContainerResponse response = endpoint.apply(data);

                    if (!asyncResponderHolder.isAsync()) {
                        responder.process(response);
                    } else {
                        externalRequestScope.suspend(asyncResponderHolder.externalContext, locator);
                    }
                } catch (final Throwable throwable) {
                    responder.process(throwable);
                } finally {
                    asyncResponderHolder.release();
                    // clear base URI from the thread
                    OutboundJaxrsResponse.Builder.clearBaseUri();
                }
            }
        });
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

    /**
     * Ensure that the value a {@value HttpHeaders#LOCATION} header is an absolute URI, if present among headers.
     * <p/>
     * Relative URI value will be made absolute using a base request URI.
     *
     * @param location location URI; value of the HTTP {@value HttpHeaders#LOCATION} response header.
     * @param headers  mutable map of response headers.
     * @param request  container request.
     * @param incompatible if set to {@code true}, uri will be resolved against the request uri, not the base uri;
     *                     this is correct against RFC7231, but does violate the JAX-RS 2.0 specs
     */
    private static void ensureAbsolute(final URI location, final MultivaluedMap<String, Object> headers,
                                       final ContainerRequest request, final boolean incompatible) {
        if (location == null || location.isAbsolute()) {
            return;
        }
        // according to RFC7231 (HTTP/1.1), this field can contain one single URI reference
        final URI uri = incompatible ? request.getRequestUri() : request.getBaseUri();
        headers.putSingle(HttpHeaders.LOCATION, uri.resolve(location));
    }

    private static class AsyncResponderHolder implements Value<AsyncContext> {

        private final Responder responder;
        private final ExternalRequestScope externalScope;
        private final RequestScope.Instance scopeInstance;
        private final ExternalRequestContext<?> externalContext;

        private volatile AsyncResponder asyncResponder;

        private AsyncResponderHolder(final Responder responder,
                                     final ExternalRequestScope externalRequestScope,
                                     final RequestScope.Instance scopeInstance,
                                     final ExternalRequestContext<?> externalContext) {
            this.responder = responder;
            this.externalScope = externalRequestScope;
            this.scopeInstance = scopeInstance;
            this.externalContext = externalContext;
        }

        @Override
        public AsyncContext get() {
            final AsyncResponder ar = new AsyncResponder(responder, scopeInstance, externalScope, externalContext);
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

        private final RequestProcessingContext processingContext;
        private final ServerRuntime runtime;

        private final CompletionCallbackRunner completionCallbackRunner = new CompletionCallbackRunner();
        private final ConnectionCallbackRunner connectionCallbackRunner = new ConnectionCallbackRunner();

        private final TracingLogger tracingLogger;

        public Responder(final RequestProcessingContext processingContext, final ServerRuntime runtime) {
            this.processingContext = processingContext;
            this.runtime = runtime;

            this.tracingLogger = TracingLogger.getInstance(processingContext.request());
        }

        public void process(ContainerResponse response) {
            processingContext.monitoringEventBuilder().setContainerResponse(response);
            response = processResponse(response);
            release(response);
        }

        private ContainerResponse processResponse(ContainerResponse response) {
            final Stage<ContainerResponse> respondingRoot = processingContext.createRespondingRoot();

            if (respondingRoot != null) {
                response = Stages.process(response, respondingRoot);
            }
            writeResponse(response);

            // no-exception zone
            // the methods below are guaranteed to not throw any exceptions
            completionCallbackRunner.onComplete(null);
            return response;
        }

        /**
         * Process {@code throwable} by using exception mappers and generating the mapped
         * response if possible.
         * <p>
         * Note about logging:
         * <ul>
         * <li>
         * we do not log exceptions that are mapped by ExceptionMappers.
         * </li><li>
         * All other exceptions are logged: WebApplicationExceptions with entities,
         * exceptions that were unsuccessfully mapped
         * </li>
         * </ul>
         * </p>
         *
         * @param throwable Exception to be processed.
         */
        public void process(final Throwable throwable) {
            final ContainerRequest request = processingContext.request();
            processingContext.monitoringEventBuilder().setException(throwable, RequestEvent.ExceptionCause.ORIGINAL);
            processingContext.triggerEvent(RequestEvent.Type.ON_EXCEPTION);

            ContainerResponse response = null;
            try {
                final Response exceptionResponse = mapException(throwable);
                try {
                    try {
                        response = convertResponse(exceptionResponse);
                        if (!runtime.disableLocationHeaderRelativeUriResolution) {
                            ensureAbsolute(response.getLocation(), response.getHeaders(), request,
                                    runtime.rfc7231LocationHeaderRelativeUriResolution);
                        }
                        processingContext.monitoringEventBuilder().setContainerResponse(response)
                                .setResponseSuccessfullyMapped(true);
                    } finally {
                        processingContext.triggerEvent(RequestEvent.Type.EXCEPTION_MAPPING_FINISHED);
                    }

                    processResponse(response);
                } catch (final Throwable respError) {
                    LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_PROCESSING_RESPONSE_FROM_ALREADY_MAPPED_EXCEPTION());
                    processingContext.monitoringEventBuilder()
                            .setException(respError, RequestEvent.ExceptionCause.MAPPED_RESPONSE);
                    processingContext.triggerEvent(RequestEvent.Type.ON_EXCEPTION);
                    throw respError;
                }
            } catch (final Throwable responseError) {
                if (throwable != responseError
                        && !(throwable instanceof MappableException && throwable.getCause() == responseError)) {
                    LOGGER.log(Level.FINE, LocalizationMessages.ERROR_EXCEPTION_MAPPING_ORIGINAL_EXCEPTION(), throwable);
                }

                if (!processResponseError(responseError)) {
                    // Pass the exception to the container.
                    LOGGER.log(Level.FINE, LocalizationMessages.ERROR_EXCEPTION_MAPPING_THROWN_TO_CONTAINER(), responseError);

                    try {
                        request.getResponseWriter().failure(responseError);
                    } finally {
                        completionCallbackRunner.onComplete(responseError);
                    }
                }
            } finally {
                release(response);
            }
        }

        /**
         * If {@value org.glassfish.jersey.server.ServerProperties#PROCESSING_RESPONSE_ERRORS_ENABLED} is set to true then try to
         * handle errors raised during response processing.
         *
         * @param responseError a throwable that occurred during response processing.
         * @return {@code true} if the given response error has been processed, {@code false} otherwise.
         */
        private boolean processResponseError(final Throwable responseError) {
            boolean processed = false;

            if (runtime.processResponseErrors) {
                // Try to obtain response from response error mapper.
                final Iterable<ResponseErrorMapper> mappers = Providers.getAllProviders(runtime.locator,
                        ResponseErrorMapper.class);

                try {
                    Response processedError = null;
                    for (final ResponseErrorMapper mapper : mappers) {
                        processedError = mapper.toResponse(responseError);
                        if (processedError != null) {
                            break;
                        }
                    }

                    if (processedError != null) {
                        processResponse(new ContainerResponse(processingContext.request(), processedError));
                        processed = true;
                    }
                } catch (final Throwable throwable) {
                    LOGGER.log(Level.FINE, LocalizationMessages.ERROR_EXCEPTION_MAPPING_PROCESSED_RESPONSE_ERROR(), throwable);
                }
            }

            return processed;
        }

        private ContainerResponse convertResponse(final Response exceptionResponse) {
            final ContainerResponse containerResponse = new ContainerResponse(processingContext.request(), exceptionResponse);
            containerResponse.setMappedFromException(true);
            return containerResponse;
        }

        @SuppressWarnings("unchecked")
        private Response mapException(final Throwable originalThrowable) throws Throwable {
            LOGGER.log(Level.FINER, LocalizationMessages.EXCEPTION_MAPPING_START(), originalThrowable);

            Throwable throwable = originalThrowable;
            boolean inMappable = false;
            boolean mappingNotFound = false;

            do {
                if (throwable instanceof MappableException) {
                    inMappable = true;
                } else if (inMappable || throwable instanceof WebApplicationException) {
                    // in case ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED is true, allow
                    // wrapped MessageBodyProviderNotFoundException to propagate
                    if (runtime.processResponseErrors && throwable instanceof InternalServerErrorException
                            && throwable.getCause() instanceof MessageBodyProviderNotFoundException) {
                        throw throwable;
                    }
                    Response waeResponse = null;

                    if (throwable instanceof WebApplicationException) {
                        final WebApplicationException webApplicationException = (WebApplicationException) throwable;

                        // set mapped throwable
                        processingContext.routingContext().setMappedThrowable(throwable);

                        waeResponse = webApplicationException.getResponse();
                        if (waeResponse.hasEntity()) {
                            LOGGER.log(Level.FINE, LocalizationMessages
                                    .EXCEPTION_MAPPING_WAE_ENTITY(waeResponse.getStatus()), throwable);
                            return waeResponse;
                        }
                    }

                    final long timestamp = tracingLogger.timestamp(ServerTraceEvent.EXCEPTION_MAPPING);
                    final ExceptionMapper mapper = runtime.exceptionMappers.findMapping(throwable);
                    if (mapper != null) {
                        processingContext.monitoringEventBuilder().setExceptionMapper(mapper);
                        processingContext.triggerEvent(RequestEvent.Type.EXCEPTION_MAPPER_FOUND);
                        try {
                            final Response mappedResponse = mapper.toResponse(throwable);

                            if (tracingLogger.isLogEnabled(ServerTraceEvent.EXCEPTION_MAPPING)) {
                                tracingLogger.logDuration(ServerTraceEvent.EXCEPTION_MAPPING,
                                        timestamp, mapper, throwable, throwable.getLocalizedMessage(),
                                        mappedResponse != null ? mappedResponse.getStatusInfo() : "-no-response-");
                            }

                            // set mapped throwable
                            processingContext.routingContext().setMappedThrowable(throwable);

                            if (mappedResponse != null) {
                                // response successfully mapped
                                if (LOGGER.isLoggable(Level.FINER)) {
                                    final String message = String.format(
                                            "Exception '%s' has been mapped by '%s' to response '%s' (%s:%s).",
                                            throwable.getLocalizedMessage(),
                                            mapper.getClass().getName(),
                                            mappedResponse.getStatusInfo().getReasonPhrase(),
                                            mappedResponse.getStatusInfo().getStatusCode(),
                                            mappedResponse.getStatusInfo().getFamily());
                                    LOGGER.log(Level.FINER, message);
                                }
                                return mappedResponse;
                            } else {
                                return Response.noContent().build();
                            }
                        } catch (final Throwable mapperThrowable) {
                            // spec: If the exception mapping provider throws an exception while creating a Response
                            // then return a server error (status code 500) response to the client.
                            LOGGER.log(Level.SEVERE, LocalizationMessages.EXCEPTION_MAPPER_THROWS_EXCEPTION(mapper.getClass()),
                                    mapperThrowable);
                            LOGGER.log(Level.SEVERE, LocalizationMessages.EXCEPTION_MAPPER_FAILED_FOR_EXCEPTION(), throwable);
                            return Response.serverError().build();
                        }
                    }

                    if (waeResponse != null) {
                        LOGGER.log(Level.FINE, LocalizationMessages
                                .EXCEPTION_MAPPING_WAE_NO_ENTITY(waeResponse.getStatus()), throwable);

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
            final ContainerRequest request = processingContext.request();
            final ContainerResponseWriter writer = request.getResponseWriter();

            if (!runtime.disableLocationHeaderRelativeUriResolution) {
                ServerRuntime.ensureAbsolute(response.getLocation(), response.getHeaders(), response.getRequestContext(),
                        runtime.rfc7231LocationHeaderRelativeUriResolution);
            }

            if (!response.hasEntity()) {
                tracingLogger.log(ServerTraceEvent.FINISHED, response.getStatusInfo());
                tracingLogger.flush(response.getHeaders());

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
                    public OutputStream getOutputStream(final int contentLength) throws IOException {
                        if (!runtime.disableLocationHeaderRelativeUriResolution) {
                            ServerRuntime.ensureAbsolute(response.getLocation(), response.getHeaders(),
                                    response.getRequestContext(), runtime.rfc7231LocationHeaderRelativeUriResolution);
                        }
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
                            request.getWriterInterceptors()));
                } catch (final MappableException mpe) {
                    if (mpe.getCause() instanceof IOException) {
                        connectionCallbackRunner.onDisconnect(processingContext.asyncContext());
                    }
                    throw mpe;
                }
                tracingLogger.log(ServerTraceEvent.FINISHED, response.getStatusInfo());
                tracingLogger.flush(response.getHeaders());

                setWrittenResponse(response);

            } catch (final Throwable ex) {
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
                    boolean close = !response.isChunked();
                    if (response.isChunked()) {
                        try {
                            response.commitStream();
                        } catch (final Exception e) {
                            LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_COMMITTING_OUTPUT_STREAM(), e);
                            close = true;
                        }

                        final ChunkedOutput chunked = (ChunkedOutput) entity;
                        try {
                            chunked.setContext(
                                    runtime.requestScope,
                                    runtime.requestScope.referenceCurrent(),
                                    request,
                                    response,
                                    connectionCallbackRunner,
                                    processingContext.asyncContextValue());
                        } catch (final IOException ex) {
                            LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_WRITING_RESPONSE_ENTITY_CHUNK(), ex);
                            close = true;
                        }
                        // suspend the writer indefinitely (passing null timeout handler is ok in such case) if the output is not
                        // already closed.
                        // TODO what to do if we detect that the writer has already been suspended? override the timeout value?
                        if (!chunked.isClosed()
                                && !writer.suspend(AsyncResponder.NO_TIMEOUT, TimeUnit.SECONDS, null)) {
                            LOGGER.fine(LocalizationMessages.ERROR_SUSPENDING_CHUNKED_OUTPUT_RESPONSE());
                        }
                    }

                    if (close) {
                        try {
                            // the response must be closed here instead of just flushed or committed. Some
                            // output streams writes out bytes only on close (for example GZipOutputStream).
                            response.close();
                        } catch (final Exception e) {
                            LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_CLOSING_COMMIT_OUTPUT_STREAM(), e);
                        }
                    }
                }
            }

            return response;
        }

        private void setWrittenResponse(final ContainerResponse response) {
            processingContext.monitoringEventBuilder()
                    .setContainerResponse(response)
                    .setSuccess(response.getStatus() < Response.Status.BAD_REQUEST.getStatusCode())
                    .setResponseWritten(true);
        }

        private void release(final ContainerResponse responseContext) {
            try {
                processingContext.closeableService().close();

                // Commit the container response writer if not in chunked mode
                // responseContext may be null in case the request processing was cancelled.
                if (responseContext != null && !responseContext.isChunked()) {
                    // responseContext.commitStream();
                    responseContext.close();
                }

            } catch (final Throwable throwable) {
                LOGGER.log(Level.WARNING, LocalizationMessages.RELEASING_REQUEST_PROCESSING_RESOURCES_FAILED(), throwable);
            } finally {
                runtime.externalRequestScope.close();
                processingContext.triggerEvent(RequestEvent.Type.FINISHED);
            }
        }
    }

    private static class AsyncResponder implements AsyncContext, ContainerResponseWriter.TimeoutHandler, CompletionCallback {

        private static final Logger LOGGER = Logger.getLogger(AsyncResponder.class.getName());

        private static final TimeoutHandler DEFAULT_TIMEOUT_HANDLER = new TimeoutHandler() {
            @Override
            public void handleTimeout(final AsyncResponse asyncResponse) {
                throw new ServiceUnavailableException();
            }
        };

        private final Object stateLock = new Object();
        private State state = RUNNING;
        private boolean cancelled = false;

        private final Responder responder;
        // TODO this instance should be released once async invocation is finished.
        private final RequestScope.Instance scopeInstance;
        private final ExternalRequestContext<?> foreignScopeInstance;
        private final ExternalRequestScope requestScopeListener;

        private volatile TimeoutHandler timeoutHandler = DEFAULT_TIMEOUT_HANDLER;

        private final List<AbstractCallbackRunner<?>> callbackRunners;

        public AsyncResponder(final Responder responder,
                              final RequestScope.Instance scopeInstance,
                              final ExternalRequestScope requestScopeListener,
                              final ExternalRequestContext<?> foreignScopeInstance) {
            this.responder = responder;
            this.scopeInstance = scopeInstance;
            this.foreignScopeInstance = foreignScopeInstance;
            this.requestScopeListener = requestScopeListener;

            this.callbackRunners = Collections.unmodifiableList(Arrays.asList(
                    responder.completionCallbackRunner, responder.connectionCallbackRunner));

            responder.completionCallbackRunner.register(this);
        }

        @Override
        public void onTimeout(final ContainerResponseWriter responseWriter) {
            final TimeoutHandler handler = timeoutHandler;
            try {
                synchronized (stateLock) {
                    if (state == SUSPENDED) {
                        handler.handleTimeout(this);
                    }
                }
            } catch (final Throwable throwable) {
                resume(throwable);
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
            responder.runtime.managedAsyncExecutor.get().submit(new Runnable() {
                @Override
                public void run() {
                    responder.runtime.requestScope.runInScope(scopeInstance, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                requestScopeListener.resume(foreignScopeInstance, responder.runtime.locator);
                                final Response response = producer.call();
                                if (response != null) {
                                    resume(response);
                                }
                            } catch (final Throwable t) {
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
                    if (responder.processingContext.request().getResponseWriter().suspend(
                            AsyncResponse.NO_TIMEOUT, TimeUnit.SECONDS, this)) {
                        state = SUSPENDED;
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean resume(final Object response) {
            return resume(new Runnable() {
                @Override
                public void run() {
                    try {
                        requestScopeListener.resume(foreignScopeInstance, responder.runtime.locator);
                        final Response jaxrsResponse =
                                (response instanceof Response) ? (Response) response : Response.ok(response).build();
                        if (!responder.runtime.disableLocationHeaderRelativeUriResolution) {
                            ServerRuntime.ensureAbsolute(jaxrsResponse.getLocation(), jaxrsResponse.getHeaders(),
                                    responder.processingContext.request(),
                                    responder.runtime.rfc7231LocationHeaderRelativeUriResolution);
                        }
                        responder.process(new ContainerResponse(responder.processingContext.request(), jaxrsResponse));
                    } catch (final Throwable t) {
                        responder.process(t);
                    }
                }
            });
        }

        @Override
        public boolean resume(final Throwable error) {
            return resume(new Runnable() {
                @Override
                public void run() {
                    try {
                        requestScopeListener.resume(foreignScopeInstance, responder.runtime.locator);
                        responder.process(new MappableException(error));
                    } catch (final Throwable error) {
                        // Ignore the exception - already resumed but may be rethrown by ContainerResponseWriter#failure.
                    }
                }
            });
        }

        private boolean resume(final Runnable handler) {
            synchronized (stateLock) {
                if (state != SUSPENDED) {
                    return false;
                }
                state = RESUMED;
            }

            try {
                responder.runtime.requestScope.runInScope(scopeInstance, handler);
            } finally {
                scopeInstance.release();
            }

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
                        requestScopeListener.resume(foreignScopeInstance, responder.runtime.locator);
                        final Response response = responseValue.get();
                        responder.process(new ContainerResponse(responder.processingContext.request(), response));
                    } catch (final Throwable t) {
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
        public boolean setTimeout(final long time, final TimeUnit unit) {
            try {
                responder.processingContext.request().getResponseWriter().setSuspendTimeout(time, unit);
                return true;
            } catch (final IllegalStateException ex) {
                LOGGER.log(Level.FINER, "Unable to set timeout on the AsyncResponse.", ex);
                return false;
            }
        }

        @Override
        public void setTimeoutHandler(final TimeoutHandler handler) {
            timeoutHandler = handler;
        }

        @Override
        public Collection<Class<?>> register(final Class<?> callback) {
            Preconditions.checkNotNull(callback, LocalizationMessages.PARAM_NULL("callback"));

            return register(Injections.getOrCreate(responder.runtime.locator, callback));
        }

        @Override
        public Map<Class<?>, Collection<Class<?>>> register(final Class<?> callback, final Class<?>... callbacks) {
            Preconditions.checkNotNull(callback, LocalizationMessages.PARAM_NULL("callback"));
            Preconditions.checkNotNull(callbacks, LocalizationMessages.CALLBACK_ARRAY_NULL());
            for (final Class<?> additionalCallback : callbacks) {
                Preconditions.checkNotNull(additionalCallback, LocalizationMessages.CALLBACK_ARRAY_ELEMENT_NULL());
            }

            final Map<Class<?>, Collection<Class<?>>> results = new HashMap<>();

            results.put(callback, register(callback));

            for (final Class<?> c : callbacks) {
                results.put(c, register(c));
            }

            return results;
        }

        @Override
        public Collection<Class<?>> register(final Object callback) {
            Preconditions.checkNotNull(callback, LocalizationMessages.PARAM_NULL("callback"));

            final Collection<Class<?>> result = new LinkedList<>();
            for (final AbstractCallbackRunner<?> runner : callbackRunners) {
                if (runner.supports(callback.getClass())) {
                    if (runner.register(callback)) {
                        result.add(runner.getCallbackContract());
                    }
                }
            }

            return result;
        }

        @Override
        public Map<Class<?>, Collection<Class<?>>> register(final Object callback, final Object... callbacks) {
            Preconditions.checkNotNull(callback, LocalizationMessages.PARAM_NULL("callback"));
            Preconditions.checkNotNull(callbacks, LocalizationMessages.CALLBACK_ARRAY_NULL());
            for (final Object additionalCallback : callbacks) {
                Preconditions.checkNotNull(additionalCallback, LocalizationMessages.CALLBACK_ARRAY_ELEMENT_NULL());
            }

            final Map<Class<?>, Collection<Class<?>>> results = new HashMap<>();

            results.put(callback.getClass(), register(callback));

            for (final Object c : callbacks) {
                results.put(c.getClass(), register(c));
            }

            return results;
        }
    }

    /**
     * Abstract composite callback runner.
     * <p/>
     * The runner supports registering multiple callbacks of a specific type and the execute the callback method
     * on all the registered callbacks.
     *
     * @param <T> callback type
     */
    abstract static class AbstractCallbackRunner<T> {

        private final Queue<T> callbacks = new ConcurrentLinkedQueue<>();
        private final Logger logger;

        /**
         * Create new callback runner.
         *
         * @param logger logger instance to be used by the runner to fire logging events.
         */
        protected AbstractCallbackRunner(final Logger logger) {
            this.logger = logger;
        }

        /**
         * Return true if this callback runner supports the {@code callbackClass}.
         *
         * @param callbackClass Callback to be checked.
         * @return True if this callback runner supports the {@code callbackClass}; false otherwise.
         */
        public final boolean supports(final Class<?> callbackClass) {
            return getCallbackContract().isAssignableFrom(callbackClass);
        }

        /**
         * Get the callback contract supported by this callback runner.
         *
         * @return callback contract supported by this callback runner.
         */
        public abstract Class<?> getCallbackContract();

        /**
         * Register new callback instance.
         *
         * @param callback new callback instance to be registered.
         * @return {@code true} upon successful registration, {@code false} otherwise.
         */
        @SuppressWarnings("unchecked")
        public boolean register(final Object callback) {
            return callbacks.offer((T) callback);
        }

        /**
         * Execute all registered callbacks using the supplied invoker.
         *
         * @param invoker invoker responsible for to executing all registered callbacks.
         */
        protected final void executeCallbacks(final Closure<T> invoker) {
            for (final T callback : callbacks) {
                try {
                    invoker.invoke(callback);
                } catch (final Throwable t) {
                    logger.log(Level.WARNING, LocalizationMessages.ERROR_ASYNC_CALLBACK_FAILED(callback.getClass().getName()), t);
                }
            }
        }
    }

    private static class CompletionCallbackRunner
            extends AbstractCallbackRunner<CompletionCallback> implements CompletionCallback {

        private static final Logger LOGGER = Logger.getLogger(CompletionCallbackRunner.class.getName());

        private CompletionCallbackRunner() {
            super(LOGGER);
        }

        @Override
        public Class<?> getCallbackContract() {
            return CompletionCallback.class;
        }

        @Override
        public void onComplete(final Throwable throwable) {
            executeCallbacks(new Closure<CompletionCallback>() {
                @Override
                public void invoke(final CompletionCallback callback) {
                    callback.onComplete(throwable);
                }
            });
        }
    }

    private static class ConnectionCallbackRunner
            extends AbstractCallbackRunner<ConnectionCallback> implements ConnectionCallback {

        private static final Logger LOGGER = Logger.getLogger(ConnectionCallbackRunner.class.getName());

        private ConnectionCallbackRunner() {
            super(LOGGER);
        }

        @Override
        public Class<?> getCallbackContract() {
            return ConnectionCallback.class;
        }

        @Override
        public void onDisconnect(final AsyncResponse disconnected) {
            executeCallbacks(new Closure<ConnectionCallback>() {
                @Override
                public void invoke(final ConnectionCallback callback) {
                    callback.onDisconnect(disconnected);
                }
            });
        }
    }
}
