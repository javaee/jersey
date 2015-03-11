/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.linking;

import java.net.URI;
import java.util.Iterator;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.linking.mapping.ResourceMappingContext;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
public class EntityDescriptorTest {

    public static class TestClassA {

        @InjectLink
        protected String foo;

        @InjectLink
        private String bar;

        public String baz;
    }

    ResourceMappingContext mockRmc = new ResourceMappingContext() {

        @Override
        public ResourceMappingContext.Mapping getMapping(Class<?> resource) {
            return null;
        }
    };

    /**
     * Test for declared properties
     */
    @Test
    public void testDeclaredProperties() {
        System.out.println("Declared properties");
        EntityDescriptor instance = EntityDescriptor.getInstance(TestClassA.class);
        assertEquals(2, instance.getLinkFields().size());
        assertEquals(1, instance.getNonLinkFields().size());
    }

    public static class TestClassB extends TestClassA {

        @InjectLink
        private String bar;
    }

    /**
     * Test for inherited properties
     */
    @Test
    public void testInheritedProperties() {
        System.out.println("Inherited properties");
        EntityDescriptor instance = EntityDescriptor.getInstance(TestClassB.class);
        assertEquals(2, instance.getLinkFields().size());
        assertEquals(1, instance.getNonLinkFields().size());
    }

    private static final String TEMPLATE_A = "foo";

    @Path(TEMPLATE_A)
    public static class TestResourceA {
    }

    public static class TestClassC {

        @InjectLink(resource = TestResourceA.class, bindings = {@Binding(name = "bar", value = "baz")})
        String res;
    }

    @Test
    public void testResourceLink() {
        System.out.println("Resource class link");
        EntityDescriptor instance = EntityDescriptor.getInstance(TestClassC.class);
        assertEquals(1, instance.getLinkFields().size());
        assertEquals(0, instance.getNonLinkFields().size());
        InjectLinkFieldDescriptor linkDesc = (InjectLinkFieldDescriptor) instance.getLinkFields().iterator().next();
        assertEquals(TEMPLATE_A, linkDesc.getLinkTemplate(mockRmc));
        assertEquals("baz", linkDesc.getBinding("bar"));
    }

    public static class TestClassD {

        @InjectLink(value = TEMPLATE_A, style = InjectLink.Style.RELATIVE_PATH)
        private String res1;

        @InjectLink(value = TEMPLATE_A, style = InjectLink.Style.RELATIVE_PATH)
        private URI res2;
    }

    @Test
    public void testStringLink() {
        System.out.println("String link");
        EntityDescriptor instance = EntityDescriptor.getInstance(TestClassD.class);
        assertEquals(2, instance.getLinkFields().size());
        assertEquals(0, instance.getNonLinkFields().size());
        Iterator<FieldDescriptor> i = instance.getLinkFields().iterator();
        while (i.hasNext()) {
            InjectLinkFieldDescriptor linkDesc = (InjectLinkFieldDescriptor) i.next();
            assertEquals(TEMPLATE_A, linkDesc.getLinkTemplate(mockRmc));
        }
    }

    @Test
    public void testSetLink() {
        System.out.println("Set link");
        EntityDescriptor instance = EntityDescriptor.getInstance(TestClassD.class);
        Iterator<FieldDescriptor> i = instance.getLinkFields().iterator();
        TestClassD testClass = new TestClassD();
        while (i.hasNext()) {
            InjectLinkFieldDescriptor linkDesc = (InjectLinkFieldDescriptor) i.next();
            URI value = UriBuilder.fromPath(linkDesc.getLinkTemplate(mockRmc)).build();
            linkDesc.setPropertyValue(testClass, value);
        }
        assertEquals(TEMPLATE_A, testClass.res1);
        assertEquals(TEMPLATE_A, testClass.res2.toString());
    }

}
