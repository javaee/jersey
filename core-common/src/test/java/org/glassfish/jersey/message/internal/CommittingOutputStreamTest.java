/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.model.internal.CommonConfig;
import org.glassfish.jersey.model.internal.ComponentBag;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Test the {@link CommittingOutputStream}.
 *
 * @author Miroslav Fuksa
 */
public class CommittingOutputStreamTest {

    private static class Passed {
        boolean b;

        public void pass() {
            b = true;
        }
    }

    @Test
    public void testExactSizeOfBuffer() throws IOException {
        final Passed passed = new Passed();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        CommittingOutputStream cos = new CommittingOutputStream();
        setupBufferedStreamProvider(passed, baos, cos, 3);

        cos.write((byte) 1);
        cos.write((byte) 2);
        cos.write((byte) 3);
        checkNotYetCommitted(passed, baos, cos);
        cos.commit();
        check(baos, new byte[]{1, 2, 3});
        assertTrue(passed.b);
        cos.close();
    }

    private void checkNotYetCommitted(Passed passed, ByteArrayOutputStream baos, CommittingOutputStream cos) {
        assertFalse(passed.b);
        assertFalse(cos.isCommitted());
        check(baos, null);
    }

    private void checkCommitted(Passed passed, CommittingOutputStream cos) {
        assertTrue(passed.b);
        assertTrue(cos.isCommitted());
    }

    private void setupBufferedStreamProvider(final Passed passed, final ByteArrayOutputStream baos, CommittingOutputStream cos,
                                             final int expectedContentLength) {
        cos.setStreamProvider(new OutboundMessageContext.StreamProvider() {
            @Override
            public OutputStream getOutputStream(int contentLength) throws IOException {
                assertEquals(expectedContentLength, contentLength);
                passed.pass();
                return baos;
            }
        });
        cos.enableBuffering(3);
    }

    private void setupStreamProvider(final Passed passed, final ByteArrayOutputStream baos, CommittingOutputStream cos) {
        cos.setStreamProvider(new OutboundMessageContext.StreamProvider() {

            @Override
            public OutputStream getOutputStream(int contentLength) throws IOException {
                passed.pass();
                return baos;
            }
        });

    }

    @Test
    public void testExactSizeOfBufferByClose() throws IOException {
        final Passed passed = new Passed();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        CommittingOutputStream cos = new CommittingOutputStream();
        setupBufferedStreamProvider(passed, baos, cos, 3);

        cos.write((byte) 1);
        cos.write((byte) 2);
        cos.write((byte) 3);
        checkNotYetCommitted(passed, baos, cos);
        cos.close();
        check(baos, new byte[]{1, 2, 3});
        assertTrue(passed.b);
    }

    @Test
    public void testLessBytesThanLimit() throws IOException {
        final Passed passed = new Passed();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        CommittingOutputStream cos = new CommittingOutputStream();
        setupBufferedStreamProvider(passed, baos, cos, 2);

        cos.write((byte) 1);
        cos.write((byte) 2);
        checkNotYetCommitted(passed, baos, cos);

        cos.commit();

        check(baos, new byte[]{1, 2});
        cos.close();
        assertTrue(passed.b);
    }

    @Test
    public void testNoBytes() throws IOException {
        final Passed passed = new Passed();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        CommittingOutputStream cos = new CommittingOutputStream();
        setupBufferedStreamProvider(passed, baos, cos, 0);

        checkNotYetCommitted(passed, baos, cos);

        cos.commit();

        check(baos, null);
        cos.close();
        assertTrue(passed.b);
    }

    @Test
    public void testBufferOverflow() throws IOException {
        final Passed passed = new Passed();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        CommittingOutputStream cos = new CommittingOutputStream();
        setupBufferedStreamProvider(passed, baos, cos, -1);

        cos.write((byte) 1);
        cos.write((byte) 2);
        cos.write((byte) 3);
        checkNotYetCommitted(passed, baos, cos);
        cos.write((byte) 4);
        check(baos, new byte[]{1, 2, 3, 4});
        cos.write((byte) 5);
        check(baos, new byte[]{1, 2, 3, 4, 5});

        cos.commit();

        check(baos, new byte[]{1, 2, 3, 4, 5});
        cos.close();
    }

    @Test
    public void testNotBufferedOS() throws IOException {
        final Passed passed = new Passed();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        CommittingOutputStream cos = new CommittingOutputStream();
        setupStreamProvider(passed, baos, cos);

        checkNotYetCommitted(passed, baos, cos);
        cos.write((byte) 1);
        checkCommitted(passed, cos);
        check(baos, new byte[]{1});
        cos.write((byte) 2);
        checkCommitted(passed, cos);
        cos.write((byte) 3);
        cos.write((byte) 4);
        check(baos, new byte[]{1, 2, 3, 4});
        cos.write((byte) 5);
        check(baos, new byte[]{1, 2, 3, 4, 5});

        cos.commit();
        checkCommitted(passed, cos);
        check(baos, new byte[]{1, 2, 3, 4, 5});
        cos.close();
    }

    private void check(ByteArrayOutputStream baos, byte... bytes) {
        assertEquals(bytes == null ? 0 : bytes.length, baos.size());

        if (bytes != null) {
            final byte[] actualBytes = baos.toByteArray();
            for (int i = 0; i < bytes.length; i++) {
                assertEquals(bytes[i], actualBytes[i]);
            }
        }
    }

