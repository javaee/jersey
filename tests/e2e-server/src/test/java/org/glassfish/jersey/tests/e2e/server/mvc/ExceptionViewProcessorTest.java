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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.TestViewProcessor;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class ExceptionViewProcessorTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(ExplicitTemplate.class)
                .register(MvcFeature.class)
                .register(TestViewProcessor.class)
                .register(WebAppExceptionMapper.class);
    }

    @Provider
    public static class WebAppExceptionMapper implements ExceptionMapper<WebApplicationException> {

        public Response toResponse(WebApplicationException exception) {
            // Absolute.
            if (exception.getResponse().getStatus() == 404) {
                return Response
                        .status(404)
                        .entity(new Viewable("/org/glassfish/jersey/tests/e2e/server/mvc/ExceptionViewProcessorTest/404", "404"))
                        .build();
            }

            // Relative.
            if (exception.getResponse().getStatus() == 406) {
                return Response.status(406).entity(
                        new Viewable(
                                "/org/glassfish/jersey/tests/e2e/server/mvc/ExceptionViewProcessorTest/WebAppExceptionMapper/406",
                                "406")).build();
            }

            return exception.getResponse();
        }
    }

    @Path("/")
    public static class ExplicitTemplate {

        @GET
        @Produces("application/foo")
        public Viewable get() {
            return new Viewable("show", "get");
        }
    }

    @Test
    public void testAbsoluteExplicitTemplate() throws IOException {
        final Invocation.Builder request = target("/does-not-exist").request();

        Response cr = request.get(Response.class);
        assertEquals(404, cr.getStatus());

        Properties p = new Properties();
        p.load(cr.readEntity(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ExceptionViewProcessorTest/404.testp", p.getProperty("path"));
        assertEquals("404", p.getProperty("model"));
    }

    @Test
    public void testResolvingClassExplicitTemplate() throws IOException {
        final Invocation.Builder request = target("/").request("application/wrong-media-type");

        Response cr = request.get(Response.class);
        assertEquals(406, cr.getStatus());

        Properties p = new Properties();
        p.load(cr.readEntity(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ExceptionViewProcessorTest/WebAppExceptionMapper/406.testp",
                p.getProperty("path"));
        assertEquals("406", p.getProperty("model"));
    }
}
