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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.process.internal.PriorityComparator;
import org.glassfish.jersey.process.internal.PriorityComparator.Order;

/**
 * Entry point of the reader interceptor chain. It contstructs the chain of wrapped
 * interceptor and invokes it. At the end of the chain {@link the MessageBodyWriter MBW}
 * is invoked which writes the entity to the output stream. The
 * {@link ExceptionWrapperInterceptor} is always invoked on the client as a first
 * interceptor.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
@SuppressWarnings("rawtypes")
public class ReaderInterceptorExecutor extends InterceptorExecutor implements ReaderInterceptorContext {
    private final List<ReaderInterceptor> interceptors;
    private InputStream inputStream;
    private final MultivaluedMap<String, String> headers;
    private Iterator<ReaderInterceptor> iterator;

    /**
     * Reads a type from the {@link InputStream entityStream} using interceptors.
     *
     * @param genericType the generic type that is to be read from the input stream.
     * @param annotations an array of the annotations on the declaration of the artifact
     *            that will be initialized with the produced instance. E.g. if the message
     *            body is to be converted into a method parameter, this will be the
     *            annotations on that parameter returned by
     *            <code>Method.getParameterAnnotations</code>.
     * @param mediaType the media type of the HTTP entity.
     * @param httpHeaders the mutable HTTP headers associated with HTTP entity.
     * @param properties the mutable map of {@link Request#getProperties() request-scoped
     *            properties}.
     * @param entityStream the {@link InputStream} of the HTTP entity. The stream is not
     *            closed after reading the entity.
     * @param workers {@link MessageBodyWorkers Message body workers}.
     * @param intercept true if the user interceptors should be executed. Otherwise only
     *            {@link ExceptionWrapperInterceptor exception wrapping interceptor} will
     *            be executed in the client.
     * @return the type that was read from the {@code entityStream}.
     * @throws WebApplicationException Thrown when {@link MessageBodyReader message body
     *             reader} fails.
     * @throws IOException Thrown when reading from the {@code entityStream} fails.
     */
    public ReaderInterceptorExecutor(GenericType genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> headers, Map<String, Object> properties, InputStream inputStream,
            MessageBodyWorkers workers, boolean intercept) {
        super(genericType, annotations, mediaType, properties);
        this.headers = headers;
        this.inputStream = inputStream;

        interceptors = new ArrayList<ReaderInterceptor>();
        for (ReaderInterceptor interceptor : workers.getReaderInterceptors()) {
            if (intercept || (interceptor instanceof ExceptionWrapperInterceptor)) {
                interceptors.add(interceptor);
            }
        }
        Collections.sort(interceptors, new PriorityComparator<ReaderInterceptor>(Order.ASCENDING));

        interceptors.add(new TerminalReaderInterceptor(workers));
        this.iterator = interceptors.iterator();
    }

    /**
     * Returns next {@link ReaderInterceptor interceptor} in the chain. Stateful method.
     *
     * @return Next interceptor.
     */
    public ReaderInterceptor getNextInterceptor() {
        if (!iterator.hasNext()) {
            return null;
        }
        return iterator.next();
    }

    /**
     * Starts the interceptor chain execution.
     *
     * @return an entity read from the stream.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object proceed() throws IOException {
        ReaderInterceptor nextInterceptor = getNextInterceptor();
        if (nextInterceptor == null) {
            throw new ProcessingException(LocalizationMessages.ERROR_INTERCEPTOR_READER_PROCEED());
        }
        return nextInterceptor.aroundReadFrom(this);
    }

    @Override
    public InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public void setInputStream(InputStream is) {
        this.inputStream = is;

    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Terminal reader interceptor which choose the appropriate {@link MessageBodyReader}
     * and reads the entity from the input stream. The order of actions is the following: <br>
     * 1. choose the appropriate {@link MessageBodyWriter} <br>
     * 3. reads the entity from the output stream <br>
     */
    private static class TerminalReaderInterceptor implements ReaderInterceptor {
        private final MessageBodyWorkers workers;

        public TerminalReaderInterceptor(MessageBodyWorkers workers) {
            super();
            this.workers = workers;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final MessageBodyReader bodyReader = workers.getMessageBodyReader(context.getType(), context.getGenericType(),
                    context.getAnnotations(), context.getMediaType());

            if (bodyReader == null) {
                throw new MessageBodyProviderNotFoundException(LocalizationMessages.ERROR_NOTFOUND_MESSAGEBODYREADER(
                        context.getMediaType(), context.getType(), context.getGenericType()));
            }

            Object entity = bodyReader.readFrom(context.getType(), context.getGenericType(), context.getAnnotations(),
                    context.getMediaType(), context.getHeaders(), context.getInputStream());

            if (bodyReader instanceof CompletableReader) {
                entity = ((CompletableReader) bodyReader).complete(entity);
            }
            return entity;

        }

    }
}
