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

package org.glassfish.jersey.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.client.RxInvokerProvider;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.ext.Provider;

import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.assertTrue;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Sanity test for {@link Invocation.Builder#rx()} methods.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ClientRxTest {

    public static final ExecutorService EXECUTOR_SERVICE =
            Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("rxTest-%d").build());

    public final Client client = ClientBuilder.newClient();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void afterClass() {
        client.close();
    }

    @Test
    public void testRxInvoker() {
        // explicit register is not necessary, but it can be used.
        client.register(TestRxInvokerProvider.class, RxInvokerProvider.class);

        String s = target(client).request().rx(TestRxInvoker.class).get();

        assertTrue("Provided RxInvoker was not used.", s.startsWith("rxTestInvoker"));
    }

    @Test
    public void testRxInvokerWithExecutor() {
        // implicit register (not saying that the contract is RxInvokerProvider).
        client.register(TestRxInvokerProvider.class);

        ExecutorService executorService = Executors
                .newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("rxTest-%d").build());
        String s = target(client).request().rx(TestRxInvoker.class, EXECUTOR_SERVICE).get();

        assertTrue("Provided RxInvoker was not used.", s.startsWith("rxTestInvoker"));
        assertTrue("Executor Service was not passed to RxInvoker", s.contains("rxTest-"));
    }

    @Test
    public void testRxInvokerInvalid() {
        Invocation.Builder request = target(client).request();
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(AllOf.allOf(new StringContains("null"), new StringContains("clazz")));
        request.rx((Class) null).get();
    }

    @Test
    public void testRxInvokerNotRegistered() {
        Invocation.Builder request = target(client).request();
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(AllOf.allOf(
                new StringContains("TestRxInvoker"),
                new StringContains("not registered"),
                new StringContains("RxInvokerProvider")));
        request.rx(TestRxInvoker.class).get();
    }

    @Test
    public void testRxInvokerWithExecutorInvalid() {
        Invocation.Builder request = target(client).request();
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(AllOf.allOf(new StringContains("null"), new StringContains("clazz")));
        request.rx((Class) null, EXECUTOR_SERVICE);
    }

    private WebTarget target(Client client) {
        // Uri is not relevant, the call won't be ever executed.
        return client.target("http://localhost:9999");
    }

    public static class InvalidInvokerProvider implements RxInvokerProvider<InvalidInvokerProvider>, RxInvoker<Void> {

        @Override
        public InvalidInvokerProvider getRxInvoker(SyncInvoker invocationBuilder, ExecutorService executorService) {
            return null;
        }

        @Override
        public boolean isProviderFor(Class<?> clazz) {
            return true;
        }

        @Override
        public Void get() {
            return null;
        }

        @Override
        public <R> Void get(Class<R> responseType) {
            return null;
        }

        @Override
        public <R> Void get(GenericType<R> responseType) {
            return null;
        }

        @Override
        public Void put(Entity<?> entity) {
            return null;
        }

        @Override
        public <R> Void put(Entity<?> entity, Class<R> responseType) {
            return null;
        }

        @Override
        public <R> Void put(Entity<?> entity, GenericType<R> responseType) {
            return null;
        }

        @Override
        public Void post(Entity<?> entity) {
            return null;
        }

        @Override
        public <R> Void post(Entity<?> entity, Class<R> responseType) {
            return null;
        }

        @Override
        public <R> Void post(Entity<?> entity, GenericType<R> responseType) {
            return null;
        }

        @Override
        public Void delete() {
            return null;
        }

        @Override
        public <R> Void delete(Class<R> responseType) {
            return null;
        }

        @Override
        public <R> Void delete(GenericType<R> responseType) {
            return null;
        }

        @Override
        public Void head() {
            return null;
        }

        @Override
        public Void options() {
            return null;
        }

        @Override
        public <R> Void options(Class<R> responseType) {
            return null;
        }

        @Override
        public <R> Void options(GenericType<R> responseType) {
            return null;
        }

        @Override
        public Void trace() {
            return null;
        }

        @Override
        public <R> Void trace(Class<R> responseType) {
            return null;
        }

        @Override
        public <R> Void trace(GenericType<R> responseType) {
            return null;
        }

        @Override
        public Void method(String name) {
            return null;
        }

        @Override
        public <R> Void method(String name, Class<R> responseType) {
            return null;
        }

        @Override
        public <R> Void method(String name, GenericType<R> responseType) {
            return null;
        }

        @Override
        public Void method(String name, Entity<?> entity) {
            return null;
        }

        @Override
        public <R> Void method(String name, Entity<?> entity, Class<R> responseType) {
            return null;
        }

        @Override
        public <R> Void method(String name, Entity<?> entity, GenericType<R> responseType) {
            return null;
        }
    }

    @Provider
    public static class TestRxInvokerProvider implements RxInvokerProvider<TestRxInvoker> {
        @Override
        public TestRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
            return new TestRxInvoker(syncInvoker, executorService);
        }

        @Override
        public boolean isProviderFor(Class<?> clazz) {
            return TestRxInvoker.class.equals(clazz);
        }
    }

    private static class TestRxInvoker extends AbstractRxInvoker<String> {

        private TestRxInvoker(SyncInvoker syncInvoker, ExecutorService executor) {
            super(syncInvoker, executor);
        }

        @Override
        public <R> String method(String name, Entity<?> entity, Class<R> responseType) {
            return "rxTestInvoker" + (getExecutorService() == null ? "" : " rxTest-");
        }

        @Override
        public <R> String method(String name, Entity<?> entity, GenericType<R> responseType) {
            return "rxTestInvoker" + (getExecutorService() == null ? "" : " rxTest-");
        }
    }
}
