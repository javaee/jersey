package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

/**
 * JAX-RS Provider class for processing Jersey 2 Spring integration container lifecycle events.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Provider
public class SpringLifecycleListener implements ContainerLifecycleListener {
    private static final Logger LOGGER = Logger.getLogger(SpringLifecycleListener.class.getName());
    private ServiceLocator locator;

    @Inject
    private ApplicationContext ctx;

    @Inject
    public SpringLifecycleListener(ServiceLocator loc) {
        LOGGER.fine("SpringLifecycleListener: "+loc);
        locator = loc;
    }

    @Override
    public void onStartup(Container container) {
        LOGGER.fine("onStartup: " + container);
    }

    @Override
    public void onReload(Container container) {
        LOGGER.fine("onReload: "+container);
        if(ctx instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)ctx).refresh();
        }
    }

    @Override
    public void onShutdown(Container container) {
        LOGGER.fine("onShutdown: " + container);
        if(ctx instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)ctx).close();
        }
    }

}
