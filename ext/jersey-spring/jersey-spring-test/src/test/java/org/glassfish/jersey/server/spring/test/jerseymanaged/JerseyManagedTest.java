
package org.glassfish.jersey.server.spring.test.jerseymanaged;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.server.spring.test.DummyHK2Service;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class JerseyManagedTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig rc = new ResourceConfig(AccountJerseyResource.class)
                .register(new DummyHK2ServiceBinder<DummyHK2Service>(new DummyHK2Service(), DummyHK2Service.class))
                .register(SpringLifecycleListener.class)
                .register(RequestContextFilter.class)
       ;
        rc.property(ServerProperties.PROVIDER_PACKAGES, RequestContextFilter.class.getPackage().getName());
        rc.property("contextConfigLocation", "applicationContext.xml");
        System.out.println("rc: "+rc.getClasses());
        return rc;
    }

    @Test
    public void testInjectSingleton() {
        System.out.println("** testInjectSingleton: ");
        BigDecimal newBalance = new BigDecimal(Math.random());
        target("/jersey/account/inject/xyz123").request().put(Entity.entity(newBalance.toString(), MediaType.TEXT_PLAIN_TYPE));
        BigDecimal balance = target("/jersey/account/inject/xyz123").request().get(BigDecimal.class);
        assertEquals(newBalance, balance);
    }

//    @Test
    public void testInjectRequest() {
    }

//    @Test
    public void testInjectPerLookup() {
    }

//    @Test
    public void testAutowiredSingleton() {
    }

//    @Test
    public void testAutowiredRequest() {
    }

//    @Test
    public void testAutowiredPerLookup() {
    }

//    @Override
//    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
//        return new GrizzlyServletTestContainerFactory();
//    }

    static class DummyHK2ServiceBinder<T> extends AbstractBinder {
        private T service;
        private Class<T> contractType;

        public DummyHK2ServiceBinder(T service, Class<T> contractType) {
            this.service = service;
            this.contractType = contractType;
        }

        @Override
        protected void configure() {
            bind(BuilderHelper.link(contractType).in(PerLookup.class).build());
        }
    }
}