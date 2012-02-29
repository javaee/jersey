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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.process.internal.ResponseProcessor.RespondingContext;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class RequestInvokerTest {

    private static abstract class AbstractInvocationCallback implements InvocationCallback {

        @Override
        public void cancelled() {
        }

        @Override
        public void failure(Throwable exception) {
        }

        @Override
        public void result(Response response) {
        }

        @Override
        public void resumed() {
        }

        @Override
        public void suspended(long time, TimeUnit unit, InvocationContext context) {
        }
    }

    public static class Module extends AbstractModule {

        @Override
        @SuppressWarnings("unchecked")
        protected void configure() {

            bind(RespondingContext.class).to(DefaultRespondingContext.class).in(RequestScope.class);

            bind().to(ResponseProcessor.Builder.class);
            bind().to(RequestInvoker.class);
        }
    }

    public RequestInvokerTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    private Services init(org.glassfish.hk2.Module acceptorsModule) {
        final Services services = HK2.get().create(null,
                new ProcessingTestModule(),
                acceptorsModule,
                new Module());

        ProcessingTestModule.initProviders(services);

        return services;
    }

    @Test
    public void testLinear() throws Exception {
        final Services services = init(new LinearRequestProcessorTest.Module());
        final RequestInvoker invoker = services.forContract(RequestInvoker.class).get();
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        try {
            requestScope.enter();

            invoker.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("").build(),
                    new AbstractInvocationCallback() {

                        @Override
                        public void result(Response response) {
                            assertEquals(123, response.readEntity(Integer.class).intValue());
                        }

                        @Override
                        public void failure(Throwable exception) {
                            fail(exception.getMessage());
                        }
                    });

            Future<Response> result = invoker.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("").build());
            assertEquals(123, result.get().readEntity(Integer.class).intValue());

            invoker.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("text").build(),
                    new AbstractInvocationCallback() {

                        @Override
                        public void result(Response response) {
                            assertEquals(-1, response.readEntity(Integer.class).intValue());
                        }

                        @Override
                        public void failure(Throwable exception) {
                            fail(exception.getMessage());
                        }
                    });

            result = invoker.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("text").build());
            assertEquals(-1, result.get().readEntity(Integer.class).intValue());
        } finally {
            requestScope.exit();
        }
    }

    @Test
    public void testHiearchical() throws Exception {
        final Services services = init(new HierarchicalRequestProcessorTest.Module());
        final RequestInvoker invoker = services.forContract(RequestInvoker.class).get();
        final RequestScope requestScope = services.forContract(RequestScope.class).get();

        try {
            requestScope.enter();
            invoker.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("").build(),
                    new AbstractInvocationCallback() {

                        @Override
                        public void result(Response response) {
                            assertEquals(145, response.readEntity(Integer.class).intValue());
                        }

                        @Override
                        public void failure(Throwable exception) {
                            fail(exception.getMessage());
                        }
                    });

            Future<Response> result = invoker.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("").build());
            assertEquals(145, result.get().readEntity(Integer.class).intValue());

            invoker.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("text").build(),
                    new AbstractInvocationCallback() {

                        @Override
                        public void result(Response response) {
                            assertEquals(-1, response.readEntity(Integer.class).intValue());
                        }

                        @Override
                        public void failure(Throwable exception) {
                            fail(exception.getMessage());
                        }
                    });

            result = invoker.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("text").build());
            assertEquals(-1, result.get().readEntity(Integer.class).intValue());

        } finally {
            requestScope.exit();
        }
    }
}
