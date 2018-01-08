/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.ext.cdi1x.internal.spi.InjectionManagerInjectedTarget;
import org.glassfish.jersey.ext.cdi1x.internal.spi.InjectionManagerStore;
import org.glassfish.jersey.ext.cdi1x.internal.spi.InjectionTargetListener;
import org.glassfish.jersey.internal.inject.InjectionManager;

/**
 * Generic {@link InjectionManagerStore injection manager store} that allows multiple
 * injection managers to run in parallel. {@link #lookupInjectionManager()}
 * method must be implemented that shall be utilized at runtime in the case that more than a single
 * injection manager has been registered.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @since 2.20
 */
public abstract class GenericInjectionManagerStore implements InjectionManagerStore, InjectionTargetListener {

    private final List<InjectionManagerInjectedTarget> injectionTargets;

    private volatile InjectionManager injectionManager;

    private volatile boolean multipleInjectionManagers = false;

    public GenericInjectionManagerStore() {
        injectionTargets = new LinkedList<>();
    }

    @Override
    public void registerInjectionManager(final InjectionManager injectionManager) {
        if (!multipleInjectionManagers) {
            if (this.injectionManager == null) { // first one
                this.injectionManager = injectionManager;
            } else { // second one
                this.injectionManager = null;
                multipleInjectionManagers = true;
            } // first and second case
        }

        // pass the injection manager to registered injection targets anyway
        for (final InjectionManagerInjectedTarget target : injectionTargets) {
            target.setInjectionManager(injectionManager);
        }
    }

    @Override
    public InjectionManager getEffectiveInjectionManager() {
        return !multipleInjectionManagers ? injectionManager : lookupInjectionManager();
    }

    /**
     * CDI container specific method to obtain the actual injection manager
     * belonging to the Jersey application where the current HTTP requests
     * is being processed.
     *
     * @return actual injection manager.
     */
    public abstract InjectionManager lookupInjectionManager();

    @Override
    public void notify(final InjectionManagerInjectedTarget target) {

        injectionTargets.add(target);
    }
}
