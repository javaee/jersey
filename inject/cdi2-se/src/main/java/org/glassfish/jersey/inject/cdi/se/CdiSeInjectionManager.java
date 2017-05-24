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

package org.glassfish.jersey.inject.cdi.se;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Unmanaged;

import org.glassfish.jersey.inject.cdi.se.bean.JerseyBean;
import org.glassfish.jersey.inject.cdi.se.injector.ContextInjectionResolverImpl;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;

/**
 * Implementation of {@link InjectionManager} that starts CDI SE container, scans all users bean according to beans.xml file and
 * register Jersey's internal beans using {@link SeBeanRegisterExtension} extension.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class CdiSeInjectionManager implements InjectionManager {

    // Keeps all binders and bindings added to the InjectionManager during the bootstrap.
    private final AbstractBinder bindings = new AbstractBinder() {
        @Override
        protected void configure() {
        }
    };

    private SeContainer container;

    private BeanManager beanManager;

    @Override
    public void register(Binding binding) {
        bindings.bind(binding);
    }

    @Override
    public void register(Iterable<Binding> bindings) {
        for (Binding binding : bindings) {
            this.bindings.bind(binding);
        }
    }

    @Override
    public void register(Binder binder) {
        for (Binding binding : Bindings.getBindings(this, binder)) {
            bindings.bind(binding);
        }
    }

    @Override
    public void register(Object provider) throws IllegalArgumentException {
        throw new IllegalArgumentException(LocalizationMessages.CDI_2_PROVIDER_NOT_REGISTRABLE(provider.getClass()));
    }

    @Override
    public boolean isRegistrable(Class<?> clazz) {
        return false;
    }

    @Override
    public <T> T createAndInitialize(Class<T> createMe) {
        if (container != null) {
            Unmanaged.UnmanagedInstance<T> unmanaged = new Unmanaged<>(createMe).newInstance();
            return unmanaged.produce()
                    .inject()
                    .postConstruct()
                    .get();
        } else {
            // TODO: method is invoked before #completeRegistration - creates AutoDiscoverable, ForcedAutoDiscoverable.
            // Hack: creates an object with default constructor and without an injection.
            try {
                Constructor<T> constructor = createMe.getConstructor();
                return constructor.newInstance();
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException("Cannot create an instance of a class: " + createMe, e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contractOrImpl, Annotation... qualifiers) {
        List<ServiceHolder<T>> result = new ArrayList<>();
        for (Bean<?> bean : beanManager.getBeans(contractOrImpl, qualifiers)) {
            CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
            T reference = (T) beanManager.getReference(bean, contractOrImpl, ctx);

            int rank = 1;
            if (bean instanceof JerseyBean) {
                rank = ((JerseyBean) bean).getRank();
            }

            result.add(new ServiceHolderImpl<>(reference, (Class<T>) bean.getBeanClass(), bean.getTypes(), rank));
        }
        return result;
    }

    @Override
    public <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers) {
        return getInstanceInternal(contractOrImpl, qualifiers);
    }

    @Override
    public <T> T getInstance(Class<T> contractOrImpl) {
        return getInstanceInternal(contractOrImpl);
    }

    @Override
    public <T> T getInstance(Type contractOrImpl) {
        return getInstanceInternal(contractOrImpl);
    }

    @SuppressWarnings("unchecked")
    private <T> T getInstanceInternal(Type contractOrImpl, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(contractOrImpl, qualifiers);
        if (beans.isEmpty()) {
            return null;
        }

        Bean<?> bean = beans.iterator().next();
        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, contractOrImpl, ctx);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getInstance(ForeignDescriptor foreignDescriptor) {
        Bean bean = (Bean) foreignDescriptor.get();
        CreationalContext ctx = beanManager.createCreationalContext(bean);
        return bean.create(ctx);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ForeignDescriptor createForeignDescriptor(Binding binding) {
        Class<?> clazz;
        if (ClassBinding.class.isAssignableFrom(binding.getClass())) {
            clazz = ((ClassBinding<?>) binding).getService();
        } else if (InstanceBinding.class.isAssignableFrom(binding.getClass())) {
            clazz = ((InstanceBinding<?>) binding).getService().getClass();
        } else {
            throw new RuntimeException(
                    org.glassfish.jersey.internal.LocalizationMessages
                    .UNKNOWN_DESCRIPTOR_TYPE(binding.getClass().getSimpleName()));
        }

        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        if (beans.isEmpty()) {
            return null;
        }

        Bean bean = beans.iterator().next();
        CreationalContext ctx = beanManager.createCreationalContext(bean);
        return ForeignDescriptor.wrap(bean, instance -> bean.destroy(instance, ctx));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getAllInstances(Type contractOrImpl) {
        List<T> result = new ArrayList<>();
        for (Bean<?> bean : beanManager.getBeans(contractOrImpl)) {
            CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
            Object reference = beanManager.getReference(bean, contractOrImpl, ctx);
            result.add((T) reference);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void inject(Object instance) {
        if (beanManager != null) {
            AnnotatedType annotatedType = beanManager.createAnnotatedType((Class) instance.getClass());
            InjectionTarget injectionTarget = beanManager.createInjectionTarget(annotatedType);
            CreationalContext context = beanManager.createCreationalContext(null);
            injectionTarget.inject(instance, context);
        }
    }

    @Override
    public void preDestroy(Object preDestroyMe) {
        this.container.destroy(preDestroyMe);
    }

    @Override
    public void completeRegistration() throws IllegalStateException {
        bindings.bind(Bindings.service(this).to(InjectionManager.class));
        bindings.install(new ContextInjectionResolverImpl.Binder(this::getBeanManager));

        SeContainerInitializer containerInitializer = SeContainerInitializer.newInstance();
        containerInitializer.addExtensions(new SeBeanRegisterExtension(bindings));
        this.container = containerInitializer.initialize();
        this.beanManager = container.getBeanManager();
    }

    protected AbstractBinder getBindings() {
        return bindings;
    }

    public SeContainer getContainer() {
        return container;
    }

    public void setContainer(SeContainer container) {
        this.container = container;
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }

    public void setBeanManager(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @Override
    public void shutdown() {
        if (container != null && container.isRunning()) {
            container.close();
        }
    }

    @Override
    public void inject(Object injectMe, String classAnalyzer) {
        // TODO: Used only in legacy CDI integration.
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer) {
        // TODO: Used only in legacy CDI integration.
        throw new UnsupportedOperationException();
    }
}
