/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.test.JerseyTest;

/**
 * Parent for set of ParameterTypeArgumentOrder tests.
 *
 * Contains all the providers and resources for the tests. The resource config creation and test methods were separated into
 * subclasses.
 *
 * @author Paul Sandoz
 */
public abstract class AbstractParameterTypeArgumentOrderTest extends JerseyTest {

    @Provider
    public static class ObjectWriter implements MessageBodyWriter {

        @Override
        public boolean isWriteable(final Class type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(final Object o, final Class type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final Object o, final Class type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap httpHeaders, final OutputStream entityStream)
                throws IOException, WebApplicationException {
            entityStream.write(o.toString().getBytes());
        }
    }

    public static class GenericClassWriter<T> implements MessageBodyWriter<T> {

        private final Class c;

        GenericClassWriter(final Class c) {
            this.c = c;
        }

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return c.isAssignableFrom(type);
        }

        @Override
        public long getSize(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write((c.getSimpleName() + type.getSimpleName()).getBytes());
        }
    }

    public static class A {}

    @Provider
    public static class AWriter extends GenericClassWriter<A> {

        public AWriter() {
            super(A.class);
        }
    }

    public static class B extends A {}

    @Provider
    public static class BWriter extends GenericClassWriter<B> {

        public BWriter() {
            super(B.class);
        }
    }

    public static class C extends B {}

    @Provider
    public static class CWriter extends GenericClassWriter<C> {

        public CWriter() {
            super(C.class);
        }
    }

    @Path("/")
    public static class ClassResource {

        @GET
        @Path("a")
        public A getA() {
            return new A();
        }

        @GET
        @Path("b")
        public B getB() {
            return new B();
        }

        @GET
        @Path("c")
        public C getC() {
            return new C();
        }
    }

    public static class GenericClassReaderWriter<T> implements MessageBodyWriter<T>, MessageBodyReader<T> {

        private final Class c;

        GenericClassReaderWriter(final Class c) {
            this.c = c;
        }

        @Override
        public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                  final MediaType mediaType) {
            return c.isAssignableFrom(type);
        }

        @Override
        public T readFrom(final Class<T> type, final Type genericType, final Annotation[] annotations,
                          final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                          final InputStream entityStream) throws IOException, WebApplicationException {
            try {
                return (T) c.newInstance();
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return c.isAssignableFrom(type);
        }

        @Override
        public long getSize(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write((c.getSimpleName() + type.getSimpleName()).getBytes());
        }
    }

    @Provider
    public static class AReaderWriter<T> extends GenericClassReaderWriter<T> {

        public AReaderWriter() {
            super(A.class);
        }
    }

    @Provider
    public static class BReaderWriter extends GenericClassReaderWriter<B> {

        public BReaderWriter() {
            super(B.class);
        }
    }

    @Provider
    public static class CReaderWriter extends GenericClassReaderWriter<C> {

        public CReaderWriter() {
            super(C.class);
        }
    }

}
