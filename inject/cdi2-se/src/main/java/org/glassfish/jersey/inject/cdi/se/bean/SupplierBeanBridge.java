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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.glassfish.jersey.inject.cdi.se.ParameterizedTypeImpl;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;

/**
 * Creates an implementation of {@link javax.enterprise.inject.spi.Bean} interface using Jersey's {@link SupplierInstanceBinding}.
 * Binding provides the information about the bean also called {@link javax.enterprise.inject.spi.BeanAttributes} information.
 * The {@code Bean} does not use {@link org.glassfish.jersey.inject.cdi.se.injector.JerseyInjectionTarget} because serves already
 * the instances created by underlying {@link Supplier} injected target on which the call is delegated.
 * <p>
 * This implementation works as bridge between {@link Supplier} and its provided value. This solves the case when the concrete
 * type of supplier value is fetched from {@link org.glassfish.jersey.internal.inject.InjectionManager} then this
 * {@link javax.enterprise.inject.spi.Bean} implementation just invokes {@link Supplier#get} method on underlying/registered
 * supplier.
 * <p>
 * Inject example:
 * <pre>
 * AbstractBinder {
 *     &#64;Override
 *     protected void configure() {
 *         bindFactory(MyBeanFactory.class)
 *              .to(MyBean.class)
 *              .in(Singleton.class)&#59;
 *     }
 * }
 * </pre>
 * Register example:
 * <pre>
 *  &#64;Path("/")
 *  public class MyResource {
 *    &#64;Inject
 *    private MyBean myBean&#59;
 *  }
 * </pre>
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
class SupplierBeanBridge extends JerseyBean<Object> {

    private final BeanManager beanManager;
    private ParameterizedType type;
    private boolean disposable;
    private SupplierClassBinding binding;

    // This bridge can create multiple instances using the method 'provide' therefore must map created suppliers because of
    // 'dispose' invocation later on.
    // TODO: Key as a WeakReference - prevent objects in scope which never dispose the objects such as PerLookup.
    private final Map<Object, DisposableSupplier<Object>> disposableSuppliers = new IdentityHashMap<>();

    /**
     * Creates a new Jersey-specific {@link javax.enterprise.inject.spi.Bean} instance.
     *
     * @param binding {@link javax.enterprise.inject.spi.BeanAttributes} part of the bean.
     */
    @SuppressWarnings("unchecked")
    SupplierBeanBridge(SupplierClassBinding binding, BeanManager beanManager) {
        super(binding);

        // Register wrapper for factory functionality, wrapper automatically call service locator which is able to retrieve
        // the service in the proper context and scope. Bridge is registered for all contracts but is able to lookup from
        // service locator only using the first contract.
        Type contract = null;
        if (binding.getContracts().iterator().hasNext()) {
            contract = (Type) binding.getContracts().iterator().next();
        }

        this.binding = binding;
        this.beanManager = beanManager;
        this.disposable = DisposableSupplier.class.isAssignableFrom(binding.getSupplierClass());
        this.type = new ParameterizedTypeImpl(Supplier.class, contract);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object create(CreationalContext creationalContext) {
        if (type != null) {
            Supplier<?> supplier = getSupplier(beanManager, type);
            Object instance = supplier.get();
            if (disposable) {
                disposableSuppliers.put(instance, (DisposableSupplier) supplier);
            }
            return instance;
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void destroy(Object instance, CreationalContext context) {
        if (disposable) {
            DisposableSupplier disposableSupplier = disposableSuppliers.get(instance);
            disposableSupplier.dispose(instance);
            disposableSuppliers.remove(instance);
        }
    }

    private static Supplier<?> getSupplier(BeanManager beanManager, ParameterizedType supplierType) {
        Set<Bean<?>> beans = beanManager.getBeans(supplierType);
        if (beans.isEmpty()) {
            return null;
        }

        Bean<?> bean = beans.iterator().next();
        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
        return (Supplier<?>) beanManager.getReference(bean, supplierType, ctx);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Annotation> getScope() {
        return binding.getScope() == null ? Dependent.class : transformScope(binding.getScope());
    }
}
