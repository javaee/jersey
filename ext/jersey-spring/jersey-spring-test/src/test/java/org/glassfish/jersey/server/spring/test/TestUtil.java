package org.glassfish.jersey.server.spring.test;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;

class TestUtil {

    public static ResourceConfig registerHK2Services(ResourceConfig rc) {
        rc
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
                });
        return rc;
    }
}
