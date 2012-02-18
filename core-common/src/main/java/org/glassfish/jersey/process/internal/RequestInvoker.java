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
package org.glassfish.jersey.process.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Request.RequestBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Request invoker is the main request to response processing entry point. It invokes
 * requests and returns a {@link ListenableFuture listenable response future}.
 * <p/>
 * Request invoker uses an (injectable) {@link RequestProcessor request processor}
 * to run each invoked request through the (injected) request processing stages (acceptors
 * until a terminal request processing stage {@link Inflecting referencing} an
 * {@link Inflector}&lt;{@link Request}, {@link Response}&gt; is reached.
 * The inflector referenced by the terminal request processing stage is then wrapped
 * into a {@link SuspendableInflectorAdapter suspendable inflector} which is subsequently
 * invoked. Once a response from the inflector is available, it is processed by
 * a {@link ResponseProcessor response processor} before it is made available in the
 * response future returned by the request invoker. If a {@link Callback response callback}
 * is supplied, it is invoked at the end of the response processing chain.
 * <p/>
 * Request and response processing tasks are handled by a pair of dedicated customizable
 * {@link ExecutorService executors}, one for request and the other one for response
 * processing. By default, the request processing is executed on the caller thread.
 * If the request processing is not {@link InvocationContext#suspend() suspended}
 * in the inflector, the response processing is by default executed synchronously
 * on the caller thread  too. In case the request processing is suspended, the response
 * processing is resumed in the thread executing the code that resumed the response
 * processing.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class RequestInvoker implements Inflector<Request, ListenableFuture<Response>> {

    /**
     * {@link RequestInvoker} request transformation callback.
     * <p/>
     * The callback is invoked when the request transformation is terminated either
     * successfully or by a failure.
     */
    public static interface Callback {

        /**
         * Invoked after a successful request transformation.
         *
         * @param response request transformation result.
         */
        public void result(Response response);

        /**
         * Invoked in case of a transformation failure.
         *
         * @param exception exception describing the failure.
         */
        public void failure(Throwable exception);
    }
    private static final Callback EMPTY_CALLBACK = new Callback() {

        @Override
        public void result(Response response) {
        }

        @Override
        public void failure(Throwable exception) {
        }
    };

    /**
     * Injectable invocation context that can be used to control various aspects
     * of the invocation, e.g. the threading model.
     */
    public static interface InvocationContext {

        /**
         * Invocation context status.
         */
        public static enum Status {

            /**
             * Indicates the invocation context is running. This is a default state
             * the invocation context is in case the invocation execution flow
             * has not been explicitly modified (yet).
             */
            RUNNING,
            /**
             * Indicates the invocation running in the invocation context has been
             * canceled.
             */
            CANCELLED,
            /**
             * Indicates the invocation running in the invocation context has been
             * suspended.
             *
             * @see InvocationContext#suspend()
             * @see InvocationContext#suspend(long)
             * @see InvocationContext#suspend(long, TimeUnit)
             */
            SUSPENDED,
            /**
             * Indicates the invocation running in the invocation context has been
             * resumed.
             *
             * @see InvocationContext#resume(Response)
             * @see InvocationContext#resume(Throwable)
             */
            RESUMED
        }

        /**
         * Store a request scope snapshot in the internal stack.
         *
         * @param snapshot request scope snapshot to be stored.
         */
        public void pushRequestScope(RequestScope.Snapshot snapshot);

        /**
         * Retrieve a request scope snapshot stored in the internal stack.
         * <p/>
         * Note: the method blocks if no scope snapshot is stored and waits
         * until a snapshot is available.
         *
         * @return the most recently stored request scope snapshot.
         */
        public RequestScope.Snapshot popRequestScope();

        /**
         * Get the current status of the invocation context.
         *
         * @return current status of the invocation context
         */
        public Status status();

        /**
         * Resume the previously suspended request invocation with a response.
         *
         * @param response response to be used in the resumed invocation processing.
         * @throws IllegalStateException in case the invocation context
         *     has not been suspended yet or has already been resumed.
         *
         * @see javax.ws.rs.core.ExecutionContext#resume(Object)
         */
        public void resume(Response response);

        /**
         * Resume the previously suspended request invocation with an exception.
         *
         * @param exception exception to be used in the resumed invocation processing.
         * @throws IllegalStateException in case the invocation context
         *     has not been suspended yet or has already been resumed.
         *
         * @see javax.ws.rs.core.ExecutionContext#resume(Exception)
         */
        public void resume(Throwable exception);

        /**
         * Suspend a request invocation. The method is re-entrant, IOW calling
         * the method multiple times has the same effect as calling it only once.
         *
         * In case the invocation has been {@link Status#RESUMED resumed} or
         * {@link Status#CANCELLED canceled} already, the call to suspend is ignored.
         *
         * @return {@link Future future} representing a handle of the suspended
         *    request invocation that can be used for querying its current state
         *    via one of the {@code Future.isXxx()} methods. The handle can also
         *    be used to {@link Future#cancel(boolean) cancel} the invocation
         *    altogether.
         *
         * @see javax.ws.rs.core.ExecutionContext#suspend()
         */
        public Future<?> suspend();

        /**
         * Suspend a request invocation for up to the specified time in milliseconds.
         * <p/>
         * If called on an already suspended invocation, the existing timeout value
         * is overridden by a new value and the suspension timeout counter is reset.
         * This means that the suspended invocation will time out in:
         * <pre>
         *     System.currentTimeMillis() + timeInMillis
         * </pre>
         * .
         * In case the invocation has been {@link Status#RESUMED resumed} or
         * {@link Status#CANCELLED canceled} already, the call to suspend is ignored.
         *
         * @param timeInMillis suspension timeout in milliseconds.
         * @return {@link Future future} representing a handle of the suspended
         *    request invocation that can be used for querying its current state
         *    via one of the {@code Future.isXxx()} methods. The handle can also
         *    be used to {@link Future#cancel(boolean) cancel} the invocation
         *    altogether.
         *
         * @see javax.ws.rs.core.ExecutionContext#suspend(long)
         */
        public Future<?> suspend(long timeInMillis);

        /**
         * Suspend a request invocation for up to the specified time.
         * <p/>
         * If called on an already suspended invocation, the existing timeout value
         * is overridden by a new value and the suspension timeout counter is reset.
         * This means that the suspended invocation will time out in:
         * <pre>
         *     System.currentTimeMillis() + unit.toMillis(time)
         * </pre>
         * .
         * In case the invocation has been {@link Status#RESUMED resumed} or
         * {@link Status#CANCELLED canceled} already, the call to suspend is ignored.
         *
         * @param time suspension timeout value.
         * @param unit suspension timeout time unit.
         * @return {@link Future future} representing a handle of the suspended
         *    request invocation that can be used for querying its current state
         *    via one of the {@code Future.isXxx()} methods. The handle can also
         *    be used to {@link Future#cancel(boolean) cancel} the invocation
         *    altogether.
         *
         * @see javax.ws.rs.core.ExecutionContext#suspend(long, TimeUnit)
         */
        public Future<?> suspend(long time, TimeUnit unit);

        /**
         * Get the remaining time to the suspension timeout in milliseconds.
         *
         * @return the remaining time to the suspension timeout in milliseconds.
         */
        public long suspesionTimeout();

        /**
         * Set the default response to be used in case the suspended request invocation
         * times out.
         *
         * @param response data to be sent back to the client in case the suspended
         *     request invocation times out.
         *
         * @see javax.ws.rs.core.ExecutionContext#setResponse(Object)
         */
        public void setResponse(Response response);

        /**
         * Returns default response to be send back to the client in case the suspended
         * request invocation times out. The method may return {@code null} if no default
         * response was set in the invocation context.
         *
         * @return default response to be sent back to the client in case the suspended
         *     request invocation times out or {@code null} if no default response
         *     was set.
         *
         * @see javax.ws.rs.core.ExecutionContext#getResponse()
         */
        public Response getResponse();
    }
    //
    @Inject
    private RequestScope requestScope;
    @Inject
    private RequestProcessor requestProcessor;
    @Inject
    private SuspendableInflectorAdapter.Builder suspendableInflectorBuilder;
    @Inject
    private FilteringInflector.Builder filteringInflectorBuilder;
    @Inject
    private ResponseProcessor.Builder responseProcessorBuilder;
    @Inject
    private Services services;
    //
    private final ExecutorService requestingExecutor;
    private final ExecutorService respondingExecutor;

    /**
     * Default constructor meant to be used by the injection framework.
     */
    public RequestInvoker() {
        // Injection constructor

        this.requestingExecutor = MoreExecutors.sameThreadExecutor();
        this.respondingExecutor = MoreExecutors.sameThreadExecutor();
    }

    /**
     * All-fields constructor used for direct (non-injection) construction.
     *
     * @param requestScope
     * @param requestProcessor
     * @param suspendableInflectorBuilder
     * @param responseProcessorBuilder
     * @param requestingExecutor
     * @param respondingExecutor
     */
    public RequestInvoker(
            RequestScope requestScope,
            RequestProcessor requestProcessor,
            SuspendableInflectorAdapter.Builder suspendableInflectorBuilder,
            ResponseProcessor.Builder responseProcessorBuilder,
            ExecutorService requestingExecutor,
            ExecutorService respondingExecutor) {
        this.requestScope = requestScope;
        this.requestProcessor = requestProcessor;
        this.suspendableInflectorBuilder = suspendableInflectorBuilder;
        this.responseProcessorBuilder = responseProcessorBuilder;

        this.requestingExecutor = requestingExecutor;
        this.respondingExecutor = respondingExecutor;
    }

    /**
     * Transform request data of a given type into a response result of the different type.
     *
     * @param request request data to be transformed into a response result.
     * @return future response.
     */
    @Override
    public ListenableFuture<Response> apply(final Request request) {
        return apply(request, EMPTY_CALLBACK);
    }

    /**
     * Transform request data of a given type into a response result of the
     * different type.
     * <p/>
     * After the result is produced the provided {@link Callback result callback}
     * is invoked. The result callback can be invoked on a different thread but
     * still in the same {@link InvocationContext request invocation context}.
     *
     * @param request request data to be transformed into a response result.
     * @param callback result callback called when the request transformation is
     *     done. Must not be {@code null}.
     * @return future response.
     */
    public ListenableFuture<Response> apply(final Request request, final Callback callback) {
        final Callable<ListenableFuture<Response>> requester = new Callable<ListenableFuture<Response>>() {

            @Override
            public ListenableFuture<Response> call() {

                // TODO this seems somewhat too specific to our Message implementation.
                // We should come up with a solution that is more generic
                // Messaging impl specific stuff does not belong to the generic invoker framework
                final Ref<MessageBodyWorkers> workersRef = services.forContract(new TypeLiteral<Ref<MessageBodyWorkers>>() {}).get();
                final RequestBuilder rb = Requests.toBuilder(request);
                Requests.setMessageWorkers(rb, workersRef.get());
                Request requestWithWorkers = rb.build();
                // transform request, inflect and map potential exceptions
                Pair<Request, Optional<Inflector<Request, Response>>> result;

                try {
                    result = requestProcessor.apply(requestWithWorkers);
                } catch (final WebApplicationException wae) {
                    result = Tuples.<Request, Optional<Inflector<Request, Response>>>of(requestWithWorkers,
                            Optional.<Inflector<Request, Response>>of(new Inflector<Request, Response>() {

                        @Override
                        public Response apply(Request data) {
                            return wae.getResponse();
                        }
                    }));
                }

                final Optional<Inflector<Request, Response>> inflector = result.right();
                if (!inflector.isPresent()) {
                    throw new InflectorNotFoundException("Terminal stage did not provide an inflector");
                }

                final Inflector<Request, Response> workersAwareResponseInflector = new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        final Response originalResponse = inflector.get().apply(data);
                        if (originalResponse != null) {
                            final ResponseBuilder rb = Responses.toBuilder(originalResponse);
                            Responses.setMessageWorkers(rb, workersRef.get());
                            return rb.build();
                        } else {
                            return null;
                        }
                    }
                };

                final SuspendableInflectorAdapter suspendableInflector =
                        suspendableInflectorBuilder.build(filteringInflectorBuilder.build(workersAwareResponseInflector));

                ListenableFuture<Response> response = null;
                try {
                    response = suspendableInflector.apply(result.left());
                } finally {
                    final ExceptionMappers exceptionMappers = services.forContract(ExceptionMappers.class).get();
                    final ResponseProcessor responseProcessor =
                            responseProcessorBuilder.build(callback, response, suspendableInflector, exceptionMappers);

                    suspendableInflector.pushRequestScope(requestScope.takeSnapshot());
                    response.addListener(responseProcessor, respondingExecutor);

                    return responseProcessor;
                }
            }
        };

        try {
            try {
                return requestingExecutor.submit(requester).get();
            } catch (InterruptedException ex) {
                throw new ProcessingException(LocalizationMessages.REQUEST_EXECUTION_INTERRUPTED(), ex);
            } catch (ExecutionException ex) {
                final Throwable cause = ex.getCause();
                if (cause instanceof ProcessingException) {
                    throw (ProcessingException) cause;
                } else {
                    throw new ProcessingException(LocalizationMessages.REQUEST_EXECUTION_FAILED(), cause);
                }
            }
        } catch (ProcessingException ex) {
            try {
                SettableFuture<Response> failedResponse = SettableFuture.create();
                failedResponse.setException(ex);
                return failedResponse;
            } finally {
                callback.failure(ex);
            }
        }
    }
}
