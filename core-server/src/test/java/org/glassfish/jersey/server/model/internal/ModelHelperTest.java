/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.server.model.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.ws.rs.Path;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Constantino Cronemberger (ccronemberger at yahoo.com.br)
 */
public class ModelHelperTest {

    @Test
    public void testClass() {
        final Class cls = ModelHelper.getAnnotatedResourceClass(MyAnnotatedClass.class);
        Assert.assertSame(MyAnnotatedClass.class, cls);
    }

    @Test
    public void testSubClass() {
        // Spring with CGLIB proxies creates sub-classes
        final Object obj = new MyAnnotatedClass() {};
        Assert.assertNotSame(MyAnnotatedClass.class, obj.getClass());
        final Class cls = ModelHelper.getAnnotatedResourceClass(obj.getClass());
        Assert.assertSame(MyAnnotatedClass.class, cls);
    }

    @Test
    public void testProxyClass() throws Exception {
        // Spring can also create proxies for beans
        final Object obj = Proxy
                .newProxyInstance(getClass().getClassLoader(), new Class[] {MyServiceInterface.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                        return null;
                    }
                });
        final Class cls = ModelHelper.getAnnotatedResourceClass(obj.getClass());
        Assert.assertSame(MyServiceInterface.class, cls);
    }

    @Path("test")
    public interface MyServiceInterface {}

    @Path("test")
    public static class MyAnnotatedClass implements MyServiceInterface {}
}
