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
import java.util.List;

/**
 * Interface provides the communication API between Jersey and Dependency Injection provider.
 * <p>
 * Lifecycle methods should be called in this order:
 * <ul>
 * <li>{@link #completeRegistration()} - notifies that Jersey bootstrap has been finished and DI provider should be ready for a runtime.</li>
 * <li>{@link #shutdown()} - Jersey application has been closed and DI provider should make needed cleaning steps.</li>
 * </ul>
 * <p>
 * All {@code getInstance} methods can be called after {@link #completeRegistration()} method has been called because at this all
 * components are bound to injection manager and ready for getting.
 * In turn, {@link #shutdown()} method stops the possibility to use these methods and closes {@code InjectionManager}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public interface InjectionManager {

    /**
     * Completes {@link InjectionManager} and the underlying DI provider. All registered components are bound to injection
     * manager and after an invocation of this method all components are available using e.g. {@link #getInstance(Class)}.
     */
    void completeRegistration();

    /**
     * Shuts down the entire {@link InjectionManager} and the underlying DI provider.
     * <p>
     * Shutdown phase is dedicated to make some final cleaning steps regarding underlying DI provider.
     */
    void shutdown();

    /**
     * Registers one bean represented using fields in the provided descriptor. The final bean can be direct bean or
     * factory object which will create the bean at the time of injection. {@code InjectionManager} is able to register a bean
     * represented by a class or direct instance.
     *
     * @param binding one descriptor.
     * @see ClassBinding
     * @see InstanceBinding
     * @see SupplierClassBinding
     * @see SupplierInstanceBinding
     */
    void register(Binding binding);

    /**
     * Registers a collection of beans represented using fields in the provided descriptors. The final bean can be
     * direct bean or factory object which will create the bean at the time of injection. {@code InjectionManager} is able to
     * register a bean represented by a class or direct instance.
     *
     * @param descriptors collection of descriptors.
     * @see ClassBinding
     * @see InstanceBinding
     * @see SupplierClassBinding
     * @see SupplierInstanceBinding
     */
    void register(Iterable<Binding> descriptors);

    /**
     * Registers beans which are included in {@link Binder}. {@code Binder} can contains all descriptors extending
     * {@link Binding} or other binders which are installed together in tree-structure. This method will get all descriptors
     * bound in the given binder and register them in the order how the binders are installed together. In the tree structure,
     * the deeper on the left side will be processed first.
     *
     * @param binder collection of descriptors.
     * @see ClassBinding
     * @see InstanceBinding
     * @see SupplierClassBinding
     * @see SupplierInstanceBinding
     */
    void register(Binder binder);

    /**
     * Registers a provider. An implementation of the {@link InjectionManager} should test whether the type of the object can be
     * registered using the method {@link #isRegistrable(Class)}. Then a caller has an certainty that the instance of the tested
     * class can be registered in {@code InjectionManager}. If {@code InjectionManager} is not able to register the provider
     * then {@link IllegalArgumentException} is thrown.
     *
     * @param provider object that can be registered in {@code InjectionManager}.
     * @throws IllegalArgumentException provider cannot be registered.
     */
    void register(Object provider) throws IllegalArgumentException;

    /**
     * Tests whether the provided {@code clazz} can be registered by the implementation of the {@link InjectionManager}.
     *
     * @param clazz type that is tested whether is registrable by the implementation of {@code InjectionManager}.
     * @return {@code true} if the {@code InjectionManager} is able to register this type.
     */
    boolean isRegistrable(Class<?> clazz);

    /**
     * Creates, injects and post-constructs an object with the given class. This is equivalent to calling the
     * {@code create-class} method followed by the {@code inject-class} method followed by the {@code post-construct} method.
     * <p>
     * The object created is not managed by the injection manager.
     *
     * @param createMe The non-null class to create this object from;
     * @return An instance of the object that has been created, injected and post constructed.
     */
    <T> T createAndInitialize(Class<T> createMe);

    /**
     * Gets all services from this injection manager that implements this contract or has this implementation along with
     * information about the service which can be kept by {@link ServiceHolder}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param qualifiers     The set of qualifiers that must match this service definition.
     * @param <T>            Instance type.
     * @return An instance of the contract or impl along with other information. May return  null if there is no provider that
     * provides the given implementation or contract.
     */
    <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contractOrImpl, Annotation... qualifiers);

    /**
     * Gets the best service from this injection manager that implements this contract or has this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InjectionManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param qualifiers     The set of qualifiers that must match this service definition.
     * @param <T>            Instance type.
     * @return An instance of the contract or impl.  May return  null if there is no provider that provides the given
     * implementation or contract.
     */
    <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers);

    /**
     * Gets the best service from this injection manager that implements this contract or has this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InjectionManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param classAnalyzer  -------
     * @param <T>            Instance type.
     * @return An instance of the contract or impl.  May return  null if there is no provider that provides the given
     * implementation or contract.
     */
    // TODO: Remove CLASS ANALYZER - NEEDED ONLY IN CdiComponentProvider
    <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer);

    /**
     * Gets the best service from this injection manager that implements this contract or has this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InjectionManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param <T>            Instance type.
     * @return An instance of the contract or impl.  May return  null if there is no provider that provides the given
     * implementation or contract.
     */
    <T> T getInstance(Class<T> contractOrImpl);

    /**
     * Gets the best service from this injection manager that implements this contract or has this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InjectionManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param <T>            Instance type.
     * @return An instance of the contract or impl.  May return  null if there is no provider that provides the given
     * implementation or contract.
     */
    <T> T getInstance(Type contractOrImpl);

    /**
     * Gets the service instance according to {@link ForeignDescriptor} which is specific to the underlying DI provider.
     *
     * @param foreignDescriptor DI specific descriptor.
     * @return service instance according to foreign descriptor.
     */
    Object getInstance(ForeignDescriptor foreignDescriptor);

    /**
     * Creates and registers the descriptor in the underlying DI provider and returns {@link ForeignDescriptor} that is specific
     * descriptor for the underlying DI provider.
     *
     * @param binding jersey descriptor.
     * @return specific foreign descriptor of the underlying DI provider.
     */
    ForeignDescriptor createForeignDescriptor(Binding binding);

    /**
     * Gets all services from this injection manager that implement this contract or have this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InjectionManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param <T>            Instance type.
     * @return A list of services implementing this contract or concrete implementation.  May not return null, but  may return
     * an empty list
     */
    <T> List<T> getAllInstances(Type contractOrImpl);

    /**
     * Analyzes the given object and inject into its fields and methods.
     * The object injected in this way will not be managed by HK2
     *
     * @param injectMe The object to be analyzed and injected into
     */
    void inject(Object injectMe);

    /**
     * This will analyze the given object and inject into its fields and methods. The object injected in this way will not be
     * managed by HK2
     *
     * @param injectMe The object to be analyzed and injected into
     */
    // TODO: Remove CLASS ANALYZER - only in legacy CDI integration.
    void inject(Object injectMe, String classAnalyzer);

    /**
     * Analyzes the given object and call the preDestroy method. The object given will not be managed by bean manager.
     *
     * @param preDestroyMe The object to preDestroy
     */
    void preDestroy(Object preDestroyMe);
}
