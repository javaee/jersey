/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.inject.hk2;

import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Petr Bouda
 */
public class InjectionManagerTest {

    @Test
    public void testServiceLocatorParent() {
        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(EnglishGreeting.class);
            }
        };
        ServiceLocator parentLocator = ServiceLocatorUtilities.bind(binder);

        InjectionManager injectionManager = Injections.createInjectionManager(parentLocator);
        injectionManager.completeRegistration();
        assertNotNull(injectionManager.getInstance(EnglishGreeting.class));
    }

    @Test
    public void testInjectionManagerParent() {
        ClassBinding<EnglishGreeting> greetingBinding = Bindings.serviceAsContract(EnglishGreeting.class);
        InjectionManager parentInjectionManager = Injections.createInjectionManager();
        parentInjectionManager.register(greetingBinding);
        parentInjectionManager.completeRegistration();

        InjectionManager injectionManager = Injections.createInjectionManager(parentInjectionManager);
        injectionManager.completeRegistration();
        assertNotNull(injectionManager.getInstance(EnglishGreeting.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownParent() {
        Injections.createInjectionManager(new Object());
    }

    @Test
    public void testIsRegistrable() {
        InjectionManager injectionManager = Injections.createInjectionManager();
        assertTrue(injectionManager.isRegistrable(Binder.class));
        assertTrue(injectionManager.isRegistrable(AbstractBinder.class));
        assertFalse(injectionManager.isRegistrable(org.glassfish.jersey.internal.inject.AbstractBinder.class));
        assertFalse(injectionManager.isRegistrable(String.class));
    }

    @Test
    public void testRegisterBinder() {
        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(EnglishGreeting.class);
            }
        };

        InjectionManager injectionManager = Injections.createInjectionManager();
        injectionManager.register(binder);
        injectionManager.completeRegistration();
        assertNotNull(injectionManager.getInstance(EnglishGreeting.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUnknownProvider() {
        InjectionManager injectionManager = Injections.createInjectionManager();
        injectionManager.register(new Object());
    }
}
