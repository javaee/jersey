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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.sse.InboundSseEvent;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;

/**
 * Inbound event.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class InboundEvent implements InboundSseEvent {

    private static final GenericType<String> STRING_AS_GENERIC_TYPE = new GenericType<>(String.class);

    private final String name;
    private final String id;
    private final String comment;
    private final byte[] data;
    private final long reconnectDelay;

    private final MessageBodyWorkers messageBodyWorkers;
    private final Annotation[] annotations;
    private final MediaType mediaType;
    private final MultivaluedMap<String, String> headers;

    /**
     * Inbound event builder. This implementation is not thread-safe.
     */
    static class Builder {

        private String name;
        private String id;
        private long reconnectDelay = SseFeature.RECONNECT_NOT_SET;
        private final ByteArrayOutputStream dataStream;

        private final MessageBodyWorkers workers;
        private final Annotation[] annotations;
        private final MediaType mediaType;
        private final MultivaluedMap<String, String> headers;
        private final StringBuilder commentBuilder;

        /**
         * Create new inbound event builder.
         *
         * @param workers     configured client-side {@link MessageBodyWorkers entity providers} used for
         *                    {@link javax.ws.rs.ext.MessageBodyReader} lookup.
         * @param annotations annotations attached to the Java type to be read. Used for
         *                    {@link javax.ws.rs.ext.MessageBodyReader} lookup.
         * @param mediaType   media type of the SSE event data.
         *                    Used for {@link javax.ws.rs.ext.MessageBodyReader} lookup.
         * @param headers     response headers. Used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
         */
        public Builder(MessageBodyWorkers workers,
                       Annotation[] annotations,
                       MediaType mediaType,
                       MultivaluedMap<String, String> headers) {
            this.workers = workers;
            this.annotations = annotations;
            this.mediaType = mediaType;
            this.headers = headers;

            this.commentBuilder = new StringBuilder();
            this.dataStream = new ByteArrayOutputStream();
        }

        /**
         * Set inbound event name.
         * <p/>
         * Value of the received SSE {@code "event"} field.
         *
         * @param name {@code "event"} field value.
         * @return updated builder instance.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set inbound event identifier.
         * <p/>
         * Value of the received SSE {@code "id"} field.
         *
         * @param id {@code "id"} field value.
         * @return updated builder instance.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Add a comment line to the event.
         * <p>
         * The comment line will be added to the received SSE event comment as a new line in the comment field.
         * If the comment line parameter is {@code null}, the call will be ignored.
         * </p>
         *
         * @param commentLine comment line to be added to the event comment.
         * @return updated builder instance.
         * @since 2.21
         */
        public Builder commentLine(final CharSequence commentLine) {
            if (commentLine != null) {
                commentBuilder.append(commentLine).append('\n');
            }

            return this;
        }

        /**
         * Set reconnection delay (in milliseconds) that indicates how long the event receiver should wait
         * before attempting to reconnect in case a connection to SSE event source is lost.
         * <p>
         * Value of the received SSE {@code "retry"} field.
         * </p>
         *
         * @param milliseconds reconnection delay in milliseconds. Negative values un-set the reconnection delay.
         * @return updated builder instance.
         * @since 2.3
         */
        public Builder reconnectDelay(long milliseconds) {
            if (milliseconds < 0) {
                milliseconds = SseFeature.RECONNECT_NOT_SET;
            }
            this.reconnectDelay = milliseconds;
            return this;
        }

        /**
         * Add more inbound event data.
         *
         * @param data byte array containing data stored in the incoming event.
         * @return updated builder instance.
         */
        public Builder write(byte[] data) {
            if (data == null || data.length == 0) {
                return this;
            }

            try {
                this.dataStream.write(data);
            } catch (IOException ex) {
                // ignore - this is not possible with ByteArrayOutputStream
            }
            return this;
        }

        /**
         * Build a new inbound event instance using the supplied data.
         *
         * @return new inbound event instance.
         */
        public InboundEvent build() {
            return new InboundEvent(
                    name,
                    id,
                    commentBuilder.length() > 0 ? commentBuilder.substring(0, commentBuilder.length() - 1) : null,
                    reconnectDelay,
                    dataStream.toByteArray(),
                    workers,
                    annotations,
                    mediaType,
                    headers);
        }
    }

    private InboundEvent(final String name,
                         final String id,
                         final String comment,
                         final long reconnectDelay,
                         final byte[] data,
                         final MessageBodyWorkers messageBodyWorkers,
                         final Annotation[] annotations,
                         final MediaType mediaType,
                         final MultivaluedMap<String, String> headers) {
        this.name = name;
        this.id = id;
        this.comment = comment;
        this.reconnectDelay = reconnectDelay;
        this.data = stripLastLineBreak(data);
        this.messageBodyWorkers = messageBodyWorkers;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.headers = headers;
    }

    /**
     * Get event name.
     * <p>
     * Contains value of SSE {@code "event"} field. This field is optional. Method may return {@code null}, if the event
     * name is not specified.
     * </p>
     *
     * @return event name, or {@code null} if not set.
     */
    public String getName() {
        return name;
    }

    /**
     * Get event identifier.
     * <p>
     * Contains value of SSE {@code "id"} field. This field is optional. Method may return {@code null}, if the event
     * identifier is not specified.
     * </p>
     *
     * @return event id.
     * @since 2.3
     */
    public String getId() {
        return id;
    }

    /**
     * Get a comment string that accompanies the event.
     * <p>
     * Contains value of the comment associated with SSE event. This field is optional. Method may return {@code null},
     * if the event comment is not specified.
     * </p>
     *
     * @return comment associated with the event.
     * @since 2.21
     */
    public String getComment() {
        return comment;
    }

    /**
     * Get new connection retry time in milliseconds the event receiver should wait before attempting to
     * reconnect after a connection to the SSE event source is lost.
     * <p>
     * Contains value of SSE {@code "retry"} field. This field is optional. Method returns {@link SseFeature#RECONNECT_NOT_SET}
     * if no value has been set.
     * </p>
     *
     * @return reconnection delay in milliseconds or {@link SseFeature#RECONNECT_NOT_SET} if no value has been set.
     * @since 2.3
     */
    public long getReconnectDelay() {
        return reconnectDelay;
    }

    /**
     * Check if the connection retry time has been set in the event.
     *
     * @return {@code true} if new reconnection delay has been set in the event, {@code false} otherwise.
     * @since 2.3
     */
    public boolean isReconnectDelaySet() {
        return reconnectDelay > SseFeature.RECONNECT_NOT_SET;
    }

    /**
     * Check if the event is empty (i.e. does not contain any data).
     *
     * @return {@code true} if current instance does not contain any data, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return data.length == 0;
    }

    /**
     * Get the original event data string {@link String}.
     *
     * @return event data de-serialized into a string.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     * @since 2.3
     */
    public String readData() {
        return readData(STRING_AS_GENERIC_TYPE, null);
    }

    /**
     * Read event data as a given Java type.
     *
     * @param type Java type to be used for event data de-serialization.
     * @return event data de-serialized as an instance of a given type.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     * @since 2.3
     */
    public <T> T readData(Class<T> type) {
        return readData(new GenericType<T>(type), null);
    }

    /**
     * Read event data as a given generic type.
     *
     * @param type generic type to be used for event data de-serialization.
     * @return event data de-serialized as an instance of a given type.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     * @since 2.3
     */
    @SuppressWarnings("unused")
    public <T> T readData(GenericType<T> type) {
        return readData(type, null);
    }

    /**
     * Read event data as a given Java type.
     *
     * @param messageType Java type to be used for event data de-serialization.
     * @param mediaType   {@link MediaType media type} to be used for event data de-serialization.
     * @return event data de-serialized as an instance of a given type.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     * @since 2.3
     */
    @SuppressWarnings("unused")
    public <T> T readData(Class<T> messageType, MediaType mediaType) {
        return readData(new GenericType<T>(messageType), mediaType);
    }

    /**
     * Read event data as a given generic type.
     *
     * @param type      generic type to be used for event data de-serialization.
     * @param mediaType {@link MediaType media type} to be used for event data de-serialization.
     * @return event data de-serialized as an instance of a given type.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     * @since 2.3
     */
    public <T> T readData(GenericType<T> type, MediaType mediaType) {
        final MediaType effectiveMediaType = mediaType == null ? this.mediaType : mediaType;
        final MessageBodyReader reader =
                messageBodyWorkers.getMessageBodyReader(type.getRawType(), type.getType(), annotations, mediaType);
        if (reader == null) {
            throw new MessageBodyProviderNotFoundException(LocalizationMessages.EVENT_DATA_READER_NOT_FOUND());
        }
        return readAndCast(type, effectiveMediaType, reader);
    }

    @SuppressWarnings("unchecked")
    private <T> T readAndCast(GenericType<T> type, MediaType effectiveMediaType, MessageBodyReader reader) {
        try {
            return (T) reader.readFrom(
                    type.getRawType(),
                    type.getType(),
                    annotations,
                    effectiveMediaType,
                    headers,
                    new ByteArrayInputStream(data));
        } catch (IOException ex) {
            throw new ProcessingException(ex);
        }
    }

    /**
     * Get the raw event data bytes.
     *
     * @return raw event data bytes. The returned byte array may be empty if the event does not
     * contain any data.
     */
    @SuppressWarnings("unused")
    public byte[] getRawData() {
        if (isEmpty()) {
            return data;
        }

        return Arrays.copyOf(data, data.length);
    }

    @Override
    public String toString() {
        String s;

        try {
            s = readData();
        } catch (ProcessingException e) {
            s = "<Error reading data into a string>";
        }

        return "InboundEvent{"
                + "name='" + name + '\''
                + ", id='" + id + '\''
                + ", comment=" + (comment == null ? "[no comments]" : '\'' + comment + '\'')
                + ", data=" + s
                + '}';
    }

    /**
     * String last line break from data. (Last line-break should not be considered as part of received data).
     *
     * @param data data
     * @return updated byte array.
     */
    private static byte[] stripLastLineBreak(final byte[] data) {

        if (data.length > 0 && data[data.length - 1] == '\n') {
            return Arrays.copyOf(data, data.length - 1);
        }

        return data;
    }
}
