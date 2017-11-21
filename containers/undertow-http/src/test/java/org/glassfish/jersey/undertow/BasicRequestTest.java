package org.glassfish.jersey.undertow;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class BasicRequestTest extends AbstractUndertowServerTester {

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class TestResource {

        @GET
        @Path("/dummy")
        public String dummyResponse(@QueryParam("answer") String answer) {
            return answer;
        }

        @GET
        @Path("/exception/{status}")
        public void exceptionResponse(@PathParam("status") int status) {
            throw new WebApplicationException(status);
        }
    }

    @Before
    public void setup() {
        startServer(TestResource.class);
    }

    @Test
    public void test400StatusCode() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(getUri().path("exception/400").build());
        assertEquals(400, target.request().get(Response.class).getStatus());
    }

    @Test
    public void test500StatusCode() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(getUri().path("exception/500").build());
        assertEquals(500, target.request().get(Response.class).getStatus());
    }

    @Test
    public void testValidResponse() {
        String answer = "Tested, working.";
        Client client = ClientBuilder.newClient();
        URI uri = getUri().path("dummy").queryParam("answer", answer).build();
        WebTarget target = client.target(uri);
        assertEquals(answer, target.request().get(String.class));
    }
}