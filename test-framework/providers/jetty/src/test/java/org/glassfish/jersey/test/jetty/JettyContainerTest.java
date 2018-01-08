/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.jetty;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.inject.hk2.DelayedHk2InjectionManager;
import org.glassfish.jersey.inject.hk2.ImmediateHk2InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.jetty.JettyHttpContainer;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.glassfish.hk2.api.ServiceLocator;

import org.jvnet.hk2.internal.ServiceLocatorImpl;

import org.eclipse.jetty.server.Server;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link JettyHttpContainer}.
 *
 * @author Arul Dhesiaseelan (aruld at acm org)
 * @author Miroslav Fuksa
 */
public class JettyContainerTest extends JerseyTest {

    /**
     * Creates new instance.
     */
    public JettyContainerTest() {
        super(new JettyTestContainerFactory());
    }

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(Resource.class);
    }

    /**
     * Test resource class.
     */
    @Path("one")
    public static class Resource {

        /**
         * Test resource method.
         *
         * @return Test simple string response.
         */
        @GET
        public String getSomething() {
            return "get";
        }
    }

    @Test
    /**
     * Test {@link Server Jetty Server} container.
     */
    public void testJettyContainerTarget() {
        final Response response = target().path("one").request().get();

        assertEquals("Response status unexpected.", 200, response.getStatus());
        assertEquals("Response entity unexpected.", "get", response.readEntity(String.class));
    }

    /**
     * Test that defined ServiceLocator becomes a parent of the newly created service locator.
     */
    @Test
    public void testParentServiceLocator() {
        final ServiceLocator locator = new ServiceLocatorImpl("MyServiceLocator", null);
        final Server server = JettyHttpContainerFactory.createServer(URI.create("http://localhost:9876"),
                new ResourceConfig(Resource.class), false, locator);
        JettyHttpContainer container = (JettyHttpContainer) server.getHandler();
        InjectionManager injectionManager = container.getApplicationHandler().getInjectionManager();

        ServiceLocator serviceLocator;
        if (injectionManager instanceof ImmediateHk2InjectionManager) {
            serviceLocator = ((ImmediateHk2InjectionManager) injectionManager).getServiceLocator();
        } else if (injectionManager instanceof DelayedHk2InjectionManager) {
            serviceLocator = ((DelayedHk2InjectionManager) injectionManager).getServiceLocator();
        } else {
            throw new RuntimeException("Invalid Hk2 InjectionManager");
        }
        assertTrue("Application injection manager was expected to have defined parent locator",
                   serviceLocator.getParent() == locator);
    }
}
