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

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
public class LinkELContextTest {

    @Test
    public void testExpressionFactory() {
        System.out.println("Create expression factory");
        ExpressionFactory factory = ExpressionFactory.newInstance();
        assertNotNull(factory);
    }

    @Test
    public void testLiteralExpression() {
        System.out.println("Literal expression");
        ExpressionFactory factory = ExpressionFactory.newInstance();
        LinkELContext context = new LinkELContext(new BooleanBean(), null);
        ValueExpression expr = factory.createValueExpression(context,
                "${1+2}", int.class);
        Object value = expr.getValue(context);
        assertEquals(3, value);
    }

    public static final String ID = "10";
    public static final String NAME = "TheName";

    public static class EntityBean {

        private String id = ID;
        private String name = NAME;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    public void testExpression() {
        System.out.println("Raw expression");
        ExpressionFactory factory = ExpressionFactory.newInstance();
        LinkELContext context = new LinkELContext(new EntityBean(), null);
        ValueExpression expr = factory.createValueExpression(context,
                "${entity.id}", String.class);
        Object value = expr.getValue(context);
        assertEquals(ID, value);
    }

    @Test
    public void testEmbeddedExpression() {
        System.out.println("Embedded expression");
        ExpressionFactory factory = ExpressionFactory.newInstance();
        LinkELContext context = new LinkELContext(new EntityBean(), null);
        ValueExpression expr = factory.createValueExpression(context,
                "foo/${entity.id}/bar", String.class);
        Object value = expr.getValue(context);
        assertEquals("foo/" + ID + "/bar", value);
    }

    @Test
    public void testMultipleExpressions() {
        System.out.println("Multiple expressions");
        ExpressionFactory factory = ExpressionFactory.newInstance();
        LinkELContext context = new LinkELContext(new EntityBean(), null);
        ValueExpression expr = factory.createValueExpression(context,
                "foo/${entity.id}/bar/${entity.name}", String.class);
        Object value = expr.getValue(context);
        assertEquals("foo/" + ID + "/bar/" + NAME, value);
    }

    public static class OuterEntityBean {

        private EntityBean inner = new EntityBean();

        public EntityBean getInner() {
            return inner;
        }
    }

    @Test
    public void testNestedExpression() {
        System.out.println("Nested expression");
        ExpressionFactory factory = ExpressionFactory.newInstance();
        LinkELContext context = new LinkELContext(new OuterEntityBean(), null);
        ValueExpression expr = factory.createValueExpression(context,
                "${entity.inner.id}", String.class);
        Object value = expr.getValue(context);
        assertEquals(ID, value);
    }

    public static class BooleanBean {

        public boolean getEnabled() {
            return true;
        }

        public boolean getValue(boolean value) {
            return value;
        }
    }

    @Test
    public void testBooleanExpression() {
        System.out.println("Boolean expression");
        ExpressionFactory factory = ExpressionFactory.newInstance();
        LinkELContext context = new LinkELContext(new BooleanBean(), null);
        ValueExpression expr = factory.createValueExpression(context,
                "${entity.enabled}", boolean.class);
        Object value = expr.getValue(context);
        assertEquals(true, value);
        expr = factory.createValueExpression(context,
                "${entity.getValue(true)}", boolean.class);
        value = expr.getValue(context);
        assertEquals(true, value);
        expr = factory.createValueExpression(context,
                "${entity.getValue(false)}", boolean.class);
        value = expr.getValue(context);
        assertEquals(false, value);
    }

}
