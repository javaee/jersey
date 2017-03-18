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
package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Reproducer for JERSEY-2705. Client side entity InputStream exception
 * in chunked mode should not lead to the same behavior on the server side,
 * as if no exception occurred at all.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ChunkedInputStreamClosedPrematurelyTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(ChunkedInputStreamClosedPrematurelyTest.class.getName());
    private static final Exception NO_EXCEPTION = new Exception("No exception.");

    private static final AtomicInteger NEXT_REQ_ID = new AtomicInteger(0);
    private static final String REQ_ID_PARAM_NAME = "test-req-id";
    private static final int BYTES_TO_SEND = 1024 * 1024 + 13;

    @Path("/test")
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "JavaDoc"})
    public static class TestResource {

        private static final ConcurrentMap<String, SettableFuture<Exception>> REQUEST_MAP = new ConcurrentHashMap<>();

        @QueryParam(REQ_ID_PARAM_NAME)
        private String reqId;

        @POST
        public String post(InputStream is) {
            final byte[] buffer = new byte[4096];
            int readTotal = 0;

            Exception thrown = NO_EXCEPTION;
            try {
                int read;
                while ((read = is.read(buffer)) > -1) {
                    readTotal += read;
                }
            } catch (Exception ex) {
                thrown = ex;
            }

            if (!getFutureFor(reqId).set(thrown)) {
                LOGGER.log(Level.WARNING,
                        "Unable to set stream processing exception into the settable future instance for request id " + reqId,
                        thrown);
            }

            return Integer.toString(readTotal);
        }

        @Path("/requestWasMade")
        @GET
        public Boolean getRequestWasMade() {
            // add a new future for the request if not there yet to avoid race conditions with POST processing
            final SettableFuture<Exception> esf = getFutureFor(reqId);
            try {
                // wait for up to three second for a request to be made;
                // there is always a value, if set...
                return esf.get(3, TimeUnit.SECONDS) != null;
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new InternalServerErrorException("Post request processing has timed out for request id " + reqId, e);
            }
        }

        @Path("/requestCausedException")
        @GET
        public Boolean getRequestCausedException() {
            final SettableFuture<Exception> esf = getFutureFor(reqId);
            try {
                return esf.get(3, TimeUnit.SECONDS) != NO_EXCEPTION;
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new InternalServerErrorException("Post request processing has timed out for request id " + reqId, e);
            }
        }

        private SettableFuture<Exception> getFutureFor(String key) {
            final SettableFuture<Exception> esf = SettableFuture.create();
            final SettableFuture<Exception> oldEsf = REQUEST_MAP.putIfAbsent(key, esf);
            return (oldEsf != null) ? oldEsf : esf;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 7);
    }

    /**
     * A sanity test to check the normal use case is working as expected.
     */
    @Test
    public void testUninterrupted() {
        final String testReqId = nextRequestId("testUninterrupted");

        Response testResponse = target("test").queryParam(REQ_ID_PARAM_NAME, testReqId)
                .request().post(Entity.entity("0123456789ABCDEF", MediaType.APPLICATION_OCTET_STREAM));
        assertEquals("Unexpected response status code.", 200, testResponse.getStatus());
        assertEquals("Unexpected response entity.", "16", testResponse.readEntity(String.class));

        assertTrue("POST request " + testReqId + " has not reached the server.",
                target("test").path("requestWasMade").queryParam(REQ_ID_PARAM_NAME, testReqId)
                        .request().get(Boolean.class));
        assertFalse("POST request " + testReqId + " has caused an unexpected exception on the server.",
                target("test").path("requestCausedException").queryParam(REQ_ID_PARAM_NAME, testReqId)
                        .request().get(Boolean.class));
    }

    /**
     * This test simulates how Jersey Client should behave after JERSEY-2705 gets fixed.
     *
     * @throws Exception in case the test fails to execute.
     */
    @Test
    public void testInterruptedJerseyHttpUrlConnection() throws Exception {

        final String testReqId = nextRequestId("testInterruptedJerseyHttpUrlConnection");

        URL postUrl = UriBuilder.fromUri(getBaseUri()).path("test").queryParam(REQ_ID_PARAM_NAME, testReqId).build().toURL();
        final HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(1024);
            OutputStream entityStream = connection.getOutputStream();
            ReaderWriter.writeTo(new ExceptionThrowingInputStream(BYTES_TO_SEND), entityStream);
            Assert.fail("Expected ProcessingException has not been thrown.");
        } catch (IOException expected) {
            // so far so good
        } finally {
            connection.disconnect();
        }
        // we should make it to the server, but there the exceptional behaviour should get noticed
        assertTrue("POST request " + testReqId + " has not reached the server.",
                target("test").path("requestWasMade").queryParam(REQ_ID_PARAM_NAME, testReqId).request().get(Boolean.class));
        assertTrue("POST request " + testReqId + " did not cause an expected exception on the server.",
                target("test").path("requestCausedException").queryParam(REQ_ID_PARAM_NAME, testReqId)
                        .request().get(Boolean.class));
    }

    /**
     * This test reproduces the Jersey Client behavior reported in JERSEY-2705.
     */
    @Ignore
    @Test
    public void testInterruptedJerseyClient() {
        final String testReqId = nextRequestId("testInterruptedJerseyClient");

        try {
            target("test").queryParam(REQ_ID_PARAM_NAME, testReqId).request()
                    .post(Entity.entity(new ExceptionThrowingInputStream(BYTES_TO_SEND), MediaType.APPLICATION_OCTET_STREAM));
            Assert.fail("Expected ProcessingException has not been thrown.");
        } catch (ProcessingException expected) {
            // so far so good
        }
        // we should make it to the server, but there the exceptional behaviour should get noticed
        assertTrue("POST request " + testReqId + " has not reached the server.",
                target("test").path("requestWasMade").queryParam(REQ_ID_PARAM_NAME, testReqId).request().get(Boolean.class));
        assertTrue("POST request " + testReqId + " did not cause an expected exception on the server.",
                target("test").path("requestCausedException").queryParam(REQ_ID_PARAM_NAME, testReqId)
                        .request().get(Boolean.class));
    }

    private static String nextRequestId(String testMethodName) {
        return String.format(testMethodName + "-%03d", NEXT_REQ_ID.getAndIncrement());
    }

    /**
     * InputStream implementation that allows "reading" as many bytes as specified by threshold constructor parameter.
     * Throws an IOException if read operation is attempted after the threshold is exceeded.
     */
    private class ExceptionThrowingInputStream extends InputStream {

        private final int threshold;
        private int offset = 0;

        /**
         * Get me a new stream that throws exception.
         *
         * @param threshold this number of bytes will be read all right
         */
        public ExceptionThrowingInputStream(int threshold) {
            this.threshold = threshold;
        }

        @Override
        public int read() throws IOException {
            if (offset++ < threshold) {
                return 'A';
            } else {
                throw new IOException("stream closed");
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            offset += len;
            if (offset < threshold) {
                Arrays.fill(b, off, off + len, (byte) 'A');
                return len;
            } else {
                throw new IOException("Stream closed");
            }
        }
    }
}
