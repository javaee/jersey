/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.server;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * JERSEY-2500 reproducer test.
 *
 * Tests, that correct exceptions are thrown in case no MessageBodyProvider was matched on server.
 *
 * - InternalServerErrorException for MBW (JSR339, chapter 4.2.2, step 7)
 * - NotSupportedException for MBR (JSR339, chapter 4.2.1, step 6)
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class MessageBodyProvidersExceptionsTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(MessageBodyProvidersExceptionsTest.class.getName());

    @Override
    protected Application configure() {
        return new ResourceConfig(
                Resource.class,
                WebAppExceptionMapper.class
        );
    }

    @Path("resource")
    public static class Resource {

        @GET
        @Path("write")
        @Produces(MediaType.TEXT_PLAIN)
        public Resource failOnWrite() {
            return this;
        }

        @POST
        @Path("read")
        @Consumes("foo/bar")
        @Produces(MediaType.TEXT_PLAIN)
        public String failOnRead() {
            return "this-should-never-be-returned";
        }
    }

    @Provider
    public static class WebAppExceptionMapper implements ExceptionMapper<WebApplicationException> {

        @Override
        public Response toResponse(WebApplicationException exception) {
            LOGGER.fine("ExceptionMapper was invoked.");
            // return the exception class name as an entity for further comparison
            return Response.status(200).header("writer-exception", "after-first-byte").entity(exception.getClass().getName())
                    .build();
        }
    }

    @Test
    public void testReaderThrowsCorrectException() {
        Response response = target().path("resource/write").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());
        String resString = response.readEntity(String.class);
        // no MBW should have been found, InternalServerErrorException expected
        assertEquals("javax.ws.rs.InternalServerErrorException", resString);
    }

    @Test
    public void testWriterThrowsCorrectException() {
        Response response = target().path("resource/read").request().post(Entity.entity("Hello, world", "text/plain"));
        assertEquals(200, response.getStatus());
        String resString = response.readEntity(String.class);
        // no MBR should have been found, NotSupportedException expected
        assertEquals("javax.ws.rs.NotSupportedException", resString);
    }
}
