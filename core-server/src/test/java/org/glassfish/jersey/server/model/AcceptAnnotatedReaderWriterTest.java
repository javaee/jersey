/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import junit.framework.Assert;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class AcceptAnnotatedReaderWriterTest {

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }

    public static class StringWrapper {

        public String s;

        public StringWrapper(String s) {
            this.s = s;
        }
    }

    public static class StringWrapperFoo extends StringWrapper {

        public StringWrapperFoo(String s) {
            super(s);
        }
    }

    public static class StringWrapperBar extends StringWrapper {

        public StringWrapperBar(String s) {
            super(s);
        }
    }

    public static final String APPLICATION_BAR = "application/bar";
    public static final String APPLICATION_FOO = "application/foo";

    public static abstract class StringWrapperWorker<T extends StringWrapper> implements MessageBodyReader<T>, MessageBodyWriter<T> {

        @Provider
        @Produces(APPLICATION_BAR)
        @Consumes(APPLICATION_BAR)
        public static class BarStringWorker<StringWrapper> extends StringWrapperWorker {

            @Override
            MediaType getMediaType() {
                return MediaType.valueOf(APPLICATION_BAR);
            }

            @Override
            String getPrefix() {
                return "bar: ";
            }
        }

        @Provider
        @Produces(APPLICATION_BAR)
        @Consumes(APPLICATION_BAR)
        public static class BarBarStringWorker<StringWrapperBar> extends BarStringWorker {
        }

        @Provider
        @Produces(APPLICATION_FOO)
        @Consumes(APPLICATION_FOO)
        public static class FooStringWorker<StringWrapper> extends StringWrapperWorker {

            @Override
            MediaType getMediaType() {
                return MediaType.valueOf(APPLICATION_FOO);
            }

            @Override
            String getPrefix() {
                return "foo: ";
            }
        }

        @Provider
        @Produces(APPLICATION_FOO)
        @Consumes(APPLICATION_FOO)
        public static class FooFooStringWorker<StringWrapperFoo> extends FooStringWorker {
        }

        abstract MediaType getMediaType();
        abstract String getPrefix();

        @Override
        public boolean isWriteable(
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
            return javaAndMediaTypeKnown(type, mediaType);
        }

        private boolean javaAndMediaTypeKnown(final Class<?> clazz, final MediaType mediaType) {
            String wrapperName = getClass().getTypeParameters()[0].getName();
            return clazz.getSimpleName().equals(wrapperName) && getMediaType().isCompatible(mediaType);
        }

        @Override
        public long getSize(
                T t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(
                T t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException, WebApplicationException {
            String s = getPrefix() + t.s;
            entityStream.write(s.getBytes());
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return javaAndMediaTypeKnown(type, mediaType);
        }

        @Override
        public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            try {
                T result = type.newInstance();
                result.s = readString(entityStream).substring(getPrefix().length());
                return result;
            } catch (Exception e) {
                return null;
            }
        }

    }

    @Path("/")
    public static class TwoGetMethodsResource {

        @GET
        public StringWrapperFoo getFoo() {
            return new StringWrapperFoo("1st");
        }

        @GET
        public StringWrapperBar getBar() {
            return new StringWrapperBar("2nd");
        }
    }

    @Test
    public void testAcceptGet() throws Exception {

        ApplicationHandler app = createApplication(TwoGetMethodsResource.class, StringWrapperWorker.FooFooStringWorker.class, StringWrapperWorker.BarBarStringWorker.class);

        assertEquals("foo: 1st", app.apply(Requests.from("/", "GET").accept("application/foo").build()).get().readEntity(String.class));
        assertEquals("foo: 1st", app.apply(Requests.from("/", "GET").accept("application/bar;q=0.8, application/foo").build()).get().readEntity(String.class));
        assertEquals("bar: 2nd", app.apply(Requests.from("/", "GET").accept("application/bar;q=0.5, application/foo;q=0.2").build()).get().readEntity(String.class));
        assertEquals("foo: 1st", app.apply(Requests.from("/", "GET").accept("applcation/baz, application/foo;q=0.8").build()).get().readEntity(String.class));
        assertEquals("bar: 2nd", app.apply(Requests.from("/", "GET").accept("application/bar").build()).get().readEntity(String.class));
        assertEquals("bar: 2nd", app.apply(Requests.from("/", "GET").accept("application/foo;q=0.8, application/bar").build()).get().readEntity(String.class));
        assertEquals("bar: 2nd", app.apply(Requests.from("/", "GET").accept("applcation/baz, application/bar;q=0.8").build()).get().readEntity(String.class));
    }

    @Path("/")
    public static class SingleGetMethodResource {

        @GET
        public StringWrapper get() {
            return new StringWrapper("content");
        }
    }

    @Test
    public void testSingleMethodAcceptGet() throws Exception {

        final ApplicationHandler app = createApplication(SingleGetMethodResource.class, StringWrapperWorker.FooStringWorker.class, StringWrapperWorker.BarStringWorker.class);

        assertEquals("foo: content", app.apply(Requests.from("/", "GET").accept("application/foo").build()).get().readEntity(String.class));
        assertEquals("foo: content", app.apply(Requests.from("/", "GET").accept("application/bar;q=0.5, application/foo").build()).get().readEntity(String.class));
        assertEquals("foo: content", app.apply(Requests.from("/", "GET").accept("applcation/baz, application/foo;q=0.8").build()).get().readEntity(String.class));
        assertEquals("bar: content", app.apply(Requests.from("/", "GET").accept("application/bar").build()).get().readEntity(String.class));
        assertEquals("bar: content", app.apply(Requests.from("/", "GET").accept("application/foo;q=0.5, application/bar").build()).get().readEntity(String.class));
        assertEquals("bar: content", app.apply(Requests.from("/", "GET").accept("applcation/baz, application/bar;q=0.8").build()).get().readEntity(String.class));
    }

    @Path("/")
    public static class MultiplePostMethodResource {

        @Context HttpHeaders httpHeaders;

        @POST
        public StringWrapperBar postFoo2Bar(StringWrapperFoo foo) {
            Assert.assertEquals("application/foo", httpHeaders.getMediaType().toString());
            return new StringWrapperBar(foo.s);
        }

        @POST
        public StringWrapperBar postBar2Bar(StringWrapperBar bar) {
            Assert.assertEquals("application/bar", httpHeaders.getMediaType().toString());
            return new StringWrapperBar(bar.s);
        }

        @POST
        public StringWrapperFoo postBar2Foo(StringWrapperBar bar) {
            Assert.assertEquals("application/bar", httpHeaders.getMediaType().toString());
            return new StringWrapperFoo(bar.s);
        }

        @POST
        public StringWrapperFoo postFoo2Foo(StringWrapperFoo foo) {
            Assert.assertEquals("application/foo", httpHeaders.getMediaType().toString());
            return new StringWrapperFoo(foo.s);
        }
    }

    @Test
    public void testSingleMethodConsumesProducesPost() throws Exception {

        final ApplicationHandler app = createApplication(MultiplePostMethodResource.class, StringWrapperWorker.FooFooStringWorker.class, StringWrapperWorker.BarBarStringWorker.class);

        assertEquals("foo: foo", app.apply(Requests.from("/", "POST").entity(new StringWrapperFoo("foo")).type(APPLICATION_FOO).accept(APPLICATION_FOO).build()).get().readEntity(String.class));
        assertEquals("foo: bar", app.apply(Requests.from("/", "POST").entity(new StringWrapperBar("bar")).type(APPLICATION_BAR).accept(APPLICATION_FOO).build()).get().readEntity(String.class));
        assertEquals("bar: foo", app.apply(Requests.from("/", "POST").entity(new StringWrapperFoo("foo")).type(APPLICATION_FOO).accept(APPLICATION_BAR).build()).get().readEntity(String.class));
        assertEquals("bar: bar", app.apply(Requests.from("/", "POST").entity(new StringWrapperBar("bar")).type(APPLICATION_BAR).accept(APPLICATION_BAR).build()).get().readEntity(String.class));
    }

    static String readString(InputStream is) throws IOException {
        byte[] buffer = new byte[2048];
        StringBuilder result = new StringBuilder();
        int i;
        while ((i = is.read(buffer)) > -1) {
            result.append(new String(buffer, 0, i));
        }
        is.close();
        return result.toString();
    }
}
