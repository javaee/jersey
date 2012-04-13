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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Request.RequestBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyProcessingException;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestScope.Snapshot;
import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
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
 * into a {@link AsyncInflectorAdapter suspendable inflector} which is subsequently
 * invoked. Once a response from the inflector is available, it is processed by
 * a {@link ResponseProcessor response processor} before it is made available in the
 * response future returned by the request invoker. If a {@link InvocationCallback response callback}
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

    private static final InvocationCallback EMPTY_CALLBACK = new InvocationCallback() {

        @Override
        public void result(final Response response) {
        }

        @Override
        public void failure(final Throwable exception) {
        }

        @Override
        public void suspended(final long time, final TimeUnit unit, final InvocationContext context) {
        }

        @Override
        public void suspendTimeoutChanged(final long time, final TimeUnit unit) {
        }

        @Override
        public void resumed() {
        }

        @Override
        public void cancelled() {
        }
    };
    //
    @Inject
    private RequestScope requestScope;
    @Inject
    private RequestProcessor requestProcessor;
    @Inject
    private FilteringInflector.Builder filteringInflectorBuilder;
    @Inject
    private ResponseProcessor.Builder responseProcessorBuilder;
    @Inject
    private Services services;
    //
    @Inject
    private ProcessingExecutorsFactory executorsFactory;


    /**
     * Default constructor meant to be used by the injection framework.
     */
    public RequestInvoker() {
        // Injection constructor
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
     * After the result is produced the provided {@link InvocationCallback result callback}
     * is invoked. The result callback can be invoked on a different thread but
     * still in the same {@link InvocationContext request invocation context}.
     *
     * @param request request data to be transformed into a response result.
     * @param callback result callback called when the request transformation is
     *     done. Must not be {@code null}.
     * @return future response.
     */
    public ListenableFuture<Response> apply(final Request request, final InvocationCallback callback) {
        // FIXME: Executing request-scoped code.
        //        We should enter and exit request scope here, in the invoker.
        //        All code that needs to run in the scope should be converted
        //        into stages that are executed by the invoker
        //        (e.g. RequestExecutionInitStage).
        final Snapshot scopeSnapshot = requestScope.takeSnapshot();
        final ResponseProcessor responseProcessor = responseProcessorBuilder.build(callback);
        final Runnable requester = new Runnable() {

            @Override
            public void run() {
                if (requestScope.isActive()) {
                    // running inside a scope already (same-thread execution)
                    runInScope();
                } else {
                    try {
                        requestScope.enter(scopeSnapshot);
                        runInScope();
                    } finally {
                        requestScope.exit();
                    }
                }
            }

            public void runInScope() {
                // TODO this seems somewhat too specific to our Message implementation.
                // We should come up with a solution that is more generic
                // Messaging impl specific stuff does not belong to the generic invoker framework
                final MessageBodyWorkers workers =
                        services.forContract(MessageBodyWorkers.class).get();

                final AsyncInflectorAdapter asyncInflector =
                        new AsyncInflectorAdapter(new AcceptingInvoker(workers), callback);

                Ref<InvocationContext> icRef =
                        services.forContract(new TypeLiteral<Ref<InvocationContext>>() {}).get();
                icRef.set(asyncInflector);

                ListenableFuture<Response> response = asyncInflector.apply(request);
                responseProcessor.setRequestScopeSnapshot(requestScope.takeSnapshot());
                response.addListener(responseProcessor, executorsFactory.getRespondingExecutor());
            }
        };

        try {
            try {
                executorsFactory.getRequestingExecutor().submit(requester);
                return responseProcessor;
            } catch (RejectedExecutionException ex) {
                throw new ProcessingException(LocalizationMessages.REQUEST_EXECUTION_FAILED(), ex);
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

    private class AcceptingInvoker implements Inflector<Request, Response> {

        private final MessageBodyWorkers workers;

        public AcceptingInvoker(MessageBodyWorkers workers) {
            this.workers = workers;
        }

        @Override
        public Response apply(Request request) {
            final RequestBuilder rb = Requests.toBuilder(request);
            Requests.setMessageWorkers(rb, workers);
            Request requestWithWorkers = rb.build();

            Pair<Request, Optional<Inflector<Request, Response>>> result;
            try {
                result = requestProcessor.apply(requestWithWorkers);
            } catch (final MessageBodyProcessingException mbpe) {
                 // Handling of exception from Message Body Providers on client -> just throw it and do not return response
                throw mbpe;
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
                        Responses.setMessageWorkers(rb, workers);
                        return rb.build();
                    } else {
                        return null;
                    }
                }
            };

            // FIXME filter processor should be part of the acceptor chain
            final FilteringInflector filteringInflector = filteringInflectorBuilder.build(workersAwareResponseInflector);

            return filteringInflector.apply(result.left());
        }
    }
}
