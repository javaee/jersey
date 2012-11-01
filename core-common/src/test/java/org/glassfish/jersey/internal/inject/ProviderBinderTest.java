/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal.inject;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.glassfish.jersey.model.internal.DefaultConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests {@link ProviderBinder}.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class ProviderBinderTest {

    public static final class CustomReaderA implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return null;
        }
    }

    public static final class CustomReaderB implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return null;
        }
    }

    public static final class SimpleFeatureA implements Feature {

        private boolean initB;

        public SimpleFeatureA() {
        }

        public SimpleFeatureA(final boolean initB) {
            this.initB = initB;
        }

        @Override
        public boolean configure(final Configurable configurable) {
            configurable.register(initB ? CustomReaderB.class : CustomReaderA.class);
            return true;
        }
    }

    public static final class SimpleFeatureB implements Feature {

        @Override
        public boolean configure(final Configurable configurable) {
            configurable.register(CustomReaderB.class);
            return true;
        }
    }

    public static final class InstanceFeatureA implements Feature {

        private boolean initB;

        public InstanceFeatureA() {
        }

        public InstanceFeatureA(final boolean initB) {
            this.initB = initB;
        }

        @Override
        public boolean configure(final Configurable configurable) {
            configurable.register(initB ? new CustomReaderB() : new CustomReaderA());
            return true;
        }
    }

    public static final class ComplexFeature implements Feature {

        @Override
        public boolean configure(final Configurable configurable) {
            configurable.register(SimpleFeatureA.class);
            configurable.register(SimpleFeatureB.class);
            return true;
        }
    }

    public static final class RecursiveFeature implements Feature {

        @Override
        public boolean configure(final Configurable configurable) {
            configurable.register(CustomReaderA.class);
            configurable.register(RecursiveFeature.class);
            return true;
        }
    }

    public static final class RecursiveInstanceFeature implements Feature {

        @Override
        public boolean configure(final Configurable configurable) {
            configurable.register(new CustomReaderA());
            configurable.register(new RecursiveInstanceFeature());
            return true;
        }
    }

    @Test
    public void testConfigureFeatureHierarchy() throws Exception {
        final DefaultConfig config = new DefaultConfig();
        config.register(ComplexFeature.class);

        ProviderBinder.configureFeatures(config.getFeatureBag(), config, Injections.createLocator());

        final Set<Feature> enabledFeatures = config.getFeatureBag().getEnabledFeatures();
        assertEquals(1, enabledFeatures.size());
        assertEquals(ComplexFeature.class, enabledFeatures.iterator().next().getClass());

        final Set<Class<?>> providerClasses = config.getProviderClasses();
        assertEquals(2, providerClasses.size());
        assertTrue(providerClasses.contains(CustomReaderA.class));
        assertTrue(providerClasses.contains(CustomReaderB.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigureFeatureRecursive() throws Exception {
        final DefaultConfig config = new DefaultConfig();
        config.register(RecursiveFeature.class);

        ProviderBinder.configureFeatures(config.getFeatureBag(), config, Injections.createLocator());
    }

    @Test
    public void testConfigureFeatureInstances() throws Exception {
        final DefaultConfig config = new DefaultConfig();
        config.register(new SimpleFeatureA());
        config.register(new SimpleFeatureA(true));
        config.register(new SimpleFeatureA());

        ProviderBinder.configureFeatures(config.getFeatureBag(), config, Injections.createLocator());

        final Set<Feature> enabledFeatures = config.getFeatureBag().getEnabledFeatures();
        assertEquals(3, enabledFeatures.size());

        final Set<Class<?>> providerClasses = config.getProviderClasses();
        assertEquals(2, providerClasses.size());
        assertTrue(providerClasses.contains(CustomReaderA.class));
        assertTrue(providerClasses.contains(CustomReaderB.class));
    }

    @Test
    public void testConfigureFeatureInstancesProviderInstances() throws Exception {
        final DefaultConfig config = new DefaultConfig();
        config.register(new InstanceFeatureA());
        config.register(new InstanceFeatureA(true));
        config.register(new InstanceFeatureA());

        ProviderBinder.configureFeatures(config.getFeatureBag(), config, Injections.createLocator());

        final Set<Feature> enabledFeatures = config.getFeatureBag().getEnabledFeatures();
        assertEquals(3, enabledFeatures.size());

        final Set<Object> providerInstances = config.getProviderInstances();
        assertEquals(3, providerInstances.size());

        int a = 0;
        int b = 0;
        for (final Object instance : providerInstances) {
            if (instance instanceof CustomReaderA) {
                a++;
            } else {
                b++;
            }
        }
        assertEquals(2, a);
        assertEquals(1, b);
    }

    @Test
    public void testConfigureFeatureInstanceRecursive() throws Exception {
        final DefaultConfig config = new DefaultConfig();
        config.register(new RecursiveInstanceFeature());

        try {
            ProviderBinder.configureFeatures(config.getFeatureBag(), config, Injections.createLocator());
            fail("StackOverflowError expected.");
        } catch (StackOverflowError soe) {
            // OK.
        } catch (Throwable e) {
            // OK. On Windows StackOverflowError is wrapped into another exception.
            while (e != null) {
                if (e instanceof StackOverflowError) {
                    // OK.
                    return;
                }
                e = e.getCause();
            }
            fail("StackOverflowError expected.");
        }
    }
}
