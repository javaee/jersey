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
package org.glassfish.jersey.message.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageBodyWorkers.MessageBodySizeCallback;
import org.glassfish.jersey.process.internal.PriorityComparator;
import org.glassfish.jersey.process.internal.PriorityComparator.Order;

/**
 * Entry point of the writer interceptor chain. It constructs the chain of wrapped
 * interceptor and invokes it. At the end of the chain the {@link MessageBodyWriter}
 * is invoked which writes the entity to the output stream. The
 * {@link ExceptionWrapperInterceptor} is always invoked on the client as a first
 * interceptor.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
@SuppressWarnings("rawtypes")
public class WriterInterceptorExecutor extends InterceptorExecutor implements WriterInterceptorContext {
    private Iterator<WriterInterceptor> iterator;

    private OutputStream outputStream;
    private final MultivaluedMap<String, Object> headers;
    private Object entity;

    /**
     * Reads a type from the {@link java.io.InputStream entityStream} using interceptors.
     *
     * @param entity entity object to be processed.
     * @param genericType the generic type that is to be read from the input stream.
     * @param annotations an array of the annotations on the declaration of the artifact
     *            that will be initialized with the produced instance. E.g. if the message
     *            body is to be converted into a method parameter, this will be the
     *            annotations on that parameter returned by
     *            {@code Method.getParameterAnnotations}.
     * @param mediaType the media type of the HTTP entity.
     * @param headers the mutable HTTP headers associated with HTTP entity.
     * @param propertiesDelegate a request-scoped properties depegate.
     * @param entityStream the {@link java.io.InputStream} of the HTTP entity. The stream is not
     *            closed after reading the entity.
     * @param workers {@link MessageBodyWorkers Message body workers}.
     * @param sizeCallback {@link MessageBodySizeCallback} instance. Can be null.
     * @param intercept true if the user interceptors should be executed. Otherwise only
     *            {@link ExceptionWrapperInterceptor exception wrapping interceptor} will
     *            be executed in the client.
     * @param writeEntity true if the entity should be written. Otherwise only headers will
     *            be written to underlying {@link OutputStream}.
     */
    public WriterInterceptorExecutor(Object entity, GenericType genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> headers, PropertiesDelegate propertiesDelegate, OutputStream entityStream,
            MessageBodyWorkers workers, MessageBodySizeCallback sizeCallback, boolean intercept, boolean writeEntity) {

        super(genericType, annotations, mediaType, propertiesDelegate);
        this.entity = entity;
        this.headers = headers;
        this.outputStream = entityStream;

        List<WriterInterceptor> interceptors = new ArrayList<WriterInterceptor>();
        for (WriterInterceptor interceptor : workers.getWriterInterceptors()) {
            if (intercept || (interceptor instanceof ExceptionWrapperInterceptor)) {
                interceptors.add(interceptor);
            }
        }
        Collections.sort(interceptors, new PriorityComparator<WriterInterceptor>(Order.ASCENDING));

        interceptors.add(new TerminalWriterInterceptor(workers, sizeCallback, writeEntity));

        this.iterator = interceptors.iterator();
    }

    /**
     * Returns next {@link WriterInterceptor interceptor} in the chain. Stateful method.
     *
     * @return Next interceptor.
     */
    public WriterInterceptor getNextInterceptor() {
        if (!iterator.hasNext()) {
            return null;
        }
        return iterator.next();
    }

    /**
     * Starts the interceptor chain execution.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void proceed() throws IOException {
        WriterInterceptor nextInterceptor = getNextInterceptor();
        if (nextInterceptor == null) {
            throw new ProcessingException(LocalizationMessages.ERROR_INTERCEPTOR_WRITER_PROCEED());
        }
        nextInterceptor.aroundWriteTo(this);
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void setEntity(Object entity) {
        this.entity = entity;
    }

    @Override
    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    @Override
    public void setOutputStream(OutputStream os) {
        this.outputStream = os;

    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return this.headers;
    }

    /**
     * Terminal writer interceptor which choose the appropriate {@link MessageBodyWriter}
     * and writes the entity to the output stream. The order of actions is the following: <br>
     * 1. choose the appropriate {@link MessageBodyWriter} <br>
     * 2. if callback is defined then it retrieves size and passes it to the callback <br>
     * 3. writes the entity to the output stream <br>
     *
     */
    private static class TerminalWriterInterceptor implements WriterInterceptor {
        private final MessageBodyWorkers workers;
        private final MessageBodySizeCallback sizeCallback;
        private final boolean writeEntity;

        public TerminalWriterInterceptor(MessageBodyWorkers workers, MessageBodySizeCallback sizeCallback, boolean writeEntity) {
            super();
            this.workers = workers;
            this.sizeCallback = sizeCallback;
            this.writeEntity = writeEntity;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void aroundWriteTo(WriterInterceptorContext context) throws WebApplicationException, IOException {

            final MessageBodyWriter writer = workers.getMessageBodyWriter(context.getType(), context.getGenericType(),
                    context.getAnnotations(), context.getMediaType());
            if (writer == null) {
                throw new MessageBodyProviderNotFoundException(LocalizationMessages.ERROR_NOTFOUND_MESSAGEBODYWRITER(
                        context.getMediaType(), context.getType(), context.getGenericType()));
            }
            if (sizeCallback != null) {
                long size = writer.getSize(context.getEntity(), context.getType(), context.getGenericType(),
                        context.getAnnotations(), context.getMediaType());
                sizeCallback.onRequestEntitySize(size);
            }

            if(writeEntity) {
                writer.writeTo(context.getEntity(), context.getType(), context.getGenericType(), context.getAnnotations(),
                        context.getMediaType(), context.getHeaders(), context.getOutputStream());
            }
        }
    }

}
