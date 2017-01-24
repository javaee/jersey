package org.glassfish.jersey.jetty.connector;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.concurrent.ThreadLocalRandom;

@Path("/")
public class DummyRestService {

    static String helloMessage = "Hello " + ThreadLocalRandom.current().nextLong();

    @GET
    @Path("world")
    @Produces("application/json")
    public Response hello() {
        return Response.accepted().entity(new DummyRestApi.Data(helloMessage)).build();
    }


}
