package org.glassfish.jersey.tests.e2e.client;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test no successful (3XX, 4XX, 5XX) responses with no empty body.
 *
 * @author Ballesi Ezequiel (ezequielballesi at gmail.com)
 */
public class NonSuccessfulResponseTest extends JerseyTest {

	  @Override
	    protected Application configure() {
	        return new ResourceConfig(TestResource.class);
	    }

	    @Path("resource")
	    public static class TestResource {

	        @GET
	        @Path("/{status}")
	        public Response getXXX(@PathParam("status") int status) {
	            return Response.status(status).entity("get").build();
	        }

	        @POST
	        @Path("/{status}")	        
	        public Response postXXX(@PathParam("status") int status, String post) {
	            return Response.status(status).entity(post).build();
	        }

	    }

	    @Test
	    public void testGet3XX() {
			generalTestGet(302);
	    }

	    @Test
	    public void testPost3XX() {
	        generalTestPost(302);
	    }
	    
	    @Test
	    public void testGet4XX() {
			generalTestGet(401);
	    }

	    @Test
	    public void testPost4XX() {
	        generalTestPost(401);
	    }
	    
	    @Test
	    public void testGet5XX() {
			generalTestGet(500);
	    }

	    @Test
	    public void testPost5XX() {
	        generalTestPost(500);
	    }	    
	    
	    private void generalTestGet(int status) {
	    	WebTarget target = target("resource").path(Integer.toString(status));
	    	SyncInvoker sync = target.request();
	    	Response response = sync.get(Response.class);
	    	Assert.assertEquals(status, response.getStatus());
	    	Assert.assertEquals("get", response.readEntity(String.class));
	    }    	

		private void generalTestPost(int status) {
			Entity<String> entity = Entity.entity("entity", MediaType.WILDCARD_TYPE);	    	
	        WebTarget target = target("resource").path(Integer.toString(status));
	        SyncInvoker sync = target.request();
	        Response response = sync.post(entity, Response.class);
	        Assert.assertEquals(status, response.getStatus());
	        Assert.assertEquals("entity", response.readEntity(String.class));
		}   
	    
}
