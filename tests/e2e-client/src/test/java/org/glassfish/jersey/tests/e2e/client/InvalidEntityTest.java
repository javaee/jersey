package org.glassfish.jersey.tests.e2e.client;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.rx.rxjava.RxObservableInvokerProvider;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InvalidEntityTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig()
                .register(Resource.class)
                .register(JacksonFeature.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new GrizzlyConnectorProvider())
                .register(RxObservableInvokerProvider.class)
                .register(JacksonFeature.class);
    }

    @Test(expected = ExecutionException.class)
    public void shouldReceiveErrorOnInvalidEntity() throws ExecutionException, InterruptedException, TimeoutException {
        target("/test")
                .path("/")
                .request(MediaType.APPLICATION_JSON)
                .async()
                .get(new EmptyCallback())
                .get(30, TimeUnit.SECONDS);
    }

    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public static class Resource {

        @GET
        public String get() {
            return "{\"id\":1,\"owner\":\"123\"}"; // intentionally invalid json
        }
    }

    public static class Entity {
        public long id;
        public String name;
    }

    private static class EmptyCallback implements InvocationCallback<Entity> {

        @Override
        public void completed(Entity entity) {

        }

        @Override
        public void failed(Throwable throwable) {

        }
    }

}
