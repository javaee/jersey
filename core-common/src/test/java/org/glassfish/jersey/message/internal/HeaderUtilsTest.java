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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * {@link HeaderUtils} unit tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class HeaderUtilsTest {

    public HeaderUtilsTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testCreateInbound() throws Exception {
        final MultivaluedMap<String, String> inbound = HeaderUtils.createInbound();
        assertNotNull(inbound);

        // Test mutability.
        inbound.putSingle("key", "value");
        assertEquals("value", inbound.getFirst("key"));
    }

    @Test
    public void testEmpty() throws Exception {
        final MultivaluedMap<String, String> emptyStrings = HeaderUtils.empty();
        assertNotNull(emptyStrings);

        final MultivaluedMap<String, Object> emptyObjects = HeaderUtils.empty();
        assertNotNull(emptyObjects);

        // Test immutability.
        try {
            emptyStrings.putSingle("key", "value");
            fail("UnsupportedOperationException expected.");
        } catch (UnsupportedOperationException ex) {
            // passed
        }
        try {
            emptyObjects.putSingle("key", "value");
            fail("UnsupportedOperationException expected.");
        } catch (UnsupportedOperationException ex) {
            // passed
        }
    }

    @Test
    public void testCreateOutbound() throws Exception {
        final MultivaluedMap<String, Object> outbound = HeaderUtils.createOutbound();
        assertNotNull(outbound);

        // Test mutability.
        outbound.putSingle("key", "value");
        assertEquals("value", outbound.getFirst("key"));

        final Object value = new Object();
        outbound.putSingle("key", value);
        assertEquals(value, outbound.getFirst("key"));
    }

    @Test
    public void testAsString() throws Exception {
        assertNull(HeaderUtils.asString(null, null));

        final String value = "value";
        assertSame(value, HeaderUtils.asString(value, null));

        final URI uri = new URI("test");
        assertEquals(uri.toASCIIString(), HeaderUtils.asString(uri, null));
    }

    @Test
    public void testAsStringList() throws Exception {
        assertNotNull(HeaderUtils.asStringList(null, null));
        assertTrue(HeaderUtils.asStringList(null, null).isEmpty());

        final URI uri = new URI("test");
        final ArrayList<Object> values = Lists.<Object>newArrayList("value", null, uri);

        // test string values
        final List<String> stringList = HeaderUtils.asStringList(values, null);
        assertEquals(Lists.newArrayList("value", "[null]", uri.toASCIIString()),
                stringList);

        // tests live view
        values.add("value2");
        assertEquals(Lists.newArrayList("value", "[null]", uri.toASCIIString(), "value2"),
                stringList);
        values.remove(1);
        assertEquals(Lists.newArrayList("value", uri.toASCIIString(), "value2"),
                stringList);

    }

    @Test
    public void testAsStringHeaders() throws Exception {
        assertNull(HeaderUtils.asStringHeaders(null));

        final AbstractMultivaluedMap<String, Object> headers = HeaderUtils.createOutbound();

        headers.putSingle("k1", "value");
        headers.add("k1", "value2");

        final URI uri = new URI("test");
        headers.putSingle("k2", uri);

        headers.putSingle("k3", "value3");

        final MultivaluedMap<String, String> stringHeaders = HeaderUtils.asStringHeaders(headers);

        // test string values
        assertEquals(Lists.newArrayList("value", "value2"),
                stringHeaders.get("k1"));
        assertEquals(Lists.newArrayList(uri.toASCIIString()),
                stringHeaders.get("k2"));
        assertEquals(Lists.newArrayList("value3"),
                stringHeaders.get("k3"));

        // test live view
        headers.get("k1").remove(1);
        headers.add("k2", "value4");
        headers.remove("k3");

        assertEquals(Lists.newArrayList("value"),
                stringHeaders.get("k1"));
        assertEquals(Lists.newArrayList(uri.toASCIIString(), "value4"),
                stringHeaders.get("k2"));
        assertFalse(stringHeaders.containsKey("k3"));
    }

    @Test
    public void testAsHeaderString() throws Exception {
        assertNull(HeaderUtils.asHeaderString(null, null));

        final URI uri = new URI("test");
        final ArrayList<Object> values = Lists.<Object>newArrayList("value", null, uri);

        // test string values
        final String result = HeaderUtils.asHeaderString(values, null);
        assertEquals("value,[null]," + uri.toASCIIString(), result);
    }
}
