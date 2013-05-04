package org.glassfish.jersey.tests.spring;

import static org.junit.Assert.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.simple.SimpleTestContainerFactory;
import org.glassfish.jersey.tests.spring.springmanaged.SpringManagedSingletonResource;
import org.junit.Test;

public class SimpleContainerTest extends JerseyTest {

    /**
     * Creates new instance.
     */
    public SimpleContainerTest() {
        super(new SimpleTestContainerFactory());
    }

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(Resource.class, SpringManagedSingletonResource.class);
    }

    /**
     * Test resource class.
     */
    @Path("one")
    public static class Resource {

        /**
         * Test resource method.
         *
         * @return Test simple string response.
         */
        @GET
        public String getSomething() {
            return "get";
        }
    }

    @Test
    /**
     * Test {@link Simple HttpServer} container.
     */
    public void testSimpleContainerTarget() {
        final Response response = target().path("one").request().get();
        String e = response.readEntity(String.class);
        System.out.println("resp: "+e);
        
        System.out.println("resp: "+target().path("managedsingleton/item").request().get());

        assertEquals("Response status unexpected.", 200, response.getStatus());
        assertEquals("Response entity unexpected.", "get", e);
    }
}