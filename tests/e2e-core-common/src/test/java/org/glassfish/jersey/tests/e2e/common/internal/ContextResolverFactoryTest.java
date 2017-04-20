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

package org.glassfish.jersey.tests.e2e.common.internal;

import java.util.Collections;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.tests.e2e.common.TestRuntimeDelegate;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Context resolvers factory unit test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ContextResolverFactoryTest {

    @Provider
    private static class CustomStringResolver implements ContextResolver<String> {

        public static final String VALUE = "foof";

        @Override
        public String getContext(Class<?> type) {
            return VALUE;
        }
    }

    @Provider
    @Produces("application/*")
    private static class CustomIntegerResolverA implements ContextResolver<Integer> {

        public static final int VALUE = 1001;

        @Override
        public Integer getContext(Class<?> type) {
            return VALUE;
        }
    }

    @Provider
    @Produces("application/json")
    private static class CustomIntegerResolverB implements ContextResolver<Integer> {

        public static final int VALUE = 2002;

        @Override
        public Integer getContext(Class<?> type) {
            return VALUE;
        }
    }

    @Provider
    @Produces("application/json")
    private static class CustomIntegerResolverC implements ContextResolver<Integer> {

        public static final int VALUE = 3003;

        @Override
        public Integer getContext(Class<?> type) {
            return VALUE;
        }
    }

    private static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(CustomStringResolver.class).to(ContextResolver.class);
            bind(CustomIntegerResolverA.class).to(ContextResolver.class);
            bind(CustomIntegerResolverB.class).to(ContextResolver.class);
        }
    }

    private ContextResolverFactory crf;

    public ContextResolverFactoryTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Before
    public void setUp() {
        InjectionManager injectionManager = Injections.createInjectionManager();
        ProviderBinder providerBinder = new ProviderBinder(injectionManager);
        providerBinder.bindClasses(Collections.singleton(CustomIntegerResolverC.class));
        injectionManager.register(new Binder());

        BootstrapBag bootstrapBag = new BootstrapBag();
        ContextResolverFactory.ContextResolversConfigurator configurator =
                new ContextResolverFactory.ContextResolversConfigurator();
        configurator.init(injectionManager, bootstrapBag);
        injectionManager.completeRegistration();
        configurator.postInit(injectionManager, bootstrapBag);

        crf = injectionManager.getInstance(ContextResolverFactory.class);
    }

    @Test
    public void testResolve() {
        assertEquals(CustomStringResolver.VALUE, crf.resolve(String.class, MediaType.WILDCARD_TYPE).getContext(String.class));
        assertEquals(CustomStringResolver.VALUE, crf.resolve(String.class, MediaType.TEXT_PLAIN_TYPE).getContext(String.class));

        assertEquals(CustomIntegerResolverA.VALUE,
                crf.resolve(Integer.class, MediaType.APPLICATION_XML_TYPE).getContext(Integer.class));
        assertEquals(CustomIntegerResolverA.VALUE,
                crf.resolve(Integer.class, MediaType.valueOf("application/*")).getContext(Integer.class));

        // Test that resolver "B" is shadowed by a custom resolver "C"
        assertEquals(CustomIntegerResolverC.VALUE,
                crf.resolve(Integer.class, MediaType.APPLICATION_JSON_TYPE).getContext(Integer.class));

        // Test that there is no matching provider
        assertNull(crf.resolve(Integer.class, MediaType.TEXT_PLAIN_TYPE));
    }
}
