/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ChunkedOutput;
import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link ConnectionCallback connection callback}.
 *
 * @author Miroslav Fuksa
 */
public class AsyncCallbackTest extends JerseyTest {

    public static final AtomicBoolean onDisconnectCalled = new AtomicBoolean(false);

    public static CountDownLatch streamClosedSignal;
    public static CountDownLatch callbackCalledSignal;

    @Path("resource")
    public static class Resource {

        @GET
        @ManagedAsync
        @Path("outputStream")
        public void get(@Suspended final AsyncResponse asyncResponse) throws IOException, InterruptedException {
            asyncResponse.register(MyConnectionCallback.class);
            final InputStream is = new InputStream() {
                private int counter = 0;

                @Override
                public int read() throws IOException {
                    return 65 + (++counter % 35);
                }

            };
            asyncResponse.resume(is);
        }

        @GET
        @ManagedAsync
        @Path("chunked")
        public void getChunkedOutput(@Suspended AsyncResponse asyncResponse) throws IOException, InterruptedException {
            asyncResponse.register(MyConnectionCallback.class);
            ChunkedOutput<String> chunkedOutput = new ChunkedOutput<String>(String.class);
            asyncResponse.resume(chunkedOutput);
            for (int i = 0; i < 50000; i++) {
                chunkedOutput.write("something-");
            }
        }
    }

    public static class TestLatch extends CountDownLatch {

        private final String name;
        private final int multiplier;

        public TestLatch(int count, String name, int multiplier) {
            super(count);
            this.name = name;
            this.multiplier = multiplier;
        }

        @Override
        public void countDown() {
            super.countDown();
        }

        @Override
        public void await() throws InterruptedException {
            final boolean success = super.await(10 * multiplier, TimeUnit.SECONDS);
            Assert.assertTrue(
                    Thread.currentThread().getName() + ": Latch [" + name + "] awaiting -> timeout!!!",
                    success);
        }
    }

    @Before
    public void setup() {
        onDisconnectCalled.set(false);
        streamClosedSignal = new TestLatch(1, "streamClosedSignal", getAsyncTimeoutMultiplier());
        callbackCalledSignal = new TestLatch(1, "callbackCalledSignal", getAsyncTimeoutMultiplier());

    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testOutputStream() throws InterruptedException, IOException {
        _testConnectionCallback("resource/outputStream");
    }

    @Test
    public void testChunkedOutput() throws InterruptedException, IOException {
        _testConnectionCallback("resource/chunked");
    }

    private void _testConnectionCallback(String path) throws IOException, InterruptedException {
        final Response response = target().path(path).request().get();
        final InputStream inputStream = response.readEntity(InputStream.class);
        for (int i = 0; i < 500; i++) {
            inputStream.read();
        }
        response.close();
        streamClosedSignal.countDown();
        callbackCalledSignal.await();
        Assert.assertTrue(onDisconnectCalled.get());
    }

    public static class MyConnectionCallback implements ConnectionCallback {

        @Override
        public void onDisconnect(AsyncResponse disconnected) {
            onDisconnectCalled.set(true);
            callbackCalledSignal.countDown();
        }
    }
}
