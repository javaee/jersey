/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.gf.ejb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.jersey.internal.inject.Utilities;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.spi.ComponentProvider;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;

/**
 * EJB component provider.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public final class EJBComponentProvider implements ComponentProvider {

    private static final Logger LOGGER = Logger.getLogger(
            EJBComponentProvider.class.getName());

    private boolean ejbInterceptorRegistered = false;

    /**
     * HK2 factory to provide EJB components obtained via JNDI lookup.
     */
    private static class EjbFactory<T> implements Factory<T> {

        final InitialContext ctx;
        final Class<T> clazz;

        @SuppressWarnings("unchecked")
        @Override
        public T provide() {
            try {
                return (T) lookup(ctx, clazz, clazz.getSimpleName());
            } catch (NamingException ex) {
                Logger.getLogger(ApplicationHandler.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        @Override
        public void dispose(T instance) {
            // do nothing
        }

        public EjbFactory(Class<T> rawType, InitialContext ctx) {
            this.clazz = rawType;
            this.ctx = ctx;
        }
    }

    /**
     * Annotations to determine EJB components.
     */
    private static final Set<String> EjbComponentAnnotations = Collections.unmodifiableSet(new HashSet<String>() {{
        add("javax.ejb.Stateful");
        add("javax.ejb.Stateless");
        add("javax.ejb.Singleton");
    }});

    private ServiceLocator locator = null;

    // ComponentProvider
    @Override
    public void initialize(final ServiceLocator locator) {
        this.locator = locator;
    }

    private void registerEjbInterceptor() {
        try {
            InitialContext ic = getInitialContext();
            if (ic == null) {
                throw new IllegalStateException(LocalizationMessages.INITIAL_CONTEXT_NOT_FOUND());
            }
            Object interceptorBinder = ic.lookup("java:org.glassfish.ejb.container.interceptor_binding_spi");
            // Some implementations of InitialContext return null instead of
            // throwing NamingException if there is no Object associated with
            // the name
            if (interceptorBinder == null) {
                throw new IllegalStateException(LocalizationMessages.EJB_INTERCEPTOR_BIND_API_NOT_AVAILABLE());
            }

            Method interceptorBinderMethod = interceptorBinder.getClass().
                    getMethod("registerInterceptor", java.lang.Object.class);

            try {

                // create an interceptor instance via reflection
                final Class<?> interceptorClass = Class.forName(EJBComponentProvider.class.getPackage().getName() + ".EjbComponentInterceptor");
                final Object interceptor = interceptorClass.getConstructor(ServiceLocator.class).newInstance(locator);

                interceptorBinderMethod.invoke(interceptorBinder, interceptor);

                this.ejbInterceptorRegistered = true;
                LOGGER.log(Level.INFO, LocalizationMessages.EJB_INTERCEPTOR_BOUND());
            } catch (Exception ex) {
                throw new IllegalStateException(LocalizationMessages.EJB_INTERCEPTOR_CONFIG_ERROR(), ex);
            }

        } catch (NamingException ex) {
            throw new IllegalStateException(LocalizationMessages.EJB_INTERCEPTOR_BIND_API_NOT_AVAILABLE(), ex);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(LocalizationMessages.EJB_INTERCEPTOR_BIND_API_NON_CONFORMANT(), ex);
        } catch (SecurityException ex) {
            throw new IllegalStateException(LocalizationMessages.EJB_INTERCEPTOR_CONFIG_SECURITY_ERROR(), ex);
        } catch (LinkageError ex) {
            throw new IllegalStateException(LocalizationMessages.EJB_INTERCEPTOR_CONFIG_LINKAGE_ERROR(), ex);
        }
    }

    // ComponentProvider
    @Override
    public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {

        if (locator == null) {
            throw new IllegalStateException(LocalizationMessages.EJB_COMPONENT_PROVIDER_NOT_INITIALIZED_PROPERLY());
        }

        if (!isEjbComponent(component)) {
            return false;
        }

        if (!ejbInterceptorRegistered) {
            registerEjbInterceptor();
        }

        try {
            DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
            DynamicConfiguration dc = dcs.createDynamicConfiguration();

            //noinspection unchecked
            Utilities.createConstantFactoryDescriptor(
                    new EjbFactory(component, new InitialContext()),
                    PerLookup.class,
                    null, null, null,
                    providerContracts.toArray(new Type[providerContracts.size()]));

            dc.commit();
            return true;
        } catch (NamingException ex) {
            Logger.getLogger(ApplicationHandler.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    @Override
    public void done() {
        if (ejbInterceptorRegistered) {
            DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
            DynamicConfiguration dc = dcs.createDynamicConfiguration();

            dc.bind(BuilderHelper.link(EJBExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class).build());
            dc.commit();
        }
    }

    private boolean isEjbComponent(Class<?> component) {
        for (Annotation a : component.getAnnotations()) {
            if (EjbComponentAnnotations.contains(a.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }


    private static InitialContext getInitialContext() {
        try {
            // Deployment on Google App Engine will
            // result in a LinkageError
            return new InitialContext();
        } catch (NamingException ex) {
            return null;
        } catch (LinkageError ex) {
            return null;
        }
    }

    private static Object lookup(InitialContext ic, Class<?> c, String name) throws NamingException {
        try {
            return lookupSimpleForm(ic, name);
        } catch (NamingException ex) {
            LOGGER.log(Level.WARNING, LocalizationMessages.EJB_CLASS_SIMPLE_LOOKUP_FAILED(c.getName()), ex);

            return lookupFullyQualifiedForm(ic, c, name);
        }
    }

    private static Object lookupSimpleForm(InitialContext ic, String name) throws NamingException {
        String jndiName = "java:module/" + name;
        return ic.lookup(jndiName);
    }

    private static Object lookupFullyQualifiedForm(InitialContext ic, Class<?> c, String name) throws NamingException {
        String jndiName = "java:module/" + name + "!" + c.getName();
        return ic.lookup(jndiName);
    }
}
