/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Inject;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.uri.UriComponent;
import org.glassfish.jersey.uri.UriTemplate;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Default implementation of the routing context as well as URI information provider.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@RequestScoped
public class UriRoutingContext implements RoutingContext, ExtendedUriInfo {

    private final LinkedList<MatchResult> matchResults = Lists.newLinkedList();
    private final LinkedList<Object> matchedResources = Lists.newLinkedList();
    private final LinkedList<UriTemplate> templates = Lists.newLinkedList();
    private MultivaluedHashMap<String, String> encodedTemplateValues;
    private MultivaluedHashMap<String, String> decodedTemplateValues;
    private final LinkedList<String> paths = Lists.newLinkedList();
    private Inflector<ContainerRequest, ContainerResponse> inflector;
    private final LinkedList<RuntimeResource> matchedRuntimeResources = Lists.newLinkedList();
    volatile private ResourceMethod matchedResourceMethod = null;
    volatile private Resource matchedResourceModel = null;
    private final ProcessingProviders processingProviders;

    /**
     * Injection constructor.
     *
     * @param requestContext      request reference.
     * @param processingProviders processing providers.
     */
    @Inject
    UriRoutingContext(Ref<ContainerRequest> requestContext, ProcessingProviders processingProviders) {
        this.requestContext = requestContext;
        this.processingProviders = processingProviders;
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
    public void pushTemplates(UriTemplate resourceTemplate, UriTemplate methodTemplate) {
        if (encodedTemplateValues == null) {
            encodedTemplateValues = new MultivaluedHashMap<String, String>();
        }

        final Iterator<MatchResult> matchResultIterator = matchResults.iterator();
        templates.push(resourceTemplate);
        if (methodTemplate != null) {
            templates.push(methodTemplate);
            // fast-forward the match result iterator to second element in the stack
            matchResultIterator.next();
        }

        pushMatchedTemplateValues(resourceTemplate, matchResultIterator.next());
        if (methodTemplate != null) {
            // use the match result from the top of the stack
            pushMatchedTemplateValues(methodTemplate, matchResults.peek());
        }
    }

    private void pushMatchedTemplateValues(UriTemplate template, MatchResult matchResult) {
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
    public void setInflector(final Inflector<ContainerRequest, ContainerResponse> inflector) {
        this.inflector = inflector;
    }

    @Override
    public Inflector<ContainerRequest, ContainerResponse> getInflector() {
        return inflector;
    }

    @Override
    public Iterable<RankedProvider<ContainerRequestFilter>> getBoundRequestFilters() {
        return emptyIfNull(inflector instanceof ResourceMethodInvoker ?
                ((ResourceMethodInvoker) inflector).getRequestFilters() : null);
    }

    @Override
    public Iterable<RankedProvider<ContainerResponseFilter>> getBoundResponseFilters() {
        return emptyIfNull(inflector instanceof ResourceMethodInvoker ?
                ((ResourceMethodInvoker) inflector).getResponseFilters() : null);
    }

    @Override
    public Iterable<ReaderInterceptor> getBoundReaderInterceptors() {
        return inflector instanceof ResourceMethodInvoker ?
                ((ResourceMethodInvoker) inflector).getReaderInterceptors()
                : processingProviders.getSortedGlobalReaderInterceptors();
    }

    @Override
    public Iterable<WriterInterceptor> getBoundWriterInterceptors() {
        return inflector instanceof ResourceMethodInvoker ?
                ((ResourceMethodInvoker) inflector).getWriterInterceptors()
                : processingProviders.getSortedGlobalWriterInterceptors();
    }

    @Override
    public void setMatchedResourceMethod(ResourceMethod resourceMethod) {
        this.matchedResourceMethod = resourceMethod;
    }

    @Override
    public void setMatchedResource(Resource resourceModel) {
        this.matchedResourceModel = resourceModel;
    }

    @Override
    public void pushMatchedRuntimeResource(RuntimeResource runtimeResource) {
        this.matchedRuntimeResources.push(runtimeResource);
    }

    // UriInfo
    private Ref<ContainerRequest> requestContext;

    private static <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
        return iterable == null ? Collections.<T>emptyList() : iterable;
    }

    @Override
    public URI getAbsolutePath() {
        return URI.create(getEncodedPath());
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return new JerseyUriBuilder().uri(getAbsolutePath());
    }

    @Override
    public URI getBaseUri() {
        return requestContext.get().getBaseUri();
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return new JerseyUriBuilder().uri(getBaseUri());
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
        return requestContext.get().getPath(true);
    }

    @Override
    public String getPath(boolean decode) {
        return requestContext.get().getPath(decode);
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
        return UriComponent.decodeQuery(getRequestUri(), decode);
    }

    @Override
    public URI getRequestUri() {
        return requestContext.get().getRequestUri();
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return UriBuilder.fromUri(getRequestUri());
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

    @Override
    public Method getResourceMethod() {
        return inflector instanceof ResourceMethodInvoker ?
                ((ResourceMethodInvoker) inflector).getResourceMethod() : null;
    }

    @Override
    public Class<?> getResourceClass() {
        return inflector instanceof ResourceMethodInvoker ?
                ((ResourceMethodInvoker) inflector).getResourceClass() : null;
    }

    @Override
    public List<RuntimeResource> getMatchedRuntimeResources() {
        return this.matchedRuntimeResources;
    }

    @Override
    public ResourceMethod getMatchedResourceMethod() {
        return matchedResourceMethod;
    }

    @Override
    public Resource getMatchedModelResource() {
        return matchedResourceModel;
    }

    @Override
    public URI resolve(URI uri) {
        return UriTemplate.resolve(getBaseUri(), uri);
    }

    @Override
    public URI relativize(URI uri) {
        if (!uri.isAbsolute()) {
            uri = resolve(uri);
        }

        return UriTemplate.relativize(getRequestUri(), uri);
    }
}
