/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ResourceContext;

import javax.inject.Inject;
import javax.inject.Scope;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.CustomAnnotationLiteral;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ExtendedResourceContext;
import org.glassfish.jersey.server.model.ResourceModel;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AliasDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Jersey implementation of JAX-RS {@link ResourceContext resource context}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyResourceContext implements ExtendedResourceContext {

    private final ServiceLocator locator;

    private final Set<Class<?>> bindingCache;
    private final Object bindingCacheLock;

    private volatile ResourceModel resourceModel;

    /**
     * Create new JerseyResourceContext.
     *
     * @param locator HK2 service locator.
     */
    @Inject
    JerseyResourceContext(ServiceLocator locator) {
        this.locator = locator;

        this.bindingCache = Sets.newIdentityHashSet();
        this.bindingCacheLock = new Object();
    }

    @Override
    public <T> T getResource(Class<T> resourceClass) {
        try {
            return Injections.getOrCreate(locator, resourceClass);
        } catch (Exception ex) {
            Logger.getLogger(JerseyResourceContext.class.getName()).log(Level.WARNING,
                    LocalizationMessages.RESOURCE_LOOKUP_FAILED(resourceClass), ex);
        }
        return null;
    }

    @Override
    public <T> T initResource(T resource) {
        locator.inject(resource);
        return resource;
    }

    /**
     * Binds {@code resourceClass} into HK2 context as singleton.
     *
     * The bound class is then cached internally so that any sub-sequent attempt to bind that class
     * as a singleton is silently ignored.
     *
     * @param <T>           type of the resource class.
     * @param resourceClass resource class that should be bound. If the class is not
     *                      annotated with {@link javax.inject.Singleton Singleton annotation} it
     *                      will be ignored by this method.
     */
    public <T> void bindResource(Class<T> resourceClass) {
        if (bindingCache.contains(resourceClass)) {
            return;
        }

        synchronized (bindingCacheLock) {
            if (bindingCache.contains(resourceClass)) {
                return;
            }

            final DynamicConfiguration dc = Injections.getConfiguration(locator);
            unsafeBindResource(resourceClass, null, dc);
            dc.commit();
        }
    }

    /**
     * Binds {@code resourceClass} into HK2 context as singleton.
     *
     * The bound class is then cached internally so that any sub-sequent attempt to bind that class
     * as a singleton is silently ignored.
     *
     * @param resource singleton resource instance that should be bound as singleton. If the class is not
     *                 annotated with {@link javax.inject.Singleton Singleton annotation} it
     *                 will be ignored by this method.
     */
    public <T> void bindResourceIfSingleton(T resource) {
        final Class<?> resourceClass = resource.getClass();
        if (bindingCache.contains(resourceClass)) {
            return;
        }

        synchronized (bindingCacheLock) {
            if (bindingCache.contains(resourceClass)) {
                return;
            }
            if (getScope(resourceClass) == Singleton.class) {
                final DynamicConfiguration dc = Injections.getConfiguration(locator);
                //noinspection unchecked
                Injections.addBinding(Injections.newBinder(resource).to((Class<T>) resourceClass), dc);
                dc.commit();
            }

            bindingCache.add(resourceClass);
        }
    }

    /**
     * Bind a resource instance in a HK2 context.
     *
     * The bound resource instance is internally cached to make sure any sub-sequent attempts to bind the
     * class are silently ignored.
     * <p>
     * WARNING: This version of method is not synchronized as well as the cache is not checked for existing
     * bindings before the resource is bound and cached.
     * </p>
     *
     * @param resource resource instance to be bound.
     * @param providerModel provider model for the resource class. If not {@code null}, the class
     *                      wil be bound as a contract provider too.
     * @param dc            dynamic HK2 service locator configuration.
     */
    public void unsafeBindResource(
            final Object resource,
            final org.glassfish.jersey.model.ContractProvider providerModel,
            final DynamicConfiguration dc) {
        final Class<?> resourceClass = resource.getClass();
        if (providerModel != null) {
            final Class<? extends Annotation> scope = providerModel.getScope();
            final ActiveDescriptor<?> descriptor =
                    dc.bind(BuilderHelper.createConstantDescriptor(resource, null, resourceClass));

            for (Class contract : Providers.getProviderContracts(resourceClass)) {
                @SuppressWarnings("unchecked")
                AliasDescriptor aliasDescriptor = new AliasDescriptor(locator, descriptor, contract.getName(), null);
                aliasDescriptor.setScope(scope.getName());
                aliasDescriptor.addQualifierAnnotation(CustomAnnotationLiteral.INSTANCE);

                dc.bind(aliasDescriptor);
            }
        } else {
            Injections.addBinding(Injections.newBinder(resourceClass).to(resourceClass).in(getScope(resourceClass)), dc);
        }
        bindingCache.add(resourceClass);
    }

    private static Class<? extends Annotation> getScope(Class<?> resourceClass) {
        final Collection<Class<? extends Annotation>> scopes =
                ReflectionHelper.getAnnotationTypes(resourceClass, Scope.class);

        return scopes.isEmpty() ? RequestScoped.class : scopes.iterator().next();
    }

    /**
     * Bind a resource class in a HK2 context.
     *
     * The bound resource class is internally cached to make sure any sub-sequent attempts to bind the
     * class are silently ignored.
     * <p>
     * WARNING: This version of method is not synchronized as well as the cache is not checked for existing
     * bindings before the resource is bound and cached.
     * </p>
     *
     * @param <T>           resource class type.
     * @param resourceClass resource class to be bound.
     * @param providerModel provider model for the class. If not {@code null}, the class
     *                      wil be bound as a contract provider too.
     * @param dc            dynamic HK2 service locator configuration.
     */
    public <T> void unsafeBindResource(
            final Class<T> resourceClass,
            final org.glassfish.jersey.model.ContractProvider providerModel,
            final DynamicConfiguration dc) {

        if (providerModel != null) {
            final Class<? extends Annotation> scope = providerModel.getScope();
            final ActiveDescriptor<?> descriptor =
                    dc.bind(BuilderHelper.activeLink(resourceClass).to(resourceClass).in(scope).build());

            for (Class contract : providerModel.getContracts()) {
                @SuppressWarnings("unchecked")
                AliasDescriptor aliasDescriptor = new AliasDescriptor(locator, descriptor, contract.getName(), null);
                aliasDescriptor.setScope(scope.getName());
                aliasDescriptor.setRanking(providerModel.getPriority(contract));
                aliasDescriptor.addQualifierAnnotation(CustomAnnotationLiteral.INSTANCE);

                dc.bind(aliasDescriptor);
            }
        } else {
            Injections.addBinding(Injections.newBinder(resourceClass).to(resourceClass).in(getScope(resourceClass)), dc);
        }
        bindingCache.add(resourceClass);
    }

    @Override
    public ResourceModel getResourceModel() {
        return this.resourceModel;
    }

    /**
     * Set the {@link ResourceModel resource mode} of the application associated with this context.
     * @param resourceModel Resource model on which the {@link org.glassfish.jersey.server.ApplicationHandler application}
     *                      is based.
     */
    public void setResourceModel(ResourceModel resourceModel) {
        this.resourceModel = resourceModel;
    }

    /**
     * Injection binder for {@link JerseyResourceContext}.
     */
    public static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindAsContract(JerseyResourceContext.class).to(ResourceContext.class).to(ExtendedResourceContext.class)
                    .in(Singleton.class);
        }
    }

}
