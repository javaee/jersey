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
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestScope.Instance;

import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;

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
        final Instance instance = requestScope.suspendCurrent();
        final ResponseProcessor responseProcessor = responseProcessorBuilder.build(callback);
        final Runnable requester = new Runnable() {

            @Override
            public void run() {
                final AsyncInflectorAdapter asyncInflector = new AsyncInflectorAdapter(new AcceptingInvoker(), callback);

                Ref<InvocationContext> icRef = services.forContract(new TypeLiteral<Ref<InvocationContext>>() {
                }).get();
                icRef.set(asyncInflector);

                ListenableFuture<Response> response = asyncInflector.apply(request);
                responseProcessor.setRequestScopeInstance(instance);
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
                SettableFuture<Response> failedResponse = SettableFuture.create();
                failedResponse.setException(ex);
                return failedResponse;
            } finally {
                callback.failure(ex);
            }
        }
    }

    private class AcceptingInvoker implements Inflector<Request, Response> {

        @Override
        public Response apply(Request request) {
            Pair<Request, Optional<Inflector<Request, Response>>> result;
            try {
                result = requestProcessor.apply(request);
                // MessageBodyProcessingException from Message Body Providers is propagated up and response is not returned
            } catch (final WebApplicationException wae) {
                result = Tuples.of(request,
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

            return inflector.get().apply(result.left());
        }
    }
}
