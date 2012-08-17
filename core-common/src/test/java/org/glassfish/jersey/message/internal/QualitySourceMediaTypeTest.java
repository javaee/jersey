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
package org.glassfish.jersey.message.internal;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Quality source media type unit tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@RunWith(Parameterized.class)
public class QualitySourceMediaTypeTest {
    @Parameterized.Parameters
    // expected result, media type, quality source media type
    public static List<Object[]> testBeds() {
        return Arrays.asList(new Object[][]{
                {Boolean.TRUE, MediaType.APPLICATION_JSON_TYPE, new QualitySourceMediaType("application", "json")},
                {Boolean.TRUE, MediaType.APPLICATION_JSON_TYPE, new QualitySourceMediaType("application", "json", 1000, null)},
                {Boolean.FALSE, MediaType.APPLICATION_JSON_TYPE, new QualitySourceMediaType("application", "json", 500, null)},
                {Boolean.FALSE, MediaType.APPLICATION_JSON_TYPE, new QualitySourceMediaType("application", "xml")}
        });
    }

    private final boolean expectEquality;
    private final MediaType mediaType;
    private final QualitySourceMediaType qsMediaType;

    public QualitySourceMediaTypeTest(boolean expectEquality, MediaType mediaType, QualitySourceMediaType qsMediaType) {
        this.expectEquality = expectEquality;
        this.mediaType = mediaType;
        this.qsMediaType = qsMediaType;
    }

    @Test
    public void testEquals() throws Exception {
        if (expectEquality) {
            Assert.assertEquals("Types not equal.", mediaType, qsMediaType);
            Assert.assertEquals("Types not equal.", qsMediaType, mediaType);
            Assert.assertEquals(
                    String.format("Hash codes not equal for %s and %s.", mediaType.toString(), qsMediaType.toString()),
                    mediaType.hashCode(), qsMediaType.hashCode());
        } else {
            Assert.assertFalse(String.format("False equality of %s and %s", mediaType.toString(), qsMediaType.toString()),
                    qsMediaType.equals(mediaType));
        }
    }
}
