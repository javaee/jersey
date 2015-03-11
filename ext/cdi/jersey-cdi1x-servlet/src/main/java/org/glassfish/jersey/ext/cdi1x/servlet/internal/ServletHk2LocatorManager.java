/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.ext.cdi1x.servlet.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;

import org.glassfish.jersey.ext.cdi1x.internal.CdiUtil;
import org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2InjectedTarget;
import org.glassfish.jersey.ext.cdi1x.internal.spi.InjectionTargetListener;
import org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2LocatorManager;
import org.glassfish.jersey.internal.util.collection.DataStructures;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * {@link org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2LocatorManager Locator manager} for servlet based containers. The provider
 * enables WAR and EAR to be deployed on a servlet container and be properly injected.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 * @since 2.17
 */
public class ServletHk2LocatorManager implements Hk2LocatorManager, InjectionTargetListener {

    private final ConcurrentMap<String, ServiceLocator> locatorsByContextPath;
    private final ConcurrentMap<ClassLoader, List<Hk2InjectedTarget>> injectionTargetsByAppClassLoader;

    private volatile BeanManager beanManager;

    private volatile ServiceLocator locator;
    private volatile ServletContext servletContext;

    private volatile boolean multipleLocators = false;

    public ServletHk2LocatorManager() {
        locatorsByContextPath = DataStructures.createConcurrentMap();
        injectionTargetsByAppClassLoader = DataStructures.createConcurrentMap();
    }

    @Override
    public void registerLocator(final ServiceLocator locator) {
        if (this.locator == null) {
            this.locator = locator;
        } else {
            multipleLocators = true;
            // We will get a dynamic proxy here.
            servletContext = CdiUtil.getBeanReference(ServletContext.class, getBeanManager());
        }

        // Store given locator under particular context path.
        final javax.servlet.ServletConfig hk2ServletConfig = locator.getService(javax.servlet.ServletConfig.class);
        final ServletContext hk2ServletContext = hk2ServletConfig != null
                ? hk2ServletConfig.getServletContext()                      // servlet
                : locator.getService(javax.servlet.ServletContext.class);   // servlet filter

        locatorsByContextPath.put(hk2ServletContext.getContextPath(), locator);

        // Set effective locator to all injection targets with the same class loader.
        final ClassLoader webappClassLoader = hk2ServletContext.getClassLoader();
        final List<Hk2InjectedTarget> targets = injectionTargetsByAppClassLoader.get(webappClassLoader);
        if (targets != null) {
            for (final Hk2InjectedTarget target : targets) {
                target.setLocator(locator);
            }
        }
    }

    private BeanManager getBeanManager() {
        if (beanManager == null) {
            beanManager = CdiUtil.getBeanManager();
        }
        return beanManager;
    }

    @Override
    public ServiceLocator getEffectiveLocator() {
        return locator == null
                ? null
                : !multipleLocators ? locator : locatorsByContextPath.get(servletContext.getContextPath());
    }

    @Override
    public void notify(final Hk2InjectedTarget target) {
        final List<Hk2InjectedTarget> newList = new LinkedList<>();
        final List<Hk2InjectedTarget> existingList = injectionTargetsByAppClassLoader
                .putIfAbsent(target.getInjectionTargetClassLoader(), newList);

        ((existingList != null) ? existingList : newList).add(target);
    }
}
