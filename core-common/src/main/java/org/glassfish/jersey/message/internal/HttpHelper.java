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
package org.glassfish.jersey.message.internal;

import java.net.URI;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Helper classes for HTTP.
 *
 * @author Paul Sandoz
 * @author Santiago Pericas-Geertsen (Santiago.PericasGeertsen at oracle.com)
 */
final class HttpHelper {

    /**
     * Get the content type from the "Content-Length".
     * <p>
     * @param headers Request or response headers.
     * @return The content length. If no "Content-Length" is present then -1 is
     *     returned.
     */
    public static int getContentLength(final Headers headers) {
        String value = headers.header(HttpHeaders.CONTENT_LENGTH);
        try {
            return (value != null && value.length() > 0) ? Integer.parseInt(value) : -1;
        } catch (NumberFormatException ex) {
            throw exception("Content-Length", value, ex);
        }
    }

    /**
     * Get the content type from the "Content-Type".
     * <p>
     * @param headers Request or response headers.
     * @return The content type. If no "Content-Type" is present then null is
     *     returned.
     */
    public static MediaType getContentType(final Headers headers) {
        final List<String> vs = headers.headerValues(HttpHeaders.CONTENT_TYPE);
        return (vs != null && vs.size() > 0) ? getContentType(vs.get(0)) : null;
    }

    /**
     * Get the content type from a String.
     * <p>
     * @param contentTypeString the content type as a String.
     * @return The content type. If no "Content-Type" is present then null is
     *     returned.
     */
    public static MediaType getContentType(final String contentTypeString) {
        try {
            return (contentTypeString != null) ? MediaType.valueOf(contentTypeString) : null;
        } catch (IllegalArgumentException e) {
            throw exception("Content-Type", contentTypeString, e);
        }
    }

    /**
     * Get the content type from an Object.
     * <p>
     * @param contentType the content type as an Object.
     * @return The content type. If no "Content-Type" is present then null is
     *     returned.
     */
    public static MediaType getContentType(final Object contentType) {
        if (contentType == null) {
            return null;
        }

        if (contentType instanceof MediaType) {
            return (MediaType) contentType;
        } else {
            return MediaType.valueOf(contentType.toString());
        }
    }

    /**
     * Get the content language as a Locale instance.
     *
     * @param headers Request or response headers.
     * @return the content language as a locale instance or null.
     */
    public static Locale getContentLanguageAsLocale(final Headers headers) {
        final List<String> vs = headers.headerValues(HttpHeaders.CONTENT_LANGUAGE);
        return (vs != null && vs.size() > 0) ? HttpHelper.getLanguageTagAsLocale(vs.get(0)) : null;
    }

    public static Locale getLanguageTagAsLocale(final String language) {
        if (language == null) {
            return null;
        }

        try {
            return new LanguageTag(language).getAsLocale();
        } catch (java.text.ParseException e) {
            throw exception("Content-Language", language, e);
        }
    }

    public static Date getDate(final Headers headers) {
        String date = headers.header(HttpHeaders.DATE);
        try {
            return date == null ? null : HttpHeaderReader.readDate(date);
        } catch (ParseException e) {
            throw exception("Date", date, e);
        }

    }

    public static Set<MatchingEntityTag> getIfMatch(final Headers headers) {
        final String ifMatch = headers.header(HttpHeaders.IF_MATCH);
        if (ifMatch == null || ifMatch.length() == 0) {
            return null;
        }
        try {
            return HttpHeaderReader.readMatchingEntityTag(ifMatch);
        } catch (java.text.ParseException e) {
            throw exception("If-Match", ifMatch, e);
        }
    }

    public static Set<MatchingEntityTag> getIfNoneMatch(final Headers headers) {
        final String ifNoneMatch = headers.header(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch == null || ifNoneMatch.length() == 0) {
            return null;
        }
        try {
            return HttpHeaderReader.readMatchingEntityTag(ifNoneMatch);
        } catch (java.text.ParseException e) {
            throw exception("If-None-Match", ifNoneMatch, e);
        }
    }

    /**
     * Get the list of Media type from the "Accept" of an HTTP request.
     * <p>
     * @param headers Request or response headers.
     * @return The list of MediaType. This list
     *         is ordered with the highest quality acceptable Media type occurring first
     *         (see {@link MediaTypes#MEDIA_TYPE_COMPARATOR}).
     *         If no "Accept" is present then a list with a single item of the Media
     *         type "*\\/*" is returned.
     */
    public static List<AcceptableMediaType> getAccept(final Headers headers) {
        final String accept = headers.header(HttpHeaders.ACCEPT);
        if (accept == null || accept.length() == 0) {
            return MediaTypes.GENERAL_ACCEPT_MEDIA_TYPE_LIST;
        }
        try {
            return HttpHeaderReader.readAcceptMediaType(accept);
        } catch (java.text.ParseException e) {
            throw exception("Accept", accept, e);
        }
    }

    public static List<AcceptableMediaType> getAccept(final Headers headers,
            List<QualitySourceMediaType> priorityMediaTypes) {
        final String accept = headers.header(HttpHeaders.ACCEPT);
        if (accept == null || accept.length() == 0) {
            return MediaTypes.GENERAL_ACCEPT_MEDIA_TYPE_LIST;
        }
        try {
            return HttpHeaderReader.readAcceptMediaType(accept, priorityMediaTypes);
        } catch (java.text.ParseException e) {
            throw exception("Accept", accept, e);
        }
    }

