/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.QualitySourceMediaType;

/**
 * Represents function S as defined in the Request Matching part of the spec.
 *
 * @author Jakub Podlesak
 */
class CombinedClientServerMediaType {

    private static int wildcardsMatched(MediaType clientMt, MediaType serverMt) {
        return b2i(clientMt.isWildcardType() ^ serverMt.isWildcardType())
                + b2i(clientMt.isWildcardSubtype() ^ serverMt.isWildcardSubtype());
    }

    private static int b2i(boolean b) {
        return b ? 1 : 0;
    }

    private static int m2i(MediaType mt) {
        return 10 * b2i(mt.isWildcardType()) + b2i(mt.isWildcardSubtype());
    }
    MediaType combinedMediaType;
    int q, qs, d;
    public static final CombinedClientServerMediaType NO_MATCH = new CombinedClientServerMediaType();

    private CombinedClientServerMediaType() {
    }

    public static CombinedClientServerMediaType create(MediaType clientMt, MediaType serverMt) {
        if (!clientMt.isCompatible(serverMt)) {
            return NO_MATCH;
        }

        CombinedClientServerMediaType result = new CombinedClientServerMediaType();

        result.combinedMediaType = MediaTypes.stripQualityParams(MediaTypes.mostSpecific(clientMt, serverMt));
        result.d = wildcardsMatched(clientMt, serverMt);
        result.q = MediaTypes.getQuality(clientMt);
        result.qs = QualitySourceMediaType.getQualitySource(serverMt);
        return result;
    }

    private final static Comparator<MediaType> partialOrderComparator = new Comparator<MediaType>() {

        @Override
        public int compare(MediaType o1, MediaType o2) {
            return m2i(o2) - m2i(o1);
        }
    };

    public final static Comparator<CombinedClientServerMediaType> COMPARATOR = new Comparator<CombinedClientServerMediaType>() {

        @Override
        public int compare(CombinedClientServerMediaType c1, CombinedClientServerMediaType c2) {
            int partialComparism = partialOrderComparator.compare(c1.combinedMediaType, c2.combinedMediaType);
            if (partialComparism > 0) {
                return 1;
            } else {
                if (partialComparism == 0) {
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
        return String.format("%s:%d:%d:%d", combinedMediaType, q, qs,d);
    }


}
