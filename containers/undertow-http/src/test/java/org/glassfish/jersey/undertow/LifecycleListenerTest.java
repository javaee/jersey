package org.glassfish.jersey.undertow;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class LifecycleListenerTest extends AbstractUndertowServerTester {

    @Path("/")
    public static class TestResource {
        @GET
        public void doNothing() {
        }
    }

    public static class Reloader extends AbstractContainerLifecycleListener {
        Container container;

        public void reload(ResourceConfig newConfig) {
            container.reload(newConfig);
        }

        @Override
        public void onStartup(Container container) {
            this.container = container;
        }
    }

    @Test
    public void testReload() throws Exception {
        Reloader reloader = new Reloader();
        ResourceConfig config = new ResourceConfig();
        config.registerInstances(reloader);

        startServer(config);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(getUri().path("/").build());
        assertEquals(404, target.request().get(Response.class).getStatus());

        reloader.reload(new ResourceConfig(TestResource.class));
        assertEquals(204, target.request().get(Response.class).getStatus());
    }
}
