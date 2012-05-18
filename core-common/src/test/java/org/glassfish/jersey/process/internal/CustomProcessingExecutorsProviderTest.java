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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.ProcessingExecutorsModule;
import org.glassfish.jersey.process.internal.ResponseProcessor.RespondingContext;
import org.glassfish.jersey.spi.ProcessingExecutorsProvider;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Test for custom processing executors provider support.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class CustomProcessingExecutorsProviderTest {
    private static final Logger LOGGER = Logger.getLogger(CustomProcessingExecutorsProviderTest.class.getName());

    private static final String REQ_THREAD_NAME = "custom-requesting-thread";
    private static final String RESP_THREAD_NAME = "custom-responding-thread";

    public static class Module extends AbstractModule {

        @Override
        @SuppressWarnings("unchecked")
        protected void configure() {
            final LinearAcceptor inflectingStage = Stages.asLinearAcceptor(new Inflector<Request, Response>() {

                @Override
                public Response apply(Request data) {
                    return Response.ok(Thread.currentThread().getName()).build();
                }
            });

            bind(LinearAcceptor.class).annotatedWith(Stage.Root.class).toInstance(inflectingStage);
            bind(RequestProcessor.class).to(LinearRequestProcessor.class);

            bind(RespondingContext.class).to(DefaultRespondingContext.class).in(RequestScope.class);

            bind().to(ResponseProcessor.Builder.class);
            bind().to(RequestInvoker.class);

        }
    }

    public CustomProcessingExecutorsProviderTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    private Services init() {
        final ProcessingExecutorsProvider customExecutorsProvider = new ProcessingExecutorsProvider() {

            @Override
            public ExecutorService getRequestingExecutor() {
                return Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(REQ_THREAD_NAME + "-%s").build());
            }

            @Override
            public ExecutorService getRespondingExecutor() {
                return Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(RESP_THREAD_NAME + "-%s").build());
            }
        };

        final Services services = HK2.get().create(null,
                new ProcessingTestModule(),
                new ProcessingExecutorsModule(customExecutorsProvider),
                new Module());

        ProcessingTestModule.initProviders(services);

        return services;
    }

    @Test
    public void testCustomProcessingExecutors() throws Exception {
        final Services services = init();
        final RequestInvoker invoker = services.forContract(RequestInvoker.class).get();
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean passed = new AtomicBoolean(false);

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {
                invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("").build(),
                        new AbstractInvocationCallback() {

                            @Override
                            public void result(Response response) {

                                try {
                                    final String reqThreadName = response.readEntity(String.class);
                                    assertTrue("Unexpected request processing thread name: " + reqThreadName,
                                            reqThreadName.startsWith(REQ_THREAD_NAME));

                                    final String respThreadName = Thread.currentThread().getName();
                                    assertTrue("Unexpected response processing thread name: " + respThreadName,
                                            respThreadName.startsWith(RESP_THREAD_NAME));

                                    passed.set(true);
                                } finally {
                                    latch.countDown();
                                }
                            }

                            @Override
                            public void failure(Throwable exception) {
                                try {
                                    LOGGER.log(Level.ALL, "Request processing failed.", exception);
                                    fail(exception.getMessage());
                                } finally {
                                    latch.countDown();
                                }
                            }
                        });
            }

        });
        latch.await();
        assertTrue("Test failed", passed.get());

    }
}
