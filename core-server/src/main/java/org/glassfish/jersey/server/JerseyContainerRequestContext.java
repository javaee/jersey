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
package org.glassfish.jersey.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.AcceptableLanguageTag;
import org.glassfish.jersey.message.internal.AcceptableMediaType;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.glassfish.jersey.message.internal.InboundMessageContext;
import org.glassfish.jersey.message.internal.MatchingEntityTag;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.message.internal.VariantSelector;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;
import org.glassfish.jersey.uri.UriComponent;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Jersey container request context.
 *
 * An instance of the request context is passed by the container to the
 * {@link ApplicationHandler} for each incoming client request.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyContainerRequestContext extends InboundMessageContext
        implements ContainerRequestContext, Request, HttpHeaders {

    private static URI DEFAULT_BASE_URI = URI.create("/");

    private static URI normalizeBaseUri(URI baseUri) {
        return baseUri.normalize();
    }

    // Request-scoped properties delegate
    private final PropertiesDelegate propertiesDelegate;
    // Absolute application root URI (base URI)
    private URI baseUri;
    // Absolute request URI
    private URI requestUri;
    // Lazily computed encoded request path (relative to application root URI)
    private String encodedRelativePath = null;
    // Lazily computed decoded request path (relative to application root URI)
    private String decodedRelativePath = null;
    // Request method
    private String httpMethod;
    // Request security context
    private SecurityContext securityContext;
    // Request filter chain execution aborting response
    private Response abortResponse;
    // Vary header value to be set in the response
    private String varyValue;
    // UriInfo reference
    private UriInfo uriInfo;
    // Entity providers
    private MessageBodyWorkers workers;
    // Custom Jersey container request scoped initializer
    private RequestScopedInitializer requestScopedInitializer;
    // Request-scoped response writer of the invoking container
    private ContainerResponseWriter responseWriter;


    /**
     * Create new Jersey container request context.
     *
     * @param baseUri            base application URI.
     * @param requestUri         request URI.
     * @param httpMethod         request HTTP method name.
     * @param securityContext    security context of the current request. Must not be {@code null}.
     *                           The {@link SecurityContext#getUserPrincipal()} must return
     *                           {@code null} if the current request has not been authenticated
     *                           by the container.
     * @param propertiesDelegate custom {@link PropertiesDelegate properties delegate}
     *                           to be used by the context.
     */
    public JerseyContainerRequestContext(
            URI baseUri,
            URI requestUri,
            String httpMethod,
            SecurityContext securityContext,
            PropertiesDelegate propertiesDelegate) {

        this.baseUri = baseUri == null ? DEFAULT_BASE_URI : normalizeBaseUri(baseUri);
        this.requestUri = requestUri;
        this.httpMethod = httpMethod;
        this.securityContext = securityContext;
        this.propertiesDelegate = propertiesDelegate;
    }

    /**
     * Get a custom container extensions initializer for the current request.
     *
     * The initializer is guaranteed to be run from within the request scope of
     * the current request.
     *
     * @return custom container extensions initializer or {@code null} if not
     *     available.
     */
    public RequestScopedInitializer getRequestScopedInitializer() {
        return requestScopedInitializer;
    }

    /**
     * Set a custom container extensions initializer for the current request.
     *
     * The initializer is guaranteed to be run from within the request scope of
     * the current request.
     *
     * @param requestScopedInitializer custom container extensions initializer.
     */
    public void setRequestScopedInitializer(RequestScopedInitializer requestScopedInitializer) {
        this.requestScopedInitializer = requestScopedInitializer;
    }

    /**
     * Get the container response writer for the current request.
     *
     * @return container response writer.
     */
    public ContainerResponseWriter getResponseWriter() {
        return responseWriter;
    }

    /**
     * Set the container response writer for the current request.
     *
     * @param responseWriter container response writer. Must not be {@code null}.
     */
    public void setWriter(ContainerResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    /**
     * Read entity from a context entity input stream.
     *
     * @param <T>         entity Java object type.
     * @param rawType     raw Java entity type.
     * @return entity read from a context entity input stream.
     */
    public <T> T readEntity(Class<T> rawType) {
        return readEntity(rawType, propertiesDelegate);
    }

    /**
     * Read entity from a context entity input stream.
     *
     * @param <T>         entity Java object type.
     * @param rawType     raw Java entity type.
     * @param annotations entity annotations.
     * @return entity read from a context entity input stream.
     */
    public <T> T readEntity(Class<T> rawType, Annotation[] annotations) {
        return super.readEntity(rawType, annotations, propertiesDelegate);
    }

    /**
     * Read entity from a context entity input stream.
     *
     * @param <T>     entity Java object type.
     * @param rawType raw Java entity type.
     * @param type    generic Java entity type.
     * @return entity read from a context entity input stream.
     */
    public <T> T readEntity(Class<T> rawType, Type type) {
        return super.readEntity(rawType, type, propertiesDelegate);
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
    public <T> T readEntity(Class<T> rawType, Type type, Annotation[] annotations) {
        return super.readEntity(rawType, type, annotations, propertiesDelegate);
    }

    @Override
    public Object getProperty(String name) {
        return propertiesDelegate.getProperty(name);
    }

    @Override
    public Enumeration<String> getPropertyNames() {
        return propertiesDelegate.getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object object) {
        propertiesDelegate.setProperty(name, object);
    }

    @Override
    public void removeProperty(String name) {
        propertiesDelegate.removeProperty(name);
    }

    /**
     * Get the underlying properties delegate.
     *
     * @return underlying properties delegate.
     */
    PropertiesDelegate getPropertiesDelegate() {
        return propertiesDelegate;
    }

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public void setUriInfo(UriInfo uriInfo) {
        // TODO use the initializer
        this.uriInfo = uriInfo;
    }

    /**
     * Get base request URI.
     *
     * @return base request URI.
     */
    public URI getBaseUri() {
        return baseUri;
    }

    /**
     * Get request URI.
     *
     * @return request URI.
     */
    public URI getRequestUri() {
        return requestUri;
    }

    @Override
    public void setRequestUri(URI requestUri) throws IllegalStateException {
        this.requestUri = requestUri;
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException {
        this.baseUri = baseUri;
        this.requestUri = requestUri;
    }

    /**
     * Get the path of the current request relative to the application root (base)
     * URI as a string.
     *
     * @param decode controls whether sequences of escaped octets are decoded
     *               ({@code true}) or not ({@code false}).
     * @return relative request path.
     */
    public String getPath(boolean decode) {
        if (decode) {
            if (decodedRelativePath != null) {
                return decodedRelativePath;
            }

            return decodedRelativePath = UriComponent.decode(encodedRelativePath(), UriComponent.Type.PATH);
        } else {
            return encodedRelativePath();
        }
    }

    private String encodedRelativePath() {
        if (encodedRelativePath != null) {
            return encodedRelativePath;
        }

        String result;
        final String requestUriRawPath = requestUri.getRawPath();
        if (baseUri == null) {
            result = requestUriRawPath;
        } else {
            final String applicationRootUriRawPath = baseUri.getRawPath();
            if (applicationRootUriRawPath.length() > requestUriRawPath.length()) {
                result = "";
            } else {
                result = requestUriRawPath.substring(applicationRootUriRawPath.length());
            }
        }

        if (result.isEmpty()) {
            result = "/";
        }

        return encodedRelativePath = (result.charAt(0) == '/') ? result : '/' + result;
    }

    @Override
    public String getMethod() {
        return httpMethod;
    }

    @Override
    public void setMethod(String method) throws IllegalStateException {
        this.httpMethod = method;
    }

    @Override
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        this.securityContext = context;
    }

    @Override
    public Request getRequest() {
        return this;
    }

    @Override
    public void abortWith(Response response) {
        this.abortResponse = response;
    }

    /**
     * Get the request filter chain aborting response if set, or {@code null} otherwise.
     *
     * @return request filter chain aborting response if set, or {@code null} otherwise.
     */
    public Response getAbortResponse() {
        return abortResponse;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return super.getRequestCookies();
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return Lists.transform(getQualifiedAcceptableMediaTypes(), new Function<AcceptableMediaType, MediaType>() {
            @Override
            public MediaType apply(AcceptableMediaType input) {
                return input;
            }
        });
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return Lists.transform(getQualifiedAcceptableLanguages(), new Function<AcceptableLanguageTag, Locale>() {

            @Override
            public Locale apply(AcceptableLanguageTag input) {
                return input.getAsLocale();
            }
        });
    }

    /**
     * Get the message body workers associated with the request.
     *
     * @return message body workers.
     */
    public MessageBodyWorkers getWorkers() {
        return workers;
    }

    /**
     * Set the message body workers associated with the request.
     *
     * @param workers message body workers.
     */
    public void setWorkers(MessageBodyWorkers workers) {
        this.workers = workers;
    }

    // JAX-RS request

    @Override
    public Variant selectVariant(List<Variant> variants) throws IllegalArgumentException {
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("The list of variants is null or empty");
        }
        Ref<String> varyValueRef = Refs.emptyRef();
        final Variant variant = VariantSelector.selectVariant(this, variants, varyValueRef);
        this.varyValue = varyValueRef.get();
        return variant;
    }

    /**
     * Get the value of HTTP Vary response header to be set in the response,
     * or {@code null} if no value is to be set.
     *
     * @return value of HTTP Vary response header to be set in the response if available,
     *         {@code null} otherwise.
     */
    public String getVaryValue() {
        // TODO add Vary header to response headers
        return varyValue;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(EntityTag eTag) {
        if (eTag == null) {
            throw new IllegalArgumentException("Parameter 'eTag' cannot be null.");
        }

        Response.ResponseBuilder r = evaluateIfMatch(eTag);
        if (r != null) {
            return r;
        }
        return evaluateIfNoneMatch(eTag);
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(Date lastModified) {
        if (lastModified == null) {
            throw new IllegalArgumentException("Parameter 'lastModified' cannot be null.");
        }

        final long lastModifiedTime = lastModified.getTime();
        Response.ResponseBuilder r = evaluateIfUnmodifiedSince(lastModifiedTime);
        if (r != null) {
            return r;
        }
        return evaluateIfModifiedSince(lastModifiedTime);
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
        if (lastModified == null || eTag == null) {
            throw new IllegalArgumentException("Parameters 'lastModified' and 'eTag' cannot be null.");
        }

        Response.ResponseBuilder r = evaluateIfMatch(eTag);
        if (r != null) {
            return r;
        }

        final long lastModifiedTime = lastModified.getTime();
        r = evaluateIfUnmodifiedSince(lastModifiedTime);
        if (r != null) {
            return r;
        }

        final boolean isGetOrHead = getMethod().equals("GET") || getMethod().equals("HEAD");
        final Set<MatchingEntityTag> matchingTags = getIfNoneMatch();
        if (matchingTags != null) {
            r = evaluateIfNoneMatch(eTag, matchingTags, isGetOrHead);
            // If the If-None-Match header is present and there is no
            // match then the If-Modified-Since header must be ignored
            if (r == null) {
                return r;
            }

            // Otherwise if the If-None-Match header is present and there
            // is a match then the If-Modified-Since header must be checked
            // for consistency
        }

        final String ifModifiedSinceHeader = getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSinceHeader != null && ifModifiedSinceHeader.length() > 0 && isGetOrHead) {
            r = evaluateIfModifiedSince(lastModifiedTime, ifModifiedSinceHeader);
            if (r != null) {
                r.tag(eTag);
            }
        }

        return r;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions() {
        Set<MatchingEntityTag> matchingTags = getIfMatch();
        if (matchingTags == null) {
            return null;
        }

        // Since the resource does not exist the method must not be
        // perform and 412 Precondition Failed is returned
        return Responses.responseBuilder(Response.Status.PRECONDITION_FAILED, workers);
    }

    // Private methods
    private Response.ResponseBuilder evaluateIfMatch(EntityTag eTag) {
        Set<? super MatchingEntityTag> matchingTags = getIfMatch();
        if (matchingTags == null) {
            return null;
        }

        // The strong comparison function must be used to compare the entity
        // tags. Thus if the entity tag of the entity is weak then matching
        // of entity tags in the If-Match header should fail.
        if (eTag.isWeak()) {
            return Responses.responseBuilder(Response.Status.PRECONDITION_FAILED, workers);
        }

        if (matchingTags != MatchingEntityTag.ANY_MATCH && !matchingTags.contains(eTag)) {
            // 412 Precondition Failed
            return Responses.responseBuilder(Response.Status.PRECONDITION_FAILED, workers);
        }

        return null;
    }

    private Response.ResponseBuilder evaluateIfNoneMatch(EntityTag eTag) {
        Set<MatchingEntityTag> matchingTags = getIfNoneMatch();
        if (matchingTags == null) {
            return null;
        }

        final String httpMethod = getMethod();
        return evaluateIfNoneMatch(eTag, matchingTags, httpMethod.equals("GET") || httpMethod.equals("HEAD"));
    }

    private Response.ResponseBuilder evaluateIfNoneMatch(EntityTag eTag, Set<? super MatchingEntityTag> matchingTags,
                                                         boolean isGetOrHead) {
        if (isGetOrHead) {
            if (matchingTags == MatchingEntityTag.ANY_MATCH) {
                // 304 Not modified
                final Response.ResponseBuilder responseBuilder = Responses.responseBuilder(Response.Status.NOT_MODIFIED, workers);
                responseBuilder.tag(eTag);
                return responseBuilder;
            }

            // The weak comparison function may be used to compare entity tags
            if (matchingTags.contains(eTag) || matchingTags.contains(new EntityTag(eTag.getValue(), !eTag.isWeak()))) {
                // 304 Not modified
                final Response.ResponseBuilder responseBuilder = Responses.responseBuilder(Response.Status.NOT_MODIFIED, workers);
                responseBuilder.tag(eTag);
                return responseBuilder;
            }
        } else {
            // The strong comparison function must be used to compare the entity
            // tags. Thus if the entity tag of the entity is weak then matching
            // of entity tags in the If-None-Match header should fail if the
            // HTTP method is not GET or not HEAD.
            if (eTag.isWeak()) {
                return null;
            }

            if (matchingTags == MatchingEntityTag.ANY_MATCH || matchingTags.contains(eTag)) {
                // 412 Precondition Failed
                return Responses.responseBuilder(Response.Status.PRECONDITION_FAILED, workers);
            }
        }

        return null;
    }

    private Response.ResponseBuilder evaluateIfUnmodifiedSince(long lastModified) {
        String ifUnmodifiedSinceHeader = getHeaderString(HttpHeaders.IF_UNMODIFIED_SINCE);
        if (ifUnmodifiedSinceHeader != null && ifUnmodifiedSinceHeader.length() > 0) {
            try {
                long ifUnmodifiedSince = HttpHeaderReader.readDate(ifUnmodifiedSinceHeader).getTime();
                if (roundDown(lastModified) > ifUnmodifiedSince) {
                    // 412 Precondition Failed
                    return Responses.responseBuilder(Response.Status.PRECONDITION_FAILED, workers);
                }
            } catch (ParseException ex) {
                // Ignore the header if parsing error
            }
        }

        return null;
    }

    private Response.ResponseBuilder evaluateIfModifiedSince(long lastModified) {
        String ifModifiedSinceHeader = getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSinceHeader == null || ifModifiedSinceHeader.length() == 0) {
            return null;
        }

        final String httpMethod = getMethod();
        if (httpMethod.equals("GET") || httpMethod.equals("HEAD")) {
            return evaluateIfModifiedSince(lastModified, ifModifiedSinceHeader);
        } else {
            return null;
        }
    }

    private Response.ResponseBuilder evaluateIfModifiedSince(long lastModified, String ifModifiedSinceHeader) {
        try {
            long ifModifiedSince = HttpHeaderReader.readDate(ifModifiedSinceHeader).getTime();
            if (roundDown(lastModified) <= ifModifiedSince) {
                // 304 Not modified
                return Responses.responseBuilder(Response.Status.NOT_MODIFIED, workers);
            }
        } catch (ParseException ex) {
            // Ignore the header if parsing error
        }

        return null;
    }

    /**
     * Round down the time to the nearest second.
     *
     * @param time the time to round down.
     * @return the rounded down time.
     */
    private static long roundDown(long time) {
        return time - time % 1000;
    }


    /**
     * Get the values of a HTTP request header. The returned List is read-only.
     * This is a shortcut for {@code getRequestHeaders().get(name)}.
     *
     * @param name the header name, case insensitive.
     * @return a read-only list of header values.
     * @throws IllegalStateException if called outside the scope of a request.
     */
    @Override
    public List<String> getRequestHeader(String name) {
        return getHeaders().get(name);
    }

    /**
     * Get the values of HTTP request headers. The returned Map is case-insensitive
     * wrt. keys and is read-only. The method never returns {@code null}.
     *
     * @return a read-only map of header names and values.
     * @throws IllegalStateException if called outside the scope of a request.
     */
    @Override
    public MultivaluedMap<String, String> getRequestHeaders() {
        return getHeaders();
    }
}
