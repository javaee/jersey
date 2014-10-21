package org.glassfish.jersey.tests.integration.jersey2689;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/")
public class Resource {
    @POST
    @Path("/post-bean")
    public void processBean(@Valid SampleBean bean) {
        //do-nothing
    }
}
