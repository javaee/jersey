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
package org.glassfish.jersey.media.sse;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Outgoing event message queue.
 *
 * When returned from resource method, underlying connection is kept open and application
 * is able to write messages. One instance of this class corresponds with exactly one HTTP connection.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class EventChannel {

    /**
     * {@link String} representation of Server sent events media type. ("{@value}").
     */
    public static final String SERVER_SENT_EVENTS = "text/event-stream";

    /**
     * Server sent events media type.
     */
    public static final MediaType SERVER_SENT_EVENTS_TYPE = MediaType.valueOf(SERVER_SENT_EVENTS);

    private final long interval;
    private final TimeUnit timeUnit;

    /**
     * Internal representation of single event.
     */
    class Event {
        private final String name;
        private final String id;
        private final Class type;
        private final Object data;

        /**
         * Create new Event with given properties.
         *
         * @param name event name.
         * @param id event id.
         * @param type type of data.
         * @param data event data.
         */
        Event(String name, String id, Class type, Object data) {
            this.name = name;
            this.id = id;
            this.type = type;
            this.data = data;
        }

        /**
         * Get event name.
         *
         * @return event name.
         */
        public String getName() {
            return name;
        }

        /**
         * Get event id.
         *
         * @return event id.
         */
        public String getId() {
            return id;
        }

        /**
         * Get data type.
         *
         * @return data type.
         */
        public Class getType() {
            return type;
        }

        /**
         * Get event data.
         *
         * @return event data.
         */
        public Object getData() {
            return data;
        }
    }

    private BlockingDeque<Event> queue = new LinkedBlockingDeque<Event>();
    private boolean closed = false;

    /**
     * Default constructor.
     */
    public EventChannel() {
        this(null, null);
    }

    /**
     * Create {@link EventChannel} with specified type and polling interval. Polling interval is used when retrieving
     * data - basically it specifies how often will be connection checked if it is closed from the client side.
     *
     * @param interval polling interval. Default value is {@code 5}.
     * @param timeUnit polling interval {@link TimeUnit}. Default value is {@code TimeUnit.SECONDS}.
     */
    public EventChannel(Long interval, TimeUnit timeUnit) {
        this.interval = (interval == null ? 5 : interval);
        this.timeUnit = (timeUnit == null ? TimeUnit.SECONDS : timeUnit);
    }

    /**
     * Send a message with given parameters.
     *
     * @param eventName event name.
     * @param eventId event id.
     * @param dataType {@link Class} which will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     *          MUST NOT be {@code null}.
     * @param data actual data. MUST NOT be {@code null}.
     * @throws IllegalStateException when trying to write message to closed {@link EventChannel}.
     * @throws IllegalArgumentException when dataType or data is null.
     */
    public void write(String eventName, String eventId, Object data, Class<?> dataType)
            throws IllegalStateException, IllegalArgumentException {
        _send(eventName, eventId, dataType, data);
    }

    /**
     * Send a message with given parameters.
     *
     * @param eventName event name.
     * @param dataType {@link Class} which will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     *          MUST NOT be {@code null}.
     * @param data actual data. MUST NOT be {@code null}.
     * @throws IllegalStateException when trying to write message to closed {@link EventChannel}.
     * @throws IllegalArgumentException when dataType or data is null.
     */
    public void write(String eventName, Object data, Class<?> dataType)
            throws IllegalStateException, IllegalArgumentException {
        _send(eventName, null, dataType, data);
    }

    /**
     * Send a message with given parameters.
     *
     * @param dataType {@link Class} which will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     *          MUST NOT be {@code null}.
     * @param data actual data. MUST NOT be {@code null}.
     * @throws IllegalStateException when trying to write message to closed {@link EventChannel}.
     * @throws IllegalArgumentException when dataType or data is null.
     */
    public void write(Object data, Class<?> dataType) throws IllegalStateException, IllegalArgumentException {
        _send(null, null, dataType, data);
    }

    /**
     * Send a message with given parameters.
     *
     * @param eventName event name.
     * @param eventId event id.
     * @param dataType {@link Class} which will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     * @param data actual data.
     * @throws IllegalStateException when trying to write message to closed {@link EventChannel}.
     * @throws IllegalArgumentException when dataType or data is null.
     */
    private void _send(@Nullable String eventName,
                       @Nullable String eventId,
                       Class<?> dataType, Object data) throws IllegalStateException, IllegalArgumentException {

        if(closed) {
            throw new IllegalStateException();
        }

        if(dataType == null || data == null) {
            throw new IllegalArgumentException();
        }

        queue.add(new Event(eventName, eventId, dataType, data));
    }

    /**
     * Get information about {@link EventChannel} state.
     *
     * @return {@code true} when {@link EventChannel} is closed, otherwise {@code false}.
     */
    public boolean isClosed() {
        return closed == true;
    }

    /**
     * Close underlying outgoing connection
     */
    public void close() {
        queue.add(new Event(null, null, null, null));
        this.closed = true;
    }

    /**
     * Get next message to write (process) by {@link javax.ws.rs.ext.MessageBodyWriter}.
     *
     * @return next {@link EventChannel.Event} to write.
     * @throws InterruptedException when polling from underlying queue is interrupted.
     *
     * @see EventChannel#EventChannel(Long, java.util.concurrent.TimeUnit)
     */
    /* package */ Event getMessageToWrite() throws InterruptedException {
        if(closed) {
            return null;
        }
        return queue.poll(interval, timeUnit);
    }

    /**
     * Close this instance (used only by {@link javax.ws.rs.ext.MessageBodyWriter} when underlying connection is closed).
     */
    /* package */ void clientClose() {
        closed = true;
    }
}
