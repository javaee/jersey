/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

/**
 * Adapter for {@link Request Jersey Request} to {@link javax.ws.rs.core.Request
 * JAX-RS Request}.
 *
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 */
// TODO Methods in this class should cache results to improve performance.
// TODO remove or make package-private
public final class JaxrsRequestView implements javax.ws.rs.core.Request {

    private Request wrapped;

    public JaxrsRequestView(Request wrapped) {
        this.wrapped = wrapped;
    }

    static Request unwrap(javax.ws.rs.core.Request request) {
        if (request instanceof JaxrsRequestView) {
            return ((JaxrsRequestView) request).wrapped;
        }

        throw new IllegalArgumentException(String.format("Request class type '%s' not supported.", request.getClass().getName()));
    }

    @Override
    public String getMethod() {
        return wrapped.method();
    }

    public URI getUri() {
        return wrapped.uri();
    }

    public JaxrsRequestHeadersView getHeaders() {
        return wrapped.getJaxrsHeaders();
    }

    public Object getEntity() {
        return wrapped.content();
    }

    public <T> T readEntity(Class<T> type) throws MessageProcessingException {
        return wrapped.content(type);
    }

    public <T> T readEntity(Class<T> rawEntityType, Type entityType) throws MessageProcessingException {
        return wrapped.content(rawEntityType,  entityType);
    }

    public <T> T readEntity(Class<T> type, Annotation[] annotations) throws MessageProcessingException {
        return wrapped.content(type, annotations);
    }

    public <T> T readEntity(Class<T> rawEntityType, Type entityType, Annotation[] annotations) throws MessageProcessingException {
        return wrapped.content(rawEntityType,  entityType, annotations);
    }

    public boolean hasEntity() {
        return !wrapped.isEmpty();
    }

    public void bufferEntity() throws MessageProcessingException {
        wrapped.bufferEntity();
    }

    public void close() throws MessageProcessingException {
        wrapped.close();
    }

