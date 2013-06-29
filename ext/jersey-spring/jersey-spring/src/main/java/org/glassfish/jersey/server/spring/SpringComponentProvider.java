package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Custom ComponentProvider class for making Jersey skip JAX-RS Spring component
 * lifecycle management and leave it to us.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class SpringComponentProvider implements ComponentProvider {
    private static final Logger LOGGER = Logger.getLogger(SpringComponentProvider.class.getName());
    private ServiceLocator locator;

    @Override
    public void initialize(ServiceLocator locator) {
        LOGGER.fine("initialize(): " + locator);
        this.locator = locator;
    }

    @Override
    public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {
        LOGGER.fine("bind(): "+component+", "+providerContracts);

        if(component.isAnnotationPresent(Component.class)) {
            return true;
        }
        return false;
    }

    @Override
    public void done() {
        LOGGER.fine("done()");
    }
}
