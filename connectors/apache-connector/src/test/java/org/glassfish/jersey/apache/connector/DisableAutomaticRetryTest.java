package org.glassfish.jersey.apache.connector;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DisableAutomaticRetryTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(DisableAutomaticRetryTest.class.getName());

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final AutoCloseServer server = new AutoCloseServer();

    public DisableAutomaticRetryTest() {
        super((TestContainerFactory) null);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        executor.submit(server);
    }

    @After
    public void tearDown() throws Exception {
        server.serverSocket.close();
        executor.shutdownNow();

        if (executor.awaitTermination(5, TimeUnit.SECONDS) == false) {
            LOGGER.log(Level.SEVERE, "Executor timeout on shutdown");
        }
    }

    @Test
    public void testAutomaticRetry() {
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());

        Client client = ClientBuilder.newClient(config);
        WebTarget r = client.target(getBaseUri());

        try {
            r.request().get(String.class);
            fail("request should fail");
        } catch (Exception e) { }

        // 1 times + retry 3 times
        assertEquals(4, server.connectionCount);
    }

    @Test
    public void testDisableAutomaticRetry() {
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ApacheClientProperties.DISABLE_AUTOMATIC_RETRIES, true);

        Client client = ClientBuilder.newClient(config);
        WebTarget r = client.target(getBaseUri());
        try {
            r.request().get(String.class);
            fail("request should fail");
        } catch (Exception e) { }

        assertEquals(1, server.connectionCount);
    }

    public class AutoCloseServer implements Runnable {

        public ServerSocket serverSocket;
        public int connectionCount = 0;

        @Override
        public void run() {
            LOGGER.info("AutoCloseServer started");

            final int port = getPort();

            try {
                serverSocket = new ServerSocket(port);

                while (true) {
                    try (Socket clientSocket = serverSocket.accept()) {
                        connectionCount++;
                        LOGGER.info("AutoCloseServer received connection");
                    }
                }
            } catch (IOException e) {
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) { }
                }
            }
        }
    }

    @Path("dummy")
    public static class DummyResource { }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(DummyResource.class);
        return config;
    }
}
