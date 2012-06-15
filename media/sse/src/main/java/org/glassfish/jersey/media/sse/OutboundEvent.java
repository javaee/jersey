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

import javax.ws.rs.core.MediaType;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public final class OutboundEvent {

    /**
     * Used for creating {@link OutboundEvent} instances.
     */
    public static class Builder {

        private String name;
        private String comment;
        private String id;
        private Class type;
        private Object data;
        private MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        /**
         * Set event name.
         *
         * Will be send as field name "event".
         *
         * @param name field name "event" value.
         * @return updated builder instance.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set event id.
         *
         * @param id event id.
         * @return updated builder instance.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set {@link MediaType} of event data.
         *
         * <p>When it is set, it will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup. Default value is
         * {@link MediaType#TEXT_PLAIN}.</p>
         *
         * @param mediaType {@link MediaType} of event data.
         * @return updated builder instance.
         */
        public Builder mediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        /**
         * Set comment. It will be send before serialized event if it contains data or as a separate "event".
         *
         * @param comment comment string.
         * @return updated builder instance.
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Set event data and java type of event data. Type will  be used for {@link javax.ws.rs.ext.MessageBodyWriter}
         * lookup.
         *
         * @param type java type of supplied data. MUST NOT be {@code null}.
         * @param data event data. MUST NOT be {@code null}.
         * @return updated builder instance.
         */
        public Builder data(Class type, Object data) {
            if(type == null || data == null) {
                throw new IllegalArgumentException();
            }

            this.type = type;
            this.data = data;
            return this;
        }

        /**
         * Build {@link OutboundEvent}.
         *
         * <p>There are two valid configurations:
         * <ul>
         *     <li>when {@link Builder#comment} is set, all other parameters are optional. If {@link Builder#data(Class, Object)}
         *     and {@link Builder#type} is set, event will be serialized after comment.</li>
         *     <li>when {@link Builder#comment} is not set, {@link Builder#data(Class, Object)} and {@link Builder#type} HAVE TO
         *     be set, all other parameters are optional.</li>
         * </ul></p>
         *
         * @return new {@link OutboundEvent} instance.
         * @throws IllegalStateException when called with invalid configuration.
         */
        public OutboundEvent build() throws IllegalStateException {
            if(comment == null) {
                if((data == null) && (type == null)) {
                    throw new IllegalStateException();
                }
            }

            return new OutboundEvent(name, id, type, mediaType, data, comment);
        }
    }

    private final String name;
    private final String comment;
    private final String id;
    private final Class type;
    private final MediaType mediaType;
    private final Object data;

    /**
     * Create new OutboundEvent with given properties.
     *
     * @param name event name (field name "event").
     * @param id event id.
     * @param type java type of events data.
     * @param mediaType {@link MediaType} of events data.
     * @param data events data.
     * @param comment comment.
     */
    OutboundEvent(String name, String id, Class type, MediaType mediaType, Object data, String comment) {
        this.name = name;
        this.comment = comment;
        this.id = id;
        this.type = type;
        this.mediaType = mediaType;
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
     * Get data {@link MediaType}.
     *
     * @return data {@link MediaType}.
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * Get comment
     *
     * @return comment.
     */
    public String getComment() {
        return comment;
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
