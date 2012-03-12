/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.sse;

import org.glassfish.jersey.media.sse.Event;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.EventChannelWriter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ServerSentEventsTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(ServerSentEventsResource.class, DomainResource.class, EventChannelWriter.class);
    }

    @Test
    public void testEventSource() throws InterruptedException, URISyntaxException {

        new EventSource(target().path(App.ROOT_PATH), Executors.newCachedThreadPool()) {
            @Override
            public void onEvent(Event event) {
                try {
                    System.out.println("# Received: " + event);
                    System.out.println(event.getData(String.class));

                    assertEquals("message", event.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread.sleep(1000);
        target().path(App.ROOT_PATH).request().post(Entity.text("message"));
        target().path(App.ROOT_PATH).request().delete();
    }

    @Test
    public void testCreateDomain() throws InterruptedException, URISyntaxException {

        // I don't really care what data are there (don't want to add too much complexity for this sample)
        final Response response = target().path("domain/start").request().post(Entity.text("data"), Response.class);
        final ExecutorService executorService = Executors.newCachedThreadPool();

        final AtomicInteger doneCount = new AtomicInteger(0);

        new EventSource(target().path(response.getHeaders().getLocation().toString()), executorService) {

            int messageCount = 0;

            @Override
            public void onEvent(Event event) {
                try {
                    messageCount++;

                    System.out.println("# Received: " + event);
                    System.out.println(event.getData(String.class));

                    if(event.getData(String.class).equals("done")) {
                        assertEquals(6, messageCount);
                        doneCount.incrementAndGet();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        final EventSource eventSource = new EventSource(target().path(response.getHeaders().getLocation().toString()), executorService) {

            int messageCount = 0;

            @Override
            public void onEvent(Event event) {
                try {
                    messageCount++;

                    System.out.println("# Received: " + event);
                    System.out.println(event.getData(String.class));

                    if (event.getData(String.class).equals("done")) {
                        assertEquals(6, messageCount);
                        doneCount.incrementAndGet();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        eventSource.addEventListener(new EventListener() {

            int messageCount = 0;

            @Override
            public void onEvent(Event event) {
                try {
                    messageCount++;

                    System.out.println("# Received: " + event);
                    System.out.println(event.getData(String.class));

                    if(event.getData(String.class).equals("done")) {
                        assertEquals(6, messageCount);
                        doneCount.incrementAndGet();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        eventSource.addEventListener("domain-progress", new EventListener() {

            int messageCount = 0;

            @Override
            public void onEvent(Event event) {
                try {
                    messageCount++;

                    System.out.println("# Received: " + event);
                    System.out.println(event.getData(String.class));

                    if(event.getData(String.class).equals("done")) {
                        assertEquals(6, messageCount);
                        doneCount.incrementAndGet();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        executorService.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(4, doneCount.get());
    }
}
