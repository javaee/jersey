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
import java.util.Comparator;
import java.util.Map;

import javax.ws.rs.core.MediaType;

/**
 * A quality source media type.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class QualitySourceMediaType extends MediaType implements Qualified {
    /**
     * Comparator for lists of quality source media types.
     */
    public static final Comparator<QualitySourceMediaType> COMPARATOR =
            new Comparator<QualitySourceMediaType>() {

                @Override
                public int compare(QualitySourceMediaType o1, QualitySourceMediaType o2) {
                    int i = Quality.QUALIFIED_COMPARATOR.compare(o1, o2);
                    if (i != 0) {
                        return i;
                    }
                    return MediaTypes.PARTIAL_ORDER_COMPARATOR.compare(o1, o2);
                }
            };

    private final int qs;

    /**
     * Create new quality source media type instance with a {@link Quality#DEFAULT
     * default quality factor} value.
     *
     * @param type    the primary type, {@code null} is equivalent to
     *                {@link #MEDIA_TYPE_WILDCARD}
     * @param subtype the subtype, {@code null} is equivalent to
     *                {@link #MEDIA_TYPE_WILDCARD}
     */
    public QualitySourceMediaType(String type, String subtype) {
        super(type, subtype); // no need to add default quality parameter.
        qs = Quality.DEFAULT;
    }

    /**
     * Create new quality source media type instance.
     *
     * @param type       the primary type, {@code null} is equivalent to
     *                   {@link #MEDIA_TYPE_WILDCARD}
     * @param subtype    the subtype, {@code null} is equivalent to
     *                   {@link #MEDIA_TYPE_WILDCARD}
     * @param quality    quality source factor value in [ppt]. See {@link Qualified}.
     * @param parameters a map of media type parameters, {@code null} is the same as an
     *                   empty map.
     */
    public QualitySourceMediaType(String type, String subtype, int quality, Map<String, String> parameters) {
        super(type, subtype, Quality.enhanceWithQualityParameter(parameters, Quality.QUALITY_SOURCE_PARAMETER_NAME, quality));
        this.qs = quality;
    }

    // used by QualitySourceMediaType.valueOf method; no need to fix parameter map
    private QualitySourceMediaType(String type, String subtype, Map<String, String> parameters, int quality) {
        super(type, subtype, parameters);
        this.qs = quality;
    }

    /**
     * Get quality source factor value (in [ppt]).
     *
     * @return quality source factor value.
     */
    @Override
    public int getQuality() {
        return qs;
    }

    /**
     * Create new quality source media type instance from the supplied
     * {@link HttpHeaderReader HTTP header reader}.
     *
     * @param reader HTTP header reader.
     * @return new acceptable media type instance.
     *
     * @throws ParseException in case the input data parsing failed.
     */
    public static QualitySourceMediaType valueOf(HttpHeaderReader reader) throws ParseException {
        // Skip any white space
        reader.hasNext();

        // Get the type
        String type = reader.nextToken().toString();
        reader.nextSeparator('/');
        // Get the subtype
        String subType = reader.nextToken().toString();

        int qs = Quality.DEFAULT;
        Map<String, String> parameters = null;
        if (reader.hasNext()) {
            parameters = HttpHeaderReader.readParameters(reader);
            if (parameters != null) {
                qs = getQs(parameters.get(Quality.QUALITY_SOURCE_PARAMETER_NAME));
            }
        }

        // use private constructor to skip quality value validation step
        return new QualitySourceMediaType(type, subType, parameters, qs);
    }

    /**
     * Extract quality source information from the supplied {@link MediaType} value.
     *
     * If no quality source parameter is present in the media type, {@link Quality#DEFAULT
     * default quality} is returned.
     *
     * @param mediaType media type.
     * @return quality source parameter value or {@link Quality#DEFAULT default quality},
     * if no quality source parameter is present.
     *
     * @throws IllegalArgumentException in case the quality source parameter value could not be parsed.
     */
    public static int getQualitySource(final MediaType mediaType) throws IllegalArgumentException {
        if (mediaType instanceof QualitySourceMediaType) {
            return ((QualitySourceMediaType) mediaType).getQuality();
        } else {
            return getQs(mediaType);
        }
    }

    private static int getQs(MediaType mt) throws IllegalArgumentException {
        try {
            return getQs(mt.getParameters().get(Quality.QUALITY_SOURCE_PARAMETER_NAME));
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static int getQs(String v) throws ParseException {
        if (v == null) {
            return Quality.DEFAULT;
        }

        return HttpHeaderReader.readQualityFactor(v);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj instanceof QualitySourceMediaType) {
            final QualitySourceMediaType other = (QualitySourceMediaType) obj;
            return this.qs == other.qs;
        } else {
            // obj is a plain MediaType instance
            // with a quality source factor set to default (1.0)
            return this.qs == Quality.DEFAULT;
        }
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return (this.qs == Quality.DEFAULT) ? hash : 47 * hash + this.qs;
    }

    @Override
    public String toString() {
        return "{" + super.toString() + ", qs=" + qs + "}";
    }
}
