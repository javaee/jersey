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

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Processes result of the request transformation (successful or not). The response
 * processor also represents the ultimate future request-to-response transformation
 * result.
 * <p />
 * A response processor is invoked when the request-to-response transformation
 * processing is finished. When invoked, the response processor retrieves the
 * transformation result.
 *
 * If the transformation was successful and a response instance
 * is returned, the response processor runs the response instance through the chain of
 * registered response filters and returns a result once finished.
 *
 * In case the request transformation finished with an exception, the response processor
 * tries to map the exception to a response using the registered {@link ExceptionMapper
 * exception mappers} and, if successful, runs the mapped response instance through
 * the chain of registered response filters and returns a result once finished.
 * In case the exception was not mapped to a response, the exception is presented
 * as the ultimate request-to-response transformation result.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ResponseProcessor extends AbstractFuture<Response> implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ResponseProcessor.class.getName());

    /**
     * Injectable context that can be used during the data processing for
     * registering {@link Stage} instances that will be invoked during the
     * response processing.
     */
    public static interface RespondingContext {

        /**
         * Push response transformation function that should be applied
         *
         * @param responseTransformation response transformation function.
         */
        void push(Function<Response, Response> responseTransformation);

        /**
         * Create an ({@link Optional optional} responder chain from all transformation
         * previously pushed into the context.
         *
         * @return created responder chain or {@link Optional#absent() absent value}
         *     in case of no registered transformations.
         */
        Optional<Responder> createStageChain();
    }

    /**
     * Response processor builder that enables "assisted" injection of response
     * processor.
     */
    public static class Builder {

        @Inject
        private RequestScope requestScope;
        @Inject
        private Factory<RespondingContext> respondingCtxProvider;
        @Inject
        private Factory<StagingContext<Response>> responseStagingCtxProvider;

        /**
         *
         * @param requestScope
         * @param exceptionMapper
         * @param respondingCtxProvider
         * @param responseStagingCtxProvider
         */
        public Builder(
                RequestScope requestScope,
                ExceptionMapper<Throwable> exceptionMapper,
                Factory<RespondingContext> respondingCtxProvider,
                Factory<StagingContext<Response>> responseStagingCtxProvider) {
            this.requestScope = requestScope;
            this.respondingCtxProvider = respondingCtxProvider;
            this.responseStagingCtxProvider = responseStagingCtxProvider;
        }

        /**
         * Default constructor meant to be used by injection framework.
         */
        public Builder() {
            // Injection constructor
        }

        public ResponseProcessor build(
                final InvocationCallback callback,
                final ListenableFuture<Response> response,
                final InvocationContext invocationContext,
                final ExceptionMappers exceptionMappers) {

            return new ResponseProcessor(
                    callback,
                    response,
                    invocationContext,
                    requestScope,
                    exceptionMappers,
                    respondingCtxProvider,
                    responseStagingCtxProvider);
        }
    }
    //
    private final InvocationCallback callback;
    private final ListenableFuture<Response> inflectedResponse;
    private final InvocationContext invocationContext;
    //
    private final RequestScope requestScope;
    private final ExceptionMappers exceptionMappers;
    private final Factory<RespondingContext> respondingCtxProvider;
    private final Factory<StagingContext<Response>> responseStagingCtxProvider;

    private ResponseProcessor(
            InvocationCallback callback,
            ListenableFuture<Response> inflectedResponse,
            InvocationContext invocationContext,
            RequestScope requestScope,
            ExceptionMappers exceptionMappers,
            Factory<RespondingContext> respondingCtxProvider,
            Factory<StagingContext<Response>> responseStagingCtxProvider) {
        this.callback = callback;
        this.inflectedResponse = inflectedResponse;
        this.invocationContext = invocationContext;
        this.requestScope = requestScope;
        this.exceptionMappers = exceptionMappers;
        this.respondingCtxProvider = respondingCtxProvider;
        this.responseStagingCtxProvider = responseStagingCtxProvider;
    }

    @Override
    public void run() {
        if (inflectedResponse.isCancelled()) {
            // the request processing has been cancelled; just cancel this future & return
            super.cancel(true);
            return;
        }

        runInScope(new Runnable() {

            @Override
            public void run() {
                Response response;
                try {
                    response = inflectedResponse.get();
                } catch (Exception ex) {
                    final Throwable unwrapped = (ex instanceof ExecutionException) ? ex.getCause() : ex;
                    LOGGER.log(Level.FINE,
                            "Request-to-response transformation finished with an exception.", unwrapped);

                    try {
                        response = mapException(unwrapped);
                    } catch (Exception ex2) {
                        setResult(ex2);
                        return;
                    }

                    if (response == null) {
                        setResult(unwrapped);
                        return;
                    }
                }

                for (int i = 0; i < 2; i++) {
                    try {
                        response = runResponders(response);
                        break;
                    } catch (Exception ex) {
                        LOGGER.log(Level.FINE,
                                "Responder chain execution finished with an exception.", ex);
                        if (i == 0) {
                            // try to map the first responder exception
                            try {
                                response = mapException(ex);
                            } catch (Exception ex2) {
                                setResult(ex2);
                                return;
                            }
                        }

                        if (response == null) {
                            setResult(ex);
                            return;
                        }
                    }
                }

                setResult(response);
            }
        });
    }

    private void runInScope(Runnable task) {
        if (requestScope.isActive()) {
            // running inside a scope already (same-thread execution
            task.run();
        } else {
            try {
                requestScope.enter(invocationContext.popRequestScope());
                task.run();
            } finally {
                requestScope.exit();
            }
        }
    }

    private Response runResponders(Response response) {
        Optional<Responder> responder = respondingCtxProvider.get().createStageChain();

        if (responder.isPresent()) {
            final StagingContext<Response> context = responseStagingCtxProvider.get();

            Pair<Response, Optional<Responder>> continuation = Tuples.of(response, responder);
            while (continuation.right().isPresent()) {
                Responder next = continuation.right().get();
                context.beforeStage(next, continuation.left());
                continuation = next.apply(continuation.left());
                context.afterStage(next, continuation.left());
            }

            return continuation.left();
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private Response mapException(Throwable exception) throws Exception {
        if (!(exception instanceof MappableException)) {
            return null;
        }

        Response response = null;
        exception = exception.getCause();
        if (exception instanceof WebApplicationException) {
            response = ((WebApplicationException) exception).getResponse();
        }
        if ((response == null || !response.hasEntity()) && exceptionMappers != null) {
            javax.ws.rs.ext.ExceptionMapper mapper = exceptionMappers.find(exception.getClass());
            if (mapper != null) {
                response = mapper.toResponse(exception); // may throw exception
            }
        }
        return response;
    }

    private void setResult(Response response) {
        super.set(response);
        notifyCallback(response);
    }

    private void setResult(Throwable exception) {
        super.setException(exception);
        notifyCallback(exception);
    }

    private void notifyCallback(Response response) {
        try {
            callback.result(response);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, LocalizationMessages.CALLBACK_METHOD_INVOCATION_FAILED("result"), ex);
        }
    }

    private void notifyCallback(Throwable exception) {
        try {
            callback.failure(exception);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, LocalizationMessages.CALLBACK_METHOD_INVOCATION_FAILED("failure"), ex);
        }
    }
}
