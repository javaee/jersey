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

package org.glassfish.jersey.server.model;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;

import javax.ws.rs.POST;
import javax.ws.rs.core.Context;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul Sandoz
 */
public class GenericMethodListTest {

    public abstract static class AFoo<T, V> {

        @POST
        public abstract T create(T newObject, @Context String s, @Context V v);
    }

    public abstract static class ABar extends AFoo<String, Integer> {
    }

    public class AResource extends ABar {

        public String create(String newObject, String s, Integer v) {
            return newObject;
        }
    }

    @Test
    public void testGenericAbstractClasses() {
        MethodList ml = new MethodList(AResource.class);
        Iterator<AnnotatedMethod> i = ml.iterator();
        assertTrue(i.hasNext());

        AnnotatedMethod am = i.next();
        Method m = am.getMethod();
        Type[] types = m.getGenericParameterTypes();
        assertTrue(types[0] instanceof TypeVariable);
        assertTrue(types[1] instanceof Class);
        assertTrue(types[2] instanceof TypeVariable);
    }

    public static interface IFoo<T, V> {

        @POST
        public T create(T newObject, @Context String s, @Context V v);
    }

    public static interface IBar extends IFoo<String, Integer> {
    }

    public class IResource implements IBar {

        public String create(String newObject, String s, Integer v) {
            return newObject;
        }
    }

    @Test
    public void testGeneriInterfaceClasses() {
        MethodList ml = new MethodList(IResource.class);
        Iterator<AnnotatedMethod> i = ml.iterator();
        assertTrue(i.hasNext());

        AnnotatedMethod am = i.next();
        Method m = am.getMethod();
        Type[] types = m.getGenericParameterTypes();
        assertTrue(types[0] instanceof TypeVariable);
        assertTrue(types[1] instanceof Class);
        assertTrue(types[2] instanceof TypeVariable);
    }
}
