/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Make sure exceptions, that are not mapped to responses get logged.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ExceptionLoggingTest extends JerseyTest {

    private static class MyCheckedException extends Exception {}

    private static class MyRuntimeException extends RuntimeException {}

    @Path("/")
    public static class ExceptionResource {

        @Path("runtime")
        public String runtimeException() {
            throw new MyRuntimeException();
        }

        @Path("checked")
        public String checkedException() throws MyCheckedException {
            throw new MyCheckedException();
        }
    }

    @Override
    protected Application configure() {
        set(TestProperties.RECORD_LOG_LEVEL, Level.FINE.intValue());

        return new ResourceConfig(ExceptionResource.class, Writer.class, Resource.class);
    }

    @Test
    public void testRuntime() throws Exception {
        final Response response = target().path("runtime").request().get();
        assertEquals(500, response.getStatus());
        assertEquals(getLastLoggedRecord().getThrown().getClass(), MyRuntimeException.class);
    }

    @Test
    public void testChecked() throws Exception {
        final Response response = target().path("checked").request().get();
        assertEquals(500, response.getStatus());
        assertEquals(getLastLoggedRecord().getThrown().getClass(), MyCheckedException.class);
    }

    @Provider
    @Produces(MediaType.WILDCARD)
    public static class Writer implements MessageBodyWriter<ExceptionLoggingTestPOJO> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == ExceptionLoggingTestPOJO.class;
        }

        @Override
        public long getSize(ExceptionLoggingTestPOJO entityForReader, Class<?> type, Type genericType,
                            Annotation[] annotations, MediaType mediaType) {
            return 0;
        }

        @Override
        public void writeTo(ExceptionLoggingTestPOJO entityForReader, Class<?> type, Type genericType,
                            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
            throw new RuntimeException("test");
        }
    }

    @Path("resource")
    public static class Resource {

        @Path("entity")
        @GET
        public ExceptionLoggingTestPOJO entity() {
            return new ExceptionLoggingTestPOJO();
        }
    }

    @Test
    public void testReaderFails() throws Exception {
        final Response response = target().path("resource/entity").request().get();
        assertEquals(500, response.getStatus());

        assertEquals(getLastLoggedRecord().getThrown().getMessage(), "test");
    }

    static class ExceptionLoggingTestPOJO {

    }
}
