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
package org.glassfish.jersey.server;

import java.util.concurrent.ExecutionException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.internal.inject.AbstractBinder;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ResourceConfigTest {

    @Test
    public void testGetElementsDefault1() {
        final String[] elements = ResourceConfig.getElements(new String[]{"a b,c;d\ne"});

        assertEquals(elements[0], "a");
        assertEquals(elements[1], "b");
        assertEquals(elements[2], "c");
        assertEquals(elements[3], "d");
        assertEquals(elements[4], "e");
    }

    @Test
    public void testGetElementsDefault2() {
        final String[] elements = ResourceConfig.getElements(new String[]{"a    b, ,c;d\n\n\ne"});

        assertEquals(elements[0], "a");
        assertEquals(elements[1], "b");
        assertEquals(elements[2], "c");
        assertEquals(elements[3], "d");
        assertEquals(elements[4], "e");
    }

    @Test
    public void testGetElementsExplicitDelimiter() {
        final String[] elements = ResourceConfig.getElements(new String[]{"a b,c;d\ne"}, " ;");

        assertEquals(elements[0], "a");
        assertEquals(elements[1], "b,c");
        assertEquals(elements[2], "d\ne");
    }

    @Test
    public void testResourceConfigClasses() {
        ResourceConfig resourceConfig = new MyResourceConfig2();
        ApplicationHandler ah = new ApplicationHandler(resourceConfig);

        assertEquals(1, ah.getConfiguration().getClasses().size());
    }

    @Test
    public void testResourceConfigInjection() throws InterruptedException, ExecutionException {
        final int rcId = 12345;
        ResourceConfig resourceConfig = new MyResourceConfig2(rcId);
        ApplicationHandler ah = new ApplicationHandler(resourceConfig);

        assertSame(resourceConfig, ah.getServiceLocator().getService(Application.class));

        ContainerResponse r = ah.apply(RequestContextBuilder.from("/", "/resource?id=" + rcId, "GET").build()).get();
        assertEquals(200, r.getStatus());
        assertEquals("Injected application instance not same as used for building the Jersey handler.",
                "true", r.getEntity());
    }

    @Test
    public void testResourceConfigMergeApplications() throws Exception {
        // No custom binder.
        ApplicationHandler ah = new ApplicationHandler(ResourceConfig.class);
        assertEquals(0, ah.getConfiguration().getCustomBinders().size());

        // with MyBinder
        ah = new ApplicationHandler(MyResourceConfig1.class);
        assertEquals(1, ah.getConfiguration().getCustomBinders().size());

        // Add myBinder + one default.
        final MyBinder defaultBinder = new MyBinder();
        ResourceConfig rc = ResourceConfig.forApplicationClass(MyResourceConfig1.class);
        rc.addBinders(defaultBinder);
        ah = new ApplicationHandler(rc);
        assertEquals(2, ah.getConfiguration().getCustomBinders().size());
        assertTrue(ah.getConfiguration().getCustomBinders().contains(defaultBinder));
    }

    public static class MyResourceConfig1 extends ResourceConfig {
        public MyResourceConfig1() {
            addBinders(new MyBinder());
        }
    }

    public static class MyResourceConfig2 extends ResourceConfig {

        private final int id;

        public MyResourceConfig2() {
            this(0);
        }

        public MyResourceConfig2(int id) {
            this.id = id;
            addClasses(MyResource.class);
        }
    }

    @Path("resource")
    public static class MyResource {

        @Context
        Application app;

        @GET
        public String test(@QueryParam("id") int rcId) {
            return Boolean.toString((app instanceof MyResourceConfig2) && ((MyResourceConfig2) app).id == rcId);
        }
    }

    public static class MyBinder extends AbstractBinder {

        @Override
        protected void configure() {
            // do nothing
        }
    }
}
