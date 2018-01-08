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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.glassfish.jersey.SslConfigurator;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class SslFilterTest {

    private static final int PORT = 8321;

    @Before
    public void beforeTest() {
        System.setProperty("javax.net.ssl.keyStore", this.getClass().getResource("/keystore_server").getPath());
        System.setProperty("javax.net.ssl.keyStorePassword", "asdfgh");
        System.setProperty("javax.net.ssl.trustStore", this.getClass().getResource("/truststore_server").getPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "asdfgh");
    }

    @Test
    public void testBasicEcho() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        SslEchoServer server = new SslEchoServer();
        try {
            server.start();
            String message = "Hello world\n";
            ByteBuffer readBuffer = ByteBuffer.allocate(message.length());
            Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> clientSocket = openClientSocket("localhost", readBuffer, latch,
                    null);

            clientSocket.write(stringToBuffer(message), new CompletionHandler<ByteBuffer>() {
                @Override
                public void failed(Throwable t) {
                    t.printStackTrace();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            clientSocket.close();
            readBuffer.flip();
            String received = bufferToString(readBuffer);
            assertEquals(message, received);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testEcho100k() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        SslEchoServer server = new SslEchoServer();
        try {
            server.start();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
            }
            String message = sb.toString() + "\n";
            ByteBuffer readBuffer = ByteBuffer.allocate(message.length());
            Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> clientSocket = openClientSocket("localhost", readBuffer, latch,
                    null);

            clientSocket.write(stringToBuffer(message), new CompletionHandler<ByteBuffer>() {
                @Override
                public void failed(Throwable t) {
                    t.printStackTrace();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            clientSocket.close();
            readBuffer.flip();
            String received = bufferToString(readBuffer);
            assertEquals(message, received);
        } finally {
            server.stop();
        }
    }

    /**
     * Like {@link #testBasicEcho()}, but the conversation is terminated by the server.
     */
    @Test
    public void testCloseServer() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        SslEchoServer server = new SslEchoServer();
        try {
            server.start();
            String message = "Hello world\n";
            ByteBuffer readBuffer = ByteBuffer.allocate(message.length());
            Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> clientSocket = openClientSocket("localhost", readBuffer, latch,
                    null);

            clientSocket.write(stringToBuffer(message), new CompletionHandler<ByteBuffer>() {
                @Override
                public void failed(Throwable t) {
                    t.printStackTrace();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            server.stop();
            readBuffer.flip();
            String received = bufferToString(readBuffer);
            assertEquals(message, received);
        } finally {
            server.stop();
        }
    }

    /**
     * Test SSL re-handshake triggered by the server.
     * <p/>
     * Sends a short message. When the message has been sent by the client, the server triggers re-handshake
     * and the client send a long message to make sure the re-handshake is performed during application data flow.
     */
    @Test
    public void testRehandshakeServer() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        final SslEchoServer server = new SslEchoServer();
        try {
            server.start();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
            }
            String message1 = "Hello";
            String message2 = sb.toString() + "\n";
            ByteBuffer readBuffer = ByteBuffer.allocate(message1.length() + message2.length());
            final CountDownLatch message1Latch = new CountDownLatch(1);
            Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> clientSocket = openClientSocket("localhost", readBuffer, latch,
                    null);

            clientSocket.write(stringToBuffer(message1), new CompletionHandler<ByteBuffer>() {
                @Override
                public void failed(Throwable t) {
                    t.printStackTrace();
                }

                @Override
                public void completed(ByteBuffer result) {
                    try {
                        message1Latch.countDown();
                        server.rehandshake();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            assertTrue(message1Latch.await(5, TimeUnit.SECONDS));

            clientSocket.write(stringToBuffer(message2), new CompletionHandler<ByteBuffer>() {
                @Override
                public void failed(Throwable t) {
                    t.printStackTrace();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            clientSocket.close();
            readBuffer.flip();
            String received = bufferToString(readBuffer);
            assertEquals(message1 + message2, received);
        } finally {
            server.stop();
        }
    }

    /**
     * Test SSL re-handshake triggered by the client.
     * <p/>
     * The same as {@link #testRehandshakeServer()} except, the client starts re-handshake this time.
     */
    @Test
    public void testRehandshakeClient() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        final SslEchoServer server = new SslEchoServer();
        try {
            server.start();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
            }
            String message1 = "Hello";
            String message2 = sb.toString() + "\n";
            ByteBuffer readBuffer = ByteBuffer.allocate(message1.length() + message2.length());
            final CountDownLatch message1Latch = new CountDownLatch(1);
            final Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> clientSocket = openClientSocket("localhost", readBuffer,
                    latch, null);

            clientSocket.write(stringToBuffer(message1), new CompletionHandler<ByteBuffer>() {
                @Override
                public void failed(Throwable t) {
                    t.printStackTrace();
                }

                @Override
                public void completed(ByteBuffer result) {
                    message1Latch.countDown();
                    // startSsl is overloaded in the test so it will start re-handshake, calling startSsl on a filter
                    // for a second time will not normally cause a re-handshake
                    clientSocket.startSsl();
                }
            });

            assertTrue(message1Latch.await(5, TimeUnit.SECONDS));

            clientSocket.write(stringToBuffer(message2), new CompletionHandler<ByteBuffer>() {
                @Override
                public void failed(Throwable t) {
                    t.printStackTrace();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            clientSocket.close();
            readBuffer.flip();
            String received = bufferToString(readBuffer);
            assertEquals(message1 + message2, received);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testHostameVerificationFail() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        SslEchoServer server = new SslEchoServer();
        try {
            server.start();
            System.out.println("=== SSLHandshakeException (certificate_unknown) on the server expected ===");
            openClientSocket("127.0.0.1", ByteBuffer.allocate(0), latch, null);
            fail();
        } catch (SSLException e) {
            // expected
        } finally {
            server.stop();
        }
    }

    @Test
    public void testCustomHostameVerificationFail() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        SslEchoServer server = new SslEchoServer();
        try {
            server.start();
            HostnameVerifier verifier = new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return false;
                }
            };

            openClientSocket("localhost", ByteBuffer.allocate(0), latch, verifier);
            fail();
        } catch (SSLException e) {
            // expected
        } finally {
            server.stop();
        }
    }

    @Test
    public void testCustomHostameVerificationPass() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        SslEchoServer server = new SslEchoServer();
        try {
            server.start();
            HostnameVerifier verifier = new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            };

            openClientSocket("127.0.0.1", ByteBuffer.allocate(0), latch, verifier);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testClientAuthentication() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        SslEchoServer server = new SslEchoServer();
        try {
            server.setClientAuthentication();
            server.start();
            String message = "Hello world\n";
            ByteBuffer readBuffer = ByteBuffer.allocate(message.length());
            final Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> clientSocket = openClientSocket("localhost", readBuffer,
                    latch, null);

            clientSocket.write(stringToBuffer(message), new CompletionHandler<ByteBuffer>() {
                @Override
                public void failed(Throwable t) {
                    t.printStackTrace();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            clientSocket.close();
            readBuffer.flip();
            String received = bufferToString(readBuffer);
            assertEquals(message, received);
        } finally {
            server.stop();
        }
    }

    private String bufferToString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes);
    }

    private ByteBuffer stringToBuffer(String string) {
        byte[] bytes = string.getBytes();
        return ByteBuffer.wrap(bytes);
    }

    /**
     * Creates an SSL client. Returns when SSL handshake has been completed.
     *
     * @param completionLatch latch that will be triggered when the expected number of bytes has been received.
     * @param readBuffer      buffer where received message will be written. Must be the size of the expected message,
     *                        because when it is filled {@code completionLatch} will be triggered.
     * @throws Throwable any exception that occurs until SSL handshake has completed.
     */
    private Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> openClientSocket(String host,
                                                                                    final ByteBuffer readBuffer,
                                                                                    final CountDownLatch completionLatch,
                                                                                    HostnameVerifier customHostnameVerifier)
            throws Throwable {
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreFile(this.getClass().getResource("/truststore_client").getPath())
                .trustStorePassword("asdfgh")
                .keyStoreFile(this.getClass().getResource("/keystore_client").getPath())
                .keyStorePassword("asdfgh");

        TransportFilter transportFilter = new TransportFilter(17_000, ThreadPoolConfig.defaultConfig(), 100_000);
        final SslFilter sslFilter = new SslFilter(transportFilter, sslConfig.createSSLContext(), host, customHostnameVerifier);

        // exceptions errors that occur before SSL handshake has finished are thrown from this method
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        final CountDownLatch connectLatch = new CountDownLatch(1);
        final CountDownLatch startSslLatch = new CountDownLatch(1);
        Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> clientSocket = new Filter<ByteBuffer, ByteBuffer, ByteBuffer,
                ByteBuffer>(
                sslFilter) {

            @Override
            void processConnect() {
                connectLatch.countDown();
            }

            @Override
            boolean processRead(ByteBuffer data) {
                readBuffer.put(data);
                if (!readBuffer.hasRemaining()) {
                    completionLatch.countDown();
                }
                return false;
            }

            @Override
            void startSsl() {
                if (startSslLatch.getCount() == 1) {
                    downstreamFilter.startSsl();
                    try {
                        startSslLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    sslFilter.rehandshake();
                }
            }

            @Override
            void processSslHandshakeCompleted() {
                startSslLatch.countDown();
            }

            @Override
            void processError(Throwable t) {
                if (connectLatch.getCount() == 1 || startSslLatch.getCount() == 1) {
                    exception.set(t);
                    connectLatch.countDown();
                    startSslLatch.countDown();
                }
            }

            @Override
            void write(ByteBuffer data, CompletionHandler<ByteBuffer> completionHandler) {
                downstreamFilter.write(data, completionHandler);
            }

            @Override
            void processConnectionClosed() {
                downstreamFilter.close();
            }

            @Override
            void close() {
                downstreamFilter.close();
            }
        };

        clientSocket.connect(new InetSocketAddress(host, PORT), null);
        try {
            connectLatch.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        clientSocket.startSsl();
        if (exception.get() != null) {
            clientSocket.close();
            throw exception.get();
        }

        return clientSocket;
    }

    /**
     * SSL echo server. It expects a message to be terminated with \n.
     */
    private static class SslEchoServer {

        private final ServerSocket serverSocket;
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        private volatile SSLSocket socket;
        private volatile boolean stopped = false;

        SslEchoServer() throws IOException {
            ServerSocketFactory socketFactory = SSLServerSocketFactory.getDefault();
            serverSocket = socketFactory.createServerSocket(PORT);

        }

        void setClientAuthentication() {
            ((SSLServerSocket) serverSocket).setNeedClientAuth(true);
        }

        void start() {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket = (SSLSocket) serverSocket.accept();
                        InputStream inputStream = socket.getInputStream();

                        OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), 100);

                        while (!stopped) {
                            int result = inputStream.read();
                            if (result == -1) {
                                return;
                            }
                            outputStream.write(result);
                            // '\n' indicates end of the client message
                            if (result == '\n') {
                                outputStream.flush();
                                return;
                            }
                        }

                    } catch (IOException e) {
                        if (!e.getClass().equals(SocketException.class)) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        void stop() throws IOException {
            executorService.shutdown();
            serverSocket.close();
            if (socket != null) {
                socket.close();
            }
        }

        void rehandshake() throws IOException {
            socket.startHandshake();
        }
    }
}
