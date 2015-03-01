/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Comparator;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.Quality;
import org.glassfish.jersey.message.internal.QualitySourceMediaType;

/**
 * Represents function S as defined in the Request Matching part of the spec.
 *
 * @author Jakub Podlesak
 * @author Miroslav Fuksa
 */
final class CombinedMediaType {

    /**
     * Constant combined type representing no match.
     */
    static final CombinedMediaType NO_MATCH = new CombinedMediaType(null, 0, 0, 0);

    private static int matchedWildcards(MediaType clientMt, EffectiveMediaType serverMt) {
        return b2i(clientMt.isWildcardType() ^ serverMt.isWildcardType())
                + b2i(clientMt.isWildcardSubtype() ^ serverMt.isWildcardSubType());
    }

    private static int b2i(boolean b) {
        return b ? 1 : 0;
    }

    /**
     * Combined client/server media type, stripped of q and qs parameters.
     */
    final MediaType combinedType;
    /**
     * Client-specified media type quality.
     */
    final int q;
    /**
     * Server-specified media type quality.
     */
    final int qs;
    /**
     * Distance of the combined media types.
     * <ul>
     * <li>
     * 0 - if the type and subtype of both combined media types match exactly (i.e. ["m/n" + "m/n"]).
     * </li>
     * <li>
     * 1 - if one media type contains a wildcard type or subtype value that matches a concrete type or subtype value.
     * </li>
     * <li>
     * 2 - if one of the media types is a {@link MediaType#WILDCARD_TYPE} and the other one is a concrete media type.
     * </li>
     * </ul>
     */
    final int d;

    private CombinedMediaType(final MediaType combinedType, final int q, final int qs, final int d) {
        this.combinedType = combinedType;
        this.q = q;
        this.qs = qs;
        this.d = d;
    }

    /**
     * Create combined client/server media type.
     *
     * if the two types are not compatible, {@link #NO_MATCH} is returned.
     *
     * @param clientType client-side media type.
     * @param serverType server-side media type.
     * @return combined client/server media type.
     */
    static CombinedMediaType create(MediaType clientType, EffectiveMediaType serverType) {
        if (!clientType.isCompatible(serverType.getMediaType())) {
            return NO_MATCH;
        }

        final MediaType strippedClientType = MediaTypes.stripQualityParams(clientType);
        final MediaType strippedServerType = MediaTypes.stripQualityParams(serverType.getMediaType());

        return new CombinedMediaType(
                MediaTypes.mostSpecific(strippedClientType, strippedServerType),
                MediaTypes.getQuality(clientType),
                QualitySourceMediaType.getQualitySource(serverType.getMediaType()),
                matchedWildcards(clientType, serverType));
    }

    /**
     * Comparator used to compare {@link CombinedMediaType}. The comparator sorts the elements of list
     * in the ascending order from the most appropriate to the least appropriate combined media type.
     */
    static final Comparator<CombinedMediaType> COMPARATOR = new Comparator<CombinedMediaType>() {

        @Override
        public int compare(CombinedMediaType c1, CombinedMediaType c2) {
            // more concrete is better
            int delta = MediaTypes.PARTIAL_ORDER_COMPARATOR.compare(c1.combinedType, c2.combinedType);
            if (delta != 0) {
                return delta;
            }

            // higher is better
            delta = Quality.QUALITY_VALUE_COMPARATOR.compare(c1.q, c2.q);
            if (delta != 0) {
                return delta;
            }

            // higher is better
            delta = Quality.QUALITY_VALUE_COMPARATOR.compare(c1.qs, c2.qs);
            if (delta != 0) {
                return delta;
            }

            // lower is better
            return Integer.compare(c1.d, c2.d);
        }
    };

    @Override
    public String toString() {
        return String.format("%s;q=%d;qs=%d;d=%d", combinedType, q, qs, d);
    }

    /**
     * {@link MediaType Media type} extended by flag indicating whether media type was
     * obtained from user annotations {@link Consumes} or {@link Produces} or has no
     * annotation and therefore was derived from {@link MessageBodyWorkers}.
     */
    static class EffectiveMediaType {

        /**
         * True if the MediaType was not defined by annotation and therefore was
         * derived from Message Body Providers.
         */
        private final boolean derived;
        private final MediaType mediaType;

        /**
         * Creates new instance with {@code mediaType} and flag indicating the origin of
         * the mediaType.
         *
         * @param mediaType                The media type.
         * @param fromMessageBodyProviders True if {@code mediaType} was derived from
         *                                 {@link MessageBodyWorkers}.
         */
        public EffectiveMediaType(MediaType mediaType, boolean fromMessageBodyProviders) {
            this.derived = fromMessageBodyProviders;
            this.mediaType = mediaType;
        }

        /**
         * Creates new instance with {@code mediaType} which was obtained from user
         * annotations {@link Consumes} or {@link Produces}.
         *
         * @param mediaTypeValue The string media type.
         */
        public EffectiveMediaType(String mediaTypeValue) {
            this(MediaType.valueOf(mediaTypeValue), false);
        }

        /**
         * Creates new instance with {@code mediaType} which was obtained from user
         * annotations {@link Consumes} or {@link Produces}.
         *
         * @param mediaType The media type.
         */
        public EffectiveMediaType(MediaType mediaType) {
            this(mediaType, false);
        }

        /**
         * Returns true if Type of {@link MediaType} was originally  defined as wildcard.
         *
         * @return Returns true if method {@link Consumes} or {@link Produces} was
         * annotated with wildcard type (for example '*&#47;*').
         */
        public boolean isWildcardType() {
            return mediaType.isWildcardType();
        }

        /**
         * Returns True if SubType of {@link MediaType} was originally defined as wildcard.
         *
         * @return Returns true if method {@link Consumes} or {@link Produces} was
         * annotated with wildcard subtype (for example 'text&#47;*').
         */
        public boolean isWildcardSubType() {
            return mediaType.isWildcardSubtype();
        }

        /**
         * Returns {@link MediaType}.
         *
         * @return Media type.
         */
        public MediaType getMediaType() {
            return mediaType;
        }

        /**
         * Return flag value whether the {@code MediaType} was not defined by annotation and therefore was derived from
         * Message Body Providers.
         *
         * @return {@code true} if the {@code MediaType} was not defined by annotation and therefore was derived from
         * Message Body Providers, {@code false} otherwise.
         */
        boolean isDerived() {
            return derived;
        }

        @Override
        public String toString() {
            return String.format("mediaType=[%s], fromProviders=%b", mediaType, derived);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EffectiveMediaType)) {
                return false;
            }

            EffectiveMediaType that = (EffectiveMediaType) o;

            return derived == that.derived && mediaType.equals(that.mediaType);
        }

        @Override
        public int hashCode() {
            int result = (derived ? 1 : 0);
            result = 31 * result + mediaType.hashCode();
            return result;
        }
    }

}
