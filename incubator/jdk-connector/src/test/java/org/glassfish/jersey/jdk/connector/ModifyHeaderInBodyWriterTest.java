/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector;

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

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class ModifyHeaderInBodyWriterTest extends JerseyTest {

    private static final String WRITE_BYTES = "write bytes";
    private static final String WRITE_BYTE = "byte";
    private static final String HEADER_NAME = "myHeader";
    private static final String HEADER_VALUE = "myHeaderValue";

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, HeaderModifyingWriter.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(HeaderModifyingWriter.class);
        config.connectorProvider(new JdkConnectorProvider());
    }

    @Test
    public void testBufferedWriteBytes() {
        doTestWriteBytes(RequestEntityProcessing.BUFFERED);
    }

    @Test
    public void testChunkedWriteBytes() {
        doTestWriteBytes(RequestEntityProcessing.CHUNKED);
    }

    private void doTestWriteBytes(RequestEntityProcessing requestEntityProcessing) {
        Response response = target("echo").request().property(ClientProperties.REQUEST_ENTITY_PROCESSING, requestEntityProcessing)
                .post(Entity.entity(WRITE_BYTES, MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(HEADER_VALUE, response.getHeaderString(HEADER_NAME));
        assertEquals(WRITE_BYTES, response.readEntity(String.class));
    }

    @Test
    public void testBufferedWriteByte() {
        doTestWriteByte(RequestEntityProcessing.BUFFERED);
    }

    @Test
    public void testChunkedWriteByte() {
        doTestWriteByte(RequestEntityProcessing.CHUNKED);
    }

    private void doTestWriteByte(RequestEntityProcessing requestEntityProcessing) {
        Response response = target("echo").request().property(ClientProperties.REQUEST_ENTITY_PROCESSING, requestEntityProcessing)
                .post(Entity.entity(WRITE_BYTE, MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(HEADER_VALUE, response.getHeaderString(HEADER_NAME));
        assertEquals(WRITE_BYTE, response.readEntity(String.class));
    }

    @Test
    public void testBufferedWriteNothing() {
        doTestWriteNothing(RequestEntityProcessing.BUFFERED);
    }

    @Test
    public void testChunkedWriteNothing() {
        doTestWriteNothing(RequestEntityProcessing.CHUNKED);
    }

    private void doTestWriteNothing(RequestEntityProcessing requestEntityProcessing) {
        Response response = target("echo").request().property(ClientProperties.REQUEST_ENTITY_PROCESSING, requestEntityProcessing)
                .post(Entity.entity("", MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(HEADER_VALUE, response.getHeaderString(HEADER_NAME));
        assertEquals("", response.readEntity(String.class));
    }

    @Provider
    @Produces("text/plain")
    public static class HeaderModifyingWriter implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(
                final Class<?> type,
                final Type genericType,
                final Annotation[] annotations,
                final MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(
                final String t,
                final Class<?> type,
                final Type genericType,
                final Annotation[] annotations,
                final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(
                final String t,
                final Class<?> type,
                final Type genericType,
                final Annotation[] annotations,
                final MediaType mediaType,
                final MultivaluedMap<String, Object> httpHeaders,
                final OutputStream entityStream) throws IOException, WebApplicationException {

            httpHeaders.putSingle(HEADER_NAME, HEADER_VALUE);

            if (WRITE_BYTES.equals(t)) {
                entityStream.write(t.getBytes());
            }

            if (WRITE_BYTE.equals(t)) {
                for (byte b : t.getBytes()) {
                    entityStream.write(b);
                }
            }
        }
    }

    @Path("/")
    public static class Resource {

        @Path("echo")
        @Produces("text/html")
        @POST
        public Response echo(String msg, @HeaderParam(HEADER_NAME) String header) {
            return Response.ok().entity(msg).header(HEADER_NAME, HEADER_VALUE).build();
        }
    }
}
