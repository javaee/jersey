package org.glassfish.jersey.server.spring.test;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.process.internal.RequestScoped;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Application;

/**
 * JAX-RS application class for configuring injectable services in HK2 registry for testing purposes.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class MyApplication extends Application {

    @Inject
    public MyApplication(ServiceLocator serviceLocator) {
        DynamicConfiguration dc = Injections.getConfiguration(serviceLocator);

        Injections.addBinding(Injections.newBinder(HK2ServiceSingleton.class).to(HK2ServiceSingleton.class)
                .in(Singleton.class), dc);

        Injections.addBinding(Injections.newBinder(HK2ServiceRequestScoped.class).to(HK2ServiceRequestScoped.class)
                .in(RequestScoped.class), dc);

        Injections.addBinding(Injections.newBinder(HK2ServicePerLookup.class).to(HK2ServicePerLookup.class)
                .in(PerLookup.class), dc);

        dc.commit();
    }

}
