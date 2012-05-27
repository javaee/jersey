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

/*
 * Portions contributed by Joseph Walton (Atlassian)
 */

package org.glassfish.jersey.message.internal;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;

import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.hk2.scopes.PerThread;

import org.jvnet.hk2.annotations.Inject;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class SaxParserFactoryInjectionProviderTest {
    private Services services;
    private SAXParserFactory f1;
    private SAXParserFactory f2;
    private SAXParserFactory ff1;
    private SAXParserFactory ff2;

    @Before
    public void setUp() {
        services = createServices();
    }

    private static final FeaturesAndProperties EMPTY_FEATURES_AND_PROPERTIES = new FeaturesAndProperties() {
        @Override
        public Object getProperty(String propertyName) {
            return null;
        }

        @Override
        public boolean isProperty(String name) {
            return false;
        }

        @Override
        public Map<String, Object> getProperties() {
            return Collections.emptyMap();
        }
    };

    public static Services createServices(Module... additionalModules) {
        Module[] modules = new Module[additionalModules.length + 2];

        modules[0] = new AbstractModule() {
            @Override
            protected void configure() {
                bind(FeaturesAndProperties.class).toFactory(new Factory<FeaturesAndProperties>() {
                    @Override
                    public FeaturesAndProperties get() throws ComponentException {
                        return EMPTY_FEATURES_AND_PROPERTIES;
                    }
                });
                bind(SAXParserFactory.class).toFactory(SaxParserFactoryInjectionProvider.class).in(PerThread.class);
                bind(MySPFProvider.class).to(MySPFProvider.class).in(PerLookup.class);
            }
        };
        modules[1] = new ContextInjectionResolver.Module();
        System.arraycopy(additionalModules, 0, modules, 2, additionalModules.length);
        return HK2.get().create(null, modules);
    }

    @Test
    public void xmlReaderDoesNotResolveExternalParameterEntities() throws Exception {
        String url = "file:///no-such-file";
        String content = "<!DOCTYPE x [<!ENTITY % pe SYSTEM '" + url + "'> %pe;]><x/>";
        getSPF().newSAXParser().getXMLReader().parse(new InputSource(new ByteArrayInputStream(content.getBytes("us-ascii"))));
    }

    /**
     * Making sure that the same instance of SAXParserFactory is used if injected multiple times in the same thread.
     */
    @Test
    public void testSameForSameThreads() {
        f1 = getSPF();
        f2 = getSPF();
        ff1 = getSPFFromFactory();
        ff2 = getSPFFromFactory();
        assertNotNull(f1);
        assertNotNull(f2);
        assertNotNull(ff1);
        assertNotNull(ff2);
        assertTrue(f1 == f2);
        assertTrue(f2 == ff1);
        assertTrue(ff1 == ff2);
    }

    /**
     * Making sure that a different instance of SAXParserFactory is used for each different thread.
     * @throws InterruptedException
     */
    @Test
    public void testDifferentForDifferentThreads() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                f1 = getSPF();
                ff1 = getSPFFromFactory();
            }
        });
        t.start();
        f2 = getSPF();
        ff2 = getSPFFromFactory();
        t.join();
        assertNotNull(f1);
        assertNotNull(f2);
        assertNotNull(ff1);
        assertNotNull(ff2);
        assertTrue(f1 != f2);
        assertTrue(ff1 != ff2);
        assertTrue(f1 == ff1);
        assertTrue(f2 == ff2);
    }

    private SAXParserFactory getSPF() {
        return services.forContract(SAXParserFactory.class).get();
    }

    private SAXParserFactory getSPFFromFactory() {
        return services.forContract(MySPFProvider.class).get().getSPF();
    }

    /**
     * Class to emulate injecting a Factory&lt;SAXParserFactory&gt;
     */
    public static class MySPFProvider {
        private final Factory<SAXParserFactory> f;

        public MySPFProvider(@Inject Factory<SAXParserFactory> f) {
            this.f = f;
        }

        public SAXParserFactory getSPF() {
            return f.get();
        }
    }
}
