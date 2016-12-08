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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.NioErrorHandler;
import javax.ws.rs.core.NioWriterHandler;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.internal.LocalizationMessages;

import jersey.repackaged.com.google.common.base.MoreObjects;

/**
 * An outbound JAX-RS response message.
 *
 * The implementation delegates method calls to an {@link #getContext() underlying
 * outbound message context}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class OutboundJaxrsResponse extends javax.ws.rs.core.Response {

    private final OutboundMessageContext context;
    private final StatusType status;

    private boolean closed = false;
    private boolean buffered = false;

    /**
     * Get an OutboundJaxrsResponse instance for a given JAX-RS response.
     *
     * @param response response instance to from.
     * @return corresponding {@code OutboundJaxrsResponse} instance.
     */
    public static OutboundJaxrsResponse from(javax.ws.rs.core.Response response) {
        if (response instanceof OutboundJaxrsResponse) {
            return (OutboundJaxrsResponse) response;
        } else {
            final StatusType status = response.getStatusInfo();
            final OutboundMessageContext context = new OutboundMessageContext();
            context.getHeaders().putAll(response.getMetadata());
            context.setEntity(response.getEntity());
            return new OutboundJaxrsResponse(status, context);
        }
    }

    /**
     * Create new outbound JAX-RS response message instance.
     *
     * @param status  response status.
     * @param context underlying outbound message context.
     */
    public OutboundJaxrsResponse(StatusType status, OutboundMessageContext context) {
        this.status = status;
        this.context = context;
    }

    /**
     * Get the underlying outbound message context.
     *
     * @return underlying outbound message context.
     */
    public OutboundMessageContext getContext() {
        return context;
    }

    @Override
    public int getStatus() {
        return status.getStatusCode();
    }

    @Override
    public StatusType getStatusInfo() {
        return status;
    }

    @Override
    public Object getEntity() {
        if (closed) {
            throw new IllegalStateException(LocalizationMessages.RESPONSE_CLOSED());
        }
        return context.getEntity();
    }

    @Override
    public <T> T readEntity(Class<T> type) throws ProcessingException {
        throw new IllegalStateException(LocalizationMessages.NOT_SUPPORTED_ON_OUTBOUND_MESSAGE());
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) throws ProcessingException {
        throw new IllegalStateException(LocalizationMessages.NOT_SUPPORTED_ON_OUTBOUND_MESSAGE());
    }

    @Override
    public <T> T readEntity(Class<T> type, Annotation[] annotations) throws ProcessingException {
        throw new IllegalStateException(LocalizationMessages.NOT_SUPPORTED_ON_OUTBOUND_MESSAGE());
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) throws ProcessingException {
        throw new IllegalStateException(LocalizationMessages.NOT_SUPPORTED_ON_OUTBOUND_MESSAGE());
    }

    @Override
    public boolean hasEntity() {
        if (closed) {
            throw new IllegalStateException(LocalizationMessages.RESPONSE_CLOSED());
        }
        return context.hasEntity();
    }

    @Override
    public boolean bufferEntity() throws ProcessingException {
        if (closed) {
            throw new IllegalStateException(LocalizationMessages.RESPONSE_CLOSED());
        }

        if (!context.hasEntity() || !InputStream.class.isAssignableFrom(context.getEntityClass())) {
            return false;
        }

        if (buffered) {
            // already buffered
            return true;
        }
        final InputStream in = InputStream.class.cast(context.getEntity());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } catch (IOException ex) {
            throw new ProcessingException(ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                throw new ProcessingException(ex);
            }
        }

        context.setEntity(new ByteArrayInputStream(out.toByteArray()));
        buffered = true;
        return true;
    }

    @Override
    public void close() throws ProcessingException {
        closed = true;
        context.close();
        if (buffered) {
            // release buffer
            context.setEntity(null);
        } else if (context.hasEntity() && InputStream.class.isAssignableFrom(context.getEntityClass())) {
            try {
                InputStream.class.cast(context.getEntity()).close();
            } catch (IOException ex) {
                throw new ProcessingException(ex);
            }
        }
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return context.getStringHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return context.getHeaderString(name);
    }

    @Override
    public MediaType getMediaType() {
        return context.getMediaType();
    }

    @Override
    public Locale getLanguage() {
        return context.getLanguage();
    }

    @Override
    public int getLength() {
        return context.getLength();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return context.getResponseCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return context.getEntityTag();
    }

    @Override
    public Date getDate() {
        return context.getDate();
    }

    @Override
    public Date getLastModified() {
        return context.getLastModified();
    }

    @Override
    public Set<String> getAllowedMethods() {
        return context.getAllowedMethods();
    }

    @Override
    public URI getLocation() {
        return context.getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return context.getLinks();
    }

    @Override
    public boolean hasLink(String relation) {
        return context.hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return context.getLink(relation);
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return context.getLinkBuilder(relation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MultivaluedMap<String, Object> getMetadata() {
        return context.getHeaders();
    }

    @Override
    public String toString() {
        return MoreObjects
                .toStringHelper(this)
                .add("status", status.getStatusCode())
                .add("reason", status.getReasonPhrase())
                .add("hasEntity", context.hasEntity())
                .add("closed", closed)
                .add("buffered", buffered)
                .toString();
    }

    /**
     * Outbound JAX-RS {@code Response.ResponseBuilder} implementation.
     *
     * The implementation delegates method calls to an {@link #getContext() underlying
     * outbound message context}. Upon a call to a {@link #build()} method
     * a new instance of {@link OutboundJaxrsResponse} is produced.
     */
    public static class Builder extends ResponseBuilder {

        private StatusType status;
        private final OutboundMessageContext context;

        /* thread-local storage for request baseUri for use in the response headers */
        private static final InheritableThreadLocal<URI> baseUriThreadLocal = new InheritableThreadLocal<URI>();

        /**
         * Set the {@code baseUri} of the actual request into the {@link InheritableThreadLocal}.
         * <p>
         * The {@code baseUri} will be used for absolutizing the location header
         * content in case that only a relative URI is provided.
         * </p>
         * <p>
         * After resource method invocation when the value is not needed
         * any more to be stored in {@code ThreadLocal} {@link #clearBaseUri() clearBaseUri()} should be
         * called for cleanup in order to prevent possible memory leaks.
         * </p>
         *
         * @param baseUri - baseUri of the actual request
         * @see #location(java.net.URI)
         * @since 2.4
         */
        public static void setBaseUri(URI baseUri) {
            baseUriThreadLocal.set(baseUri);
        }

        /**
         * Return request baseUri previously set by {@link #setBaseUri(java.net.URI)}.
         *
         * Returned {@link URI} is used for absolutization of the location header in case that only a relative
         * {@code URI} was provided.
         *
         * @return baseUri of the actual request
         * @see #location(java.net.URI)
         * @since 2.4
         */
        private static URI getBaseUri() {
            return baseUriThreadLocal.get();
        }

        /**
         * Remove the current thread's value for baseUri thread-local variable (set by {@link #setBaseUri(java.net.URI)}).
         *
         * Should be called after resource method invocation for cleanup.
         *
         * @see #location(java.net.URI)
         * @since 2.4
         */
        public static void clearBaseUri() {
            baseUriThreadLocal.remove();
        }

        /**
         * Create new outbound JAX-RS response builder.
         *
         * @param context underlying outbound message context.
         */
        public Builder(final OutboundMessageContext context) {
            this.context = context;
        }

        @Override
        public javax.ws.rs.core.Response build() {
            StatusType st = status;
            if (st == null) {
                st = context.hasEntity() ? Status.OK : Status.NO_CONTENT;
            }
            return new OutboundJaxrsResponse(st, new OutboundMessageContext(context));
        }

        @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
        @Override
        public ResponseBuilder clone() {
            return new Builder(new OutboundMessageContext(context)).status(status);
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder status(StatusType status) {
            if (status == null) {
                throw new IllegalArgumentException("Response status must not be 'null'");
            }

            this.status = status;
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder status(int code) {
            this.status = Statuses.from(code);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder entity(Object entity) {
            context.setEntity(entity);
            return this;
        }

        @Override
        public ResponseBuilder entity(Object entity, Annotation[] annotations) {
            context.setEntity(entity, annotations);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder type(MediaType type) {
            context.setMediaType(type);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder type(String type) {
            return type(type == null ? null : MediaType.valueOf(type));
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder variant(Variant variant) {
            if (variant == null) {
                type((MediaType) null);
                language((String) null);
                encoding(null);
                return this;
            }

            type(variant.getMediaType());
            language(variant.getLanguage());
            encoding(variant.getEncoding());

            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder variants(List<Variant> variants) {
            if (variants == null) {
                header(HttpHeaders.VARY, null);
                return this;
            }

            if (variants.isEmpty()) {
                return this;
            }

            MediaType accept = variants.get(0).getMediaType();
            boolean vAccept = false;

            Locale acceptLanguage = variants.get(0).getLanguage();
            boolean vAcceptLanguage = false;

            String acceptEncoding = variants.get(0).getEncoding();
            boolean vAcceptEncoding = false;

            for (Variant v : variants) {
                vAccept |= !vAccept && vary(v.getMediaType(), accept);
                vAcceptLanguage |= !vAcceptLanguage && vary(v.getLanguage(), acceptLanguage);
                vAcceptEncoding |= !vAcceptEncoding && vary(v.getEncoding(), acceptEncoding);
            }

            StringBuilder vary = new StringBuilder();
            append(vary, vAccept, HttpHeaders.ACCEPT);
            append(vary, vAcceptLanguage, HttpHeaders.ACCEPT_LANGUAGE);
            append(vary, vAcceptEncoding, HttpHeaders.ACCEPT_ENCODING);

            if (vary.length() > 0) {
                header(HttpHeaders.VARY, vary.toString());
            }
            return this;
        }

        @Override
        public ResponseBuilder entity(NioWriterHandler writer) {
            // TODO JAX-RS 2.1: to be implemented
            throw new UnsupportedOperationException("TODO JAX-RS 2.1: to be implemented");
        }

        @Override
        public ResponseBuilder entity(NioWriterHandler writer, NioErrorHandler error) {
            // TODO JAX-RS 2.1: to be implemented
            throw new UnsupportedOperationException("TODO JAX-RS 2.1: to be implemented");
        }

        private boolean vary(MediaType v, MediaType vary) {
            return v != null && !v.equals(vary);
        }

        private boolean vary(Locale v, Locale vary) {
            return v != null && !v.equals(vary);
        }

        private boolean vary(String v, String vary) {
            return v != null && !v.equalsIgnoreCase(vary);
        }

        private void append(StringBuilder sb, boolean v, String s) {
            if (v) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(s);
            }
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder language(String language) {
            headerSingle(HttpHeaders.CONTENT_LANGUAGE, language);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder language(Locale language) {
            headerSingle(HttpHeaders.CONTENT_LANGUAGE, language);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder location(URI location) {
            URI locationUri = location;
            if (location != null && !location.isAbsolute()) {
                URI baseUri = getBaseUri();
                if (baseUri != null) {
                    locationUri = baseUri.resolve(location);
                }
            }
            headerSingle(HttpHeaders.LOCATION, locationUri);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder contentLocation(URI location) {
            headerSingle(HttpHeaders.CONTENT_LOCATION, location);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder encoding(String encoding) {
            headerSingle(HttpHeaders.CONTENT_ENCODING, encoding);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder tag(EntityTag tag) {
            headerSingle(HttpHeaders.ETAG, tag);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder tag(String tag) {
            return tag(tag == null ? null : new EntityTag(tag));
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder lastModified(Date lastModified) {
            headerSingle(HttpHeaders.LAST_MODIFIED, lastModified);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
            headerSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder expires(Date expires) {
            headerSingle(HttpHeaders.EXPIRES, expires);
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder cookie(NewCookie... cookies) {
            if (cookies != null) {
                for (NewCookie cookie : cookies) {
                    header(HttpHeaders.SET_COOKIE, cookie);
                }
            } else {
                header(HttpHeaders.SET_COOKIE, null);
            }
            return this;
        }

        @Override
        public javax.ws.rs.core.Response.ResponseBuilder header(String name, Object value) {
            return header(name, value, false);
        }

        private javax.ws.rs.core.Response.ResponseBuilder headerSingle(String name, Object value) {
            return header(name, value, true);
        }

        private javax.ws.rs.core.Response.ResponseBuilder header(String name, Object value, boolean single) {
            if (value != null) {
                if (single) {
                    context.getHeaders().putSingle(name, value);
                } else {
                    context.getHeaders().add(name, value);
                }
            } else {
                context.getHeaders().remove(name);
            }
            return this;
        }

        @Override
        public ResponseBuilder variants(Variant... variants) {
            return variants(Arrays.asList(variants));
        }

        @Override
        public ResponseBuilder links(Link... links) {
            if (links != null) {
                for (Link link : links) {
                    header(HttpHeaders.LINK, link);
                }
            } else {
                header(HttpHeaders.LINK, null);
            }
            return this;
        }

        @Override
        public ResponseBuilder link(URI uri, String rel) {
            header(HttpHeaders.LINK, Link.fromUri(uri).rel(rel).build());
            return this;
        }

        @Override
        public ResponseBuilder link(String uri, String rel) {
            header(HttpHeaders.LINK, Link.fromUri(uri).rel(rel).build());
            return this;
        }

        @Override
        public ResponseBuilder allow(String... methods) {
            if (methods == null || (methods.length == 1 && methods[0] == null)) {
                return allow((Set<String>) null);
            } else {
                return allow(new HashSet<String>(Arrays.asList(methods)));
            }
        }

        @Override
        public ResponseBuilder allow(Set<String> methods) {
            if (methods == null) {
                return header(HttpHeaders.ALLOW, null, true);
            }

            StringBuilder allow = new StringBuilder();
            for (String m : methods) {
                append(allow, true, m);
            }
            return header(HttpHeaders.ALLOW, allow, true);
        }

        @Override
        public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
            context.replaceHeaders(headers);
            return this;
        }

    }
}
