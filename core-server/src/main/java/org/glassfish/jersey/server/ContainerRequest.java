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
package org.glassfish.jersey.server;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NioCompletionHandler;
import javax.ws.rs.core.NioErrorHandler;
import javax.ws.rs.core.NioReaderHandler;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.message.internal.AcceptableLanguageTag;
import org.glassfish.jersey.message.internal.AcceptableMediaType;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.glassfish.jersey.message.internal.InboundMessageContext;
import org.glassfish.jersey.message.internal.MatchingEntityTag;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.TracingAwarePropertiesDelegate;
import org.glassfish.jersey.message.internal.VariantSelector;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;
import org.glassfish.jersey.uri.UriComponent;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Jersey container request context.
 * <p/>
 * An instance of the request context is passed by the container to the
 * {@link ApplicationHandler} for each incoming client request.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ContainerRequest extends InboundMessageContext
        implements ContainerRequestContext, Request, HttpHeaders, PropertiesDelegate {

    private static final URI DEFAULT_BASE_URI = URI.create("/");

    // Request-scoped properties delegate
    private final PropertiesDelegate propertiesDelegate;
    // Routing context and UriInfo implementation
    private final UriRoutingContext uriRoutingContext;
    // Absolute application root URI (base URI)
    private URI baseUri;
    // Absolute request URI
    private URI requestUri;
    // Lazily computed encoded request path (relative to application root URI)
    private String encodedRelativePath = null;
    // Lazily computed decoded request path (relative to application root URI)
    private String decodedRelativePath = null;
    // Lazily computed "absolute path" URI
    private URI absolutePathUri = null;
    // Request method
    private String httpMethod;
    // Request security context
    private SecurityContext securityContext;
    // Request filter chain execution aborting response
    private Response abortResponse;
    // Vary header value to be set in the response
    private String varyValue;
    // Processing providers
    private ProcessingProviders processingProviders;
    // Custom Jersey container request scoped initializer
    private RequestScopedInitializer requestScopedInitializer;
    // Request-scoped response writer of the invoking container
    private ContainerResponseWriter responseWriter;
    // True if the request is used in the response processing phase (for example in ContainerResponseFilter)
    private boolean inResponseProcessingPhase;

    private static final String ERROR_REQUEST_SET_ENTITY_STREAM_IN_RESPONSE_PHASE =
            LocalizationMessages.ERROR_REQUEST_SET_ENTITY_STREAM_IN_RESPONSE_PHASE();
    private static final String ERROR_REQUEST_SET_SECURITY_CONTEXT_IN_RESPONSE_PHASE =
            LocalizationMessages.ERROR_REQUEST_SET_SECURITY_CONTEXT_IN_RESPONSE_PHASE();
    private static final String ERROR_REQUEST_ABORT_IN_RESPONSE_PHASE =
            LocalizationMessages.ERROR_REQUEST_ABORT_IN_RESPONSE_PHASE();
    private static final String METHOD_PARAMETER_CANNOT_BE_NULL_OR_EMPTY =
            LocalizationMessages.METHOD_PARAMETER_CANNOT_BE_NULL_OR_EMPTY("variants");
    private static final String METHOD_PARAMETER_CANNOT_BE_NULL_ETAG =
            LocalizationMessages.METHOD_PARAMETER_CANNOT_BE_NULL("eTag");
    private static final String METHOD_PARAMETER_CANNOT_BE_NULL_LAST_MODIFIED =
            LocalizationMessages.METHOD_PARAMETER_CANNOT_BE_NULL("lastModified");

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
    public ContainerRequest(
            final URI baseUri,
            final URI requestUri,
            final String httpMethod,
            final SecurityContext securityContext,
            final PropertiesDelegate propertiesDelegate) {
        super(true);

        this.baseUri = baseUri == null ? DEFAULT_BASE_URI : baseUri.normalize();
        this.requestUri = requestUri;
        this.httpMethod = httpMethod;
        this.securityContext = securityContext;
        this.propertiesDelegate = new TracingAwarePropertiesDelegate(propertiesDelegate);
        this.uriRoutingContext = new UriRoutingContext(this);
    }

    /**
     * Get a custom container extensions initializer for the current request.
     * <p/>
     * The initializer is guaranteed to be run from within the request scope of
     * the current request.
     *
     * @return custom container extensions initializer or {@code null} if not
     * available.
     */
    public RequestScopedInitializer getRequestScopedInitializer() {
        return requestScopedInitializer;
    }

    /**
     * Set a custom container extensions initializer for the current request.
     * <p/>
     * The initializer is guaranteed to be run from within the request scope of
     * the current request.
     *
     * @param requestScopedInitializer custom container extensions initializer.
     */
    public void setRequestScopedInitializer(final RequestScopedInitializer requestScopedInitializer) {
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
    public void setWriter(final ContainerResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    /**
     * Read entity from a context entity input stream.
     *
     * @param <T>     entity Java object type.
     * @param rawType raw Java entity type.
     * @return entity read from a context entity input stream.
     */
    public <T> T readEntity(final Class<T> rawType) {
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
    public <T> T readEntity(final Class<T> rawType, final Annotation[] annotations) {
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
    public <T> T readEntity(final Class<T> rawType, final Type type) {
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
    public <T> T readEntity(final Class<T> rawType, final Type type, final Annotation[] annotations) {
        return super.readEntity(rawType, type, annotations, propertiesDelegate);
    }

    @Override
    public Object getProperty(final String name) {
        return propertiesDelegate.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return propertiesDelegate.getPropertyNames();
    }

    @Override
    public void setProperty(final String name, final Object object) {
        propertiesDelegate.setProperty(name, object);
    }

    @Override
    public void removeProperty(final String name) {
        propertiesDelegate.removeProperty(name);
    }

    /**
     * Get the underlying properties delegate.
     *
     * @return underlying properties delegate.
     */
    public PropertiesDelegate getPropertiesDelegate() {
        return propertiesDelegate;
    }

    @Override
    public ExtendedUriInfo getUriInfo() {
        return uriRoutingContext;
    }

    void setProcessingProviders(final ProcessingProviders providers) {
        this.processingProviders = providers;
    }

    UriRoutingContext getUriRoutingContext() {
        return uriRoutingContext;
    }

    /**
     * Get all bound request filters applicable to this request.
     *
     * @return All bound (dynamically or by name) request filters applicable to the matched inflector (or an empty
     * collection if no inflector matched yet).
     */
    Iterable<RankedProvider<ContainerRequestFilter>> getRequestFilters() {
        final Inflector<RequestProcessingContext, ContainerResponse> inflector = getInflector();
        return emptyIfNull(inflector instanceof ResourceMethodInvoker
                ? ((ResourceMethodInvoker) inflector).getRequestFilters() : null);
    }

    /**
     * Get all bound response filters applicable to this request.
     * This is populated once the right resource method is matched.
     *
     * @return All bound (dynamically or by name) response filters applicable to the matched inflector (or an empty
     * collection if no inflector matched yet).
     */
    Iterable<RankedProvider<ContainerResponseFilter>> getResponseFilters() {
        final Inflector<RequestProcessingContext, ContainerResponse> inflector = getInflector();
        return emptyIfNull(inflector instanceof ResourceMethodInvoker
                ? ((ResourceMethodInvoker) inflector).getResponseFilters() : null);
    }

    /**
     * Get all reader interceptors applicable to this request.
     * This is populated once the right resource method is matched.
     *
     * @return All reader interceptors applicable to the matched inflector (or an empty
     * collection if no inflector matched yet).
     */
    @Override
    protected Iterable<ReaderInterceptor> getReaderInterceptors() {
        final Inflector<RequestProcessingContext, ContainerResponse> inflector = getInflector();
        return inflector instanceof ResourceMethodInvoker
                ? ((ResourceMethodInvoker) inflector).getReaderInterceptors()
                : processingProviders.getSortedGlobalReaderInterceptors();
    }

    @Override
    public void entity(NioReaderHandler reader) {
        // TODO JAX-RS 2.1: to be implemented
        throw new UnsupportedOperationException("TODO JAX-RS 2.1: to be implemented");
    }

    @Override
    public void entity(NioReaderHandler reader, NioCompletionHandler completion) {
        // TODO JAX-RS 2.1: to be implemented
        throw new UnsupportedOperationException("TODO JAX-RS 2.1: to be implemented");
    }

    @Override
    public void entity(NioReaderHandler reader, NioErrorHandler error) {
        // TODO JAX-RS 2.1: to be implemented
        throw new UnsupportedOperationException("TODO JAX-RS 2.1: to be implemented");
    }

    @Override
    public void entity(NioReaderHandler reader, NioCompletionHandler completion, NioErrorHandler error) {
        // TODO JAX-RS 2.1: to be implemented
        throw new UnsupportedOperationException("TODO JAX-RS 2.1: to be implemented");
    }

    /**
     * Get all writer interceptors applicable to this request.
     *
     * @return All writer interceptors applicable to the matched inflector (or an empty
     * collection if no inflector matched yet).
     */
    Iterable<WriterInterceptor> getWriterInterceptors() {
        final Inflector<RequestProcessingContext, ContainerResponse> inflector = getInflector();
        return inflector instanceof ResourceMethodInvoker
                ? ((ResourceMethodInvoker) inflector).getWriterInterceptors()
                : processingProviders.getSortedGlobalWriterInterceptors();
    }

    private Inflector<RequestProcessingContext, ContainerResponse> getInflector() {
        return uriRoutingContext.getEndpoint();
    }

    private static <T> Iterable<T> emptyIfNull(final Iterable<T> iterable) {
        return iterable == null ? Collections.<T>emptyList() : iterable;
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

    /**
     * Get the absolute path of the request. This includes everything preceding the path (host, port etc),
     * but excludes query parameters or fragment.
     *
     * @return the absolute path of the request.
     */
    public URI getAbsolutePath() {
        if (absolutePathUri != null) {
            return absolutePathUri;
        }

        return absolutePathUri = new JerseyUriBuilder().uri(requestUri).replaceQuery("").fragment("").build();
    }

    @Override
    public void setRequestUri(final URI requestUri) throws IllegalStateException {
        if (!uriRoutingContext.getMatchedURIs().isEmpty()) {
            throw new IllegalStateException("Method could be called only in pre-matching request filter.");
        }

        this.encodedRelativePath = null;
        this.decodedRelativePath = null;
        this.absolutePathUri = null;
        this.uriRoutingContext.invalidateUriComponentViews();

        this.requestUri = requestUri;
    }

    @Override
    public void setRequestUri(final URI baseUri, final URI requestUri) throws IllegalStateException {
        if (!uriRoutingContext.getMatchedURIs().isEmpty()) {
            throw new IllegalStateException("Method could be called only in pre-matching request filter.");
        }

        this.encodedRelativePath = null;
        this.decodedRelativePath = null;
        this.absolutePathUri = null;
        this.uriRoutingContext.invalidateUriComponentViews();

        this.baseUri = baseUri;
        this.requestUri = requestUri;
        OutboundJaxrsResponse.Builder.setBaseUri(baseUri);
    }

    /**
     * Get the path of the current request relative to the application root (base)
     * URI as a string.
     *
     * @param decode controls whether sequences of escaped octets are decoded
     *               ({@code true}) or not ({@code false}).
     * @return relative request path.
     */
    public String getPath(final boolean decode) {
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

        final String requestUriRawPath = requestUri.getRawPath();

        if (baseUri == null) {
            return encodedRelativePath = requestUriRawPath;
        }

        final int baseUriRawPathLength = baseUri.getRawPath().length();
        return encodedRelativePath = baseUriRawPathLength < requestUriRawPath.length()
                ? requestUriRawPath.substring(baseUriRawPathLength) : "";
    }

    @Override
    public String getMethod() {
        return httpMethod;
    }

    @Override
    public void setMethod(final String method) throws IllegalStateException {
        if (!uriRoutingContext.getMatchedURIs().isEmpty()) {
            throw new IllegalStateException("Method could be called only in pre-matching request filter.");
        }
        this.httpMethod = method;
    }

    /**
     * Like {@link #setMethod(String)} but does not throw {@link IllegalStateException} if the method is invoked in other than
     * pre-matching phase.
     *
     * @param method HTTP method.
     */
    public void setMethodWithoutException(final String method) {
        this.httpMethod = method;
    }

    @Override
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    public void setSecurityContext(final SecurityContext context) {
        Preconditions.checkState(!inResponseProcessingPhase, ERROR_REQUEST_SET_SECURITY_CONTEXT_IN_RESPONSE_PHASE);
        this.securityContext = context;
    }

    @Override
    public void setEntityStream(final InputStream input) {
        Preconditions.checkState(!inResponseProcessingPhase, ERROR_REQUEST_SET_ENTITY_STREAM_IN_RESPONSE_PHASE);
        super.setEntityStream(input);
    }

    @Override
    public Request getRequest() {
        return this;
    }

    @Override
    public void abortWith(final Response response) {
        Preconditions.checkState(!inResponseProcessingPhase, ERROR_REQUEST_ABORT_IN_RESPONSE_PHASE);
        this.abortResponse = response;
    }

    /**
     * Notify this request that the response created from this request is already being
     * processed. This means that the request processing phase has finished and this
     * request can be used only in the request processing phase (for example in
     * ContainerResponseFilter).
     * <p/>
     * The request can be used for processing of more than one response (in async cases).
     * Then this method should be called when the first response is created from this
     * request. Multiple calls to this method has the same effect as calling the method
     * only once.
     */
    public void inResponseProcessing() {
        this.inResponseProcessingPhase = true;
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
            public MediaType apply(final AcceptableMediaType input) {
                return input;
            }
        });
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return Lists.transform(getQualifiedAcceptableLanguages(), new Function<AcceptableLanguageTag, Locale>() {

            @Override
            public Locale apply(final AcceptableLanguageTag input) {
                return input.getAsLocale();
            }
        });
    }

    // JAX-RS request

    @Override
    public Variant selectVariant(final List<Variant> variants) throws IllegalArgumentException {
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException(METHOD_PARAMETER_CANNOT_BE_NULL_OR_EMPTY);
        }
        final Ref<String> varyValueRef = Refs.emptyRef();
        final Variant variant = VariantSelector.selectVariant(this, variants, varyValueRef);
        this.varyValue = varyValueRef.get();
        return variant;
    }

    /**
     * Get the value of HTTP Vary response header to be set in the response,
     * or {@code null} if no value is to be set.
     *
     * @return value of HTTP Vary response header to be set in the response if available,
     * {@code null} otherwise.
     */
    public String getVaryValue() {
        return varyValue;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(final EntityTag eTag) {
        if (eTag == null) {
            throw new IllegalArgumentException(METHOD_PARAMETER_CANNOT_BE_NULL_ETAG);
        }

        final Response.ResponseBuilder r = evaluateIfMatch(eTag);
        if (r != null) {
            return r;
        }
        return evaluateIfNoneMatch(eTag);
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(final Date lastModified) {
        if (lastModified == null) {
            throw new IllegalArgumentException(METHOD_PARAMETER_CANNOT_BE_NULL_LAST_MODIFIED);
        }

        final long lastModifiedTime = lastModified.getTime();
        final Response.ResponseBuilder r = evaluateIfUnmodifiedSince(lastModifiedTime);
        if (r != null) {
            return r;
        }
        return evaluateIfModifiedSince(lastModifiedTime);
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(final Date lastModified, final EntityTag eTag) {
        if (lastModified == null) {
            throw new IllegalArgumentException(METHOD_PARAMETER_CANNOT_BE_NULL_LAST_MODIFIED);
        }
        if (eTag == null) {
            throw new IllegalArgumentException(METHOD_PARAMETER_CANNOT_BE_NULL_ETAG);
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

        final boolean isGetOrHead = "GET".equals(getMethod()) || "HEAD".equals(getMethod());
        final Set<MatchingEntityTag> matchingTags = getIfNoneMatch();
        if (matchingTags != null) {
            r = evaluateIfNoneMatch(eTag, matchingTags, isGetOrHead);
            // If the If-None-Match header is present and there is no
            // match then the If-Modified-Since header must be ignored
            if (r == null) {
                return null;
            }

            // Otherwise if the If-None-Match header is present and there
            // is a match then the If-Modified-Since header must be checked
            // for consistency
        }

        final String ifModifiedSinceHeader = getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSinceHeader != null && !ifModifiedSinceHeader.isEmpty() && isGetOrHead) {
            r = evaluateIfModifiedSince(lastModifiedTime, ifModifiedSinceHeader);
            if (r != null) {
                r.tag(eTag);
            }
        }

        return r;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions() {
        final Set<MatchingEntityTag> matchingTags = getIfMatch();
        if (matchingTags == null) {
            return null;
        }

        // Since the resource does not exist the method must not be
        // perform and 412 Precondition Failed is returned
        return Response.status(Response.Status.PRECONDITION_FAILED);
    }

    // Private methods
    private Response.ResponseBuilder evaluateIfMatch(final EntityTag eTag) {
        final Set<? extends EntityTag> matchingTags = getIfMatch();
        if (matchingTags == null) {
            return null;
        }

        // The strong comparison function must be used to compare the entity
        // tags. Thus if the entity tag of the entity is weak then matching
        // of entity tags in the If-Match header should fail.
        if (eTag.isWeak()) {
            return Response.status(Response.Status.PRECONDITION_FAILED);
        }

        if (matchingTags != MatchingEntityTag.ANY_MATCH && !matchingTags.contains(eTag)) {
            // 412 Precondition Failed
            return Response.status(Response.Status.PRECONDITION_FAILED);
        }

        return null;
    }

    private Response.ResponseBuilder evaluateIfNoneMatch(final EntityTag eTag) {
        final Set<MatchingEntityTag> matchingTags = getIfNoneMatch();
        if (matchingTags == null) {
            return null;
        }

        final String httpMethod = getMethod();
        return evaluateIfNoneMatch(eTag, matchingTags, "GET".equals(httpMethod) || "HEAD".equals(httpMethod));
    }

    private Response.ResponseBuilder evaluateIfNoneMatch(final EntityTag eTag, final Set<? extends EntityTag> matchingTags,
                                                         final boolean isGetOrHead) {
        if (isGetOrHead) {
            if (matchingTags == MatchingEntityTag.ANY_MATCH) {
                // 304 Not modified
                return Response.notModified(eTag);
            }

            // The weak comparison function may be used to compare entity tags
            if (matchingTags.contains(eTag) || matchingTags.contains(new EntityTag(eTag.getValue(), !eTag.isWeak()))) {
                // 304 Not modified
                return Response.notModified(eTag);
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
                return Response.status(Response.Status.PRECONDITION_FAILED);
            }
        }

        return null;
    }

    private Response.ResponseBuilder evaluateIfUnmodifiedSince(final long lastModified) {
        final String ifUnmodifiedSinceHeader = getHeaderString(HttpHeaders.IF_UNMODIFIED_SINCE);
        if (ifUnmodifiedSinceHeader != null && !ifUnmodifiedSinceHeader.isEmpty()) {
            try {
                final long ifUnmodifiedSince = HttpHeaderReader.readDate(ifUnmodifiedSinceHeader).getTime();
                if (roundDown(lastModified) > ifUnmodifiedSince) {
                    // 412 Precondition Failed
                    return Response.status(Response.Status.PRECONDITION_FAILED);
                }
            } catch (final ParseException ex) {
                // Ignore the header if parsing error
            }
        }

        return null;
    }

    private Response.ResponseBuilder evaluateIfModifiedSince(final long lastModified) {
        final String ifModifiedSinceHeader = getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSinceHeader == null || ifModifiedSinceHeader.isEmpty()) {
            return null;
        }

        final String httpMethod = getMethod();
        if ("GET".equals(httpMethod) || "HEAD".equals(httpMethod)) {
            return evaluateIfModifiedSince(lastModified, ifModifiedSinceHeader);
        } else {
            return null;
        }
    }

    private Response.ResponseBuilder evaluateIfModifiedSince(final long lastModified, final String ifModifiedSinceHeader) {
        try {
            final long ifModifiedSince = HttpHeaderReader.readDate(ifModifiedSinceHeader).getTime();
            if (roundDown(lastModified) <= ifModifiedSince) {
                // 304 Not modified
                return Response.notModified();
            }
        } catch (final ParseException ex) {
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
    private static long roundDown(final long time) {
        return time - time % 1000;
    }

    /**
     * Get the values of a HTTP request header. The returned List is read-only.
     * This is a shortcut for {@code getRequestHeaders().get(name)}.
     *
     * @param name the header name, case insensitive.
     * @return a read-only list of header values.
     *
     * @throws IllegalStateException if called outside the scope of a request.
     */
    @Override
    public List<String> getRequestHeader(final String name) {
        return getHeaders().get(name);
    }

    /**
     * Get the values of HTTP request headers. The returned Map is case-insensitive
     * wrt. keys and is read-only. The method never returns {@code null}.
     *
     * @return a read-only map of header names and values.
     *
     * @throws IllegalStateException if called outside the scope of a request.
     */
    @Override
    public MultivaluedMap<String, String> getRequestHeaders() {
        return getHeaders();
    }

    /**
     * Check if the container request has been properly initialized for processing.
     *
     * @throws IllegalStateException in case the internal state is not ready for processing.
     */
    void checkState() throws IllegalStateException {
        if (securityContext == null) {
            throw new IllegalStateException("SecurityContext set in the ContainerRequestContext must not be null.");
        } else if (responseWriter == null) {
            throw new IllegalStateException("ResponseWriter set in the ContainerRequestContext must not be null.");
        }
    }
}
