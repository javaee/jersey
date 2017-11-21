package org.glassfish.jersey.undertow;

import io.undertow.Undertow;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractUndertowServerTester {
    private static final Logger LOGGER = Logger.getLogger(AbstractUndertowServerTester.class.getName());
    private static final int DEFAULT_PORT = 9998;

    /**
     * Get the port to be used for test application deployments.
     *
     * @return The HTTP port of the URI
     */
    private int getPort() {
        final String value = AccessController
                .doPrivileged(PropertiesHelper.getSystemProperty("jersey.config.test.container.port"));
        if (value != null) {

            try {
                final int i = Integer.parseInt(value);
                if (i <= 0) {
                    throw new NumberFormatException("Value not positive.");
                }
                return i;
            } catch (NumberFormatException e) {
                LOGGER.log(Level.CONFIG,
                        "Value of 'jersey.config.test.container.port'"
                                + " property is not a valid positive integer [" + value + "]."
                                + " Reverting to default [" + DEFAULT_PORT + "].",
                        e);
            }
        }
        return DEFAULT_PORT;
    }

    private volatile Undertow server;

    UriBuilder getUri() {
        return UriBuilder.fromUri("http://localhost").port(getPort());
    }

    void startServer(Class... resources) {
        ResourceConfig config = new ResourceConfig(resources);
        config.register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
        final URI baseUri = getBaseUri();
        server = UndertowHttpContainerFactory.createServer(baseUri, config);
        LOGGER.log(Level.INFO, "Jetty-http server started on base uri: " + baseUri);
    }

    void startServer(ResourceConfig config) {
        final URI baseUri = getBaseUri();
        server = UndertowHttpContainerFactory.createServer(baseUri, config);
        LOGGER.log(Level.INFO, "Jetty-http server started on base uri: " + baseUri);
    }

    private URI getBaseUri() {
        return UriBuilder.fromUri("http://localhost/").port(getPort()).build();
    }

    private void stopServer() {
        try {
            server.stop();
            server = null;
            LOGGER.log(Level.INFO, "Jetty-http server stopped.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void tearDown() {
        if (server != null) {
            stopServer();
        }
    }
}
