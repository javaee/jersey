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

import java.text.ParseException;
import java.util.Map;

import javax.ws.rs.core.MediaType;

/**
 * A quality source media type.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class QualitySourceMediaType extends MediaType {
    /**
     * Quality source header value parameter name.
     */
    public static final String QUALITY_SOURCE_PARAMETER_NAME = "qs";

    private final int qs;

    /**
     * Create new quality source media type instance with a {@link Quality#DEFAULT_QUALITY
     * default quality factor} value.
     *
     * @param type    the primary type, {@code null} is equivalent to
     *                {@link #MEDIA_TYPE_WILDCARD}
     * @param subtype the subtype, {@code null} is equivalent to
     *                {@link #MEDIA_TYPE_WILDCARD}
     */
    public QualitySourceMediaType(String type, String subtype) {
        super(type, subtype);
        qs = Quality.DEFAULT_QUALITY;
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
        super(type, subtype, parameters);
        this.qs = quality;
    }

    /**
     * Get quality source factor value (in [ppt]).
     *
     * @return quality source factor value.
     */
    public int getQualitySource() {
        return qs;
    }

    /**
     * Create new quality source media type instance from the supplied
     * {@link HttpHeaderReader HTTP header reader}.
     *
     * @param reader HTTP header reader.
     * @return new acceptable media type instance.
     * @throws ParseException in case the input data parsing failed.
     */
    public static QualitySourceMediaType valueOf(HttpHeaderReader reader) throws ParseException {
        // Skip any white space
        reader.hasNext();

        // Get the type
        String type = reader.nextToken();
        reader.nextSeparator('/');
        // Get the subtype
        String subType = reader.nextToken();

        int qs = Quality.DEFAULT_QUALITY;
        Map<String, String> parameters = null;
        if (reader.hasNext()) {
            parameters = HttpHeaderReader.readParameters(reader);
            if (parameters != null) {
                qs = getQs(parameters.get(QUALITY_SOURCE_PARAMETER_NAME));
            }
        }

        return new QualitySourceMediaType(type, subType, qs, parameters);
    }

    /**
     * Extract quality source information from the supplied {@link MediaType} value.
     *
     * If no quality source parameter is present in the media type, {@link Quality#DEFAULT_QUALITY
     * default quality} is returned.
     *
     * @param mediaType media type.
     * @return quality source parameter value or {@link Quality#DEFAULT_QUALITY default quality},
     *         if no quality source parameter is present.
     * @throws IllegalArgumentException in case the quality source parameter value could not be parsed.
     */
    public static int getQualitySource(MediaType mediaType) throws IllegalArgumentException {
        if (mediaType instanceof QualitySourceMediaType) {
            QualitySourceMediaType qsmt = (QualitySourceMediaType) mediaType;
            return qsmt.getQualitySource();
        } else {
            return getQs(mediaType);
        }
    }

    private static int getQs(MediaType mt) throws IllegalArgumentException {
        try {
            return getQs(mt.getParameters().get(QUALITY_SOURCE_PARAMETER_NAME));
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static int getQs(String v) throws ParseException {
        if (v == null) {
            return Quality.DEFAULT_QUALITY;
        }

        try {
            final int qs = (int) (Float.valueOf(v) * 1000.0);
            if (qs < 0) {
                throw new ParseException("The quality source (qs) value, " + v + ", must be non-negative number", 0);
            }
            return qs;
        } catch (NumberFormatException ex) {
            ParseException pe = new ParseException("The quality source (qs) value, " + v + ", is not a valid value", 0);
            pe.initCause(ex);
            throw pe;
        }
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
            return this.qs == Quality.DEFAULT_QUALITY;
        }
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return (this.qs == Quality.DEFAULT_QUALITY) ? hash : 47 * hash + this.qs;
    }
}
