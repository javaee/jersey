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

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestScope.Instance;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;


/**
 * Request invoker is the main request to response data processing entry point. It accepts
 * request data and returns a {@link ListenableFuture listenable response data future}.
 * <p/>
 * Request invoker runs the request data through the (injected) request data processing stages
 * until a terminal request processing stage {@link Inflecting referencing} an
 * {@link Inflector} is reached.
 * The inflector referenced by the terminal request processing stage is then wrapped
 * into a {@link AsyncInflectorAdapter suspendable inflector} which is subsequently
 * invoked. Once a response data from the inflector is available, it is processed by
 * a {@link ResponseProcessor response processor} before it is made available in the
 * response future returned by the request invoker. If a {@link InvocationCallback response callback}
 * is supplied, it is invoked at the end of the response processing chain.
 * <p/>
 * Request and response processing flows are executed in the context of dedicated customizable
 * {@link ExecutorService executors}, one for request and the other one for response
 * data processing. By default, the request processing is executed on the caller thread.
 * If the request processing is not {@link InvocationContext#suspend() suspended}
 * in the inflector, the response processing is by default executed synchronously
 * on the caller thread too. In case the request processing is suspended, the response
 * processing is resumed in the thread executing the code that resumed the response
 * processing.
 *
 * @param <REQUEST> request processing data type.
 * @param <RESPONSE> response processing data type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class RequestInvoker<REQUEST, RESPONSE> {

    private static final InvocationCallback EMPTY_CALLBACK = new InvocationCallback() {

        @Override
        public void result(final Object response) {
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

    /**
     * Injection-enabled {@link RequestInvoker} instance builder.
     */
    public static final class Builder {
        @Inject
        private RequestScope requestScope;
        @Inject
        private ResponseProcessor.Builder<Response> responseProcessorBuilder;
        @Inject
        private Factory<Ref<InvocationContext>> invocationContextReferenceFactory;
        @Inject
        private ProcessingExecutorsFactory executorsFactory;

        /**
         * Build a new {@link RequestInvoker request invoker} configured to use
         * the supplied request processor for processing requests.
         *
         * @param rootStage root processing stage.
         * @return new request invoker instance.
         */
        public RequestInvoker<Request, Response> build(final Stage<Request> rootStage) {

            final AsyncInflectorAdapter.Builder<Request,Response> asyncAdapterBuilder =
                    new AsyncInflectorAdapter.Builder<Request, Response>() {
                        @Override
                        public AsyncInflectorAdapter<Request, Response> create(
                                Inflector<Request, Response> wrapped, InvocationCallback<Response> callback) {
                            return new AsyncInflectorAdapter<Request, Response>(wrapped, callback) {

                                @Override
                                protected Response convertResponse(Response response) {
                                    return response;
                                }
                            };
                        }
                    };

            return new RequestInvoker<Request, Response>(
                    rootStage,
                    requestScope,
                    asyncAdapterBuilder,
                    responseProcessorBuilder,
                    invocationContextReferenceFactory,
                    executorsFactory);
        }

    }

    private final Stage<REQUEST> rootStage;
    private final RequestScope requestScope;
    private final AsyncInflectorAdapter.Builder<REQUEST, RESPONSE> asyncAdapterBuilder;
    private final ResponseProcessor.Builder<RESPONSE> responseProcessorBuilder;
    private final Factory<Ref<InvocationContext>> invocationContextReferenceFactory;
    private final ProcessingExecutorsFactory executorsFactory;

    private RequestInvoker(
            final Stage<REQUEST> rootStage,
            final RequestScope requestScope,
            final AsyncInflectorAdapter.Builder<REQUEST, RESPONSE> asyncAdapterBuilder,
            final ResponseProcessor.Builder<RESPONSE> responseProcessorBuilder,
            final Factory<Ref<InvocationContext>> invocationContextReferenceFactory,
            final ProcessingExecutorsFactory executorsFactory) {

        this.requestScope = requestScope;
        this.rootStage = rootStage;
        this.asyncAdapterBuilder = asyncAdapterBuilder;
        this.responseProcessorBuilder = responseProcessorBuilder;
        this.invocationContextReferenceFactory = invocationContextReferenceFactory;
        this.executorsFactory = executorsFactory;
    }

    /**
     * Transform request data of a given type into a response result of the different type.
     *
     * @param request request data to be transformed into a response result.
     * @return future response.
     */
    @SuppressWarnings("unchecked")
    public ListenableFuture<RESPONSE> apply(final REQUEST request) {
        return apply(request, (InvocationCallback<RESPONSE>) EMPTY_CALLBACK);
    }

    /**
     * Transform request data of a given type into a response result of the
     * different type.
     * <p/>
     * After the result is produced the provided {@link InvocationCallback result callback}
     * is invoked. The result callback can be invoked on a different thread but
     * still in the same {@link InvocationContext request invocation context}.
     *
     * @param request  request data to be transformed into a response result.
     * @param callback result callback called when the request transformation is
     *                 done. Must not be {@code null}.
     * @return future response.
     */
    public ListenableFuture<RESPONSE> apply(final REQUEST request, final InvocationCallback<RESPONSE> callback) {
        // FIXME: Executing request-scoped code.
        //        We should enter and exit request scope here, in the invoker.
        //        All code that needs to run in the scope should be converted
        //        into stages that are executed by the invoker
        //        (e.g. RequestExecutionInitStage).
        final Instance instance = requestScope.suspendCurrent();
        final AsyncInflectorAdapter<REQUEST, RESPONSE> asyncInflector =
                asyncAdapterBuilder.create(new AcceptingInvoker(), callback);
        final ResponseProcessor<RESPONSE> responseProcessor =
                responseProcessorBuilder.build(asyncInflector, callback, instance);
        final Runnable requester = new Runnable() {

            @Override
            public void run() {

                invocationContextReferenceFactory.get().set(asyncInflector);

                ListenableFuture<RESPONSE> response = asyncInflector.apply(request);
                response.addListener(responseProcessor, executorsFactory.getRespondingExecutor());
            }
        };

        try {
            try {
                executorsFactory.getRequestingExecutor().submit(new Runnable() {

                    @Override
                    public void run() {
                        requestScope.runInScope(instance, requester);
                    }
                });
                return responseProcessor;
            } catch (RejectedExecutionException ex) {
                throw new ProcessingException(LocalizationMessages.REQUEST_EXECUTION_FAILED(), ex);
            }
        } catch (ProcessingException ex) {
            try {
                SettableFuture<RESPONSE> failedResponse = SettableFuture.create();
                failedResponse.setException(ex);
                return failedResponse;
            } finally {
                callback.failure(ex);
            }
        }
    }

    private class AcceptingInvoker implements Inflector<REQUEST, RESPONSE> {

        @Override
        public RESPONSE apply(REQUEST request) {
            Stage<REQUEST> lastStage = null;
            Stage.Continuation<REQUEST> continuation = Stage.Continuation.of(request, rootStage);
            Stage<REQUEST> currentStage;
            while ((currentStage = continuation.next()) != null) {
                lastStage = currentStage;
                continuation = currentStage.apply(continuation.result());
            }

            Inflector<REQUEST, RESPONSE> inflector = Stages.extractInflector(lastStage);
            REQUEST result = continuation.result();

            Preconditions.checkState(lastStage != null,
                    "No stage has been invoked as part of the processing.");

            if (inflector == null) {
                throw new InflectorNotFoundException("Terminal stage did not provide an inflector");
            }

            return inflector.apply(result);
        }
    }
}
