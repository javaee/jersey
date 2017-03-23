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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Class testing that providers are managed correctly in the singleton scope.
 *
 * @author Miroslav Fuksa
 */
public class SingletonProviderTest extends JerseyTest {
    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(SingletonResource.class, AnyFilter.class,
                ExceptionMappingProvider.class, SingletonStringMessageProvider.class, MyContextResolver.class);
    }

    @Test
    public void filterTest() {
        String str;
        str = target().path("resource/filter").request().get().readEntity(String.class);
        assertEquals("filter:1", str);

        str = target().path("resource/filter").request().get().readEntity(String.class);
        assertEquals("filter:2", str);
    }


    @Test
    public void exceptionMapperTest() {
        String str;
        str = target().path("resource/exception").request().get().readEntity(String.class);
        assertEquals("mapper:1", str);
        str = target().path("resource/exception").request().get().readEntity(String.class);
        assertEquals("mapper:2", str);

    }

    @Test
    public void messageBodyWriterTest() {
        String str1;
        str1 = target().path("resource/messagebody").request("text/test").get().readEntity(String.class);
        assertTrue(str1.endsWith(":1"));
        String str2;
        str2 = target().path("resource/messagebody").request("text/test").get().readEntity(String.class);
        assertTrue(str2.endsWith(":2"));

        assertEquals(str1.substring(0, str1.length() - 2), str2.substring(0, str2.length() - 2));
    }

    @Test
    public void messageBodyReaderTest() {
        String str1 = target().path("resource/messagebodyreader").request("text/plain")
                .put(Entity.entity("from-client", "text/test")).readEntity(String.class);
        assertTrue(str1.endsWith(":1"));
        String str2 = target().path("resource/messagebodyreader").request("text/plain").put(Entity.entity("from-client",
                "text/test")).readEntity(String.class);
        assertTrue(str2.endsWith(":2"));

        assertEquals(str1.substring(0, str1.length() - 2), str2.substring(0, str2.length() - 2));
    }

    @Test
    public void contextResolverTest() {
        String str1 = target().path("resource/context").request("text/plain").get().readEntity(String.class);
        assertEquals("context:1", str1);

        String str2 = target().path("resource/context").request("text/plain").get().readEntity(String.class);
        assertEquals("context:2", str2);
    }

    @Path("resource")
    public static class SingletonResource {
        @GET
        @Path("filter")
        public String getCounterFromFilter(@HeaderParam("counter") int counter) {
            return "filter:" + counter;
        }

        @GET
        @Path("exception")
        public String throwException() {
            throw new SingletonTestException("test exception");
        }

        @GET
        @Path("messagebody")
        @Produces("text/test")
        public String messageBodyTest() {
            return "messagebody:";
        }

        @PUT
        @Path("messagebodyreader")
        @Produces("text/plain")
        @Consumes("text/test")
        public String messageBodyReaderTest(String entity) {
            return "put:" + entity;
        }

        @GET
        @Path("context")
        @Produces(MediaType.TEXT_PLAIN)
        public String testContextResolver(@Context ContextResolvers resolvers) {
            ContextResolver<String> contextResolver = resolvers.resolve(String.class, MediaType.TEXT_PLAIN_TYPE);
            String context = contextResolver.getContext(String.class);
            return context;
        }
    }


    public static class AnyFilter implements ContainerRequestFilter {
        private int counter = 1;

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.getHeaders().add("counter", String.valueOf(counter++));
        }
    }


    @Provider
    public static class ExceptionMappingProvider implements ExceptionMapper<SingletonTestException> {
        private int counter = 1;

        @Override
        public Response toResponse(SingletonTestException exception) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("mapper:" + counter++).build();
        }
    }

    public static class SingletonTestException extends RuntimeException {
        public SingletonTestException() {
            super();
        }

        public SingletonTestException(String message) {
            super(message);
        }
    }

    @Produces(MediaType.TEXT_PLAIN)
    public static class MyContextResolver implements ContextResolver<String> {
        private int i = 1;

        @Override
        public String getContext(Class<?> type) {
            if (type == String.class) {
                return "context:" + i++;
            }
            return null;
        }
    }

    @Produces({"text/test"})
    @Consumes({"text/test"})
    public static final class SingletonStringMessageProvider extends AbstractMessageReaderWriterProvider<String> {
        private int counter = 1;
        private int readerCounter = 1;


        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public String readFrom(
                Class<String> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders,
                InputStream entityStream) throws IOException {
            return readFromAsString(entityStream, mediaType) + this + ":" + readerCounter++;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(
                String t,
                Class<?> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException {
            writeToAsString(t + this + ":" + counter++, entityStream, mediaType);
        }
    }
}