    /**
     * Get the list of language tag from the "Accept-Language" of an HTTP request.
     * <p>
     * @param headers Request or response headers.
     * @return The list of LanguageTag. This list
     *         is ordered with the highest quality acceptable language tag occurring first.
     */
    public static List<AcceptableLanguageTag> getAcceptLanguage(final Headers headers) {
        final String acceptLanguage = headers.header(HttpHeaders.ACCEPT_LANGUAGE);
        if (acceptLanguage == null || acceptLanguage.length() == 0) {
            return Collections.singletonList(new AcceptableLanguageTag("*", null));
        }
        try {
            return HttpHeaderReader.readAcceptLanguage(acceptLanguage);
        } catch (java.text.ParseException e) {
            throw exception("Accept-Language", acceptLanguage, e);
        }
    }

    /**
     * Get the list of language tag from the "Accept-Charset" of an HTTP request.
     * <p>
     * @param headers Request or response headers.
     * @return The list of AcceptableToken. This list
     *         is ordered with the highest quality acceptable charset occurring first.
     */
    public static List<AcceptableToken> getAcceptCharset(final Headers headers) {
        final String acceptCharset = headers.header(HttpHeaders.ACCEPT_CHARSET);
        try {
            if (acceptCharset == null || acceptCharset.length() == 0) {
                return Collections.singletonList(new AcceptableToken("*"));
            }
            return HttpHeaderReader.readAcceptToken(acceptCharset);
        } catch (java.text.ParseException e) {
            throw exception("Accept-Charset", acceptCharset, e);
        }
    }

    /**
     * Get the list of language tag from the "Accept-Charset" of an HTTP request.
     * <p>
     * @param headers Request or response headers.
     * @return The list of AcceptableToken. This list
     *         is ordered with the highest quality acceptable charset occurring first.
     */
    public static List<AcceptableToken> getAcceptEncoding(final Headers headers) {
        final String acceptEncoding = headers.header(HttpHeaders.ACCEPT_ENCODING);
        try {
            if (acceptEncoding == null || acceptEncoding.length() == 0) {
                return Collections.singletonList(new AcceptableToken("*"));
            }
            return HttpHeaderReader.readAcceptToken(acceptEncoding);
        } catch (java.text.ParseException e) {
            throw exception("Accept-Encoding", acceptEncoding, e);
        }
    }

    private static HeaderValueException exception(final String headerName, Object headerValue, Exception e) {
        return new HeaderValueException(LocalizationMessages.UNABLE_TO_PARSE_HEADER_VALUE(headerName, headerValue), e);
    }

    /**
     * Ascertain if an entity of a specific Media type is capable of being
     * produced from a list of Media type.
     *
     * @param contentType The Media type.
     * @param accept The list of Media types of entities that may be produced. This list
     *        MUST be ordered with the highest quality acceptable Media type occurring first
     *         (see {@link MediaTypes#MEDIA_TYPE_COMPARATOR}).
     * @return true if the Media type can be produced, otherwise false.
     */
    public static boolean produces(final MediaType contentType, List<MediaType> accept) {
        for (final MediaType a : accept) {
            if (a.getType().equals("*")) {
                return true;
            }

            if (contentType.isCompatible(a)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get allowed methods from HTTP header "Allow".
     *
     * @param headers Request or response headers.
     * @return Value of "Allow" header
     */
    public static Set<String> getAllowedMethods(final Headers headers) {
        final String allowed = headers.header("Allow");
        if (allowed == null || allowed.length() == 0) {
            return Collections.emptySet();
        }
        try {
            return new HashSet<String>(HttpHeaderReader.readStringList(allowed));
        } catch (java.text.ParseException e) {
            throw exception("Allow", allowed, e);
        }
    }

    public static Set<Link> getLinks(final Headers headers) {
        // TODO add Link constant to HttpHeaders
        List<String> links = headers.headerValues("Link");
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
            throw exception("Link", links, e);
        }
    }

    public static Map<String, Cookie> getCookies(final Headers headers) {
        List<String> cookies = headers.headerValues(HttpHeaders.COOKIE);
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

    public static Map<String, NewCookie> getNewCookies(final Headers headers) {
        List<String> cookies = headers.headerValues(HttpHeaders.SET_COOKIE);
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

    public static EntityTag getEntityTag(final Headers headers) {
        String value = headers.header(HttpHeaders.ETAG);
        try {
            return value == null ? null : EntityTag.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw exception("ETag", value, ex);
        }
    }

    public static Date getLastModified(final Headers headers) {
        String value = headers.header(HttpHeaders.LAST_MODIFIED);
        try {
            return value == null ? null : HttpHeaderReader.readDate(value);
        } catch (ParseException e) {
            throw exception("Last-Modified", value, e);
        }
    }

    public static URI getLocation(final Headers headers) {
        String value = headers.header(HttpHeaders.LOCATION);
        try {
            return value == null ? null : URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw exception("Location", value, ex);
        }
    }

    /**
     * Prevents instantiation.
     */
    private HttpHelper() {
    }
}
