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
import java.lang.reflect.Type;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.ProcessingException;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Base outbound message context implementation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
// TODO revise - remove unused methods, fix warnings etc.
public class OutboundMessageContext {
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    private final MultivaluedMap<String, Object> headers;
    private final CommittingOutputStream rootStream;

    private Object entity;
    private GenericType<?> entityType;
    private Annotation[] entityAnnotations = EMPTY_ANNOTATIONS;
    private OutputStream entityStream;

    /**
     * Output stream provider.
     */
    public static interface StreamProvider {
        /**
         * Get the output stream.
         *
         * The method is called once as part of a "commit" operation immediately after
         * the {@link #commit()} method has been invoked.
         *
         * @return the adapted output stream.
         * @throws java.io.IOException in case of an IO error.
         */
        public OutputStream getOutputStream() throws IOException;

        /**
         * Perform the commit functionality.
         *
         * The method is called once as part of a "commit" operation before the first byte
         * is written to the provider stream.
         *
         * @throws java.io.IOException in case of an IO error.
         */
        public void commit() throws IOException;
    }

    /**
     * Create new outbound message context.
     */
    public OutboundMessageContext() {
        this.headers = HeadersFactory.createOutbound();
        this.rootStream = new CommittingOutputStream();
        this.entityStream = rootStream;
    }

    /**
     * Create new outbound message context copying the content
     * of another context.
     *
     * @param original the original outbound message context.
     */
    public OutboundMessageContext(OutboundMessageContext original) {
        this.headers = HeadersFactory.createOutbound();
        this.headers.putAll(original.headers);
        this.rootStream = new CommittingOutputStream();
        this.entityStream = rootStream;

        this.entity = original.entity;
        this.entityType = original.entityType;
        this.entityAnnotations = original.entityAnnotations;
    }

    // Message headers

    /**
     * Add a new header value.
     *
     * @param name  header name.
     * @param value header value.
     * @return updated context.
     */
    public OutboundMessageContext header(String name, Object value) {
        headers.add(name, value);
        return this;
    }

    /**
     * Add new header values.
     *
     * @param name   header name.
     * @param values header values.
     * @return updated context.
     */
    public OutboundMessageContext headers(String name, Object... values) {
        headers.addAll(name, values);
        return this;
    }

    /**
     * Add new header values.
     *
     * @param name   header name.
     * @param values header values.
     * @return updated context.
     */
    public OutboundMessageContext headers(String name, Iterable<?> values) {
        headers.addAll(name, iterableToList(values));
        return this;
    }

    /**
     * Add new headers.
     *
     * @param headers new headers.
     * @return updated context.
     */
    public OutboundMessageContext headers(MultivaluedMap<String, Object> headers) {
        headers.putAll(headers);
        return this;
    }

    /**
     * Remove a header.
     *
     * @param name header name.
     * @return updated context.
     */
    public OutboundMessageContext remove(String name) {
        headers.remove(name);
        return this;
    }

    /**
     * Replace header values with a new single header value.
     *
     * @param name  header name.
     * @param value new single header value.
     * @return updated context.
     */
    public OutboundMessageContext replace(String name, Object value) {
        headers.putSingle(name, value);
        return this;
    }

    /**
     * Replace header values.
     *
     * @param name   header name.
     * @param values new header values.
     * @return updated context.
     */
    public OutboundMessageContext replace(String name, Iterable<?> values) {
        headers.remove(name);
        headers.put(name, iterableToList(values));
        return this;
    }

    /**
     * Replace all headers.
     *
     * @param headers new headers.
     * @return updated context.
     */
    public OutboundMessageContext replaceAll(MultivaluedMap<String, Object> headers) {
        headers.clear();
        headers.putAll(headers);

        return this;
    }

    private List<Object> iterableToList(final Iterable<?> values) {
        final LinkedList<Object> linkedList = new LinkedList<Object>();

        for (Object element : values) {
            linkedList.add(element);
        }

        return linkedList;
    }


    // TODO: optimize internal use of getHeaderString(String) and stringify(String) for outbound object-oriented headers.
    //       Methods that use the function or the function itself should be optimized
    //       for working with object-based header values. The object-to string-to object
    //       conversion should be deferred if possible.

