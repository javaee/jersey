package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnector;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ClientBufferingDisabledTest extends JerseyTest {

    private static final long LENGTH = 200000000l;
    private static final int CHUNK = 2048;
    private static CountDownLatch postLatch = new CountDownLatch(1);

    @Override
    protected Application configure() {
        return new ResourceConfig(MyResource.class);
    }

    @Path("resource")
    public static class MyResource {

        @POST
        public long post(InputStream is) throws IOException {
            int b;
            long count = 0;
            boolean firstByte = true;
            while ((b = is.read()) != -1) {
                if (firstByte) {
                    firstByte = false;
                    postLatch.countDown();
                }

                count++;
            }
            return count;
        }
    }


    /**
     * Test that buffering can be disabled with {@link HttpURLConnection}. By default, the
     * {@code HttpURLConnection} buffers the output entity in order to calculate the
     * Content-length request attribute. This cause problems for large entities.
     * <p>
     * This test currently uses {@link HttpUrlConnector.ConnectionFactory} to setup
     * chunk output parameter on the connection. The other way is to use
     * {@link HttpURLConnection#setFixedLengthStreamingMode(int)} method to disable
     * buffering. In the future it should use the support which allows to
     * disable buffering
     * <p/>
     * <p>
     * The similar functionality was in Jersey 1.x but did not work due to bug
     * in {@code HttpURLConnection} - this should be firstly investigated before
     * implementing the issue.
     * <p/>
     */
    @Test
    // TODO: this is workaround to https://java.net/jira/browse/JERSEY-2024
    // implement test without using ConnectionFactory after the issue is fixed.
    public void testDisableBuffering() {

        HttpUrlConnector.ConnectionFactory factory = new HttpUrlConnector.ConnectionFactory() {
            @Override
            public HttpURLConnection getConnection(URL endpointUrl) throws IOException {
                HttpURLConnection conn = (HttpURLConnection) endpointUrl.openConnection();
                conn.setChunkedStreamingMode(CHUNK);
                return conn;
            }
        };

        // This IS sends out 10 chunks and waits whether they were received on the server. This tests
        // whether the buffering is disabled.
        InputStream is = new InputStream() {
            private int cnt = 0;

            @Override
            public int read() throws IOException {
                cnt++;
                if (cnt > CHUNK * 10) {
                    try {
                        postLatch.await(3, TimeUnit.SECONDS);
                        Assert.assertEquals("waiting for chunk on the server side time-outed", 0, postLatch.getCount());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (cnt <= LENGTH) {
                    return 'a';
                } else {
                    return -1;
                }
            }
        };

        ClientConfig clientConfig = new ClientConfig();
        Connector connector = new HttpUrlConnector(factory);
        clientConfig.connector(connector);
        Client client = ClientBuilder.newClient(clientConfig);
        final Response response
                = client.target(getBaseUri()).path("resource")
                .request().post(Entity.entity(is, MediaType.APPLICATION_OCTET_STREAM));
        Assert.assertEquals(200, response.getStatus());
        final long count = response.readEntity(long.class);
        System.out.println(count);
        Assert.assertEquals(LENGTH, count);
    }
}
