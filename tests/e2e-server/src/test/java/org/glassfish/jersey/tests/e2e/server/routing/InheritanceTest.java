/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.routing;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Michal Gajdos
 */
public class InheritanceTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class).register(WriterA.class).register(WriterB.class).register(WriterString.class);
    }

    public static class A {

        String value;

        public A() {
            this.value = "a";
        }

        public A(final String value) {
            this.value = value;
        }
    }

    public static class B extends A {

        public B() {
            super("b");
        }

        public B(final String value) {
            super("b" + value);
        }
    }

    @Path("/")
    public static class Resource {

        @GET
        public A getA() {
            return new B("a");
        }
    }

    public static class WriterA implements MessageBodyWriter<A> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return A.class.isAssignableFrom(type);
        }

        @Override
        public long getSize(final A a, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final A a, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(("a" + a.value).getBytes());
        }
    }

    @Produces({"text/plain"})
    public static class WriterString implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(s.getBytes());
        }
    }

    @Produces("b/b")
    public static class WriterB implements MessageBodyWriter<B> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return B.class.isAssignableFrom(type);
        }

        @Override
        public long getSize(final B b, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final B b,
                            final Class<?> type,
                            final Type genericType,
                            final Annotation[] annotations,
                            final MediaType mediaType,
                            final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(("b" + b.value).getBytes());
        }
    }

    @Test
    public void testWriterEntityInheritance() throws Exception {
        assertThat(target().request("a/b").get(String.class), equalTo("aba"));
    }
}