    @Test
    public void testPropertiesWithMessageContext() throws IOException {
        final int size = 20;
        Map<String, Object> properties = Maps.newHashMap();
        properties.put(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, size);
        final RuntimeType runtime = RuntimeType.CLIENT;

        checkBufferSize(size, properties, runtime);
    }

    @Test
    public void testPropertiesWithMessageContextVeryBigBuffer() throws IOException {
        final int size = 200000;
        Map<String, Object> properties = Maps.newHashMap();
        properties.put(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, size);
        final RuntimeType runtime = RuntimeType.CLIENT;

        checkBufferSize(size, properties, runtime);
    }

    @Test
    public void testPropertiesWithMessageContextMissingServerSpecific() throws IOException {
        final int size = 22;
        Map<String, Object> properties = Maps.newHashMap();
        properties.put(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, size);
        properties.put(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER + ".client", size * 2);
        checkBufferSize(size, properties, RuntimeType.SERVER);
    }

    @Test
    public void testPropertiesWithMessageContextMissingServerAtAll() throws IOException {
        final int size = 22;
        Map<String, Object> properties = Maps.newHashMap();
        properties.put(PropertiesHelper.getPropertyNameForRuntime(
                        CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, RuntimeType.CLIENT), size);
        checkBufferSize(CommittingOutputStream.DEFAULT_BUFFER_SIZE, properties, RuntimeType.SERVER);
        checkBufferSize(size, properties, RuntimeType.CLIENT);
    }

    @Test
    public void testPropertiesWithMessageContextClientOverrides() throws IOException {
        final int size = 22;
        Map<String, Object> properties = Maps.newHashMap();
        properties.put(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, size);
        properties.put(PropertiesHelper.getPropertyNameForRuntime(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER,
                RuntimeType.CLIENT), size * 2);

        checkBufferSize(size * 2, properties, RuntimeType.CLIENT);
        checkBufferSize(size, properties, RuntimeType.SERVER);
    }

    @Test
    public void testPropertiesWithMessageContextDefaultNoProps() throws IOException {
        Map<String, Object> properties = Maps.newHashMap();
        final RuntimeType runtime = RuntimeType.CLIENT;

        checkBufferSize(CommittingOutputStream.DEFAULT_BUFFER_SIZE, properties, runtime);
    }

    private void checkBufferSize(int expectedSize, Map<String, Object> properties, RuntimeType runtime) throws IOException {
        OutboundMessageContext outboundMessageContext = new OutboundMessageContext();
        final Passed passed = new Passed();
        outboundMessageContext.setStreamProvider(new OutboundMessageContext.StreamProvider() {
            @Override
            public OutputStream getOutputStream(int contentLength) throws IOException {

                assertEquals(-1, contentLength);
                passed.pass();
                return null;
            }
        });
        CommonConfig configuration = new CommonConfig(runtime, ComponentBag.INCLUDE_ALL);
        configuration.setProperties(properties);
        outboundMessageContext.enableBuffering(configuration);

        final OutputStream entityStream = outboundMessageContext.getEntityStream();
        for (int i = 1; (i < 1000000) && (!passed.b); i++) {
            entityStream.write((byte) 65);
            if (i >= expectedSize + 1) {
                break;
            } else {
                assertFalse("committed already with byte #" + i, passed.b);
            }
        }

        assertTrue(passed.b);
    }


    @Test
    public void testEnableBuffering() {
        CommittingOutputStream cos = new CommittingOutputStream();
        cos.enableBuffering(500);
    }

    @Test
    public void testEnableBufferingIllegalStateException() throws IOException {
        CommittingOutputStream cos = new CommittingOutputStream();
        cos.setStreamProvider(new OutboundMessageContext.StreamProvider() {
            @Override
            public OutputStream getOutputStream(int contentLength) throws IOException {
                return null;
            }
        });
        cos.write('a');
        try {
            cos.enableBuffering(500);
            fail("should throw IllegalStateException because of late setup of enableBuffering when bytes are already written.");
        } catch (IllegalStateException e) {
            System.out.println("this is ok - exception should be thrown: " + e.getMessage());
            // ok - should be thrown (bytes are already written).
        }
    }

    @Test
    public void testSetStramProviderIllegalStateException1() throws IOException {
        CommittingOutputStream cos = new CommittingOutputStream();
        cos.enableBuffering(1);
        writeAndCheckIllegalState(cos);
    }

    @Test
    public void testSetStramProviderIllegalStateException2() throws IOException {
        CommittingOutputStream cos = new CommittingOutputStream();
        writeAndCheckIllegalState(cos);
    }

    private void writeAndCheckIllegalState(CommittingOutputStream cos) throws IOException {
        try {
            cos.write('a');
            cos.write('a');
            cos.write('a');
            cos.write('a');
            cos.write('a');
            fail("should throw IllegalStateException because of missing stream provider (bytes are already written).");
        } catch (IllegalStateException e) {
            System.out.println("this is ok - exception should be thrown: " + e.getMessage());
            // ok - should be thrown (bytes are already written).
        }
    }

}



