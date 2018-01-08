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
package org.glassfish.jersey.media.sse.internal;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link javax.ws.rs.sse.SseBroadcaster} test.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class JerseySseBroadcasterTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String TEST_EXCEPTION_MSG = "testException";

    @Test
    public void testOnErrorNull() {
        try (JerseySseBroadcaster broadcaster = new JerseySseBroadcaster()) {

            thrown.expect(IllegalArgumentException.class);
            broadcaster.onError(null);
        }
    }

    @Test
    public void testOnCloseNull() {
        try (JerseySseBroadcaster jerseySseBroadcaster = new JerseySseBroadcaster()) {

            thrown.expect(IllegalArgumentException.class);
            jerseySseBroadcaster.onClose(null);
        }
    }

    @Test
    public void testOnErrorFromOnNext() throws InterruptedException {
        try (JerseySseBroadcaster broadcaster = new JerseySseBroadcaster()) {

            final CountDownLatch latch = new CountDownLatch(1);


            broadcaster.onError((subscriber, throwable) -> {
                if (TEST_EXCEPTION_MSG.equals(throwable.getMessage())) {
                    latch.countDown();
                }
            });

            broadcaster.register(new SseEventSink() {
                @Override
                public boolean isClosed() {
                    return false;
                }

                @Override
                public CompletionStage<?> send(OutboundSseEvent event) {
                    throw new RuntimeException(TEST_EXCEPTION_MSG);
                }

                @Override
                public void close() {

                }
            });

            broadcaster.broadcast(new JerseySse().newEvent("ping"));
            Assert.assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnClose() throws InterruptedException {
        try (JerseySseBroadcaster broadcaster = new JerseySseBroadcaster()) {

            final CountDownLatch latch = new CountDownLatch(1);

            final SseEventSink eventSink = new SseEventSink() {
                @Override
                public boolean isClosed() {
                    return false;
                }

                @Override
                public CompletionStage<?> send(OutboundSseEvent event) {
                    return null;
                }

                @Override
                public void close() {

                }
            };
            broadcaster.register(eventSink);

            broadcaster.onClose((s) -> {
                if (s.equals(eventSink)) {
                    latch.countDown();
                }
            });

            broadcaster.close();
            Assert.assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        }
    }

}
