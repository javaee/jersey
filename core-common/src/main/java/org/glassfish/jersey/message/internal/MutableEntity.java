/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MessageProcessingException;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.transform.Source;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.collection.InstanceTypePair;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.jvnet.hk2.annotations.Inject;

/**
 * Mutable message entity implementation class.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
class MutableEntity implements Entity, Entity.Builder<MutableEntity> {

    private static final Logger LOGGER = Logger.getLogger(MutableEntity.class.getName());
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    //
    private static final InputStream EMPTY = new InputStream() {

        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    public static MutableEntity empty(AbstractMutableMessage<?> message) {
        return new MutableEntity(message, EMPTY);
    }
    // stream representation of contentStream
    private InputStream contentStream;
    private boolean isContentStreamBuffered;
    // java object representation of contentStream
    private InstanceTypePair<?> instanceType;
    // reference to enclosing message
    private AbstractMutableMessage<?> message;
    // writer annotations
    private Annotation[] writeAnnotations = EMPTY_ANNOTATIONS;
    // message body workers to read and write entities
    @Inject
    protected MessageBodyWorkers workers;

    public MutableEntity(AbstractMutableMessage<?> message, InputStream content) {
        this.message = message;
        this.contentStream = content;
    }

    public MutableEntity(AbstractMutableMessage<?> message, final InstanceTypePair<?> instanceType) {
        this.message = message;
        this.instanceType = instanceType;
    }

    public MutableEntity(AbstractMutableMessage<?> message, final MutableEntity that) {
        this.message = message;
        this.instanceType = that.instanceType;
        this.workers = that.workers; // TODO should we copy workers?
    }

    @Override
    public boolean isEmpty() {
        if (contentStream == null) {
            return instanceType == null;
        } else {
            try {
                if (contentStream.available() > 0) {
                    return false;
                } else if (contentStream.markSupported()) {
                    contentStream.mark(1);
                    int i = contentStream.read();
                    contentStream.reset();
                    return i == -1;
                } else {
                    return true;
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public MutableEntity writeAnnotations(Annotation[] annotations) {
        this.writeAnnotations = Arrays.copyOf(annotations, annotations.length);
        return this;
    }

    @Override
    public Object content() {
        return content(Object.class);
        // TODO the following might be more suitable:
//        return (instanceType == null) ? contentStream : content(Object.class);
    }

    @Override
    public <T> T content(Class<T> rawType) {
        final Type genericSuperclass = rawType.getGenericSuperclass();
        return content(rawType, (genericSuperclass instanceof ParameterizedType) ? genericSuperclass : rawType, EMPTY_ANNOTATIONS);
    }

    @Override
    public <T> T content(GenericType<T> type) {
        return content(type.getRawType(), type.getType(), EMPTY_ANNOTATIONS);
    }

    @Override
    public <T> T content(Class<T> rawType, Annotation[] annotations) {
        final Type genericSuperclass = rawType.getGenericSuperclass();
        return content(rawType, (genericSuperclass instanceof ParameterizedType) ? genericSuperclass : rawType, annotations);
    }

    @Override
    public <T> T content(GenericType<T> type, Annotation[] annotations) {
        return content(type.getRawType(), type.getType(), annotations);
    }

    @SuppressWarnings("unchecked")
    private <T> T content(Class<T> rawType, Type type, Annotation[] readAnnotations) {
        if (isEmpty()) {
            return null;
        }

        final boolean typeIsAssignableFromMyInstance =
                (instanceType != null) && (rawType.isAssignableFrom(instanceType.instance().getClass()));

        if (typeIsAssignableFromMyInstance) {
            return rawType.cast(instanceType.instance());
        }

        if (instanceType != null && !isContentStreamBuffered) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (bufferEntityInstance(baos)) {
                return null;
            } else {
                contentStream = new ByteArrayInputStream(baos.toByteArray());
            }
        }

        // TODO wire up workers
        if (workers == null) {
            return null;
        }

        final MediaType mediaType = getMsgContentType();

        final MessageBodyReader<T> br = workers.getMessageBodyReader(rawType, type, readAnnotations, mediaType);
        if (br == null) {
            throw new MessageBodyProviderNotFoundException(LocalizationMessages.ERROR_NOTFOUND_MESSAGEBODYREADER(mediaType,
                    rawType));
        }

        try {
            T t = br.readFrom(rawType, type, readAnnotations, mediaType, message.toJaxrsHeaderMap(), contentStream);
            if (br instanceof CompletableReader) {
                t = ((CompletableReader<T>) br).complete(t);
            }

            if (isContentStreamBuffered) {
                contentStream.reset();
            } else if (!(t instanceof Closeable)  && !(t instanceof Source)) {
                invalidateContentStream();
            }

            instanceType = InstanceTypePair.of(t);
            return t;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error reading entity from input stream", ex);
        }

        return null;
    }

    private MediaType getMsgContentType() {
        if (message == null) {
            return null;
        }
        final String result = message.header(HttpHeaders.CONTENT_TYPE);
        if (result == null || result.isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        return MediaType.valueOf(result);
    }

    @Override
    public Type type() {
        if (instanceType != null) {
            return instanceType.type();
        }

        // TODO try to read the stream
        return null;
    }

    @Override
    public MutableEntity content(Object content) {
        if (content instanceof InputStream) {
            this.instanceType = null;
            this.contentStream = InputStream.class.cast(content);
            return this;
        }

        invalidateContentStream();
        if (content != null) {
            this.instanceType = InstanceTypePair.of(content);
        } else {
            instanceType = null;
        }
        return this;
    }

    @Override
    public MutableEntity content(Object content, Type type) {
        invalidateContentStream();
        if (content != null) {
            this.instanceType = InstanceTypePair.of(content, type);
        } else {
            instanceType = null;
        }
        return this;
    }

    @Override
    public <T> MutableEntity content(Object content, GenericType<T> type) {
        invalidateContentStream();
        if (content != null) {
            this.instanceType = InstanceTypePair.of(content, type.getType());
        } else {
            instanceType = null;
        }
        return this;
    }

    @Override
    public void bufferEntity() throws MessageProcessingException {
        try {
            if (((contentStream == null || contentStream.available() <= 0) && instanceType == null) || isContentStreamBuffered) {
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (contentStream != null) {
                try {
                    ReaderWriter.writeTo(contentStream, baos);
                } finally {
                    invalidateContentStream();
                }
            } else {
                // instanceType != null && contentStream == null

                if (bufferEntityInstance(baos)) {
                    return;
                }
            }

            contentStream = new ByteArrayInputStream(baos.toByteArray());
            isContentStreamBuffered = true;
        } catch (IOException ex) {
            throw new MessageProcessingException(LocalizationMessages.MESSAGE_CONTENT_BUFFERING_FAILED(), ex);
        }
    }

    private boolean bufferEntityInstance(ByteArrayOutputStream baos) {
        final Type myInstanceType = instanceType.type();
        final Object myInstance = instanceType.instance();
        if (myInstanceType == null || workers == null || myInstance == null) {
            return true;
        }

        final MediaType mediaType = getMsgContentType();

        final MessageBodyWriter writer = workers.getMessageBodyWriter(myInstance.getClass(), myInstanceType, writeAnnotations, mediaType);
        if (writer == null) {
            // TODO throw an exception?
            return true;
        } else {
            try {
                writer.writeTo(instanceType.instance(), myInstance.getClass(), myInstanceType, writeAnnotations, mediaType, null, baos);
                baos.close();
            } catch (IOException ex) {
                Logger.getLogger(MutableEntity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (WebApplicationException ex) {
                Logger.getLogger(MutableEntity.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    @Override
    public boolean isEntityRetrievable() {
        return !isEmpty() && type() != null;
    }

    public MutableEntity workers(MessageBodyWorkers workers) {
        this.workers = workers;
        return this;
    }

    public MessageBodyWorkers workers() {
        return workers;
    }

    private void invalidateContentStream() {
        if (contentStream != null) {
            try {
                contentStream.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, LocalizationMessages.MESSAGE_CONTENT_INPUT_STREAM_CLOSE_FAILED(), ex);
            }
            this.contentStream = null;
        }
    }
}
