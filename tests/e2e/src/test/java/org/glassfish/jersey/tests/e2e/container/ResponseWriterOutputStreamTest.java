/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.container;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * This is really weird approach and test.
 *
 * @author Michal Gajdos
 */
public class ResponseWriterOutputStreamTest extends JerseyContainerTest {

    @Path("/")
    public static class Resource {

        @GET
        @Produces("text/plain")
        public void get(final ContainerRequest context) throws IOException {
            assertThat(context.getMethod(), is("GET"));

            final ContainerResponse response = new ContainerResponse(context, Response.ok().build());
            final OutputStream os = context.getResponseWriter()
                    .writeResponseStatusAndHeaders("RESOURCE".getBytes().length, response);
            os.write("RESOURCE".getBytes());
            os.close();
        }

        @POST
        @Produces("text/plain")
        public void post(final ContainerRequest context) throws IOException {
            assertThat(context.getMethod(), is("POST"));

            final String s = context.readEntity(String.class);
            assertEquals("RESOURCE", s);

            final ContainerResponse response = new ContainerResponse(context, Response.ok().build());
            final OutputStream os = context.getResponseWriter()
                    .writeResponseStatusAndHeaders("RESOURCE".getBytes().length, response);
            os.write("RESOURCE".getBytes());
            os.close();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testGet() {
        assertThat(target().request().get(String.class), is("RESOURCE"));
    }

    @Test
    public void testPost() {
        assertThat(target().request().post(Entity.text("RESOURCE"), String.class), is("RESOURCE"));
    }

    @Test
    public void testAll() {
        testGet();
        testPost();
    }
}
