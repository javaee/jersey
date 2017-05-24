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

package org.glassfish.jersey.inject.cdi.se.injector;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.glassfish.jersey.internal.inject.InjectionResolver;

/**
 * Class that accepts all registered {@link InjectionResolver} and inject all possible values annotated by JAX-RS annotations
 * into provided instance in {@link #inject(Object)}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
class JerseyInstanceInjector<T> {

    private final Bean<T> bean;
    private final Map<Field, InjectionResolver> cachedFields;

    private final JerseyProxyResolver proxyResolver = new JerseyProxyResolver();

    /**
     * Constructor that creates a new class injector for the given class.
     *
     * @param bean      information about the injected class.
     * @param resolvers all resolvers which are registered in the application.
     */
    JerseyInstanceInjector(Bean<T> bean, Collection<InjectionResolver> resolvers) {
        this.bean = bean;
        this.cachedFields = analyzeFields(bean.getBeanClass(), resolvers);
    }

    /**
     * Takes an instance an inject the annotated field which were analyzed during the injector construction in method
     * {@link #analyzeFields(Class, Collection)}.
     *
     * @param injectMe an instance into which the values will be injected.
     */
    void inject(T injectMe) {
        InjectionUtils.justInject(injectMe, bean, cachedFields, proxyResolver);
    }

    /**
     * Takes a class and returns all fields along with {@link InjectionResolver} which will be used for injection during injection
     * process.
     *
     * @param clazz     class to be analyzed.
     * @param resolvers all registered injection resolvers.
     * @return immutable map of all fields along with injection resolvers using that can be injected.
     */
    private Map<Field, InjectionResolver> analyzeFields(Class<?> clazz, Collection<InjectionResolver> resolvers) {
        Map<? extends Class<?>, InjectionResolver> injectAnnotations = InjectionUtils.mapAnnotationToResolver(resolvers);

        Collector collector = new Collector();
        Set<Field> fields = InjectionUtils.getFields(clazz, injectAnnotations.keySet(), collector);
        collector.throwIfErrors();
        return InjectionUtils.mapElementToResolver(fields, injectAnnotations);
    }
}
