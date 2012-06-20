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
import java.util.concurrent.Future;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.TestRuntimeDelegate;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.ResponseProcessor.RespondingContext;
import org.glassfish.jersey.spi.ExceptionMappers;
import static org.glassfish.jersey.process.internal.StringAppender.append;

import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Basic request invoker test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class RequestInvokerTest {

    /**
     * Create a test instance.
     */
    public RequestInvokerTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    private Services init() {
        final Services services = HK2.get().create(null, new ProcessingTestModule());

        ProcessingTestModule.initProviders(services);

        return services;
    }

    public static final class String2IntegerRequestInvokerBuilder {

        @Inject
        private RequestScope requestScope;
        @Inject
        Injector injector;
        @Inject
        private Factory<Ref<InvocationContext>> invocationContextReferenceFactory;
        @Inject
        private ProcessingExecutorsFactory executorsFactory;

        public RequestInvoker<String, Integer> build(final Stage<String> rootStage) {
            final AsyncInflectorAdapter.Builder<String, Integer> asyncAdapterBuilder = new AsyncInflectorAdapter.Builder<String, Integer>() {

                @Override
                public AsyncInflectorAdapter<String, Integer> create(Inflector<String, Integer> wrapped, InvocationCallback<Integer> callback) {
                    return new AsyncInflectorAdapter<String, Integer>(wrapped, callback) {

                        @Override
                        protected Integer convertResponse(String request, Response response) {
                            return Integer.parseInt(response.getEntity().toString());
                        }
                    };
                }
            };
            return new RequestInvoker<String, Integer>(rootStage, requestScope, asyncAdapterBuilder, injector.inject(String2IntegerResponseProcessorBuilder.class), invocationContextReferenceFactory, executorsFactory);
        }
    }

    public static class String2IntegerResponseProcessorBuilder implements ResponseProcessor.Builder<Integer> {

        @Inject
        private RequestScope requestScope;
        private Factory<ResponseProcessor.RespondingContext<Integer>> respondingCtxProvider = new Factory<ResponseProcessor.RespondingContext<Integer>>() {

            @Override
            public RespondingContext<Integer> get() throws ComponentException {
                return new DefaultRespondingContext<Integer>();
            }
        };
        @Inject
        private Factory<ExceptionMappers> exceptionMappersProvider;

        /**
         * Default constructor meant to be used by injection framework.
         */
        public String2IntegerResponseProcessorBuilder() {
        }

        @Override
        public ResponseProcessor<Integer> build(
                final Future<Integer> inflectedResponse,
                final SettableFuture<Integer> processedResponse,
                final InvocationCallback<Integer> callback,
                final RequestScope.Instance scopeInstance) {

            return new ResponseProcessor<Integer>(
                    callback,
                    inflectedResponse,
                    processedResponse,
                    respondingCtxProvider,
                    scopeInstance,
                    requestScope,
                    exceptionMappersProvider) {

                @Override
                protected Integer convertResponse(Response exceptionResponse) {
                    return Integer.parseInt(exceptionResponse.getEntity().toString());
                }
            };
        }
    }

    @Test
    public void testInvocation() throws Exception {
        final Services services = init();
        final RequestInvoker<String, Integer> invoker = services.forContract(String2IntegerRequestInvokerBuilder.class).get().build(createProcessingRoot());
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        invoker.apply("",
                new AbstractInvocationCallback<Integer>() {

                    @Override
                    public void result(Integer response) {
                        assertEquals(123, response.intValue());
                    }

                    @Override
                    public void failure(Throwable exception) {
                        fail(exception.getMessage());
                    }
                });

        Future<Integer> result = invoker.apply("");
        assertEquals(123, result.get().intValue());

        invoker.apply("text",
                new AbstractInvocationCallback<Integer>() {

                    @Override
                    public void result(Integer response) {
                        assertEquals(-1, response.intValue());
                    }

                    @Override
                    public void failure(Throwable exception) {
                        fail(exception.getMessage());
                    }
                });

        result = invoker.apply("text");
        assertEquals(-1, result.get().intValue());
    }

    @Test
    public void testWaeThrownInRequestPreProcessingChain() throws Exception {
        final Services services = init();
        final RequestInvoker<String, Integer> invoker = services.forContract(String2IntegerRequestInvokerBuilder.class).get().build(createWaeThrowingProcessingRoot());
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {

                invoker.apply("",
                        new AbstractInvocationCallback<Integer>() {

                            @Override
                            public void result(Integer response) {
                                assertEquals("1", response.toString());
                            }

                            @Override
                            public void failure(Throwable exception) {
                                fail(exception.getMessage());
                            }
                        });
            }
        });

        requestScope.runInScope(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                Future<Integer> result = invoker.apply("");
                assertEquals("1", result.get().toString());
                return null;
            }
        });
    }

    @Test
    public void testArbitraryExceptionThrownInRequestPreProcessingChain() throws Exception {
        final Services services = init();
        final RequestInvoker<String, Integer> invoker = services.forContract(String2IntegerRequestInvokerBuilder.class).get().build(createArbitraryExceptionThrowingProcessingRoot());
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {

                invoker.apply("",
                        new AbstractInvocationCallback<Integer>() {

                            @Override
                            public void result(Integer response) {
                                fail("Failure callback method expected to be invoked.");
                            }

                            @Override
                            public void failure(Throwable exception) {
                                // unwinding ProcessingTestModule exception mapper's exception
                                assertTrue("Unexpected exception type", "test".equals(exception.getCause().getMessage()));
                            }
                        });
            }
        });

        requestScope.runInScope(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                Future<Integer> result = invoker.apply("");
                try {
                    result.get();
                    fail("ExecutionException expected to be raised.");
                } catch (ExecutionException ex) {
                    // unwinding ExecutionException and ProcessingTestModule exception mapper's exception
                    assertTrue("Unexpected exception type", "test".equals(ex.getCause().getCause().getMessage()));
                } catch (Exception ex) {
                    fail("ExecutionException expected to be raised.");
                }

                return null;
            }
        });
    }

    private static Stage<String> createProcessingRoot() {

        final Stage<String> inflectingStage = Stages.asStage(new Inflector<String, Integer>() {

            @Override
            public Integer apply(String data) {
                try {
                    return Integer.valueOf(data);
                } catch (NumberFormatException ex) {
                    throw new MappableException(ex);
                }
            }
        });

        return Stages.chain(append("1")).to(append("2")).to(append("3")).build(inflectingStage);
    }

    private static Stage<String> createWaeThrowingProcessingRoot() {

        final Stage<String> inflectingStage = Stages.asStage(new Inflector<String, Integer>() {

            @Override
            public Integer apply(String data) {
                try {
                    return Integer.valueOf(data);
                } catch (NumberFormatException ex) {
                    throw new MappableException(ex);
                }
            }
        });

        return Stages.chain(append("1")).to(new Function<String, String>() {

            @Override
            public String apply(String data) {
                throw new WebApplicationException(Response.ok(data).build());
            }
        }).to(append("3")).build(inflectingStage);
    }

    private static Stage<String> createArbitraryExceptionThrowingProcessingRoot() {

        final Stage<String> inflectingStage = Stages.asStage(new Inflector<String, Integer>() {

            @Override
            public Integer apply(String data) {
                try {
                    return Integer.valueOf(data);
                } catch (NumberFormatException ex) {
                    throw new MappableException(ex);
                }
            }
        });

        return Stages.chain(append("1")).to(new Function<String, String>() {

            @Override
            public String apply(String data) {
                throw new RuntimeException("test");
            }
        }).to(append("3")).build(inflectingStage);
    }
}
