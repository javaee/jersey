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
package org.glassfish.jersey.server.internal.routing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;

import org.glassfish.jersey._remove.Helper;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.uri.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriComponent;
import org.glassfish.jersey.uri.UriTemplate;
import org.glassfish.jersey.uri.internal.UriBuilderImpl;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * Default implementation of the routing context as well as URI information provider.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class UriRoutingContext implements RoutingContext, ExtendedUriInfo {

    private final LinkedList<MatchResult> matchResults = Lists.newLinkedList();
    private final LinkedList<Object> matchedResources = Lists.newLinkedList();
    private final LinkedList<UriTemplate> templates = Lists.newLinkedList();
    private MultivaluedHashMap<String, String> encodedTemplateValues;
    private MultivaluedHashMap<String, String> decodedTemplateValues;
    private final LinkedList<String> paths = Lists.newLinkedList();
    private Optional<MediaType> effectiveMediaType = Optional.fromNullable(null);
    private Optional<Type> responseMethodType = Optional.fromNullable(null);
    private Optional<Annotation[]> responseMethodAnnotations = Optional.fromNullable(null);
    private Inflector<Request, Response> inflector = null;

    /**
     * Injection constructor.
     *
     * @param request request reference.
     */
    UriRoutingContext(@Inject Ref<Request> request) {
        this.request = request;
    }

    // RoutingContext
    @Override
    public void pushMatchResult(MatchResult matchResult) {
        matchResults.push(matchResult);
    }

    @Override
    public void pushMatchedResource(Object resource) {
        matchedResources.push(resource);
    }

    @Override
    public Object peekMatchedResource() {
        return matchedResources.peek();
    }

    @Override
    public void pushLeftHandPath() {
        final String rightHandPath = getFinalMatchingGroup();
        final int rhpLength = (rightHandPath != null) ? rightHandPath.length() : 0;
        final String encodedRequestPath = getAbsolutePath().toString();
        // TODO: do we need to cut the starting slash ?
//        paths.addFirst(encodedRequestPath.substring(startIndex, encodedRequestPath.length() - rhpLength));
        if (encodedRequestPath.length() != rhpLength) {
            final int startIndex = ((encodedRequestPath.length() > 1) && (encodedRequestPath.charAt(0) == '/')) ? 1 : 0;
            paths.addFirst(encodedRequestPath.substring(startIndex, encodedRequestPath.length() - rhpLength));
        }
    }

    @Override
    public void pushTemplate(UriTemplate template) {
        templates.addFirst(template);

        if (encodedTemplateValues == null) {
            encodedTemplateValues = new MultivaluedHashMap<String, String>();
        }

        MatchResult matchResult = peekMatchResult();
        int i = 1;
        for (String templateVariable : template.getTemplateVariables()) {
            final String value = matchResult.group(i++);
            encodedTemplateValues.addFirst(templateVariable, value);

            if (decodedTemplateValues != null) {
                decodedTemplateValues.addFirst(
                        UriComponent.decode(templateVariable, UriComponent.Type.PATH_SEGMENT),
                        UriComponent.decode(value, UriComponent.Type.PATH));
            }
        }
    }

    @Override
    public MatchResult peekMatchResult() {
        return matchResults.peek();
    }

    @Override
    public String getFinalMatchingGroup() {
        final MatchResult mr = matchResults.peek();
        if (mr == null) {
            return null;
        }

        String finalGroup = mr.group(mr.groupCount());
        // We have found a match but the right hand path pattern did not match anything
        // so just returning an empty string as a final matching group result.
        // Otherwise a non-empty patterns would fail to match the right-hand-path properly.
        // See also PatternWithGroups.match(CharSequence) implementation.
        return finalGroup == null ? "" : finalGroup;
    }

    @Override
    public LinkedList<MatchResult> getMatchedResults() {
        return matchResults;
    }

    @Override
    public MediaType getEffectiveAcceptableType() {
        return effectiveMediaType.orNull();
    }

    @Override
    public void setEffectiveAcceptableType(MediaType type) {
        effectiveMediaType = Optional.of(type);
    }

    @Override
    public Type getResponseMethodType() {
        return responseMethodType.orNull();
    }

    @Override
    public void setResponseMethodType(Type responseType) throws NullPointerException {
        responseMethodType = Optional.of(responseType);
    }


    @Override
    public void setResponseMethodAnnotations(Annotation[] annotations) throws NullPointerException {
        responseMethodAnnotations = Optional.of(annotations);
    }

    @Override
    public Annotation[] getResponseMethodAnnotations() {
        return responseMethodAnnotations.orNull();
    }

    @Override
    public void setInflector(final Inflector<Request, Response> inflector) {
        this.inflector = inflector;
    }

    @Override
    public Inflector<Request, Response> getInflector() {
        return inflector;
    }

    // UriInfo
    private Ref<Request> request;

    @Override
    public URI getAbsolutePath() {
        return URI.create(getEncodedPath());
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return new UriBuilderImpl().uri(getAbsolutePath());
    }

    @Override
    public URI getBaseUri() {
        return Requests.baseUri(request.get());
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return new UriBuilderImpl().uri(Requests.baseUri(request.get()));
    }

    @Override
    public List<Object> getMatchedResources() {
        return Collections.unmodifiableList(matchedResources);
    }

    @Override
    public List<String> getMatchedURIs() {
        return getMatchedURIs(true);
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        final List<String> result;
        if (decode) {
            result = Lists.transform(paths, new Function<String, String>() {

                @Override
                public String apply(String input) {
                    return UriComponent.decode(input, UriComponent.Type.PATH);
                }

            });

        } else {
            result = paths;
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public String getPath() {
        return Requests.relativePath(request.get(), true);
    }

    @Override
    public String getPath(boolean decode) {
        return Requests.relativePath(request.get(), decode);
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return getPathParameters(true);
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        if (decode) {
            if (decodedTemplateValues != null) {
                return decodedTemplateValues;
            }

            decodedTemplateValues = new MultivaluedHashMap<String, String>();
            for (Map.Entry<String, List<String>> e : encodedTemplateValues.entrySet()) {
                decodedTemplateValues.put(
                        UriComponent.decode(e.getKey(), UriComponent.Type.PATH_SEGMENT),
                        // we need to keep the ability to add new entries
                        new LinkedList<String>(Lists.transform(e.getValue(), new Function<String, String>() {

                            @Override
                            public String apply(String input) {
                                return UriComponent.decode(input, UriComponent.Type.PATH);
                            }
                        })));
            }

            return decodedTemplateValues;
        } else {
            return encodedTemplateValues;
        }
    }

    private String getEncodedPath() {
        final URI requestUri = getRequestUri();
        final String rp = requestUri.toString();
        final String qrp = requestUri.getRawQuery();
        return qrp == null ? rp : rp.substring(0, rp.length() - qrp.length() - 1);
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        final String ep = getEncodedPath();
        final String base = getBaseUri().toString();
        return UriComponent.decodePath(ep.substring(base.length()), decode);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return UriComponent.decodeQuery(getRequestUri(), false);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return UriComponent.decodeQuery(getRequestUri(), true);
    }

    @Override
    public URI getRequestUri() {
        return Helper.unwrap(request.get()).getUri();
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return UriBuilder.fromUri(Helper.unwrap(request.get()).getUri());
    }

    // ExtendedUriInfo
    @Override
    public Throwable getMappedThrowable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<UriTemplate> getMatchedTemplates() {
        return Collections.unmodifiableList(templates);
    }

    @Override
    public List<PathSegment> getPathSegments(String name) {
        return getPathSegments(name, true);
    }

    @Override
    public List<PathSegment> getPathSegments(String name, boolean decode) {
        int[] bounds = getPathParameterBounds(name);
        if (bounds != null) {
            String path = matchResults.getLast().group();
            // Work out how many path segments are up to the start
            // and end position of the matching path parameter value
            // This assumes that the path always starts with a '/'
            int segmentsStart = 0;
            for (int x = 0; x < bounds[0]; x++) {
                if (path.charAt(x) == '/') {
                    segmentsStart++;
                }
            }
            int segmentsEnd = segmentsStart;
            for (int x = bounds[0]; x < bounds[1]; x++) {
                if (path.charAt(x) == '/') {
                    segmentsEnd++;
                }
            }

            return getPathSegments(decode).subList(segmentsStart - 1, segmentsEnd);
        } else {
            return Collections.emptyList();
        }
    }

    private int[] getPathParameterBounds(String name) {
        Iterator<UriTemplate> templatesIterator = templates.iterator();
        Iterator<MatchResult> matchResultsIterator = matchResults.iterator();
        while (templatesIterator.hasNext()) {
            MatchResult mr = matchResultsIterator.next();
            // Find the index of path parameter
            int pIndex = getLastPathParameterIndex(name, templatesIterator.next());
            if (pIndex != -1) {
                int pathLength = mr.group().length();
                int segmentIndex = mr.end(pIndex + 1);
                int groupLength = segmentIndex - mr.start(pIndex + 1);

                // Find the absolute position of the end of the
                // capturing group in the request path
                while (matchResultsIterator.hasNext()) {
                    mr = matchResultsIterator.next();
                    segmentIndex += mr.group().length() - pathLength;
                    pathLength = mr.group().length();
                }
                return new int[]{segmentIndex - groupLength, segmentIndex};
            }
        }
        return null;
    }

    private int getLastPathParameterIndex(String name, UriTemplate t) {
        int i = 0;
        int pIndex = -1;
        for (String parameterName : t.getTemplateVariables()) {
            if (parameterName.equals(name)) {
                pIndex = i;
            }
            i++;
        }
        return pIndex;
    }
}
