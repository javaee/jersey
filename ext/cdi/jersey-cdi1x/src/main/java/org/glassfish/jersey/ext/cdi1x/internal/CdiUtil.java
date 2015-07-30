/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.ext.cdi1x.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Qualifier;

import org.glassfish.jersey.ext.cdi1x.internal.spi.BeanManagerProvider;
import org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2LocatorManager;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;

/**
 * Common CDI utility methods.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Michal Gajdos
 */
public final class CdiUtil {

    private static final BeanManagerProvider BEAN_MANAGER_PROVIDER = new DefaultBeanManagerProvider();

    /**
     * Prevent instantiation.
     */
    private CdiUtil() {
        throw new AssertionError("No instances allowed.");
    }

    /**
     * Get me list of qualifiers included in given annotation list.
     *
     * @param annotations list of annotations to introspect
     * @return annotations from the input list that are marked as qualifiers
     */
    public static Annotation[] getQualifiers(final Annotation[] annotations) {
        final List<Annotation> result = new ArrayList<>(annotations.length);
        for (final Annotation a : annotations) {
            if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                result.add(a);
            }
        }
        return result.toArray(new Annotation[result.size()]);
    }

    /**
     * Get me current bean manager. Method first tries to lookup available providers via {@code META-INF/services}. If not found
     * the bean manager is returned from the default provider.
     *
     * @return bean manager
     */
    public static BeanManager getBeanManager() {
        final BeanManagerProvider provider = lookupService(BeanManagerProvider.class);
        if (provider != null) {
            return provider.getBeanManager();
        }

        return BEAN_MANAGER_PROVIDER.getBeanManager();
    }

    /**
     * Create new instance of {@link org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2LocatorManager}. Method first tries to lookup
     * available manager via {@code META-INF/services} and if not found a new instance of default one is returned.
     *
     * @return an instance of locator manager.
     */
    static Hk2LocatorManager createHk2LocatorManager() {
        final Hk2LocatorManager manager = lookupService(Hk2LocatorManager.class);
        return manager != null ? manager : new SingleHk2LocatorManager();
    }

    /**
     * Look for a service of given type. If more then one service is found the method sorts them are returns the one with highest
     * priority.
     *
     * @param clazz type of service to look for.
     * @param <T>   type of service to look for
     * @return instance of service with highest priority or {@code null} if service of given type cannot be found.
     * @see javax.annotation.Priority
     */
    static <T> T lookupService(final Class<T> clazz) {
        final List<RankedProvider<T>> providers = new LinkedList<>();

        for (final T provider : ServiceFinder.find(clazz)) {
            providers.add(new RankedProvider<>(provider));
        }
        Collections.sort(providers, new RankedComparator<T>(RankedComparator.Order.ASCENDING));

        return providers.isEmpty() ? null : providers.get(0).getProvider();
    }

    /**
     * Obtain a bean reference of given type from the bean manager.
     *
     * @param clazz         type of the bean to get reference to.
     * @param bean          the {@link Bean} object representing the managed bean.
     * @param beanManager   bean manager used to obtain an instance of the requested bean.
     * @param <T>           type of the bean to be returned.
     * @return a bean reference or {@code null} if a bean instance cannot be found.
     */
    static <T> T getBeanReference(final Class<T> clazz, final Bean bean, final BeanManager beanManager) {
        final CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
        final Object result = beanManager.getReference(bean, clazz, creationalContext);

        return clazz.cast(result);
    }

    /**
     * Get me scope of a bean corresponding to given class.
     *
     * @param beanClass bean class in question.
     * @param beanManager actual bean manager.
     * @return actual bean scope or null, if the scope could not be determined.
     */
    public static Class<? extends Annotation> getBeanScope(final Class<?> beanClass, final BeanManager beanManager) {
        final Set<Bean<?>> beans = beanManager.getBeans(beanClass);
        if (beans.isEmpty()) {
            return null;
        }
        for (Bean b : beans) {
            return b.getScope();
        }
        return null;
    }
}
