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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;

import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

/**
 * Writer for {@link OutboundEvent}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class OutboundEventWriter implements MessageBodyWriter<OutboundEvent> {

    private static final class References {
        @Inject
        private Ref<MessageBodyWorkers> messageBodyWorkers;
    }

    @Inject
    private Services services;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return (type.equals(OutboundEvent.class));
    }

    @Override
    public long getSize(OutboundEvent incomingEvent, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeTo(OutboundEvent outboundEvent, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
        final References references = services.forContract(Injector.class).get().inject(References.class);

        if(outboundEvent.getComment() != null) {
            entityStream.write(String.format(": %s\n", outboundEvent.getComment()).getBytes());
        }

        if(outboundEvent.getType() != null) {
            final MessageBodyWorkers messageBodyWorkers = references.messageBodyWorkers.get();
            final MessageBodyWriter messageBodyWriter = messageBodyWorkers.getMessageBodyWriter(outboundEvent.getType(),
                    null, annotations, (outboundEvent.getMediaType() == null ? MediaType.TEXT_PLAIN_TYPE : outboundEvent.getMediaType()));
            if(outboundEvent.getName() != null) {
                entityStream.write(String.format("event: %s\n", outboundEvent.getName()).getBytes());
            }
            if(outboundEvent.getId() != null) {
                entityStream.write(String.format("id: %s\n", outboundEvent.getName()).getBytes());
            }

            messageBodyWriter.writeTo(outboundEvent.getData(), outboundEvent.getClass(), null, annotations, mediaType, httpHeaders, new OutputStream() {

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
        }

        entityStream.write("\n\n".getBytes());
        entityStream.flush();
    }
}
