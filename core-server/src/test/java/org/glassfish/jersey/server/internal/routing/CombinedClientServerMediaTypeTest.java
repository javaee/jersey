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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Jakub Podlesak
 */
public class CombinedClientServerMediaTypeTest {

    @Test
    public void testCreate() {
        checkCombination("text/html;q=1", "text/html;qs=1", "text", "html", 1000, 1000, 0);
        checkCombination("text/*;q=0.5", "text/html;qs=0.8", "text", "html", 500, 800, 1);
        checkCombination("*/*;q=0.2", "text/*;qs=0.8", "text", "*", 200, 800, 1);
        checkCombination("text/html;q=0.2", "*/*;qs=0.8", "text", "html", 200, 800, 2);
    }

    @Test
    public void testComparator() {
        final Comparator<CombinedClientServerMediaType> comparator = CombinedClientServerMediaType.COMPARATOR;

        CombinedClientServerMediaType c1 = CombinedClientServerMediaType.create(MediaType.valueOf("text/html"),
                new CombinedClientServerMediaType.EffectiveMediaType("text/html"));
        CombinedClientServerMediaType c2 = CombinedClientServerMediaType.create(MediaType.valueOf("text/html"),
                new CombinedClientServerMediaType.EffectiveMediaType("text/html"));
        assertTrue(comparator.compare(c1, c2) == 0);

        c1 = CombinedClientServerMediaType.create(MediaType.valueOf("text/html"), new CombinedClientServerMediaType
                .EffectiveMediaType("text/html;qs=0.7"));
        c2 = CombinedClientServerMediaType.create(MediaType.valueOf("application/xml"),
                new CombinedClientServerMediaType.EffectiveMediaType("application/xml;qs=0.7"));
        assertTrue(comparator.compare(c1, c2) == 0);

        c1 = CombinedClientServerMediaType.create(MediaType.valueOf("text/html"), new CombinedClientServerMediaType
                .EffectiveMediaType("text/html;qs=0.8"));
        c2 = CombinedClientServerMediaType.create(MediaType.valueOf("application/xml"),
                new CombinedClientServerMediaType.EffectiveMediaType("application/xml;qs=0.7"));
        assertTrue(comparator.compare(c1, c2) > 0);

        c1 = CombinedClientServerMediaType.create(MediaType.valueOf("text/html"), new CombinedClientServerMediaType
                .EffectiveMediaType("text/html;qs=0.7"));
        c2 = CombinedClientServerMediaType.create(MediaType.valueOf("application/xml;q=0.9"),
                new CombinedClientServerMediaType.EffectiveMediaType("application/xml;qs=0.7"));
        assertTrue(comparator.compare(c1, c2) > 0);

        c1 = CombinedClientServerMediaType.create(MediaType.valueOf("text/html"), new CombinedClientServerMediaType
                .EffectiveMediaType("text/html;qs=0.5"));
        c2 = CombinedClientServerMediaType.create(MediaType.valueOf("application/xml;q=0.9"),
                new CombinedClientServerMediaType.EffectiveMediaType("application/xml;qs=0.9"));
        assertTrue(comparator.compare(c1, c2) > 0);

        c1 = CombinedClientServerMediaType.create(MediaType.valueOf("text/*"), new CombinedClientServerMediaType
                .EffectiveMediaType("text/html;qs=0.7"));
        c2 = CombinedClientServerMediaType.create(MediaType.valueOf("application/xml"),
                new CombinedClientServerMediaType.EffectiveMediaType("application/xml;qs=0.7"));
        assertTrue(comparator.compare(c1, c2) < 0);
    }

    public void checkCombination(String clientType, String serverType, String type, String subtype, int q, int qs, int d) {
        MediaType clientMt = MediaType.valueOf(clientType);
        CombinedClientServerMediaType.EffectiveMediaType serverMt = new CombinedClientServerMediaType.EffectiveMediaType
                (serverType);
        CombinedClientServerMediaType combinedType = CombinedClientServerMediaType.create(clientMt, serverMt);
        checkCombinedType(type, subtype, q, qs, d, combinedType);
    }

    public void checkCombinedType(String type, String subtype, int q, int qs, int d, CombinedClientServerMediaType combinedType) {
        assertEquals(q, combinedType.getQ());
        assertEquals(qs, combinedType.getQs());
        assertEquals(d, combinedType.getD());
        assertEquals(type, combinedType.getCombinedMediaType().getType());
        assertEquals(subtype, combinedType.getCombinedMediaType().getSubtype());
    }
}
