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

import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.glassfish.hk2.api.Factory;

/**
 * Utility class to create a new injection binding descriptions for arbitrary Java beans.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public final class Descriptors {

    private Descriptors() {
        throw new AssertionError("Utility class instantiation forbidden.");
    }

    /**
     * Start building a new class-based service binding.
     * <p>
     * Does NOT service the service type itself as a contract type.
     *
     * @param <T>         service type.
     * @param serviceType service class.
     * @return initialized binding builder.
     */
    public static <T> ClassBeanDescriptor<T> service(Class<T> serviceType) {
        return new ClassBeanDescriptor<>(serviceType);
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
    public static <T> ClassBeanDescriptor<T> serviceAsContract(Class<T> serviceType) {
        return new ClassBeanDescriptor<>(serviceType).to(serviceType);
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
    @SuppressWarnings("unchecked")
    public static <T> ClassBeanDescriptor<T> service(GenericType<T> serviceType) {
        return (ClassBeanDescriptor<T>) new ClassBeanDescriptor<>(serviceType.getRawType()).asType(serviceType.getType());
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
    @SuppressWarnings("unchecked")
    public static <T> ClassBeanDescriptor<T> serviceAsContract(GenericType<T> serviceType) {
        return (ClassBeanDescriptor<T>) new ClassBeanDescriptor<>(serviceType.getRawType())
                .asType(serviceType.getType())
                .to(serviceType.getType());
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
    @SuppressWarnings("unchecked")
    public static <T> ClassBeanDescriptor<T> serviceAsContract(Type serviceType) {
        return new ClassBeanDescriptor<>((Class<T>) ReflectionHelper.getRawClass(serviceType))
                .asType(serviceType)
                .to(serviceType);
    }

    /**
     * Start building a new instance-based service binding. The binding is naturally
     * considered to be a {@link javax.inject.Singleton singleton-scoped}.
     * <p>
     * Does NOT service the service type itself as a contract type.
     *
     * @param <T>     service type.
     * @param service service instance.
     * @return initialized binding builder.
     */
    public static <T> InstanceBeanDescriptor<T> service(T service) {
        return new InstanceBeanDescriptor<>(service);
    }

    /**
     * Start building a new instance-based service binding. The binding is naturally
     * considered to be a {@link javax.inject.Singleton singleton-scoped}.
     * <p>
     * Binds the generic service type itself as a contract type.
     *
     * @param <T>     service type.
     * @param service service instance.
     * @return initialized binding builder.
     */
    public static <T> InstanceBeanDescriptor<T> serviceAsContract(T service) {
        return new InstanceBeanDescriptor<>(service, service.getClass());
    }

    /**
     * Start building a new factory class-based service binding.
     *
     * @param <T>          service type.
     * @param factoryType  service factory class.
     * @param factoryScope factory scope.
     * @return initialized binding builder.
     */
    public static <T> ClassFactoryDescriptor<T> factory(
            Class<? extends Factory<T>> factoryType, Class<? extends Annotation> factoryScope) {
        return new ClassFactoryDescriptor<>(factoryType, factoryScope);
    }

    /**
     * Start building a new factory class-based service binding.
     * <p>
     * The factory itself is bound in a per-lookup scope.
     *
     * @param <T>         service type.
     * @param factoryType service factory class.
     * @return initialized binding builder.
     */
    public static <T> ClassFactoryDescriptor<T> factory(Class<? extends Factory<T>> factoryType) {
        return new ClassFactoryDescriptor<>(factoryType, null);
    }

    /**
     * Start building a new factory instance-based service binding.
     *
     * @param <T>     service type.
     * @param factory service instance.
     * @return initialized binding builder.
     */
    public static <T> InstanceFactoryDescriptor<T> factory(Factory<T> factory) {
        return new InstanceFactoryDescriptor<>(factory);
    }

    /**
     * Start building a new injection resolver binding. The injection resolver is naturally
     * considered to be a {@link javax.inject.Singleton singleton-scoped}.
     * <p>
     * There is no need to provide any additional information. Other method on {@link Descriptor}
     * will be ignored.
     *
     * @param <T>        type of the injection resolver.
     * @param resolver   injection resolver instance.
     * @return initialized binding builder.
     */
    public static <T extends InjectionResolver> InjectionResolverDescriptor<T> injectionResolver(T resolver) {
        return new InjectionResolverDescriptor<>(resolver);
    }
}
