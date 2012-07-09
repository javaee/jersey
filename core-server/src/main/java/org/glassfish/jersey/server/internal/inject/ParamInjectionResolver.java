/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
import org.jvnet.tigertypes.Types;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Abstract base class for resolving JAX-RS {@link @XxxParam} injection.
 *
 * @param <A> supported parameter injection annotation.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class ParamInjectionResolver<A extends Annotation> implements InjectionResolver<A> {

    @Inject
    private ServiceLocator services;
    final Predicate<ValueFactoryProvider> concreteValueFactoryClassFilter;

    /**
     * Initialize the base parameter injection resolver.
     *
     * @param injectionAnnotationClass parameter injection annotation class.
     * @param valueFactoryProviderClass parameter value factory provider class.
     */
    public ParamInjectionResolver(final Class<A> injectionAnnotationClass, final Class<? extends AbstractValueFactoryProvider<A>> valueFactoryProviderClass) {
        this.concreteValueFactoryClassFilter = new Predicate<ValueFactoryProvider>() {

            @Override
            public boolean apply(ValueFactoryProvider input) {
                return valueFactoryProviderClass.isInstance(input);
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {

        AnnotatedElement annotated = injectee.getParent();
        Class componentClass = injectee.getInjecteeClass();
        Type genericType = injectee.getRequiredType();
        boolean isHk2Factory = Types.isSubClassOf(genericType, Factory.class);

        final Type targetGenericType;
        if (isHk2Factory) {
            targetGenericType = Types.getTypeArgument(genericType, 0);
        } else {
            targetGenericType = genericType;
        }
        final Class<?> targetType = Types.erasure(targetGenericType);

        Set<ValueFactoryProvider> providers = Sets.filter(Providers.getProviders(services, ValueFactoryProvider.class), concreteValueFactoryClassFilter);
        final ValueFactoryProvider valueFactoryProvider = providers.iterator().next(); // get first provider in the set
        final Parameter parameter = Parameter.create(
                componentClass,
                componentClass,
                false,
                targetType,
                targetGenericType,
                annotated.getDeclaredAnnotations());

        final Factory<?> valueFactory = valueFactoryProvider.getValueFactory(parameter);
        if (valueFactory != null) {
            if (isHk2Factory) {
                return valueFactory;
            } else {
                return valueFactory.provide();
            }
        }

        return null;
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
