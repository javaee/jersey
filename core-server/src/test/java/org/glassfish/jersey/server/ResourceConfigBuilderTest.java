/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ResourceConfigBuilderTest {
    @Test
    public void testEmpty() {
        ResourceConfig resourceConfig = new ResourceConfig();

        assertTrue(resourceConfig.getClasses() != null);
        assertTrue(resourceConfig.getClasses().isEmpty());

        assertTrue(resourceConfig.getSingletons() != null);
        assertTrue(resourceConfig.getSingletons().isEmpty());

    }

    @Test
    public void testClasses() {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceConfigBuilderTest.class);

        assertTrue(resourceConfig.getClasses() != null);
        assertTrue(resourceConfig.getClasses().size() == 1);
        assertTrue(resourceConfig.getClasses().contains(ResourceConfigBuilderTest.class));

        assertTrue(resourceConfig.getSingletons() != null);
        assertTrue(resourceConfig.getSingletons().isEmpty());
    }

    @Test
    public void testSingletons() {
        final ResourceConfigBuilderTest resourceConfigBuilderTest = new ResourceConfigBuilderTest();

        ResourceConfig resourceConfig = new ResourceConfig().registerInstances(resourceConfigBuilderTest);

        assertTrue(resourceConfig.getClasses() != null);
        assertTrue(resourceConfig.getClasses().isEmpty());

        assertTrue(resourceConfig.getSingletons() != null);
        assertTrue(resourceConfig.getSingletons().size() == 1);
        assertTrue(resourceConfig.getSingletons().contains(resourceConfigBuilderTest));
    }

    @Test
    public void testApplication() {
        final javax.ws.rs.core.Application application = new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                return super.getClasses();
            }

            @Override
            public Set<Object> getSingletons() {
                return super.getSingletons();
            }
        };

        ApplicationHandler ah = new ApplicationHandler(application);
        assertTrue(ah.getServiceLocator().getService(Application.class).equals(application));
    }

    /**
     * test that I can initialize resource config with application class instead of an application instance
     * and then read the app properties
     */
    @Test
    public void testApplicationClassProperties() {
        ResourceConfig resourceConfig = initApp(MyApplication.class);

        assertTrue(resourceConfig.getProperties().containsKey("myProperty"));
        assertTrue(resourceConfig.getProperties().get("myProperty").equals("myValue"));
    }

    /**
     * test that I can initialize resource config with application class instead of an application instance
     * and then read the app classes
     */
    @Test
    public void testApplicationClassClasses() {
        ResourceConfig resourceConfig = initApp(MyApplication2.class);

        assertTrue(!resourceConfig.getClasses().isEmpty());
    }

    private static class MyApplication extends ResourceConfig {
        public MyApplication() {
            property("myProperty", "myValue");
        }
    }

    public static class MyApplication2 extends ResourceConfig {
        public MyApplication2() {
            super(MyResource.class);
        }
    }

    @Path("resource")
    public static class MyResource {
        @GET
        public String getIt() {
            return "get it";
        }
    }

    private static ResourceConfig initApp(Class<? extends Application> appClass) {
        return new ApplicationHandler(appClass).getConfiguration();
    }

    public static class TestProvider implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return context.proceed();
        }
    }

    // Reproducer JERSEY-1637
    @Test
    public void testRegisterNullOrEmptyContracts() {
        final TestProvider provider = new TestProvider();

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(TestProvider.class, (Class<?>[]) null);
        assertFalse(resourceConfig.getConfiguration().isRegistered(TestProvider.class));

        resourceConfig.register(provider,  (Class<?>[]) null);
        assertFalse(resourceConfig.getConfiguration().isRegistered(TestProvider.class));
        assertFalse(resourceConfig.getConfiguration().isRegistered(provider));

        resourceConfig.register(TestProvider.class,  new Class[0]);
        assertFalse(resourceConfig.getConfiguration().isRegistered(TestProvider.class));

        resourceConfig.register(provider,  new Class[0]);
        assertFalse(resourceConfig.getConfiguration().isRegistered(TestProvider.class));
        assertFalse(resourceConfig.getConfiguration().isRegistered(provider));
    }
}
