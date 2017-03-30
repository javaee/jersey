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

package org.glassfish.jersey.tests.e2e.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ProvidersOrderingTest extends JerseyTest {

    public static class MyType {
    }

    public static class MyTypeExt extends MyType {
    }

    public static class MyTypeExtExt extends MyTypeExt {
    }

    @Produces("application/test1")
    public static class MyMBW1 implements MessageBodyWriter<MyType> {

        private final List<Class<?>> callList;

        public MyMBW1(List<Class<?>> callList) {
            this.callList = callList;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            callList.add(this.getClass());
            return false;
        }

        @Override
        public long getSize(MyType myType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(MyType myType,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
        }
    }

    @Produces("application/*")
    public static class MyMBW2 implements MessageBodyWriter<MyType> {

        private final List<Class<?>> callList;

        public MyMBW2(List<Class<?>> callList) {
            this.callList = callList;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            callList.add(this.getClass());
            return false;
        }

        @Override
        public long getSize(MyType myType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(MyType myType,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
        }
    }

    @Produces("*/*")
    public static class MyMBW3 implements MessageBodyWriter<MyType> {

        private final List<Class<?>> callList;

        public MyMBW3(List<Class<?>> callList) {
            this.callList = callList;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            callList.add(this.getClass());
            return true;
        }

        @Override
        public long getSize(MyType myType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(MyType myType,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write("test".getBytes());
        }
    }

    @Produces("application/*")
    public static class MyMBW4 implements MessageBodyWriter<MyTypeExt> {

        private final List<Class<?>> callList;

        public MyMBW4(List<Class<?>> callList) {
            this.callList = callList;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            callList.add(this.getClass());
            return false;
        }

        @Override
        public long getSize(MyTypeExt myType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(MyTypeExt myType,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
        }
    }

    @Produces("application/xml")
    public static class MyMBW5 implements MessageBodyWriter<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return false;
        }

        @Override
        public long getSize(Object myType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(Object myType,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
        }
    }

    @Consumes("application/test")
    public static class MyMBR1 implements MessageBodyReader<MyType> {

        private final List<Class<?>> callList;

        public MyMBR1(List<Class<?>> callList) {
            this.callList = callList;
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            callList.add(this.getClass());
            return false;
        }

        @Override
        public MyType readFrom(Class<MyType> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders,
                               InputStream entityStream) throws IOException, WebApplicationException {
            return null;
        }
    }

    @Consumes("application/test1")
    public static class MyMBR2 implements MessageBodyReader<MyTypeExt> {

        private final List<Class<?>> callList;

        public MyMBR2(List<Class<?>> callList) {
            this.callList = callList;
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            callList.add(this.getClass());
            return false;
        }

        @Override
        public MyTypeExt readFrom(Class<MyTypeExt> type,
                                  Type genericType,
                                  Annotation[] annotations,
                                  MediaType mediaType,
                                  MultivaluedMap<String, String> httpHeaders,
                                  InputStream entityStream) throws IOException, WebApplicationException {
            return null;
        }
    }

    @Consumes("application/test1")
    public static class MyMBR3 implements MessageBodyReader<MyTypeExtExt> {

        private final List<Class<?>> callList;

        public MyMBR3(List<Class<?>> callList) {
            this.callList = callList;
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            callList.add(this.getClass());
            return true;
        }

        @Override
        public MyTypeExtExt readFrom(Class<MyTypeExtExt> type,
                                     Type genericType,
                                     Annotation[] annotations,
                                     MediaType mediaType,
                                     MultivaluedMap<String, String> httpHeaders,
                                     InputStream entityStream) throws IOException, WebApplicationException {
            return new MyTypeExtExt();
        }
    }

    @Path("/")
    public static class MyResource {

        @Context
        MessageBodyWorkers messageBodyWorkers;

        @PUT
        @Consumes("application/test1")
        public MyType getMyType(MyTypeExtExt myType) {

            final Map<MediaType, List<MessageBodyWriter>> writers = messageBodyWorkers.getWriters(MediaType.APPLICATION_XML_TYPE);
            final List<MessageBodyWriter> messageBodyWriters = writers.get(MediaType.APPLICATION_XML_TYPE);

            assertTrue(messageBodyWriters.get(0) instanceof MyMBW5);

            return myType;
        }

        @Path("bytearray")
        @POST
        public byte[] bytearray(byte[] bytes) {
            return bytes;
        }
    }

    private List<Class<?>> callList;

    @Override
    protected Application configure() {
        callList = new ArrayList<>();

        final ResourceConfig resourceConfig = new ResourceConfig(MyResource.class, MyMBW5.class);
        resourceConfig.registerInstances(new MyMBW1(callList), new MyMBW2(callList), new MyMBW3(callList), new MyMBW4(callList),
                new MyMBR1(callList), new MyMBR2(callList), new MyMBR3(callList), new MyByteArrayProvider());
        return resourceConfig;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void orderingTest() {
        callList.clear();
        WebTarget target = target();

        try {
            Response response = target.request("application/test1")
                    .put(Entity.entity("test", "application/test1"), Response.class);

            assertNotNull(response);
            assertEquals("Request was not handled correctly, most likely fault in MessageBodyWorker selection.",
                    200, response.getStatus());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail("Request was not handled correctly, most likely fault in MessageBodyWorker selection.");
        }

        List<Class<?>> classes = Arrays.asList(
                MyMBR3.class, // MBR - smallest type distance (MBR1 and MBR2 are not called because of that)
                MyMBW4.class, // MBW - type distance
                MyMBW1.class, // MBW - most specific media type application/test1
                MyMBW2.class, // MBW - application/*
                MyMBW3.class  // MBW - */*, first usable writer (returns true to isWriteable call)
        );

        assertEquals(classes, callList);
    }

    @Test
    public void replaceBuiltInProvider() {
        callList.clear();
        WebTarget target = target("bytearray");

        try {
            Response response = target.request().post(Entity.text("replaceBuiltInProvider"), Response.class);

            assertNotNull(response);
            assertEquals("Request was not handled correctly, most likely fault in MessageBodyWorker selection.",
                    200, response.getStatus());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail("Request was not handled correctly, most likely fault in MessageBodyWorker selection.");
        }

        assertTrue(MyByteArrayProvider.counter == 2); // used to read and write entity on server side.

    }

    @Produces({"application/octet-stream", "*/*"})
    @Consumes({"application/octet-stream", "*/*"})
    public static final class MyByteArrayProvider extends AbstractMessageReaderWriterProvider<byte[]> {

        public static int counter = 0;

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == byte[].class;
        }

        @Override
        public byte[] readFrom(
                Class<byte[]> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders,
                InputStream entityStream) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeTo(entityStream, out);
            counter++;
            return out.toByteArray();
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == byte[].class;
        }

        @Override
        public void writeTo(
                byte[] t,
                Class<?> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException {
            counter++;
            entityStream.write(t);
        }

        @Override
        public long getSize(byte[] t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return t.length;
        }
    }
}
