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
package org.glassfish.jersey.internal.util;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Flow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test Jersey {@link javax.ws.rs.Flow.Publisher} implementation, {@link JerseyPublisher}.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class JerseyPublisherTest {

    private static final Logger LOGGER = Logger.getLogger(JerseyPublisherTest.class.getName());

    @Test
    public void test() throws InterruptedException {

        final CountDownLatch openLatch1 = new CountDownLatch(1);
        final CountDownLatch openLatch2 = new CountDownLatch(1);
        final CountDownLatch openLatch3 = new CountDownLatch(1);

        final CountDownLatch writeLatch1 = new CountDownLatch(3);
        final CountDownLatch writeLatch2 = new CountDownLatch(2);
        final CountDownLatch writeLatch3 = new CountDownLatch(1);

        final CountDownLatch closeLatch = new CountDownLatch(3);

        final JerseyPublisher<String> publisher = new JerseyPublisher<>();
        final PublisherTestSubscriber subscriber1 =
                new PublisherTestSubscriber("SUBSCRIBER-1", openLatch1, writeLatch1, closeLatch);
        final PublisherTestSubscriber subscriber2 =
                new PublisherTestSubscriber("SUBSCRIBER-2", openLatch2, writeLatch2, closeLatch);
        final PublisherTestSubscriber subscriber3 =
                new PublisherTestSubscriber("SUBSCRIBER-3", openLatch3, writeLatch3, closeLatch);

        publisher.submit("START");  // sent before any subscriber subscribed - should not be received

        publisher.subscribe(subscriber1);
        publisher.submit("Zero");   // before receive, but should be received by SUBSCRIBER-1
        assertTrue(openLatch1.await(200, TimeUnit.MILLISECONDS));

        subscriber1.receive(3);
        publisher.submit("One");    // should be received by SUBSCRIBER-1

        publisher.subscribe(subscriber2);
        assertTrue(openLatch2.await(200, TimeUnit.MILLISECONDS));
        subscriber2.receive(5);

        publisher.submit("Two");    // should be received by SUBSCRIBER-1 and SUBSCRIBER-2

        publisher.subscribe(subscriber3);
        assertTrue(openLatch3.await(200, TimeUnit.MILLISECONDS));
        subscriber3.receive(5);

        publisher.submit("Three");  // should be received by SUBSCRIBER-2 and SUBSCRIBER-3

        assertTrue(writeLatch1.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(writeLatch2.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(writeLatch3.await(1000, TimeUnit.MILLISECONDS));

        Queue<String> result = subscriber1.getReceivedData();
        assertEquals(3, result.size());
        assertEquals("Zero", result.poll());
        assertEquals("One", result.poll());
        assertEquals("Two", result.poll());

        result = subscriber2.getReceivedData();
        assertEquals(2, result.size());
        assertEquals("Two", result.poll());
        assertEquals("Three", result.poll());

        result = subscriber3.getReceivedData();
        assertEquals(1, result.size());
        assertEquals("Three", result.poll());

        publisher.close();
        subscriber1.receive(1);     // --> with this, the CDL is successfully counted down and await returns true
        assertTrue(closeLatch.await(10000, TimeUnit.MILLISECONDS));
        // - the strange thing is, that all SubmissionPublisher attempts to invoke all the three callbacks
        //   and gets to the sysout behind the onComplete() call for all the three subscribers (see output)
    }

    class PublisherTestSubscriber implements Flow.Subscriber<String> {
        private final String name;
        private final CountDownLatch openLatch;
        private final CountDownLatch writeLatch;
        private final CountDownLatch closeLatch;
        private Flow.Subscription subscription;
        private final Queue<String> data;

        PublisherTestSubscriber(final String name,
                                final CountDownLatch openLatch,
                                final CountDownLatch writeLatch,
                                final CountDownLatch closeLatch) {
            this.name = name;
            this.openLatch = openLatch;
            this.writeLatch = writeLatch;
            this.closeLatch = closeLatch;
            this.data = new ConcurrentLinkedQueue<>();
        }

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            this.subscription = subscription;
            openLatch.countDown();
        }

        @Override
        public void onNext(final String item) {
            data.add(item);
            writeLatch.countDown();
            LOGGER.info("[" + name + "] (" + Thread.currentThread().getName() + ") " + item);
        }

        @Override
        public void onError(final Throwable throwable) {
            LOGGER.log(Level.INFO, "[" + name + "] (" + Thread.currentThread().getName() + ") onError()", throwable);
        }

        @Override
        public void onComplete() {
            LOGGER.info("[" + name + "] (" + Thread.currentThread().getName() + ") onComplete()");
            closeLatch.countDown();
        }

        @Override
        public String toString() {
            return this.name + " " + Thread.currentThread().getName();
        }

        public void receive(final long n) {
            if (subscription != null) {
                subscription.request(n);
            } else {
                System.out.println("[" + this.name + "] Subscription is null");
            }
        }

        /**
         * Retrieve stored (received) data for assertions.
         *
         * @return all the data received by subscriber.
         */
        Queue<String> getReceivedData() {
            return this.data;
        }

        /**
         * Instruct subscriber to request {@code n} items.
         *
         * @param n amount of items to request
         */
        public void request(final long n) {
            this.subscription.request(n);
        }
    }
}
