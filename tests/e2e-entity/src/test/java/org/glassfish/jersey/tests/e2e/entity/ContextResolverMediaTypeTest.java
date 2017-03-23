/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

/**
 * @author Paul Sandoz
 * @author Martin Matula
 */
@RunWith(Enclosed.class)
public class ContextResolverMediaTypeTest {

    @Produces("text/plain")
    @Provider
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class TextPlainContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "text/plain";
        }
    }

    @Produces("text/*")
    @Provider
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class TextContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "text/*";
        }
    }

    @Produces("*/*")
    @Provider
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class WildcardContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "*/*";
        }
    }

    @Produces({"text/plain", "text/html"})
    @Provider
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class TextPlainHtmlContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "text/plain/html";
        }

    }

    @Produces("text/html")
    @Provider
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class TextHtmlContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "text/html";
        }

    }

    @Path("/")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class ContextResource {

        @Context
        Providers p;

        @Context
        ContextResolver<String> cr;

        @GET
        @Path("{id: .+}")
        public String get(@PathParam("id") MediaType m) {
            ContextResolver<String> cr = p.getContextResolver(String.class, m);

            // Verify cache is working
            ContextResolver<String> cachedCr = p.getContextResolver(String.class, m);
            assertEquals(cr, cachedCr);

            if (cr == null) {
                return "NULL";
            } else {
                return cr.getContext(null);
            }
        }
    }

    public static class ProduceTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ContextResource.class,
                    TextPlainContextResolver.class,
                    TextContextResolver.class,
                    WildcardContextResolver.class);
        }

        @Test
        public void testProduce() throws IOException {

            WebTarget target = target();

            assertEquals("text/plain", target.path("text/plain").request().get(String.class));
            assertEquals("text/*", target.path("text/*").request().get(String.class));
            assertEquals("*/*", target.path("*/*").request().get(String.class));

            assertEquals("text/*", target.path("text/html").request().get(String.class));

            assertEquals("*/*", target.path("application/xml").request().get(String.class));
            assertEquals("*/*", target.path("application/*").request().get(String.class));
        }
    }

    public static class ProducesTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ContextResource.class,
                    TextPlainHtmlContextResolver.class,
                    TextContextResolver.class,
                    WildcardContextResolver.class);
        }

        @Test
        public void testProduces() throws IOException {
            WebTarget target = target();

            assertEquals("text/plain/html", target.path("text/plain").request().get(String.class));
            assertEquals("text/plain/html", target.path("text/html").request().get(String.class));
            assertEquals("text/*", target.path("text/*").request().get(String.class));
            assertEquals("*/*", target.path("*/*").request().get(String.class));

            assertEquals("text/*", target.path("text/csv").request().get(String.class));

            assertEquals("*/*", target.path("application/xml").request().get(String.class));
            assertEquals("*/*", target.path("application/*").request().get(String.class));
        }
    }

    public static class ProducesSeparateTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ContextResource.class,
                    TextPlainContextResolver.class,
                    TextHtmlContextResolver.class,
                    TextContextResolver.class,
                    WildcardContextResolver.class);
        }

        @Test
        public void testProducesSeparate() throws IOException {
            WebTarget target = target();

            assertEquals("text/plain", target.path("text/plain").request().get(String.class));
            assertEquals("text/html", target.path("text/html").request().get(String.class));
            assertEquals("text/*", target.path("text/*").request().get(String.class));
            assertEquals("*/*", target.path("*/*").request().get(String.class));

            assertEquals("text/*", target.path("text/csv").request().get(String.class));

            assertEquals("*/*", target.path("application/xml").request().get(String.class));
            assertEquals("*/*", target.path("application/*").request().get(String.class));
        }
    }

    public static class ProducesXXXTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ContextResource.class,
                    TextPlainContextResolver.class,
                    TextHtmlContextResolver.class);
        }

        @Test
        public void testProducesXXX() throws IOException {
            WebTarget target = target();

            assertEquals("text/plain", target.path("text/plain").request().get(String.class));
            assertEquals("text/html", target.path("text/html").request().get(String.class));
            assertEquals("NULL", target.path("text/*").request().get(String.class));
            assertEquals("NULL", target.path("*/*").request().get(String.class));

            assertEquals("NULL", target.path("text/csv").request().get(String.class));

            assertEquals("NULL", target.path("application/xml").request().get(String.class));
            assertEquals("NULL", target.path("application/*").request().get(String.class));
        }
    }
}
