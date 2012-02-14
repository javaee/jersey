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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

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
     * Get the map of media type to list of message body writers that are
     * compatible with a media type.
     *
     * @param mediaType the compatible media type.
     * @return the map of media type to list of message body writers.
     */
    public Map<MediaType, List<MessageBodyReader>> getReaders(MediaType mediaType);

    /**
     * Get the map of media type to list of message body writers that are
     * compatible with a media type.
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
     * @param genericType the type of object to be produced. E.g. if the
     * message body is to be converted into a method parameter, this will be
     * the formal type of the method parameter as returned by
     * <code>Class.getGenericParameterTypes</code>.
     *
     * @param annotations an array of the annotations on the declaration of the
     * artifact that will be initialized with the produced instance. E.g. if the
     * message body is to be converted into a method parameter, this will be
     * the annotations on that parameter returned by
     * <code>Class.getParameterAnnotations</code>.
     *
     * @param mediaType the media type of the data that will be read, this will
     * be compared to the values of {@link javax.ws.rs.Consumes} for
     * each candidate reader and only matching readers will be queried.
     *
     * @return a MessageBodyReader that matches the supplied criteria or null
     * if none is found.
     */
    <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType,
            Annotation annotations[], MediaType mediaType);

    /**
     * Get a message body writer that matches a set of criteria.
     *
     * @param <T> the type of the object that is to be written.
     *
     * @param type the class of object that is to be written.
     *
     * @param genericType the type of object to be written. E.g. if the
     * message body is to be produced from a field, this will be
     * the declared type of the field as returned by
     * <code>Field.getGenericType</code>.
     *
     * @param annotations an array of the annotations on the declaration of the
     * artifact that will be written. E.g. if the
     * message body is to be produced from a field, this will be
     * the annotations on that field returned by
     * <code>Field.getDeclaredAnnotations</code>.
     *
     * @param mediaType the media type of the data that will be written, this will
     * be compared to the values of {@link javax.ws.rs.Produces} for
     * each candidate writer and only matching writers will be queried.
     *
     * @return a MessageBodyReader that matches the supplied criteria or null
     * if none is found.
     */
    <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType,
            Annotation annotations[], MediaType mediaType);

    /**
     * Get the list of media types supported for a Java type.
     *
     * @param <T> the type of object that is to be read.
     *
     * @param type the class of object that is to be read.
     *
     * @param genericType the type of object to be read. E.g. if the
     * message body is to be read as a method parameter, this will be
     * the declared type of the parameter as returned by
     * <code>Method.getGenericParameterTypes</code>.
     *
     * @param annotations an array of the annotations on the declaration of the
     * artifact that will be read. E.g. if the
     * message body is to be consumed as a method parameter, this will be
     * the annotations on that parameter returned by
     * <code>Method.getParameterAnnotations</code>.
     *
     * @return the list of supported media types, the list is ordered as
     * follows: a/b &lt a/* &lt *\\/*
     */
    <T> List<MediaType> getMessageBodyReaderMediaTypes(
            Class<T> type,
            Type genericType,
            Annotation[] annotations);

    /**
     * Get the list of media types supported for a Java type.
     *
     * @param <T> the type of object that is to be written.
     *
     * @param type the class of object that is to be written.
     *
     * @param genericType the type of object to be written. E.g. if the
     * message body is to be produced from a field, this will be
     * the declared type of the field as returned by
     * <code>Field.getGenericType</code>.
     *
     * @param annotations an array of the annotations on the declaration of the
     * artifact that will be written. E.g. if the
     * message body is to be produced from a field, this will be
     * the annotations on that field returned by
     * <code>Field.getDeclaredAnnotations</code>.
     *
     * @return the list of supported media types, the list is ordered as
     * follows: a/b &lt a/* &lt *\\/*
     */
    <T> List<MediaType> getMessageBodyWriterMediaTypes(
            Class<T> type,
            Type genericType,
            Annotation[] annotations);

    /**
     * Get the most acceptable media type supported for a Java type given a set
     * of acceptable media types.
     *
     * @param <T> the type of object that is to be written.
     *
     * @param type the class of object that is to be written.
     *
     * @param genericType the type of object to be written. E.g. if the
     * message body is to be produced from a field, this will be
     * the declared type of the field as returned by
     * <code>Field.getGenericType</code>.
     *
     * @param annotations an array of the annotations on the declaration of the
     * artifact that will be written. E.g. if the
     * message body is to be produced from a field, this will be
     * the annotations on that field returned by
     * <code>Field.getDeclaredAnnotations</code>.
     *
     * @param acceptableMediaTypes the list of acceptable media types, sorted
     *        according to the quality with the media type of highest quality
     *        occurring first first.
     * @return the best media types
     */
    <T> MediaType getMessageBodyWriterMediaType(
            Class<T> type,
            Type genericType,
			Annotation[] annotations,
            List<MediaType> acceptableMediaTypes);
}
