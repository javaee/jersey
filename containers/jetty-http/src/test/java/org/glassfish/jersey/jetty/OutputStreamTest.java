/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;


/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class OutputStreamTest extends AbstractJettyServerTester {
    @Path("/output")
    public static class TestResource {
        @Produces("text/plain")
        @GET
        public void get(ContainerRequest requestContext) throws IOException {
            assertEquals("GET", requestContext.getMethod());

            ContainerResponse response = new ContainerResponse(requestContext, Response.ok().build());
            OutputStream os = requestContext.getResponseWriter().writeResponseStatusAndHeaders("RESOURCE".getBytes().length, response);
            os.write("RESOURCE".getBytes());
        }

        @Produces("text/plain")
        @POST
        public void post(ContainerRequest requestContext) throws IOException {
            assertEquals("POST", requestContext.getMethod());

            String s = requestContext.readEntity(String.class);
            assertEquals("RESOURCE", s);

            ContainerResponse response = new ContainerResponse(requestContext, Response.ok().build());
            OutputStream os = requestContext.getResponseWriter().writeResponseStatusAndHeaders("RESOURCE".getBytes().length, response);
            os.write("RESOURCE".getBytes());
        }
    }

    @Test
    public void testGet() {
        startServer(TestResource.class);

        WebTarget r = ClientBuilder.newClient(new ClientConfig(LoggingFilter.class)).target(getUri().path("output").build());
        assertEquals("RESOURCE", r.request().get(String.class));
    }

    @Test
    public void testPost() {
        startServer(TestResource.class);

        WebTarget r = ClientBuilder.newClient(new ClientConfig(LoggingFilter.class)).target(getUri().path("output").build());
        String s = r.request().post(Entity.text("RESOURCE"), String.class);
        assertEquals("RESOURCE", s);
    }

    @Test
    public void testAll() {
        startServer(TestResource.class);

        WebTarget r = ClientBuilder.newClient(new ClientConfig(LoggingFilter.class)).target(getUri().path("output").build());
        assertEquals("RESOURCE", r.request().get(String.class));

        r = ClientBuilder.newClient(new ClientConfig(LoggingFilter.class)).target(getUri().path("output").build());
        assertEquals("RESOURCE", r.request().post(Entity.text("RESOURCE"), String.class));
    }
}
