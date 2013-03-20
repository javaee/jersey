/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * OutboundJaxrsResponse unit tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class OutboundJaxrsResponseTest {

    private static class TestInputStream extends ByteArrayInputStream {

        private boolean isRead;
        private boolean isClosed;

        private TestInputStream() {
            super("test".getBytes());
        }

        @Override
        public synchronized int read() {
            final int read = super.read();
            isRead = read == -1;
            return read;
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            final int read = super.read(b, off, len);
            isRead = read == -1;
            return read;
        }

        @Override
        public int read(byte[] b) throws IOException {
            final int read = super.read(b);
            isRead = read == -1;
            return read;
        }

        @Override
        public void close() throws IOException {
            isClosed = true;
            super.close();
        }
    }

    private static final OutboundMessageContext.StreamProvider TEST_PROVIDER
            = new OutboundMessageContext.StreamProvider() {
        @Override
        public OutputStream getOutputStream(int contentLength) throws IOException {
            return new ByteArrayOutputStream();
        }
    };

    private OutboundJaxrsResponse.ResponseBuilder rb;

    /**
     * Create test class.
     */
    public OutboundJaxrsResponseTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Before
    public void setUp() {
        rb = new OutboundJaxrsResponse.Builder(new OutboundMessageContext()).status(Response.Status.OK);
    }


    /**
     * Test of empty entity buffering.
     */
    @Test
    public void testBufferEmptyEntity() {
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.build());
        r.getContext().setStreamProvider(TEST_PROVIDER);

        assertFalse("Buffer entity should return 'false' if no entity.", r.bufferEntity());
    }

    /**
     * Test of non-stream entity buffering.
     */
    @Test
    public void testBufferNonStreamEntity() {
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.entity(new Object()).build());
        r.getContext().setStreamProvider(TEST_PROVIDER);

        assertFalse("Buffer entity should return 'false' for non-stream entity.", r.bufferEntity());
    }

    /**
     * Test of stream entity buffering.
     */
    @Test
    public void testBufferStreamEntity() {
        TestInputStream tis = new TestInputStream();
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.entity(tis).build());
        r.getContext().setStreamProvider(TEST_PROVIDER);

        assertTrue("Buffer entity should return 'true' for stream entity.", r.bufferEntity());
        assertTrue("Second call to buffer entity should return 'true' for stream entity.", r.bufferEntity()); // second call
        assertTrue("Buffered stream has not been fully read.", tis.isRead);
        assertTrue("Buffered stream has not been closed after buffering.", tis.isClosed);
    }

    /**
     * Test of closing response with empty entity.
     */
    @Test
    public void testCloseEmptyEntity() {
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.build());
        r.getContext().setStreamProvider(TEST_PROVIDER);

        r.close();
        try {
            r.bufferEntity();
            fail("IllegalStateException expected when buffering entity on closed response.");
        } catch (IllegalStateException ex) {
            // ok
        }
        r.close(); // second call should pass
    }

    /**
     * Test of closing response with non-stream entity.
     */
    @Test
    public void testCloseNonStreamEntity() {
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.entity(new Object()).build());
        r.getContext().setStreamProvider(TEST_PROVIDER);

        r.close();
        try {
            r.bufferEntity();
            fail("IllegalStateException expected when buffering entity on closed response.");
        } catch (IllegalStateException ex) {
            // ok
        }
        r.close(); // second call should pass
    }

    /**
     * Test of closing response with stream entity.
     */
    @Test
    public void testCloseStreamEntity() {
        TestInputStream tis = new TestInputStream();
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.entity(tis).build());
        r.getContext().setStreamProvider(TEST_PROVIDER);

        r.close();
        try {
            r.bufferEntity();
            fail("IllegalStateException expected when buffering entity on closed response.");
        } catch (IllegalStateException ex) {
            // ok
        }
        r.close(); // second call should pass

        assertFalse("Unbuffered closed response stream entity should not be read.", tis.isRead);
        assertTrue("Closed response stream entity should have been closed.", tis.isClosed);
    }

    /**
     * Test of closing response with empty entity.
     */
    @Test
    public void testCloseEmptyEntityNoStreamProvider() {
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.build());
        r.close();
        try {
            r.bufferEntity();
            fail("IllegalStateException expected when buffering entity on closed response.");
        } catch (IllegalStateException ex) {
            // ok
        }
        r.close(); // second call should pass
    }

    /**
     * Test of closing response with non-stream entity.
     */
    @Test
    public void testCloseNonStreamEntityNoStreamProvider() {
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.entity(new Object()).build());
        r.close();
        try {
            r.bufferEntity();
            fail("IllegalStateException expected when buffering entity on closed response.");
        } catch (IllegalStateException ex) {
            // ok
        }
        r.close(); // second call should pass
    }

    /**
     * Test of closing response with stream entity.
     */
    @Test
    public void testCloseStreamEntityNoStreamProvider() {
        TestInputStream tis = new TestInputStream();
        final OutboundJaxrsResponse r = OutboundJaxrsResponse.from(rb.entity(tis).build());
        r.close();
        try {
            r.bufferEntity();
            fail("IllegalStateException expected when buffering entity on closed response.");
        } catch (IllegalStateException ex) {
            // ok
        }
        r.close(); // second call should pass

        assertFalse("Unbuffered closed response stream entity should not be read.", tis.isRead);
        assertTrue("Closed response stream entity should have been closed.", tis.isClosed);
    }

}
