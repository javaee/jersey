package org.glassfish.jersey.apache.connector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

public class ClosingTest extends JerseyTest {

    private PoolingHttpClientConnectionManager connectionManager;

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig();
        config.register(TestResource.class);
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        this.connectionManager = new PoolingHttpClientConnectionManager(60, TimeUnit.SECONDS);
        config.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        config.connectorProvider(new ApacheConnectorProvider());
    }

    private int getLeasedConnections() {
        return connectionManager.getTotalStats().getLeased();
    }

    private int getAvailableConnections() {
        return connectionManager.getTotalStats().getAvailable();
    }

    private int getOpenConnections() {
        return getLeasedConnections() + getAvailableConnections();
    }

    @Test
    public void testClosingUnconsumedResponseAbortsConnection() throws Exception {
        assertEquals(0, getOpenConnections());

        Response response = target().path("productInfo")
            .request(MediaType.TEXT_PLAIN_TYPE)
            .get();
        assertEquals(200, response.getStatus());

        assertEquals(1, getLeasedConnections());
        InputStream entityStream = response.readEntity(InputStream.class);

        // should close the connection without consuming it. must not throw here
        response.close();
        assertEquals(0, getOpenConnections());

        // must not throw here
        entityStream.close();
        assertEquals(0, getOpenConnections());
    }

    @Test
    public void testClosingUnconsumedStreamConsumesConnection() throws Exception {
        assertEquals(0, getOpenConnections());

        Response response = target().path("productInfo")
            .request(MediaType.TEXT_PLAIN_TYPE)
            .get();
        assertEquals(200, response.getStatus());

        InputStream entityStream = response.readEntity(InputStream.class);

        // should consume the stream. must not throw here
        entityStream.close();
        // connection should be kept alive after consume
        assertEquals(0, getLeasedConnections());
        assertEquals(1, getAvailableConnections());

        // must not throw here
        response.close();
        assertEquals(0, getLeasedConnections());
        assertEquals(1, getAvailableConnections());
    }

    @Test
    public void testClosingConsumedStream() throws Exception {
        assertEquals(0, getOpenConnections());

        Response response = target().path("productInfo")
            .request(MediaType.TEXT_PLAIN_TYPE)
            .get();
        assertEquals(200, response.getStatus());

        InputStream entityStream = response.readEntity(InputStream.class);

        consume(entityStream);

        // connection should be kept alive after consume
        assertEquals(0, getLeasedConnections());
        assertEquals(1, getAvailableConnections());

        entityStream.close();
        response.close();

        assertEquals(0, getLeasedConnections());
        assertEquals(1, getAvailableConnections());
    }

    @Test
    public void testClosingConsumedResponse() throws Exception {
        assertEquals(0, getOpenConnections());

        Response response = target().path("productInfo")
            .request(MediaType.TEXT_PLAIN_TYPE)
            .get();
        assertEquals(200, response.getStatus());

        InputStream entityStream = response.readEntity(InputStream.class);

        consume(entityStream);

        // connection should be kept alive after consume
        assertEquals(0, getLeasedConnections());
        assertEquals(1, getAvailableConnections());

        response.close();
        entityStream.close();

        assertEquals(0, getLeasedConnections());
        assertEquals(1, getAvailableConnections());
    }

    @Test
    public void testBufferedMultipleReadEntity() throws Exception {
        assertEquals(0, getOpenConnections());

        Response response = target().path("productInfo")
            .request(MediaType.TEXT_PLAIN_TYPE)
            .get();

        response.bufferEntity();
        assertEquals(0, getLeasedConnections());
        assertEquals(1, getAvailableConnections());

        assertEquals("foo\n", new String(response.readEntity(byte[].class), "us-ascii"));
        assertEquals("foo\n", response.readEntity(String.class));

        response.close();

        assertEquals(0, getLeasedConnections());
        assertEquals(1, getAvailableConnections());
    }

    private static void consume(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        for (int readden = in.read(buffer); readden >= 0; readden = in.read(buffer)) {
        }
    }

    @Path("/")
    public static class TestResource {
        @GET
        @Path("/productInfo")
        @Produces(MediaType.TEXT_PLAIN)
        public String getProductInfo() {
            return "foo\n";
        }
    }
}
