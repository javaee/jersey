/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.gf.cdi.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ExceptionMapper;

import javax.annotation.ManagedBean;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transactional;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.ScopedBindingBuilder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.hk2.utilities.cache.Cache;
import org.glassfish.hk2.utilities.cache.Computable;

/**
 * Jersey CDI integration implementation.
 * Implements {@link ComponentProvider Jersey component provider} to serve CDI beans
 * obtained from the actual CDI bean manager.
 * To properly inject JAX-RS/Jersey managed beans into CDI, it also
 * serves as a {@link Extension CDI Extension}, that intercepts CDI injection targets.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Priority(200)
public class CdiComponentProvider implements ComponentProvider, Extension {

    private static final Logger LOGGER = Logger.getLogger(CdiComponentProvider.class.getName());

    /**
     * Name to be used when binding CDI injectee skipping class analyzer to HK2 service locator.
     */
    public static final String CDI_CLASS_ANALYZER = "CdiInjecteeSkippingClassAnalyzer";

    static final String TRANSACTIONAL_WAE = "org.glassfish.jersey.cdi.transactional.wae";

    private ServiceLocator locator;
    private BeanManager beanManager;

    private Map<Class<?>, Set<Method>> methodsToSkip = new HashMap<Class<?>, Set<Method>>();
    private Map<Class<?>, Set<Field>> fieldsToSkip = new HashMap<Class<?>, Set<Field>>();

    /**
     * HK2 factory to provide CDI components obtained from CDI bean manager.
     * The factory handles CDI managed components as well as non-contextual managed beans.
     */
    private static class CdiFactory<T> implements Factory<T> {

        private interface InstanceManager<T> {
            /**
             * Get me correctly instantiated and injected instance.
             *
             * @param clazz type of the component to instantiate.
             * @return injected component instance.
             */
            T getInstance(Class<T> clazz);

            /**
             * Do whatever needs to be done before given instance is destroyed.
             *
             * @param instance to be destroyed.
             */
            void preDestroy(T instance);
        }

        final Class<T> clazz;
        final BeanManager beanManager;
        final ServiceLocator locator;
        final InstanceManager<T> referenceProvider;
        final Annotation[] qualifiers;

        @SuppressWarnings("unchecked")
        @Override
        public T provide() {
            final T instance = referenceProvider.getInstance(clazz);
            if (instance != null) {
                return instance;
            }
            throw new NoSuchElementException(LocalizationMessages.CDI_LOOKUP_FAILED(clazz));
        }

        @Override
        public void dispose(final T instance) {
            referenceProvider.preDestroy(instance);
        }

        /**
         * Create new factory instance for given type and bean manager.
         *
         * @param rawType     type of the components to provide.
         * @param locator     actual HK2 service locator instance
         * @param beanManager current bean manager to get references from.
         * @param cdiManaged  set to true if the component should be managed by CDI
         */
        CdiFactory(final Class<T> rawType, final ServiceLocator locator, final BeanManager beanManager, final boolean cdiManaged) {
            this.clazz = rawType;
            this.qualifiers = getQualifiers(clazz.getAnnotations());
            this.beanManager = beanManager;
            this.locator = locator;
            this.referenceProvider = cdiManaged ? new InstanceManager<T>() {
                @Override
                public T getInstance(final Class<T> clazz) {

                    final Set<Bean<?>> beans = beanManager.getBeans(clazz, qualifiers);
                    for (final Bean b : beans) {
                        final Object instance = beanManager.getReference(b, clazz, beanManager.createCreationalContext(b));
                        return (T) instance;
                    }
                    return null;
                }

                @Override
                public void preDestroy(final T instance) {
                    // do nothing
                }


            } : new InstanceManager<T>() {

                final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
                final InjectionTarget<T> injectionTarget = beanManager.createInjectionTarget(annotatedType);
                final CreationalContext creationalContext = beanManager.createCreationalContext(null);

                @Override
                public T getInstance(final Class<T> clazz) {
                    final T instance = injectionTarget.produce(creationalContext);
                    injectionTarget.inject(instance, creationalContext);
                    if (locator != null) {
                        locator.inject(instance, CDI_CLASS_ANALYZER);
                    }
                    injectionTarget.postConstruct(instance);

                    return instance;
                }

                @Override
                public void preDestroy(final T instance) {
                    injectionTarget.preDestroy(instance);
                }
            };
        }
    }

    @Override
    public void initialize(final ServiceLocator locator) {

        this.locator = locator;

        beanManager = beanManagerFromJndi();
        if (beanManager != null) {
            final CdiComponentProvider extension = beanManager.getExtension(this.getClass());
            if (extension != null) {
                extension.locator = this.locator;
                this.fieldsToSkip = extension.fieldsToSkip;
                this.methodsToSkip = extension.methodsToSkip;
                LOGGER.config(LocalizationMessages.CDI_PROVIDER_INITIALIZED());
            }
        }
    }

    /**
     * CDI producer for CDI bean constructor String parameters, that should be injected by JAX-RS.
     */
    @ApplicationScoped
    public static class JaxRsParamProducer {

        /**
         * Internal cache to store CDI {@link InjectionPoint} to Jersey {@link Parameter} mapping.
         */
        final Cache<InjectionPoint, Parameter> parameterCache = new Cache<InjectionPoint, Parameter>(new Computable<InjectionPoint, Parameter>() {

            @Override
            public Parameter compute(final InjectionPoint injectionPoint) {
                final Annotated annotated = injectionPoint.getAnnotated();
                final Class<?> clazz = injectionPoint.getMember().getDeclaringClass();

                if (annotated instanceof AnnotatedParameter) {

                    final AnnotatedParameter annotatedParameter = (AnnotatedParameter) annotated;
                    final AnnotatedCallable callable = annotatedParameter.getDeclaringCallable();

                    if (callable instanceof AnnotatedConstructor) {

                        final AnnotatedConstructor ac = (AnnotatedConstructor) callable;
                        final int position = annotatedParameter.getPosition();
                        final List<Parameter> parameters = Parameter.create(clazz, clazz, ac.getJavaMember(), false);

                        return parameters.get(position);
                    }
                }

                return null;
            }
        });

        /**
         * Provide a String value for given injection point. If the injection point does not refer
         * to a CDI bean constructor parameter, or the value could not be found, the method will return null.
         *
         * @param injectionPoint actual injection point.
         * @param beanManager    current application bean manager.
         * @return String value for given injection point.
         */
        @javax.enterprise.inject.Produces
        public String getParameterValue(final InjectionPoint injectionPoint, final BeanManager beanManager) {

            final Parameter parameter = parameterCache.compute(injectionPoint);

            if (parameter != null) {

                final ServiceLocator locator = beanManager.getExtension(CdiComponentProvider.class).locator;

                final Set<ValueFactoryProvider> providers = Providers.getProviders(locator, ValueFactoryProvider.class);

                for (final ValueFactoryProvider vfp : providers) {
                    final Factory<?> valueFactory = vfp.getValueFactory(parameter);
                    if (valueFactory != null) {
                        return (String) valueFactory.provide();
                    }
                }
            }

            return null;
        }
    }

    @Override
    public boolean bind(final Class<?> clazz, final Set<Class<?>> providerContracts) {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(LocalizationMessages.CDI_CLASS_BEING_CHECKED(clazz));
        }

        if (beanManager == null) {
            return false;
        }

        final boolean isCdiManaged = isCdiComponent(clazz);
        final boolean isManagedBean = isManagedBean(clazz);
        final boolean isJaxRsComponent = isJaxRsComponentType(clazz);

        if (!isCdiManaged && !isManagedBean && !isJaxRsComponent) {
            return false;
        }

        final DynamicConfiguration dc = Injections.getConfiguration(locator);

        final ServiceBindingBuilder bindingBuilder =
                Injections.newFactoryBinder(new CdiFactory(clazz, locator, beanManager, isCdiManaged));

        bindingBuilder.to(clazz);
        for (final Class contract : providerContracts) {
            bindingBuilder.to(contract);
        }

        Injections.addBinding(bindingBuilder, dc);

        dc.commit();

        if (LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config(LocalizationMessages.CDI_CLASS_BOUND_WITH_CDI(clazz));
        }

        return true;
    }

    @Override
    public void done() {
        bindHk2ClassAnalyzer();
        bindWaeRestoringExceptionMapper();
    }

    private void bindWaeRestoringExceptionMapper() {
        final DynamicConfiguration dc = Injections.getConfiguration(locator);
        Injections.addBinding(Injections.newBinder(TransactionalExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class), dc);
        dc.commit();
    }

    private boolean isCdiComponent(final Class<?> component) {
        final Annotation[] qualifiers = getQualifiers(component.getAnnotations());
        return !beanManager.getBeans(component, qualifiers).isEmpty();
    }

    private static Annotation[] getQualifiers(final Annotation[] annotations) {
        final List<Annotation> result = new ArrayList<Annotation>(annotations.length);
        for (final Annotation a : annotations) {
            if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                result.add(a);
            }
        }
        return result.toArray(new Annotation[result.size()]);
    }

    private boolean isManagedBean(final Class<?> component) {
        return component.isAnnotationPresent(ManagedBean.class);
    }

    @SuppressWarnings("unused")
    private void afterTypeDiscovery(@Observes final AfterTypeDiscovery afterTypeDiscovery) {
        final List<Class<?>> interceptors = afterTypeDiscovery.getInterceptors();
        interceptors.add(WebApplicationExceptionPreservingInterceptor.class);
    }

    @SuppressWarnings("unused")
    private void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(JaxRsParamProducer.class));
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(WebApplicationExceptionPreservingInterceptor.class));
    }

    /**
     * Transactional interceptor to help retain {@link WebApplicationException}
     * thrown by transactional beans.
     */
    @Priority(Interceptor.Priority.PLATFORM_BEFORE + 199)
    @Interceptor
    @Transactional
    public static final class WebApplicationExceptionPreservingInterceptor {

        @Inject
        BeanManager beanManager;

        @AroundInvoke
        public Object intercept(final InvocationContext ic) throws Exception {
            try {
                return ic.proceed();
            } catch (final WebApplicationException wae) {
                final CdiComponentProvider extension = beanManager.getExtension(CdiComponentProvider.class);
                final ContainerRequest jerseyRequest = extension.locator.getService(ContainerRequest.class);
                if (jerseyRequest != null) {
                    jerseyRequest.setProperty(TRANSACTIONAL_WAE, wae);
                }
                throw wae;
            }
        }
    }

    @SuppressWarnings("unused")
    private void processInjectionTarget(@Observes final ProcessInjectionTarget event) {
        final InjectionTarget it = event.getInjectionTarget();
        final Class<?> componentClass = event.getAnnotatedType().getJavaClass();

        final Set<InjectionPoint> injectionPoints = it.getInjectionPoints();

        for (final InjectionPoint injectionPoint : injectionPoints) {
            final Member member = injectionPoint.getMember();
            if (member instanceof Field) {
                addInjecteeToSkip(componentClass, fieldsToSkip, (Field) member);
            } else if (member instanceof Method) {
                addInjecteeToSkip(componentClass, methodsToSkip, (Method) member);
            }
        }

        if (isJerseyOrDependencyType(componentClass)) {
            event.setInjectionTarget(new CdiInjectionTarget(it) {

                @Override
                public Set getInjectionPoints() {
                    // Tell CDI to ignore Jersey (or it's dependencies) classes when injecting.
                    // CDI will not treat these classes as CDI beans (as they are not).
                    return Collections.emptySet();
                }
            });
        } else if (isJaxRsComponentType(componentClass)) {
            event.setInjectionTarget(new CdiInjectionTarget(it) {

                @Override
                public Set getInjectionPoints() {
                    // Inject CDI beans into JAX-RS resources/providers/application.
                    return it.getInjectionPoints();
                }
            });
        }
    }

    private <T> void addInjecteeToSkip(final Class<?> componentClass, final Map<Class<?>, Set<T>> toSkip, final T member) {
        if (!toSkip.containsKey(componentClass)) {
            toSkip.put(componentClass, new HashSet<T>());
        }
        toSkip.get(componentClass).add(member);
    }

    /**
     * Introspect given type to determine if it represents a JAX-RS component.
     *
     * @param clazz type to be introspected.
     * @return true if the type represents a JAX-RS component type.
     */
    /* package */
    static boolean isJaxRsComponentType(final Class<?> clazz) {
        return Application.class.isAssignableFrom(clazz) ||
                Providers.isJaxRsProvider(clazz) ||
                Resource.from(clazz) != null;
    }

    private static boolean isJerseyOrDependencyType(final Class<?> clazz) {
        final String pckg = clazz.getPackage().getName();

        // Ignore Jersey Examples from this check.
        return pckg.contains("org.glassfish.hk2")
                || pckg.contains("jersey.repackaged")
                || pckg.contains("org.jvnet.hk2")
                || (pckg.startsWith("org.glassfish.jersey")
                    && !pckg.startsWith("org.glassfish.jersey.examples")
                    && !pckg.startsWith("org.glassfish.jersey.tests"));
    }

    private static BeanManager beanManagerFromJndi() {
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            return (BeanManager) initialContext.lookup("java:comp/BeanManager");
        } catch (final Exception ex) {
            try {
                return CDI.current().getBeanManager();
            } catch (final Exception e) {
                LOGGER.config(LocalizationMessages.CDI_BEAN_MANAGER_JNDI_LOOKUP_FAILED());
                return null;
            }
        } finally {
            if (initialContext != null) {
                try {
                    initialContext.close();
                } catch (final NamingException ignored) {
                    // no-op
                }
            }
        }
    }

    private void bindHk2ClassAnalyzer() {

        final ClassAnalyzer defaultClassAnalyzer =
                locator.getService(ClassAnalyzer.class, ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME);

        final int skippedElements = methodsToSkip.size() + fieldsToSkip.size();

        final ClassAnalyzer customizedClassAnalyzer = skippedElements > 0
                ? new InjecteeSkippingAnalyzer(defaultClassAnalyzer, methodsToSkip, fieldsToSkip)
                : defaultClassAnalyzer;

        final DynamicConfiguration dc = Injections.getConfiguration(locator);

        final ScopedBindingBuilder bindingBuilder =
                Injections.newBinder(customizedClassAnalyzer);

        bindingBuilder.analyzeWith(ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME)
                .to(ClassAnalyzer.class)
                .named(CDI_CLASS_ANALYZER);

        Injections.addBinding(bindingBuilder, dc);

        dc.commit();
    }

    private abstract class CdiInjectionTarget implements InjectionTarget {

        private final InjectionTarget delegate;

        protected CdiInjectionTarget(final InjectionTarget delegate) {
            this.delegate = delegate;
        }

        @Override
        public void inject(final Object t, final CreationalContext cc) {
            delegate.inject(t, cc);
            if (locator != null) {
                locator.inject(t, CDI_CLASS_ANALYZER);
            }
        }

        @Override
        public void postConstruct(final Object t) {
            delegate.postConstruct(t);
        }

        @Override
        public void preDestroy(final Object t) {
            delegate.preDestroy(t);
        }

        @Override
        public Object produce(final CreationalContext cc) {
            return delegate.produce(cc);
        }

        @Override
        public void dispose(final Object t) {
            delegate.dispose(t);
        }

        @Override
        public abstract Set getInjectionPoints();
    }
}
