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

import org.glassfish.jersey.message.MessageBodyWorkers;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Base functionality for {@link EventProcessor} and {@link EventProcessorReader}.
 *
 * Processes incoming stream and parses {@link Event}s.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
/* package */ final class EventReceiver implements Closeable {

    private enum State {
        START,

        COMMENT,
        FIELD_NAME,
        FIELD_VALUE_FIRST,
        FIELD_VALUE,

        EVENT_FIRED
    }

    private final InputStream inputStream;
    private final Annotation[] annotations;
    private final MediaType mediaType;
    private final MultivaluedMap<String, String> headers;
    private final MessageBodyWorkers messageBodyWorkers;
    private boolean closed = false;

    private void processField(Event event, String name, byte[] value) {
        if(name.equals("event")) {
            event.setName(new String(value));
        } else if(name.equals("data")) {
            event.addData(value);
            event.addData(new byte[]{'\n'});
        } else if(name.equals("id")) {
            String s = new String(value);
            try {
                // TODO: check the value [0-9]*
                Integer.parseInt(new String(value));
            } catch (NumberFormatException nfe) {
                s = "";
            }
            event.setId(s);
        } else if(name.equals("retry")) {
            // TODO
        } else {
            // ignore
        }
    }

    /* package */ void process(@Nullable final LinkedBlockingQueue<Event> eventQueue, @Nullable final EventSource eventSource) {
        /**
         * http://dev.w3.org/html5/eventsource/
         * last editors draft from 13 March 2012
         */

        Event event = new Event(messageBodyWorkers, annotations, mediaType, headers);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        State currentState = State.START;
        String fieldName = null;

        try {
            int data = 0;
            while(currentState != State.EVENT_FIRED && (data = inputStream.read()) != -1) {

                switch (currentState) {

                    case START:
                        if(data == ':') {
                            currentState = State.COMMENT;
                        } else if(data != '\n') {
                            baos.write(data);
                            currentState = State.FIELD_NAME;
                        } else if(data == '\n') {
                            if(!event.isEmpty()) {
                                // fire!
                                if(eventQueue != null) {
                                    eventQueue.add(event);
                                }

                                if(eventSource != null) {
                                    eventSource.onReceivedEvent(event);
                                }

                                currentState = State.EVENT_FIRED;
                            }

                            event = new Event(messageBodyWorkers, annotations, mediaType, headers);
                        }
                        break;
                    case COMMENT:
                        if(data == '\n') {
                            currentState = State.START;
                        }
                        break;
                    case FIELD_NAME:
                        if(data == ':') {
                            fieldName = baos.toString();
                            baos.reset();
                            currentState = State.FIELD_VALUE_FIRST;
                        } else if(data == '\n') {
                            processField(event, baos.toString(), "".getBytes());
                            baos.reset();
                            currentState = State.START;
                        } else {
                            baos.write(data);
                        }
                        break;
                    case FIELD_VALUE_FIRST:
                        // first space has to be skipped
                        if(data != ' ') {
                            baos.write(data);
                        }

                        if(data == '\n') {
                            processField(event, fieldName, baos.toByteArray());
                            baos.reset();
                            currentState = State.START;
                            break;
                        }

                        currentState = State.FIELD_VALUE;
                        break;
                    case FIELD_VALUE:
                        if(data == '\n') {
                            processField(event, fieldName, baos.toByteArray());
                            baos.reset();
                            currentState = State.START;
                        } else {
                            baos.write(data);
                        }
                        break;
                }

            }

            if(data == -1) {
                closed = true;
            }
        } catch (IOException e) {
            closed = true;
        }

    }

    /**
     * Constructor.
     *
     * @param inputStream raw entity input stream
     * @param annotations to be passed to {@link Event} instance for use during {@link Event#getData(Class)} call
     * @param mediaType to be passed to {@link Event} instance for use during {@link Event#getData(Class)} call
     * @param headers to be passed to {@link Event} instance for use during {@link Event#getData(Class)} call
     * @param messageBodyWorkers to be passed to {@link Event} instance for use during {@link Event#getData(Class)} call
     */
    EventReceiver(InputStream inputStream, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> headers, MessageBodyWorkers messageBodyWorkers) {
        this.inputStream = inputStream;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.headers = headers;
        this.messageBodyWorkers = messageBodyWorkers;
    }

    /**
     * Get object state.
     *
     * @return true if no new {@link Event} can be received, false otherwise.
     */
    boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        inputStream.close();
    }

}
