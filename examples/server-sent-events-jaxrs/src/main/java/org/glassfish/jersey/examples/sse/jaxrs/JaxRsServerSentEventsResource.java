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

import java.io.IOException;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@Path("server-sent-events")
public class JaxRsServerSentEventsResource {

    private static volatile SseEventSink eventSink = null;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void getMessageQueue(@Context SseEventSink sink) {
        eventSink = sink;
    }

    @POST
    public void addMessage(final String message, @Context Sse sse) throws IOException {
        final SseEventSink localSink = eventSink;
        if (localSink != null) {
            localSink.send(sse.newEventBuilder().name("custom-message").data(String.class, message).build());
        }
    }

    @DELETE
    public void close() throws IOException {
        final SseEventSink localSink = eventSink;
        if (localSink != null) {
            eventSink.close();
        }
        eventSink = null;
    }

    @POST
    @Path("domains/{id}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void startDomain(@PathParam("id") final String id, @Context SseEventSink domainSink, @Context Sse sse) {
        new Thread(() -> {
            try {
                domainSink.send(sse.newEventBuilder()
                                    .name("domain-progress")
                                    .data(String.class, "starting domain " + id + " ...")
                                    .build());
                Thread.sleep(200);
                domainSink.send(sse.newEventBuilder().name("domain-progress").data(String.class, "50%").build());
                Thread.sleep(200);
                domainSink.send(sse.newEventBuilder().name("domain-progress").data(String.class, "60%").build());
                Thread.sleep(200);
                domainSink.send(sse.newEventBuilder().name("domain-progress").data(String.class, "70%").build());
                Thread.sleep(200);
                domainSink.send(sse.newEventBuilder().name("domain-progress").data(String.class, "99%").build());
                Thread.sleep(200);
                domainSink.send(sse.newEventBuilder().name("domain-progress").data(String.class, "done").build());
                domainSink.close();

            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
