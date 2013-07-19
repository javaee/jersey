package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Custom ComponentProvider class.
 * Responsible for 1) bootstrapping Jersey 2 Spring integration and
 * 2) making Jersey skip JAX-RS Spring component lifecycle management and leave it to us.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class SpringComponentProvider implements ComponentProvider {
    private static final Logger LOGGER = Logger.getLogger(SpringComponentProvider.class.getName());
    private ServiceLocator locator;
    private ApplicationContext ctx;

    @Override
    public void initialize(ServiceLocator locator) {
        LOGGER.fine("initialize(): " + locator);
        this.locator = locator;

        LOGGER.fine("initializing Spring context");
        ServletContext sc = locator.getService(ServletContext.class, new Annotation[] {});
        if(sc != null) {
            ctx = WebApplicationContextUtils.getWebApplicationContext(sc);
        } else {
            ctx = new ClassPathXmlApplicationContext(new String[] {"applicationContext.xml"});
        }
        if(ctx == null) {
            LOGGER.severe("failed to get Spring context, jersey-spring init skipped");
            return;
        }
        LOGGER.fine("Spring context initialized");

        // initialize HK2 spring-bridge
        SpringBridge.getSpringBridge().initializeSpringBridge(locator);
        SpringIntoHK2Bridge springBridge = locator.getService(SpringIntoHK2Bridge.class);
        springBridge.bridgeSpringBeanFactory(ctx);

        // register Spring @Autowired annotation handler with HK2 ServiceLocator
        ServiceLocatorUtilities.addOneConstant(locator, new AutowiredInjectResolver(ctx));

        ServiceLocatorUtilities.addOneConstant(locator, ctx, "SpringContext", ApplicationContext.class);
        LOGGER.info("jersey-spring initialized");
    }

    // detect JAX-RS classes that are also Spring @Components.
    // register these with HK2 ServiceLocator to manage their lifecycle using Spring.
    @Override
    public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {
        if(component.isAnnotationPresent(Component.class)) {
            DynamicConfiguration c = Injections.getConfiguration(locator);
            String[] beanNames = ctx.getBeanNamesForType(component);
            if(beanNames == null || beanNames.length != 1) {
                LOGGER.severe(String.format("none or multiple beans found in Spring context of type %s, skipping", component));
                return false;
            }
            String beanName = beanNames[0];

            ServiceBindingBuilder bb = Injections.newFactoryBinder(new SpringComponentProvider.SpringManagedBeanFactory(ctx, locator, beanName));
            bb.to(component);
            Injections.addBinding(bb, c);
            c.commit();
            LOGGER.fine(String.format("Spring managed bean '%s' registered with HK2", beanName));

            return true;
        }
        return false;
    }

    @Override
    public void done() {
        LOGGER.fine("done()");
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
}