    /**
     * Get a message header as a single string value.
     *
     * Each single header value is converted to String using a
     * {@link javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate} if one is available
     * via {@link javax.ws.rs.ext.RuntimeDelegate#createHeaderDelegate(java.lang.Class)}
     * for the header value class or using its {@code toString} method  if a header
     * delegate is not available.
     *
     * @param name the message header.
     * @return the message header value. If the message header is not present then
     *         {@code null} is returned. If the message header is present but has no
     *         value then the empty string is returned. If the message header is present
     *         more than once then the values of joined together and separated by a ','
     *         character.
     */
    public String getHeaderString(String name) {
        return toHeaderString(headers.get(name));
    }

    private String toHeaderString(List<Object> values) {
        if (values == null) {
            return null;
        }
        final Iterator<String> stringValues = stringify(values).iterator();
        if (!stringValues.hasNext()) {
            return "";
        }

        StringBuilder buffer = new StringBuilder(stringValues.next());
        while (stringValues.hasNext()) {
            buffer.append(',').append(stringValues.next());
        }

        return buffer.toString();
    }

    private List<String> stringify(final List<Object> headerValues) {
        if (headerValues == null || headerValues.isEmpty()) {
            return Collections.emptyList();
        }

        return HeadersFactory.toString(headerValues, RuntimeDelegate.getInstance());
    }

    /**
     * Get a single typed header value.
     *
     * @param <T>       header value type.
     * @param name      header name.
     * @param valueType header value class.
     * @param converter from string conversion function. Is expected to throw {@link ProcessingException}
     *                  if conversion fails.
     * @return value of the header, or {@code null} if not present.
     */
    private <T> T singleHeader(String name, Class<T> valueType, Function<String, T> converter) {
        final List<Object> values = headers.get(name);

        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new HeaderValueException(LocalizationMessages.TOO_MANY_HEADER_VALUES(name, values.toString()));
        }

        Object value = values.get(0);
        if (value == null) {
            return null;
        }

