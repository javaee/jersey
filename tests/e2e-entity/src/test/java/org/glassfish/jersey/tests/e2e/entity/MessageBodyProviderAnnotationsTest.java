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

package org.glassfish.jersey.tests.e2e.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests annotations passed to message body provider.
 *
 * @author Miroslav Fuksa
 */
public class MessageBodyProviderAnnotationsTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(ContainerReaderWriter.class, Resource.class);
    }

    @Test
    public void testWriter() {
        String get = target().path("test").request("test/text").get(String.class);
        assertEquals("get-ok", get);
    }

    @Test
    public void testReader() {
        String get = target().path("test").request("text/plain").post(Entity.entity("test", "test/text"), String.class);
        assertEquals("ok", get);
    }


    @Produces("test/text")
    @Consumes("test/text")
    public static class ContainerReaderWriter implements MessageBodyWriter<String>, MessageBodyReader<Bean> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
                WebApplicationException {
            OutputStreamWriter osw = new OutputStreamWriter(entityStream);

            if (compareAnnotations(new Class<?>[]{MyAnnotation.class, Produces.class, GET.class}, annotations)) {
                osw.write(s + "-ok");
            } else {
                osw.write(s + "-fail");
            }
            osw.flush();
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == Bean.class;
        }

        @Override
        public Bean readFrom(Class<Bean> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                             MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
                WebApplicationException {
            if (compareAnnotations(new Class<?>[]{MyAnnotation.class}, annotations)) {
                return new Bean("ok");
            } else {
                return new Bean("fail");
            }
        }
    }

    public static class Bean {
        private final String value;

        public Bean(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static boolean compareAnnotations(Class[] expected, Annotation[] actual) {
        if (expected.length != actual.length) {
            return false;
        }

        for (Class<?> e : expected) {
            boolean found = false;
            for (Annotation a : actual) {
                if (e.equals(a.annotationType())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static final Annotation ANNOTATION;

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface MyAnnotation {
    }

    static {
        @MyAnnotation
        class TempClass {
        }
        ;
        ANNOTATION = TempClass.class.getAnnotation(MyAnnotation.class);
    }

    @Path("test")
    public static class Resource {
        @GET
        @Produces("test/text")
        public Response get() {
            Response response = Response.ok().entity("get", new Annotation[]{ANNOTATION}).build();
            return response;
        }

        @POST
        @Consumes("test/text")
        @Produces("text/plain")
        public String post(@MyAnnotation Bean entity) {
            return entity.getValue();
        }
    }
}
