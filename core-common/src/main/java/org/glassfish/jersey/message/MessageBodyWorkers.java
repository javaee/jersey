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
package org.glassfish.jersey.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.message.internal.ExceptionWrapperInterceptor;

/**
 * An injectable interface providing lookup of {@link MessageBodyReader} and
 * {@link MessageBodyWriter} instances.
 *
 * @see javax.ws.rs.core.Context
 * @see MessageBodyReader
 * @see MessageBodyWriter
 * @author Paul Sandoz
 */
public interface MessageBodyWorkers {
    /**
     * Get the map of media type to list of message body writers that are compatible with
     * a media type.
     *
     * @param mediaType the compatible media type.
     * @return the map of media type to list of message body writers.
     */
    public Map<MediaType, List<MessageBodyReader>> getReaders(MediaType mediaType);

    /**
     * Get the map of media type to list of message body writers that are compatible with
     * a media type.
     *
     * @param mediaType the compatible media type.
     * @return the map of media type to list of message body writers.
     */
    public Map<MediaType, List<MessageBodyWriter>> getWriters(MediaType mediaType);

    /**
     * Convert a map media type to list of message body readers to a string.
     *
     * @param readers the map media type to list of message body readers
     * @return the string representation.
     */
    public String readersToString(Map<MediaType, List<MessageBodyReader>> readers);

    /**
     * Convert a map media type to list of message body writers to a string.
     *
     * @param writers the map media type to list of message body readers
     * @return the string representation.
     */
    public String writersToString(Map<MediaType, List<MessageBodyWriter>> writers);

