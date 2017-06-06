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

import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjecteeImpl;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.Cache;

/**
 * Injection resolver for {@link Context @Context} injection annotation.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class ContextInjectionResolverImpl implements InjectionResolver<Context>, ContextInjectionResolver {

    private Supplier<BeanManager> beanManager;

    /**
     * Creates a new {@link ContextInjectionResolver} with {@link BeanManager} to fetch Bean descriptors.
     *
     * @param beanManager current bean manager.
     */
    ContextInjectionResolverImpl(Supplier<BeanManager> beanManager) {
        this.beanManager = beanManager;
    }

    private final Cache<Type, Bean<?>> descriptorCache = new Cache<>(key -> {
        Set<Bean<?>> beans = beanManager.get().getBeans(key);
        if (beans.isEmpty()) {
            return null;
        }
        return beans.iterator().next();
    });

    @Override
    public Object resolve(Injectee injectee) {
        Injectee newInjectee = injectee;
        if (injectee.isFactory()) {
            newInjectee = getFactoryInjectee(injectee, ReflectionHelper.getTypeArgument(injectee.getRequiredType(), 0));
        }

        Bean<?> bean = descriptorCache.apply(newInjectee.getRequiredType());

        if (bean != null) {
            CreationalContext ctx = beanManager.get().createCreationalContext(bean);
            Object result = bean.create(ctx);

            if (injectee.isFactory()) {
                return (Supplier<Object>) () -> result;
            } else {
                return result;
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

    @Override
    public Class<Context> getAnnotation() {
        return Context.class;
    }

    /**
     * Context injection resolver binder.
     */
    public static final class Binder extends AbstractBinder {

        private Supplier<BeanManager> beanManager;

        public Binder(Supplier<BeanManager> beanManager) {
            this.beanManager = beanManager;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void configure() {
            ContextInjectionResolverImpl resolver = new ContextInjectionResolverImpl(beanManager);

            /*
             * Binding for CDI, without this binding JerseyInjectionTarget wouldn't know about the ContextInjectionTarget and
             * injection into fields would be disabled.
             */
            bind(resolver)
                    .to(new GenericType<InjectionResolver<Context>>() {})
                    .to(ContextInjectionResolver.class);

            /*
             * Binding for Jersey, without this binding Jersey wouldn't put together ContextInjectionResolver and
             * DelegatedInjectionValueParamProvider and therefore injection into resource method would be disabled.
             */
            bind(Bindings.service(resolver))
                    .to(new GenericType<InjectionResolver<Context>>() {})
                    .to(ContextInjectionResolver.class);
        }
    }

    private Injectee getFactoryInjectee(Injectee injectee, Type requiredType) {
        return new RequiredTypeOverridingInjectee(injectee, requiredType);
    }

    private static class RequiredTypeOverridingInjectee extends InjecteeImpl {
        private RequiredTypeOverridingInjectee(Injectee injectee, Type requiredType) {
            setFactory(injectee.isFactory());
            setInjecteeClass(injectee.getInjecteeClass());
            setInjecteeDescriptor(injectee.getInjecteeDescriptor());
            setOptional(injectee.isOptional());
            setParent(injectee.getParent());
            setPosition(injectee.getPosition());
            setRequiredQualifiers(injectee.getRequiredQualifiers());
            setRequiredType(requiredType);
        }
    }
}
