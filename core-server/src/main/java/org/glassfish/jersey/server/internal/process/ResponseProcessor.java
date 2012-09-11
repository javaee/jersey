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
package org.glassfish.jersey.server.internal.process;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Provider;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.process.internal.ChainableStage;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.spi.ExceptionMappers;

import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;

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
 * tries to map the exception to a response using the registered {@link javax.ws.rs.ext.ExceptionMapper
 * exception mappers} and, if successful, runs the mapped response instance through
 * the chain of registered response filters and returns a result once finished.
 * In case the exception was not mapped to a response, the exception is presented
 * as the ultimate request-to-response transformation result.
 *
 * @param <DATA> processed data type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class ResponseProcessor<DATA> implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ResponseProcessor.class.getName());

    /**
     * Injectable context that can be used during the data processing for
     * registering response processing functions that will be invoked during the
     * response processing.
     *
     * @param <DATA> processed data type.
     */
    public static interface RespondingContext<DATA> {

        /**
         * Push response transformation function that should be applied.
         *
         * @param responseTransformation response transformation function.
         */
        void push(Function<DATA, DATA> responseTransformation);

        /**
         * Push chainable response transformation stage that should be applied.
         *
         * @param stage response transformation chainable stage.
         */
        void push(ChainableStage<DATA> stage);

        /**
         * (Optionally) create a responder chain from all transformations
         * previously pushed into the context.
         *
         * @return created responder chain root or {@code null} in case of no
         *         registered transformations.
         */
        Stage<DATA> createResponderRoot();
    }

    /**
     * Response processor factory.
     *
     * @param <DATA> processed data type.
     */
    public static interface Builder<DATA> {

        /**
         * Create a new response processor for a given request / response message exchange.
         *
         * @param inflectedResponse inflected response data future.
         * @param processedResponse settable future that will be used to set the response
         *                          processing result.
         * @param callback          the invocation callback to be invoked once the
         *                          response processing has finished.
         * @param scopeInstance     the instance of the request scope this processor
         *                          belongs to.
         * @return new response processor instance.
         */
        public ResponseProcessor<DATA> build(
                final Future<DATA> inflectedResponse,
                final SettableFuture<DATA> processedResponse,
                final ProcessingCallback<DATA> callback,
                final RequestScope.Instance scopeInstance);
    }

    private final RequestScope requestScope;
    private volatile RequestScope.Instance scopeInstance;
    private final ProcessingCallback<DATA> callback;
    private final Future<DATA> inflectedResponse;
    private final SettableFuture<DATA> processedResponse;
    private final Provider<RespondingContext<DATA>> respondingCtxProvider;
    private final Provider<ExceptionMappers> exceptionMappersProvider;

    /**
     * Create new response processor for a given request / response message exchange.
     *
     * @param callback                 the invocation callback to be invoked once the
     *                                 response processing has finished.
     * @param inflectedResponse        inflected response data future.
     * @param processedResponse        settable future that will be used to set the response
     *                                 processing result.
     * @param respondingCtxProvider    responding context provider.
     * @param scopeInstance            the instance of the request scope this processor
     *                                 belongs to.
     * @param requestScope             Jersey request scope.
     * @param exceptionMappersProvider exception mappers provide.
     */
    protected ResponseProcessor(
            final ProcessingCallback<DATA> callback,
            final Future<DATA> inflectedResponse,
            SettableFuture<DATA> processedResponse, final Provider<RespondingContext<DATA>> respondingCtxProvider,
            final RequestScope.Instance scopeInstance,
            final RequestScope requestScope,
            final Provider<ExceptionMappers> exceptionMappersProvider) {
        this.processedResponse = processedResponse;
        this.requestScope = requestScope;
        this.scopeInstance = scopeInstance;
        this.callback = callback;
        this.inflectedResponse = inflectedResponse;

        this.respondingCtxProvider = respondingCtxProvider;
        this.exceptionMappersProvider = exceptionMappersProvider;
    }

    @Override
    public void run() {
        try {
            requestScope.runInScope(scopeInstance, new Runnable() {

                @Override
                public void run() {
                    if (inflectedResponse.isCancelled()) {
                        // the request processing has been cancelled; just cancel this future & return
                        processedResponse.cancel(true);
                        return;
                    }

                    DATA response;
                    try {
                        response = inflectedResponse.get();
                    } catch (Throwable ex) {
                        final Throwable unwrapped = (ex instanceof ExecutionException) ? ex.getCause() : ex;
                        LOGGER.log(Level.FINE,
                                "Request-to-response transformation finished with an exception.", unwrapped);

                        try {
                            response = convertResponse(mapException(unwrapped));
                        } catch (Throwable ex2) {
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
                        } catch (Throwable ex) {
                            LOGGER.log(Level.FINE,
                                    "Responder chain execution finished with an exception.", ex);
                            if (i == 0) {
                                // try to map the first responder exception
                                try {
                                    response = convertResponse(mapException(ex));
                                } catch (Throwable ex2) {
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
        } finally {
            scopeInstance.release();
        }
    }

    private DATA runResponders(DATA response) {
        Stage<DATA> responder = respondingCtxProvider.get().createResponderRoot();

        if (responder != null) {
            Stage.Continuation<DATA> continuation = Stage.Continuation.of(response, responder);
            while (continuation.hasNext()) {
                continuation = continuation.next().apply(continuation.result());
            }

            return continuation.result();
        }

        return response;
    }

    /**
     * Convert an exception-mapped JAX-RS {@link Response response} to supported
     * processing data type.
     *
     * @param exceptionResponse a processing exception mapped to a JAX-RS response.
     * @return JAX-RS exception-mapped response transformed to supported processing
     *         data type.
     */
    protected abstract DATA convertResponse(Response exceptionResponse);

    @SuppressWarnings("unchecked")
    private Response mapException(Throwable exception) throws Exception {
        Response response = null;

        if (exception instanceof MappableException) {
            exception = exception.getCause();
        }

        if (exception instanceof WebApplicationException) {
            response = ((WebApplicationException) exception).getResponse();
        }
        final ExceptionMappers exceptionMappers = exceptionMappersProvider.get();
        if ((response == null || !response.hasEntity()) && exceptionMappers != null) {
            ExceptionMapper mapper = exceptionMappers.find(exception.getClass());
            if (mapper != null) {
                response = mapper.toResponse(exception); // may throw exception
            }
        }
        return response;
    }

    private void setResult(DATA response) {
        try {
            processedResponse.set(response);
        } finally {
            notifyCallback(response);
        }
    }

    private void setResult(Throwable exception) {
        try {
            processedResponse.setException(exception);
        } finally {
            notifyCallback(exception);
        }
    }

    private void notifyCallback(DATA response) {
        try {
            callback.result(response);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    LocalizationMessages.CALLBACK_METHOD_INVOCATION_FAILED("result", Thread.currentThread().getName()), ex);
        }
    }

    private void notifyCallback(Throwable exception) {
        try {
            callback.failure(exception);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    LocalizationMessages.CALLBACK_METHOD_INVOCATION_FAILED("failure", Thread.currentThread().getName()), ex);
        }
    }
}
