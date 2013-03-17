/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.servlet_3_sse_1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Item store test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Ignore
public class ItemStoreResourceITCase extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(ItemStoreResourceITCase.class.getName());

    @Override
    protected Application configure() {
        return new ItemStoreApp();
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.register(SseFeature.class);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    /**
     * Test the item addition, addition event broadcasting and item retrieval from {@link ItemStoreResource}.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testItemsStore() throws Exception {
        final int MAX_LISTENERS = 3;

        final List<String> items = Collections.unmodifiableList(Arrays.asList("foo", "bar", "baz"));

        final WebTarget itemsTarget = target("resources/items");
        final CountDownLatch latch = new CountDownLatch(items.size() * MAX_LISTENERS * 2);
        final List<Queue<Integer>> indexQueues = new ArrayList<Queue<Integer>>(MAX_LISTENERS);
        final EventSource[] sources = new EventSource[MAX_LISTENERS];
        final AtomicInteger sizeEventsCount = new AtomicInteger(0);

        for (int i = 0; i < MAX_LISTENERS; i++) {
            final EventSource es = new EventSource(itemsTarget.path("events"), false);
            sources[i] = es;

            final Queue<Integer> indexes = new ConcurrentLinkedQueue<Integer>();
            indexQueues.add(indexes);

            es.register(new EventListener() {
                @Override
                public void onEvent(InboundEvent inboundEvent) {
                    try {
                        if (inboundEvent.getName() == null) {
                            final String data = inboundEvent.getData();
                            LOGGER.info("Received event: " + data);
                            indexes.add(items.indexOf(data));
                        } else if ("size".equals(inboundEvent.getName())) {
                            sizeEventsCount.incrementAndGet();
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error getting event data", e);
                        indexes.add(-999);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        try {
            for (EventSource source : sources) {
                source.open();
                LOGGER.info("Source opened.");
            }

            for (String item : items) {
                final Response response = itemsTarget.request().post(Entity.form(new Form("name", item)));
                assertEquals("Posting new item has failed.", 204, response.getStatus());
                LOGGER.info("Item " + item + " posted.");
            }

            assertTrue("Waiting to receive all events has timed out.", latch.await(3, TimeUnit.SECONDS));
        } finally {
            for (EventSource source : sources) {
                if (source.isOpen()) {
                    // assertTrue("Waiting to close a source has timed out.", source.close(1, TimeUnit.SECONDS));
                    source.close(1, TimeUnit.SECONDS);
                    LOGGER.info("Source closed.");
                }
            }
        }

        String postedItems = itemsTarget.request().get(String.class);
        for (String item : items) {
            assertTrue("Item '" + item + "' not stored on server.", postedItems.contains(item));
        }

        int queueId = 0;
        for (Queue<Integer> indexes : indexQueues) {
            assertEquals("Not received the expected number of events in queue " + queueId, items.size(), indexes.size());
            for (int i = 0; i < items.size(); i++) {
                assertTrue("Event for '" + items.get(i) + "' not received in queue " + queueId, indexes.contains(i));
            }
            queueId++;
        }

        assertEquals("Number of received 'size' events does not match.", items.size() * MAX_LISTENERS, sizeEventsCount.get());
    }
}
