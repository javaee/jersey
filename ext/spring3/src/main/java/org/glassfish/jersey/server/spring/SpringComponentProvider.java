/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.server.spring;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.glassfish.jersey.internal.inject.SupplierFactory;
import org.glassfish.jersey.hk2.HK2InstanceManager;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.spi.inject.AbstractBinder;
import org.glassfish.jersey.spi.inject.Binder;
import org.glassfish.jersey.spi.inject.Descriptor;
import org.glassfish.jersey.spi.inject.Descriptors;
import org.glassfish.jersey.spi.inject.InstanceManager;

import org.glassfish.hk2.api.InjectionResolver;

import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;

import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Custom ComponentProvider class.
 * Responsible for 1) bootstrapping Jersey 2 Spring integration and
 * 2) making Jersey skip JAX-RS Spring component life-cycle management and leave it to us.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class SpringComponentProvider implements ComponentProvider {

    private static final Logger LOGGER = Logger.getLogger(SpringComponentProvider.class.getName());
    private static final String DEFAULT_CONTEXT_CONFIG_LOCATION = "applicationContext.xml";
    private static final String PARAM_CONTEXT_CONFIG_LOCATION = "contextConfigLocation";
    private static final String PARAM_SPRING_CONTEXT = "contextConfig";

    private volatile InstanceManager instanceManager;
    private volatile ApplicationContext ctx;

    @Override
    public void initialize(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(LocalizationMessages.CTX_LOOKUP_STARTED());
        }

        ServletContext sc = instanceManager.getInstance(ServletContext.class);

        if (sc != null) {
            // servlet container
            ctx = WebApplicationContextUtils.getWebApplicationContext(sc);
        } else {
            // non-servlet container
            ctx = createSpringContext();
        }
        if (ctx == null) {
            LOGGER.severe(LocalizationMessages.CTX_LOOKUP_FAILED());
            return;
        }
        LOGGER.config(LocalizationMessages.CTX_LOOKUP_SUCESSFUL());

        // initialize HK2 spring-bridge

        HK2InstanceManager hk2InstanceManager = (HK2InstanceManager) instanceManager;
        SpringBridge.getSpringBridge().initializeSpringBridge(hk2InstanceManager.getServiceLocator());
        SpringIntoHK2Bridge springBridge = instanceManager.getInstance(SpringIntoHK2Bridge.class);
        springBridge.bridgeSpringBeanFactory(ctx);

        // register Spring @Autowired annotation handler with HK2 ServiceLocator
        Binder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new AutowiredInjectResolver(ctx))
                        .to(InjectionResolver.class);

                bind(ctx)
                        .to(ApplicationContext.class)
                        .named("SpringContext");
            }
        };

        instanceManager.register(binder);
        LOGGER.config(LocalizationMessages.SPRING_COMPONENT_PROVIDER_INITIALIZED());
    }

    // detect JAX-RS classes that are also Spring @Components.
    // register these with HK2 ServiceLocator to manage their lifecycle using Spring.
    @Override
    public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {

        if (ctx == null) {
            return false;
        }

        if (AnnotationUtils.findAnnotation(component, Component.class) != null) {
            String[] beanNames = ctx.getBeanNamesForType(component);
            if (beanNames == null || beanNames.length != 1) {
                LOGGER.severe(LocalizationMessages.NONE_OR_MULTIPLE_BEANS_AVAILABLE(component));
                return false;
            }
            String beanName = beanNames[0];

            Descriptor descriptor = Descriptors.factory(new SpringManagedBeanFactory(ctx, instanceManager, beanName))
                    .to(component)
                    .to(providerContracts);
            instanceManager.register(descriptor);

            LOGGER.config(LocalizationMessages.BEAN_REGISTERED(beanNames[0]));
            return true;
        }
        return false;
    }

    @Override
    public void done() {
    }

    private ApplicationContext createSpringContext() {
        ApplicationHandler applicationHandler = instanceManager.getInstance(ApplicationHandler.class);
        ApplicationContext springContext = (ApplicationContext) applicationHandler.getConfiguration()
                .getProperty(PARAM_SPRING_CONTEXT);
        if (springContext == null) {
            String contextConfigLocation = (String) applicationHandler.getConfiguration()
                    .getProperty(PARAM_CONTEXT_CONFIG_LOCATION);
            springContext = createXmlSpringConfiguration(contextConfigLocation);
        }
        return springContext;
    }

    private ApplicationContext createXmlSpringConfiguration(String contextConfigLocation) {
        if (contextConfigLocation == null) {
            contextConfigLocation = DEFAULT_CONTEXT_CONFIG_LOCATION;
        }
        return ctx = new ClassPathXmlApplicationContext(contextConfigLocation, "jersey-spring-applicationContext.xml");
    }

    private static class SpringManagedBeanFactory extends SupplierFactory {

        private final ApplicationContext ctx;
        private final InstanceManager instanceManager;
        private final String beanName;

        private SpringManagedBeanFactory(ApplicationContext ctx, InstanceManager instanceManager, String beanName) {
            this.ctx = ctx;
            this.instanceManager = instanceManager;
            this.beanName = beanName;
        }

        @Override
        public Object provide() {
            Object bean = ctx.getBean(beanName);
            if (bean instanceof Advised) {
                try {
                    // Unwrap the bean and inject the values inside of it
                    Object localBean = ((Advised) bean).getTargetSource().getTarget();
                    instanceManager.inject(localBean);
                } catch (Exception e) {
                    // Ignore and let the injection happen as it normally would.
                    instanceManager.inject(bean);
                }
            } else {
                instanceManager.inject(bean);
            }
            return bean;
        }
    }
}
