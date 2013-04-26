/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.jersey.message.internal.QualitySourceMediaType;

/**
 * Represents function S as defined in the Request Matching part of the spec.
 *
 * @author Jakub Podlesak
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
class CombinedClientServerMediaType {

    private static int wildcardsMatched(MediaType clientMt, EffectiveMediaType serverMt) {
        return b2i(clientMt.isWildcardType() ^ serverMt.isWildcardType())
                + b2i(clientMt.isWildcardSubtype() ^ serverMt.isWildcardSubType());
    }

    private static int b2i(boolean b) {
        return b ? 1 : 0;
    }

    private static int m2i(MediaType mt) {
        return 10 * b2i(mt.isWildcardType()) + b2i(mt.isWildcardSubtype());
    }

    private MediaType combinedMediaType;
    private int q, qs, d;
    private static final CombinedClientServerMediaType NO_MATCH = new CombinedClientServerMediaType();

    private CombinedClientServerMediaType() {
    }

    /**
     * Create combined client/server media type.
     *
     * @param clientMt client-side media type.
     * @param serverMt server-side media type.
     * @return combined client/server media type.
     */
    public static CombinedClientServerMediaType create(MediaType clientMt, EffectiveMediaType serverMt) {
        if (!clientMt.isCompatible(serverMt.getMediaType())) {
            return NO_MATCH;
        }

        CombinedClientServerMediaType result = new CombinedClientServerMediaType();

        result.combinedMediaType = MediaTypes.stripQualityParams(MediaTypes.mostSpecific(clientMt, serverMt.getMediaType()));
        result.d = wildcardsMatched(clientMt, serverMt);
        result.q = MediaTypes.getQuality(clientMt);
        result.qs = QualitySourceMediaType.getQualitySource(serverMt.getMediaType());
        return result;
    }

    private final static Comparator<MediaType> partialOrderComparator = new Comparator<MediaType>() {

        @Override
        public int compare(MediaType o1, MediaType o2) {
            return m2i(o2) - m2i(o1);
        }
    };


    /**
     * Comparator used to compare {@link CombinedClientServerMediaType}. The comparator sorts the elements of list
     * in the ascending order from the least appropriate to the most appropriate {@link MediaType media type}.
     */
    final static Comparator<CombinedClientServerMediaType> COMPARATOR = new Comparator<CombinedClientServerMediaType>() {

        @Override
        public int compare(CombinedClientServerMediaType c1, CombinedClientServerMediaType c2) {
            int partialComparison = partialOrderComparator.compare(c1.combinedMediaType, c2.combinedMediaType);

            if (partialComparison > 0) {
                return 1;
            } else {
                if (partialComparison == 0) {
                    if (c1.q > c2.q) {
                        return 1;
                    } else if (c1.q == c2.q) {
                        if (c1.qs > c2.qs) {
                            return 1;
                        } else if (c1.qs == c2.qs) {
                            return c2.d - c1.d;
                        }
                    }
                }
            }
            return -1;
        }
    };

    @Override
    public String toString() {
        return String.format("%s:%d:%d:%d", combinedMediaType, q, qs, d);
    }


    int getQ() {
        return q;
    }

    int getQs() {
        return qs;
    }

    int getD() {
        return d;
    }

    MediaType getCombinedMediaType() {
        return combinedMediaType;
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
         *         annotated with wildcard type (for example '*&#47;*').
         */
        public boolean isWildcardType() {
            return mediaType.isWildcardType();
        }

        /**
         * Returns True if SubType of {@link MediaType} was originally defined as wildcard.
         *
         * @return Returns true if method {@link Consumes} or {@link Produces} was
         *         annotated with wildcard subtype (for example 'text&#47;*').
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
            if (this == o) return true;
            if (!(o instanceof EffectiveMediaType)) return false;

            EffectiveMediaType that = (EffectiveMediaType) o;

            if (derived != that.derived) return false;
            if (!mediaType.equals(that.mediaType)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (derived ? 1 : 0);
            result = 31 * result + mediaType.hashCode();
            return result;
        }
    }

}
