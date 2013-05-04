package org.glassfish.jersey.tests.spring;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.SpringFeature;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.simple.SimpleContainer;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.simple.SimpleTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.tests.spring.TestApp;
import org.junit.Test;

public class RequestResourceTest extends JerseyTest {

    public RequestResourceTest() {
        super(new SimpleTestContainerFactory());
    }

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(TestApp.TestResource.class, SpringLifecycleListener.class);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new SimpleTestContainerFactory();
    }
    
    @Test
    public void testGet() {
        Response r = target().path("testresource").request().get();
        System.out.println("r: "+r);
        String msg = r.readEntity(String.class);
        System.out.println(">> "+msg);
        assertEquals("hello, world!", msg);
    }
    
}
