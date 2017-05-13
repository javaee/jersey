/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.rx.rxjava;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

import rx.Subscriber;

/**
 * @author Michal Gajdos
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class RxObservableTest {

    private Client client;
    private Client clientWithExecutor;
    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        client = ClientBuilder.newClient().register(TerminalClientRequestFilter.class);
        client.register(RxObservableInvokerProvider.class);
        executor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
                .setNameFormat("jersey-rx-client-test-%d")
                .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                .build());

        clientWithExecutor = ClientBuilder.newBuilder().executorService(executor).build();
        clientWithExecutor.register(TerminalClientRequestFilter.class);
        clientWithExecutor.register(RxObservableInvokerProvider.class);
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();

        client.close();
        client = null;
    }

    @Test
    public void testNotFoundResponse() throws Exception {
        final RxObservableInvoker invoker = client.target("http://jersey.java.net")
                                                  .request()
                                                  .header("Response-Status", 404)
                                                  .rx(RxObservableInvoker.class);

        testInvoker(invoker, 404, false);
    }

    @Test
    public void testNotFoundWithCustomExecutor() throws Exception {
        final RxObservableInvoker invoker = clientWithExecutor.target("http://jersey.java.net")
                                                              .request()
                                                              .header("Response-Status", 404)
                                                              .rx(RxObservableInvoker.class);

        testInvoker(invoker, 404, true);
    }

    @Test(expected = NotFoundException.class)
    public void testNotFoundReadEntityViaClass() throws Throwable {
        try {
            client.target("http://jersey.java.net")
                  .request()
                  .header("Response-Status", 404)
                  .rx(RxObservableInvoker.class)
                  .get(String.class)
                  .toBlocking()
                  .toFuture()
                  .get();
        } catch (final Exception expected) {
            // java.util.concurrent.ExecutionException
            throw expected
                    // javax.ws.rs.ProcessingException
                    // .getCause()
                    // javax.ws.rs.NotFoundException
                    .getCause();
        }
    }

    @Test(expected = NotFoundException.class)
    public void testNotFoundReadEntityViaGenericType() throws Throwable {
        try {
            client.target("http://jersey.java.net")
                  .request()
                  .header("Response-Status", 404)
                  .rx(RxObservableInvoker.class)
                  .get(new GenericType<String>() { })
                  .toBlocking()
                  .toFuture()
                  .get();
        } catch (final Exception expected) {

            expected.printStackTrace();

            // java.util.concurrent.ExecutionException
            throw expected
                    // javax.ws.rs.NotFoundException
                    .getCause();
        }
    }

    @Test
    public void testReadEntityViaClass() throws Throwable {
        final String response = client.target("http://jersey.java.net")
                                      .request()
                                      .rx(RxObservableInvoker.class)
                                      .get(String.class)
                                      .toBlocking()
                                      .toFuture()
                                      .get();

        assertThat(response, is("NO-ENTITY"));
    }

    @Test
    public void testReadEntityViaGenericType() throws Throwable {
        final String response = client.target("http://jersey.java.net")
                                      .request()
                                      .rx(RxObservableInvoker.class)
                                      .get(new GenericType<String>() { })
                                      .toBlocking()
                                      .toFuture()
                                      .get();

        assertThat(response, is("NO-ENTITY"));
    }

    private void testInvoker(final RxObservableInvoker rx, final int expectedStatus, final boolean testDedicatedThread)
            throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Response> responseRef = new AtomicReference<>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<>();

        rx.get().subscribe(new Subscriber<Response>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(final Throwable e) {
                errorRef.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(final Response response) {
                responseRef.set(response);
            }
        });

        latch.await();

        if (errorRef.get() == null) {
            testResponse(responseRef.get(), expectedStatus, testDedicatedThread);
        } else {
            throw (Exception) errorRef.get();
        }
    }

    private static void testResponse(final Response response, final int expectedStatus, final boolean testDedicatedThread) {
        assertThat(response.getStatus(), is(expectedStatus));
        assertThat(response.readEntity(String.class), is("NO-ENTITY"));

        // Executor.
        assertThat(response.getHeaderString("Test-Thread"), testDedicatedThread
                ? containsString("jersey-rx-client-test") : containsString("ForkJoinPool.commonPool"));

        // Properties.
        assertThat(response.getHeaderString("Test-Uri"), is("http://jersey.java.net"));
        assertThat(response.getHeaderString("Test-Method"), is("GET"));
    }
}
