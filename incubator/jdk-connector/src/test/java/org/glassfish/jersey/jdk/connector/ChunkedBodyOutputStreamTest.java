/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.fail;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class ChunkedBodyOutputStreamTest {

    @Test
    public void testBasic() throws IOException {
        AsynchronousBodyInputStream responseBody = new AsynchronousBodyInputStream();
        ChunkedBodyOutputStream chunkedStream = getOutputStream(responseBody, 21);

        String sentBody = TestUtils.generateBody(500);
        byte[] sentBytes = sentBody.getBytes();
        for (byte b : sentBytes) {
            chunkedStream.write(b);
        }

        chunkedStream.close();
        verifyReceivedMessage(sentBody, responseBody);
    }

    @Test
    public void testChunkSize() throws IOException {
        doTestChunkSize(1);
    }

    @Test
    public void testChunkSizeWithArray() throws IOException {
        doTestChunkSize(8);
    }

    private void doTestChunkSize(int batchSize) throws IOException {
        final int chunkSize = 21;
        AsynchronousBodyInputStream responseBody = new AsynchronousBodyInputStream() {

            private boolean receivedLess = false;

            @Override
            synchronized void notifyDataAvailable(ByteBuffer availableData) {
                if (availableData.remaining() > chunkSize) {
                    fail();
                }

                if (availableData.remaining() < chunkSize) {
                    assertFalse(receivedLess);
                    receivedLess = true;
                }

                super.notifyDataAvailable(availableData);
            }
        };

        ChunkedBodyOutputStream chunkedStream = getOutputStream(responseBody, chunkSize);

        String sentBody = TestUtils.generateBody(100);
        byte[] sentBytes = sentBody.getBytes();
        if (batchSize > 1) {
            for (int i = 0; i < sentBytes.length; i += 8) {
                chunkedStream.write(sentBytes, i, Math.min(sentBytes.length - i, 8));
            }
        } else {
            for (byte b : sentBytes) {
                chunkedStream.write(b);
            }
        }

        chunkedStream.close();
        verifyReceivedMessage(sentBody, responseBody);
    }

    private ChunkedBodyOutputStream getOutputStream(AsynchronousBodyInputStream responseBody, int chunkSize) {
        ChunkedBodyOutputStream chunkedStream = new ChunkedBodyOutputStream(chunkSize);
        Filter<ByteBuffer, ?, ?, ?> mockTransportFilter = createMockTransportFilter(responseBody);
        chunkedStream.open(mockTransportFilter);
        return chunkedStream;
    }

    private void verifyReceivedMessage(String sentBody, AsynchronousBodyInputStream responseBody) throws IOException {
        byte[] sentBytes = sentBody.getBytes();
        byte[] receivedBytes = new byte[sentBytes.length];

        for (int i = 0; i < sentBytes.length; i++) {
            int b = responseBody.tryRead();
            if (b == -1) {
                fail();
            }

            receivedBytes[i] = (byte) b;
        }

        if (responseBody.tryRead() != -1) {
            fail();
        }

        String receivedBody = new String(receivedBytes);
        assertEquals(sentBody, receivedBody);
    }

    Filter<ByteBuffer, ?, ?, ?> createMockTransportFilter(final AsynchronousBodyInputStream responseBody) {
        HttpParser parser = new HttpParser(Integer.MAX_VALUE, Integer.MAX_VALUE);
        parser.reset(true);
        final TransferEncodingParser transferEncodingParser = TransferEncodingParser
                .createChunkParser(responseBody, parser, 1000);
        return new Filter<ByteBuffer, Void, Void, Void>(null) {

            @Override
            public void write(ByteBuffer chunk, CompletionHandler<ByteBuffer> completionHandler) {
                try {
                    if (transferEncodingParser.parse(chunk)) {
                        responseBody.notifyAllDataRead();
                    }

                    completionHandler.completed(chunk);
                } catch (ParseException e) {
                    completionHandler.failed(e);
                }
            }
        };
    }
}
