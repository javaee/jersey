package org.glassfish.jersey.server.spring.test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import org.springframework.stereotype.Repository;

@Repository
@Path("/spring/repository")
public class RepositoryResource {
    private String message;
    
    @PUT
    @Path("message")
    @Consumes(MediaType.TEXT_PLAIN)
    public String setMessage(String message) {
        this.message = message;
        return message;
    }
    
    @GET
    @Path("message")
    public String getMessage() {
        return message;
    }
}