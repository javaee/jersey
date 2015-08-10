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

package org.glassfish.jersey.ext.cdi1x.internal;

import java.util.LinkedList;
import java.util.List;

import org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2InjectedTarget;
import org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2LocatorManager;
import org.glassfish.jersey.ext.cdi1x.internal.spi.InjectionTargetListener;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Generic {@link Hk2LocatorManager Locator manager} that allows multiple
 * HK2 service locators to run in parallel. {@link #lookupLocator()}
 * method must be implemented that shall be utilized at runtime in the case that more than a single
 * HK2 service locator has been registered.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @since 2.20
 */
public abstract class GenericHk2LocatorManager implements Hk2LocatorManager, InjectionTargetListener {

    private final List<Hk2InjectedTarget> injectionTargets;

    private volatile ServiceLocator locator;

    private volatile boolean multipleLocators = false;

    public GenericHk2LocatorManager() {
        injectionTargets = new LinkedList<>();
    }

    @Override
    public void registerLocator(final ServiceLocator locator) {
        if (!multipleLocators) {
            if (this.locator == null) { // first one
                this.locator = locator;
            } else { // second one
                this.locator = null;
                multipleLocators = true;
            } // first and second case
        }

        // pass the locator to registered injection targets anyway
        for (final Hk2InjectedTarget target : injectionTargets) {
            target.setLocator(locator);
        }
    }

    @Override
    public ServiceLocator getEffectiveLocator() {
        return !multipleLocators ? locator : lookupLocator();
    }

    /**
     * CDI container specific method to obtain the actual HK2 service locator
     * belonging to the Jersey application where the current HTTP requests
     * is being processed.
     *
     * @return actual HK2 service locator.
     */
    public abstract ServiceLocator lookupLocator();

    @Override
    public void notify(final Hk2InjectedTarget target) {

        injectionTargets.add(target);
    }
}
