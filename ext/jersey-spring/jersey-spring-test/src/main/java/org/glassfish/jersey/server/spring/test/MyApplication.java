package org.glassfish.jersey.server.spring.test;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Injections;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Application;

public class MyApplication extends Application {

    @Inject
    public MyApplication(ServiceLocator serviceLocator) {
        DynamicConfiguration dc = Injections.getConfiguration(serviceLocator);
        Injections.addBinding(Injections.newBinder(DummyHK2Service.class).to(DummyHK2Service.class)
                .in(Singleton.class), dc);
        dc.commit();
    }

}
