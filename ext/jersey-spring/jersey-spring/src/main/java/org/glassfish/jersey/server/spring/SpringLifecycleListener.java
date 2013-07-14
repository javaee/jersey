package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;

/**
 * JAX-RS Provider class for bootstrapping Jersey 2 Spring integration.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Provider
public class SpringLifecycleListener implements ContainerLifecycleListener {
    private static final Logger LOGGER = Logger.getLogger(SpringLifecycleListener.class.getName());
    private ServiceLocator locator;
    private ApplicationContext ctx;

    @Inject
    public SpringLifecycleListener(ServiceLocator loc) {
        LOGGER.fine("SpringLifecycleListener: "+loc);
        locator = loc;
        ctx = locator.getService(ApplicationContext.class, new Annotation[] {});
    }

    @Override
    public void onStartup(Container container) {
        LOGGER.fine("onStartup: "+container);
    }

    @Override
    public void onReload(Container container) {
        LOGGER.fine("onReload: "+container);
    }

    @Override
    public void onShutdown(Container container) {
        LOGGER.fine("onShutdown: "+container);
    }

}
