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

package org.glassfish.jersey.internal.util;

import java.util.Map;

import javax.ws.rs.RuntimeType;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa
 *
 */
public class PropertiesHelperTest {

    @Test
    public void testGetValueWithType() {
        Map<String, String> properties = Maps.newHashMap();
        final String key = "my.property";
        properties.put(key, "15");

        assertEquals("15", PropertiesHelper.getValue(properties, key, String.class, null));
        assertEquals(Integer.valueOf(15), PropertiesHelper.getValue(properties, key, Integer.class, null));
        assertEquals(Long.valueOf(15), PropertiesHelper.getValue(properties, key, Long.class, null));
    }

    @Test
    public void testGetValueWithTypeAndDefaultValue() {
        Map<String, String> properties = Maps.newHashMap();
        final String key = "my.property";
        properties.put(key, "15");

        assertEquals("15", PropertiesHelper.getValue(properties, key, "80", String.class, null));
        assertEquals(Integer.valueOf(30), PropertiesHelper.getValue(properties, "non.existing", 30, Integer.class, null));
        assertEquals(Long.valueOf(20), PropertiesHelper.getValue(properties, "non.existing", 20L, Long.class, null));
    }

    @Test
    public void testGetValueWithDefaultValue() {
        Map<String, String> properties = Maps.newHashMap();
        final String key = "my.property";
        properties.put(key, "15");

        assertEquals("15", PropertiesHelper.getValue(properties, key, "80", null));
        assertEquals(Integer.valueOf(30), PropertiesHelper.getValue(properties, "non.existing", 30, null));
        assertEquals(Long.valueOf(20), PropertiesHelper.getValue(properties, "non.existing", 20L, null));
    }

    @Test
    public void testGetValueByIgnoredRuntime() {
        Map<String, String> properties = Maps.newHashMap();
        final String key = "my.property";
        properties.put(key, "15");

        assertEquals("15", PropertiesHelper.getValue(properties, RuntimeType.CLIENT, key, String.class, null));
        assertEquals(Integer.valueOf(15), PropertiesHelper.getValue(properties, RuntimeType.CLIENT, key, Integer.class,
                null));
        assertEquals(Long.valueOf(15), PropertiesHelper.getValue(properties, RuntimeType.SERVER, key, Long.class, null));
    }

    @Test
    public void testGetValueByRuntime1() {
        Map<String, String> properties = Maps.newHashMap();
        final String key = "jersey.config.my.property";
        properties.put(key, "15");
        properties.put("jersey.config.client.my.property", "999");
        properties.put("jersey.config.server.my.property", "1");

        assertEquals("999", PropertiesHelper.getValue(properties, RuntimeType.CLIENT, key, String.class, null));
        assertEquals(Integer.valueOf(999), PropertiesHelper.getValue(properties, RuntimeType.CLIENT, key, Integer.class,
                null));
        assertEquals(Long.valueOf(1), PropertiesHelper.getValue(properties, RuntimeType.SERVER, key, Long.class, null));

        assertEquals("15", PropertiesHelper.getValue(properties, key, String.class, null));
        assertEquals(Integer.valueOf(15), PropertiesHelper.getValue(properties, key, Integer.class, null));
        assertEquals(Long.valueOf(15), PropertiesHelper.getValue(properties, key, Long.class, null));
    }

    @Test
    public void testGetValueByRuntime2() {
        Map<String, String> properties = Maps.newHashMap();
        final String key = "jersey.config.my.property";
        properties.put(key, "15");
        properties.put("jersey.config.client.my.property", "999");

        assertEquals("999", PropertiesHelper.getValue(properties, RuntimeType.CLIENT, key, String.class, null));
        assertEquals(Integer.valueOf(999), PropertiesHelper.getValue(properties, RuntimeType.CLIENT, key, Integer.class,
                null));
        assertEquals(Long.valueOf(15), PropertiesHelper.getValue(properties, RuntimeType.SERVER, key, Long.class, null));
        assertEquals(Long.valueOf(15), PropertiesHelper.getValue(properties, RuntimeType.SERVER, key, 800L, Long.class,
                null));

        assertEquals("15", PropertiesHelper.getValue(properties, key, String.class, null));
        assertEquals(Integer.valueOf(15), PropertiesHelper.getValue(properties, key, Integer.class, null));
        assertEquals(Long.valueOf(15), PropertiesHelper.getValue(properties, key, Long.class, null));
    }

    @Test
    public void testGetValueByRuntime3() {
        Map<String, String> properties = Maps.newHashMap();
        final String key = "jersey.config.my.property";
        properties.put("jersey.config.client.my.property", "999");

        assertEquals("999", PropertiesHelper.getValue(properties, RuntimeType.CLIENT, key, String.class, null));
        assertNull(PropertiesHelper.getValue(properties, key, String.class, null));
        assertNull(PropertiesHelper.getValue(properties, RuntimeType.SERVER, key, String.class, null));
        assertEquals("55", PropertiesHelper.getValue(properties, key, "55", String.class, null));
    }

    /**
     * There is a value but of different type and no way how to transform.
     */
    @Test
    public void testGetValueNoTransformation() {
        Map<String, Object> properties = Maps.newHashMap();
        final String key = "my.property";
        properties.put(key, Boolean.TRUE);

        assertNull(PropertiesHelper.getValue(properties, key, Integer.class, null));
        //look at System.out, there is a message:
        //      WARNING: There is no way how to transform value "true" [java.lang.Boolean] to type [java.lang.Integer].
    }

    @Test
    public void testFallback() {
        Map<String, String> fallback = Maps.newHashMap();
        fallback.put("my.property", "my.old.property");

        Map<String, Object> properties = Maps.newHashMap();
        properties.put("my.old.property", "foo");

        assertEquals("foo", PropertiesHelper.getValue(properties, "my.property", String.class, fallback));
    }

    @Test
    public void testPropertyNameDeclination() {
        String myProperty = "jersey.config.my.property";
        String myClientProperty = "jersey.config.client.my.property";
        String myServerProperty = "jersey.config.server.my.property";
        String myNonJerseyProperty = "my.property";

        assertEquals(myProperty, PropertiesHelper.getPropertyNameForRuntime(myProperty, null));

        assertEquals(myClientProperty, PropertiesHelper.getPropertyNameForRuntime(myProperty, RuntimeType.CLIENT));
        assertEquals(myServerProperty, PropertiesHelper.getPropertyNameForRuntime(myProperty, RuntimeType.SERVER));

        assertEquals(myServerProperty, PropertiesHelper.getPropertyNameForRuntime(myServerProperty, RuntimeType.SERVER));
        assertEquals(myServerProperty, PropertiesHelper.getPropertyNameForRuntime(myServerProperty, RuntimeType.CLIENT));

        assertEquals(myClientProperty, PropertiesHelper.getPropertyNameForRuntime(myClientProperty, RuntimeType.SERVER));
        assertEquals(myClientProperty, PropertiesHelper.getPropertyNameForRuntime(myClientProperty, RuntimeType.CLIENT));

        assertEquals(myNonJerseyProperty, PropertiesHelper.getPropertyNameForRuntime(myNonJerseyProperty, RuntimeType.CLIENT));
        assertEquals(myNonJerseyProperty, PropertiesHelper.getPropertyNameForRuntime(myNonJerseyProperty, RuntimeType.CLIENT));
        assertEquals(myNonJerseyProperty, PropertiesHelper.getPropertyNameForRuntime(myNonJerseyProperty, null));
    }

}
