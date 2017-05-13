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

package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;

import javax.inject.Provider;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Implementation of {@link Binder} interface dedicated to keep some level of code compatibility between previous HK2
 * implementation and new DI SPI.
 * <p>
 * Currently, there are supported only bind method and more complicated method where HK2 interfaces are required were omitted.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public abstract class AbstractBinder implements Binder {

    private List<Binding> internalBindings = new ArrayList<>();

    private List<AbstractBinder> installed = new ArrayList<>();

    private InjectionManager injectionManager;

    private boolean configured = false;

    /**
     * Implement to provide binding definitions using the exposed binding methods.
     */
    protected abstract void configure();

    /**
     * Sets {@link InjectionManager} to be able to create instance providers using the injection manager. {@code InjectionManager}
     * should be called before the invocation of {@link #configure()}, otherwise immediate invocation {@link Provider#get()}
     * returns
     *
     * @param injectionManager injection manager to create a provider.
     */
    void setInjectionManager(InjectionManager injectionManager) {
        this.injectionManager = injectionManager;
    }

    /**
     * Creates a new instance of {@link Provider} which is able to retrieve a managed instance registered in
     * {@link InjectionManager}. If {@code InjectionManager} is {@code null} at the time of calling {@link Provider#get()} then
     * {@link IllegalStateException} is thrown.
     *
     * @param clazz class of managed instance.
     * @param <T>   type of the managed instance returned using provider.
     * @return provider with instance of managed instance.
     */
    protected final <T> Provider<T> createManagedInstanceProvider(Class<T> clazz) {
        return () -> {
            if (injectionManager == null) {
                throw new IllegalStateException(LocalizationMessages.INJECTION_MANAGER_NOT_PROVIDED());
            }
            return injectionManager.getInstance(clazz);
        };
    }

    /**
     * Start building a new class-based service binding.
     * <p>
     * Does NOT bind the service type itself as a contract type.
     *
     * @param <T>         service type.
     * @param serviceType service class.
     * @return initialized binding builder.
     */
    public <T> ClassBinding<T> bind(Class<T> serviceType) {
        ClassBinding<T> binding = Bindings.service(serviceType);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Binds the provided binding and return the same instance.
     *
     * @param binding binding.
     * @return the same provided binding.
     */
    public Binding bind(Binding binding) {
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Start building a new class-based service binding.
     * <p>
     * Binds the service type itself as a contract type.
     *
     * @param <T>         service type.
     * @param serviceType service class.
     * @return initialized binding builder.
     */
    public <T> ClassBinding<T> bindAsContract(Class<T> serviceType) {
        ClassBinding<T> binding = Bindings.serviceAsContract(serviceType);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Start building a new generic type-based service binding.
     * <p>
     * Binds the generic service type itself as a contract type.
     *
     * @param <T>         service type.
     * @param serviceType generic service type information.
     * @return initialized binding builder.
     */
    public <T> ClassBinding<T> bindAsContract(GenericType<T> serviceType) {
        ClassBinding<T> binding = Bindings.service(serviceType);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Start building a new generic type-based service binding.
     * <p>
     * Binds the generic service type itself as a contract type.
     *
     * @param serviceType generic service type information.
     * @return initialized binding builder.
     */
    public ClassBinding<Object> bindAsContract(Type serviceType) {
        ClassBinding<Object> binding = Bindings.serviceAsContract(serviceType);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Start building a new instance-based service binding. The binding is naturally
     * considered to be a {@link javax.inject.Singleton singleton-scoped}.
     * <p>
     * Does NOT bind the service type itself as a contract type.
     *
     * @param <T>     service type.
     * @param service service instance.
     * @return initialized binding builder.
     */
    public <T> InstanceBinding<T> bind(T service) {
        InstanceBinding<T> binding = Bindings.service(service);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Start building a new supplier class-based service binding.
     *
     * @param <T>           service type.
     * @param supplierType  service supplier class.
     * @param supplierScope factory scope.
     * @return initialized binding builder.
     */
    public <T> SupplierClassBinding<T> bindFactory(
            Class<? extends Supplier<T>> supplierType, Class<? extends Annotation> supplierScope) {
        SupplierClassBinding<T> binding = Bindings.supplier(supplierType, supplierScope);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Start building a new supplier class-based service binding.
     * <p>
     * The supplier itself is bound in a per-lookup scope.
     *
     * @param <T>          service type.
     * @param supplierType service supplier class.
     * @return initialized binding builder.
     */
    public <T> SupplierClassBinding<T> bindFactory(Class<? extends Supplier<T>> supplierType) {
        SupplierClassBinding<T> binding = Bindings.supplier(supplierType);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Start building a new supplier instance-based service binding.
     *
     * @param <T>     service type.
     * @param factory service instance.
     * @return initialized binding builder.
     */
    public <T> SupplierInstanceBinding<T> bindFactory(Supplier<T> factory) {
        SupplierInstanceBinding<T> binding = Bindings.supplier(factory);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Start building a new injection resolver binding. The injection resolver is naturally
     * considered to be a {@link javax.inject.Singleton singleton-scoped}.
     * <p>
     * There is no need to provide any additional information. Other method on {@link Binding}
     * will be ignored.
     *
     * @param <T>      type of the injection resolver.
     * @param resolver injection resolver instance.
     * @return initialized binding builder.
     */
    public <T extends InjectionResolver> InjectionResolverBinding<T> bind(T resolver) {
        InjectionResolverBinding<T> binding = Bindings.injectionResolver(resolver);
        internalBindings.add(binding);
        return binding;
    }

    /**
     * Adds all binding definitions from the binders to the binding configuration.
     *
     * @param binders binders whose binding definitions should be configured.
     */
    public final void install(AbstractBinder... binders) {
        Arrays.stream(binders)
                .filter(Objects::nonNull)
                .forEach(installed::add);
    }

    @Override
    public Collection<Binding> getBindings() {
        invokeConfigure();
        List<Binding> bindings = installed.stream()
                .flatMap(binder -> Bindings.getBindings(injectionManager, binder).stream())
                .collect(Collectors.toList());
        bindings.addAll(internalBindings);
        return bindings;
    }

    private void invokeConfigure() {
        if (!configured) {
            configure();
            configured = true;
        }
    }
}
