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

package org.glassfish.jersey.examples.sse.jersey;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ChunkedOutput;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Path("domain")
public class DomainResource {

    private static final Map<Integer, Process> processes = new ConcurrentHashMap<Integer, Process>();

    @Path("start")
    @POST
    public Response post(@DefaultValue("0") @QueryParam("testSources") int testSources) {
        final Process process = new Process(testSources);
        processes.put(process.getId(), process);

        Executors.newSingleThreadExecutor().execute(process);

        final URI processIdUri = UriBuilder.fromResource(DomainResource.class).path("process/{id}").build(process.getId());
        return Response.created(processIdUri).build();
    }

    @Path("process/{id}")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @GET
    public EventOutput getProgress(@PathParam("id") int id,
                                   @DefaultValue("false") @QueryParam("testSource") boolean testSource) {
        final Process process = processes.get(id);

        if (process != null) {
            if (testSource) {
                process.release();
            }
            final EventOutput eventOutput = new EventOutput();
            process.getBroadcaster().add(eventOutput);
            return eventOutput;
        } else {
            throw new NotFoundException();
        }
    }

    static class Process implements Runnable {

        private static final AtomicInteger counter = new AtomicInteger(0);

        private final int id;
        private final CountDownLatch latch;
        private final SseBroadcaster broadcaster = new SseBroadcaster() {
            @Override
            public void onException(ChunkedOutput<OutboundEvent> outboundEventChunkedOutput, Exception exception) {
                exception.printStackTrace();
            }
        };

        public Process(int testReceivers) {
            id = counter.incrementAndGet();
            latch = testReceivers > 0 ? new CountDownLatch(testReceivers) : null;
        }

        public int getId() {
            return id;
        }

        public SseBroadcaster getBroadcaster() {
            return broadcaster;
        }

        public boolean release() {
            if (latch == null) {
                return false;
            }

            latch.countDown();
            return true;
        }

        public void run() {
            try {
                if (latch != null) {
                    // wait for all test EventSources to be registered
                    latch.await(5, TimeUnit.SECONDS);
                }

                broadcaster.broadcast(
                        new OutboundEvent.Builder().name("domain-progress").data(String.class, "starting domain " + id + " ...")
                                .build());
                broadcaster.broadcast(new OutboundEvent.Builder().name("domain-progress").data(String.class, "50%").build());
                broadcaster.broadcast(new OutboundEvent.Builder().name("domain-progress").data(String.class, "60%").build());
                broadcaster.broadcast(new OutboundEvent.Builder().name("domain-progress").data(String.class, "70%").build());
                broadcaster.broadcast(new OutboundEvent.Builder().name("domain-progress").data(String.class, "99%").build());
                broadcaster.broadcast(new OutboundEvent.Builder().name("domain-progress").data(String.class, "done").build());
                broadcaster.closeAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
