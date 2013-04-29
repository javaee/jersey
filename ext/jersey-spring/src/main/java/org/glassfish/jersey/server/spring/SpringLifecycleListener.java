package org.glassfish.jersey.server.spring;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import java.util.logging.Logger;

/**
*
* @author Marko Asplund (marko.asplund at gmail.com)
*/
@Provider
public class SpringLifecycleListener implements ContainerLifecycleListener {
    private static final Logger LOGGER = Logger.getLogger(SpringLifecycleListener.class.getName());
    private ServiceLocator serviceLocator;
    
    @Inject
    public SpringLifecycleListener(ServiceLocator loc) {
        LOGGER.info("SpringLifecycleListener: "+loc);
        serviceLocator = loc;
    }

    @Override
    public void onStartup(Container container) {
        LOGGER.info("onStartup: "+container);
    }

    @Override
    public void onReload(Container container) {
        LOGGER.info("onReload: "+container);
    }

    @Override
    public void onShutdown(Container container) {
        LOGGER.info("onShutdown: "+container);
    }

}
