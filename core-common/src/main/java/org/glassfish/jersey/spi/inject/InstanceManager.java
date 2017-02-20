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

package org.glassfish.jersey.spi.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import org.glassfish.jersey.spi.ServiceHolder;

/**
 * Interface provides the communication API between Jersey and Dependency Injection provider
 * <p>
 * First, the method {@link #initialize(String, InstanceManager, String, Binder...)} should be call to initialize DI provider
 * (e.g. create underlying storage for registered services) and to do other stuff needed for successful start of DI provider.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
// TODO: Exception??
public interface InstanceManager {

    /**
     * This will initialize the {@code InstanceManager} and underlying DI provider. The method may get the array of binders to
     * register {@link Descriptor} them during initialization process. {@code name} and {@code parent} are not required parameters
     * and can be null without the initialization exception.
     *
     * @param name          Name of the instance manager.
     * @param parent        Parent instance manager on which new instance manager should be dependent.
     * @param classAnalyzer -------
     * @param binders       Binders with descriptions to include them during initialization process.
     */
    // TODO: Remove CLASS ANALYZER
    // TODO: Are binders needed only for HK2 dependent?
    void initialize(String name, InstanceManager parent, String classAnalyzer, Binder... binders);

    /**
     * This will shutdown the entire instance manager and underlying DI provider along with injected executors and schedulers.
     */
    void shutdown();

    /**
     * This will register one bean represented using fields in the provided descriptor. The final bean can be direct bean or
     * factory object which will create the bean at the time of injection. {@code InstanceManager} is able to register a bean
     * represented by a class or direct instance.
     *
     * @param descriptor one descriptor.
     * @see ClassBeanDescriptor
     * @see InstanceBeanDescriptor
     * @see ClassFactoryDescriptor
     * @see InstanceFactoryDescriptor
     */
    void register(Descriptor descriptor);

    /**
     * This will register a collection of beans represented using fields in the provided descriptors. The final bean can be
     * direct bean or factory object which will create the bean at the time of injection. {@code InstanceManager} is able to
     * register a bean represented by a class or direct instance.
     *
     * @param descriptors collection of descriptors.
     * @see ClassBeanDescriptor
     * @see InstanceBeanDescriptor
     * @see ClassFactoryDescriptor
     * @see InstanceFactoryDescriptor
     */
    void register(Iterable<Descriptor> descriptors);

    /**
     * This will register beans which are included in {@link Binder}. {@code Binder} can contains all descriptors extending
     * {@link Descriptor} or other binders which are installed together in tree-structure. This method will get all descriptors
     * bound in the given binder and register them in the order how the binders are installed together. In the tree structure,
     * the deeper on the left side will be processed first.
     *
     * @param binder collection of descriptors.
     * @see ClassBeanDescriptor
     * @see InstanceBeanDescriptor
     * @see ClassFactoryDescriptor
     * @see InstanceFactoryDescriptor
     */
    void register(Binder binder);

    /**
     * This method creates, injects and post-constructs an object with the given class. This is equivalent to calling the
     * {@code create-class} method followed by the {@code inject-class} method followed by the {@code post-construct} method.
     * <p>
     * The object created is not managed by the instance manager.
     *
     * @param createMe The non-null class to create this object from;
     * @return An instance of the object that has been created, injected and post constructed.
     */
    <T> T createAndInitialize(Class<T> createMe);

    /**
     * Gets all services from this instance manager that implements this contract or has this implementation along with
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
     * Gets the best service from this instance manager that implements this contract or has this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InstanceManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param qualifiers     The set of qualifiers that must match this service definition.
     * @param <T>            Instance type.
     * @return An instance of the contract or impl.  May return  null if there is no provider that provides the given
     * implementation or contract.
     */
    <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers);

    /**
     * Gets the best service from this instance manager that implements this contract or has this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InstanceManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param classAnalyzer  -------
     * @param <T>            Instance type.
     * @return An instance of the contract or impl.  May return  null if there is no provider that provides the given
     * implementation or contract.
     */
    // TODO: Remove CLASS ANALYZER
    <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer);

    /**
     * Gets the best service from this instance manager that implements this contract or has this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InstanceManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param <T>            Instance type.
     * @return An instance of the contract or impl.  May return  null if there is no provider that provides the given
     * implementation or contract.
     */
    <T> T getInstance(Class<T> contractOrImpl);

    /**
     * Gets the best service from this instance manager that implements this contract or has this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InstanceManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param <T>            Instance type.
     * @return An instance of the contract or impl.  May return  null if there is no provider that provides the given
     * implementation or contract.
     */
    <T> T getInstance(Type contractOrImpl);

    /**
     * Gets all services from this instance manager that implement this contract or have this implementation.
     * <p>
     * Use this method only if other information is not needed otherwise use, otherwise use
     * {@link InstanceManager#getAllServiceHolders(Class, Annotation...)}.
     *
     * @param contractOrImpl May not be null, and is the contract or concrete implementation to get the best instance of.
     * @param <T>            Instance type.
     * @return A list of services implementing this contract or concrete implementation.  May not return null, but  may return
     * an empty list
     */
    <T> List<T> getAllInstances(Type contractOrImpl);

    /**
     * This will analyze the given object and inject into its fields and methods.
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
    // TODO: Remove CLASS ANALYZER
    void inject(Object injectMe, String classAnalyzer);

    /**
     * This will analyze the given object and call the preDestroy method. The object given will not be managed by bean manager.
     *
     * @param preDestroyMe The object to preDestroy
     */
    void preDestroy(Object preDestroyMe);
}
