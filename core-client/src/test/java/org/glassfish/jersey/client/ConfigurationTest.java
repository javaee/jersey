/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import org.junit.Ignore;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.client.Feature;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ConfigurationTest {

    public ConfigurationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSnapshot() {
        Configuration c_a = new Configuration().setProperty("common_name", "common_value");

        Configuration c_b = c_a.snapshot();
        assertNotNull(c_b);
        assertNotSame(c_a, c_b);
        assertEquals(c_a, c_b);
        assertEquals("common_value", c_a.getProperty("common_name"));
        assertEquals("common_value", c_b.getProperty("common_name"));

        c_b.setProperty("name", "value");

        assertFalse(c_a.equals(c_b));
        assertEquals("value", c_b.getProperty("name"));
        assertNull(c_a.getProperty("name"));
    }

    @Test
    public void testGetProperties() {
        Configuration instance = new Configuration();
        Map result = instance.getProperties();
        assertNotNull(result);

        instance.setProperty("name", "value");
        assertEquals("value", result.get("name"));

        try {
            result.remove("name");
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ex) {
            // ok
        }
    }

    @Test
    public void testGetProperty() {
        Configuration instance = new Configuration().setProperty("name", "value");
        assertEquals("value", instance.getProperty("name"));
        assertNull(instance.getProperty("other"));
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testGetFeatures() {
        Configuration instance = new Configuration();
        Set expResult = null;
        Set result = instance.getFeatures();
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testIsEnabled() {
        Class<? extends Feature> feature = null;
        Configuration instance = new Configuration();
        boolean expResult = false;
        boolean result = instance.isEnabled(feature);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testGetProviderClasses() {
        Configuration instance = new Configuration();
        Set expResult = null;
        Set result = instance.getProviderClasses();
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testGetProviderInstances() {
        Configuration instance = new Configuration();
        Set expResult = null;
        Set result = instance.getProviderInstances();
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testUpdate() {
        Configuration configuration = null;
        org.glassfish.jersey.client.Configuration instance = new org.glassfish.jersey.client.Configuration();
        Configuration expResult = null;
        Configuration result = instance.update(configuration);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testRegister_Class() {
        Class<?> providerClass = null;
        org.glassfish.jersey.client.Configuration instance = new org.glassfish.jersey.client.Configuration();
        Configuration expResult = null;
        Configuration result = instance.register(providerClass);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testRegister_ObjectArr() {
        Object[] providers = null;
        org.glassfish.jersey.client.Configuration instance = new org.glassfish.jersey.client.Configuration();
        Configuration expResult = null;
        Configuration result = instance.register(providers);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testEnable_Class() {
        Class<? extends Feature> feature = null;
        org.glassfish.jersey.client.Configuration instance = new org.glassfish.jersey.client.Configuration();
        Configuration expResult = null;
        Configuration result = instance.enable(feature);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testEnable_Feature() {
        Feature feature = null;
        org.glassfish.jersey.client.Configuration instance = new org.glassfish.jersey.client.Configuration();
        Configuration expResult = null;
        Configuration result = instance.enable(feature);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore("not ready yet")
    // TODO implement test
    public void testDisable() {
        Class<? extends Feature> feature = null;
        org.glassfish.jersey.client.Configuration instance = new org.glassfish.jersey.client.Configuration();
        Configuration expResult = null;
        Configuration result = instance.disable(feature);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    public void testSetProperties() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("name1", "value1");
        props.put("name2", "value2");
        props.put("name3", "value3");

        Configuration instance = new Configuration().setProperties(props);
        assertEquals("value1", instance.getProperty("name1"));
        assertEquals("value2", instance.getProperty("name2"));
        assertEquals("value3", instance.getProperty("name3"));
        assertNull(instance.getProperty("other"));
    }

    @Test
    public void testSetProperty() {
        Configuration instance = new Configuration();
        assertTrue(instance.getProperties().isEmpty());

        instance.setProperty("name", "value");
        assertFalse(instance.getProperties().isEmpty());
        assertEquals(1, instance.getProperties().size());
        assertEquals("value", instance.getProperty("name"));
    }
}
