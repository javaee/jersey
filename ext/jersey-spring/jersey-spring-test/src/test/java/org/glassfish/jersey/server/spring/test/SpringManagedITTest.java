package org.glassfish.jersey.server.spring.test;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Spring managed JAX-RS resources.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class SpringManagedITTest extends AccountResourceTestBase {
    private static final Logger LOGGER = Logger.getLogger(SpringManagedITTest.class.getName());

    @Override
    protected ResourceConfig configure(ResourceConfig rc) {
        return rc.register(AccountSpringResource.class);
    }

    @Override
    protected String getResourcePath() {
        return "/spring/account";
    }

    @Test
    public void testResourceScope() {
        WebTarget t = target(getResourceFullPath());
        String message = "hello, world";
        String echo = t.path("message").request().put(Entity.text(message), String.class);
        assertEquals(message, echo);
        String msg = t.path("message").request().get(String.class);
        assertEquals(message, msg);
    }

}
