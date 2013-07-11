package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.util.Map;
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
        LOGGER.fine("initializing Spring context");
        ApplicationContext ctx = null;
        if(container instanceof ServletContainer) {
            ServletContainer sc = (ServletContainer)container;
            ctx = WebApplicationContextUtils.getWebApplicationContext(sc.getServletContext());
        } else {
            ctx = new ClassPathXmlApplicationContext(new String[] {"applicationContext.xml"});
        }
        if(ctx == null) {
            LOGGER.severe("failed to get Spring context, jersey-spring init skipped");
            return;
        }

        LOGGER.fine("registering Spring injection resolvers");
        if(ctx != null) {
            // initialize HK2 spring-bridge
            SpringBridge.getSpringBridge().initializeSpringBridge(locator);
            SpringIntoHK2Bridge springBridge = locator.getService(SpringIntoHK2Bridge.class);
            springBridge.bridgeSpringBeanFactory(ctx);

            DynamicConfiguration c = Injections.getConfiguration(locator);

            // register Spring @Autowired annotation handler with HK2 ServiceLocator
            AutowiredInjectResolver r = new AutowiredInjectResolver(ctx);
            c.addActiveDescriptor(BuilderHelper.createConstantDescriptor(r));

            // register Spring context
            c.addActiveDescriptor(BuilderHelper.createConstantDescriptor(ctx, "SpringContext", ApplicationContext.class));
            c.commit();

        } else {
            LOGGER.severe("not a ServletContainer, jersey-spring init aborted: "+container);
            return;
        }

        // detect JAX-RS resource classes that are also Spring @Components.
        // register these with HK2 ServiceLocator to manage their lifecycle using Spring.
        LOGGER.fine("registering Spring managed JAX-RS resources");
        for(Class<?> cl : container.getConfiguration().getClasses()) {
            if(!cl.isAnnotationPresent(Component.class)) {
                continue;
            }
            DynamicConfiguration c = Injections.getConfiguration(locator);
            Map<String, ?> beans = ctx.getBeansOfType(cl);
            if(beans.size() != 1) {
                LOGGER.severe("none or multiple beans found in Spring context of type: "+cl);
                continue;
            }
            String beanName = beans.keySet().iterator().next();

            ServiceBindingBuilder bb = Injections.newFactoryBinder(new SpringManagedBeanFactory(ctx, locator, beanName));
            bb.to(cl);
            Injections.addBinding(bb, c);
            c.commit();
            LOGGER.fine(String.format("- bean '%s' registered", beanName));
        }
        LOGGER.info("jersey-spring initialized");
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
            Object bean = ctx.getBean(beanName);
            if(!isSingleton) {
                locator.inject(bean);
            }
            LOGGER.finer("provide(): "+bean);
            return bean;
        }

        @Override
        public void dispose(Object instance) {
            LOGGER.finer("dispose(): "+instance);
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
