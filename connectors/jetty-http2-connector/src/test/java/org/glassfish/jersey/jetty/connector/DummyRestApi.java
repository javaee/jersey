package org.glassfish.jersey.jetty.connector;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public interface DummyRestApi {

    @GET
    @Path("world")
    @Produces("application/json")
    Data hello();

    class Data {
        private String data;

        public Data() {
        }

        public Data(String data) {
            this.data = data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

}
