/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Configuration;
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

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.ReflectionHelper;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Base outbound message context implementation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class OutboundMessageContext {
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    private static final List<MediaType> WILDCARD_ACCEPTABLE_TYPE_SINGLETON_LIST =
            Collections.<MediaType>singletonList(MediaTypes.WILDCARD_ACCEPTABLE_TYPE);

    private final MultivaluedMap<String, Object> headers;
    private final CommittingOutputStream committingOutputStream;

    private Object entity;
    private GenericType<?> entityType;
    private Annotation[] entityAnnotations = EMPTY_ANNOTATIONS;
    private OutputStream entityStream;


    /**
     * The callback interface which is used to get the terminal output stream into which the entity should be
     * written and to inform the implementation about the entity size.
     */
    public static interface StreamProvider {
        /**
         * Get the output stream. This method will be called after all the
         * {@link javax.ws.rs.ext.WriterInterceptor writer interceptors} are called and written entity is buffered
         * into the buffer or the buffer exceeds.
         *
         * @param contentLength the size of the buffered entity or -1 if the entity exceeded the maximum buffer
         *                      size or if the buffering is disabled.
         * @return the adapted output stream into which the serialized entity should be written. May return null
         * which will cause ignoring the written entity (in that case the entity will
         * still be written by {@link javax.ws.rs.ext.MessageBodyWriter message body writers}
         * but the output will be ignored).
         * @throws java.io.IOException in case of an IO error.
         */
        public OutputStream getOutputStream(int contentLength) throws IOException;
    }

    /**
     * Create new outbound message context.
     */
    public OutboundMessageContext() {
        this.headers = HeaderUtils.createOutbound();
        this.committingOutputStream = new CommittingOutputStream();
        this.entityStream = committingOutputStream;
    }

    /**
     * Create new outbound message context copying the content
     * of another context.
     *
     * @param original the original outbound message context.
     */
    public OutboundMessageContext(OutboundMessageContext original) {
        this.headers = HeaderUtils.createOutbound();
        this.headers.putAll(original.headers);
        this.committingOutputStream = new CommittingOutputStream();
        this.entityStream = committingOutputStream;

        this.entity = original.entity;
        this.entityType = original.entityType;
        this.entityAnnotations = original.entityAnnotations;
    }

    /**
     * Replace all headers.
     *
     * @param headers new headers.
     */
    public void replaceHeaders(MultivaluedMap<String, Object> headers) {
        getHeaders().clear();
        if (headers != null) {
            getHeaders().putAll(headers);
        }
    }

    /**
     * Get a multi-valued map representing outbound message headers with their values converted
     * to strings.
     *
     * @return multi-valued map of outbound message header names to their string-converted values.
     */
    public MultivaluedMap<String, String> getStringHeaders() {
        return HeaderUtils.asStringHeaders(headers);
    }

    /**
     * Get a message header as a single string value.
     * <p>
     * Each single header value is converted to String using a
     * {@link javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate} if one is available
     * via {@link javax.ws.rs.ext.RuntimeDelegate#createHeaderDelegate(java.lang.Class)}
     * for the header value class or using its {@code toString} method  if a header
     * delegate is not available.
     *
     * @param name the message header.
     * @return the message header value. If the message header is not present then
     * {@code null} is returned. If the message header is present but has no
     * value then the empty string is returned. If the message header is present
     * more than once then the values of joined together and separated by a ','
     * character.
     */
    public String getHeaderString(String name) {
        return HeaderUtils.asHeaderString(headers.get(name), RuntimeDelegate.getInstance());
    }

    /**
     * Get a single typed header value.
     *
     * @param <T>         header value type.
     * @param name        header name.
     * @param valueType   header value class.
     * @param converter   from string conversion function. Is expected to throw {@link ProcessingException}
     *                    if conversion fails.
     * @param convertNull if {@code true} this method calls the provided converter even for {@code null}. Otherwise this
     *                    method returns the {@code null} without calling the converter.
     * @return value of the header, or (possibly converted) {@code null} if not present.
     */
    private <T> T singleHeader(String name, Class<T> valueType, Function<String, T> converter, boolean convertNull) {
        final List<Object> values = headers.get(name);

        if (values == null || values.isEmpty()) {
            return convertNull ? converter.apply(null) : null;
        }
        if (values.size() > 1) {
            throw new HeaderValueException(
                    LocalizationMessages.TOO_MANY_HEADER_VALUES(name, values.toString()),
                    HeaderValueException.Context.OUTBOUND);
        }

        Object value = values.get(0);
        if (value == null) {
            return convertNull ? converter.apply(null) : null;
        }

        if (valueType.isInstance(value)) {
            return valueType.cast(value);
        } else {
            try {
                return converter.apply(HeaderUtils.asString(value, null));
            } catch (ProcessingException ex) {
                throw exception(name, value, ex);
            }
        }
    }

    private static HeaderValueException exception(final String headerName, Object headerValue, Exception e) {
        return new HeaderValueException(LocalizationMessages.UNABLE_TO_PARSE_HEADER_VALUE(headerName, headerValue), e,
                HeaderValueException.Context.OUTBOUND);
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
        }, false);
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
        }, false);
    }

    /**
     * Get the media type of the entity.
     *
     * @return the media type or {@code null} if not specified (e.g. there's no
     * message entity).
     */
    public MediaType getMediaType() {
        return singleHeader(HttpHeaders.CONTENT_TYPE, MediaType.class, new Function<String, MediaType>() {
            @Override
            public MediaType apply(String input) {
                return MediaType.valueOf(input);
            }
        }, false);
    }

    /**
     * Get a list of media types that are acceptable for the message.
     *
     * @return a read-only list of requested message media types sorted according
     * to their q-value, with highest preference first.
     */
    @SuppressWarnings("unchecked")
    public List<MediaType> getAcceptableMediaTypes() {
        final List<Object> values = headers.get(HttpHeaders.ACCEPT);

        if (values == null || values.isEmpty()) {
            return WILDCARD_ACCEPTABLE_TYPE_SINGLETON_LIST;
        }
        final List<MediaType> result = new ArrayList<MediaType>(values.size());
        final RuntimeDelegate rd = RuntimeDelegate.getInstance();
        boolean conversionApplied = false;
        for (final Object value : values) {
            try {
                if (value instanceof MediaType) {
                    final AcceptableMediaType _value = AcceptableMediaType.valueOf((MediaType) value);
                    conversionApplied = _value != value; // true if value was not an instance of AcceptableMediaType already
                    result.add(_value);
                } else {
                    conversionApplied = true;
                    result.addAll(HttpHeaderReader.readAcceptMediaType(HeaderUtils.asString(value, rd)));
                }
            } catch (java.text.ParseException e) {
                throw exception(HttpHeaders.ACCEPT, value, e);
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
     * to their q-value, with highest preference first.
     */
    public List<Locale> getAcceptableLanguages() {
        final List<Object> values = headers.get(HttpHeaders.ACCEPT_LANGUAGE);

        if (values == null || values.isEmpty()) {
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
                    result.addAll(Lists.transform(
                            HttpHeaderReader.readAcceptLanguage(HeaderUtils.asString(value, rd)),
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
        for (String cookie : HeaderUtils.asStringList(cookies, RuntimeDelegate.getInstance())) {
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
     * strings.
     */
    public Set<String> getAllowedMethods() {
        final String allowed = getHeaderString(HttpHeaders.ALLOW);
        if (allowed == null || allowed.isEmpty()) {
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
     * <p>
     * <B>Note</B>: {@link #getLengthLong() getLengthLong()}
     * should be preferred over this method, since it returns a {@code long}
     * instead and is therefore more portable.</P>
     *
     * @return Content-Length as a postive integer if present and valid number, {@code -1} if negative number.
     * @throws ProcessingException when {@link Integer#parseInt(String)} (String)} throws {@link NumberFormatException}.
     */
    public int getLength() {
        return singleHeader(HttpHeaders.CONTENT_LENGTH, Integer.class, new Function<String, Integer>() {
            @Override
            public Integer apply(String input) {
                try {
                    if (input != null && !input.isEmpty()) {
                        int i = Integer.parseInt(input);
                        if (i >= 0) {
                            return i;
                        }
                    }
                    return -1;

                } catch (NumberFormatException ex) {
                    throw new ProcessingException(ex);
                }
            }
        }, true);
    }

    /**
     * Get Content-Length value.
     *
     * @return Content-Length as a positive long if present and valid number, {@code -1} if negative number.
     * @throws ProcessingException when {@link Long#parseLong(String)} throws {@link NumberFormatException}.
     */
    public long getLengthLong() {
        return singleHeader(HttpHeaders.CONTENT_LENGTH, Long.class, new Function<String, Long>() {
            @Override
            public Long apply(String input) {
                try {
                    if (input != null && !input.isEmpty()) {
                        long l = Long.parseLong(input);
                        if (l >= 0) {
                            return l;
                        }
                    }
                    return -1L;
                } catch (NumberFormatException ex) {
                    throw new ProcessingException(ex);
                }
            }
        }, true);
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
        for (String cookie : HeaderUtils.asStringList(cookies, RuntimeDelegate.getInstance())) {
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
        }, false);
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
        }, false);
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
        }, false);
    }

    /**
     * Get the links attached to the message as header.
     *
     * @return links, may return empty {@link java.util.Set} if no links are present. Never
     * returns {@code null}.
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
            if (value instanceof Link) {
                result.add((Link) value);
            } else {
                conversionApplied = true;
                try {
                    result.add(Link.valueOf(HeaderUtils.asString(value, rd)));
                } catch (IllegalArgumentException e) {
                    throw exception(HttpHeaders.LINK, value, e);
                }
            }
        }

        if (conversionApplied) {
            // cache converted
            headers.put(HttpHeaders.LINK, new ArrayList<Object>(Collections2
                    .transform(result, new Function<Link, Object>() {
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
     * otherwise.
     */
    public boolean hasLink(String relation) {
        for (Link link : getLinks()) {
            List<String> relations = LinkProvider.getLinkRelations(link.getRel());
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
            List<String> relations = LinkProvider.getLinkRelations(link.getRel());
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
     * present.
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
     * <p>
     * The method returns {@code true} if the entity is present, returns
     * {@code false} otherwise.
     *
     * @return {@code true} if there is an entity present in the message,
     * {@code false} otherwise.
     */
    public boolean hasEntity() {
        return entity != null;
    }

    /**
     * Get the message entity Java instance.
     * <p>
     * Returns {@code null} if the message does not contain an entity.
     *
     * @return the message entity or {@code null} if message does not contain an
     * entity body.
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
        setEntity(entity, ReflectionHelper.genericTypeFor(entity));
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param annotations annotations attached to the entity.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity, Annotation[] annotations) {
        setEntity(entity, ReflectionHelper.genericTypeFor(entity));
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
        return entityType == null ? null : entityType.getRawType();
    }

    /**
     * Get the message entity type information.
     *
     * @return message entity type.
     */
    public Type getEntityType() {
        return entityType == null ? null : entityType.getType();
    }

    /**
     * Set the message entity type information.
     * <p>
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
        return entityAnnotations.clone();
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
     * Enable a buffering of serialized entity. The buffering will be configured from configuration. The property
     * determining the size of the buffer is {@link CommonProperties#OUTBOUND_CONTENT_LENGTH_BUFFER}.
     * </p>
     * The buffering functionality is by default disabled and could be enabled by calling this method. In this case
     * this method must be called before first bytes are written to the {@link #getEntityStream() entity stream}.
     *
     * @param configuration runtime configuration.
     */
    public void enableBuffering(Configuration configuration) {
        final Integer bufferSize = CommonProperties.getValue(configuration.getProperties(),
                configuration.getRuntimeType(), CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, Integer.class);
        if (bufferSize != null) {
            committingOutputStream.enableBuffering(bufferSize);
        } else {
            committingOutputStream.enableBuffering();
        }
    }

    /**
     * Set a stream provider callback.
     * <p/>
     * This method must be called before first bytes are written to the {@link #getEntityStream() entity stream}.
     *
     * @param streamProvider non-{@code null} output stream provider.
     */
    public void setStreamProvider(StreamProvider streamProvider) {
        committingOutputStream.setStreamProvider(streamProvider);
    }


    /**
     * Commits the {@link #getEntityStream() entity stream} if it wasn't already committed.
     *
     * @throws IOException in case of the IO error.
     */
    public void commitStream() throws IOException {
        if (!committingOutputStream.isCommitted()) {
            entityStream.flush();
            if (!committingOutputStream.isCommitted()) {
                committingOutputStream.commit();
                committingOutputStream.flush();
            }
        }
    }

    /**
     * Returns {@code true} if the entity stream has been committed.
     *
     * @return {@code true} if the entity stream has been committed. Otherwise returns {@code false}.
     */
    public boolean isCommitted() {
        return committingOutputStream.isCommitted();
    }

    /**
     * Closes the context. Flushes and closes the entity stream.
     */
    public void close() {
        if (hasEntity()) {
            try {
                final OutputStream es = getEntityStream();
                es.flush();
                es.close();
            } catch (IOException e) {
                // Happens when the client closed connection before receiving the full response.
                // This is OK and not interesting in vast majority of the cases
                // hence the log level set to FINE to make sure it does not flood the log unnecessarily
                // (especially for clients disconnecting from SSE listening, which is very common).
                Logger.getLogger(OutboundMessageContext.class.getName()).log(Level.FINE, e.getMessage(), e);
            } finally {
                // In case some of the output stream wrapper does not delegate close() call we
                // close the root stream manually to make sure it commits the data.
                if (!committingOutputStream.isClosed()) {
                    try {
                        committingOutputStream.close();
                    } catch (IOException e) {
                        // Just log the exception
                        Logger.getLogger(OutboundMessageContext.class.getName()).log(Level.FINE, e.getMessage(), e);
                    }
                }
            }
        }
    }
}
