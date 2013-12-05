/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.gf.ejb.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ext.ExceptionMapper;

import javax.annotation.Priority;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;

/**
 * EJB component provider.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Priority(300)
@SuppressWarnings("UnusedDeclaration")
public final class EjbComponentProvider implements ComponentProvider, ResourceMethodInvocationHandlerProvider {

    private static final Logger LOGGER = Logger.getLogger(
            EjbComponentProvider.class.getName());

    private InitialContext initialContext;
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
        final DynamicConfiguration configuration = Injections.getConfiguration(locator);
        Injections.addBinding(Injections.newBinder(this).to(ResourceMethodInvocationHandlerProvider.class), configuration);
        configuration.commit();
    }

    private void registerEjbInterceptor() {
        try {
            initialContext = getInitialContext();
            Object interceptorBinder = initialContext.lookup("java:org.glassfish.ejb.container.interceptor_binding_spi");
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
                final Class<?> interceptorClass =
                        Class.forName(EjbComponentProvider.class.getPackage().getName() + ".EjbComponentInterceptor");
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
    @SuppressWarnings("unchecked")
    @Override
    public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(LocalizationMessages.EJB_CLASS_BEING_CHECKED(component));
        }

        if (locator == null) {
            throw new IllegalStateException(LocalizationMessages.EJB_COMPONENT_PROVIDER_NOT_INITIALIZED_PROPERLY());
        }

        if (!isEjbComponent(component)) {
            return false;
        }

        if (!ejbInterceptorRegistered) {
            registerEjbInterceptor();
        }

        DynamicConfiguration dc = Injections.getConfiguration(locator);

        final ServiceBindingBuilder bindingBuilder = Injections.newFactoryBinder(new EjbFactory(component, initialContext));

        bindingBuilder.to(component);
        for (Class contract : providerContracts) {
            bindingBuilder.to(contract);
        }

        Injections.addBinding(bindingBuilder, dc);

        dc.commit();

        if (LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config(LocalizationMessages.EJB_CLASS_BOUND_WITH_CDI(component));
        }

        return true;
    }

    @Override
    public void done() {
        registerEjbExceptionMapper();
    }

    private void registerEjbExceptionMapper() {
        final DynamicConfiguration dc = Injections.getConfiguration(locator);
        Injections.addBinding(Injections.newBinder(EjbExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class), dc);
        dc.commit();
    }

    private boolean isEjbComponent(Class<?> component) {
        for (Annotation a : component.getAnnotations()) {
            if (EjbComponentAnnotations.contains(a.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InvocationHandler create(Invocable method) {

        final Class<?> resourceClass = method.getHandler().getHandlerClass();

        if (resourceClass == null || !isEjbComponent(resourceClass)) {
            return null;
        }

        final Method handlingMethod = method.getDefinitionMethod();

        for (Class iFace : remoteAndLocalIfaces(resourceClass)) {
            try {
                final Method iFaceMethod = iFace.getDeclaredMethod(handlingMethod.getName(), handlingMethod.getParameterTypes());
                if (iFaceMethod != null) {
                    return new InvocationHandler() {
                        @Override
                        public Object invoke(Object target, Method ignored, Object[] args)
                                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                            return iFaceMethod.invoke(target, args);
                        }
                    };
                }
            } catch (NoSuchMethodException ex) {
                logLookupException(handlingMethod, resourceClass, iFace, ex);
            } catch (SecurityException ex) {
                logLookupException(handlingMethod, resourceClass, iFace, ex);
            }
        }
        return null;
    }

    private void logLookupException(final Method method, final Class<?> component, Class<?> iFace, Exception ex) {
        LOGGER.log(
                Level.WARNING,
                LocalizationMessages.EJB_INTERFACE_HANDLING_METHOD_LOOKUP_EXCEPTION(method, component, iFace), ex);
    }

    private List<Class> remoteAndLocalIfaces(final Class<?> resourceClass) {
        final List<Class> allLocalOrRemoteIfaces = new LinkedList<Class>();
        if (resourceClass.isAnnotationPresent(Remote.class)) {
            allLocalOrRemoteIfaces.addAll(Arrays.asList(resourceClass.getAnnotation(Remote.class).value()));
        }
        if (resourceClass.isAnnotationPresent(Local.class)) {
            allLocalOrRemoteIfaces.addAll(Arrays.asList(resourceClass.getAnnotation(Local.class).value()));
        }
        for (Class<?> i : resourceClass.getInterfaces()) {
            if (i.isAnnotationPresent(Remote.class) || i.isAnnotationPresent(Local.class)) {
                allLocalOrRemoteIfaces.add(i);
            }
        }
        return allLocalOrRemoteIfaces;
    }

    private static InitialContext getInitialContext() {
        try {
            // Deployment on Google App Engine will
            // result in a LinkageError
            return new InitialContext();
        } catch (Exception ex) {
            throw new IllegalStateException(LocalizationMessages.INITIAL_CONTEXT_NOT_AVAILABLE(), ex);
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