    @Override
    public Variant selectVariant(List<Variant> variants) throws IllegalArgumentException {
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("The list of variants is null or empty");
        }
        // TODO add Vary header to response
        return VariantSelector.selectVariant(wrapped, variants);
    }

    @Override
    public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
        if (eTag == null) {
            throw new IllegalArgumentException("Parameter 'eTag' cannot be null.");
        }

        ResponseBuilder r = evaluateIfMatch(eTag);
        if (r != null) {
            return r;
        }
        return evaluateIfNoneMatch(eTag);
    }

    @Override
    public ResponseBuilder evaluatePreconditions(Date lastModified) {
        if (lastModified == null) {
            throw new IllegalArgumentException("Parameter 'lastModified' cannot be null.");
        }

        final long lastModifiedTime = lastModified.getTime();
        ResponseBuilder r = evaluateIfUnmodifiedSince(lastModifiedTime);
        if (r != null) {
            return r;
        }
        return evaluateIfModifiedSince(lastModifiedTime);
    }

    @Override
    public ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
        if (lastModified == null || eTag == null) {
            throw new IllegalArgumentException("Parameters 'lastModified' and 'eTag' cannot be null.");
        }

        ResponseBuilder r = evaluateIfMatch(eTag);
        if (r != null) {
            return r;
        }

        final long lastModifiedTime = lastModified.getTime();
        r = evaluateIfUnmodifiedSince(lastModifiedTime);
        if (r != null) {
            return r;
        }

        final boolean isGetOrHead = getMethod().equals("GET") || getMethod().equals("HEAD");
        final Set<MatchingEntityTag> matchingTags = HttpHelper.getIfNoneMatch(wrapped);
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

        final String ifModifiedSinceHeader = wrapped.header("If-Modified-Since");
        if (ifModifiedSinceHeader != null && ifModifiedSinceHeader.length() > 0 && isGetOrHead) {
            r = evaluateIfModifiedSince(lastModifiedTime, ifModifiedSinceHeader);
            if (r != null) {
                r.tag(eTag);
            }
        }

        return r;
    }

    @Override
    public ResponseBuilder evaluatePreconditions() {
        Set<MatchingEntityTag> matchingTags = HttpHelper.getIfMatch(wrapped);
        if (matchingTags == null) {
            return null;
        }

        // Since the resource does not exist the method must not be
        // perform and 412 Precondition Failed is returned
        return Responses.preconditionFailed(wrapped.toJaxrsRequest());
    }

    // Private methods
    private ResponseBuilder evaluateIfMatch(EntityTag eTag) {
        Set<? super MatchingEntityTag> matchingTags = HttpHelper.getIfMatch(wrapped);
        if (matchingTags == null) {
            return null;
        }

        // The strong comparison function must be used to compare the entity
        // tags. Thus if the entity tag of the entity is weak then matching
        // of entity tags in the If-Match header should fail.
        if (eTag.isWeak()) {
            return Responses.preconditionFailed(wrapped.toJaxrsRequest());
        }

        if (matchingTags != MatchingEntityTag.ANY_MATCH && !matchingTags.contains(eTag)) {
            // 412 Precondition Failed
            return Responses.preconditionFailed(wrapped.toJaxrsRequest());
        }

        return null;
    }

    private ResponseBuilder evaluateIfNoneMatch(EntityTag eTag) {
        Set<MatchingEntityTag> matchingTags = HttpHelper.getIfNoneMatch(wrapped);
        if (matchingTags == null) {
            return null;
        }

        final String httpMethod = getMethod();
        return evaluateIfNoneMatch(eTag, matchingTags, httpMethod.equals("GET") || httpMethod.equals("HEAD"));
    }

    private ResponseBuilder evaluateIfNoneMatch(EntityTag eTag, Set<? super MatchingEntityTag> matchingTags,
            boolean isGetOrHead) {
        if (isGetOrHead) {
            if (matchingTags == MatchingEntityTag.ANY_MATCH) {
                // 304 Not modified
                return Responses.notModified(eTag, wrapped.toJaxrsRequest());
            }

            // The weak comparison function may be used to compare entity tags
            if (matchingTags.contains(eTag) || matchingTags.contains(new EntityTag(eTag.getValue(), !eTag.isWeak()))) {
                // 304 Not modified
                return Responses.notModified(eTag, wrapped.toJaxrsRequest());
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
                return Responses.preconditionFailed(wrapped.toJaxrsRequest());
            }
        }

        return null;
    }

    private ResponseBuilder evaluateIfUnmodifiedSince(long lastModified) {
        String ifUnmodifiedSinceHeader = wrapped.header("If-Unmodified-Since");
        if (ifUnmodifiedSinceHeader != null && ifUnmodifiedSinceHeader.length() > 0) {
            try {
                long ifUnmodifiedSince = HttpHeaderReader.readDate(ifUnmodifiedSinceHeader).getTime();
                if (roundDown(lastModified) > ifUnmodifiedSince) {
                    // 412 Precondition Failed
                    return Responses.preconditionFailed(wrapped.toJaxrsRequest());
                }
            } catch (ParseException ex) {
                // Ignore the header if parsing error
            }
        }

        return null;
    }

    private ResponseBuilder evaluateIfModifiedSince(long lastModified) {
        String ifModifiedSinceHeader = wrapped.header("If-Modified-Since");
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

    private ResponseBuilder evaluateIfModifiedSince(long lastModified, String ifModifiedSinceHeader) {
        try {
            long ifModifiedSince = HttpHeaderReader.readDate(ifModifiedSinceHeader).getTime();
            if (roundDown(lastModified) <= ifModifiedSince) {
                // 304 Not modified
                return Responses.notModified(wrapped.toJaxrsRequest());
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

    public Map<String, Object> getProperties() {
        return wrapped.properties();
    }
}
