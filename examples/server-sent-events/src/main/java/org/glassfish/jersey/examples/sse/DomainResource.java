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

import org.glassfish.jersey.media.sse.Broadcaster;
import org.glassfish.jersey.media.sse.EventChannel;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Path("domain")
public class DomainResource {

    private final static Map<Integer, Process> processes = new HashMap<Integer, Process>();

    @Path("start")
    @POST
    public Response post() {
        final Process process = new Process();
        processes.put(process.getId(), process);

        Executors.newCachedThreadPool().execute(process);

        return Response.created(UriBuilder.fromResource(DomainResource.class).path("process/{id}").build(process.getId())).build();
    }

    @Path("process/{id}")
    @Produces(EventChannel.SERVER_SENT_EVENTS)
    @GET
    public EventChannel getProgress(@PathParam("id") int id) {
        final Process process = processes.get(id);

        if(process != null) {
            final EventChannel eventChannel = new EventChannel();
            process.getBroadcaster().registerEventChannel(eventChannel);
            return eventChannel;
        } else {
            throw new WebApplicationException(404);
        }
    }


    static class Process implements Runnable {

        private static final AtomicInteger counter = new AtomicInteger(0);

        int id;
        Broadcaster broadcaster = new Broadcaster();

        public Process() {
            id = counter.incrementAndGet();
        }

        public int getId() {
            return id;
        }

        public Broadcaster getBroadcaster() {
            return broadcaster;
        }

        public void run() {
            try {
                Thread.sleep(1000);
                broadcaster.broadcast("domain-progress", "starting domain " + id + " ...", String.class);
                Thread.sleep(1000);
                broadcaster.broadcast("domain-progress", "50%", String.class);
                Thread.sleep(1000);
                broadcaster.broadcast("domain-progress", "60%", String.class);
                Thread.sleep(1000);
                broadcaster.broadcast("domain-progress", "70%", String.class);
                Thread.sleep(1000);
                broadcaster.broadcast("domain-progress", "99%", String.class);
                Thread.sleep(1000);
                broadcaster.broadcast("domain-progress", "done", String.class);
                broadcaster.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
