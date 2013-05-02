package org.glassfish.jersey.server.spring;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.tests.spring.TestApp;
import org.junit.Test;

public class RequestResourceITCase extends JerseyTest {
    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig().registerClasses(TestApp.class);
    }
    
    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }
    
    @Test
    public void testGet() {
        Response r = target().path("testresource").request().get();
        assertEquals("hello, world!", r.readEntity(String.class));
    }
    
}
