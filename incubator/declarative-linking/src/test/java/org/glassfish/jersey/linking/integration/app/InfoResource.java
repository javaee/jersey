package org.glassfish.jersey.linking.integration.app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.linking.integration.representations.Info;

@Path("/info")
public class InfoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response info() {
        Info info = new Info();
        info.setVersion("1.1");
        return Response.ok(info).build();
    }

}
