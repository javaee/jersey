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

package org.glassfish.jersey.tests.e2e.client;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the behaviour of the Async client when the {@link java.util.concurrent.Future} is cancelled.
 *
 * <p>
 * Tests, that if the async request future is cancelled by the client,
 * the {@link javax.ws.rs.client.InvocationCallback#completed(Object)} callback is not invoked and that
 * {@link java.util.concurrent.CancellationException} is correctly returned (according to spec.) to
 * {@link javax.ws.rs.client.InvocationCallback#failed(Throwable)} callback method.
 * </p>
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class CancelFutureClientTest extends JerseyTest {

    public static final long MAX_WAITING_SECONDS = 2L;
    final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }

    @Test
    public void testCancelFuture() throws InterruptedException, TimeoutException {
        Future<Response> future = target().path("test").request().async().get(
                new InvocationCallback<Response>() {
                    public void completed(final Response response) {
                        fail("[completed()] callback was invoked, although the Future should have been cancelled.");
                    }

                    public void failed(final Throwable throwable) {
                        assertEquals(CancellationException.class, throwable.getClass());
                        countDownLatch.countDown();
                    }
                }
        );
        if (!future.cancel(true)) {
            fail("The Future could not be canceled.");
        }

        // prevent the test container to stop the method execution before the callbacks can be reached.
        if (!countDownLatch.await(MAX_WAITING_SECONDS * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS)) {
            throw new TimeoutException("Callback was not triggered within the time limit." + countDownLatch.getCount());
        }
    }

    @Path("test")
    public static class TestResource {
        @GET
        public Response get() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return Response.noContent().build();
        }
    }
}
