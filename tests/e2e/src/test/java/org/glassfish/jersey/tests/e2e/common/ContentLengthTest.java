/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ContentLengthTest extends JerseyTest {

    public static String STR = "string";

    @Override
    protected Application configure() {
        ResourceConfig rc = new ResourceConfig();
        rc.registerClasses(MyTypeResource.class, MyTypeWriter.class, ResourceGetByteNoHead.class);
        return rc;
    }

    public static class MyType {

        public String s = STR;
    }

    @Path("/")
    public static class MyTypeResource {

        @GET
        public MyType getMyType() {
            return new MyType();
        }
    }

    @Provider
    public static class MyTypeWriter implements MessageBodyWriter<MyType> {

        @Override
        public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            return aClass.equals(MyType.class);
        }

        @Override
        public long getSize(MyType myType, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            return myType.s.length();
        }

        @Override
        public void writeTo(MyType myType,
                            Class<?> aClass,
                            Type type,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> stringObjectMultivaluedMap,
                            OutputStream outputStream) throws IOException, WebApplicationException {
            outputStream.write(myType.s.getBytes());
        }
    }

    @Test
    public void testGetContentLengthCustomWriter() throws Exception {
        Response response = target().request().get(Response.class);
        assertEquals(200, response.getStatus());
        assertEquals(STR.length(), Integer.parseInt(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)));
        assertTrue(response.hasEntity());
    }

    @Test
    public void testHeadContentLengthCustomWriter() throws Exception {
        Response response = target().request().head();
        assertEquals(200, response.getStatus());
        assertEquals(STR.length(), Integer.parseInt(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)));
        assertFalse(response.hasEntity());
    }

    @Path("/byte")
    public static class ResourceGetByteNoHead {

        @GET
        public byte[] get() {
            return "GET".getBytes();
        }
    }

    @Test
    public void testGetByte() throws Exception {
        Response response = target().path("byte").request().get(Response.class);
        assertEquals(200, response.getStatus());
        assertEquals(3, Integer.parseInt(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)));
        assertTrue(response.hasEntity());
    }

    @Test
    public void testHeadByte() throws Exception {
        Response response = target().path("byte").request().head();
        assertEquals(200, response.getStatus());
        assertEquals(3, Integer.parseInt(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)));
        assertFalse(response.hasEntity());
    }
}
