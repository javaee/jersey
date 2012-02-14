/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * A request writer for writing header values and a request entity.
 *
 * @author Paul Sandoz
 */
public class RequestWriter {

    private static final Logger LOGGER = Logger.getLogger(RequestWriter.class.getName());
    protected static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    public RequestWriter() {
    }

    /**
     * A lister for listening to events when writing a request entity.
     */
    // TODO how to register/obtain the listenerin a generic way?
    protected interface RequestEntityWriterListener {

        /**
         * Called when the size of the request entity is obtained.
         * <p>
         * Enables the appropriate setting of HTTP headers
         * for the size of the request entity and/or configure an appropriate
         * transport encoding.
         *
         * @param size the size, in bytes, of the request entity, otherwise -1
         *        if the size cannot be determined before serialization.
         * @throws java.io.IOException
         */
        void onRequestEntitySize(long size) throws IOException;

        /**
         * Called when the output stream is required to write the request
         * entity.
         *
         * @return the output stream to write the request entity.
         * @throws java.io.IOException
         */
        OutputStream onGetOutputStream() throws IOException;
    }

    /**
     * A writer for writing a request entity.
     */
    // TODO how to register/obtain the writer in a generic way?
    protected interface RequestEntityWriter {

        /**
         *
         * @return size the size, in bytes, of the request entity, otherwise -1
         *         if the size cannot be determined before serialization.
         */
        long getSize();

        /**
         *
         * @return the media type of the request entity.
         */
        MediaType getMediaType();

        /**
         * Write the request entity.
         *
         * @param out the output stream to write the request entity.
         * @throws java.io.IOException
         */
        void writeRequestEntity(OutputStream out) throws IOException;
    }

    /**
     * Default {@link RequestEntityWriter} implementation.
     */
    private final class RequestEntityWriterImpl implements RequestEntityWriter {

        private final Request request;
        private final Object entity;
        private final Type entityType;
        private MediaType mediaType;
        private final long size;
        private final MessageBodyWriter writer;