    /**
     * Get a message body reader that matches a set of criteria.
     *
     * @param <T> the type of object to be read.
     *
     * @param type the class of object to be read.
     *
     * @param genericType the type of object to be produced. E.g. if the message body is
     *            to be converted into a method parameter, this will be the formal type of
     *            the method parameter as returned by
     *            <code>Class.getGenericParameterTypes</code>.
     *
     * @param annotations an array of the annotations on the declaration of the artifact
     *            that will be initialized with the produced instance. E.g. if the message
     *            body is to be converted into a method parameter, this will be the
     *            annotations on that parameter returned by
     *            <code>Class.getParameterAnnotations</code>.
     *
     * @param mediaType the media type of the data that will be read, this will be
     *            compared to the values of {@link javax.ws.rs.Consumes} for each
     *            candidate reader and only matching readers will be queried.
     *
     * @return a MessageBodyReader that matches the supplied criteria or null if none is
     *         found.
     */
    <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation annotations[], MediaType mediaType);

    /**
     * Get a message body writer that matches a set of criteria.
     *
     * @param <T> the type of the object that is to be written.
     *
     * @param type the class of object that is to be written.
     *
     * @param genericType the type of object to be written. E.g. if the message body is to
     *            be produced from a field, this will be the declared type of the field as
     *            returned by <code>Field.getGenericType</code>.
     *
     * @param annotations an array of the annotations on the declaration of the artifact
     *            that will be written. E.g. if the message body is to be produced from a
     *            field, this will be the annotations on that field returned by
     *            <code>Field.getDeclaredAnnotations</code>.
     *
     * @param mediaType the media type of the data that will be written, this will be
     *            compared to the values of {@link javax.ws.rs.Produces} for each
     *            candidate writer and only matching writers will be queried.
     *
     * @return a MessageBodyReader that matches the supplied criteria or null if none is
     *         found.
     */
    <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation annotations[], MediaType mediaType);

    /**
     * Get the list of media types supported for a Java type.
     *
     * @param <T> the type of object that is to be read.
     *
     * @param type the class of object that is to be read.
     *
     * @param genericType the type of object to be read. E.g. if the message body is to be
     *            read as a method parameter, this will be the declared type of the
     *            parameter as returned by <code>Method.getGenericParameterTypes</code>.
     *
     * @param annotations an array of the annotations on the declaration of the artifact
     *            that will be read. E.g. if the message body is to be consumed as a
     *            method parameter, this will be the annotations on that parameter
     *            returned by <code>Method.getParameterAnnotations</code>.
     *
     * @return the list of supported media types, the list is ordered as follows: a/b &lt
     *         a/* &lt *\\/*
     */
    <T> List<MediaType> getMessageBodyReaderMediaTypes(Class<T> type, Type genericType, Annotation[] annotations);

    /**
     * Get the list of media types supported for a Java type.
     *
     * @param <T> the type of object that is to be written.
     *
     * @param type the class of object that is to be written.
     *
     * @param genericType the type of object to be written. E.g. if the message body is to
     *            be produced from a field, this will be the declared type of the field as
     *            returned by <code>Field.getGenericType</code>.
     *
     * @param annotations an array of the annotations on the declaration of the artifact
     *            that will be written. E.g. if the message body is to be produced from a
     *            field, this will be the annotations on that field returned by
     *            <code>Field.getDeclaredAnnotations</code>.
     *
     * @return the list of supported media types, the list is ordered as follows: a/b &lt
     *         a/* &lt *\\/*
     */
    <T> List<MediaType> getMessageBodyWriterMediaTypes(Class<T> type, Type genericType, Annotation[] annotations);

    /**
     * Get the most acceptable media type supported for a Java type given a set of
     * acceptable media types.
     *
     * @param <T> the type of object that is to be written.
     *
     * @param type the class of object that is to be written.
     *
     * @param genericType the type of object to be written. E.g. if the message body is to
     *            be produced from a field, this will be the declared type of the field as
     *            returned by <code>Field.getGenericType</code>.
     *
     * @param annotations an array of the annotations on the declaration of the artifact
     *            that will be written. E.g. if the message body is to be produced from a
     *            field, this will be the annotations on that field returned by
     *            <code>Field.getDeclaredAnnotations</code>.
     *
     * @param acceptableMediaTypes the list of acceptable media types, sorted according to
     *            the quality with the media type of highest quality occurring first
     *            first.
     * @return the best media types
     */
    <T> MediaType getMessageBodyWriterMediaType(Class<T> type, Type genericType, Annotation[] annotations,
            List<MediaType> acceptableMediaTypes);

    /**
     * Returns global reader interceptors.
     *
     * @return Reader interceptors.
     */
    public Set<ReaderInterceptor> getReaderInterceptors();

    /**
     * Returns global writer interceptors.
     *
     * @return Writer interceptors.
     */
    public Set<WriterInterceptor> getWriterInterceptors();

    /**
     * Reads a type from the {@link InputStream entityStream} using interceptors. If the
     * parameter {@code intercept} is true then {@link ReaderInterceptor reader
     * interceptors} are excecuted before calling the {@link MessageBodyReader message
     * body reader}. The appropriate {@link MessageBodyReader message body reader} is
     * choosen after the interceptor execution based on parameter passed to this method
     * and modified by the interceptors.
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
     * @param intercept true if the user interceptors should be executed. Otherwise only
     *            {@link ExceptionWrapperInterceptor exception wrapping interceptor} will
     *            be executed in the client.
     * @return the entity that was read from the {@code entityStream}.
     * @throws WebApplicationException Thrown when {@link MessageBodyReader message body
     *             reader} fails.
     * @throws IOException Thrown when reading from the {@code entityStream} fails.
     */
    public <T> Object readFrom(GenericType<T> genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, Map<String, Object> properties, InputStream entityStream,
            boolean intercept) throws WebApplicationException, IOException;

    /**
     * Writers a type to the {@link OutputStream entityStream} using interceptors. If the
     * parameter {@code intercept} is true then {@link WriterInterceptor writer
     * interceptors} are excecuted before calling the {@link MessageBodyWriter message
     * body writer}. The appropriate {@link MessageBodyWriter message body writer} is
     * choosen after the interceptor execution based on parameter passed to this method
     * and modified by the interceptors.
     *
     * @param entity Entity to be written to the entityStream
     * @param genericType the generic type to be written into the {@code entityStream}.
     * @param annotations an array of the annotations on the resource method that returns
     *            the object.
     * @param mediaType the media type of the HTTP entity.
     * @param httpHeaders the mutable HTTP headers associated with HTTP entity.
     * @param properties the mutable map of {@link Request#getProperties() request-scoped
     *            properties}.
     * @param entityStream the {@link OutputStream} for the HTTP entity.
     * @param sizeCallback the {@link MessageBodySizeCallback} which will be invoked to
     *            pass the size of the written entity. The callback will be invoked before
     *            the first byte is written to the {@code entityStream}.
     * @param intercept true if the user interceptors should be executed. Otherwise only
     *            {@link ExceptionWrapperInterceptor exception wrapping interceptor} will
     *            be executed in the client.
     * @throws WebApplicationException Thrown when {@link MessageBodyReader message body
     *             reader} fails.
     * @throws IOException Thrown when reading from the {@code entityStream} fails.
     */
    public <T> void writeTo(Object entity, GenericType<T> genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, Map<String, Object> properties, OutputStream entityStream,
            MessageBodySizeCallback sizeCallback, boolean intercept) throws java.io.IOException,
            javax.ws.rs.WebApplicationException;

    /**
     * Callback which will be used to pass back the size of the entity. It will be invoked
     * in method
     * {@link MessageBodyWorkers#writeTo(Object, GenericType, Annotation[], MediaType,
     * MultivaluedMap, Map, OutputStream, MessageBodySizeCallback, boolean)}
     * after selection of the {@link MessageBodyWriter message body writer} and before
     * writing to the output stream.
     */
    public interface MessageBodySizeCallback {

        /**
         * Called when the size of the request entity is obtained. <p>Enables the appropriate
         * setting of HTTP headers for the size of the request entity and/or configure
         * an appropriate transport encoding.</p>
         *
         * @param size Size in bytes of the request
         *            entity, otherwise -1 if the size cannot be determined before
         *            serialization.
         * @throws IOException When IO operations fail
         */
        public void onRequestEntitySize(long size) throws IOException;
    }

}
