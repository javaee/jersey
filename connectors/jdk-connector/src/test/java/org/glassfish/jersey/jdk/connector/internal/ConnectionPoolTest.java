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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import javax.net.ServerSocketFactory;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdk.connector.JdkConnectorProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class ConnectionPoolTest extends JerseyTest {

    @Test
    public void testBasic() throws InterruptedException {
        String msg1 = "message 1";
        String msg2 = "message 2";
        CountDownLatch latch = new CountDownLatch(2);
        sendMessageToJersey(msg1, latch);
        sendMessageToJersey(msg2, latch);

        /* the idle timeout is 10s and only 1 connection is allowed, so the test should fail unless the pool reuses
        the connection for both requests */
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private void sendMessageToJersey(String message, final CountDownLatch latch) {
        target("echo").request().async().post(Entity.entity(message, MediaType.TEXT_PLAIN), new InvocationCallback<String>() {
            @Override
            public void completed(String response) {
                System.out.println("#Received: " + response);
                latch.countDown();
            }

            @Override
            public void failed(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    @Test
    public void testPersistentConnection() throws IOException, InterruptedException {
        TestServer testServer = new TestServer(true);

        try {
            testServer.start();
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger result1 = new AtomicInteger(-1);
            sendGetToTestServer(result1, latch);
            AtomicInteger result2 = new AtomicInteger(-1);
            sendGetToTestServer(result2, latch);

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            assertEquals(1, result1.get());
            assertEquals(1, result2.get());
        } finally {
            testServer.stop();
        }
    }

    @Test
    public void testNonPersistentConnection() throws IOException, InterruptedException {
        TestServer testServer = new TestServer(false);

        try {
            testServer.start();
            CountDownLatch latch1 = new CountDownLatch(1);
            AtomicInteger result1 = new AtomicInteger(-1);
            sendGetToTestServer(result1, latch1);
            assertTrue(latch1.await(5, TimeUnit.SECONDS));
            CountDownLatch latch2 = new CountDownLatch(1);

            AtomicInteger result2 = new AtomicInteger(-1);
            sendGetToTestServer(result2, latch2);

            assertTrue(latch2.await(5, TimeUnit.SECONDS));

            assertEquals(1, result1.get());
            assertEquals(2, result2.get());
        } finally {
            testServer.stop();
        }
    }

    private void sendGetToTestServer(final AtomicInteger result, final CountDownLatch latch) {
        getClient().target("http://localhost:" + TestServer.PORT).request().async().get(new InvocationCallback<Integer>() {
            @Override
            public void completed(Integer response) {
                System.out.println("#Received: " + response);
                result.set(response);
                latch.countDown();
            }

            @Override
            public void failed(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(EchoResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new JdkConnectorProvider());
        config.property(JdkConnectorProperties.MAX_CONNECTIONS_PER_DESTINATION, 1);
        config.property(JdkConnectorProperties.CONNECTION_IDLE_TIMEOUT, 10_000);
    }

    @Path("/echo")
    public static class EchoResource {

        @POST
        public String post(String entity) {
            return entity;
        }
    }

    private static class TestServer {

        static final int PORT = 8321;

        private final boolean persistentConnection;
        private final ServerSocket serverSocket;
        private final ExecutorService executorService = Executors.newCachedThreadPool();
        private final AtomicInteger connectionsCount = new AtomicInteger(0);

        private volatile boolean stopped = false;

        TestServer(boolean persistentConnection) throws IOException {
            this.persistentConnection = persistentConnection;
            ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();
            serverSocket = socketFactory.createServerSocket(PORT);
        }

        void start() {
            executorService.execute(() -> {
                try {
                    while (!stopped) {
                        final Socket socket = serverSocket.accept();
                        connectionsCount.incrementAndGet();
                        executorService.submit(() -> handleConnection(socket));

                    }
                } catch (IOException e) {
                    //do nothing
                }
            });
        }

        private void handleConnection(Socket socket) {

            try {
                InputStream inputStream = socket.getInputStream();
                ByteArrayOutputStream receivedMessage = new ByteArrayOutputStream();

                while (!stopped && !socket.isClosed()) {
                    int result = inputStream.read();
                    if (result == -1) {
                        return;
                    }

                    receivedMessage.write((byte) result);
                    String msg = new String(receivedMessage.toByteArray(), "ASCII");
                    if (msg.contains("\r\n\r\n")) {
                        receivedMessage = new ByteArrayOutputStream();
                        OutputStream outputStream = socket.getOutputStream();
                        String response = "HTTP/1.1 200 OK\r\nContent-Length: 1\r\nContent-Type: text/plain\r\n";
                        if (!persistentConnection) {
                            response += "Connection: Close\r\n";
                        }
                        response += "\r\n" + connectionsCount.get();
                        outputStream.write(response.getBytes("ASCII"));
                        outputStream.flush();
                    }
                }
            } catch (IOException e) {
                if (!e.getClass().equals(SocketException.class)) {
                    e.printStackTrace();
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void stop() throws IOException {
            executorService.shutdown();
            serverSocket.close();
        }
    }
}
