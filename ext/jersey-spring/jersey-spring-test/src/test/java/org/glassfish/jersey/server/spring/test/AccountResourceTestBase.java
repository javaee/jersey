package org.glassfish.jersey.server.spring.test;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * Base class for JAX-RS resource tests.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public abstract class AccountResourceTestBase extends JerseyTest {
    private static final String TEST_WEBAPP_CONTEXT_PATH = "jersey.spring.test.contextPath";
    private static final String TEST_CONTAINER_FACTORY_EXTERNAL = "org.glassfish.jersey.test.external.ExternalTestContainerFactory";

    @Override
    protected Application configure() {
        ResourceConfig rc = new ResourceConfig()
                .register(SpringLifecycleListener.class)
                .register(RequestContextFilter.class)
                ;
        TestUtil.registerHK2Services(rc);
        rc.property("contextConfigLocation", "classpath:applicationContext.xml");
        return configure(rc);
    }

    protected abstract ResourceConfig configure(ResourceConfig rc);

    protected abstract String getResourcePath();

    protected String getResourceFullPath() {
        String containerFactory = System.getProperty(TestProperties.CONTAINER_FACTORY);
        if(TEST_CONTAINER_FACTORY_EXTERNAL.equals(containerFactory)) {
            return System.getProperty(TEST_WEBAPP_CONTEXT_PATH)+getResourcePath();
        }
        return getResourcePath();
    }

    // test singleton scoped Spring bean injection using @Inject + @Autowired
    @Test
    public void testSingletonScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target(getResourceFullPath());

        t.path("/singleton/xyz123").request().put(Entity.entity(newBalance.toString(), MediaType.TEXT_PLAIN_TYPE));
        BigDecimal balance = t.path("/singleton/autowired/xyz123").request().get(BigDecimal.class);
        assertEquals(newBalance, balance);
    }

    @Test
    public void testRequestScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target(getResourceFullPath());
        BigDecimal balance = t.path("request/abc456").request().put(Entity.text(newBalance), BigDecimal.class);
        assertEquals(newBalance, balance);
    }

    @Test
    public void testPrototypeScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target(getResourceFullPath());
        BigDecimal balance = t.path("prototype/abc456").request().put(Entity.text(newBalance), BigDecimal.class);
        assertEquals(new BigDecimal("987.65"), balance);
    }

}
