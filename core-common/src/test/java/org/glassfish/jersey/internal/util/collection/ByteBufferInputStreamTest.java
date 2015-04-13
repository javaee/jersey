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
package org.glassfish.jersey.internal.util.collection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.jersey.internal.LocalizationMessages;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link ByteBufferInputStream} unit tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ByteBufferInputStreamTest {

    @Test
    public void testBlockingReadAByteEmptyStream() throws Exception {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        bbis.closeQueue();
        assertEquals(-1, bbis.read());
    }

    @Test
    public void testNonBlockingReadAByteEmptyStream() throws Exception {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        bbis.closeQueue();
        assertEquals(-1, bbis.tryRead());
    }

    @Test
    public void testBlockingReadByteArrayEmptyStream() throws Exception {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        bbis.closeQueue();
        byte[] buf = new byte[1024];
        assertEquals(-1, bbis.read(buf));
    }

    @Test
    public void testNonBlockingReadByteArrayEmptyStream() throws Exception {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        bbis.closeQueue();
        byte[] buf = new byte[1024];
        assertEquals(-1, bbis.tryRead(buf));
    }

    @Test
    public void testBlockingReadByteArrayFromFinishedExactLengthStream() throws Exception {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        byte[] sourceData = new byte[1024];
        new Random().nextBytes(sourceData);
        ByteBuffer byteBuf = ByteBuffer.wrap(sourceData);
        bbis.put(byteBuf);
        bbis.closeQueue();
        byte[] buf = new byte[1024];
        assertEquals(1024, bbis.read(buf));
        // no more data to read; so it should return -1
        assertEquals(-1, bbis.read(buf));
    }

    @Test
    public void testNonBlockingReadByteArrayFromFinishedExactLengthStream() throws Exception {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        byte[] sourceData = new byte[1024];
        new Random().nextBytes(sourceData);
        ByteBuffer byteBuf = ByteBuffer.wrap(sourceData);
        bbis.put(byteBuf);
        byte[] buf = new byte[1024];
        assertEquals(1024, bbis.tryRead(buf));
        // the queue has not been close; so it should return 0
        assertEquals(0, bbis.tryRead(buf));
        bbis.closeQueue();
    }

    @Test
    public void testBlockingReadByteArrayFromUnfinishedExactLengthStream() throws Exception {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        byte[] sourceData = new byte[1024];
        new Random().nextBytes(sourceData);
        ByteBuffer byteBuf = ByteBuffer.wrap(sourceData);
        bbis.put(byteBuf);
        final byte[] buf = new byte[1024];
        assertEquals(1024, bbis.read(buf));
        final AtomicBoolean closed = new AtomicBoolean(false);
        final Semaphore s = new Semaphore(1);
        s.acquire();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // it should return -1 since there is no more data
                    assertEquals(-1, bbis.read(buf));
                    // it should only reach here if the stream has been closed
                    assertTrue(closed.get());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    s.release();
                }
            }
        });
        t.start();
        Thread.sleep(500);
        closed.set(true);
        bbis.closeQueue();
        // wait until the job is done
        s.acquire();
    }

    @Test
    public void testNonBlockingReadByteArrayFromUnfinishedExactLengthStream() throws Exception {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        byte[] sourceData = new byte[1024];
        new Random().nextBytes(sourceData);
        ByteBuffer byteBuf = ByteBuffer.wrap(sourceData);
        bbis.put(byteBuf);
        bbis.closeQueue();
        byte[] buf = new byte[1024];
        assertEquals(1024, bbis.tryRead(buf));
        assertEquals(-1, bbis.tryRead(buf));
    }

    /**
     * Test for non blocking single-byte read of the stream.
     *
     * @throws Exception in case of error.
     */
    @Test
    public void testNonBlockingReadSingleByte() throws Exception {
        final int ROUNDS = 1000;
        final int BUFFER_SIZE = 769;
        final ByteBufferInputStream bbis = new ByteBufferInputStream();

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < ROUNDS; i++) {
                        final ByteBuffer data = ByteBuffer.allocate(BUFFER_SIZE);
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println("Got interrupted.");
                            return;
                        }
                        data.clear();
                        for (int j = 0; j < data.capacity(); j++) {
                            data.put((byte) (i & 0xFF));
                        }
                        data.flip();
                        if (!bbis.put(data)) {
                            System.out.println("Pipe sink closed before writing all the data.");
                            return;
                        }
                        Thread.sleep(1); // Give the other thread a chance to run.
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                } finally {
                    bbis.closeQueue();
                }
            }
        });

        try {
            int i = 0;
            int j = 0;
            int c;
            while ((c = bbis.tryRead()) != -1) {
                if (c == Integer.MIN_VALUE) {
                    // nothing to read
                    Thread.yield(); // Give the other thread a chance to run.
                    continue;
                }
                assertEquals("At position: " + j, (byte) (i & 0xFF), (byte) (c & 0xFF));
                if (++j % BUFFER_SIZE == 0) {
                    i++;
                    Thread.yield(); // Give the other thread a chance to run.
                }
            }

            assertEquals("Number of bytes produced and bytes read does not match.", ROUNDS * BUFFER_SIZE, j);
        } finally {
            executor.shutdownNow();
            bbis.close();
        }

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("Waiting for the task to finish has timed out.");
        }
    }

    /**
     * Test for non blocking byte buffer based read of the stream.
     *
     * @throws Exception in case of error.
     */
    @Test
    public void testNonBlockingReadByteArray() throws Exception {
        final int ROUNDS = 1000;
        final int BUFFER_SIZE = 769;
        final ByteBufferInputStream bbis = new ByteBufferInputStream();

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < ROUNDS; i++) {
                        final ByteBuffer data = ByteBuffer.allocate(BUFFER_SIZE);
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println("Got interrupted.");
                            return;
                        }
                        data.clear();
                        for (int j = 0; j < data.capacity(); j++) {
                            data.put((byte) (i & 0xFF));
                        }
                        data.flip();
                        if (!bbis.put(data)) {
                            System.out.println("Pipe sink closed before writing all the data.");
                            return;
                        }
                        Thread.sleep(1); // Give the other thread a chance to run.
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                } finally {
                    bbis.closeQueue();
                }
            }
        });

        try {
            int i = 0;
            int j = 0;
            int c;
            byte[] buffer = new byte[443];
            while ((c = bbis.tryRead(buffer)) != -1) {
                if (c == 0) {
                    // nothing to read
                    Thread.yield(); // Give the other thread a chance to run.
                    continue;
                }
                for (int p = 0; p < c; p++) {
                    assertEquals("At position: " + j, (byte) (i & 0xFF), (byte) buffer[p]);
                    if (++j % BUFFER_SIZE == 0) {
                        i++;
                        Thread.yield(); // Give the other thread a chance to run.
                    }
                }
            }

            assertEquals("Number of bytes produced and bytes read does not match.", ROUNDS * BUFFER_SIZE, j);
        } finally {
            executor.shutdownNow();
            bbis.close();
        }

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("Waiting for the task to finish has timed out.");
        }
    }

    /**
     * Test for blocking single-byte read of the stream.
     *
     * @throws Exception in case of error.
     */
    @Test
    public void testBlockingReadSingleByte() throws Exception {
        final int ROUNDS = 1000;
        final int BUFFER_SIZE = 769;
        final ByteBufferInputStream bbis = new ByteBufferInputStream();

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < ROUNDS; i++) {
                        final ByteBuffer data = ByteBuffer.allocate(BUFFER_SIZE);
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println("Got interrupted.");
                            return;
                        }
                        data.clear();
                        for (int j = 0; j < data.capacity(); j++) {
                            data.put((byte) (i & 0xFF));
                        }
                        data.flip();
                        if (!bbis.put(data)) {
                            System.out.println("Pipe sink closed before writing all the data.");
                            return;
                        }
                        Thread.sleep(1); // Give the other thread a chance to run.
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                } finally {
                    bbis.closeQueue();
                }
            }
        });

        try {
            int i = 0;
            int j = 0;
            int c;
            while ((c = bbis.read()) != -1) {
                assertNotEquals("Should not read 'nothing' in blocking mode.", Integer.MIN_VALUE, c);

                assertEquals("At position: " + j, (byte) (i & 0xFF), (byte) c);
                if (++j % BUFFER_SIZE == 0) {
                    i++;
                    Thread.yield(); // Give the other thread a chance to run.
                }
            }

            assertEquals("Number of bytes produced and bytes read does not match.", ROUNDS * BUFFER_SIZE, j);
        } finally {
            executor.shutdownNow();
            bbis.close();
        }

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("Waiting for the task to finish has timed out.");
        }
    }

    /**
     * Test for blocking byte buffer based read of the stream.
     *
     * @throws Exception in case of error.
     */
    @Test
    public void testBlockingReadByteArray() throws Exception {
        final int ROUNDS = 1000;
        final int BUFFER_SIZE = 769;
        final ByteBufferInputStream bbis = new ByteBufferInputStream();

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < ROUNDS; i++) {
                        final ByteBuffer data = ByteBuffer.allocate(BUFFER_SIZE);
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println("Got interrupted.");
                            return;
                        }
                        data.clear();
                        for (int j = 0; j < data.capacity(); j++) {
                            data.put((byte) (i & 0xFF));
                        }
                        data.flip();
                        if (!bbis.put(data)) {
                            System.out.println("Pipe sink closed before writing all the data.");
                            return;
                        }
                        Thread.sleep(1); // Give the other thread a chance to run.
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                } finally {
                    bbis.closeQueue();
                }
            }
        });

        try {
            int i = 0;
            int j = 0;
            int c;
            byte[] buffer = new byte[443];
            while ((c = bbis.read(buffer)) != -1) {
                assertNotEquals("Should not read 0 bytes in blocking mode.", 0, c);

                for (int p = 0; p < c; p++) {
                    assertEquals("At position: " + j, (byte) (i & 0xFF), buffer[p]);
                    if (++j % BUFFER_SIZE == 0) {
                        i++;
                        Thread.yield(); // Give the other thread a chance to run.
                    }
                }
            }

            assertEquals("Number of bytes produced and bytes read does not match.", ROUNDS * BUFFER_SIZE, j);
        } finally {
            executor.shutdownNow();
            bbis.close();
        }

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("Waiting for the task to finish has timed out.");
        }
    }

    /**
     * Test for  available() method.
     *
     * @throws Exception in case of error.
     */
    @Test
    public void testAvailable() throws Exception {
        final int BUFFER_SIZE = 769;
        final ByteBufferInputStream bbis = new ByteBufferInputStream();

        ByteBuffer data = ByteBuffer.allocate(BUFFER_SIZE);
        data.clear();
        for (int j = 0; j < data.capacity(); j++) {
            data.put((byte) 'A');
        }
        data.flip();
        bbis.put(data);

        assertEquals("Available bytes", BUFFER_SIZE, bbis.available());

        data = ByteBuffer.allocate(BUFFER_SIZE);
        data.clear();
        for (int j = 0; j < data.capacity(); j++) {
            data.put((byte) 'B');
        }
        data.flip();
        bbis.put(data);

        assertEquals("Available bytes", 2 * BUFFER_SIZE, bbis.available());

        int c = bbis.read();
        assertEquals("Byte read", 'A', c);
        assertEquals("Available bytes", 2 * BUFFER_SIZE - 1, bbis.available());

        byte[] buff = new byte[199];
        int l = bbis.read(buff);
        assertEquals("Number of bytes read", buff.length, l);
        assertEquals("Available bytes", 2 * BUFFER_SIZE - 200, bbis.available());

        buff = new byte[1000];
        l = bbis.read(buff);
        assertEquals("Number of bytes read", buff.length, l);
        assertEquals("Available bytes", 2 * BUFFER_SIZE - 1200, bbis.available());

        bbis.closeQueue();

        l = bbis.read(buff);
        assertEquals("Number of bytes read", 2 * BUFFER_SIZE - 1200, l);
        assertEquals("Available bytes", 0, bbis.available());

        bbis.close();
    }

    /**
     * Test for  available() method.
     *
     * @throws Exception in case of error.
     */
    @Test
    public void testCloseWithThrowable() throws Exception {
        final int BUFFER_SIZE = 769;
        ByteBuffer data = ByteBuffer.allocate(BUFFER_SIZE);
        data.clear();
        for (int j = 0; j < data.capacity(); j++) {
            data.put((byte) 'A');
        }
        data.flip();

        // first invocation of available should fail with exception, but not subsequently due to closed stream
        testAction(new Task(createClosedExceptionStream(data, "FAILED")) {
            @Override
            public void run() throws IOException {
                bbis().available();
            }
        }, "FAILED", false);

        // first invocation of read should fail with exception, and subsequently due to closed stream
        testAction(new Task(createClosedExceptionStream(data, "FAILED")) {
            @Override
            public void run() throws IOException {
                bbis().read();
            }
        }, "FAILED", true);

        // first invocation of read should fail with exception, and subsequently due to closed stream
        testAction(new Task(createClosedExceptionStream(data, "FAILED")) {
            @Override
            public void run() throws IOException {
                bbis().read(new byte[10]);
            }
        }, "FAILED", true);

        // first invocation of tryRead should fail with exception, and subsequently due to closed stream
        testAction(new Task(createClosedExceptionStream(data, "FAILED")) {
            @Override
            public void run() throws IOException {
                bbis().tryRead();
            }
        }, "FAILED", true);

        // first invocation of tryRead should fail with exception, and subsequently due to closed stream
        testAction(new Task(createClosedExceptionStream(data, "FAILED")) {
            @Override
            public void run() throws IOException {
                bbis().tryRead(new byte[10]);
            }
        }, "FAILED", true);

        // first invocation of close should fail with exception, but not subsequently due closed stream
        testAction(new Task(createClosedExceptionStream(data, "FAILED")) {
            @Override
            public void run() throws IOException {
                bbis().close();
            }
        }, "FAILED", false);
    }

    private ByteBufferInputStream createClosedExceptionStream(ByteBuffer data, String exMsg) throws InterruptedException {
        final ByteBufferInputStream bbis = new ByteBufferInputStream();
        bbis.put(data);
        bbis.closeQueue(new Exception(exMsg));
        return bbis;
    }

    private void testAction(Task task, String exMsg, boolean retryFailOnClosed) throws IOException {
        try {
            task.run();
            fail("IOException expected.");
        } catch (IOException ex) {
            assertNotNull("Custom exception cause", ex.getCause());
            assertEquals("Custom exception cause message", exMsg, ex.getCause().getMessage());
        }

        if (retryFailOnClosed) {
            try {
                task.run();
                fail("IOException expected.");
            } catch (IOException ex) {
                assertEquals("Closed IOException message", LocalizationMessages.INPUT_STREAM_CLOSED(), ex.getMessage());
                assertNull("Closed IOException cause", ex.getCause());
            }
        } else {
            task.run();
        }

    }

    private abstract static class Task {

        private final ByteBufferInputStream bbis;

        protected Task(ByteBufferInputStream bbis) {
            this.bbis = bbis;
        }

        protected final ByteBufferInputStream bbis() {
            return bbis;
        }

        public abstract void run() throws IOException;
    }
}
