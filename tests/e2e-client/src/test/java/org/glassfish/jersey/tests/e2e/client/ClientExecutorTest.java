/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.MessageBodyReader;

import org.glassfish.jersey.client.ClientAsyncExecutor;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ClientExecutorTest extends JerseyTest {

    @Path("ClientExecutorTest")
    public static class ClientExecutorTestResource {

        @GET
        @Produces("text/plain")
        public String get() {
            return "get";
        }
    }


    @Override
    protected Application configure() {
        return new ResourceConfig(ClientExecutorTestResource.class);
    }

    private volatile String threadName = null;

    @Test
    public void testCustomExecutorRx() throws InterruptedException {

        ExecutorService clientExecutor =
                Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ClientExecutor-%d").build());

        Client client = ClientBuilder.newBuilder().executorService(clientExecutor).build();

        final CountDownLatch latch = new CountDownLatch(1);

        testRx(client, latch);

        latch.await(3, TimeUnit.SECONDS);

        assertNotNull(threadName);
        assertThat(threadName, containsString("ClientExecutor"));
    }

    @Test
    public void testDefaultExecutorRx() throws InterruptedException {

        Client client = ClientBuilder.newClient();

        final CountDownLatch latch = new CountDownLatch(1);

        testRx(client, latch);

        latch.await(3, TimeUnit.SECONDS);
        assertNotNull(threadName);
        assertThat(threadName, containsString("jersey-client-async-executor"));
    }

    @Test
    public void testDefaultExecutorAsync() throws InterruptedException {

        Client client = ClientBuilder.newClient();

        final CountDownLatch latch = new CountDownLatch(1);

        testAsync(client, latch);

        latch.await(3, TimeUnit.SECONDS);
        assertNotNull(threadName);
        assertThat(threadName, containsString("jersey-client-async-executor"));
    }

    @Test
    public void testJerseyCustomExecutorAsync() throws InterruptedException {
        Client client = ClientBuilder.newClient();
        client.register(MyExecutorProvider.class);

        final CountDownLatch latch = new CountDownLatch(1);

        testAsync(client, latch);

        latch.await(3, TimeUnit.SECONDS);
        assertNotNull(threadName);
        assertThat(threadName, containsString("MyExecutorProvider"));
    }


    private void testRx(Client client, CountDownLatch latch) {
        client.target(UriBuilder.fromUri(getBaseUri()).path("ClientExecutorTest"))
              .register(new MessageBodyReader<ClientExecutorTest>() {
                  @Override
                  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                      return true;
                  }

                  @Override
                  public ClientExecutorTest readFrom(Class<ClientExecutorTest> type, Type genericType,
                                                     Annotation[] annotations,
                                                     MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                                     InputStream entityStream) throws IOException, WebApplicationException {

                      ClientExecutorTest.this.threadName = Thread.currentThread().getName();
                      latch.countDown();

                      return new ClientExecutorTest();

                  }
              })
              .request()
              .rx()
              .get(ClientExecutorTest.class);
    }

    private void testAsync(Client client, CountDownLatch latch) {
        client.target(UriBuilder.fromUri(getBaseUri()).path("ClientExecutorTest"))
              .register(new MessageBodyReader<ClientExecutorTest>() {
                  @Override
                  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                      return true;
                  }

                  @Override
                  public ClientExecutorTest readFrom(Class<ClientExecutorTest> type, Type genericType,
                                                     Annotation[] annotations,
                                                     MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                                     InputStream entityStream) throws IOException, WebApplicationException {

                      ClientExecutorTest.this.threadName = Thread.currentThread().getName();
                      latch.countDown();

                      return new ClientExecutorTest();

                  }
              })
              .request()
              .async()
              .get(ClientExecutorTest.class);
    }

    @ClientAsyncExecutor
    public static class MyExecutorProvider implements ExecutorServiceProvider {

        public final ExecutorService executorService =
                Executors
                        .newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("MyExecutorProvider-%d").build());

        @Override
        public ExecutorService getExecutorService() {
            return executorService;
        }

        @Override
        public void dispose(ExecutorService executorService) {
            executorService.shutdown();
        }
    }
}
