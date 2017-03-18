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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.TestViewProcessor;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class ImplicitProducesViewProcessorTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(
                ImplicitTemplate.class,
                ImplicitWithGetTemplate.class,
                ImplicitWithSubResourceGetTemplate.class)
                .register(MvcFeature.class)
                .register(TestViewProcessor.class).property("jersey.config.server.tracing", "ALL");
    }

    @Path("/implicit")
    @Template
    @Produces("text/plain;qs=.5")
    public static class ImplicitTemplate {

        public String toString() {
            return "ImplicitTemplate";
        }
    }

    @Test
    public void testImplicitTemplate() throws IOException {
        final WebTarget target = target("implicit");

        Properties p = new Properties();
        Response cr = target.request("text/plain", "application/foo").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitTemplate/index.testp",
                p.getProperty("path"));
        assertEquals("ImplicitTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("application/foo", "text/plain").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitTemplate/index.testp",
                p.getProperty("path"));
        assertEquals("ImplicitTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("text/plain;q=0.5", "application/foo").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitTemplate/index.testp",
                p.getProperty("path"));
        assertEquals("ImplicitTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("application/foo", "text/plain;q=0.5").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitTemplate/index.testp",
                p.getProperty("path"));
        assertEquals("ImplicitTemplate", p.getProperty("model"));
    }

    @Path("/implicit-get")
    @Template
    @Produces("text/plain;qs=0.5")
    public static class ImplicitWithGetTemplate {

        @GET
        @Produces("application/foo;qs=0.2")
        public String toString() {
            return "ImplicitWithGetTemplate";
        }
    }

    @Test
    public void testImplicitWithGetTemplate() throws IOException {
        final WebTarget target = target("implicit-get");

        Properties p = new Properties();
        Response cr = target.request("text/plain", "application/foo").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitWithGetTemplate/index"
                        + ".testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithGetTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("application/foo", "text/plain").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitWithGetTemplate/index"
                        + ".testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithGetTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("text/plain", "application/foo;q=0.5").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitWithGetTemplate/index"
                        + ".testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithGetTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("application/foo;q=0.5", "text/plain").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitWithGetTemplate/index"
                        + ".testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithGetTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("*/*").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest/ImplicitWithGetTemplate/index"
                        + ".testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithGetTemplate", p.getProperty("model"));

        cr = target.request("text/plain;q=0.5", "application/foo").get(Response.class);
        assertEquals(new MediaType("application", "foo"), cr.getMediaType());
        assertEquals("ImplicitWithGetTemplate", cr.readEntity(String.class));
    }

    @Path("/implicit-get-subresource")
    @Template
    @Produces("text/plain;qs=0.5")
    public static class ImplicitWithSubResourceGetTemplate {

        @GET
        @Path("sub")
        @Produces("application/foo;qs=0.2")
        public String toString() {
            return "ImplicitWithSubResourceGetTemplate";
        }
    }

    @Test
    public void testImplicitWithSubResourceGetTemplate() throws IOException {
        final WebTarget target = target("implicit-get-subresource").path("sub");

        Properties p = new Properties();
        Response cr = target.request("text/plain", "application/foo").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest"
                        + "/ImplicitWithSubResourceGetTemplate/sub.testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithSubResourceGetTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("application/foo", "text/plain").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest"
                        + "/ImplicitWithSubResourceGetTemplate/sub.testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithSubResourceGetTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("text/plain", "application/foo;q=0.5").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest"
                        + "/ImplicitWithSubResourceGetTemplate/sub.testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithSubResourceGetTemplate", p.getProperty("model"));

        p = new Properties();
        cr = target.request("application/foo;q=0.5", "text/plain").get(Response.class);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        p.load(cr.readEntity(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitProducesViewProcessorTest"
                        + "/ImplicitWithSubResourceGetTemplate/sub.testp",
                p.getProperty("path"));
        assertEquals("ImplicitWithSubResourceGetTemplate", p.getProperty("model"));

        assertEquals("ImplicitWithSubResourceGetTemplate", target.request("application/foo").get(String.class));
    }

}
