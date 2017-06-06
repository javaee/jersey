/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.inject.cdi.se.bean;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.glassfish.jersey.inject.cdi.se.injector.CachedConstructorAnalyzer;
import org.glassfish.jersey.inject.cdi.se.injector.InjectionUtils;
import org.glassfish.jersey.inject.cdi.se.injector.JerseyConstructorInjectionPoint;
import org.glassfish.jersey.inject.cdi.se.injector.JerseyInjectionTarget;
import org.glassfish.jersey.inject.cdi.se.injector.WrappingJerseyInjectionTarget;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.PerThread;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedConstructor;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.enhanced.jlr.ConstructorSignatureImpl;
import org.jboss.weld.annotated.enhanced.jlr.EnhancedAnnotatedTypeImpl;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.injection.ConstructorInjectionPoint;
import org.jboss.weld.injection.producer.AbstractInstantiator;
import org.jboss.weld.injection.producer.BasicInjectionTarget;
import org.jboss.weld.injection.producer.BeanInjectionTarget;
import org.jboss.weld.injection.producer.InjectionTargetService;
import org.jboss.weld.injection.producer.Instantiator;
import org.jboss.weld.injection.producer.NonProducibleInjectionTarget;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

/**
 * Helper class to register a {@link Bean} into CDI {@link BeanManager}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class BeanHelper {

    /**
     * Forbids the creation of {@link BeanHelper} instance.
     */
    private BeanHelper() {
    }

    /**
     * Registers an instance as {@link JerseyBean} into {@link BeanManager}.
     *
     * @param binding   object containing {@link javax.enterprise.inject.spi.BeanAttributes} information.
     * @param abd       {@link AfterBeanDiscovery} event.
     * @param resolvers all registered injection resolvers.
     * @param <T>       type of the instance which is registered.
     */
    public static <T> void registerBean(InstanceBinding<T> binding, AfterBeanDiscovery abd, List<InjectionResolver> resolvers) {
        InstanceBean<T> bean = new InstanceBean<>(binding);
        /*
         * Wrap into custom injection target that is able to inject the additional @Inject, @Context, @*Param fields into
         * the given service.
         */
        InjectionTarget<T> injectionTarget = new WrappingJerseyInjectionTarget<>(bean, resolvers);
        bean.setInjectionTarget(injectionTarget);
        abd.addBean(bean);
    }

    /**
     * Registers a class as {@link JerseyBean} into {@link BeanManager}.
     *
     * @param binding     object containing {@link javax.enterprise.inject.spi.BeanAttributes} information.
     * @param abd         {@link AfterBeanDiscovery} event.
     * @param resolvers   all registered injection resolvers.
     * @param beanManager currently used bean manager.
     * @param <T>         type of the class which is registered.
     */
    public static <T> void registerBean(ClassBinding<T> binding, AfterBeanDiscovery abd, Collection<InjectionResolver> resolvers,
            BeanManager beanManager) {
        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(binding.getService());
        InjectionTarget<T> injectionTarget = beanManager.createInjectionTarget(annotatedType);

        ClassBean<T> bean = new ClassBean<>(binding);
        bean.setInjectionTarget(getJerseyInjectionTarget(binding.getService(), injectionTarget, bean, resolvers));
        abd.addBean(bean);
    }

    /**
     * Registers an instance supplier and its provided value as {@link JerseyBean}s into {@link BeanManager}.
     *
     * @param binding object containing {@link javax.enterprise.inject.spi.BeanAttributes} information.
     * @param abd     {@link AfterBeanDiscovery} event.
     * @param <T>     type of the instance which is registered.
     */
    public static <T> void registerSupplier(SupplierInstanceBinding<T> binding, AfterBeanDiscovery abd, BeanManager beanManager) {
        BeanManagerImpl manager;
        if (beanManager instanceof BeanManagerProxy) {
            manager = ((BeanManagerProxy) beanManager).unwrap();
        } else {
            manager = (BeanManagerImpl) beanManager;
        }

        /*
         * CDI does not provide sufficient support for ThreadScoped Supplier
         */
        if (binding.getScope() == PerThread.class) {
            abd.addBean(new SupplierThreadScopeBean(binding, manager));
        } else {
            abd.addBean(new SupplierInstanceBean<>(binding));
            abd.addBean(new SupplierInstanceBeanBridge<>(binding));
        }
    }

    /**
     * Registers a class supplier and its provided value as {@link JerseyBean}s into {@link BeanManager}.
     *
     * @param binding     object containing {@link javax.enterprise.inject.spi.BeanAttributes} information.
     * @param abd         {@link AfterBeanDiscovery} event.
     * @param resolvers   all registered injection resolvers.
     * @param beanManager currently used bean manager.
     * @param <T>         type of the class which is registered.
     */
    @SuppressWarnings("unchecked")
    public static <T> void registerSupplier(SupplierClassBinding<T> binding, AfterBeanDiscovery abd,
            Collection<InjectionResolver> resolvers, BeanManager beanManager) {

        Class<Supplier<T>> supplierClass = (Class<Supplier<T>>) binding.getSupplierClass();
        AnnotatedType<Supplier<T>> annotatedType = beanManager.createAnnotatedType(supplierClass);
        InjectionTarget<Supplier<T>> injectionTarget = beanManager.createInjectionTarget(annotatedType);

        SupplierClassBean<T> supplierBean = new SupplierClassBean<>(binding);
        InjectionTarget<Supplier<T>> jit = getJerseyInjectionTarget(supplierClass, injectionTarget, supplierBean, resolvers);
        supplierBean.setInjectionTarget(jit);

        abd.addBean(supplierBean);
        abd.addBean(new SupplierBeanBridge(binding, beanManager));
    }

    private static <T> InjectionTarget<T> getJerseyInjectionTarget(Class<T> clazz, InjectionTarget<T> injectionTarget,
            Bean<T> bean, Collection<InjectionResolver> resolvers) {
        BasicInjectionTarget<T> it = (BasicInjectionTarget<T>) injectionTarget;

        /*
         * Looks at whether the DefaultInstantiator resolving a valid constructor does not met this case:
         * - No constructor with @Inject annotation is defined
         * - NoArgs constructor is defined
         * - Instantiator ignores JAX-RS valid constructor with multiple params
         */
        boolean noArgConstructor = isNoArgConstructorCase(it, clazz);

        JerseyInjectionTarget<T> jit;
        /*
         * CDI is able to find a constructor that means that the class contains only one constructor of this type:
         * - default constructor
         * - non-argument constructor
         * - multi-param constructor annotated by @Inject annotation and able to inject all parameters.
         */
        if (!noArgConstructor && injectionTarget instanceof BeanInjectionTarget) {
            jit = new JerseyInjectionTarget<>(it, bean, clazz, resolvers);

        /*
         * CDI failed during the looking for a proper constructor because of these reasons:
         * - multi-param constructor not annotated by @Inject annotation
         * - multiple constructors annotated by @Inject annotation
         * - is not able to satisfied single constructor annotated by @Inject annotation
         *
         * Therefore produced NonProducibleInjectionTarget cannot create and instance, we try to find the proper constructor
         * using JAX-RS rules:
         * - largest constructor with all annotated parameters
         *
         * If JAX-RS valid constructor is not find - InjectionException is thrown
         */
        } else if (noArgConstructor || injectionTarget instanceof NonProducibleInjectionTarget) {
            CachedConstructorAnalyzer<T> analyzer =
                    new CachedConstructorAnalyzer<>(clazz, InjectionUtils.getInjectAnnotations(resolvers));

            /*
             * Contains the analyzed class any constructor that can be injected by Jersey?
             */
            if (analyzer.hasCompatibleConstructor()) {
                EnhancedAnnotatedConstructor<T> constructor = createEnhancedAnnotatedType(it)
                        .getDeclaredEnhancedConstructor(new ConstructorSignatureImpl(analyzer.getConstructor()));

                JerseyConstructorInjectionPoint<T> constructorInjectionPoint =
                        new JerseyConstructorInjectionPoint<>(constructor, bean, it.getBeanManager(), resolvers);

                Instantiator<T> instantiator = new JerseyInstantiator<>(constructorInjectionPoint);
                jit = new JerseyInjectionTarget<>(createEnhancedAnnotatedType(it), it, bean, clazz, resolvers, instantiator);

            /*
             * Instance of this class cannot be created neither CDI nor Jersey therefore mark it as non-producible.
             */
            } else {
                return new WrappingJerseyInjectionTarget<>(it, bean, resolvers);
            }
        } else {
            throw new RuntimeException("Unknown InjectionTarget for the class: " + clazz.getTypeName());
        }

        InjectionTargetService injectionTargetService = it.getBeanManager().getServices().get(InjectionTargetService.class);
        injectionTargetService.addInjectionTargetToBeInitialized(jit.getEnhancedAnnotatedType(), jit);
        return jit;
    }

    public static <T> EnhancedAnnotatedType<T> createEnhancedAnnotatedType(BasicInjectionTarget<T> it) {
        return EnhancedAnnotatedTypeImpl.of(
                (SlimAnnotatedType<T>) it.getAnnotatedType(), ClassTransformer.instance(it.getBeanManager()));
    }

    /**
     * Looks at whether the DefaultInstantiator resolving a valid constructor does not met this case:
     * - No constructor with @Inject annotation is defined
     * - NoArgs constructor is defined
     * - Instantiator ignores JAX-RS valid constructor with multiple params
     *
     * @param it    injection target containing instantiator with resolved constructor.
     * @param clazz class which analyzed constructor belongs to.
     * @param <T>   type of the analyzed class.
     * @return {@code true} if no-arg constructor was selected while multi-params constructor exists.
     */
    private static <T> boolean isNoArgConstructorCase(BasicInjectionTarget<T> it, Class<T> clazz) {
        if (!(it instanceof NonProducibleInjectionTarget)) {
            Instantiator<T> instantiator = it.getInstantiator();
            Constructor<T> constructor = instantiator.getConstructor();
            return constructor.getParameterCount() == 0 && clazz.getConstructors().length > 1;
        }

        return false;
    }

    /**
     * Wrapper class to provide Jersey implementation of {@link Instantiator} interface.
     *
     * @param <T> type which is created by instantiator.
     */
    private static class JerseyInstantiator<T> extends AbstractInstantiator<T> {

        private final ConstructorInjectionPoint<T> injectionPoint;

        private JerseyInstantiator(ConstructorInjectionPoint<T> injectionPoint) {
            this.injectionPoint = injectionPoint;
        }

        @Override
        public ConstructorInjectionPoint<T> getConstructorInjectionPoint() {
            return injectionPoint;
        }

        @Override
        public Constructor<T> getConstructor() {
            return injectionPoint.getAnnotated().getJavaMember();
        }

        @Override
        public String toString() {
            return "JerseyInstantiator [constructor=" + injectionPoint.getMember() + "]";
        }

        @Override
        public boolean hasInterceptorSupport() {
            return false;
        }

        @Override
        public boolean hasDecoratorSupport() {
            return false;
        }
    }
}
