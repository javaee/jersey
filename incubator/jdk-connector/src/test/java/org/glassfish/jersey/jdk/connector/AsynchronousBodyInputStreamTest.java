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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static junit.framework.TestCase.assertNull;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class AsynchronousBodyInputStreamTest {

    @Test
    public void testBasicAsyncRead() {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();
        doTestBasicAsyncRead(stream, new TestReadListener(stream));
    }

    @Test
    public void testBasicAsyncArrayRead() {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();
        doTestBasicAsyncRead(stream, new TestReadListener(stream, 15));
    }

    @Test
    public void testBasicAsyncReadWithException() {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();
        doTestBasicAsyncReadWithException(stream, new TestReadListener(stream));
    }

    @Test
    public void testBasicAsyncArrayReadWithException() {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();
        doTestBasicAsyncReadWithException(stream, new TestReadListener(stream, 15));
    }

    @Test
    public void testListenerExecutor() throws InterruptedException {
        final AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Thread mainThread = Thread.currentThread();
        final AtomicReference<Thread> dataAvailableThread = new AtomicReference<>();
        final AtomicReference<Thread> allDataReadThread = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            stream.setListenerExecutor(executor);
            stream.setReadListener(new ReadListener() {
                @Override
                public void onDataAvailable() throws IOException {
                    dataAvailableThread.set(Thread.currentThread());
                    while (stream.isReady()) {
                        stream.read();

                    }
                }

                @Override
                public void onAllDataRead() {
                    allDataReadThread.set(Thread.currentThread());
                    latch.countDown();
                }

                @Override
                public void onError(Throwable t) {

                }
            });

            stream.notifyDataAvailable(stringToBuffer("Message"));
            stream.notifyAllDataRead();
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertNotEquals(mainThread, dataAvailableThread.get());
        assertNotEquals(mainThread, allDataReadThread.get());
    }

    @Test
    public void testDataBeforeAsyncModeCommit() {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();

        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";
        String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        String msg4 = "DDDDD";

        stream.notifyDataAvailable(stringToBuffer(msg1));
        stream.notifyDataAvailable(stringToBuffer(msg2));

        TestReadListener readListener = new TestReadListener(stream);
        stream.setReadListener(readListener);

        assertEquals(msg1 + msg2, readListener.getReceivedData());

        stream.notifyDataAvailable(stringToBuffer(msg3));
        stream.notifyDataAvailable(stringToBuffer(msg4));

        stream.notifyAllDataRead();

        assertEquals(msg1 + msg2 + msg3 + msg4, readListener.getReceivedData());
        assertTrue(readListener.isAllDataRead());
        assertNull(readListener.getError());
    }

    @Test
    public void testDataBeforeSyncModeCommit() throws IOException {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();

        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";
        String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        String msg4 = "DDDDD";

        stream.notifyDataAvailable(stringToBuffer(msg1));
        stream.notifyDataAvailable(stringToBuffer(msg2));

        assertEquals((msg1 + msg2).length(), stream.available());

        stream.notifyDataAvailable(stringToBuffer(msg3));
        stream.notifyDataAvailable(stringToBuffer(msg4));

        assertEquals((msg1 + msg2 + msg3 + msg4).length(), stream.available());
    }

    @Test
    public void testAllDataBeforeAsyncModeCommit() {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();

        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";

        stream.notifyDataAvailable(stringToBuffer(msg1));
        stream.notifyDataAvailable(stringToBuffer(msg2));

        stream.notifyAllDataRead();

        TestReadListener readListener = new TestReadListener(stream);
        stream.setReadListener(readListener);

        assertEquals(msg1 + msg2, readListener.getReceivedData());
        assertTrue(readListener.isAllDataRead());
        assertNull(readListener.getError());
    }

    @Test
    public void testAllDataBeforeSyncModeCommit() throws IOException {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();

        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";

        stream.notifyDataAvailable(stringToBuffer(msg1));
        stream.notifyDataAvailable(stringToBuffer(msg2));

        stream.notifyAllDataRead();

        assertEquals((msg1 + msg2).length(), stream.available());
    }

    @Test
    public void testErrorBeforeAsyncModeCommit() throws IOException {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();

        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";

        stream.notifyDataAvailable(stringToBuffer(msg1));
        stream.notifyDataAvailable(stringToBuffer(msg2));

        Throwable t = new Throwable();
        stream.notifyError(t);

        TestReadListener readListener = new TestReadListener(stream);
        stream.setReadListener(readListener);

        assertEquals(msg1 + msg2, readListener.getReceivedData());
        assertFalse(readListener.isAllDataRead());
        assertTrue(readListener.getError() == t);
    }

    @Test
    public void testErrorBeforeSyncModeCommit() throws IOException {
        AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();

        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";

        stream.notifyDataAvailable(stringToBuffer(msg1));
        stream.notifyDataAvailable(stringToBuffer(msg2));

        Throwable t = new Throwable();
        stream.notifyError(t);

        try {
            stream.available();
            fail();
        } catch (Throwable e) {
            assertTrue(e.getCause() == t);
        }
    }

    @Test
    public void testUnsupportedSync() {
        final AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();
        try {
            // touch this stream to make it synchronous
            stream.tryRead();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        assertUnsupported(() -> {
            stream.isReady();
            return null;
        });

        assertUnsupported(() -> {
            stream.setReadListener(new TestReadListener(stream));
            return null;
        });

        assertUnsupported(() -> {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            try {
                stream.setListenerExecutor(executorService);
            } finally {
                executorService.shutdownNow();
            }
            return null;
        });
    }

    @Test
    public void testUnsupportedAsync() {
        final AsynchronousBodyInputStream stream = new AsynchronousBodyInputStream();
        stream.setReadListener(new TestReadListener(stream));

        assertUnsupported(() -> {
            stream.tryRead();
            return null;
        });

        assertUnsupported(() -> {
            stream.tryRead(new byte[10]);
            return null;
        });

        assertUnsupported(() -> {
            stream.tryRead(new byte[10], 0, 10);
            return null;
        });

        assertUnsupported(() -> stream.skip(10));

        assertUnsupported(() -> stream.available());
    }

    private void doTestBasicAsyncRead(AsynchronousBodyInputStream stream, TestReadListener readListener) {
        stream.setReadListener(readListener);

        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";
        String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        stream.notifyDataAvailable(stringToBuffer(msg1));
        stream.notifyDataAvailable(stringToBuffer(msg2));
        stream.notifyDataAvailable(stringToBuffer(msg3));
        stream.notifyAllDataRead();

        assertEquals(msg1 + msg2 + msg3, readListener.getReceivedData());
        assertTrue(readListener.isAllDataRead());
        assertNull(readListener.getError());
    }

    private void doTestBasicAsyncReadWithException(AsynchronousBodyInputStream stream, TestReadListener readListener) {
        stream.setReadListener(readListener);

        String msg1 = "AAAAAAAAAAAAAAAAAAAA";
        String msg2 = "BBBBBBBBBBBBB";
        String msg3 = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        stream.notifyDataAvailable(stringToBuffer(msg1));
        stream.notifyDataAvailable(stringToBuffer(msg2));
        stream.notifyDataAvailable(stringToBuffer(msg3));
        Throwable t = new Throwable();
        stream.notifyError(t);

        assertEquals(msg1 + msg2 + msg3, readListener.getReceivedData());
        assertFalse(readListener.isAllDataRead());
        assertTrue(readListener.getError() == t);
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

    private static ByteBuffer stringToBuffer(String str) {
        return ByteBuffer.wrap(str.getBytes());
    }

    private static class TestReadListener implements ReadListener {

        private final ByteArrayOutputStream receivedData = new ByteArrayOutputStream();
        private final AsynchronousBodyInputStream inputStream;
        private final int inputArraySize;

        private volatile Throwable error = null;
        private volatile boolean allDataRead = false;
        private volatile boolean listenerCallExpected = true;

        public TestReadListener(AsynchronousBodyInputStream inputStream, int inputArraySize) {
            this.inputStream = inputStream;
            this.inputArraySize = inputArraySize;
        }

        public TestReadListener(AsynchronousBodyInputStream inputStream) {
            this.inputStream = inputStream;
            this.inputArraySize = -1;
        }

        @Override
        public void onDataAvailable() throws IOException {
            if (!listenerCallExpected) {
                fail();
            }

            listenerCallExpected = false;
            while (inputStream.isReady()) {
                    if (inputArraySize == -1) {
                        receivedData.write(inputStream.read());
                    } else {
                        byte[] inputArray = new byte[inputArraySize];
                        int read = inputStream.read(inputArray);
                        receivedData.write(inputArray, 0, read);
                    }
            }

            listenerCallExpected = true;
        }

        @Override
        public void onAllDataRead() {
            allDataRead = true;
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        public boolean isAllDataRead() {
            return allDataRead;
        }

        public Throwable getError() {
            return error;
        }

        public String getReceivedData() {
            return new String(receivedData.toByteArray());
        }
    }
}
