/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.servlet.internal;

import java.lang.reflect.Proxy;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Gajdos
 */
public class ThreadLocalInvokerTest {

    public static class CheckedException extends Exception {

    }

    public static interface X {

        public String checked() throws CheckedException;

        public String runtime();
    }

    @Test
    public void testIllegalState() {
        final ThreadLocalInvoker<X> tli = new ThreadLocalInvoker<>();

        final X x = (X) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{X.class}, tli);

        boolean caught = false;
        try {
            x.checked();
        } catch (final Exception ex) {
            caught = true;
            assertEquals(IllegalStateException.class, ex.getClass());
        }
        assertTrue(caught);

        caught = false;
        try {
            x.runtime();
        } catch (final Exception ex) {
            caught = true;
            assertEquals(IllegalStateException.class, ex.getClass());
        }
        assertTrue(caught);
    }

    @Test
    public void testExceptions() {
        final ThreadLocalInvoker<X> tli = new ThreadLocalInvoker<>();

        final X x = (X) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{X.class}, tli);

        tli.set(new X() {
            public String checked() throws CheckedException {
                throw new CheckedException();
            }

            public String runtime() {
                throw new RuntimeException();
            }
        });

        boolean caught = false;
        try {
            x.checked();
        } catch (final Exception ex) {
            caught = true;
            assertEquals(CheckedException.class, ex.getClass());
        }
        assertTrue(caught);

        caught = false;
        try {
            x.runtime();
        } catch (final Exception ex) {
            caught = true;
            assertEquals(RuntimeException.class, ex.getClass());
        }
        assertTrue(caught);
    }
}
