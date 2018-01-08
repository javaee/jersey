/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.sseitemstore.jersey;

import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
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
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.BroadcasterListener;
import org.glassfish.jersey.server.ChunkedOutput;

/**
 * A resource for storing named items.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path("items")
public class ItemStoreResource {
    private static final Logger LOGGER = Logger.getLogger(ItemStoreResource.class.getName());

    private static final ReentrantReadWriteLock storeLock = new ReentrantReadWriteLock();
    private static final LinkedList<String> itemStore = new LinkedList<String>();
    private static final SseBroadcaster broadcaster = new SseBroadcaster();

    static {
        broadcaster.add(new BroadcasterListener<OutboundEvent>() {
            @Override
            public void onException(ChunkedOutput<OutboundEvent> chunkedOutput, Exception exception) {
                LOGGER.log(Level.WARNING,
                        "An exception has been thrown while broadcasting to an event output.",
                        exception);
            }

            @Override
            public void onClose(ChunkedOutput<OutboundEvent> chunkedOutput) {
                LOGGER.log(Level.INFO, "Chunked output has been closed.");
            }
        });
    }

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
     *
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
            broadcaster.closeAll();
            return "Disconnected.";
        } else if (command.length() > "reconnect ".length() && command.startsWith("reconnect ")) {
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
     * Connect or re-connect to SSE event stream.
     *
     * @param lastEventId Value of custom SSE HTTP <tt>{@value SseFeature#LAST_EVENT_ID_HEADER}</tt> header.
     *                    Defaults to {@code -1} if not set.
     * @return new SSE event output stream representing the (re-)established SSE client connection.
     * @throws InternalServerErrorException in case replaying missed events to the reconnected output stream fails.
     * @throws ServiceUnavailableException  in case the reconnect delay is set to a positive value.
     */
    @GET
    @Path("events")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput itemEvents(
            @HeaderParam(SseFeature.LAST_EVENT_ID_HEADER) @DefaultValue("-1") int lastEventId) {
        final EventOutput eventOutput = new EventOutput();

        if (lastEventId >= 0) {
            LOGGER.info("Received last event id :" + lastEventId);

            // decide the reconnect handling strategy based on current reconnect delay value.
            final long delay = reconnectDelay;
            if (delay > 0) {
                LOGGER.info("Non-zero reconnect delay [" + delay + "] - responding with HTTP 503.");
                throw new ServiceUnavailableException(delay);
            } else {
                LOGGER.info("Zero reconnect delay - reconnecting.");
                replayMissedEvents(lastEventId, eventOutput);
            }
        }

        if (!broadcaster.add(eventOutput)) {
            LOGGER.severe("!!! Unable to add new event output to the broadcaster !!!");
            // let's try to force a 5s delayed client reconnect attempt
            throw new ServiceUnavailableException(5L);
        }

        return eventOutput;
    }

    private void replayMissedEvents(final int lastEventId, final EventOutput eventOutput) {
        try {
            storeLock.readLock().lock();
            final int firstUnreceived = lastEventId + 1;
            final int missingCount = itemStore.size() - firstUnreceived;
            if (missingCount > 0) {
                LOGGER.info("Replaying events - starting with id " + firstUnreceived);
                final ListIterator<String> it = itemStore.subList(firstUnreceived, itemStore.size()).listIterator();
                while (it.hasNext()) {
                    eventOutput.write(createItemEvent(it.nextIndex() + firstUnreceived, it.next()));
                }
            } else {
                LOGGER.info("No events to replay.");
            }
        } catch (IOException ex) {
            throw new InternalServerErrorException("Error replaying missed events", ex);
        } finally {
            storeLock.readLock().unlock();
        }
    }

    /**
     * Add new item to the item store.
     *
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
            // Broadcasting an un-named event with the name of the newly added item in data
            broadcaster.broadcast(createItemEvent(eventId, name));
            // Broadcasting a named "size" event with the current size of the items collection in data
            broadcaster.broadcast(new OutboundEvent.Builder().name("size").data(Integer.class, eventId + 1).build());
        } finally {
            storeLock.writeLock().unlock();
        }
    }

    private OutboundEvent createItemEvent(final int eventId, final String name) {
        Logger.getLogger(ItemStoreResource.class.getName()).info("Creating event id [" + eventId + "] name [" + name + "]");
        return new OutboundEvent.Builder().id("" + eventId).data(String.class, name).build();
    }
}
