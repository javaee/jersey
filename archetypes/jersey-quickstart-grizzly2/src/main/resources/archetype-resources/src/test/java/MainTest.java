package $package;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Target;
import javax.ws.rs.ext.ClientFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MainTest {

    private HttpServer server;
    private Target target;

    @Before
    public void setUp() throws Exception {
        server = Main.startServer();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    /**
     * Test to see that the message "Got it!" is sent in the response.
     */
    @Test
    public void testMyResource() {
        // create the client
        Client c = ClientFactory.newClient();
        target = c.target(Main.BASE_URI);
        String responseMsg = target.path("myresource").request().get(String.class);
        assertEquals("Got it!", responseMsg);
    }
}
