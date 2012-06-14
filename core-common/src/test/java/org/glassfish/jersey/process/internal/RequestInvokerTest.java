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
import org.glassfish.jersey._remove.Helper;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.TestRuntimeDelegate;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import static org.glassfish.jersey.process.internal.StringAppender.append;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;

import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.base.Function;

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

    @Test
    public void testInvocation() throws Exception {
        final Services services = init();
        final RequestInvoker<Request, Response> invoker = services.forContract(RequestInvoker.Builder.class).get()
                .build(createProcessingRoot());
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {

                invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("").build(),
                        new AbstractInvocationCallback<Response>() {

                            @Override
                            public void result(Response response) {
                                assertEquals(123, response.readEntity(Integer.class).intValue());
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
                Future<Response> result = invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("")
                        .build());
                assertEquals(123, result.get().readEntity(Integer.class).intValue());
                return null;
            }
        });

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {
                invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("text").build(),
                        new AbstractInvocationCallback<Response>() {

                            @Override
                            public void result(Response response) {
                                assertEquals(-1, response.readEntity(Integer.class).intValue());
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
                Future<Response> result = invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("text")
                        .build());
                assertEquals(-1, result.get().readEntity(Integer.class).intValue());
                return null;
            }
        });
    }

    @Test
    public void testWaeThrownInRequestPreProcessingChain() throws Exception {
        final Services services = init();
        final RequestInvoker<Request, Response> invoker = services.forContract(RequestInvoker.Builder.class).get()
                .build(createWaeThrowingProcessingRoot());
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {

                invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("").build(),
                        new AbstractInvocationCallback<Response>() {

                            @Override
                            public void result(Response response) {
                                assertEquals("1", response.readEntity(String.class));
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
                Future<Response> result = invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("")
                        .build());
                assertEquals("1", result.get().readEntity(String.class));
                return null;
            }
        });
    }

    @Test
    public void testArbitraryExceptionThrownInRequestPreProcessingChain() throws Exception {
        final Services services = init();
        final RequestInvoker<Request, Response> invoker = services.forContract(RequestInvoker.Builder.class).get()
                .build(createArbitraryExceptionThrowingProcessingRoot());
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {

                invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("").build(),
                        new AbstractInvocationCallback<Response>() {

                            @Override
                            public void result(Response response) {
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
                Future<Response> result = invoker.apply(Requests.from("http://examples.jersey.java.net/", "GET").entity("")
                        .build());
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

    private static Stage<Request> createProcessingRoot() {

        final Stage<Request> inflectingStage = Stages.asStage(new Inflector<Request, Response>() {

            @Override
            public Response apply(Request data) {
                try {
                    return Responses.from(200, data).entity(Integer.valueOf(Helper.unwrap(data).readEntity(String.class))).build();
                } catch (NumberFormatException ex) {
                    throw new MappableException(ex);
                }
            }
        });

        return Stages.chain(append("1")).to(append("2")).to(append("3")).build(inflectingStage);
    }

    private static Stage<Request> createWaeThrowingProcessingRoot() {

        final Stage<Request> inflectingStage = Stages.asStage(new Inflector<Request, Response>() {

            @Override
            public Response apply(Request data) {
                try {
                    return Responses.from(200, data).entity(Integer.valueOf(Helper.unwrap(data).readEntity(String.class))).build();
                } catch (NumberFormatException ex) {
                    throw new MappableException(ex);
                }
            }
        });

        return Stages.chain(append("1")).to(new Function<Request, Request>() {
                    @Override
                    public Request apply(Request data) {
                        throw new WebApplicationException(
                                Responses.from(200, data).entity(Helper.unwrap(data).readEntity(String.class)).build());
                    }
                }).to(append("3")).build(inflectingStage);
    }

    private static Stage<Request> createArbitraryExceptionThrowingProcessingRoot() {

        final Stage<Request> inflectingStage = Stages.asStage(new Inflector<Request, Response>() {

            @Override
            public Response apply(Request data) {
                try {
                    return Responses.from(200, data).entity(Integer.valueOf(Helper.unwrap(data).readEntity(String.class))).build();
                } catch (NumberFormatException ex) {
                    throw new MappableException(ex);
                }
            }
        });

        return Stages.chain(append("1")).to(new Function<Request, Request>() {
                    @Override
                    public Request apply(Request data) {
                        throw new RuntimeException("test");
                    }
                }).to(append("3")).build(inflectingStage);
    }
}
