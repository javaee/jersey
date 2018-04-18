package org.glassfish.jersey.jetty.connector;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.eclipse.jetty.client.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

public class ResponseContentBufferTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(MethodTest.class.getName());

    private static final String PATH = "test";

    @Path("/test")
    public static class OverflowResource {

        private static final int RESPONSE_BUFFER_SIZE_IN_BYTE = 2 * 1024 * 1024;

        @GET
        public byte[] overflow() {
            return new byte[RESPONSE_BUFFER_SIZE_IN_BYTE + 1];
        }

    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(OverflowResource.class);
        config.register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new JettyConnectorProvider());
    }

    @Test
    public void testBufferCapacityNotExceeded() {

        try {
            // exception
            target(PATH).request().get();
        } catch (Exception e) {
            fail("Buffer capacity exceeded exception not expected");
        }
    }
}
