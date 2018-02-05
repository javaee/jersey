package org.glassfish.jersey.examples.osgihttpservice.spring;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("status2")
@Named
public class SpringResource {

    @Inject
    private StatusGenerator statusGenerator;

    @GET
    @Produces("text/plain")
    public String getStatus() {
        return statusGenerator.status();
    }
}
