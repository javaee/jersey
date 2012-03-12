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

import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.jvnet.hk2.annotations.Inject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * {@link MessageBodyWriter} for {@link EventChannel}.
 *
 * @see MessageBodyWriter
 * @see EventChannel
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Provider
public class EventChannelWriter implements MessageBodyWriter<EventChannel> {

    private static final class References {
        @Inject
        private Ref<MessageBodyWorkers> messageBodyWorkers;
    }

    private final Services services;

    public EventChannelWriter(@Inject Services services) {
        this.services = services;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return EventChannel.class.equals(type) && mediaType.equals(EventChannel.SERVER_SENT_EVENTS_TYPE);
    }

    @Override
    public long getSize(EventChannel eventChannel, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(EventChannel eventChannel, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
            throws IOException, WebApplicationException {

        final References references = services.forContract(Injector.class).get().inject(References.class);

        // TODO workaround - client side requires at least something to be sent
        entityStream.write(new byte[] {':', '\n'}); // sends blank comment
        entityStream.flush();

        do {
            EventChannel.Event event;
            try {
                event = eventChannel.getMessageToWrite();
            } catch (InterruptedException e) {
                event = null;
            }

            if(event != null && event.getType() != null) {
                final MessageBodyWorkers messageBodyWorkers = references.messageBodyWorkers.get();
                final MessageBodyWriter messageBodyWriter = messageBodyWorkers.getMessageBodyWriter(event.getType(),
                        null, annotations, MediaType.TEXT_PLAIN_TYPE /* TODO: mediaType */);
                if(event.getName() != null) {
                    entityStream.write(String.format("event: %s\n", event.getName()).getBytes());
                }
                if(event.getId() != null) {
                    entityStream.write(String.format("id: %s\n", event.getName()).getBytes());
                }

                try {
                    messageBodyWriter.writeTo(event.getData(), event.getClass(), null, annotations, mediaType, httpHeaders, new OutputStream() {

                        private boolean start = true;

                        @Override
                        public void write(int i) throws IOException {
                            if(start) {
                                entityStream.write("data: ".getBytes());
                                start = false;
                            }
                            entityStream.write(i);
                            if(i == '\n') {
                                entityStream.write("data: ".getBytes());
                            }
                        }
                    });
                } catch (IOException e) {
                    eventChannel.clientClose();
                    throw e;
                }
                entityStream.write("\n\n".getBytes());
                entityStream.flush();
            }
        } while (!eventChannel.isClosed());

    }
}
