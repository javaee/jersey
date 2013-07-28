package org.glassfish.jersey.server.spring.test.springmanaged;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import javax.ws.rs.core.Application;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class AccountResourceIT extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(AccountResourceIT.class.getName());

    @Override
    protected Application configure() {
        set(TestProperties.CONTAINER_FACTORY, "org.glassfish.jersey.test.external.ExternalTestContainerFactory");
        return new Application();
    }

    @Test
    public void test1() throws Exception {
        String r = target("/myapp").path("/jersey/account").request().get(String.class);
        assertEquals(r, "hello");
    }

}
