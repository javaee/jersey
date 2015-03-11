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
package org.glassfish.jersey.message.internal;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import jersey.repackaged.com.google.common.base.Predicate;
import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Common media types and functionality.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class MediaTypes {

    /**
     * WADL Media type.
     */
    public static final MediaType WADL_TYPE = MediaType.valueOf("application/vnd.sun.wadl+xml");

    /**
     * A comparator for media types, that implements the "partial order" defined in the resource matching algorithm
     * section of the JAX-RS specification, except that this comparator is "inverted" so that it allows for natural
     * sorting in Java collections, where "lower" values are put to the front of a collection.
     *
     * IOW, when used to sort a collection, the resulting collection can be iterated in a way that the more specific
     * media types are preferred over the less specific ones:
     *
     * <pre>
     *   m/n &lt; m/&#42; &lt; &#42;/&#42;</pre>
     *
     * The actual media type values are ignored, i.e. the different media types are considered equal if they are
     * comparably specific:
     *
     * <pre>
     *   compare(m/n, x/y) == 0
     *   compare(m/&#42;, x/&#42;) == 0</pre>
     */
    public static final Comparator<MediaType> PARTIAL_ORDER_COMPARATOR = new Comparator<MediaType>() {

        private int rank(final MediaType type) {
            // "m/n" = 0; "m/*" = 1; "*/*" = 3; ...also "*/n" = 2, but that's just nonsense...
            return ((type.isWildcardType() ? 1 : 0) << 1) | (type.isWildcardSubtype() ? 1 : 0);
        }

        @Override
        public int compare(MediaType typeA, MediaType typeB) {
            return rank(typeA) - rank(typeB);
        }
    };

    /**
     * Comparator for lists of media types.
     * <p>
     * The least specific content type of each list is obtained and then compared
     * using {@link #PARTIAL_ORDER_COMPARATOR}.
     * <p>
     * Assumes each list is already ordered according to {@link #PARTIAL_ORDER_COMPARATOR}
     * and therefore the least specific media type is at the end of the list.
     */
    public static final Comparator<List<? extends MediaType>> MEDIA_TYPE_LIST_COMPARATOR =
            new Comparator<List<? extends MediaType>>() {

                @Override
                public int compare(List<? extends MediaType> o1, List<? extends MediaType> o2) {
                    return PARTIAL_ORDER_COMPARATOR.compare(getLeastSpecific(o1), getLeastSpecific(o2));
                }

                private MediaType getLeastSpecific(List<? extends MediaType> l) {
                    // FIXME: really comparing just last one?
                    return l.get(l.size() - 1);
                }
            };
    /**
     * A singleton list containing the wildcard media type.
     */
    public static final List<MediaType> WILDCARD_TYPE_SINGLETON_LIST =
            Collections.singletonList(MediaType.WILDCARD_TYPE);
    /**
     * An acceptable media type corresponding to a wildcard type.
     */
    public static final AcceptableMediaType WILDCARD_ACCEPTABLE_TYPE = new AcceptableMediaType("*", "*");
    /**
     * An acceptable media type corresponding to a wildcard type.
     */
    public static final QualitySourceMediaType WILDCARD_QS_TYPE = new QualitySourceMediaType("*", "*");
    /**
     * A singleton list containing the wildcard media type.
     */
    public static final List<MediaType> WILDCARD_QS_TYPE_SINGLETON_LIST =
            Collections.<MediaType>singletonList(WILDCARD_QS_TYPE);

    /**
     * Cache containing frequently requested media type values with a wildcard subtype.
     */
    private static final Map<String, MediaType> WILDCARD_SUBTYPE_CACHE = new HashMap<String, MediaType>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put("application", new MediaType("application", MediaType.MEDIA_TYPE_WILDCARD));
            put("multipart", new MediaType("multipart", MediaType.MEDIA_TYPE_WILDCARD));
            put("text", new MediaType("text", MediaType.MEDIA_TYPE_WILDCARD));
        }
    };
    /**
     * Predicate for constructing filtering parameter maps that ignore the "q" and "qs" parameters.
     */
    private static final Predicate<String> QUALITY_PARAM_FILTERING_PREDICATE = new Predicate<String>() {
        @Override
        public boolean apply(String input) {
            return !Quality.QUALITY_SOURCE_PARAMETER_NAME.equals(input)
                    && !Quality.QUALITY_PARAMETER_NAME.equals(input);
        }
    };

    /**
     * Prevents initialization.
     */
    private MediaTypes() {
        throw new AssertionError("Instantiation not allowed.");
    }

    /**
     * Determine if the two media types are type-equal (their {@link MediaType#getType() type}
     * and {@link MediaType#getSubtype() subtype} are equal). For example:
     * <pre>
     *  m/n == m/n
     *  m/n;p1 == m/n;p2
     *
     *  m/n != m/y
     *  m/n != x/n
     *  m/n != x/y</pre>
     *
     * @param m1 first media type.
     * @param m2 second media type.
     * @return {@code true} if the two media types are of the same type and subtype,
     * {@code false} otherwise.
     */
    public static boolean typeEqual(MediaType m1, MediaType m2) {
        if (m1 == null || m2 == null) {
            return false;
        }

        return m1.getSubtype().equalsIgnoreCase(m2.getSubtype()) && m1.getType().equalsIgnoreCase(m2.getType());
    }

    /**
     * Determine if the two list of media types share a common
     * {@link #typeEqual(javax.ws.rs.core.MediaType, javax.ws.rs.core.MediaType) type-equal}
     * sub-list.
     *
     * @param ml1 first media type list.
     * @param ml2 second media type list.
     * @return {@code true} if the two media type lists intersect by sharing a
     * common type-equal sub-list, {@code false} otherwise.
     */
    public static boolean intersect(List<? extends MediaType> ml1, List<? extends MediaType> ml2) {
        for (MediaType m1 : ml1) {
            for (MediaType m2 : ml2) {
                if (MediaTypes.typeEqual(m1, m2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the most specific media type from a pair of media types. The most
     * specific media type is the media type from the pair that has least
     * wild cards present, or has more parameters specified.
     *
     * @param m1 the first media type.
     * @param m2 the second media type.
     * @return the most specific media type. If the media types are equally
     * specific then the first media type is returned.
     */
    public static MediaType mostSpecific(MediaType m1, MediaType m2) {
        if (m1.isWildcardType() && !m2.isWildcardType()) {
            return m2;
        }
        if (m1.isWildcardSubtype() && !m2.isWildcardSubtype()) {
            return m2;
        }
        if (m2.getParameters().size() > m1.getParameters().size()) {
            return m2;
        }

        return m1;
    }

    /**
     * Create an unmodifiable list of media types from the values declared in the {@link Consumes}
     * annotation.
     *
     * @param annotation the Consumes annotation.
     * @return the list of {@link MediaType}, ordered according to {@link #PARTIAL_ORDER_COMPARATOR}.
     */
    public static List<MediaType> createFrom(Consumes annotation) {
        if (annotation == null) {
            return WILDCARD_TYPE_SINGLETON_LIST;
        }

        return createFrom(annotation.value());
    }

    /**
     * Create an unmodifiable list of media types from the values declared in the {@link Produces}
     * annotation.
     *
     * @param annotation the Produces annotation.
     * @return the list of {@link MediaType}, ordered according to {@link #PARTIAL_ORDER_COMPARATOR}.
     */
    public static List<MediaType> createFrom(Produces annotation) {
        if (annotation == null) {
            return WILDCARD_TYPE_SINGLETON_LIST;
        }

        return createFrom(annotation.value());
    }

    /**
     * Create an unmodifiable list of media type from a string array of media types.
     *
     * @param mediaTypes the string array of media types.
     * @return the list of {@link MediaType}, ordered according to {@link #PARTIAL_ORDER_COMPARATOR}.
     */
    public static List<MediaType> createFrom(String[] mediaTypes) {
        List<MediaType> result = new ArrayList<MediaType>();

        try {
            for (String mediaType : mediaTypes) {
                HttpHeaderReader.readMediaTypes(result, mediaType);
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }

        Collections.sort(result, PARTIAL_ORDER_COMPARATOR);

        return Collections.unmodifiableList(result);
    }

    /**
     * Create a list of quality source media type from the Produces annotation.
     * <p>
     *
     * @param mime the Produces annotation.
     * @return the list of {@link QualitySourceMediaType}, ordered according to
     * {@link org.glassfish.jersey.message.internal.QualitySourceMediaType#COMPARATOR}.
     */
    public static List<MediaType> createQualitySourceMediaTypes(Produces mime) {
        if (mime == null || mime.value().length == 0) {
            return WILDCARD_QS_TYPE_SINGLETON_LIST;
        }

        return new ArrayList<MediaType>(createQualitySourceMediaTypes(mime.value()));
    }

    /**
     * Create a list of quality source media type from an array of media types.
     * <p>
     *
     * @param mediaTypes the array of media types.
     * @return the list of {@link QualitySourceMediaType}, ordered according to
     * the quality source as the primary key and {@link #PARTIAL_ORDER_COMPARATOR}
     * as the secondary key.
     */
    public static List<QualitySourceMediaType> createQualitySourceMediaTypes(String[] mediaTypes) {
        try {
            return HttpHeaderReader.readQualitySourceMediaType(mediaTypes);
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Reads quality factor from given media type.
     *
     * @param mt media type to read quality parameter from
     * @return quality factor of input media type
     */
    public static int getQuality(MediaType mt) {

        final String qParam = mt.getParameters().get(Quality.QUALITY_PARAMETER_NAME);
        return readQualityFactor(qParam);
    }

    private static int readQualityFactor(final String qParam) throws IllegalArgumentException {
        if (qParam == null) {
            return Quality.DEFAULT;
        } else {
            try {
                return HttpHeaderReader.readQualityFactor(qParam);
            } catch (ParseException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    /**
     * Strips any quality parameters, i.e. q and qs from given media type.
     *
     * @param mediaType type to strip quality parameters from
     * @return media type instance corresponding to the given one with quality parameters stripped off
     * or the original instance if no such parameters are present
     */
    public static MediaType stripQualityParams(MediaType mediaType) {
        final Map<String, String> oldParameters = mediaType.getParameters();
        if (oldParameters.isEmpty()
                || (!oldParameters.containsKey(Quality.QUALITY_SOURCE_PARAMETER_NAME)
                            && !oldParameters.containsKey(Quality.QUALITY_PARAMETER_NAME))) {
            return mediaType;
        }

        return new MediaType(mediaType.getType(), mediaType.getSubtype(),
                Maps.filterKeys(oldParameters, QUALITY_PARAM_FILTERING_PREDICATE));
    }

    /**
     * Returns MediaType with wildcard in subtype.
     *
     * @param mediaType original MediaType.
     * @return MediaType with wildcard in subtype.
     */
    public static MediaType getTypeWildCart(MediaType mediaType) {
        MediaType mt = WILDCARD_SUBTYPE_CACHE.get(mediaType.getType());

        if (mt == null) {
            mt = new MediaType(mediaType.getType(), MediaType.MEDIA_TYPE_WILDCARD);
        }

        return mt;
    }

    /**
     * Convert media types into {@link java.lang.String}. The result string contains
     * media types in the same order, separated by comma ',' and enclosed into quotes.
     * For example for input media types
     * {@link MediaType#TEXT_PLAIN_TYPE}, {@link MediaType#TEXT_PLAIN_TYPE} and
     * {@link MediaType#APPLICATION_JSON_TYPE} the result will be
     * "text/plain", "application/json", "text/html".
     *
     * @param mediaTypes {@link Iterable iterable} with {@link MediaType media types}.
     * @return Media types converted into String.
     */
    public static String convertToString(Iterable<MediaType> mediaTypes) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (MediaType mediaType : mediaTypes) {
            if (!isFirst) {
                sb.append(", ");
            } else {
                isFirst = false;
            }
            sb.append("\"").append(mediaType.toString()).append("\"");
        }
        return sb.toString();

    }

    /**
     * Check if the given media type is a wildcard type.
     *
     * A media type is considered to be a wildcard if either the media type's type or subtype is a wildcard type.
     *
     * @param mediaType media type.
     * @return {@code true} if the media type is a wildcard type, {@code false} otherwise.
     */
    public static boolean isWildcard(final MediaType mediaType) {
        return mediaType.isWildcardType() || mediaType.isWildcardSubtype();
    }
}
