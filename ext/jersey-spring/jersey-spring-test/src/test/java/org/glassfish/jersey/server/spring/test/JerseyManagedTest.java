
package org.glassfish.jersey.server.spring.test;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.inject.Singleton;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Jersey managed JAX-RS resources.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class JerseyManagedTest extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(JerseyManagedTest.class.getName());

    @Override
    protected Application configure() {
        ResourceConfig rc = new ResourceConfig(AccountJerseyResource.class)
                .register(SpringLifecycleListener.class)
                .register(RequestContextFilter.class)
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(BuilderHelper.link(HK2ServiceSingleton.class).in(Singleton.class).build());
                    }
                })
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(BuilderHelper.link(HK2ServiceRequestScoped.class).in(RequestScoped.class).build());
                    }
                })
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(BuilderHelper.link(HK2ServicePerLookup.class).in(PerLookup.class).build());
                    }
                })
       ;
        rc.property("contextConfigLocation", "classpath:applicationContext.xml");
        return rc;
    }

    // test singleton scoped Spring bean injection using @Inject + @Autowired
    @Test
    public void testSingletonScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target("/jersey/account");

        t.path("/singleton/xyz123").request().put(Entity.entity(newBalance.toString(), MediaType.TEXT_PLAIN_TYPE));
        BigDecimal balance = t.path("/singleton/autowired/xyz123").request().get(BigDecimal.class);
        assertEquals(newBalance, balance);
    }

    @Test
    public void testRequestScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target("/jersey/account");
        String a = t.path("request/abc456").request().put(Entity.text(newBalance), String.class);
        System.out.println("***: "+a);
    }

    @Test
    public void testPrototypeScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target("/jersey/account");
        String a = t.path("prototype/abc456").request().put(Entity.text(newBalance), String.class);
        System.out.println("***: "+a);
    }

}