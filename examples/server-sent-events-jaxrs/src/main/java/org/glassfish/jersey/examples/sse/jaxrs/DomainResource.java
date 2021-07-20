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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@Path("domain")
public class DomainResource {

    private static final Map<Integer, Process> processes = new ConcurrentHashMap<>();

    @Path("start")
    @POST
    public Response post(@DefaultValue("0") @QueryParam("testSources") int testSources, @Context Sse sse) {
        final Process process = new Process(testSources, sse);
        processes.put(process.getId(), process);

        Executors.newSingleThreadExecutor().execute(process);

        final URI processIdUri = UriBuilder.fromResource(DomainResource.class).path("process/{id}").build(process.getId());
        return Response.created(processIdUri).build();
    }

    @Path("process/{id}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @GET
    public void getProgress(@PathParam("id") int id,
                            @DefaultValue("false") @QueryParam("testSource") boolean testSource,
                            @Context SseEventSink eventSink) {
        final Process process = processes.get(id);

        if (process != null) {
            if (testSource) {
                process.release();
            }
            process.getBroadcaster().register(eventSink);
        } else {
            throw new NotFoundException();
        }
    }

    static class Process implements Runnable {

        private static final AtomicInteger counter = new AtomicInteger(0);

        private final int id;
        private final CountDownLatch latch;
        private final SseBroadcaster broadcaster;
        private final Sse sse;

        Process(int testReceivers, Sse sse) {
            this.sse = sse;
            this.broadcaster = sse.newBroadcaster();
            id = counter.incrementAndGet();
            latch = testReceivers > 0 ? new CountDownLatch(testReceivers) : null;
        }

        int getId() {
            return id;
        }

        SseBroadcaster getBroadcaster() {
            return broadcaster;
        }

        void release() {
            if (latch != null) {
                latch.countDown();
            }
        }

        public void run() {
            try {
                if (latch != null) {
                    // wait for all test EventSources to be registered
                    latch.await(5, TimeUnit.SECONDS);
                }

                broadcaster.broadcast(sse.newEventBuilder()
                                         .name("domain-progress")
                                         .data(String.class, "starting domain " + id + " ...")
                                         .build());
                broadcaster.broadcast(sse.newEventBuilder().name("domain-progress").data(String.class, "50%").build());
                broadcaster.broadcast(sse.newEventBuilder().name("domain-progress").data(String.class, "60%").build());
                broadcaster.broadcast(sse.newEventBuilder().name("domain-progress").data(String.class, "70%").build());
                broadcaster.broadcast(sse.newEventBuilder().name("domain-progress").data(String.class, "99%").build());
                broadcaster.broadcast(sse.newEventBuilder().name("domain-progress").data(String.class, "done").build());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
