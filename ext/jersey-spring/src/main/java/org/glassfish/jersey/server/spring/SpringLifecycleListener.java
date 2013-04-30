package org.glassfish.jersey.server.spring;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
* JAX-RS Provider class for bootstrapping Jersey 2 Spring integration.
*
* @author Marko Asplund (marko.asplund at gmail.com)
*/
@Provider
public class SpringLifecycleListener implements ContainerLifecycleListener {
    private static final Logger LOGGER = Logger.getLogger(SpringLifecycleListener.class.getName());
    private ServiceLocator locator;
    
    @Inject
    public SpringLifecycleListener(ServiceLocator loc) {
        LOGGER.fine("SpringLifecycleListener: "+loc);
        locator = loc;
    }

    @Override
    public void onStartup(Container container) {
        LOGGER.fine("onStartup: "+container);
        
        ApplicationContext ctx = null;
        if(container instanceof ServletContainer) {
            ServletContainer sc = (ServletContainer)container;
            ctx = WebApplicationContextUtils.getWebApplicationContext(sc.getServletContext());
            LOGGER.fine("context: "+ctx);
            
            LOGGER.fine("registering Spring injection resolver");
            DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
            DynamicConfiguration c = dcs.createDynamicConfiguration();
            AutowiredInjectResolver r = new AutowiredInjectResolver(ctx);
            c.addActiveDescriptor(BuilderHelper.createConstantDescriptor(r));
            c.commit();
            
            LOGGER.info("jersey-spring initialized");
        } else {
            LOGGER.info("not a ServletContainer, jersey-spring init skipped");
        }
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
