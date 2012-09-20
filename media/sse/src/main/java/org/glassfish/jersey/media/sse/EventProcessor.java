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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Used for retrieving server-sent events.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public final class EventProcessor implements Closeable {

    private final InputStream inputStream;
    private final Annotation[] annotations;
    private final MediaType mediaType;
    private final MultivaluedMap<String, String> headers;
    private final MessageBodyWorkers messageBodyWorkers;

    private volatile boolean closed;

    /**
     * Package-private constructor used by the {@link EventProcessorReader}.
     *
     * @param inputStream response input stream.
     * @param annotations annotations associated with response entity.
     * @param mediaType response entity media type.
     * @param headers response headers.
     * @param messageBodyWorkers message body workers.
     */
    EventProcessor(InputStream inputStream,
                   Annotation[] annotations,
                   MediaType mediaType,
                   MultivaluedMap<String, String> headers,
                   MessageBodyWorkers messageBodyWorkers) {
        this.inputStream = inputStream;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.headers = headers;
        this.messageBodyWorkers = messageBodyWorkers;
    }

    /**
     * Starts synchronous message processing. This method blocks until the connection is closed.
     *
     * @param eventListener Event listener that will be synchronously receive {@link InboundEvent events}.
     */
    public void process(final EventListener eventListener) {

        do {
            if (closed) {
                try {
                    inputStream.close();
                    break;
                } catch (IOException e) {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, e.getMessage(), e);
                }
            } else {
                nextEvent(eventListener);
            }
        } while (true);
    }

    @Override
    public void close() {
        // this can be called from different threads, so just setting a flag to keep it thread safe
        // the actual close will be performed by process() method
        closed = true;
    }

    private enum State {
        START,

        COMMENT,
        FIELD_NAME,
        FIELD_VALUE_FIRST,
        FIELD_VALUE,

        EVENT_FIRED
    }

    private void nextEvent(final EventListener listener) {
        /**
         * http://dev.w3.org/html5/eventsource/
         * last editors draft from 13 March 2012
         */

        InboundEvent inboundEvent = new InboundEvent(messageBodyWorkers, annotations, mediaType, headers);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        State currentState = State.START;
        String fieldName = null;

        try {
            int data = 0;
            while (currentState != State.EVENT_FIRED && (data = inputStream.read()) != -1) {

                switch (currentState) {

                    case START:
                        if (data == ':') {
                            currentState = State.COMMENT;
                        } else if (data != '\n') {
                            baos.write(data);
                            currentState = State.FIELD_NAME;
                        } else if (data == '\n') {
                            if (!inboundEvent.isEmpty()) {
                                // fire!
                                listener.onEvent(inboundEvent);
                                currentState = State.EVENT_FIRED;
                            }

                            inboundEvent = new InboundEvent(messageBodyWorkers, annotations, mediaType, headers);
                        }
                        break;
                    case COMMENT:
                        if (data == '\n') {
                            currentState = State.START;
                        }
                        break;
                    case FIELD_NAME:
                        if (data == ':') {
                            fieldName = baos.toString();
                            baos.reset();
                            currentState = State.FIELD_VALUE_FIRST;
                        } else if (data == '\n') {
                            processField(inboundEvent, baos.toString(), "".getBytes());
                            baos.reset();
                            currentState = State.START;
                        } else {
                            baos.write(data);
                        }
                        break;
                    case FIELD_VALUE_FIRST:
                        // first space has to be skipped
                        if (data != ' ') {
                            baos.write(data);
                        }

                        if (data == '\n') {
                            processField(inboundEvent, fieldName, baos.toByteArray());
                            baos.reset();
                            currentState = State.START;
                            break;
                        }

                        currentState = State.FIELD_VALUE;
                        break;
                    case FIELD_VALUE:
                        if (data == '\n') {
                            processField(inboundEvent, fieldName, baos.toByteArray());
                            baos.reset();
                            currentState = State.START;
                        } else {
                            baos.write(data);
                        }
                        break;
                }

            }

            if (data == -1) {
                close();
            }
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, e.getMessage(), e);
            close();
        }
    }

    private void processField(InboundEvent inboundEvent, String name, byte[] value) {
        if (name.equals("event")) {
            inboundEvent.setName(new String(value));
        } else if (name.equals("data")) {
            inboundEvent.addData(value);
            inboundEvent.addData(new byte[]{'\n'});
        } else if (name.equals("id")) {
            String s = new String(value);
            try {
                // TODO: check the value [0-9]*
                Integer.parseInt(new String(value));
            } catch (NumberFormatException nfe) {
                s = "";
            }
            inboundEvent.setId(s);
        } else if (name.equals("retry")) {
            // TODO
        } else {
            // ignore
        }
    }
}
