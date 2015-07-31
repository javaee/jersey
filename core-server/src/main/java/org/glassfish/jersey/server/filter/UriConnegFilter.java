/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import javax.annotation.Priority;

import org.glassfish.jersey.message.internal.LanguageTag;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.uri.UriComponent;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * A URI-based content negotiation filter mapping a dot-declared suffix in
 * URI to media type that is the value of the <code>Accept</code> header
 * or a language that is the value of the <code>Accept-Language</code> header.
 * <p>
 * This filter may be used when the acceptable media type and acceptable
 * language need to be declared in the URI.
 * <p>
 * This class may be extended to declare the mappings and the extending class,
 * <code>foo.MyUriConnegFilter</code> say, can be registered as a container request
 * filter.
 * <p>
 * If a suffix of "atom" is registered with a media type of
 * "application/atom+xml" then a GET request of:
 * <pre>GET /resource.atom</pre>
 * <p>is transformed to:</p>
 * <pre>GET /resource
 * Accept: application/atom+xml</pre>
 * Any existing "Accept" header value will be replaced.
 * <p>
 * If a suffix of "english" is registered with a language of
 * "en" then a GET request of:
 * <pre>GET /resource.english</pre>
 * <p>is transformed to:</p>
 * <pre>GET /resource
 * Accept-Language: en</pre>
 * Any existing "Accept-Language"header  value will be replaced.
 * <p/>
 * The media type mappings are processed before the language type mappings.
 *
 * @author Paul Sandoz
 * @author Martin Matula
 */
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public final class UriConnegFilter implements ContainerRequestFilter {

    protected final Map<String, MediaType> mediaTypeMappings;
    protected final Map<String, String> languageMappings;

    /**
     * Create a filter that reads the configuration (media type and language mappings)
     * from the provided {@link ResourceConfig} instance.
     * This constructor will be called by the Jersey runtime when the filter
     * class is returned from {@link javax.ws.rs.core.Application#getClasses()}.
     * The {@link ResourceConfig} instance will get auto-injected.
     *
     * @param rc ResourceConfig instance that holds the configuration for the filter.
     */
    public UriConnegFilter(@Context final Configuration rc) {
        this(extractMediaTypeMappings(rc.getProperty(ServerProperties.MEDIA_TYPE_MAPPINGS)),
                extractLanguageMappings(rc.getProperty(ServerProperties.LANGUAGE_MAPPINGS)));
    }

    /**
     * Create a filter with suffix to media type mappings and suffix to
     * language mappings.
     *
     * @param mediaTypeMappings the suffix to media type mappings.
     * @param languageMappings  the suffix to language mappings.
     */
    public UriConnegFilter(Map<String, MediaType> mediaTypeMappings, Map<String, String> languageMappings) {
        if (mediaTypeMappings == null) {
            mediaTypeMappings = Collections.emptyMap();
        }

        if (languageMappings == null) {
            languageMappings = Collections.emptyMap();
        }

        this.mediaTypeMappings = mediaTypeMappings;
        this.languageMappings = languageMappings;
    }

    @Override
    public void filter(final ContainerRequestContext rc) throws IOException {
        final UriInfo uriInfo = rc.getUriInfo();

        // Quick check for a '.' character
        String path = uriInfo.getRequestUri().getRawPath();
        if (path.indexOf('.') == -1) {
            return;
        }

        final List<PathSegment> l = uriInfo.getPathSegments(false);
        if (l.isEmpty()) {
            return;
        }

        // Get the last non-empty path segment
        PathSegment segment = null;
        for (int i = l.size() - 1; i >= 0; i--) {
            segment = l.get(i);
            if (segment.getPath().length() > 0) {
                break;
            }
        }
        if (segment == null) {
            return;
        }

        final int length = path.length();

        // Get the suffixes
        final String[] suffixes = segment.getPath().split("\\.");

        for (int i = suffixes.length - 1; i >= 1; i--) {
            final String suffix = suffixes[i];
            if (suffix.length() == 0) {
                continue;
            }

            final MediaType accept = mediaTypeMappings.get(suffix);

            if (accept != null) {
                rc.getHeaders().putSingle(HttpHeaders.ACCEPT, accept.toString());

                final int index = path.lastIndexOf('.' + suffix);
                path = new StringBuilder(path).delete(index, index + suffix.length() + 1).toString();
                suffixes[i] = "";
                break;
            }
        }

        for (int i = suffixes.length - 1; i >= 1; i--) {
            final String suffix = suffixes[i];
            if (suffix.length() == 0) {
                continue;
            }

            final String acceptLanguage = languageMappings.get(suffix);
            if (acceptLanguage != null) {
                rc.getHeaders().putSingle(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);

                final int index = path.lastIndexOf('.' + suffix);
                path = new StringBuilder(path).delete(index, index + suffix.length() + 1).toString();
                suffixes[i] = "";
                break;
            }
        }

        if (length != path.length()) {
            rc.setRequestUri(uriInfo.getRequestUriBuilder().replacePath(path).build());
        }
    }

    private static interface TypeParser<T> {

        public T valueOf(String s);
    }

    private static Map<String, MediaType> extractMediaTypeMappings(final Object mappings) {
        // parse and validate mediaTypeMappings set through MEDIA_TYPE_MAPPINGS property
        return parseAndValidateMappings(ServerProperties.MEDIA_TYPE_MAPPINGS, mappings, new TypeParser<MediaType>() {
            public MediaType valueOf(final String value) {
                return MediaType.valueOf(value);
            }
        });
    }

    private static Map<String, String> extractLanguageMappings(final Object mappings) {
        // parse and validate languageMappings set through LANGUAGE_MAPPINGS property
        return parseAndValidateMappings(ServerProperties.LANGUAGE_MAPPINGS, mappings, new TypeParser<String>() {
            public String valueOf(final String value) {
                return LanguageTag.valueOf(value).toString();
            }
        });
    }

    private static <T> Map<String, T> parseAndValidateMappings(final String property,
                                                               final Object mappings,
                                                               final TypeParser<T> parser) {
        if (mappings == null) {
            return Collections.emptyMap();
        }

        if (mappings instanceof Map) {
            return (Map<String, T>) mappings;
        }

        final HashMap<String, T> mappingsMap = Maps.newHashMap();

        if (mappings instanceof String) {
            parseMappings(property, (String) mappings, mappingsMap, parser);
        } else if (mappings instanceof String[]) {
            final String[] mappingsArray = (String[]) mappings;
            for (final String aMappingsArray : mappingsArray) {
                parseMappings(property, aMappingsArray, mappingsMap, parser);
            }
        } else {
            throw new IllegalArgumentException(LocalizationMessages.INVALID_MAPPING_TYPE(property));
        }

        encodeKeys(mappingsMap);

        return mappingsMap;
    }

    private static <T> void parseMappings(final String property, final String mappings,
                                          final Map<String, T> mappingsMap, final TypeParser<T> parser) {
        if (mappings == null) {
            return;
        }

        final String[] records = mappings.split(",");

        for (final String record : records) {
            final String[] mapping = record.split(":");
            if (mapping.length != 2) {
                throw new IllegalArgumentException(LocalizationMessages.INVALID_MAPPING_FORMAT(property, mappings));
            }

            final String trimmedSegment = mapping[0].trim();
            final String trimmedValue = mapping[1].trim();

            if (trimmedSegment.length() == 0) {
                throw new IllegalArgumentException(LocalizationMessages.INVALID_MAPPING_KEY_EMPTY(property, record));
            }
            if (trimmedValue.length() == 0) {
                throw new IllegalArgumentException(LocalizationMessages.INVALID_MAPPING_VALUE_EMPTY(property, record));
            }

            mappingsMap.put(trimmedSegment, parser.valueOf(trimmedValue));
        }
    }

    private static <T> void encodeKeys(final Map<String, T> map) {
        final Map<String, T> tempMap = new HashMap<>();
        for (final Map.Entry<String, T> entry : map.entrySet()) {
            tempMap.put(UriComponent.contextualEncode(entry.getKey(), UriComponent.Type.PATH_SEGMENT), entry.getValue());
        }
        map.clear();
        map.putAll(tempMap);
    }
}