        if (valueType.isInstance(value)) {
            return valueType.cast(value);
        } else {
            try {
                return converter.apply(HeadersFactory.toString(value, null));
            } catch (ProcessingException ex) {
                throw exception(name, value, ex);
            }
        }
    }

    private static HeaderValueException exception(final String headerName, Object headerValue, Exception e) {
        return new HeaderValueException(LocalizationMessages.UNABLE_TO_PARSE_HEADER_VALUE(headerName, headerValue), e);
    }

    /**
     * Get the mutable message headers multivalued map.
     *
     * @return mutable multivalued map of message headers.
     */
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    /**
     * Get message date.
     *
     * @return the message date, otherwise {@code null} if not present.
     */
    public Date getDate() {
        return singleHeader(HttpHeaders.DATE, Date.class, new Function<String, Date>() {
            @Override
            public Date apply(String input) {
                try {
                    return HttpHeaderReader.readDate(input);
                } catch (ParseException e) {
                    throw new ProcessingException(e);
                }
            }
        });
    }

    /**
     * Get the language of the entity.
     *
     * @return the language of the entity or {@code null} if not specified
     */
    public Locale getLanguage() {
        return singleHeader(HttpHeaders.CONTENT_LANGUAGE, Locale.class, new Function<String, Locale>() {
            @Override
            public Locale apply(String input) {
                try {
                    return new LanguageTag(input).getAsLocale();
                } catch (ParseException e) {
                    throw new ProcessingException(e);
                }
            }
        });
    }

    /**
     * Get the media type of the entity.
     *
     * @return the media type or {@code null} if not specified (e.g. there's no
     *         message entity).
     */
    public MediaType getMediaType() {
        return singleHeader(HttpHeaders.CONTENT_TYPE, MediaType.class, new Function<String, MediaType>() {
            @Override
            public MediaType apply(String input) {
                return MediaType.valueOf(input);
            }
        });
    }

    /**
     * Get a list of media types that are acceptable for the message.
     *
     * @return a read-only list of requested message media types sorted according
     *         to their q-value, with highest preference first.
     */
    @SuppressWarnings("unchecked")
    public List<MediaType> getAcceptableMediaTypes() {
        final List<Object> values = headers.get(HttpHeaders.ACCEPT);

        if (values == null || values.size() == 0) {
            return Collections.unmodifiableList(new ArrayList<MediaType>(MediaTypes.GENERAL_ACCEPT_MEDIA_TYPE_LIST));
        }
        final List<MediaType> result = new ArrayList<MediaType>(values.size());
        final RuntimeDelegate rd = RuntimeDelegate.getInstance();
        boolean conversionApplied = false;
        for (final Object value : values) {
            if (value instanceof MediaType) {
                result.add((MediaType) value);
            } else {
                conversionApplied = true;
                try {
                    result.addAll(HttpHeaderReader.readAcceptMediaType(HeadersFactory.toString(value, rd)));
                } catch (java.text.ParseException e) {
                    throw exception(HttpHeaders.ACCEPT, value, e);
                }
            }
        }

        if (conversionApplied) {
            // cache converted
            headers.put(HttpHeaders.ACCEPT, Lists.transform(result, new Function<MediaType, Object>() {
                @Override
                public Object apply(MediaType input) {
                    return input;
                }
            }));
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Get a list of languages that are acceptable for the message.
     *
     * @return a read-only list of acceptable languages sorted according
     *         to their q-value, with highest preference first.
     */
    public List<Locale> getAcceptableLanguages() {
        final List<Object> values = headers.get(HttpHeaders.ACCEPT_LANGUAGE);

        if (values == null || values.size() == 0) {
            return Collections.singletonList(new AcceptableLanguageTag("*", null).getAsLocale());
        }

        final List<Locale> result = new ArrayList<Locale>(values.size());
        final RuntimeDelegate rd = RuntimeDelegate.getInstance();
        boolean conversionApplied = false;
        for (final Object value : values) {
            if (value instanceof Locale) {
                result.add((Locale) value);
            } else {
                conversionApplied = true;
                try {
                    result.addAll(Lists.transform(HttpHeaderReader.readAcceptLanguage(HeadersFactory.toString(value, rd)),
                            new Function<AcceptableLanguageTag, Locale>() {

                                @Override
                                public Locale apply(AcceptableLanguageTag input) {
                                    return input.getAsLocale();
                                }
                            }));
                } catch (java.text.ParseException e) {
                    throw exception(HttpHeaders.ACCEPT_LANGUAGE, value, e);
                }
            }
        }

        if (conversionApplied) {
            // cache converted
            headers.put(HttpHeaders.ACCEPT_LANGUAGE, Lists.transform(result, new Function<Locale, Object>() {
                @Override
                public Object apply(Locale input) {
                    return input;
                }
            }));
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Get any cookies that accompanied the message.
     *
     * @return a read-only map of cookie name (String) to {@link javax.ws.rs.core.Cookie}.
     */
    public Map<String, Cookie> getRequestCookies() {
        final List<Object> cookies = headers.get(HttpHeaders.COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Cookie> result = new HashMap<String, Cookie>();
        for (String cookie : stringify(cookies)) {
            if (cookie != null) {
                result.putAll(HttpHeaderReader.readCookies(cookie));
            }
        }
        return result;
    }

    /**
     * Get the allowed HTTP methods from the Allow HTTP header.
     *
     * @return the allowed HTTP methods, all methods will returned as upper case
     *         strings.
     */
    public Set<String> getAllowedMethods() {
        final String allowed = getHeaderString(HttpHeaders.ALLOW);
        if (allowed == null || allowed.length() == 0) {
            return Collections.emptySet();
        }
        try {
            return new HashSet<String>(HttpHeaderReader.readStringList(allowed));
        } catch (java.text.ParseException e) {
            throw exception(HttpHeaders.ALLOW, allowed, e);
        }
    }

    /**
     * Get Content-Length value.
     *
     * @return Content-Length as integer if present and valid number. In other
     *         cases returns -1.
     */
    public int getLength() {
        return singleHeader(HttpHeaders.CONTENT_LENGTH, Integer.class, new Function<String, Integer>() {
            @Override
            public Integer apply(String input) {
                try {
                    return (input != null && input.length() > 0) ? Integer.parseInt(input) : -1;
                } catch (NumberFormatException ex) {
                    throw new ProcessingException(ex);
                }
            }
        });
    }

    /**
     * Get any new cookies set on the message message.
     *
     * @return a read-only map of cookie name (String) to a {@link javax.ws.rs.core.NewCookie new cookie}.
     */
    public Map<String, NewCookie> getResponseCookies() {
        List<Object> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, NewCookie> result = new HashMap<String, NewCookie>();
        for (String cookie : stringify(cookies)) {
            if (cookie != null) {
                NewCookie newCookie = HttpHeaderReader.readNewCookie(cookie);
                result.put(newCookie.getName(), newCookie);
            }
        }
        return result;
    }

    /**
     * Get the entity tag.
     *
     * @return the entity tag, otherwise {@code null} if not present.
     */
    public EntityTag getEntityTag() {
        return singleHeader(HttpHeaders.ETAG, EntityTag.class, new Function<String, EntityTag>() {
            @Override
            public EntityTag apply(String value) {
                try {
                    return value == null ? null : EntityTag.valueOf(value);
                } catch (IllegalArgumentException ex) {
                    throw new ProcessingException(ex);
                }
            }
        });
    }

    /**
     * Get the last modified date.
     *
     * @return the last modified date, otherwise {@code null} if not present.
     */
    public Date getLastModified() {
        return singleHeader(HttpHeaders.LAST_MODIFIED, Date.class, new Function<String, Date>() {
            @Override
            public Date apply(String input) {
                try {
                    return HttpHeaderReader.readDate(input);
                } catch (ParseException e) {
                    throw new ProcessingException(e);
                }
            }
        });
    }

    /**
     * Get the location.
     *
     * @return the location URI, otherwise {@code null} if not present.
     */
    public URI getLocation() {
        return singleHeader(HttpHeaders.LOCATION, URI.class, new Function<String, URI>() {
            @Override
            public URI apply(String value) {
                try {
                    return value == null ? null : URI.create(value);
                } catch (IllegalArgumentException ex) {
                    throw new ProcessingException(ex);
                }
            }
        });
    }

    /**
     * Get the links attached to the message as header.
     *
     * @return links, may return empty {@link java.util.Set} if no links are present. Never
     *         returns {@code null}.
     */
    public Set<Link> getLinks() {
        List<Object> values = headers.get(HttpHeaders.LINK);
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<Link> result = new HashSet<Link>(values.size());
        final RuntimeDelegate rd = RuntimeDelegate.getInstance();
        boolean conversionApplied = false;
        for (final Object value : values) {
            if (value instanceof Locale) {
                result.add((Link) value);
            } else {
                conversionApplied = true;
                try {
                    result.add(Link.valueOf(HeadersFactory.toString(value, rd)));
                } catch (IllegalArgumentException e) {
                    throw exception(HttpHeaders.LINK, value, e);
                }
            }
        }

        if (conversionApplied) {
            // cache converted
            headers.put(HttpHeaders.LINK, new ArrayList<Object>(Collections2.transform(result, new Function<Link, Object>() {
                @Override
                public Object apply(Link input) {
                    return input;
                }
            })));
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Check if link for relation exists.
     *
     * @param relation link relation.
     * @return {@code true} if the for the relation link exists, {@code false}
     *         otherwise.
     */
    public boolean hasLink(String relation) {
        for (Link link : getLinks()) {
            List<String> relations = link.getRel();
            if (relations != null && relations.contains(relation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the link for the relation.
     *
     * @param relation link relation.
     * @return the link for the relation, otherwise {@code null} if not present.
     */
    public Link getLink(String relation) {
        for (Link link : getLinks()) {
            List<String> relations = link.getRel();
            if (relations != null && relations.contains(relation)) {
                return link;
            }
        }
        return null;
    }

    /**
     * Convenience method that returns a {@link javax.ws.rs.core.Link.Builder Link.Builder}
     * for the relation.
     *
     * @param relation link relation.
     * @return the link builder for the relation, otherwise {@code null} if not
     *         present.
     */
    public Link.Builder getLinkBuilder(String relation) {
        Link link = getLink(relation);
        if (link == null) {
            return null;
        }

        return Link.fromLink(link);
    }

    // Message entity

    /**
     * Check if there is an entity available in the message.
     *
     * The method returns {@code true} if the entity is present, returns
     * {@code false} otherwise.
     *
     * @return {@code true} if there is an entity present in the message,
     *         {@code false} otherwise.
     */
    public boolean hasEntity() {
        return entity != null;
    }

    /**
     * Get the message entity Java instance.
     *
     * Returns {@code null} if the message does not contain an entity.
     *
     * @return the message entity or {@code null} if message does not contain an
     *         entity body.
     */
    public Object getEntity() {
        return entity;
    }

    /**
     * Set a new message message entity.
     *
     * @param entity entity object.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity) {
        setEntityAndType(entity);
    }

    private void setEntityAndType(Object entity) {
        final GenericType genericType;
        if (entity instanceof GenericEntity) {
            genericType = new GenericType(((GenericEntity) entity).getType());
        } else {
            genericType = (entity == null) ? null : new GenericType(entity.getClass());
        }

        setEntity(entity, genericType);
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param annotations annotations attached to the entity.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity, Annotation[] annotations) {
        setEntityAndType(entity);
        setEntityAnnotations(annotations);
    }

    /**
     * Set a new message message entity.
     *
     * @param entity entity object.
     * @param type   entity generic type information.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    private void setEntity(Object entity, GenericType<?> type) {
        if (entity instanceof GenericEntity) {
            this.entity = ((GenericEntity) entity).getEntity();
        } else {
            this.entity = entity;
        }
        // ignoring overridden generic entity type information
        this.entityType = type;
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param type        declared entity class.
     * @param annotations annotations attached to the entity.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity, Type type, Annotation[] annotations) {
        setEntity(entity, new GenericType(type));
        setEntityAnnotations(annotations);
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param annotations annotations attached to the entity.
     * @param mediaType   entity media type.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        setEntity(entity, annotations);
        setMediaType(mediaType);
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param type        declared generic entity type.
     * @param annotations annotations attached to the entity.
     * @param mediaType   entity media type.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity, Type type, Annotation[] annotations, MediaType mediaType) {
        setEntity(entity, type, annotations);
        setMediaType(mediaType);
    }

    /**
     * Set the message content media type.
     *
     * @param mediaType message content media type.
     */
    public void setMediaType(MediaType mediaType) {
        this.headers.putSingle(HttpHeaders.CONTENT_TYPE, mediaType);
    }

    /**
     * Get the raw message entity type information.
     *
     * @return raw message entity type information.
     */
    public Class<?> getEntityClass() {
        return entityType.getRawType();
    }

    /**
     * Get the message entity type information.
     *
     * @return message entity type.
     */
    public Type getEntityType() {
        return entityType.getType();
    }

    /**
     * Set the message entity type information.
     *
     * This method overrides any computed or previously set entity type information.
     *
     * @param type overriding message entity type.
     */
    public void setEntityType(Type type) {
        this.entityType = new GenericType(type);
    }

    /**
     * Get the annotations attached to the entity.
     *
     * @return entity annotations.
     */
    public Annotation[] getEntityAnnotations() {
        return entityAnnotations;
    }

    /**
     * Set the annotations attached to the entity.
     *
     * @param annotations entity annotations.
     */
    public void setEntityAnnotations(Annotation[] annotations) {
        this.entityAnnotations = (annotations == null) ? EMPTY_ANNOTATIONS : annotations;
    }

    /**
     * Get the entity output stream.
     *
     * @return entity output stream.
     */
    public OutputStream getEntityStream() {
        return entityStream;
    }

    /**
     * Set a new entity output stream.
     *
     * @param outputStream new entity output stream.
     */
    public void setEntityStream(OutputStream outputStream) {
        this.entityStream = outputStream;
    }

    /**
     * Set the output stream provider.
     *
     * @param streamProvider output stream provider.
     */
    public void setStreamProvider(StreamProvider streamProvider) {
        this.rootStream.setStreamProvider(streamProvider);
    }

    /**
     * Commits the {@link #getEntityStream() entity stream} if it wasn't already committed.
     */
    public void commitStream() {
        if (!rootStream.isCommitted()) {
            try {
                // flush the entity stream
                entityStream.flush();
                if (!rootStream.isCommitted()) {
                    // flush the committing stream
                    rootStream.flush();
                }
            } catch (Exception ioe) {
                // Do nothing - we are already handling an exception.
            }
        }
    }
}