        /**
         *
         * @param request
         */
        @SuppressWarnings("unchecked")
        public RequestEntityWriterImpl(Request request) {
            this.request = request;

            final Object e = request.getEntity();
            if (e == null) {
                throw new IllegalArgumentException("The entity of the client request is null");
            }

            if (e instanceof GenericEntity) {
                final GenericEntity ge = (GenericEntity) e;
                this.entity = ge.getEntity();
                this.entityType = ge.getType();
            } else {
                this.entity = e;
                final Type genericSuperclass = entity.getClass().getGenericSuperclass();
                this.entityType = (genericSuperclass instanceof ParameterizedType) ? genericSuperclass : entity.getClass();
            }
            final Class<?> entityClass = entity.getClass();

            request = RequestWriter.this.ensureMediaType(entityClass, entityType, request);
            this.mediaType = request.getHeaders().getMediaType();

            final MessageBodyWorkers workers = Requests.getMessageWorkers(request);
            this.writer = workers.getMessageBodyWriter(entityClass, entityType, EMPTY_ANNOTATIONS, mediaType);
            if (writer == null) {
                String message = "A message body writer for Java class " + entityClass.getName()
                        + ", and Java type " + entityType
                        + ", and MIME media type " + mediaType + " was not found";
                LOGGER.severe(message);
                Map<MediaType, List<MessageBodyWriter>> m = workers.getWriters(mediaType);
                LOGGER.log(Level.SEVERE,
                        "The registered message body writers compatible with the MIME media type are:\n{0}",
                        workers.writersToString(m));

                throw new ClientException(message);
            }

            final MultivaluedMap<String, String> headers = request.getHeaders().asMap();
            this.size = headers.containsKey(HttpHeaders.CONTENT_ENCODING)
                    ? -1
                    : writer.getSize(entity, entityClass, entityType, EMPTY_ANNOTATIONS, mediaType);
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public MediaType getMediaType() {
            return mediaType;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void writeRequestEntity(OutputStream out) throws IOException {
            // TODO handlers?
            try {
                writer.writeTo(entity, entity.getClass(), entityType,
                        EMPTY_ANNOTATIONS, mediaType, request.getHeaders().asMap(),
                        out);
                out.flush();
            } finally {
                out.close();
            }
        }
    }

    /**
     * Get a request entity writer capable of writing the request entity.
     *
     * @param request the client request.
     * @return the request entity writer.
     */
    protected RequestEntityWriter getRequestEntityWriter(final Request request) {
        return new RequestEntityWriterImpl(request);
    }

    /**
     * Write a request entity using an appropriate message body writer.
     * <p>
     * The method {@link RequestEntityWriterListener#onRequestEntitySize(long) } will be invoked
     * with the size of the request entity to be serialized.
     * The method {@link RequestEntityWriterListener#onGetOutputStream() } will be invoked
     * when the output stream is required to write the request entity.
     *
     * @param request the client request containing the request entity. If the
     *        request entity is null then the method will not write any entity.
     * @param listener the request entity listener.
     * @throws java.io.IOException
     */
    @SuppressWarnings("unchecked")
    protected void writeRequestEntity(Request request, RequestEntityWriterListener listener) throws IOException {
        Object entity = request.getEntity();
        if (entity == null) {
            return;
        }

        Type entityType;
        if (entity instanceof GenericEntity) {
            final GenericEntity ge = (GenericEntity) entity;
            entity = ge.getEntity();
            entityType = ge.getType();
        } else {
            final Type genericSuperclass = entity.getClass().getGenericSuperclass();
            entityType = (genericSuperclass instanceof ParameterizedType) ? genericSuperclass : entity.getClass();
        }
        final Class entityClass = entity.getClass();


        request = ensureMediaType(entityClass, entityType, request);
        final MediaType mediaType = request.getHeaders().getMediaType();

        final MessageBodyWriter writer = Requests.getMessageWorkers(request)
                .getMessageBodyWriter(entityClass, entityType, EMPTY_ANNOTATIONS, mediaType);
        if (writer == null) {
            throw new ClientException(
                    "A message body writer for Java type, " + entity.getClass()
                    + ", and MIME media type, " + mediaType + ", was not found");
        }

        final MultivaluedMap<String, String> headers = request.getHeaders().asMap();
        final long size = headers.containsKey(HttpHeaders.CONTENT_ENCODING)
                ? -1
                : writer.getSize(entity, entityClass, entityType, EMPTY_ANNOTATIONS, mediaType);
        listener.onRequestEntitySize(size);

        // TODO handlers?
        final OutputStream out = listener.onGetOutputStream();
        try {
            writer.writeTo(entity, entityClass, entityType, EMPTY_ANNOTATIONS, mediaType, headers, out);
            out.flush();
        } catch (IOException ex) {
            try {
                out.close();
            } catch (Exception e) {
            }
            throw ex;
        } catch (RuntimeException ex) {
            try {
                out.close();
            } catch (Exception e) {
            }
            throw ex;
        }

        out.close();
    }

    private Request ensureMediaType(Class<?> entityClass, Type entityType, Request request) {
        if (request.getHeaders().getMediaType() != null) {
            return request;
        } else {
            // Content-Type is not present choose a default type
            final List<MediaType> mediaTypes = Requests.getMessageWorkers(request)
                    .getMessageBodyWriterMediaTypes(entityClass, entityType, EMPTY_ANNOTATIONS);

            final MediaType mediaType = getMediaType(mediaTypes);
            return Requests.from(request).type(mediaType).build();
        }
    }

    private MediaType getMediaType(List<MediaType> mediaTypes) {
        if (mediaTypes.isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        } else {
            MediaType mediaType = mediaTypes.get(0);
            if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            }
            return mediaType;
        }
    }
}