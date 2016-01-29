/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageUtils;

/**
 * Client-side single inbound Server-Sent Event reader.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@ConstrainedTo(RuntimeType.CLIENT)
class InboundEventReader implements MessageBodyReader<InboundEvent> {

    private static final Logger LOGGER = Logger.getLogger(InboundEventReader.class.getName());
    private static final byte[] EOL_DATA = new byte[] {'\n'};

    @Inject
    private Provider<MessageBodyWorkers> messageBodyWorkers;

    private enum State {
        SKIPPING_PREPENDED_EMPTY_EVENTS,
        NEW_LINE,
        COMMENT,
        FIELD,
    }

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                              final MediaType mediaType) {
        return InboundEvent.class.equals(type) && SseFeature.SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
    }

    @Override
    public InboundEvent readFrom(final Class<InboundEvent> type,
                                 final Type genericType,
                                 final Annotation[] annotations,
                                 final MediaType mediaType,
                                 final MultivaluedMap<String, String> headers,
                                 final InputStream entityStream) throws IOException, WebApplicationException {
        /**
         * SSE Event parsing based on:
         *
         * http://dev.w3.org/html5/eventsource/
         * last editors draft from 13 March 2012
         */
        final ByteArrayOutputStream tokenData = new ByteArrayOutputStream();
        final String charsetName = MessageUtils.getCharset(mediaType).name();
        final InboundEvent.Builder eventBuilder =
                new InboundEvent.Builder(messageBodyWorkers.get(), annotations, mediaType, headers);

        int b = -1;
        State currentState = State.SKIPPING_PREPENDED_EMPTY_EVENTS;
        loop:
        do {
            switch (currentState) {
                /* There is a problem with the SSE event parsing, because Jersey uses ChunkedInput to separate events.
                   The problem is that ChunkedInput uses fixed character string as a separator, which is \r\n\r\n when it
                   parses SSE.
                   The problem is that SSE events are separated only by \r\n and \r\n also works as an end of a field inside
                   the event, so the fixed separator \r\n\r\n  only works if the server does not send empty events.
                   For example:

                    event: e1\r\n
                    data: d1\r\n
                    \r\n
                    \r\n
                    event: e2\r\n
                    data: d2\r\n
                    \r\n

                    is a stream of <e1> <empty event> <e2>
                    Unfortunately the ChunkedInput parser will parse it only into 2 events <e1> and <e2> and <e2> will have
                    \r\n (an empty event) prepended at the beginning. This is not fixable on the ChunkedInput parser level,
                    which is not SSE aware, so this InboundEventReader must be aware of this and skip any prepended empty events.
                    Also as a result Jersey will not deliver empty events to the user. */
                case SKIPPING_PREPENDED_EMPTY_EVENTS:
                case NEW_LINE:
                    if (b == '\r') {
                        // read next byte in case of CRLF delimiter
                        b = entityStream.read();
                        b = b == '\n' ? entityStream.read() : b;
                    } else {
                        b = entityStream.read();
                    }

                    if (b == '\n' || b == '\r' || b == -1) {
                        if (currentState == State.SKIPPING_PREPENDED_EMPTY_EVENTS) {
                            break;
                        }

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
                    b = readLineUntil(entityStream, '\n', tokenData);
                    final String commentLine = tokenData.toString(charsetName);
                    tokenData.reset();
                    eventBuilder.commentLine(commentLine.trim());
                    currentState = State.NEW_LINE;
                    break;
                case FIELD:
                    // read field name
                    b = readLineUntil(entityStream, ':', tokenData);
                    final String fieldName = tokenData.toString(charsetName);
                    tokenData.reset();

                    if (b == ':') {
                        do {
                            b = entityStream.read();
                        } while (b == ' ');

                        if (b != '\n' && b != '\r' && b != -1) {
                            tokenData.write(b);
                            b = readLineUntil(entityStream, '\n', tokenData);
                        }
                    }

                    processField(eventBuilder, fieldName, mediaType, tokenData.toByteArray());
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
     * @param delimiter delimiter to break the read (apart from {@code EOL ('\n', '\r')} or {@code EOF}).
     * @param out       output stream to write the read data to. May be {@code null}, in which case the
     *                  read data are silently discarded.
     * @return value of the last byte read.
     * @throws IOException in case the reading or writing of the data failed.
     */
    private int readLineUntil(final InputStream in, final int delimiter, final OutputStream out) throws IOException {
        int b;
        while ((b = in.read()) != -1) {
            if (b == delimiter || b == '\n' || b == '\r') {
                break;
            } else if (out != null) {
                out.write(b);
            }
        }

        return b;
    }

    private void processField(final InboundEvent.Builder inboundEventBuilder, final String name,
                              final MediaType mediaType, final byte[] value) {
        final String valueString = new String(value, MessageUtils.getCharset(mediaType));
        if ("event".equals(name)) {
            inboundEventBuilder.name(valueString);
        } else if ("data".equals(name)) {
            inboundEventBuilder.write(value);
            inboundEventBuilder.write(EOL_DATA);
        } else if ("id".equals(name)) {
            inboundEventBuilder.id(valueString);
        } else if ("retry".equals(name)) {
            try {
                inboundEventBuilder.reconnectDelay(Long.parseLong(valueString));
            } catch (final NumberFormatException ex) {
                LOGGER.log(Level.FINE, LocalizationMessages.IN_EVENT_RETRY_PARSE_ERROR(valueString), ex);
            }
        } else {
            LOGGER.fine(LocalizationMessages.IN_EVENT_FIELD_NOT_RECOGNIZED(name, valueString));
        }
    }
}
