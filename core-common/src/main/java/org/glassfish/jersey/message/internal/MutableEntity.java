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
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.xml.transform.Source;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
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

    /**
     * Input stream and its state. State is represented by the {@link Type Type enum} and
     * is used to control the execution of interceptors.
     *
     */
    private static class ContentStream {
        private InputStream contentStream;
        private Type type;

        public ContentStream(InputStream contentStream) {
            super();
            this.contentStream = contentStream;
            this.type = Type.INTERNAL;
        }

        public void setBufferedContentStream(InputStream bufferedInputStream) {
            this.contentStream = bufferedInputStream;
            this.type = (this.type == Type.EXTERNAL ? Type.EXTERNAL_BUFFERED : Type.BUFFERED);
        }

        public void setBufferedTempContentStream(InputStream inputStream) {
            this.contentStream = inputStream;
            this.type = Type.TEMP_BUFFERED;
        }

        public void setNewContentStream(InputStream contentStream) {
            this.contentStream = contentStream;
            this.type = Type.INTERNAL;
        }

        public void setExternalContentStream(InputStream contentStream) {
            this.contentStream = contentStream;
            this.type = Type.EXTERNAL;
        }

        public InputStream getInputStream() {
            return contentStream;
        }

        public Type getType() {
            return type;
        }

        public void invalidateContentStream() {
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, LocalizationMessages.MESSAGE_CONTENT_INPUT_STREAM_CLOSE_FAILED(), ex);
                }
                this.contentStream = null;
            }
        }

        public boolean isEmpty() {
            if (contentStream == null) {
                return true;
            }
            try {
                if (contentStream.available() > 0) {
                    return false;
                } else if (contentStream.markSupported()) {
                    contentStream.mark(1);
                    int i = contentStream.read();
                    contentStream.reset();
                    return i == -1;
                } else {
                    int b = contentStream.read();
                    if (b == -1) {
                        return true;
                    }

                    PushbackInputStream pbis;
                    if (contentStream instanceof PushbackInputStream) {
                        pbis = (PushbackInputStream) contentStream;
                    } else {
                        pbis = new PushbackInputStream(contentStream, 1);
                        contentStream = pbis;
                    }
                    pbis.unread(b);

                    return false;
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * State of the input stream.
         *
         * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
         *
         */
        public enum Type {
            /**
             * External input stream "from wire" which MUST be intercepted before reading
             * it by MBR.
             */
            EXTERNAL(true),
            /**
             * Internal input stream which MUST NOT be intercepted before reading by MBR.
             */
            INTERNAL(false),
            /**
             * Buffered input stream which MUST NOT be intercepted before reading by MBR.
             */
            BUFFERED(false),
            /**
             * Buffered input stream which MUST be intercepted before reading by MBR.
             */
            EXTERNAL_BUFFERED(true),
            /**
             * Temporary buffered input stream which MUST NOT be intercepted before
             * reading by MBR. Temporary buffered input stream is created by buffering the
             * already read entity in order to read it again as different type.
             */
            TEMP_BUFFERED(false);

            private final boolean intercept;

            Type(boolean intercept) {
                this.intercept = intercept;
            }

            public boolean intercept() {
                return intercept;
            }

        }

    }

    public static MutableEntity empty(AbstractMutableMessage<?> message) {
        return new MutableEntity(message, EMPTY);
    }

    // contains entity and its type
    private GenericEntity<?> genericEntity;
    // indicates if the type of the entity was set explicitly
    private boolean forceType;
    // writer annotations
    private Annotation[] writeAnnotations = EMPTY_ANNOTATIONS;
    // message body workers to read and write entities
    @Inject
    protected MessageBodyWorkers workers;
    // Entity/Content stream
    private ContentStream contentStream;
    // reference to enclosing message
    private AbstractMutableMessage<?> message;

    /**
     * Creates new instance initialized with mutable message and input stream.
     * @param message {@link AbstractMutableMessage} to which this entity belongs to.
     * @param content {@link InputStream} from which the entity could be read.
     */
    public MutableEntity(AbstractMutableMessage<?> message, InputStream content) {
        this.message = message;
        this.contentStream = new ContentStream(content);
    }

    /**
     * Creates new instance created from {@link MutableEntity other MutableEntity instance} initialized with mutable message.
     * @param message {@link AbstractMutableMessage} to which this entity belongs to.
     * @param that {@link MutableEntity other MutableEntity instance} from which this instance will be initialized.
     */
    public MutableEntity(AbstractMutableMessage<?> message, final MutableEntity that) {
        this.message = message;
        this.genericEntity = that.genericEntity;
        this.forceType = that.forceType;
        this.workers = that.workers; // TODO should we copy workers?
    }

    @Override
    public boolean isEmpty() {
        if (contentStream == null || contentStream.getInputStream() == null) {
            return genericEntity == null;
        } else {
            return contentStream.isEmpty();
        }
    }

    @Override
    public MutableEntity writeAnnotations(Annotation[] annotations) {
        this.writeAnnotations = Arrays.copyOf(annotations, annotations.length);
        return this;
    }

    @Override
    public Object content() {
        if (genericEntity == null) {
            // TODO: might be better to return the content stream
            return null;
        }
        return forceType ? genericEntity : genericEntity.getEntity();
    }

    @Override
    public <T> T content(Class<T> rawType) {
        return content(rawType, extractType(rawType), EMPTY_ANNOTATIONS);
    }

    @Override
    public <T> T content(Class<T> rawType, Type genericType) {
        return (T) content(rawType, genericType, EMPTY_ANNOTATIONS);
    }

    @Override
    public <T> T content(Class<T> rawType, Annotation[] annotations) {
        return content(rawType, extractType(rawType), annotations);
    }

    @SuppressWarnings("unchecked")
    public <T> T content(Class<T> rawType, Type type, Annotation[] readAnnotations) {
        if (isEmpty()) {
            return null;
        }

        final boolean typeIsAssignableFromMyInstance = (genericEntity != null)
                && (rawType.isAssignableFrom(genericEntity.getRawType()));

        if (typeIsAssignableFromMyInstance) {
            return rawType.cast(genericEntity.getEntity());
        }

        if (genericEntity != null && contentStream.getType() != ContentStream.Type.BUFFERED
                && contentStream.getType() != ContentStream.Type.EXTERNAL_BUFFERED) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (bufferEntityInstance(baos)) {
                return null;
            } else {
                contentStream.setBufferedTempContentStream(new ByteArrayInputStream(baos.toByteArray()));
            }
        }

        // TODO wire up workers
        if (workers == null) {
            return null;
        }

        final MediaType mediaType = getMsgContentType();

        try {
            T t = (T) workers.readFrom(
                    rawType,
                    type,
                    readAnnotations,
                    mediaType,
                    message.headers(),
                    new MapPropertiesDelegate(message.properties()),
                    contentStream.getInputStream(),
                    contentStream.getType().intercept());

            if (contentStream.getType() == ContentStream.Type.BUFFERED
                    || contentStream.getType() == ContentStream.Type.EXTERNAL_BUFFERED) {
                contentStream.getInputStream().reset();
            } else if (!(t instanceof Closeable) && !(t instanceof Source)) {
                contentStream.invalidateContentStream();
            }

            genericEntity = new GenericEntity(t, type);
            forceType = true;
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
        if (genericEntity != null) {
            return genericEntity.getType();
        }

        // TODO try to read the stream
        return null;
    }

    @Override
    public MutableEntity content(Object content) {
        contentStream.invalidateContentStream();

        if (content != null) {
            if (content instanceof GenericEntity) {
                forceType = true;
                this.genericEntity = (GenericEntity<?>) content;
            } else {
                forceType = false;
                this.genericEntity = new GenericEntity(content, extractType(content));
                if (content instanceof InputStream) {
                    contentStream.setNewContentStream(InputStream.class.cast(content));
                }
            }
        } else {
            forceType = false;
            genericEntity = null;
        }

        return this;
    }

    public void rawEntityStream(InputStream inputStream) {
        contentStream.setExternalContentStream(inputStream);
        genericEntity = null;
        forceType = false;
    }

    @Override
    public <T> MutableEntity content(T content, Type type) {
        contentStream.invalidateContentStream();
        if (content != null) {
            forceType = true;
            this.genericEntity = new GenericEntity(content, type);
        } else {
            forceType = false;
            this.genericEntity = null;
        }
        return this;
    }

    @Override
    public <T> MutableEntity content(T content, GenericType<T> type) {
        contentStream.invalidateContentStream();
        if (content != null) {
            forceType = true;
            this.genericEntity = new GenericEntity(content, type.getType());
        } else {
            forceType = false;
            this.genericEntity = null;
        }
        return this;
    }

    @Override
    public void bufferEntity() throws MessageProcessingException {
        try {
            if (contentStream.getType() == ContentStream.Type.BUFFERED
                    || contentStream.getType() == ContentStream.Type.EXTERNAL_BUFFERED) {
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (contentStream.getInputStream() != null) {
                try {
                    ReaderWriter.writeTo(contentStream.getInputStream(), baos);
                } finally {
                    contentStream.invalidateContentStream();
                }
            } else {

                if (bufferEntityInstance(baos)) {
                    return;
                }
            }

            contentStream.setBufferedContentStream(new ByteArrayInputStream(baos.toByteArray()));
        } catch (IOException ex) {
            throw new MessageProcessingException(LocalizationMessages.MESSAGE_CONTENT_BUFFERING_FAILED(), ex);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean bufferEntityInstance(ByteArrayOutputStream baos) {
        final Type myInstanceType = genericEntity.getType();
        final Object myInstance = genericEntity.getEntity();
        if (myInstanceType == null || workers == null || myInstance == null) {
            return true;
        }

        final MediaType mediaType = getMsgContentType();
        try {
            workers.writeTo(myInstance, genericEntity.getRawType(), genericEntity.getType(), writeAnnotations,
                    mediaType, (MultivaluedMap) message.headers(), new MapPropertiesDelegate(message.properties()), baos, null, false);
            baos.close();
        } catch (IOException ex) {
            Logger.getLogger(MutableEntity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (WebApplicationException ex) {
            Logger.getLogger(MutableEntity.class.getName()).log(Level.SEVERE, null, ex);
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

    private Type extractType(Object fromObject) {
        final Class<? extends Object> rawType = fromObject.getClass();
        return extractType(rawType);
    }

    private Type extractType(final Class<? extends Object> fromRawType) {
        final Type genericSuperclass = fromRawType.getGenericSuperclass();
        Type type = (genericSuperclass instanceof ParameterizedType) ? genericSuperclass : fromRawType;
        return type;
    }
}
