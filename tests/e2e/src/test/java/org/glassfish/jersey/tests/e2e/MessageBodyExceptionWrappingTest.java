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

package org.glassfish.jersey.tests.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import javax.xml.transform.stream.StreamSource;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static javax.ws.rs.client.Entity.entity;

/**
 * @author Miroslav Fuksa
 */
public class MessageBodyExceptionWrappingTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(TestResource.class);
    }

    /**
     * Tests whether fail of message body writer causes throwing exception. Previously the
     * exception was not thrown and 500 status code was returned in the response.
     */
    @Test
    public void testWrapping() {
        WebTarget resource = target().path("test");
        StreamSource source = new StreamSource() {
            @Override
            public InputStream getInputStream() {
                throw new WebApplicationException(555);
            }
        };
        try {
            Response response = resource.request().post(Entity.entity(source, MediaType.TEXT_XML_TYPE));
            fail("Exception expected, instead response with " + response.getStatus() + " status has been returned.");
        } catch (ProcessingException e) {
            assertEquals(WebApplicationException.class, e.getCause().getClass());
            assertEquals(555, ((WebApplicationException) e.getCause()).getResponse().getStatus());
        }
    }

    @Path("test")
    public static class TestResource {

        @POST
        public String echo(String entity) {
            return entity;
        }
    }

    /**
     * Provider reproducing JERSEY-1990.
     */
    @Produces("text/foo")
    public static class ThrowingUpProvider implements MessageBodyWriter<String>, MessageBodyReader<String> {
        public volatile int counter = 0;

        public int getInvocationCount() {
            return counter;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            counter++;
            final RuntimeException up = new RuntimeException("lunch");
            throw up;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
            throw new UnsupportedOperationException("This method should not have ever been called.");
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            counter++;
            final RuntimeException up = new RuntimeException("dinner");
            throw up;
        }

        @Override
        public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders,
                               InputStream entityStream) throws IOException, WebApplicationException {
            throw new UnsupportedOperationException("This method should not have ever been called.");
        }
    }

    /**
     * Reproducer for JERSEY-1990.
     */
    @Test
    public void testMbwHandlingExceptionInIsReadableWritable() {
        ThrowingUpProvider throwingUpProvider = new ThrowingUpProvider();

        final String response =
                target("test").register(throwingUpProvider).request("text/foo").post(entity("hello", "text/foo"), String.class);

        assertEquals("hello", response);
        assertEquals(2, throwingUpProvider.getInvocationCount());
    }

}
