package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Logger;
import org.glassfish.hk2.api.Factory;

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
            DynamicConfiguration c = Injections.getConfiguration(locator);
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
        for(Class<?> cl : container.getConfiguration().getClasses()) {
            if(!cl.isAnnotationPresent(Component.class)) {
                continue;
            }
            DynamicConfiguration c = Injections.getConfiguration(locator);
            Map<String, ?> beans = ctx.getBeansOfType(cl);
            if(beans.size() != 1) {
                LOGGER.severe("none or multiple beans found of type: "+cl);
                continue;
            }
            String beanName = beans.keySet().iterator().next();

            ServiceBindingBuilder bb = Injections.newFactoryBinder(new SpringManagedBeanFactory(ctx, locator, beanName));
            bb.to(cl);
            Injections.addBinding(bb, c);
            c.commit();
        }
    }

    private static class SpringManagedBeanFactory implements Factory {
        private ApplicationContext ctx;
        private ServiceLocator locator;
        private String beanName;
        private boolean isSingleton;

        private SpringManagedBeanFactory(ApplicationContext ctx, ServiceLocator locator, String beanName) {
            LOGGER.fine("SpringManagedBeanFactory()");
            this.ctx = ctx;
            this.locator = locator;
            this.beanName = beanName;
            isSingleton = ctx.isSingleton(beanName);
            if(isSingleton) {
                locator.inject(ctx.getBean(beanName));
            }
        }

        @Override
        public Object provide() {
            LOGGER.fine("provide()");
            Object bean = ctx.getBean(beanName);
            if(!isSingleton) {
                locator.inject(bean);
            }
            return bean;
        }

        @Override
        public void dispose(Object instance) {
            LOGGER.fine("dispose(): "+instance);
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
