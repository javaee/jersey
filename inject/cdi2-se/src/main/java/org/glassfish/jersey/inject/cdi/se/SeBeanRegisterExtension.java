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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.WithAnnotations;

import org.glassfish.jersey.inject.cdi.se.bean.BeanHelper;
import org.glassfish.jersey.inject.cdi.se.injector.JerseyInjectionTarget;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.internal.inject.InjectionResolverBinding;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;

import org.jboss.weld.injection.producer.BasicInjectionTarget;

/**
 * CDI extension that handles CDI bootstrap events and registers Jersey's internally used components and components registered
 * using {@link Application}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
class SeBeanRegisterExtension implements Extension {

    private final AbstractBinder bindings;

    private final List<JerseyInjectionTarget> jerseyInjectionTargets = new ArrayList<>();

    /**
     * Creates a new extension that registers Jersey's internally used components and components registered using
     * {@link Application}.
     * <p>
     * TODO: Probably will be changed during the migration from CDI SE to JAVA EE environment.
     *
     * @param bindings all register beans using Jersey.
     */
    SeBeanRegisterExtension(AbstractBinder bindings) {
        this.bindings = bindings;
    }

    /**
     * Ignores the classes which are manually added using bindings (through {@link Application} class) and scanned by CDI.
     * The manual adding is privileged and the beans scanned using CDI are ignored.
     * <p>
     * TODO: The method counts with the condition that the all bindings are known before the CDI scanning has been started,
     * can be changed during the migration from CDI SE to JAVA EE environment.
     *
     * @param pat processed type.
     * @param <T> type of the scanned bean.
     */
    public <T> void ignoreManuallyRegisteredComponents(
            @Observes @WithAnnotations({ Path.class, Provider.class }) ProcessAnnotatedType<T> pat) {
        for (Binding binding : bindings.getBindings()) {
            if (ClassBinding.class.isAssignableFrom(binding.getClass())) {
                ClassBinding<?> classBinding = (ClassBinding<?>) binding;
                if (pat.getAnnotatedType().getJavaClass() == classBinding.getService()) {
                    pat.veto();
                    return;
                }
            } else if (InstanceBinding.class.isAssignableFrom(binding.getClass())) {
                InstanceBinding<?> instanceBinding = (InstanceBinding<?>) binding;
                if (pat.getAnnotatedType().getJavaClass() == instanceBinding.getService().getClass()) {
                    pat.veto();
                    return;
                }
            }
        }
    }

    /**
     * Wraps all JAR-RS components by Jersey-specific injection target.
     *
     * @param pit process injection target.
     * @param <T> type of the processed injection target.
     */
    public <T> void observeInjectionTarget(@Observes ProcessInjectionTarget<T> pit) {
        BasicInjectionTarget<T> it = (BasicInjectionTarget<T>) pit.getInjectionTarget();
        JerseyInjectionTarget<T> jerseyInjectionTarget =
                new JerseyInjectionTarget<>(it, pit.getAnnotatedType().getJavaClass());
        jerseyInjectionTargets.add(jerseyInjectionTarget);
        pit.setInjectionTarget(jerseyInjectionTarget);
    }

    /**
     * Takes all registered bindings and registers them in {@link BeanManager}.
     * <p>
     * Method should register only Jersey internal components and class/instances registered using {@link Application}. Registered
     * classes/instances have priority therefore CDI scanning should veto these classes/instances during {
     *
     * @param abd         {@code AfterBeanDiscovery} event.
     * @param beanManager current {@code BeanManager}.
     * @link ProcessAnnotatedType} bootstrap phase.
     */
    public void registerBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        Collection<Binding> bindings = this.bindings.getBindings();

        /*
         * Filters out only registered InjectionResolvers.
         */
        List<InjectionResolver> injectionResolvers = bindings.stream()
                .filter(binding -> InjectionResolverBinding.class.isAssignableFrom(binding.getClass()))
                .map(InjectionResolverBinding.class::cast)
                .map(InjectionResolverBinding::getResolver)
                .collect(Collectors.toList());

        /*
         * Provide registered InjectionResolvers to Jersey's components which has been discovered by CDI in
         * ProcessInjectionTarget bootstrap phase.
         */
        jerseyInjectionTargets.forEach(injectionTarget -> injectionTarget.setInjectionResolvers(injectionResolvers));

        for (Binding binding : bindings) {
            if (ClassBinding.class.isAssignableFrom(binding.getClass())) {
                BeanHelper.registerBean((ClassBinding<?>) binding, abd, injectionResolvers, beanManager);

            } else if (InstanceBinding.class.isAssignableFrom(binding.getClass())) {
                BeanHelper.registerBean((InstanceBinding<?>) binding, abd, injectionResolvers);

            } else if (SupplierClassBinding.class.isAssignableFrom(binding.getClass())) {
                BeanHelper.registerSupplier((SupplierClassBinding<?>) binding, abd, injectionResolvers, beanManager);

            } else if (SupplierInstanceBinding.class.isAssignableFrom(binding.getClass())) {
                BeanHelper.registerSupplier((SupplierInstanceBinding<?>) binding, abd, beanManager);
            }
        }

        abd.addBean(new RequestScopeBean(beanManager));
    }
}