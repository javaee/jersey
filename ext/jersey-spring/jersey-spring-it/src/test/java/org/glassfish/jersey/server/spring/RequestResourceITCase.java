package org.glassfish.jersey.server.spring;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;


public class RequestResourceITCase extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig();
    }
    
    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    
    @Test
    public void testGet() {
        Response r = target("filter").path("index.jsp").request().get();
    }
}
