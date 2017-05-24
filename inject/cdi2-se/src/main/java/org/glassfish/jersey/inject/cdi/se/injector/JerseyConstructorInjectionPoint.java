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

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Provider;

import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjecteeImpl;
import org.glassfish.jersey.internal.inject.InjectionResolver;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedConstructor;
import org.jboss.weld.injection.ConstructorInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.injection.ParameterInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Class that creates a new instance using the provided constructor, selects and injects the values.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class JerseyConstructorInjectionPoint<T> extends ConstructorInjectionPoint<T> {

    private final JerseyProxyResolver proxyResolver = new JerseyProxyResolver();

    private List<Supplier<Object>> cachedSuppliers;

    private Object[] cachedProxies;

    /**
     * Creates a new constructor injection point suitable for Jersey components.
     *
     * @param constructor resolved constructor that can be injected using Jersey.
     * @param bean        bean descriptor dedicated to the parent class.
     * @param manager     current bean manager.
     * @param resolvers   all registered resolvers.
     */
    public JerseyConstructorInjectionPoint(EnhancedAnnotatedConstructor<T> constructor, Bean<T> bean, BeanManagerImpl manager,
            Collection<InjectionResolver> resolvers) {
        super(constructor, null, constructor.getJavaClass(), InjectionPointFactory.instance(), manager);

        List<InjecteeToSupplier> valueSuppliers =
                createValueSuppliers(constructor.getJavaMember(), getParameterInjectionPoints(), resolvers);

        /*
         * Caches either created proxies if the component class is not RequestScoped or caches the supplier that just create
         * values every component creates.
         */
        if (proxyResolver.isPrixiable(bean.getScope())) {
            this.cachedProxies = generateProxies(valueSuppliers);
        } else {
            this.cachedSuppliers = valueSuppliers.stream()
                    .map(is -> is.supplier)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Helper method for getting the current parameter values from a list of annotated parameters.
     *
     * @param manager The Bean manager
     * @return The object array of looked up values
     */
    public Object[] getParameterValues(BeanManagerImpl manager, CreationalContext<?> ctx, CreationalContext<?> ctxTransient) {
        if (cachedProxies == null) {
            return generateValues(cachedSuppliers);
        } else {
            return cachedProxies;
        }
    }

    private Object[] generateValues(List<Supplier<Object>> suppliers) {
        Object[] parameterValues = new Object[getParameterInjectionPoints().size()];
        for (int i = 0; i < parameterValues.length; i++) {
            parameterValues[i] = suppliers.get(i).get();
        }
        return parameterValues;
    }

    private Object[] generateProxies(List<InjecteeToSupplier> suppliers) {
        Object[] proxies = new Object[suppliers.size()];
        for (int i = 0; i < proxies.length; i++) {
            InjecteeToSupplier injecteeToSupplier = suppliers.get(i);
            if (injecteeToSupplier.injectee.isProvider()) {
                proxies[i] = new Provider<Object>() {
                    @Override
                    public Object get() {
                        return injecteeToSupplier.supplier.get();
                    }
                };
            } else {
                proxies[i] = proxyResolver.noCachedProxy(injecteeToSupplier.injectee, injecteeToSupplier.supplier);
            }
        }
        return proxies;
    }

    /**
     * Maps the parameters of the selected constructor to the injection resolver.
     *
     * @param params    all parameters of a constructor.
     * @param resolvers registered injection resolvers.
     * @return map of the parameter to injection resolver.
     */
    private List<InjecteeToSupplier> createValueSuppliers(Constructor<T> constructor,
            List<ParameterInjectionPoint<?, T>> params, Collection<InjectionResolver> resolvers) {

        List<InjecteeToSupplier> suppliers = new ArrayList<>();
        Map<? extends Class<?>, InjectionResolver> injectAnnotations = InjectionUtils.mapAnnotationToResolver(resolvers);
        for (int i = 0; i < params.size(); i++) {
            Parameter parameter = params.get(i).getAnnotated().getJavaParameter();
            InjectionResolver resolver = InjectionUtils.findResolver(injectAnnotations, parameter);
            Injectee injectee = parameterToInjectee(constructor, parameter, i);
            suppliers.add(new InjecteeToSupplier(injectee, () -> resolver.resolve(injectee)));
        }

        return suppliers;
    }

    private Injectee parameterToInjectee(Constructor<T> constructor, Parameter parameter, int position) {
        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setParent(constructor);
        if (parameter.getParameterizedType() instanceof ParameterizedType
                && InjectionUtils.isProvider(parameter.getParameterizedType())) {
            ParameterizedType paramType = (ParameterizedType) parameter.getParameterizedType();
            injectee.setRequiredType(paramType.getActualTypeArguments()[0]);
            injectee.setProvider(true);
        } else {
            injectee.setRequiredType(parameter.getType());
        }
        injectee.setPosition(position);
        return injectee;
    }

    /**
     * Holder for Injectee and Supplier types. Internal class.
     */
    private static class InjecteeToSupplier {

        private final Injectee injectee;
        private final Supplier<Object> supplier;

        private InjecteeToSupplier(Injectee injectee, Supplier<Object> supplier) {
            this.injectee = injectee;
            this.supplier = supplier;
        }
    }
}
