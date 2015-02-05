/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * MediaTypes utility method tests.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oralce.com)
 */
public class MediaTypesTest {

    @Test
    public void testConvertToString() {
        final List<MediaType> emptyList = Lists.newArrayList();
        Assert.assertEquals("", MediaTypes.convertToString(emptyList));


        Assert.assertEquals("\"text/plain\"", MediaTypes.convertToString(Lists.newArrayList(
                MediaType.TEXT_PLAIN_TYPE)));

        Assert.assertEquals("\"text/plain\", \"application/json\"",
                MediaTypes.convertToString(Lists.newArrayList(MediaType.TEXT_PLAIN_TYPE,
                        MediaType.APPLICATION_JSON_TYPE)));

        Assert.assertEquals("\"text/plain\", \"application/json\", \"text/html\"",
                MediaTypes.convertToString(Lists.newArrayList(MediaType.TEXT_PLAIN_TYPE,
                        MediaType.APPLICATION_JSON_TYPE,
                        MediaType.TEXT_HTML_TYPE)));
    }

    @Test
    public void testMostSpecific() {

        MediaType m1;
        MediaType m2;

        /*** wildcard type ***/
        m1 = MediaType.WILDCARD_TYPE;

        // wildcard type #1 - concrete type wins
        m2 = new MediaType("foo", "bar");
        _testMostSpecific(m1, m2, m2);
        _testMostSpecific(m2, m1, m2);

        // wildcard type #2 - wildcard subtype wins
        m2 = new MediaType("foo", "*");
        _testMostSpecific(m1, m2, m2);
        _testMostSpecific(m2, m1, m2);

        // wildcard type #3 - first parameter wins
        m2 = new MediaType("*", "*");
        _testMostSpecific(m1, m2, m1);
        _testMostSpecific(m2, m1, m2);

        /*** wildcard subtype ***/
        m1 = new MediaType("moo", "*");

        // wildcard subtype #1 - concrete type wins
        m2 = new MediaType("foo", "bar");
        _testMostSpecific(m1, m2, m2);
        _testMostSpecific(m2, m1, m2);

        // wildcard subtype #2 - first parameter in method wins
        m2 = new MediaType("foo", "*");
        _testMostSpecific(m1, m2, m1);
        _testMostSpecific(m2, m1, m2);

        /*** concrete types ***/
        // concrete types - first parameter in method wins
        m1 = new MediaType("moo", "boo");
        m2 = new MediaType("foo", "bar");
        _testMostSpecific(m1, m2, m1);
        _testMostSpecific(m2, m1, m2);

        /*** concrete type with parameters ***/
        m1 = new MediaType("foo", "bar", asMap("p1=v1;p2=v2"));

        // concrete type with parameters #1 - wildcard type looses
        m2 = MediaType.WILDCARD_TYPE;
        _testMostSpecific(m1, m2, m1);
        _testMostSpecific(m2, m1, m1);

        // concrete type with parameters #2 - wildcard subtype looses
        m2 = new MediaType("foo", "*");
        _testMostSpecific(m1, m2, m1);
        _testMostSpecific(m2, m1, m1);

        // concrete type with parameters #3 - concrete parameter-less type looses
        m2 = new MediaType("foo", "baz");
        _testMostSpecific(m1, m2, m1);
        _testMostSpecific(m2, m1, m1);

        // concrete type with parameters #4 - type with less parameters type looses
        m2 = new MediaType("foo", "baz", asMap("a1=b1"));
        _testMostSpecific(m1, m2, m1);
        _testMostSpecific(m2, m1, m1);

        // both concrete types with parameters #5 - first parameter in method wins
        m2 = new MediaType("foo", "baz", asMap("a1=b1;a2=b2"));
        _testMostSpecific(m1, m2, m1);
        _testMostSpecific(m2, m1, m2);
    }

    private static void _testMostSpecific(MediaType m1, MediaType m2, MediaType result) {
        assertThat("Unexpected media type selected to be most specific.",
                MediaTypes.mostSpecific(m1, m2), is(result));
    }

    /**
     * Creates a map from HTTP header parameter strings.
     *
     * @param parameters HTTP header parameters string.
     * @return HTTP header parameters map.
     */
    public static Map<String, String> asMap(String parameters) {
        HttpHeaderReader reader = HttpHeaderReader.newInstance(";" + parameters);

        if (reader.hasNext()) {
            try {
                return HttpHeaderReader.readParameters(reader);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        return Collections.emptyMap();
    }
}
