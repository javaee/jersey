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

package org.glassfish.jersey.tests.e2e.common.config;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.internal.ServiceFinderBinder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Service finder injection binder unit test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ServiceFinderBinderTest {

    private static InjectionManager injectionManager;

    public ServiceFinderBinderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                bind(TestServiceB.class).to(TestContract.class);
                bind(TestServiceD.class).to(TestContract.class);
                install(new ServiceFinderBinder<>(TestContract.class, null, RuntimeType.SERVER));
            }
        };
        injectionManager = Injections.createInjectionManager(binder);
        injectionManager.completeRegistration();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testConfigure() {
        final Set<TestContract> providers = Providers.getProviders(injectionManager, TestContract.class);
        assertEquals(4, providers.size());

        final Collection<String> providerNames =
                providers.stream()
                         .map(TestContract::name)
                         .collect(Collectors.toList());

        assertTrue(providerNames.contains(TestServiceA.class.getName()));
        assertTrue(providerNames.contains(TestServiceB.class.getName()));
        assertTrue(providerNames.contains(TestServiceC.class.getName()));
        assertTrue(providerNames.contains(TestServiceD.class.getName()));
    }
}
