package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
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
    
    @Inject
    public SpringLifecycleListener(ServiceLocator loc) {
        LOGGER.fine("SpringLifecycleListener: "+loc);
        locator = loc;
    }

    @Override
    public void onStartup(Container container) {
        LOGGER.fine("onStartup: "+container);

        // initialize Spring context
        ApplicationContext ctx = null;
        if(container instanceof ServletContainer) {
            ServletContainer sc = (ServletContainer)container;
            ctx = WebApplicationContextUtils.getWebApplicationContext(sc.getServletContext());
            if(ctx == null) {
                LOGGER.info("failed to get Spring context, jersey-spring init skipped");
            }
        } else {
            LOGGER.fine("initializing Spring context");
            ctx = new ClassPathXmlApplicationContext(new String[] {"applicationContext.xml"});
        }

        // register @Autowired annotation handler and Spring context with HK2 ServiceLocator.
        if(ctx != null) {
            LOGGER.fine("registering Spring injection resolver");
            DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
            DynamicConfiguration c = dcs.createDynamicConfiguration();
            AutowiredInjectResolver r = new AutowiredInjectResolver(ctx);
            c.addActiveDescriptor(BuilderHelper.createConstantDescriptor(r));

            c.addActiveDescriptor(BuilderHelper.createConstantDescriptor(ctx, null, ApplicationContext.class));
            c.commit();

            LOGGER.info("jersey-spring initialized");
        } else {
            LOGGER.info("not a ServletContainer, jersey-spring init skipped: "+container);
        }

        // detect JAX-RS resource classes that are also Spring @Components.
        // register these with HK2 ServiceLocator to manage their lifecycle using Spring.
        // TODO: add support for request scope.
        for(Class<?> cl : container.getConfiguration().getClasses()) {
            DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
            DynamicConfiguration c = dcs.createDynamicConfiguration();

            if(cl.isAnnotationPresent(Component.class)) {
                Object o = ctx.getBean(cl);
                if(o == null) {
                    LOGGER.severe("unable to get bean from Spring context: "+cl);
                    continue;
                }
                locator.inject(o);
                c.addActiveDescriptor(BuilderHelper.createConstantDescriptor(o, null, o.getClass()));
                c.commit();
            }
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
