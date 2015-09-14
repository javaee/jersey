/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


/**
 * Acceptable media type unit tests.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@RunWith(Parameterized.class)
public class AcceptableMediaTypeStringRepresentationTest {
    @Parameterized.Parameters
    // expected result, acceptable media type
    public static List<Object[]> getParameters() {
        final Map<String, String> emptyParams = new HashMap<String, String>();
        final Map<String, String> params = new HashMap<String, String>();
        params.put("myParam", "myValue");

        return Arrays.asList(new Object[][]{
                {"*/*", new AcceptableMediaType("*", "*")},
                {"*/*", new AcceptableMediaType("*", "*", Quality.DEFAULT, emptyParams)},
                {"*/*;q=0.75", new AcceptableMediaType("*", "*", 750, emptyParams)},
                {"text/html", new AcceptableMediaType("text", "html", Quality.DEFAULT, null)},
                {"text/html;q=0.5", new AcceptableMediaType("text", "html", 500, emptyParams)},
                {"image/*;myparam=myValue;q=0.8", new AcceptableMediaType("image", "*", 800, params)},
        });
    }

    private final String expectedValue;
    private final AcceptableMediaType testedType;

    public AcceptableMediaTypeStringRepresentationTest(final String expectedValue,
                                                       final AcceptableMediaType testedType) {
        this.expectedValue = expectedValue;
        this.testedType = testedType;
    }

    @Test
    public void testStringRepresentation() {
        final MediaTypeProvider provider = new MediaTypeProvider();
        Assert.assertEquals(expectedValue, testedType.toString());
        provider.fromString(testedType.toString());
    }
}
