/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritePendingException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static junit.framework.Assert.assertNotNull;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class AsynchronousBodyOutputStreamTest {

    @Test
    public void testBasicAsyncWrite() throws IOException {
        doTestAsyncWrite(false);
    }

    @Test
    public void testBasicAsyncArrayWrite() throws IOException {
        doTestAsyncWrite(true);
    }

    @Test
    public void testSetListenerAfterOpeningStream() throws IOException {
        TestStream stream = new TestStream(6);
        MockTransportFilter transportFilter = new MockTransportFilter();
        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";
        String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";

        TestWriteListener writeListener = new TestWriteListener(stream, -1);
        writeListener.write(msg1);
        stream.open(transportFilter);
        stream.setWriteListener(writeListener);
        writeListener.write(msg2);
        writeListener.write(msg3);
        stream.close();

        if (writeListener.getError() != null) {
            writeListener.getError().printStackTrace();
            fail();
        }
        assertEquals(msg1 + msg2 + msg3, transportFilter.getWrittenData());
    }

    @Test
    public void testTestAsyncWriteWithDelay() throws IOException {
        doTestAsyncWriteWithDelay(false);
    }

    @Test
    public void testTestAsyncWriteArrayWithDelay() throws IOException {
        doTestAsyncWriteWithDelay(true);
    }

    @Test
    public void testAsyncFlush() {
        TestStream stream = new TestStream(6);
        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";
        String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        String msg4 = "DDDDDDD";
        TestWriteListener writeListener = new TestWriteListener(stream, -1);
        stream.setWriteListener(writeListener);

        MockTransportFilter transportFilter = new MockTransportFilter();
        writeListener.write(msg1);
        transportFilter.block();
        stream.open(transportFilter);
        writeListener.flush();
        // test someone going crazy with flush
        writeListener.flush();
        transportFilter.unblock();
        transportFilter.block();
        writeListener.write(msg2);
        transportFilter.unblock();
        writeListener.flush();
        transportFilter.block();
        writeListener.write(msg3);
        writeListener.flush();
        writeListener.write(msg4);

        writeListener.flush();
        writeListener.close();
        transportFilter.unblock();

        if (writeListener.getError() != null) {
            writeListener.getError().printStackTrace();
            fail();
        }

        assertEquals(msg1 + msg2 + msg3 + msg4, transportFilter.getWrittenData());
    }

    @Test
    public void testAsyncException() {
        TestStream stream = new TestStream(6);
        String msg1 = "AAAAAAAAAAAAAAAAAAAA";

        TestWriteListener writeListener = new TestWriteListener(stream, -1);
        stream.setWriteListener(writeListener);

        MockTransportFilter transportFilter = new MockTransportFilter();
        stream.open(transportFilter);
        Throwable t = new Throwable();
        transportFilter.setException(t);
        writeListener.write(msg1);

        assertNotNull(writeListener.getError());
        assertTrue(t == writeListener.getError());
    }

    @Test
    public void testBasicSyncWrite() throws IOException, InterruptedException, TimeoutException, ExecutionException {
        doTestSyncWrite(false);
    }

    @Test
    public void testBasicSyncArrayWrite() throws IOException, InterruptedException, TimeoutException, ExecutionException {
        doTestSyncWrite(true);
    }

    @Test
    public void testSyncWriteWithDelay() throws IOException, InterruptedException, TimeoutException, ExecutionException {
        doTestSyncWriteWithDelay(false);
    }

    @Test
    public void testSyncArrayWriteWithDelay() throws IOException, InterruptedException, TimeoutException, ExecutionException {
        doTestSyncWriteWithDelay(true);
    }

    @Test
    public void testAsyncWriteWhenNotReady() throws IOException {
        TestStream stream = new TestStream(6);
        TestWriteListener writeListener = new TestWriteListener(stream, -1);
        stream.setWriteListener(writeListener);

        try {
            stream.write((byte) 'a');
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testUnsupportedSync() {
        final TestStream stream = new TestStream(10);
        stream.open(new MockTransportFilter());
        try {
            // touch this stream to make it synchronous
            stream.write((byte) 'a');
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        assertUnsupported(() -> {
            stream.isReady();
            return null;
        });

        assertUnsupported(() -> {
            stream.setWriteListener(new TestWriteListener(stream));
            return null;
        });
    }

    @Test
    public void testSyncException() throws IOException {
        TestStream stream = new TestStream(1);

        MockTransportFilter transportFilter = new MockTransportFilter();
        stream.open(transportFilter);
        Throwable t = new Throwable();
        transportFilter.setException(t);
        try {
            stream.write("aaa".getBytes());
            fail();
        } catch (IOException e) {
            assertTrue(t == e.getCause());
        }
    }

    private void doTestSyncWrite(final boolean useArray)
            throws IOException, InterruptedException, TimeoutException, ExecutionException {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            final TestStream stream = new TestStream(6);
            final String msg1 = "AAAAAAAAAAAAAAAAAAAA";
            String msg2 = "BBBBBBBBBBBBB";
            String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";

            MockTransportFilter transportFilter = new MockTransportFilter();
            Future<Boolean> future = executor.submit(() -> {
                try {
                    writeToStream(stream, msg1, useArray);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            });

            // test that synchronous write really blocks until the stream is opened
            assertFalse(future.isDone());
            stream.open(transportFilter);

            assertTrue(future.get(300, TimeUnit.SECONDS));
            writeToStream(stream, msg2, useArray);
            writeToStream(stream, msg3, useArray);
            stream.close();
            assertEquals(msg1 + msg2 + msg3, transportFilter.getWrittenData());
        } finally {
            executor.shutdownNow();
        }
    }

    private void doTestAsyncWriteWithDelay(boolean useArray) throws IOException {
        int arraySize = -1;
        if (useArray) {
            arraySize = 10;
        }

        TestStream stream = new TestStream(6);
        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";
        String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        String msg4 = "DDDDDDD";
        TestWriteListener writeListener = new TestWriteListener(stream, arraySize);
        stream.setWriteListener(writeListener);

        MockTransportFilter transportFilter = new MockTransportFilter();
        writeListener.write(msg1);
        transportFilter.block();
        stream.open(transportFilter);
        transportFilter.unblock();
        transportFilter.block();
        writeListener.write(msg2);
        transportFilter.unblock();
        transportFilter.block();
        writeListener.write(msg3);
        writeListener.write(msg4);

        writeListener.close();
        transportFilter.unblock();

        if (writeListener.getError() != null) {
            writeListener.getError().printStackTrace();
            fail();
        }

        assertEquals(msg1 + msg2 + msg3 + msg4, transportFilter.getWrittenData());
    }

    private void writeToStream(TestStream stream, String msg, boolean useArray) throws IOException {
        if (useArray) {
            stream.write(msg.getBytes());
        } else {
            byte[] bytes = msg.getBytes();
            for (byte b : bytes) {
                stream.write(b);
            }
        }
    }

    private void doTestSyncWriteWithDelay(final boolean useArray)
            throws IOException, InterruptedException, TimeoutException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            final TestStream stream = new TestStream(6);
            final String msg1 = "AAAAAAAAAAAAAAAAAAAA";
            final String msg2 = "BBBBBBBBBBBBB";
            final String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
            final String msg4 = "DDDDDDD";

            MockTransportFilter transportFilter = new MockTransportFilter();
            stream.open(transportFilter);

            final CountDownLatch blockLatch1 = new CountDownLatch(1);
            transportFilter.block(blockLatch1);

            Future<Boolean> future = executor.submit(() -> {
                try {
                    writeToStream(stream, msg1, useArray);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            });

            assertTrue(blockLatch1.await(5, TimeUnit.SECONDS));
            assertFalse(future.isDone());

            transportFilter.unblock();
            assertTrue(future.get(5, TimeUnit.SECONDS));

            final CountDownLatch blockLatch2 = new CountDownLatch(1);
            transportFilter.block(blockLatch2);

            future = executor.submit(() -> {
                try {
                    writeToStream(stream, msg2, useArray);
                    writeToStream(stream, msg3, useArray);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            });

            assertTrue(blockLatch2.await(5, TimeUnit.SECONDS));
            assertFalse(future.isDone());
            transportFilter.unblock();
            assertTrue(future.get(5, TimeUnit.SECONDS));

            final CountDownLatch blockLatch3 = new CountDownLatch(1);
            transportFilter.block(blockLatch3);

            future = executor.submit(() -> {
                try {
                    writeToStream(stream, msg4, useArray);
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            });

            assertTrue(blockLatch3.await(5, TimeUnit.SECONDS));
            assertFalse(future.isDone());
            transportFilter.unblock();
            assertTrue(future.get(5, TimeUnit.SECONDS));

            assertEquals(msg1 + msg2 + msg3 + msg4, transportFilter.getWrittenData());
        } finally {
            executor.shutdownNow();
        }
    }

    private void doTestAsyncWrite(boolean useArray) throws IOException {
        int arraySize = -1;
        if (useArray) {
            arraySize = 10;
        }

        TestStream stream = new TestStream(6);
        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";
        String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        TestWriteListener writeListener = new TestWriteListener(stream, arraySize);
        stream.setWriteListener(writeListener);

        MockTransportFilter transportFilter = new MockTransportFilter();
        writeListener.write(msg1);
        stream.open(transportFilter);
        writeListener.write(msg2);
        writeListener.write(msg3);
        stream.close();

        if (writeListener.getError() != null) {
            writeListener.getError().printStackTrace();
            fail();
        }
        assertEquals(msg1 + msg2 + msg3, transportFilter.getWrittenData());
    }

    private static void assertUnsupported(Callable unsupported) {
        try {
            unsupported.call();
            fail();
        } catch (UnsupportedOperationException e) {
            // expected
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private static class TestWriteListener implements WriteListener {

        private static final ByteBuffer CLOSE = ByteBuffer.allocate(0);
        private static final ByteBuffer FLUSH = ByteBuffer.allocate(0);

        private final ChunkedBodyOutputStream outputStream;
        private final Queue<ByteBuffer> message = new LinkedList<>();
        private final int outputArraySize;

        private volatile boolean listenerCallExpected = true;
        private volatile Throwable error;

        TestWriteListener(ChunkedBodyOutputStream outputStream) {
            this(outputStream, -1);
        }

        TestWriteListener(ChunkedBodyOutputStream outputStream, int outputArraySize) {
            this.outputStream = outputStream;

            this.outputArraySize = outputArraySize;
        }

        void write(String message) {
            byte[] bytes = message.getBytes();
            this.message.add(ByteBuffer.wrap(bytes));
            doWrite();
        }

        void close() {
            message.add(CLOSE);
            doWrite();
        }

        void flush() {
            message.add(FLUSH);
            doWrite();
        }

        @Override
        public void onWritePossible() {
            if (!listenerCallExpected) {
                fail();
            }

            listenerCallExpected = false;
            doWrite();
        }

        private void doWrite() {

            while (message.peek() != null
                    && (outputStream.isReady() || message.peek() == CLOSE || message.peek() == FLUSH)) {
                try {

                    ByteBuffer headBuffer = message.peek();

                    if (headBuffer == CLOSE) {
                        outputStream.close();
                        message.poll();
                        continue;
                    }

                    if (headBuffer == FLUSH) {
                        outputStream.flush();
                        message.poll();
                        continue;
                    }

                    if (outputArraySize == -1) {
                        outputStream.write(headBuffer.get());
                    } else {
                        int arraySize = outputArraySize;
                        if (headBuffer.remaining() < arraySize) {
                            arraySize = headBuffer.remaining();
                        }

                        byte[] outputArray = new byte[arraySize];
                        headBuffer.get(outputArray);
                        outputStream.write(outputArray);
                    }

                    if (!headBuffer.hasRemaining()) {
                        message.poll();
                    }
                } catch (IOException e) {
                    error = e;
                }
            }

            if (!outputStream.isReady()) {
                listenerCallExpected = true;
            }
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        public Throwable getError() {
            return error;
        }
    }

    private static class TestStream extends ChunkedBodyOutputStream {

        TestStream(int bufferSize) {
            super(bufferSize);
        }

        @Override
        protected ByteBuffer encodeToHttp(ByteBuffer byteBuffer) {
            return byteBuffer;
        }
    }

    private static class MockTransportFilter extends Filter<ByteBuffer, Void, Void, Void> {

        private final ByteArrayOutputStream writtenData = new ByteArrayOutputStream();

        private volatile boolean pendingWrite = false;
        private volatile boolean block = false;
        private volatile CountDownLatch blockLatch;
        private volatile CompletionHandler<ByteBuffer> completionHandler;
        private volatile Throwable exception;

        MockTransportFilter() {
            super(null);
        }

        @Override
        void write(ByteBuffer data, CompletionHandler<ByteBuffer> completionHandler) {
            if (pendingWrite) {
                completionHandler.failed(new WritePendingException());
            }

            pendingWrite = true;

            while (data.hasRemaining()) {
                writtenData.write(data.get());
            }

            if (block) {
                this.completionHandler = completionHandler;
                if (blockLatch != null) {
                    blockLatch.countDown();
                }
                return;
            }

            pendingWrite = false;
            if (exception == null) {
                completionHandler.completed(data);
            } else {
                completionHandler.failed(exception);
            }
        }

        String getWrittenData() {
            return new String(writtenData.toByteArray());
        }

        void block(CountDownLatch blockLatch) {
            this.blockLatch = blockLatch;
            block = true;
        }

        void block() {
            block = true;
        }

        void unblock() {
            block = false;
            pendingWrite = false;
            completionHandler.completed(null);
            completionHandler = null;
        }

        public void setException(Throwable exception) {
            this.exception = exception;
        }
    }
}
