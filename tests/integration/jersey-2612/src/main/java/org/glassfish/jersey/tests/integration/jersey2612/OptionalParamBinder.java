package org.glassfish.jersey.tests.integration.jersey2612;

import javax.inject.Singleton;
import javax.ws.rs.ext.ParamConverterProvider;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

final class OptionalParamBinder extends AbstractBinder {

    @Override
    protected void configure() {
        // Param converter providers
        bind(OptionalParamConverterProvider.class).to(ParamConverterProvider.class).in(Singleton.class);
    }
}
