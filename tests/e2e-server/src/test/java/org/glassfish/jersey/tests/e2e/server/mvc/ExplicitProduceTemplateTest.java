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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.AbcViewProcessor;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.DefViewProcessor;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Gajdos
 */
public class ExplicitProduceTemplateTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(ExplicitTwoGetProducesTemplate.class,
                ExplicitNoProducesTemplate.class, ExplicitWildcardProducesTemplate.class, ExplicitTemplateProducesClass.class)
                .register(MvcFeature.class)
                .register(AbcViewProcessor.class)
                .register(DefViewProcessor.class);
    }

    @Path("/explicit-no-produces")
    public static class ExplicitNoProducesTemplate {

        @GET
        @Template
        public String def() {
            return "def";
        }
    }

    @Path("/explicit-wildcard-produces")
    public static class ExplicitWildcardProducesTemplate {

        @GET
        @Template
        @Produces("*/*")
        public String def() {
            return "def";
        }
    }

    @Path("/explicit-two-get-produces")
    public static class ExplicitTwoGetProducesTemplate {

        @GET
        @Template
        @Produces("application/abc")
        public String abc() {
            return "abc";
        }

        @GET
        @Template
        @Produces("*/*")
        public String def() {
            return "def";
        }
    }

    @Path("explicitTemplateProducesClass")
    @Produces("application/abc")
    public static class ExplicitTemplateProducesClass extends ExplicitTemplateTest.ExplicitTemplate {
    }

    @Test
    public void testProducesWildcard() throws Exception {
        for (final String path : new String[] {"explicit-no-produces", "explicit-wildcard-produces",
                "explicit-two-get-produces"}) {
            final WebTarget target = target(path);

            for (final String mediaType : new String[] {"application/def", "text/plain"}) {
                final Properties p = new Properties();
                p.load(target.request(mediaType).get(InputStream.class));

                assertTrue(p.getProperty("path")
                        .matches("/org/glassfish/jersey/tests/e2e/server/mvc/ExplicitProduceTemplateTest/Explicit([a-zA-Z]+)"
                                + "Template/index.def"));
                assertEquals("def", p.getProperty("model"));
                assertEquals("DefViewProcessor", p.getProperty("name"));
            }
        }
    }

    @Test
    public void testProducesSpecific() throws Exception {
        final WebTarget target = target("explicit-two-get-produces");

        final Properties p = new Properties();
        p.load(target.request("application/abc").get(InputStream.class));

        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ExplicitProduceTemplateTest/ExplicitTwoGetProducesTemplate/index.abc",
                p.getProperty("path"));
        assertEquals("abc", p.getProperty("model"));
        assertEquals("AbcViewProcessor", p.getProperty("name"));
    }

    @Test
    public void testExplicitTemplateProducesClass() throws Exception {
        final WebTarget target = target("explicitTemplateProducesClass");

        Properties props = new Properties();
        props.load(target.request().get(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ExplicitProduceTemplateTest/ExplicitTemplateProducesClass/index.abc",
                props.getProperty("path"));
        assertEquals("method", props.getProperty("model"));

        props = new Properties();
        props.load(target.path("methodRelativePath").request().get(InputStream.class));
        assertEquals(
                "/org/glassfish/jersey/tests/e2e/server/mvc/ExplicitProduceTemplateTest/ExplicitTemplateProducesClass/relative"
                        + ".abc",
                props.getProperty("path"));
        assertEquals("methodRelativePath", props.getProperty("model"));

        props = new Properties();
        props.load(target.path("methodAbsolutePath").request().get(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ExplicitTemplateTest/ExplicitTemplate/absolute.abc",
                props.getProperty("path"));
        assertEquals("methodAbsolutePath", props.getProperty("model"));
    }
}
