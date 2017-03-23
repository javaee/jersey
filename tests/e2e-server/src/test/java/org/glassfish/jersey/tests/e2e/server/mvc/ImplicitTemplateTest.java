/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.mvc;

import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

import javax.inject.Singleton;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.TestViewProcessor;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Michal Gajdos
 */
public class ImplicitTemplateTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(
                ImplicitResource.class, AnotherImplicitResource.class,
                ImplicitSingletonResource.class, ImplicitRootResource.class,
                ImplicitGetResource.class, AnotherImplicitGetResource.class, AnotherAnotherImplicitGetResource.class)
                .register(MvcFeature.class)
                .register(TestViewProcessor.class);
    }

    @Template
    @Path("/implicit")
    public static class ImplicitResource {

        public String toString() {
            return "ImplicitTemplate";
        }
    }

    @Path("/implicit")
    public static class AnotherImplicitResource {

        public String toString() {
            return "ImplicitAnotherTemplate";
        }
    }

    @Test
    public void testImplicitTemplateResources() throws Exception {
        for (final String path : new String[] {"", "index", "get"}) {
            WebTarget target = target("implicit");
            String templateName = "index";

            if (!"".equals(path)) {
                templateName = path;
                target = target.path(path);
            }

            Properties p = new Properties();
            p.load(target.request().get(InputStream.class));
            assertEquals(
                    "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitTemplateTest/ImplicitResource/" + templateName + ".testp",
                    p.getProperty("path"));
            assertEquals("ImplicitTemplate", p.getProperty("model"));
        }
    }

    @Test
    public void testImplicitTemplateResourcesNegative() throws Exception {
        assertEquals(404, target("implicit").path("do-not-exist").request().get().getStatus());
    }

    @Path("/implicit-get")
    @Produces("text/html")
    public static class ImplicitGetResource {

        @GET
        public String get() {
            return toString();
        }

        public String toString() {
            return "ImplicitGetTemplate";
        }
    }

    @Path("/implicit-get")
    @Template
    @Produces("text/plain")
    public static class AnotherImplicitGetResource {

        @GET
        @Path("sub")
        public String get() {
            return toString();
        }

        public String toString() {
            return "AnotherImplicitGetTemplate";
        }
    }

    @Path("/implicit-get/another")
    public static class AnotherAnotherImplicitGetResource {

        @GET
        public String get() {
            return toString();
        }

        public String toString() {
            return "AnotherAnotherImplicitGetTemplate";
        }
    }

    @Test
    public void testImplicitGetTemplateResources() throws Exception {
        for (final String path : new String[] {"", "index", "get"}) {
            WebTarget target = target("implicit-get");
            String templateName = "index";

            if (!"".equals(path)) {
                templateName = path;
                target = target.path(path);
            }

            Properties p = new Properties();
            p.load(target.request("text/plain").get(InputStream.class));
            assertEquals(
                    "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitTemplateTest/AnotherImplicitGetResource/" + templateName
                            + ".testp",
                    p.getProperty("path"));
            assertEquals("AnotherImplicitGetTemplate", p.getProperty("model"));
        }
    }

    @Template
    @Singleton
    @Path("/implicit-singleton")
    public static class ImplicitSingletonResource {

        private int counter = 0;

        public String toString() {
            return "ImplicitSingletonTemplate" + counter++;
        }
    }

    @Test
    public void testImplicitTemplateSingletonResources() throws Exception {
        for (int i = 0; i < 10; i++) {
            final WebTarget target = target("implicit-singleton");

            Properties p = new Properties();
            p.load(target.request().get(InputStream.class));
            assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitTemplateTest/ImplicitSingletonResource/index.testp",
                    p.getProperty("path"));
            assertEquals("ImplicitSingletonTemplate" + i, p.getProperty("model"));
        }
    }

    @Path("/implicit-sub-resource")
    public static class ImplicitRootResource {

        @Path("sub")
        public ImplicitSubResource getSubResource() {
            return new ImplicitSubResource("ImplicitRootResource");
        }
    }

    public static class ImplicitSubResource {

        private final String string;

        public ImplicitSubResource(final String string) {
            this.string = string;
        }

        @Path("sub")
        public ImplicitSubSubResource getSubResource() {
            return new ImplicitSubSubResource(string + "ImplicitSubResource");
        }
    }

    @Template
    public static class ImplicitSubSubResource {

        private final String string;

        public ImplicitSubSubResource(final String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string + "ImplicitSubSubResource";
        }
    }

    @Test
    public void testImplicitTemplateSubResources() throws Exception {
        final WebTarget target = target("implicit-sub-resource").path("sub").path("sub");

        Properties p = new Properties();
        p.load(target.request().get(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitTemplateTest/ImplicitSubSubResource/index.testp",
                p.getProperty("path"));
        assertEquals("ImplicitRootResourceImplicitSubResourceImplicitSubSubResource", p.getProperty("model"));
    }
}
