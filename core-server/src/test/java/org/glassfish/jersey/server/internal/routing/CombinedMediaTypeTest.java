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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * Combined media type tests.
 *
 * @author Jakub Podlesak
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class CombinedMediaTypeTest {

    @Test
    public void testCreate() {
        checkCombination("text/html;q=1", "text/html;qs=1", "text", "html", 1000, 1000, 0);
        checkCombination("text/*;q=0.5", "text/html;qs=0.8", "text", "html", 500, 800, 1);
        checkCombination("*/*;q=0.2", "text/*;qs=0.8", "text", "*", 200, 800, 1);
        checkCombination("text/html;q=0.2", "*/*;qs=0.8", "text", "html", 200, 800, 2);
    }

    @Test
    public void testCombinedMediaTypeComparator() {
        final Comparator<CombinedMediaType> comparator = CombinedMediaType.COMPARATOR;

        CombinedMediaType c1;
        CombinedMediaType c2;

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html"));
        assertThat(comparator.compare(c1, c2), equalTo(0));

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html;qs=0.7"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("application/xml"), new CombinedMediaType.EffectiveMediaType("application/xml;qs=0.7"));
        assertThat(comparator.compare(c1, c2), equalTo(0));

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html;q=0.5"), new CombinedMediaType.EffectiveMediaType("text/html"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("application/xml;q=0.5"), new CombinedMediaType.EffectiveMediaType("application/xml"));
        assertThat(comparator.compare(c1, c2), equalTo(0));

        // lower means better, higher means worse...

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/html"));
        assertThat(comparator.compare(c1, c2), lessThan(0));

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/html"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("*/*"), new CombinedMediaType.EffectiveMediaType("text/html"));
        assertThat(comparator.compare(c1, c2), lessThan(0));

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/*"));
        assertThat(comparator.compare(c1, c2), lessThan(0));

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html;qs=0.5"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/*"));
        assertThat(comparator.compare(c1, c2), lessThan(0));

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html;q=0.5"), new CombinedMediaType.EffectiveMediaType("text/html"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/*"));
        assertThat(comparator.compare(c1, c2), lessThan(0));

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html;qs=0.8"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("application/xml"), new CombinedMediaType.EffectiveMediaType("application/xml;qs=0.7"));
        assertThat(comparator.compare(c1, c2), lessThan(0));

        c1 = CombinedMediaType.create(
                MediaType.valueOf("text/html;q=0.8"), new CombinedMediaType.EffectiveMediaType("text/html"));
        c2 = CombinedMediaType.create(
                MediaType.valueOf("application/xml;q=0.7"), new CombinedMediaType.EffectiveMediaType("application/xml"));
        assertThat(comparator.compare(c1, c2), lessThan(0));
    }

    @Test
    public void testCombinedMediaTypesSorting() {
        final List<CombinedMediaType> sorted = Arrays.asList(
                CombinedMediaType.create(
                        MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html")),
                CombinedMediaType.create(
                        MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/html")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*"), new CombinedMediaType.EffectiveMediaType("text/html")),
                CombinedMediaType.create(
                        MediaType.valueOf("text/html"), new CombinedMediaType.EffectiveMediaType("text/html;qs=0.5")),
                CombinedMediaType.create(
                        MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/html;qs=0.5")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*"), new CombinedMediaType.EffectiveMediaType("text/html;qs=0.5")),
                CombinedMediaType.create(
                        MediaType.valueOf("text/html;q=0.5"), new CombinedMediaType.EffectiveMediaType("text/html")),
                CombinedMediaType.create(
                        MediaType.valueOf("text/*;q=0.5"), new CombinedMediaType.EffectiveMediaType("text/html")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*;q=0.5"), new CombinedMediaType.EffectiveMediaType("text/html")),
                CombinedMediaType.create(
                        MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/*")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*"), new CombinedMediaType.EffectiveMediaType("text/*")),
                CombinedMediaType.create(
                        MediaType.valueOf("text/*"), new CombinedMediaType.EffectiveMediaType("text/*;qs=0.5")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*"), new CombinedMediaType.EffectiveMediaType("text/*;qs=0.5")),
                CombinedMediaType.create(
                        MediaType.valueOf("text/*;q=0.5"), new CombinedMediaType.EffectiveMediaType("text/*")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*;q=0.5"), new CombinedMediaType.EffectiveMediaType("text/*")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*"), new CombinedMediaType.EffectiveMediaType("*/*")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*"), new CombinedMediaType.EffectiveMediaType("*/*;qs=0.5")),
                CombinedMediaType.create(
                        MediaType.valueOf("*/*;q=0.5"), new CombinedMediaType.EffectiveMediaType("*/*"))
        );

        List<CombinedMediaType> unsorted = new ArrayList<>(sorted);
        Collections.shuffle(unsorted);
        Collections.sort(unsorted, CombinedMediaType.COMPARATOR);

        assertThat("Combined media type sorting has failed.", unsorted, equalTo(sorted));
    }

    private static void checkCombination(final String clientType, final String serverType,
                                         final String type, final String subtype, final int q, final int qs, final int d) {

        final CombinedMediaType combinedType = CombinedMediaType.create(
                MediaType.valueOf(clientType),
                new CombinedMediaType.EffectiveMediaType(serverType));

        assertThat("Unexpected value of quality parameter.", combinedType.q, equalTo(q));
        assertThat("Unexpected value of quality source parameter.", combinedType.qs, equalTo(qs));
        assertThat("Unexpected value of distance parameter.", combinedType.d, equalTo(d));
        assertThat("Unexpected value of combined media type main type.", combinedType.combinedType.getType(), equalTo(type));
        assertThat("Unexpected value of combined media type subtype.", combinedType.combinedType.getSubtype(), equalTo(subtype));
    }
}
