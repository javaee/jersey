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

import java.lang.reflect.Type;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Representation of a single outbound SSE event.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class OutboundEvent implements OutboundSseEvent {

    private final String name;
    private final String comment;
    private final String id;
    private final GenericType type;
    private final MediaType mediaType;
    private final Object data;
    private final long reconnectDelay;

    /**
     * Used for creating {@link OutboundEvent} instances.
     */
    public static class Builder implements OutboundSseEvent.Builder {

        private String name;
        private String comment;
        private String id;
        private long reconnectDelay = SseFeature.RECONNECT_NOT_SET;
        private GenericType type;
        private Object data;
        private MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        /**
         * Set event name.
         *
         * <p>
         * Will be send as a value of the SSE {@code "event"} field. This field is optional.
         * </p>
         *
         * @param name event name.
         * @return updated builder instance.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set event id.
         * <p>
         * Will be send as a value of the SSE {@code "id"} field. This field is optional.
         * </p>
         *
         * @param id event id.
         * @return updated builder instance.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set reconnection delay (in milliseconds) that indicates how long the event receiver should wait
         * before attempting to reconnect in case a connection to SSE event source is lost.
         * <p>
         * Will be send as a value of the SSE {@code "retry"} field. This field is optional.
         * </p>
         * <p>
         * Absence of a value of this field in an {@link OutboundEvent} instance
         * is indicated by {@link SseFeature#RECONNECT_NOT_SET} value returned from
         * {@link org.glassfish.jersey.media.sse.OutboundEvent#getReconnectDelay()}.
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
         * Set the {@link MediaType media type} of the event data.
         * <p>
         * This information is mandatory. The default value is {@link MediaType#TEXT_PLAIN}.
         * </p>
         *
         * @param mediaType {@link MediaType} of event data. Must not be {@code null}.
         * @return updated builder instance.
         * @throws NullPointerException in case the {@code mediaType} parameter is {@code null}.
         */
        public Builder mediaType(final MediaType mediaType) {
            if (mediaType == null) {
                throw new NullPointerException(LocalizationMessages.OUT_EVENT_MEDIA_TYPE_NULL());
            }
            this.mediaType = mediaType;
            return this;
        }

        /**
         * Set comment string associated with the event.
         *
         * The comment will be serialized with the event, before event data are serialized. If the event
         * does not contain any data, a separate "event" that contains only the comment will be sent.
         * This information is optional, provided the event data are set.
         * <p>
         * Note that multiple invocations of this method result in a previous comment being replaced with a new one.
         * To achieve multi-line comments, a multi-line comment string has to be used.
         * </p>
         *
         * @param comment comment string.
         * @return updated builder instance.
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Set event data and java type of event data.
         *
         * Type information  will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
         * <p>
         * Note that multiple invocations of this method result in previous even data being replaced with new one.
         * </p>
         *
         * @param type java type of supplied data. Must not be {@code null}.
         * @param data event data. Must not be {@code null}.
         * @return updated builder instance.
         * @throws NullPointerException in case either {@code type} or {@code data} parameter is {@code null}.
         */
        public Builder data(Class type, Object data) {
            if (data == null) {
                throw new NullPointerException(LocalizationMessages.OUT_EVENT_DATA_NULL());
            }
            if (type == null) {
                throw new NullPointerException(LocalizationMessages.OUT_EVENT_DATA_TYPE_NULL());
            }

            this.type = new GenericType(type);
            this.data = data;
            return this;
        }

        /**
         * Set event data and a generic java type of event data.
         *
         * Type information will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
         * <p>
         * Note that multiple invocations of this method result in previous even data being replaced with new one.
         * </p>
         *
         * @param type generic type of supplied data. Must not be {@code null}.
         * @param data event data. Must not be {@code null}.
         * @return updated builder instance.
         * @throws NullPointerException in case either {@code type} or {@code data} parameter is {@code null}.
         * @since 2.3
         */
        public Builder data(GenericType type, Object data) {
            if (data == null) {
                throw new NullPointerException(LocalizationMessages.OUT_EVENT_DATA_NULL());
            }
            if (type == null) {
                throw new NullPointerException(LocalizationMessages.OUT_EVENT_DATA_TYPE_NULL());
            }

            this.type = type;
            this.data = data;
            return this;
        }

        /**
         * Set event data and java type of event data.
         *
         * This is a convenience method that derives the event data type information from the runtime type of
         * the event data. The supplied event data may be represented as {@link javax.ws.rs.core.GenericEntity}.
         * <p>
         * Note that multiple invocations of this method result in previous even data being replaced with new one.
         * </p>
         *
         * @param data event data. Must not be {@code null}.
         * @return updated builder instance.
         * @throws NullPointerException in case the {@code data} parameter is {@code null}.
         * @since 2.3
         */
        public Builder data(Object data) {
            if (data == null) {
                throw new NullPointerException(LocalizationMessages.OUT_EVENT_DATA_NULL());
            }

            return data(ReflectionHelper.genericTypeFor(data), data);
        }

        /**
         * Build {@link OutboundEvent}.
         * <p>
         * There are two valid configurations:
         * <ul>
         * <li>if a {@link Builder#comment(String) comment} is set, all other parameters are optional.
         * If event {@link Builder#data(Class, Object) data} and {@link Builder#mediaType(MediaType) media type} is set,
         * event data will be serialized after the comment.</li>
         * <li>if a {@link Builder#comment(String) comment} is not set, at least the event
         * {@link Builder#data(Class, Object) data} must be set. All other parameters are optional.</li>
         * </ul>
         * </p>
         *
         * @return new {@link OutboundEvent} instance.
         * @throws IllegalStateException when called with invalid configuration (neither a comment nor event data are set).
         */
        public OutboundEvent build() {
            if (comment == null && data == null && type == null) {
                throw new IllegalStateException(LocalizationMessages.OUT_EVENT_NOT_BUILDABLE());
            }

            return new OutboundEvent(name, id, reconnectDelay, type, mediaType, data, comment);
        }
    }

    /**
     * Create new OutboundEvent with given properties.
     *
     * @param name           event name (field name "event").
     * @param id             event id.
     * @param reconnectDelay reconnection delay in milliseconds.
     * @param type           java type of events data.
     * @param mediaType      {@link MediaType} of events data.
     * @param data           events data.
     * @param comment        comment.
     */
    OutboundEvent(final String name,
                  final String id,
                  final long reconnectDelay,
                  final GenericType type,
                  final MediaType mediaType,
                  final Object data,
                  final String comment) {
        this.name = name;
        this.comment = comment;
        this.id = id;
        this.reconnectDelay = reconnectDelay;
        this.type = type;
        this.mediaType = mediaType;
        this.data = data;
    }

    /**
     * Get event name.
     * <p>
     * This field is optional. If specified, will be send as a value of the SSE {@code "event"} field.
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
     * This field is optional. If specified, the value is send as a value of the SSE {@code "id"} field.
     * </p>
     *
     * @return event identifier, or {@code null} if not set.
     */
    public String getId() {
        return id;
    }

    /**
     * Get connection retry time in milliseconds the event receiver should wait before attempting to
     * reconnect after a connection to the SSE source is lost.
     * <p>
     * This field is optional. If specified, the value is send as a value of the SSE {@code "retry"} field.
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
     * @return {@code true} if reconnection delay in milliseconds has been set in the event, {@code false} otherwise.
     * @since 2.3
     */
    public boolean isReconnectDelaySet() {
        return reconnectDelay > SseFeature.RECONNECT_NOT_SET;
    }

    /**
     * Get data type.
     * <p>
     * This information is used to select a proper {@link javax.ws.rs.ext.MessageBodyWriter} to be used for
     * serializing the {@link #getData() event data}.
     * </p>
     *
     * @return data type. May return {@code null}, if the event does not contain any data.
     */
    public Class<?> getType() {
        return type == null ? null : type.getRawType();
    }

    /**
     * Get generic data type.
     * <p>
     * This information is used to select a proper {@link javax.ws.rs.ext.MessageBodyWriter} to be used for
     * serializing the {@link #getData() event data}.
     * </p>
     *
     * @return generic data type. May return {@code null}, if the event does not contain any data.
     * @since 2.3
     */
    public Type getGenericType() {
        return type == null ? null : type.getType();
    }

    /**
     * Get {@link MediaType media type} of the event data.
     * <p>
     * This information is used to a select proper {@link javax.ws.rs.ext.MessageBodyWriter} to be used for
     * serializing the {@link #getData() event data}.
     * </p>
     *
     * @return data {@link MediaType}.
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * Get a comment string that accompanies the event.
     * <p>
     * If specified, the comment value is sent with the event as one or more SSE comment lines
     * (depending on line breaks in the actual data string), before any actual event data are serialized.
     * If the event instance does not contain any data, a separate "event" that contains only the comment
     * will be sent. Comment information is optional, provided the event data are set.
     * </p>
     *
     * @return comment associated with the event.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Get event data.
     * <p>
     * The event data, if specified, are serialized and sent as one or more SSE event {@code "data"} fields
     * (depending on the line breaks in the actual serialized data content). The data are serialized
     * using an available {@link javax.ws.rs.ext.MessageBodyWriter} that is selected based on the event
     * {@link #getType() type}, {@link #getGenericType()} generic type} and {@link #getMediaType()} media type}.
     * </p>
     *
     * @return event data. May return {@code null}, if the event does not contain any data.
     */
    public Object getData() {
        return data;
    }
}
