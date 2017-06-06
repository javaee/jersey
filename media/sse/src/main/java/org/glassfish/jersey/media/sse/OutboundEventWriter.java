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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageUtils;

/**
 * Writer for {@link OutboundSseEvent}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class OutboundEventWriter implements MessageBodyWriter<OutboundSseEvent> {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    // encoding does not matter (lower ASCII characters)
    private static final byte[] COMMENT_LEAD = ": ".getBytes(UTF8);
    private static final byte[] NAME_LEAD = "event: ".getBytes(UTF8);
    private static final byte[] ID_LEAD = "id: ".getBytes(UTF8);
    private static final byte[] RETRY_LEAD = "retry: ".getBytes(UTF8);
    private static final byte[] DATA_LEAD = "data: ".getBytes(UTF8);
    private static final byte[] EOL = {'\n'};

    @Inject
    private Provider<MessageBodyWorkers> workersProvider;

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return OutboundSseEvent.class.isAssignableFrom(type) && SseFeature.SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
    }

    @Override
    public long getSize(final OutboundSseEvent incomingEvent,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeTo(final OutboundSseEvent outboundEvent,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {

        final Charset charset = MessageUtils.getCharset(mediaType);
        if (outboundEvent.getComment() != null) {
            for (final String comment : outboundEvent.getComment().split("\n")) {
                entityStream.write(COMMENT_LEAD);
                entityStream.write(comment.getBytes(charset));
                entityStream.write(EOL);
            }
        }

        if (outboundEvent.getType() != null) {
            if (outboundEvent.getName() != null) {
                entityStream.write(NAME_LEAD);
                entityStream.write(outboundEvent.getName().getBytes(charset));
                entityStream.write(EOL);
            }
            if (outboundEvent.getId() != null) {
                entityStream.write(ID_LEAD);
                entityStream.write(outboundEvent.getId().getBytes(charset));
                entityStream.write(EOL);
            }
            if (outboundEvent.getReconnectDelay() > SseFeature.RECONNECT_NOT_SET) {
                entityStream.write(RETRY_LEAD);
                entityStream.write(Long.toString(outboundEvent.getReconnectDelay()).getBytes(charset));
                entityStream.write(EOL);
            }

            final MediaType eventMediaType =
                    outboundEvent.getMediaType() == null ? MediaType.TEXT_PLAIN_TYPE : outboundEvent.getMediaType();
            final MessageBodyWriter messageBodyWriter = workersProvider.get().getMessageBodyWriter(outboundEvent.getType(),
                    outboundEvent.getGenericType(), annotations, eventMediaType);
            messageBodyWriter.writeTo(
                    outboundEvent.getData(),
                    outboundEvent.getType(),
                    outboundEvent.getGenericType(),
                    annotations,
                    eventMediaType,
                    httpHeaders,
                    new OutputStream() {

                        private boolean start = true;

                        @Override
                        public void write(final int i) throws IOException {
                            if (start) {
                                entityStream.write(DATA_LEAD);
                                start = false;
                            }
                            entityStream.write(i);
                            if (i == '\n') {
                                entityStream.write(DATA_LEAD);
                            }
                        }
                    });
            entityStream.write(EOL);
        }
    }
}
