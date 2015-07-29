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

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.InterceptorContext;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.tests.e2e.InterceptorGzipTest.GZIPReaderTestInterceptor;
import org.glassfish.jersey.tests.e2e.InterceptorGzipTest.GZIPWriterTestInterceptor;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests interceptors.
 *
 * @author Miroslav Fuksa
 * @author Michal Gajdos
 */
public class InterceptorCustomTest extends JerseyTest {

    private static final String FROM_RESOURCE = "-from_resource";
    private static final String ENTITY = "hello, this is text entity";

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(TestResource.class, GZIPWriterTestInterceptor.class, GZIPReaderTestInterceptor.class,
                PlusOneWriterInterceptor.class, MinusOneReaderInterceptor.class, AnnotationsReaderWriterInterceptor.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(GZIPReaderTestInterceptor.class)
                .register(GZIPWriterTestInterceptor.class)
                .register(PlusOneWriterInterceptor.class)
                .register(MinusOneReaderInterceptor.class)
                .register(AnnotationsReaderWriterInterceptor.class);
    }

    @Test
    public void testMoreInterceptorsAndFilter() throws IOException {
        WebTarget target = target().path("test");

        Response response = target.request().put(Entity.entity(ENTITY, MediaType.TEXT_PLAIN_TYPE));
        String str = response.readEntity(String.class);
        assertEquals(ENTITY + FROM_RESOURCE, str);
    }

    @Path("test")
    public static class TestResource {

        @PUT
        @Produces("text/plain")
        @Consumes("text/plain")
        public String put(String str) {
            System.out.println("resource: " + str);
            return str + FROM_RESOURCE;
        }
    }

    /**
     * Interceptor which adds +1 to each byte written into the stream.
     */
    @Provider
    @Priority(300)
    public static class PlusOneWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final OutputStream old = context.getOutputStream();
            context.setOutputStream(new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    if (b == 255) {
                        old.write(0);
                    } else {
                        old.write(b + 1);
                    }
                }
            });
            context.proceed();
        }
    }

    /**
     * Interceptor which adds +1 to each byte read from the stream.
     */
    @Provider
    @Priority(300)
    public static class MinusOneReaderInterceptor implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final InputStream old = context.getInputStream();
            context.setInputStream(new InputStream() {

                @Override
                public int read() throws IOException {
                    int b = old.read();
                    if (b == -1) {
                        return -1;
                    } else if (b == 0) {
                        return 255;
                    } else {
                        return b - 1;
                    }
                }
            });

            return context.proceed();
        }
    }

    /**
     * Interceptor that tries to set annotations of {@link InterceptorContext} to {@code null},
     * empty array and finally to the original value.
     */
    @Provider
    @Priority(300)
    public static class AnnotationsReaderWriterInterceptor implements ReaderInterceptor, WriterInterceptor {

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final Annotation[] annotations = context.getAnnotations();

            // Fails if no NPE is thrown.
            unsetAnnotations(context);
            // Ok.
            setAnnotations(context, new Annotation[0]);
            // Ok.
            setAnnotations(context, annotations);

            return context.proceed();
        }

        @Override
        public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
            final Annotation[] annotations = context.getAnnotations();

            // Fails if no NPE is thrown.
            unsetAnnotations(context);
            // Ok.
            setAnnotations(context, new Annotation[0]);
            // Ok.
            setAnnotations(context, annotations);

            context.proceed();
        }

        private void setAnnotations(final InterceptorContext context, final Annotation[] annotations) {
            if (context.getAnnotations() != null) {
                context.setAnnotations(annotations);
            }
        }

        private void unsetAnnotations(final InterceptorContext context) {
            try {
                context.setAnnotations(null);

                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // OK.
            }
        }
    }

    @Provider
    @Priority(100)
    public static class IOExceptionReaderInterceptor implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            throw new IOException("client io");
        }
    }

    @Test
    public void testIOException() throws IOException {
        client().register(IOExceptionReaderInterceptor.class);

        WebTarget target = target().path("test");

        Response response = target.request().put(Entity.entity(ENTITY, MediaType.TEXT_PLAIN_TYPE));

        try {
            response.readEntity(String.class);
            fail("ProcessingException expected.");
        } catch (ProcessingException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

}
