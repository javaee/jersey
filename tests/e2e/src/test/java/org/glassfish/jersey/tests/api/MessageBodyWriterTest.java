/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.api;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test of {@link MessageBodyWriter}.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class MessageBodyWriterTest extends JerseyTest {

    public static final String HEADER_NAME = "MessageBodyWriterTestHeader";
    public static final String HEADER_VALUE_CLIENT = "Client";
    public static final String HEADER_VALUE_SERVER = "Server";


    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, StringWriter.class);
    }

    @Provider
    @Produces("text/plain")
    public static class StringWriter implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(
                String t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(
                String t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException, WebApplicationException {
            httpHeaders.putSingle(HEADER_NAME, HEADER_VALUE_SERVER);
            entityStream.write(t.getBytes());
        }
    }

    @Path("/")
    public static class Resource {

        @POST
        public String post(@HeaderParam(HEADER_NAME) final String header,
                           final String post) {
            assertEquals(HEADER_VALUE_CLIENT, header);
            return post;

        }

    }

    @Test
    public void testHeaders() throws Exception {
        final Response response = target().path("/").request("text/plain").
                header(HEADER_NAME, HEADER_VALUE_CLIENT).post(Entity.entity("content", "text/plain"));

        assertEquals("content", response.readEntity(String.class));
        assertEquals(HEADER_VALUE_SERVER, response.getHeaderString(HEADER_NAME));
    }

}
