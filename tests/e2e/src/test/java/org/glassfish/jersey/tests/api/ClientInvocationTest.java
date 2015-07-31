/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.api;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link Invocation} E2E API tests.
 *
 * @author Michal Gajdos
 */
public class ClientInvocationTest extends JerseyTest {

    private static final int INVOCATIONS = 5;

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(Resource.class);
    }

    @Path("/")
    public static class Resource {

        @GET
        public String get() {
            return "OK";
        }

        @POST
        public String post(final String entity) {
            return entity;
        }
    }

    @Test
    public void testMultipleSyncInvokerCalls() throws Exception {
        final Invocation.Builder request = target().request();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(request.get().readEntity(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleSyncInvokerCallsAsString() throws Exception {
        final Invocation.Builder request = target().request();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(request.get(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleSyncInvokerCallsAsGenericType() throws Exception {
        final Invocation.Builder request = target().request();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(request.get(new GenericType<String>() {}), is("OK"));
        }
    }

    @Test
    public void testMultipleSyncInvokerCallsWithEntity() throws Exception {
        final Invocation.Builder request = target().request();

        for (int i = 0; i < INVOCATIONS; i++) {
            final String entity = "Message: " + i;
            assertThat(request.post(Entity.text(entity)).readEntity(String.class), is(entity));
        }
    }

    @Test
    public void testMultipleSyncInvokerCallsAsStringWithEntity() throws Exception {
        final Invocation.Builder request = target().request();

        for (int i = 0; i < INVOCATIONS; i++) {
            final String entity = "Message: " + i;
            assertThat(request.post(Entity.text(entity), String.class), is(entity));
        }
    }

    @Test
    public void testMultipleSyncInvokerCallsAsGenericTypeWithEntity() throws Exception {
        final Invocation.Builder request = target().request();

        for (int i = 0; i < INVOCATIONS; i++) {
            final String entity = "Message: " + i;
            assertThat(request.post(Entity.text(entity), new GenericType<String>() {}), is(entity));
        }
    }

    @Test
    public void testMultipleAsyncInvokerCalls() throws Exception {
        final AsyncInvoker request = target().request().async();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(request.get().get().readEntity(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleAsyncInvokerCallsAsString() throws Exception {
        final AsyncInvoker request = target().request().async();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(request.get(String.class).get(), is("OK"));
        }
    }

    @Test
    public void testMultipleAsyncInvokerCallsAsGenericType() throws Exception {
        final AsyncInvoker request = target().request().async();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(request.get(new GenericType<String>() {}).get(), is("OK"));
        }
    }

    @Test
    public void testMultipleAsyncInvokerCallsWithEntity() throws Exception {
        final AsyncInvoker request = target().request().async();

        for (int i = 0; i < INVOCATIONS; i++) {
            final String entity = "Message: " + i;
            assertThat(request.post(Entity.text(entity)).get().readEntity(String.class), is(entity));
        }
    }

    @Test
    public void testMultipleAsyncInvokerCallsAsStringWithEntity() throws Exception {
        final AsyncInvoker request = target().request().async();

        for (int i = 0; i < INVOCATIONS; i++) {
            final String entity = "Message: " + i;
            assertThat(request.post(Entity.text(entity), String.class).get(), is(entity));
        }
    }

    @Test
    public void testMultipleAsyncInvokerCallsAsGenericTypeWithEntity() throws Exception {
        final AsyncInvoker request = target().request().async();

        for (int i = 0; i < INVOCATIONS; i++) {
            final String entity = "Message: " + i;
            assertThat(request.post(Entity.text(entity), new GenericType<String>() {}).get(), is(entity));
        }
    }

    @Test
    public void testMultipleInvocationInvokes() throws Exception {
        final Invocation invocation = target().request().buildGet();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.invoke().readEntity(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationInvokesAsString() throws Exception {
        final Invocation invocation = target().request().buildGet();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.invoke(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationInvokesAsGenericType() throws Exception {
        final Invocation invocation = target().request().buildGet();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.invoke(new GenericType<String>() {}), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationInvokesWithEntity() throws Exception {
        final Invocation invocation = target().request().buildPost(Entity.text("OK"));

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.invoke().readEntity(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationInvokesAsStringWithEntity() throws Exception {
        final Invocation invocation = target().request().buildPost(Entity.text("OK"));

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.invoke(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationInvokesAsGenericTypeWithEntity() throws Exception {
        final Invocation invocation = target().request().buildPost(Entity.text("OK"));

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.invoke(new GenericType<String>() {}), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationSubmits() throws Exception {
        final Invocation invocation = target().request().buildGet();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.submit().get().readEntity(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationSubmitsAsString() throws Exception {
        final Invocation invocation = target().request().buildGet();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.submit(String.class).get(), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationSubmitsAsGenericType() throws Exception {
        final Invocation invocation = target().request().buildGet();

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.submit(new GenericType<String>() {}).get(), is("OK"));
        }
    }

    @Test
    public void testMultipleCallbackInvocationSubmits() throws Exception {
        final Invocation invocation = target().request().buildGet();

        for (int i = 0; i < INVOCATIONS; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<String> response = new AtomicReference<>();

            invocation.submit(new InvocationCallback<String>() {
                @Override
                public void completed(final String s) {
                    response.set(s);
                    latch.countDown();
                }

                @Override
                public void failed(final Throwable throwable) {
                    response.set(throwable.getMessage());
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
            assertThat(response.get(), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationSubmitsWithEntity() throws Exception {
        final Invocation invocation = target().request().buildPost(Entity.text("OK"));

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.submit().get().readEntity(String.class), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationSubmitsAsStringWithEntity() throws Exception {
        final Invocation invocation = target().request().buildPost(Entity.text("OK"));

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.submit(String.class).get(), is("OK"));
        }
    }

    @Test
    public void testMultipleInvocationSubmitsAsGenericTypeWithEntity() throws Exception {
        final Invocation invocation = target().request().buildPost(Entity.text("OK"));

        for (int i = 0; i < INVOCATIONS; i++) {
            assertThat(invocation.submit(new GenericType<String>() {}).get(), is("OK"));
        }
    }

    @Test
    public void testMultipleCallbackInvocationSubmitsWithEntity() throws Exception {
        final Invocation invocation = target().request().buildPost(Entity.text("OK"));

        for (int i = 0; i < INVOCATIONS; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<String> response = new AtomicReference<>();

            invocation.submit(new InvocationCallback<String>() {
                @Override
                public void completed(final String s) {
                    response.set(s);
                    latch.countDown();
                }

                @Override
                public void failed(final Throwable throwable) {
                    response.set(throwable.getMessage());
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
            assertThat(response.get(), is("OK"));
        }
    }
}

