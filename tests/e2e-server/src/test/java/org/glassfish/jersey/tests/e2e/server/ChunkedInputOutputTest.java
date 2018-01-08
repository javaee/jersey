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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.server.ChunkedOutput;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Chunked input/output tests.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ChunkedInputOutputTest extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(ChunkedInputOutputTest.class.getName());

    /**
     * Test resource.
     */
    @Path("/test")
    public static class TestResource {
        /**
         * Get chunk stream.
         *
         * @return chunk stream.
         */
        @GET
        public ChunkedOutput<String> get() {
            final ChunkedOutput<String> output = new ChunkedOutput<>(String.class, "\r\n");

            new Thread() {
                @Override
                public void run() {
                    try {
                        output.write("test");
                        output.write("test");
                        output.write("test");
                    } catch (final IOException e) {
                        LOGGER.log(Level.SEVERE, "Error writing chunk.", e);
                    } finally {
                        try {
                            output.close();
                        } catch (final IOException e) {
                            LOGGER.log(Level.INFO, "Error closing chunked output.", e);
                        }
                    }
                }
            }.start();

            return output;
        }

        /**
         * Get chunk stream with an attached interceptor.
         *
         * @return intercepted chunk stream.
         */
        @GET
        @Path("intercepted")
        @Intercepted
        public ChunkedOutput<String> interceptedGet() {
            return get();
        }
    }

    /**
     * Test interceptor binding.
     */
    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Intercepted {

    }

    /**
     * Test interceptor - counts number of interception as well as number of wrapper output stream method calls.
     */
    @Intercepted
    public static class TestWriterInterceptor implements WriterInterceptor {

        private static final AtomicInteger interceptCounter = new AtomicInteger(0);
        private static final AtomicInteger writeCounter = new AtomicInteger(0);
        private static final AtomicInteger flushCounter = new AtomicInteger(0);
        private static final AtomicInteger closeCounter = new AtomicInteger(0);

        @Override
        public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
            interceptCounter.incrementAndGet();
            final OutputStream out = context.getOutputStream();
            context.setOutputStream(new OutputStream() {
                @Override
                public void write(final int b) throws IOException {
                    writeCounter.incrementAndGet();
                    out.write(b);
                }

                @Override
                public void write(final byte[] b) throws IOException {
                    writeCounter.incrementAndGet();
                    out.write(b);
                }

                @Override
                public void write(final byte[] b, final int off, final int len) throws IOException {
                    writeCounter.incrementAndGet();
                    out.write(b, off, len);
                }

                @Override
                public void flush() throws IOException {
                    flushCounter.incrementAndGet();
                    out.flush();
                }

                @Override
                public void close() throws IOException {
                    closeCounter.incrementAndGet();
                    out.close();
                }
            });
            context.proceed();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class, TestWriterInterceptor.class);
    }

    /**
     * Test retrieving chunked response stream as a single response string.
     *
     * @throws Exception in case of a failure during the test execution.
     */
    @Test
    public void testChunkedOutputToSingleString() throws Exception {
        final String response = target().path("test").request().get(String.class);

        assertEquals("Unexpected value of chunked response unmarshalled as a single string.",
                "test\r\ntest\r\ntest\r\n", response);
    }

    /**
     * Test retrieving chunked response stream sequentially as individual chunks using chunked input.
     *
     * @throws Exception in case of a failure during the test execution.
     */
    @Test
    public void testChunkedOutputToChunkInput() throws Exception {
        final ChunkedInput<String> input = target().path("test").request().get(new GenericType<ChunkedInput<String>>() {
        });

        int counter = 0;
        String chunk;
        while ((chunk = input.read()) != null) {
            assertEquals("Unexpected value of chunk " + counter, "test", chunk);
            counter++;
        }

        assertEquals("Unexpected numbed of received chunks.", 3, counter);
    }

    /**
     * Test retrieving intercepted chunked response stream sequentially as individual chunks using chunked input.
     *
     * @throws Exception in case of a failure during the test execution.
     */
    @Test
    public void testInterceptedChunkedOutputToChunkInput() throws Exception {
        final ChunkedInput<String> input = target().path("test/intercepted")
                .request().get(new GenericType<ChunkedInput<String>>() {
                });

        int counter = 0;
        String chunk;
        while ((chunk = input.read()) != null) {
            assertEquals("Unexpected value of chunk " + counter, "test", chunk);
            counter++;
        }

        assertThat("Unexpected numbed of received chunks.",
                counter, equalTo(3));

        assertThat("Unexpected number of chunked output interceptions.",
                TestWriterInterceptor.interceptCounter.get(), equalTo(1));
        assertThat("Unexpected number of intercepted output write calls.",
                TestWriterInterceptor.writeCounter.get(), greaterThanOrEqualTo(1));
        assertThat("Unexpected number of intercepted output flush calls.",
                TestWriterInterceptor.flushCounter.get(), greaterThanOrEqualTo(3));
        assertThat("Unexpected number of intercepted output close calls.",
                TestWriterInterceptor.closeCounter.get(), equalTo(1));
    }
}
