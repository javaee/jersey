/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Client-side single inbound Server-Sent Event reader.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@ConstrainedTo(RuntimeType.CLIENT)
class InboundEventReader implements MessageBodyReader<InboundEvent> {
    private static final byte[] EOL_DATA = new byte[]{'\n'};

    @Inject
    private Provider<MessageBodyWorkers> messageBodyWorkers;

    private static enum State {
        NEW_LINE,
        COMMENT,
        FIELD,
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return InboundEvent.class.equals(type) && mediaType.equals(SseFeature.SERVER_SENT_EVENTS_TYPE);
    }

    @Override
    public InboundEvent readFrom(Class<InboundEvent> type,
                                 Type genericType,
                                 Annotation[] annotations,
                                 MediaType mediaType,
                                 MultivaluedMap<String, String> headers,
                                 InputStream entityStream) throws IOException, WebApplicationException {
        /**
         * SSE Event parsing based on:
         *
         * http://dev.w3.org/html5/eventsource/
         * last editors draft from 13 March 2012
         */
        final ByteArrayOutputStream tokenData = new ByteArrayOutputStream();
        InboundEvent.Builder eventBuilder = new InboundEvent.Builder(messageBodyWorkers.get(), annotations, mediaType, headers);

        int b = -1;
        State currentState = State.NEW_LINE;
        loop:
        do {
            switch (currentState) {
                case NEW_LINE:
                    b = entityStream.read();
                    if (b == '\n' || b == -1) {
                        break loop;
                    }

                    if (b == ':') {
                        currentState = State.COMMENT;
                    } else {
                        tokenData.write(b);
                        currentState = State.FIELD;
                    }
                    break;
                case COMMENT:
                    // skipping comment data
                    b = readLineUntil(entityStream, '\n', null);
                    currentState = State.NEW_LINE;
                    break;
                case FIELD:
                    // read field name
                    b = readLineUntil(entityStream, ':', tokenData);
                    final String fieldName = tokenData.toString();
                    tokenData.reset();

                    if (b == ':') {
                        // read field value
                        b = entityStream.read();
                        if (b == ' ') {
                            // first space in value has to be skipped
                            b = entityStream.read();
                        }

                        if (b != '\n' && b != -1) {
                            tokenData.write(b);
                            b = readLineUntil(entityStream, '\n', tokenData);
                        }
                    }

                    processField(eventBuilder, fieldName, tokenData.toByteArray());
                    tokenData.reset();

                    currentState = State.NEW_LINE;
                    break;
            }
        } while (b != -1);

        return eventBuilder.build();
    }

    /**
     * Read input stream until a delimiter or {@code EOL ('\n')} or {@code EOF} is reached
     * and write the read data to the supplied output stream if not {@code null}, or discard
     * the data if the output stream is {@code null}.
     *
     * @param in        input stream to be read.
     * @param delimiter delimiter to break the read (apart from {@code EOL ('\n')} or {@code EOF}).
     * @param out       output stream to write the read data to. May be {@code null}, in which case the
     *                  read data are silently discarded.
     * @return value of the last byte read.
     * @throws IOException in case the reading or writing of the data failed.
     */
    private int readLineUntil(InputStream in, int delimiter, OutputStream out) throws IOException {
        int b;
        while ((b = in.read()) != -1) {
            if (b == delimiter || b == '\n') {
                break;
            } else if (out != null) {
                out.write(b);
            }
        }

        return b;
    }

    private void processField(InboundEvent.Builder inboundEventBuilder, String name, byte[] value) {
        if ("event".equals(name)) {
            inboundEventBuilder.name(new String(value));
        } else if ("data".equals(name)) {
            inboundEventBuilder.data(value);
            inboundEventBuilder.data(EOL_DATA);
        } else if ("id".equals(name)) {
            String s = new String(value);
            try {
                // TODO: check the value [0-9]*
                Integer.parseInt(new String(value));
            } catch (NumberFormatException nfe) {
                // TODO log warning
                s = "";
            }
            inboundEventBuilder.id(s);
        } else if ("retry".equals(name)) {
            // TODO SSE retry support
        } else {
            // TODO support extensions, ignore for now
        }
    }
}
