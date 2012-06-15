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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Incoming event.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InboundEvent {
    private String name = null;
    private String id = null;
    private ByteArrayOutputStream data = null;

    private final MessageBodyWorkers messageBodyWorkers;
    private final Annotation[] annotations;
    private final MediaType mediaType;
    private final MultivaluedMap<String, String> headers;

    /**
     * Constructor.
     *
     * @param messageBodyWorkers {@link MessageBodyWorkers} instance. Used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     * @param annotations annotations from corresponding resource method. Used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     * @param mediaType media type negotiated for corresponding resource method. Used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     * @param headers response headers. Used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     */
    InboundEvent(MessageBodyWorkers messageBodyWorkers, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> headers) {
        this.messageBodyWorkers = messageBodyWorkers;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.headers = headers;
    }

    /**
     * Set {@link InboundEvent} name.
     *
     * @param name event name.
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Set {@link InboundEvent} id.
     *
     * @param id event id.
     */
    void setId(String id) {
        this.id = id;
    }

    /**
     * Add data. Used by {@link EventReceiver}.
     *
     * @param data byte array containing incoming data.
     */
    void addData(byte[] data) {
        if(this.data == null) {
            this.data = new ByteArrayOutputStream();
        }

        try {
            this.data.write(data);
        } catch (IOException e) {
            this.data = null;
        }
    }

    /**
     * Get info about {@link InboundEvent} state.
     *
     * @return {@code true} if current instance does not contain data. {@code false} otherwise.
     */
    boolean isEmpty() {
        return data == null;
    }

    /**
     * Get the event name.
     *
     * @return event name or {@code null} if it is not present.
     */
    public String getName() {
        return name;
    }

    /**
     * Get event data.
     *
     * @param messageType type of stored data content. Will be used for {@link MessageBodyReader} lookup.
     * @return object of given type.
     * @throws IOException when provided type can't be read.
     */
    public <T> T getData(Class<T> messageType) throws IOException {
        return getData(messageType, null);
    }

    /**
     * Get event data.
     *
     * @param messageType type of stored data content. Will be used for {@link MessageBodyReader} lookup.
     * @param mediaType {@link MediaType} of incoming data. Will be used for {@link MessageBodyReader} lookup.
     * @return object of given type.
     * @throws IOException when provided type can't be read.
     */
    public <T> T getData(Class<T> messageType, MediaType mediaType) throws IOException {
        final MessageBodyReader<T> messageBodyReader = messageBodyWorkers.getMessageBodyReader(messageType, null, annotations, mediaType);
        return messageBodyReader.readFrom(messageType, null, annotations, (mediaType == null ? this.mediaType : mediaType),
                headers, new ByteArrayInputStream(stripLastLineBreak(data.toByteArray())));
    }

    /**
     * Get event data as {@link String}.
     *
     * @return event data de-serialized as string.
     * @throws IOException when provided type can't be read.
     */
    public String getData() throws IOException {
        return getData(String.class);
    }

    @Override
    public String toString() {
        String s;

        try {
            s = getData();
        } catch (IOException e) {
            s = "";
        }

        return "InboundEvent{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", data=" + s +
                '}';
    }

    /**
     * String last line break from data. (Last line-break should not be considered as part of received data).
     *
     * @param data data
     * @return updated byte array.
     */
    private byte[] stripLastLineBreak(byte[] data) {
        if((data.length >= 1) && (data[data.length - 1] == '\n')) {
            byte[] newArray = new byte[data.length - 1];
            System.arraycopy(data, 0, newArray, 0, data.length - 1);
            data = newArray;
        }

        return data;
    }
}
