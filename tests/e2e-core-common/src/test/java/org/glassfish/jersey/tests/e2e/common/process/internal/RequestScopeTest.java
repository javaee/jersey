/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common.process.internal;

import java.lang.reflect.Type;

import org.glassfish.jersey.inject.hk2.Hk2RequestScope;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.process.internal.RequestScope;

import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test of the {@link RequestScope request scope}.
 *
 * @author Miroslav Fuksa
 */
public class RequestScopeTest {

    @Test
    public void testScopeWithCreatedInstance() {
        final RequestScope requestScope = new Hk2RequestScope();
        assertNull(requestScope.suspendCurrent());
        final Hk2RequestScope.Instance context = (Hk2RequestScope.Instance) requestScope.createContext();
        ForeignDescriptor inhab = ForeignDescriptor.wrap(new TestProvider("a"));
        context.put(inhab, "1");
        requestScope.runInScope(context, () -> {
            assertEquals("1", context.get(inhab));
            context.release();
            assertEquals("1", context.get(inhab));
        });
        assertNull(context.get(inhab));
    }

    @Test
    public void testScopeReleaseInsideScope() {
        final RequestScope requestScope = new Hk2RequestScope();
        assertNull(requestScope.suspendCurrent());
        final Hk2RequestScope.Instance instance = (Hk2RequestScope.Instance) requestScope.createContext();
        ForeignDescriptor inhab = ForeignDescriptor.wrap(new TestProvider("a"));
        instance.put(inhab, "1");
        requestScope.runInScope(instance, () -> {
            final Hk2RequestScope.Instance internalInstance = (Hk2RequestScope.Instance) requestScope.suspendCurrent();
            assertEquals(internalInstance, instance);
            assertEquals("1", instance.get(inhab));
            instance.release();
            assertEquals("1", instance.get(inhab));
        });
        assertEquals("1", instance.get(inhab));
        instance.release();
        assertNull(instance.get(inhab));
    }

    @Test
    public void testScopeWithImplicitInstance() throws Exception {
        final RequestScope requestScope = new Hk2RequestScope();
        assertNull(requestScope.suspendCurrent());
        ForeignDescriptor inhab = ForeignDescriptor.wrap(new TestProvider("a"));
        final Hk2RequestScope.Instance instance = requestScope.runInScope(() -> {
            final Hk2RequestScope.Instance internalInstance = (Hk2RequestScope.Instance) requestScope.suspendCurrent();
            assertNull(internalInstance.get(inhab));
            internalInstance.put(inhab, "1");
            assertEquals("1", internalInstance.get(inhab));
            return internalInstance;
        });
        assertEquals("1", instance.get(inhab));
        instance.release();
        assertNull(instance.get(inhab));
    }

    @Test
    public void testScopeWithTwoInternalTasks() throws Exception {
        final RequestScope requestScope = new Hk2RequestScope();
        assertNull(requestScope.suspendCurrent());
        ForeignDescriptor inhab = ForeignDescriptor.wrap(new TestProvider("a"));
        final Hk2RequestScope.Instance instance = requestScope.runInScope(() -> {
            final Hk2RequestScope.Instance internalInstance = (Hk2RequestScope.Instance) requestScope.suspendCurrent();

            final Hk2RequestScope.Instance anotherInstance = requestScope.runInScope(() -> {
                final Hk2RequestScope.Instance currentInstance = (Hk2RequestScope.Instance) requestScope.suspendCurrent();
                assertTrue(!currentInstance.equals(internalInstance));
                currentInstance.put(inhab, "1");
                return currentInstance;
            });
            assertTrue(!anotherInstance.equals(internalInstance));
            assertEquals("1", anotherInstance.get(inhab));
            anotherInstance.release();
            assertNull(anotherInstance.get(inhab));

            return internalInstance;
        });
        instance.release();
        assertNull(instance.get(inhab));
    }

    @Test
    public void testMultipleGetInstanceCalls() throws Exception {
        final RequestScope requestScope = new Hk2RequestScope();
        assertNull(requestScope.suspendCurrent());
        ForeignDescriptor inhab = ForeignDescriptor.wrap(new TestProvider("a"));
        final Hk2RequestScope.Instance instance = requestScope.runInScope(() -> {
            final Hk2RequestScope.Instance internalInstance = (Hk2RequestScope.Instance) requestScope.suspendCurrent();
            internalInstance.put(inhab, "1");
            requestScope.suspendCurrent();
            requestScope.suspendCurrent();
            requestScope.suspendCurrent();
            requestScope.suspendCurrent();
            return internalInstance;
        });
        assertEquals("1", instance.get(inhab));
        instance.release();
        assertEquals("1", instance.get(inhab));
        instance.release();
        assertEquals("1", instance.get(inhab));
        instance.release();
        assertEquals("1", instance.get(inhab));
        instance.release();
        assertEquals("1", instance.get(inhab));
        instance.release();
        assertNull(instance.get(inhab));
    }

    /**
     * Test request scope inhabitant.
     */
    public static class TestProvider extends AbstractActiveDescriptor<String> {

        private final String id;

        public TestProvider(final String id) {
            super();
            this.id = id;
        }

        @Override
        public Class<?> getImplementationClass() {
            return String.class;
        }

        @Override
        public Type getImplementationType() {
            return getImplementationClass();
        }

        @Override
        public String create(final ServiceHandle<?> root) {
            return id;
        }
    }
}
