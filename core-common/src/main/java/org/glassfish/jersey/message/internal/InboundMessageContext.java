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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;

import javax.xml.transform.Source;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;

import com.google.common.base.Function;

/**
 * Base inbound message context implementation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
// TODO revise - remove unused methods, fix warnings etc.
public class InboundMessageContext {
    private static final Logger LOGGER = Logger.getLogger(InboundMessageContext.class.getName());
    private static final InputStream EMPTY = new InputStream() {

        @Override
        public int read() throws IOException {
            return -1;
        }
    };
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

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


    private final MultivaluedMap<String, String> headers;
    private final ContentStream contentStream;
    private MessageBodyWorkers workers;

    /**
     * Create new inbound message context.
     */
    public InboundMessageContext() {
        this.headers = HeadersFactory.createInbound();
        this.contentStream = new ContentStream(EMPTY);
    }

    // Message headers

    /**
     * Add a new header value.
     *
     * @param name  header name.
     * @param value header value.
     * @return updated context.
     */
    public InboundMessageContext header(String name, Object value) {
        this.headers.add(name, HeadersFactory.toString(value, RuntimeDelegate.getInstance()));
        return this;
    }

    /**
     * Add new header values.
     *
     * @param name   header name.
     * @param values header values.
     * @return updated context.
     */
    public InboundMessageContext headers(String name, Object... values) {
        this.headers.addAll(name, HeadersFactory.toString(Arrays.asList(values), RuntimeDelegate.getInstance()));
        return this;
    }

    /**
     * Add new header values.
     *
     * @param name   header name.
     * @param values header values.
     * @return updated context.
     */
    public InboundMessageContext headers(String name, Iterable<?> values) {
        this.headers.addAll(name, iterableToList(values));
        return this;
    }

    /**
     * Add new headers.
     *
     * @param headers new headers.
     * @return updated context.
     */
    public InboundMessageContext headers(MultivaluedMap<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Add new headers.
     *
     * @param headers new headers.
     * @return updated context.
     */
    public InboundMessageContext headers(Map<String, List<String>> headers) {
        this.headers.putAll(HeadersFactory.createInbound(headers));
        return this;
    }

    /**
     * Remove a header.
     *
     * @param name header name.
     * @return updated context.
     */
    public InboundMessageContext remove(String name) {
        this.headers.remove(name);
        return this;
    }

    /**
     * Replace header values.
     *
     * @param name   header name.
     * @param values new header values.
     * @return updated context.
     */
    public InboundMessageContext replace(String name, Iterable<?> values) {
        this.headers.remove(name);
        this.headers.put(name, iterableToList(values));
        return this;
    }

    /**
     * Replace all headers.
     *
     * @param headers new headers.
     * @return updated context.
     */
    public InboundMessageContext replaceAll(MultivaluedMap<String, String> headers) {
        this.headers.clear();
        this.headers.putAll(headers);

        return this;
    }


    private static List<String> iterableToList(final Iterable<?> values) {
        final LinkedList<String> linkedList = new LinkedList<String>();

        final RuntimeDelegate rd = RuntimeDelegate.getInstance();
        for (Object element : values) {
            linkedList.add(HeadersFactory.toString(element, rd));
        }

        return linkedList;
    }

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
        return toHeaderString(this.headers.get(name));
    }

    private String toHeaderString(List<String> values) {
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return "";
        }

        final Iterator<String> valuesIterator = values.iterator();
        StringBuilder buffer = new StringBuilder(valuesIterator.next());
        while (valuesIterator.hasNext()) {
            buffer.append(',').append(valuesIterator.next());
        }

        return buffer.toString();
    }

    /**
     * Get a single typed header value.
     *
     * @param <T>       header value type.
     * @param name      header name.
     * @param converter from string conversion function. Is expected to throw {@link org.glassfish.jersey.internal.ProcessingException}
     *                  if conversion fails.
     * @return value of the header, or {@code null} if not present.
     */
    private <T> T singleHeader(String name, Function<String, T> converter) {
        final List<String> values = this.headers.get(name);

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

        try {
            return converter.apply(HeadersFactory.toString(value, null));
        } catch (ProcessingException ex) {
            throw exception(name, value, ex);
        }
    }

    private static HeaderValueException exception(final String headerName, Object headerValue, Exception e) {
        return new HeaderValueException(LocalizationMessages.UNABLE_TO_PARSE_HEADER_VALUE(headerName, headerValue), e);
    }

    /**
     * Get the mutable message headers multivalued map.
     *
     * @return mutable multivalued map of response headers.
     */
    public MultivaluedMap<String, String> getHeaders() {
        return this.headers;
    }

    /**
     * Get message date.
     *
     * @return the message date, otherwise {@code null} if not present.
     */
    public Date getDate() {
        return singleHeader(HttpHeaders.DATE, new Function<String, Date>() {
            @Override
            public Date apply(String input) {
                try {
                    return input == null ? null : HttpHeaderReader.readDate(input);
                } catch (ParseException ex) {
                    throw new ProcessingException(ex);
                }
            }
        });
    }

    /**
     * Get If-Match header.
     *
     * @return the If-Match header value, otherwise {@code null} if not present.
     */
    public Set<MatchingEntityTag> getIfMatch() {
        final String ifMatch = getHeaderString(HttpHeaders.IF_MATCH);
        if (ifMatch == null || ifMatch.length() == 0) {
            return null;
        }
        try {
            return HttpHeaderReader.readMatchingEntityTag(ifMatch);
        } catch (java.text.ParseException e) {
            throw exception(HttpHeaders.IF_MATCH, ifMatch, e);
        }
    }

    /**
     * Get If-None-Match header.
     *
     * @return the If-None-Match header value, otherwise {@code null} if not present.
     */
    public Set<MatchingEntityTag> getIfNoneMatch() {
        final String ifNoneMatch = getHeaderString(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch == null || ifNoneMatch.length() == 0) {
            return null;
        }
        try {
            return HttpHeaderReader.readMatchingEntityTag(ifNoneMatch);
        } catch (java.text.ParseException e) {
            throw exception(HttpHeaders.IF_NONE_MATCH, ifNoneMatch, e);
        }
    }

    /**
     * Get the language of the entity.
     *
     * @return the language of the entity or {@code null} if not specified
     */
    public Locale getLanguage() {
        return singleHeader(HttpHeaders.CONTENT_LANGUAGE, new Function<String, Locale>() {
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
     * Get Content-Length value.
     *
     * @return Content-Length as integer if present and valid number. In other
     *         cases returns -1.
     */
    public int getLength() {
        return singleHeader(HttpHeaders.CONTENT_LENGTH, new Function<String, Integer>() {
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
     * Get the media type of the entity.
     *
     * @return the media type or {@code null} if not specified (e.g. there's no
     *         response entity).
     */
    public MediaType getMediaType() {
        MediaType type = singleHeader(HttpHeaders.CONTENT_TYPE, new Function<String, MediaType>() {
            @Override
            public MediaType apply(String input) {
                return MediaType.valueOf(input);
            }
        });

        return (type == null) ? MediaType.APPLICATION_OCTET_STREAM_TYPE : type;
    }

    /**
     * Get a list of media types that are acceptable for the response.
     *
     * @return a read-only list of requested response media types sorted according
     *         to their q-value, with highest preference first.
     */
    public List<AcceptableMediaType> getQualifiedAcceptableMediaTypes() {
        final String value = getHeaderString(HttpHeaders.ACCEPT);

        if (value == null || value.length() == 0) {
            return Collections.unmodifiableList(MediaTypes.GENERAL_ACCEPT_MEDIA_TYPE_LIST);
        }

        try {
            return Collections.unmodifiableList(HttpHeaderReader.readAcceptMediaType(value));
        } catch (ParseException e) {
            throw exception(HttpHeaders.ACCEPT, value, e);
        }
    }

    /**
     * Get a list of languages that are acceptable for the response.
     *
     * @return a read-only list of acceptable languages sorted according
     *         to their q-value, with highest preference first.
     */
    public List<AcceptableLanguageTag> getQualifiedAcceptableLanguages() {
        final String value = getHeaderString(HttpHeaders.ACCEPT_LANGUAGE);

        if (value == null || value.length() == 0) {
            return Collections.singletonList(new AcceptableLanguageTag("*", null));
        }

        try {
            return Collections.unmodifiableList(HttpHeaderReader.readAcceptLanguage(value));
        } catch (ParseException e) {
            throw exception(HttpHeaders.ACCEPT_LANGUAGE, value, e);
        }
    }

    /**
     * Get the list of language tag from the "Accept-Charset" of an HTTP request.
     *
     * @return The list of AcceptableToken. This list
     *         is ordered with the highest quality acceptable charset occurring first.
     */
    public List<AcceptableToken> getQualifiedAcceptCharset() {
        final String acceptCharset = getHeaderString(HttpHeaders.ACCEPT_CHARSET);
        try {
            if (acceptCharset == null || acceptCharset.length() == 0) {
                return Collections.singletonList(new AcceptableToken("*"));
            }
            return HttpHeaderReader.readAcceptToken(acceptCharset);
        } catch (java.text.ParseException e) {
            throw exception(HttpHeaders.ACCEPT_CHARSET, acceptCharset, e);
        }
    }

    /**
     * Get the list of language tag from the "Accept-Charset" of an HTTP request.
     *
     * @return The list of AcceptableToken. This list
     *         is ordered with the highest quality acceptable charset occurring first.
     */
    public List<AcceptableToken> getQualifiedAcceptEncoding() {
        final String acceptEncoding = getHeaderString(HttpHeaders.ACCEPT_ENCODING);
        try {
            if (acceptEncoding == null || acceptEncoding.length() == 0) {
                return Collections.singletonList(new AcceptableToken("*"));
            }
            return HttpHeaderReader.readAcceptToken(acceptEncoding);
        } catch (java.text.ParseException e) {
            throw exception("Accept-Encoding", acceptEncoding, e);
        }
    }

    /**
     * Get any cookies that accompanied the request.
     *
     * @return a read-only map of cookie name (String) to {@link javax.ws.rs.core.Cookie}.
     */
    public Map<String, Cookie> getRequestCookies() {
        List<String> cookies = this.headers.get(HttpHeaders.COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Cookie> result = new HashMap<String, Cookie>();
        for (String cookie : cookies) {
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
     * Get any new cookies set on the response message.
     *
     * @return a read-only map of cookie name (String) to a {@link javax.ws.rs.core.NewCookie new cookie}.
     */
    public Map<String, NewCookie> getResponseCookies() {
        List<String> cookies = this.headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, NewCookie> result = new HashMap<String, NewCookie>();
        for (String cookie : cookies) {
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
        return singleHeader(HttpHeaders.ETAG, new Function<String, EntityTag>() {
            @Override
            public EntityTag apply(String value) {
                return value == null ? null : EntityTag.valueOf(value);
            }
        });
    }

    /**
     * Get the last modified date.
     *
     * @return the last modified date, otherwise {@code null} if not present.
     */
    public Date getLastModified() {
        return singleHeader(HttpHeaders.LAST_MODIFIED, new Function<String, Date>() {
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
        return singleHeader(HttpHeaders.LOCATION, new Function<String, URI>() {
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
        List<String> links = this.headers.get(HttpHeaders.LINK);
        if (links == null || links.isEmpty()) {
            return Collections.emptySet();
        }

        try {
            Set<Link> result = new HashSet<Link>(links.size());
            for (String l : links) {
                result.add(Link.valueOf(l));
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw exception(HttpHeaders.LINK, links, e);
        }
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
     * Get context message body workers.
     *
     * @return context message body workers.
     */
    public MessageBodyWorkers getWorkers() {
        return workers;
    }

    /**
     * Set context message body workers.
     *
     * @param workers context message body workers.
     */
    public void setWorkers(MessageBodyWorkers workers) {
        this.workers = workers;
    }

    /**
     * Check if there is a non-empty entity input stream is available in the response
     * message.
     *
     * The method returns {@code true} if the entity is present, returns
     * {@code false} otherwise.
     *
     * @return {@code true} if there is an entity present in the message,
     *         {@code false} otherwise.
     */
    public boolean hasEntity() {
        return !contentStream.isEmpty();
    }

    /**
     * Get the entity input stream.
     *
     * @return entity input stream.
     */
    public InputStream getEntityStream() {
        return contentStream.getInputStream();
    }

    /**
     * Set a new entity input stream.
     *
     * @param input new entity input stream.
     */
    public void setEntityStream(InputStream input) {
        this.contentStream.setExternalContentStream(input);
    }

    /**
     * Read entity from a context entity input stream.
     *
     * @param <T>         entity Java object type.
     * @param rawType     raw Java entity type.
     * @param propertiesDelegate TODO: add javadoc
     * @return entity read from a context entity input stream.
     */
    public <T> T readEntity(Class<T> rawType, PropertiesDelegate propertiesDelegate) {
        return readEntity(rawType, rawType, EMPTY_ANNOTATIONS, propertiesDelegate);
    }


    /**
     * Read entity from a context entity input stream.
     *
     * @param <T>         entity Java object type.
     * @param rawType     raw Java entity type.
     * @param annotations entity annotations.
     * @return entity read from a context entity input stream.
     */
    public <T> T readEntity(Class<T> rawType, Annotation[] annotations, PropertiesDelegate propertiesDelegate) {
        return readEntity(rawType, rawType, annotations, propertiesDelegate);
    }

    /**
     * Read entity from a context entity input stream.
     *
     * @param <T>         entity Java object type.
     * @param rawType     raw Java entity type.
     * @param type        generic Java entity type.
     * @return entity read from a context entity input stream.
     */
    public <T> T readEntity(Class<T> rawType, Type type, PropertiesDelegate propertiesDelegate) {
        return readEntity(rawType, type, EMPTY_ANNOTATIONS, propertiesDelegate);
    }

    /**
     * Read entity from a context entity input stream.
     *
     * @param <T>         entity Java object type.
     * @param rawType     raw Java entity type.
     * @param type        generic Java entity type.
     * @param annotations entity annotations.
     * @return entity read from a context entity input stream.
     */
    @SuppressWarnings("unchecked")
    public <T> T readEntity(Class<T> rawType, Type type, Annotation[] annotations, PropertiesDelegate propertiesDelegate) {
        if (contentStream.isEmpty()) {
            return null;
        }

        if (workers == null) {
            return null;
        }

        try {
            T t = (T) workers.readFrom(
                    rawType,
                    type,
                    annotations,
                    getMediaType(),
                    headers,
                    propertiesDelegate,
                    contentStream.getInputStream(),
                    contentStream.getType().intercept());

            if (contentStream.getType() == ContentStream.Type.BUFFERED
                    || contentStream.getType() == ContentStream.Type.EXTERNAL_BUFFERED) {
                contentStream.getInputStream().reset();
            } else if (!(t instanceof Closeable) && !(t instanceof Source)) {
                contentStream.invalidateContentStream();
            }
            return t;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error reading entity from input stream", ex);
        }

        return null;
    }

    /**
     * Buffer the entity stream (if not empty).
     *
     * @throws MessageProcessingException in case of an IO error.
     */
    public boolean bufferEntity() throws MessageProcessingException {
        try {
            if (contentStream.getType() == ContentStream.Type.BUFFERED
                    || contentStream.getType() == ContentStream.Type.EXTERNAL_BUFFERED) {
                return true;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ReaderWriter.writeTo(contentStream.getInputStream(), baos);
            } finally {
                contentStream.invalidateContentStream();
            }

            contentStream.setBufferedContentStream(new ByteArrayInputStream(baos.toByteArray()));

            return true;
        } catch (IOException ex) {
            throw new MessageProcessingException(LocalizationMessages.MESSAGE_CONTENT_BUFFERING_FAILED(), ex);
        }
    }

}
