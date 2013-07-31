package org.glassfish.jersey.server.spring.test;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.core.Application;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class AccountResourceIT extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(AccountResourceIT.class.getName());

    @Override
    protected Application configure() {
        return new Application();
    }

    @Test
    public void test1() throws Exception {
        String r = target("/jersey-spring-test").path("/jersey/account/message").request().get(String.class);
        assertEquals(r, "n/a");
    }

}
