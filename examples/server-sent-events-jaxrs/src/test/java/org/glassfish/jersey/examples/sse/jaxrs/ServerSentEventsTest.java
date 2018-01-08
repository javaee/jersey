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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.sse.jaxrs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;

import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * SSE example resources test.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class ServerSentEventsTest extends JerseyTest {

    private final ExecutorService executorService =
            Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ServerSentEventsTest-%d").build());
    private final Client client = ClientBuilder.newBuilder().executorService(executorService).build();

    @Override
    protected Application configure() {
        // enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(JaxRsServerSentEventsResource.class, DomainResource.class);
    }

    @Override
    protected Client getClient() {
        return client;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        client.close();
        executorService.shutdown();
    }

    /**
     * Test consuming a single SSE event via event source.
     *
     * @throws Exception in case of a failure during the test execution.
     */
    @Test
    public void testEventSource() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> message = new AtomicReference<>();
        final SseEventSource eventSource = SseEventSource.target(target().path(App.ROOT_PATH)).build();
        eventSource.register((inboundEvent) -> {
            final String value = inboundEvent.readData();
            message.set(value);
            latch.countDown();
        });

        eventSource.open();
        target().path(App.ROOT_PATH).request().post(Entity.text("message"));

        try {
            assertTrue("Waiting for message to be delivered has timed out.",
                    latch.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS));
        } finally {
            eventSource.close();
        }
        assertThat("Unexpected SSE event data value.", message.get(), equalTo("message"));
    }


    /**
     * Test receiving all streamed messages in parallel by multiple event sources.
     *
     * @throws Exception in case of a failure during the test execution.
     */
    @Test
    public void testCreateDomain() throws Exception {
        final int MAX_CLIENTS = 25;
        final int MESSAGE_COUNT = 6;

        final Response response = target().path("domain/start")
                .queryParam("testSources", MAX_CLIENTS)
                .request().post(Entity.text("data"), Response.class);

        assertThat("Unexpected start domain response status code.",
                response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));

        final Map<Integer, Integer> messageCounts = new ConcurrentHashMap<>(MAX_CLIENTS);
        final CountDownLatch doneLatch = new CountDownLatch(MAX_CLIENTS);
        final SseEventSource[] sources = new SseEventSource[MAX_CLIENTS];

        final String processUriString = target().getUri().relativize(response.getLocation()).toString();

        final WebTarget sseTarget = target().path(processUriString).queryParam("testSource", "true");
        for (int i = 0; i < MAX_CLIENTS; i++) {
            final AtomicInteger messageCount = new AtomicInteger(0);  // will this work?
            final int id = i;
            sources[id] = SseEventSource.target(sseTarget).build();
            sources[id].register((event) -> {
                messageCount.incrementAndGet();
                final String message = event.readData(String.class);
                if ("done".equals(message)) {
                    messageCounts.put(id, messageCount.get());
                    doneLatch.countDown();
                }
            });
            sources[i].open();
        }

        doneLatch.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);

        for (SseEventSource source : sources) {
            source.close();
        }

        for (int i = 0; i < MAX_CLIENTS; i++) {
            final Integer count = messageCounts.get(i);
            assertThat("Final message not received by event source " + i, count, notNullValue());
            assertThat("Unexpected number of messages received by event source " + i,
                    count, equalTo(MESSAGE_COUNT));
        }
    }
}
