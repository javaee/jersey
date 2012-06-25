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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Taken from Jersey 1: jersey-tests:com.sun.jersey.impl.resource.AcceptWriterTest.java
 *
 * @author Paul Sandoz
 */
public class AcceptWriterTest {


    public static class StringWrapper {
        public final String s;

        public StringWrapper(String s) {
            this.s = s;
        }
    }

    @Provider
    @Produces("application/foo")
    public static class FooStringWriter implements MessageBodyWriter<StringWrapper> {
        MediaType foo = new MediaType("application", "foo");

        @Override
        public boolean isWriteable(
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
           return type == StringWrapper.class && foo.isCompatible(mediaType);
        }

        @Override
        public long getSize(
                StringWrapper t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(
                StringWrapper t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException, WebApplicationException {
            String s = "foo: " + t.s;
            entityStream.write(s.getBytes());
        }
    }

    @Provider
    @Produces("application/bar")
    public static class BarStringWriter implements MessageBodyWriter<StringWrapper> {
        MediaType bar = new MediaType("application", "bar");

        @Override
        public boolean isWriteable(
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
           return type == StringWrapper.class && bar.isCompatible(mediaType);
        }

        @Override
        public long getSize(
                StringWrapper t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(
                StringWrapper t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException, WebApplicationException {
            String s = "bar: " + t.s;
            entityStream.write(s.getBytes());
        }
    }

    @Path("/")
    public static class Resource {
        @GET
        public StringWrapper get() {
            return new StringWrapper("content");
        }
    }

    private void _test(ApplicationHandler app, String expected, String method, Object entity, String mediaType, String... accept)
            throws ExecutionException, InterruptedException {
        RequestContextBuilder requestContextBuilder = RequestContextBuilder.from("/", method);
        if (entity != null) {
            requestContextBuilder.entity(entity).type(mediaType);
        }
        ContainerRequest requestContext = requestContextBuilder.accept(accept).build();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        app.apply(requestContext, baos);
        assertEquals(expected, baos.toString());
    }

    @Test
    public void testAcceptGet() throws Exception {

        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, FooStringWriter.class, BarStringWriter.class);
        final ApplicationHandler app = new ApplicationHandler(resourceConfig);

        _test(app, "foo: content", "GET", null, null, "application/foo");
        _test(app, "foo: content", "GET", null, null, "applcation/baz, application/foo;q=0.8");
        _test(app, "bar: content", "GET", null, null, "application/bar");
        _test(app, "bar: content", "GET", null, null, "applcation/baz, application/bar;q=0.8");
    }
}
