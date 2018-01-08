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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;

import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;

import org.jboss.weld.bean.StringBeanIdentifier;
import org.jboss.weld.bean.proxy.BeanInstance;
import org.jboss.weld.bean.proxy.ContextBeanInstance;
import org.jboss.weld.bean.proxy.ProxyFactory;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Creates an implementation of {@link javax.enterprise.inject.spi.Bean} interface using Jersey's {@link SupplierInstanceBinding}.
 * Binding provides the information about the bean also called {@link javax.enterprise.inject.spi.BeanAttributes} information.
 * The {@code Bean} does not use {@link org.glassfish.jersey.inject.cdi.se.injector.JerseyInjectionTarget} because serves already
 * created proxy, therefore the create operation just return provided instance without any other contextual operation
 * (produce, inject, destroy).
 * <p>
 * This bean is special and is used only for service registered as a {@link org.glassfish.jersey.internal.inject.PerThread} and
 * works through the proxy which serves the correct instance per the given thread.
 * <p>
 * Register example:
 * <pre>
 * AbstractBinder {
 *     &#64;Override
 *     protected void configure() {
 *         bindFactory(new MyFactoryInjectionProvider())
 *              .to(MyBean.class)
 *              .in(PerThread.class);
 *     }
 * }
 * </pre>
 * Inject example:
 * <pre>
 * &#64;Path("/")
 * public class MyResource {
 *   &#64;Inject
 *   private MyBean myBean&#59;
 * }
 * </pre>
 */
public class SupplierThreadScopeBean extends JerseyBean<Object> {

    private final ThreadScopeBeanInstance<Object> beanInstance;
    private final SupplierInstanceBinding binding;
    private final Object proxy;

    /**
     * Creates a new Jersey-specific {@link javax.enterprise.inject.spi.Bean} instance.
     *
     * @param binding {@link javax.enterprise.inject.spi.BeanAttributes} part of the bean.
     */
    @SuppressWarnings("unchecked")
    SupplierThreadScopeBean(SupplierInstanceBinding binding, BeanManagerImpl manager) {
        super(binding);
        this.binding = binding;
        this.beanInstance = new ThreadScopeBeanInstance<>(binding.getSupplier(), this, manager.getContextId());
        this.proxy = createClientProxy(beanInstance, manager.getContextId());
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public Object create(CreationalContext<Object> ctx) {
        return proxy;
    }

    @Override
    public void destroy(Object instance, CreationalContext<Object> creationalContext) {
        this.beanInstance.dispose();
    }

    @Override
    public Class<?> getBeanClass() {
        return (Class<?>) this.binding.getContracts().iterator().next();
    }

    private <T> T createClientProxy(BeanInstance beanInstance, String contextId) {
        ProxyFactory<T> factory = new ProxyFactory<>(contextId, getBeanClass(), getTypes(), this);
        return factory.create(beanInstance);
    }

    private static class ThreadScopeBeanInstance<T> extends ContextBeanInstance<T> {

        private final WeakHashMap<Thread, Object> instances = new WeakHashMap<>();

        private final Supplier<T> supplier;

        /**
         * Creates a new invocation handler with supplier which provides a current injected value in proper scope.
         *
         * @param supplier provider of the value.
         */
        private ThreadScopeBeanInstance(Supplier<T> supplier, Bean<T> bean, String contextId) {
            super(bean, new StringBeanIdentifier(((PassivationCapable) bean).getId()), contextId);
            this.supplier = supplier;
        }

        @Override
        public Object invoke(Object obj, Method method, Object... arguments) throws Throwable {
            Object instance = instances.computeIfAbsent(Thread.currentThread(), thread -> supplier.get());
            return super.invoke(instance, method, arguments);
        }

        public void dispose() {
            this.instances.clear();
        }
    }
}
