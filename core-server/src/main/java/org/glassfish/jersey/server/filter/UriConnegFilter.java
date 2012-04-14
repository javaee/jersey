/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.PreMatchRequestFilter;
import org.glassfish.jersey.server.ResourceConfig;

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
 *Accept: application/atom+xml</pre>
 * Any existing "Accept" header value will be replaced.
 * <p>
 * If a suffix of "english" is registered with a language of
 * "en" then a GET request of:
 * <pre>GET /resource.english</pre>
 * <p>is transformed to:</p>
 * <pre>GET /resource
 *Accept-Language: en</pre>
 * Any existing "Accept-Language"header  value will be replaced.
 * <p>
 * The media type mappings are processed before the language type mappings.
 *
 * @author Paul Sandoz
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class UriConnegFilter implements PreMatchRequestFilter {

    public static final String PROPERTY_MEDIA_EXTENSIONS = "org.glassfish.jersey.server.filter.UriConnegFilter.mediaExtensions";
    public static final String PROPERTY_LANGUAGE_EXTENSIONS = "org.glassfish.jersey.server.filter.UriConnegFilter.languageExtensions";

    protected final Map<String, MediaType> mediaExtensions;
    protected final Map<String, String> languageExtensions;

    protected @Context UriInfo uriInfo;

    /**
     * Registers this filter into the passed {@link ResourceConfig} instance and
     * configures it.
     *
     * @param rc ResourceConfig instance where the filter should be registered.
     * @param mediaExtensions the suffix to media type mappings.
     * @param languageExtensions the suffix to language mappings.
     */
    public static void enableFor(ResourceConfig rc, Map<String, MediaType> mediaExtensions, Map<String, String> languageExtensions) {
        rc.addClasses(UriConnegFilter.class);
        rc.setProperty(PROPERTY_MEDIA_EXTENSIONS, mediaExtensions);
        rc.setProperty(PROPERTY_LANGUAGE_EXTENSIONS, languageExtensions);
    }

    /**
     * Create a filter that reads the configuration (media type and language mappings)
     * from the provided {@link ResourceConfig} instance.
     * This constructor will be called by the Jersey runtime when the filter
     * class is returned from {@link javax.ws.rs.core.Application#getClasses()}.
     * The {@link ResourceConfig} instance will get auto-injected.
     *
     * @param rc ResourceConfig instance that holds the configuration for the filter.
     */
    public UriConnegFilter(@Context ResourceConfig rc) {
        this((Map<String, MediaType>) rc.getProperty(PROPERTY_MEDIA_EXTENSIONS),
                (Map<String, String>) rc.getProperty(PROPERTY_LANGUAGE_EXTENSIONS));
    }

    /**
     * Create a filter with suffix to media type mappings and suffix to
     * language mappings.
     *
     * @param mediaExtensions the suffix to media type mappings.
     * @param languageExtensions the suffix to language mappings.
     */
    public UriConnegFilter(Map<String, MediaType> mediaExtensions, Map<String, String> languageExtensions) {
        if (mediaExtensions == null) {
            mediaExtensions = Collections.emptyMap();
        }

        if (languageExtensions == null) {
            languageExtensions = Collections.emptyMap();
        }

        this.mediaExtensions = mediaExtensions;
        this.languageExtensions = languageExtensions;
    }

    @Override
    public void preMatchFilter(FilterContext fc) throws IOException {
        // Quick check for a '.' character
        String path = uriInfo.getRequestUri().getRawPath();
        if (path.indexOf('.') == -1) {
            return;
        }

        List<PathSegment> l = uriInfo.getPathSegments(false);
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

        Request.RequestBuilder requestBuilder = null;

        for (int i = suffixes.length - 1; i >= 1; i--) {
            final String suffix = suffixes[i];
            if (suffix.length() == 0) {
                continue;
            }

            final MediaType accept = mediaExtensions.get(suffix);

            if (accept != null) {
                requestBuilder = fc.getRequestBuilder();
                requestBuilder.header(HttpHeaders.ACCEPT, accept.toString());

                final int index = path.lastIndexOf('.' + suffix);
                path = new StringBuilder(path).delete(index, index + suffix.length() + 1).toString();
                suffixes[i] = "";
                break;
            }
        }

        for (int i = suffixes.length - 1; i >= 1; i--) {
            final String suffix = suffixes[i];
            if (suffix.length() == 0)
                continue;

            final String acceptLanguage = languageExtensions.get(suffix);
            if (acceptLanguage != null) {
                if (requestBuilder == null) {
                    requestBuilder = fc.getRequestBuilder();
                }

                requestBuilder.header("Accept-Language", acceptLanguage);

                final int index = path.lastIndexOf('.' + suffix);
                path = new StringBuilder(path).delete(index, index + suffix.length() + 1).toString();
                suffixes[i] = "";
                break;
            }
        }

        if (requestBuilder != null) {
            requestBuilder.redirect(uriInfo.getRequestUriBuilder().replacePath(path).build());
            fc.setRequest(requestBuilder.build());
        }
    }
}
