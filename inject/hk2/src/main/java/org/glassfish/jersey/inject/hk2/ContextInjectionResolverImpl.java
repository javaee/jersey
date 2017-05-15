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

package org.glassfish.jersey.inject.hk2;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.core.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ForeignRequestScopeBridge;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.Cache;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.process.internal.RequestScoped;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.InjecteeImpl;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Injection resolver for {@link Context @Context} injection annotation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Singleton
public class ContextInjectionResolverImpl implements InjectionResolver<Context>, ContextInjectionResolver {

    /**
     * Context injection resolver binder.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(ContextInjectionResolverImpl.class)
                    .to(new TypeLiteral<InjectionResolver<Context>>() {})
                    .to(new TypeLiteral<org.glassfish.jersey.internal.inject.InjectionResolver<Context>>() {})
                    .to(ContextInjectionResolver.class)
                    .in(Singleton.class);
        }
    }

    @Inject
    private ServiceLocator serviceLocator;

    private final Cache<CacheKey, ActiveDescriptor<?>> descriptorCache
            = new Cache<>(cacheKey -> serviceLocator.getInjecteeDescriptor(cacheKey.injectee));

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        Type requiredType = injectee.getRequiredType();
        boolean isHk2Factory = ReflectionHelper.isSubClassOf(requiredType, Factory.class);
        Injectee newInjectee;

        if (isHk2Factory) {
            newInjectee = getFactoryInjectee(injectee, ReflectionHelper.getTypeArgument(requiredType, 0));
        } else {
            newInjectee = foreignRequestScopedInjecteeCache.apply(new CacheKey(injectee));
        }

        ActiveDescriptor<?> ad = descriptorCache.apply(new CacheKey(newInjectee));

        if (ad != null) {
            final ServiceHandle handle = serviceLocator.getServiceHandle(ad, newInjectee);

            if (isHk2Factory) {
                return asFactory(handle);
            } else {
                return handle.getService();
            }
        }
        return null;
    }

    /**
     * Jersey Injection Resolver method that just populate HK2 injectee object and delegates the processing to HK2 Injection
     * Resolver.
     *
     * @param injectee The injection point this value is being injected into
     * @return result of the injection processing.
     */
    @Override
    public Object resolve(org.glassfish.jersey.internal.inject.Injectee injectee) {
        InjecteeImpl hk2injectee = toInjecteeImpl(injectee);

        // Delegate the call to HK2 Resolver, Service Handle is not need in the delegated processing.
        return resolve(hk2injectee, null);
    }

    private static InjecteeImpl toInjecteeImpl(org.glassfish.jersey.internal.inject.Injectee injectee) {
        InjecteeImpl hk2injectee = new InjecteeImpl() {
            @Override
            public Class<?> getInjecteeClass() {
                return injectee.getInjecteeClass();
            }
        };
        hk2injectee.setRequiredType(injectee.getRequiredType());
        hk2injectee.setRequiredQualifiers(injectee.getRequiredQualifiers());
        hk2injectee.setParent(injectee.getParent());
        if (injectee.getInjecteeDescriptor() != null) {
            hk2injectee.setInjecteeDescriptor((ActiveDescriptor<?>) injectee.getInjecteeDescriptor().get());
        }
        return hk2injectee;
    }

    private Factory asFactory(final ServiceHandle handle) {
        return new Factory() {
            @Override
            public Object provide() {
                return handle.getService();
            }

            @Override
            public void dispose(final Object instance) {
            }
        };
    }

    private Injectee getFactoryInjectee(final Injectee injectee, final Type requiredType) {
        return new RequiredTypeOverridingInjectee(injectee, requiredType);
    }

    private static class RequiredTypeOverridingInjectee extends InjecteeImpl {
        private RequiredTypeOverridingInjectee(final Injectee injectee, final Type requiredType) {
            super(injectee);
            setRequiredType(requiredType);
        }
    }

    private static class DescriptorOverridingInjectee extends InjecteeImpl {
        private DescriptorOverridingInjectee(final Injectee injectee, final ActiveDescriptor descriptor) {
            super(injectee);
            setInjecteeDescriptor(descriptor);
        }
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

    private final Cache<CacheKey, Injectee> foreignRequestScopedInjecteeCache = new Cache<>(new Function<CacheKey, Injectee>() {
        @Override
        public Injectee apply(CacheKey cacheKey) {
            Injectee injectee = cacheKey.getInjectee();
            if (injectee.getParent() != null) {
                if (Field.class.isAssignableFrom(injectee.getParent().getClass())) {
                    Field f = (Field) injectee.getParent();
                    if (foreignRequestScopedComponents.get().contains(f.getDeclaringClass())) {
                        final Class<?> clazz = f.getType();
                        if (serviceLocator.getServiceHandle(clazz).getActiveDescriptor().getScopeAnnotation()
                                                                                            == RequestScoped.class) {
                            final AbstractActiveDescriptor<Object> descriptor =
                                    BuilderHelper.activeLink(clazz)
                                            .to(clazz)
                                            .in(RequestScoped.class)
                                            .build();
                            return new DescriptorOverridingInjectee(injectee, descriptor);
                        }
                    }
                }
            }
            return injectee;
        }
    });

    private LazyValue<Set<Class<?>>> foreignRequestScopedComponents = Values.lazy(
            (Value<Set<Class<?>>>) this::getForeignRequestScopedComponents);

    private Set<Class<?>> getForeignRequestScopedComponents() {
        final List<ForeignRequestScopeBridge> scopeBridges = serviceLocator.getAllServices(ForeignRequestScopeBridge.class);
        final Set<Class<?>> result = new HashSet<>();
        for (ForeignRequestScopeBridge bridge : scopeBridges) {
            final Set<Class<?>> requestScopedComponents = bridge.getRequestScopedComponents();
            if (requestScopedComponents != null) {
                result.addAll(requestScopedComponents);
            }
        }
        return result;
    }

    /**
     * Key dedicated for internal cache mechanism because two different {@link Injectee} Hk2 implementations comes from
     * Jersey side and HK2 side injection resolver.
     */
    private static class CacheKey {

        private final Injectee injectee;

        private final int hash;

        private CacheKey(Injectee injectee) {
            this.injectee = injectee;

            this.hash = Objects.hash(injectee.getInjecteeClass(),
                    injectee.getInjecteeDescriptor(),
                    injectee.getParent(),
                    injectee.getRequiredQualifiers(),
                    injectee.getRequiredType(),
                    injectee.getPosition());
        }

        private Injectee getInjectee() {
            return injectee;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CacheKey)) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return this.hash == cacheKey.hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
