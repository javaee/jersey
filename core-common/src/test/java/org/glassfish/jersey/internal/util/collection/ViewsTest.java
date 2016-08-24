/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal.util.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ViewsTest {

    @Test
    public void testTransformingListViewSize() {
        List<String> strings = new ArrayList<>();
        List<Integer> ints = Views.listView(strings, Integer::parseInt);

        assertEquals(strings.size(), ints.size());
        assertEquals(0, ints.size());
        strings.add("1");
        assertEquals(strings.size(), ints.size());
        assertEquals(1, ints.size());
        strings.remove("1");
        assertEquals(strings.size(), ints.size());
        assertEquals(0, ints.size());
    }

    @Test
    public void testTransformingListViewContent() {
        List<String> strings = new ArrayList<>();
        List<Integer> ints = Views.listView(strings, Integer::parseInt);

        strings.add("1");
        strings.add("2");
        strings.add("3");

        assertEquals(Integer.valueOf(1), ints.get(0));
        assertEquals(Integer.valueOf(2), ints.get(1));
        assertEquals(Integer.valueOf(3), ints.get(2));

        strings.remove("2");

        assertEquals(Integer.valueOf(1), ints.get(0));
        assertEquals(Integer.valueOf(3), ints.get(1));
    }

    @Test
    public void testTransformingListViewRemove() {
        List<String> strings = new ArrayList<>();
        List<Integer> ints = Views.listView(strings, Integer::parseInt);

        strings.add("1");
        strings.add("2");
        strings.add("3");

        ints.remove(Integer.valueOf(1));

        assertEquals(2, ints.size());
        assertEquals(Integer.valueOf(2), ints.get(0));
        assertEquals(Integer.valueOf(3), ints.get(1));
        assertEquals("2", strings.get(0));
        assertEquals("3", strings.get(1));
    }

    @Test
    public void testTransformingListViewModifyAdd() {
        List<String> strings = new ArrayList<>();
        List<Integer> ints = Views.listView(strings, Integer::parseInt);

        strings.add("1");
        strings.add("2");
        strings.add("3");

        try {
            ints.add(4);
        } catch (UnsupportedOperationException e) {
            return;
        }

        fail("UnsupportedOperationException expected.");
    }

    @Test
    public void testTransformingListViewModifySet() {
        List<String> strings = new ArrayList<>();
        List<Integer> ints = Views.listView(strings, Integer::parseInt);

        strings.add("1");
        strings.add("2");
        strings.add("3");

        try {
            ints.set(0, 4);
        } catch (UnsupportedOperationException e) {
            return;
        }

        fail("UnsupportedOperationException expected.");
    }

    @Test
    public void testTransformingMapViewSize() {
        Map<String, String> stringMap = new HashMap<>();
        Map<String, Integer> intMap = Views.mapView(stringMap, Integer::parseInt);

        assertEquals(stringMap.size(), intMap.size());
        assertEquals(0, intMap.size());
        stringMap.put("key1", "1");
        assertEquals(stringMap.size(), intMap.size());
        assertEquals(1, intMap.size());
        stringMap.remove("key1");
        assertEquals(stringMap.size(), intMap.size());
        assertEquals(0, intMap.size());
    }

    @Test
    public void testTransformingMapViewContent() {
        Map<String, String> stringMap = new HashMap<>();
        Map<String, Integer> intMap = Views.mapView(stringMap, Integer::parseInt);

        stringMap.put("key1", "1");
        stringMap.put("key2", "2");
        stringMap.put("key3", "3");

        assertEquals(Integer.valueOf(1), intMap.get("key1"));
        assertEquals(Integer.valueOf(2), intMap.get("key2"));
        assertEquals(Integer.valueOf(3), intMap.get("key3"));

        stringMap.remove("key2");

        assertEquals(Integer.valueOf(1), intMap.get("key1"));
        assertEquals(Integer.valueOf(3), intMap.get("key3"));
    }

    @Test
    public void testTransformingMapViewRemove() {
        Map<String, String> stringMap = new HashMap<>();
        Map<String, Integer> intMap = Views.mapView(stringMap, Integer::parseInt);

        stringMap.put("key1", "1");
        stringMap.put("key2", "2");
        stringMap.put("key3", "3");

        intMap.remove("key2");

        assertEquals(2, intMap.size());
        assertEquals(Integer.valueOf(1), intMap.get("key1"));
        assertEquals(Integer.valueOf(3), intMap.get("key3"));
        assertEquals("1", stringMap.get("key1"));
        assertEquals("3", stringMap.get("key3"));
    }

    @Test
    public void testTransformingMapViewModifyAdd() {
        Map<String, String> stringMap = new HashMap<>();
        Map<String, Integer> intMap = Views.mapView(stringMap, Integer::parseInt);

        stringMap.put("key1", "1");
        stringMap.put("key2", "2");
        stringMap.put("key3", "3");

        try {
            intMap.put("key4", 4);
        } catch (UnsupportedOperationException e) {
            return;
        }

        fail("UnsupportedOperationException expected.");
    }

    @Test
    public void testTransformingMapViewModifySet() {
        Map<String, String> stringMap = new HashMap<>();
        Map<String, Integer> intMap = Views.mapView(stringMap, Integer::parseInt);

        stringMap.put("key1", "1");
        stringMap.put("key2", "2");
        stringMap.put("key3", "3");

        try {
            intMap.entrySet().iterator().next().setValue(4);
        } catch (UnsupportedOperationException e) {
            return;
        }

        fail("UnsupportedOperationException expected.");
    }

    @Test
    public void testSetUnionViewNulls() {
        // should pass
        Set<Object> union = Views.setUnionView(Collections.emptySet(), Collections.emptySet());

        assertNotNull(union);

        // should fail
        try {
            Views.setUnionView(Collections.emptySet(), null);
            fail();
        } catch (NullPointerException e) {
            // expected.
        }

        // should fail
        try {
            Views.setUnionView(null, Collections.emptySet());
            fail();
        } catch (NullPointerException e) {
            // expected.
        }
    }

    @Test
    public void testSetUnionViewModify() {
        HashSet<String> set1 = new HashSet<>();
        HashSet<String> set2 = new HashSet<>();

        // should pass
        Set<Object> union = Views.setUnionView(set1, set2);

        assertNotNull(union);
        assertEquals(0, union.size());

        set1.add("test1");
        set2.add("test2");

        assertEquals(2, union.size());

        set1.clear();

        assertEquals(1, union.size());
    }

    @Test
    public void testSetDiffViewNulls() {
        // should pass
        Set<Object> diff = Views.setDiffView(Collections.emptySet(), Collections.emptySet());

        assertNotNull(diff);

        // should fail
        try {
            Views.setDiffView(Collections.emptySet(), null);
            fail();
        } catch (NullPointerException e) {
            // expected.
        }

        // should fail
        try {
            Views.setDiffView(null, Collections.emptySet());
            fail();
        } catch (NullPointerException e) {
            // expected.
        }
    }

    @Test
    public void testSetDiffViewModify() {
        HashSet<String> set1 = new HashSet<>();
        HashSet<String> set2 = new HashSet<>();

        // should pass
        Set<Object> diff = Views.setDiffView(set1, set2);

        assertNotNull(diff);
        assertEquals(0, diff.size());

        set1.add("test1");
        set1.add("test2");
        set1.add("test3");

        set2.add("test3");
        set2.add("test4");
        set2.add("test5");

        assertEquals(2, diff.size());

        set1.clear();

        assertEquals(0, diff.size());
    }

    @Test
    public void testSetDiffViewEmpty() {
        Set<String> set1 = Collections.emptySet();
        HashSet<String> set2 = new HashSet<>();

        // should pass
        Set<Object> diff = Views.setDiffView(set1, set2);

        assertNotNull(diff);
        assertEquals(0, diff.size());

        set2.add("test3");
        set2.add("test4");
        set2.add("test5");

        assertEquals(0, diff.size());

        set2.clear();

        assertEquals(0, diff.size());
    }
}
