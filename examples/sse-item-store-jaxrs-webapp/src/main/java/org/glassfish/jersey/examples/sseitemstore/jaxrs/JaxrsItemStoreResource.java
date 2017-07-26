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

package org.glassfish.jersey.examples.sseitemstore.jaxrs;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.glassfish.jersey.media.sse.SseFeature;


/**
 * A resource for storing named items.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path("items")
public class JaxrsItemStoreResource {
    private static final Logger LOGGER = Logger.getLogger(JaxrsItemStoreResource.class.getName());

    private static final ReentrantReadWriteLock storeLock = new ReentrantReadWriteLock();
    private static final LinkedList<String> itemStore = new LinkedList<>();

    private static final AtomicReference<SseBroadcaster> BROADCASTER = new AtomicReference<>(null);

    @Context
    private Sse sse;

    private static volatile long reconnectDelay = 0;

    /**
     * List all stored items.
     *
     * @return list of all stored items.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String listItems() {
        try {
            storeLock.readLock().lock();
            return itemStore.toString();
        } finally {
            storeLock.readLock().unlock();
        }
    }

    /**
     * Receive & process commands sent by the test client that control the internal resource state.
     * <p>
     * Following is the list of recognized commands:
     * <ul>
     * <li><b>disconnect</b> - disconnect all registered event streams.</li>
     * <li><b>reconnect now</b> - enable client reconnecting.</li>
     * <li><b>reconnect &lt;seconds&gt;</b> - disable client reconnecting.
     * Reconnecting clients will receive a HTTP 503 response with
     * {@value javax.ws.rs.core.HttpHeaders#RETRY_AFTER} set to the amount of
     * milliseconds specified.</li>
     * </ul>
     *
     * @param command command to be processed.
     * @return message about processing result.
     * @throws BadRequestException in case the command is not recognized or not specified.
     */
    @POST
    @Path("commands")
    public String processCommand(String command) {
        if (command == null || command.isEmpty()) {
            throw new BadRequestException("No command specified.");
        }

        if ("disconnect".equals(command)) {
            closeBroadcaster();
            return "Disconnected.";
        } else if ("reconnect ".length() < command.length() && command.startsWith("reconnect ")) {
            final String when = command.substring("reconnect ".length());
            try {
                reconnectDelay = "now".equals(when) ? 0 : Long.parseLong(when);
                return "Reconnect strategy updated: " + when;
            } catch (NumberFormatException ignore) {
                // ignored
            }
        }

        throw new BadRequestException("Command not recognized: '" + command + "'");
    }

    /**
     * TODO - rewrite Connect or re-connect to SSE event stream.
     *
     * @param lastEventId Value of custom SSE HTTP <tt>{@value SseFeature#LAST_EVENT_ID_HEADER}</tt> header.
     *                    Defaults to {@code -1} if not set.
     * @throws InternalServerErrorException in case replaying missed events to the reconnected output stream fails.
     * @throws ServiceUnavailableException  in case the reconnect delay is set to a positive value.
     */
    @GET
    @Path("events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void itemEvents(@HeaderParam(SseFeature.LAST_EVENT_ID_HEADER) @DefaultValue("-1") int lastEventId,
                           @Context SseEventSink eventSink) {

        if (lastEventId >= 0) {
            LOGGER.info("Received last event id :" + lastEventId);

            // decide the reconnect handling strategy based on current reconnect delay value.
            final long delay = reconnectDelay;
            if (0 < delay) {
                LOGGER.info("Non-zero reconnect delay [" + delay + "] - responding with HTTP 503.");
                throw new ServiceUnavailableException(delay);
            } else {
                LOGGER.info("Zero reconnect delay - reconnecting.");
                replayMissedEvents(lastEventId, eventSink);
            }
        }

        getBroadcaster().register(eventSink);
    }

    private void replayMissedEvents(int lastEventId, SseEventSink eventSink) {
        try {
            storeLock.readLock().lock();
            final int firstUnreceived = lastEventId + 1;
            final int missingCount = itemStore.size() - firstUnreceived;
            if (missingCount > 0) {
                LOGGER.info("Replaying events - starting with id " + firstUnreceived);
                final ListIterator<String> it = itemStore.subList(firstUnreceived, itemStore.size()).listIterator();
                while (it.hasNext()) {
                    eventSink.send(createItemEvent(it.nextIndex() + firstUnreceived, it.next()));
                }
            } else {
                LOGGER.info("No events to replay.");
            }
        } finally {
            storeLock.readLock().unlock();
        }
    }

    /**
     * Add new item to the item store.
     * <p>
     * Invoking this method will fire 2 new SSE events - 1st about newly added item and 2nd about the new item store size.
     *
     * @param name item name.
     */
    @POST
    public void addItem(@FormParam("name") String name) {
        // Ignore if the request was sent without name parameter.
        if (name == null) {
            return;
        }

        final int eventId;
        try {
            storeLock.writeLock().lock();
            eventId = itemStore.size();
            itemStore.add(name);

            SseBroadcaster sseBroadcaster = getBroadcaster();

            // Broadcasting an un-named event with the name of the newly added item in data
            sseBroadcaster.broadcast(createItemEvent(eventId, name));

            // Broadcasting a named "size" event with the current size of the items collection in data
            final OutboundSseEvent event = sse.newEventBuilder().name("size").data(Integer.class, eventId + 1).build();
            sseBroadcaster.broadcast(event);
        } finally {
            storeLock.writeLock().unlock();
        }
    }

    private OutboundSseEvent createItemEvent(final int eventId, final String name) {
        Logger.getLogger(JaxrsItemStoreResource.class.getName())
                .info("Creating event id [" + eventId + "] name [" + name + "]");
        return sse.newEventBuilder().id("" + eventId).data(String.class, name).build();
    }

    /**
     * Get stored broadcaster or create a new one and store it.
     *
     * @return broadcaster instance.
     */
    private SseBroadcaster getBroadcaster() {
        SseBroadcaster sseBroadcaster = BROADCASTER.get();
        if (sseBroadcaster == null) {
            BROADCASTER.compareAndSet(null, sse.newBroadcaster());
        }

        return BROADCASTER.get();
    }

    /**
     * Close currently stored broadcaster.
     */
    private void closeBroadcaster() {
        SseBroadcaster sseBroadcaster = BROADCASTER.getAndSet(null);
        if (sseBroadcaster == null) {
            return;
        }
        sseBroadcaster.close();
    }
}
