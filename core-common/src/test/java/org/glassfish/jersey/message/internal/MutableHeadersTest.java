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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.internal.util.collection.StringKeyIgnoreCaseMultivaluedMap;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Tests for {@code MutableHeaders} class.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class MutableHeadersTest {

    private final MutableHeaders mutableHeaders = new MutableHeaders();

    @Test
    public void testHeader() throws Exception {
        final String nullHeader = mutableHeaders.header("key");
        assertNull(nullHeader);

        mutableHeaders.headers().putSingle("key", "value1");
        final String header = mutableHeaders.header("key");
        assertEquals("value1", header);

        mutableHeaders.headers().add("key", "value2");
        final String multipleHeader = mutableHeaders.header("key");
        assertEquals("value1,value2", multipleHeader);

        mutableHeaders.headers().put("key", Arrays.asList("value3", "value4"));
        final String listHeader = mutableHeaders.header("key");
        assertEquals("value3,value4", listHeader);
    }

    @Test
    public void testHeaders() throws Exception {
        final MultivaluedMap<String, String> headersList = mutableHeaders.headers();
        assertNotNull(headersList);
        assertEquals(0, headersList.size());

        // Mutability of MultivaluedMap returned from #headers() tested in #testHeader().
    }

    @Test
    public void testHeaderValues() throws Exception {
        final List<String> nullHeader = mutableHeaders.headerValues("key");
        assertNull(nullHeader);

        mutableHeaders.headers().add("key", "value1");
        List<String> headersList = mutableHeaders.headerValues("key");
        assertNotNull(headersList);
        assertEquals(1, headersList.size());
        assertEquals("value1", headersList.get(0));

        headersList.add("value2");

        final String listHeader = mutableHeaders.header("key");
        headersList = mutableHeaders.headerValues("key");
        assertEquals("value1,value2", listHeader);
        assertEquals(2, headersList.size());
    }

    @Test
    public void testHeaderObject() throws Exception {
        mutableHeaders.header("key", 1);
        mutableHeaders.header("key", 2);

        final String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());
    }

    @Test
    public void testHeaderString() throws Exception {
        mutableHeaders.header("key", "1");
        mutableHeaders.header("key", "2");

        final String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());
    }

    @Test
    public void testHeadersStrings() throws Exception {
        mutableHeaders.headers("key", "1", "2");

        final String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());
    }

    @Test
    public void testHeadersObjects() throws Exception {
        mutableHeaders.headers("key", 1, 2);

        final String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());
    }

    @Test
    public void testHeadersIterable() throws Exception {
        mutableHeaders.headers("key", Arrays.asList(1, 2));

        final String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());
    }

    @Test
    public void testHeadersMultivaluedMap() throws Exception {
        final MultivaluedMap<String, Object> multivaluedMap = new
                StringKeyIgnoreCaseMultivaluedMap<Object>();

        multivaluedMap.add("key", 1);
        multivaluedMap.add("key", "2");

        mutableHeaders.headers(multivaluedMap);

        final String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());
    }

    @Test
    public void testHeadersMap() throws Exception {
        final Map<String, List<String>> map = new HashMap<String, List<String>>();

        map.put("key", Arrays.<String>asList("1", "2"));

        mutableHeaders.headers(map);

        final String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());
    }

    @Test
    public void testRemove() throws Exception {
        mutableHeaders.headers("key", "1", "2");

        String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());

        mutableHeaders.remove("invalidKey");
        header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());

        mutableHeaders.remove("key");
        header = mutableHeaders.header("key");
        assertNull(header);
        assertEquals(0, mutableHeaders.headers().size());
    }

    @Test
    public void testReplace() throws Exception {
        mutableHeaders.headers("key", "1", "2");

        String header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());

        final List<String> newValues = Arrays.asList("3", "4");

        mutableHeaders.replace("invalidKey", newValues);
        header = mutableHeaders.header("key");
        assertEquals("1,2", header);
        assertEquals(2, mutableHeaders.headers().size());

        mutableHeaders.replace("key", newValues);
        header = mutableHeaders.header("key");
        assertEquals("3,4", header);
        assertEquals(2, mutableHeaders.headers().size());
    }

    @Test
    public void testReplaceAll() throws Exception {
        mutableHeaders.headers("key1", "1", "2");
        mutableHeaders.headers("key2", 3);

        assertEquals(2, mutableHeaders.headers().size());

        final List<String> newValues1 = Arrays.asList("4");
        final List<String> newValues2 = Arrays.asList("5", "6");

        final MultivaluedMap<String, String> multivaluedMap = new
                StringKeyIgnoreCaseMultivaluedMap<String>();
        multivaluedMap.put("key1", newValues1);
        multivaluedMap.put("key2", newValues2);

        mutableHeaders.replaceAll(multivaluedMap);
        String header = mutableHeaders.header("key1");
        assertEquals("4", header);

        header = mutableHeaders.header("key2");
        assertEquals("5,6", header);
        assertEquals(2, mutableHeaders.headers().size());
    }

    @Test
    public void testCopyConstructor() throws Exception {
        mutableHeaders.headers("key", "1", "2");

        final MutableHeaders mutableHeaders1 = new MutableHeaders(mutableHeaders);

        final String header = mutableHeaders1.header("key");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders1.headers().size());
    }

    @Test
    public void testNullHeader() throws Exception {
        mutableHeaders.headers("key", (Object) null);

        final String header = mutableHeaders.header("key");
        assertEquals(null, header);
        assertEquals(0, mutableHeaders.headers().size());
    }

    @Test
    public void testCaseInsensitivity() throws Exception {
        mutableHeaders.headers("key", "1", "2");

        String header = mutableHeaders.header("KEy");
        assertEquals("1,2", header);
        assertEquals(1, mutableHeaders.headers().size());

        mutableHeaders.remove("KeY");

        header = mutableHeaders.header("kEy");
        assertEquals(null, header);
        assertEquals(0, mutableHeaders.headers().size());
    }

    @Test
    public void testObjectHeadersToStringHeaders() throws Exception {
        mutableHeaders.header("key", 1);
        mutableHeaders.headers().putSingle("key", "2");

        final String multipleHeader = mutableHeaders.header("key");
        assertEquals("2", multipleHeader);
    }

}
