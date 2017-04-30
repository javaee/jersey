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

package org.glassfish.jersey.hk2;

import java.util.ArrayList;
import java.util.List;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

/**
 * Implementation of {@link org.glassfish.jersey.internal.inject.InjectionManager} that is able to delay service's registration
 * and injection to {@link #completeRegistration()} phase. During the Jersey bootstrap just keep the bindings and other
 * operation for a later use.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class DelayedHk2InjectionManager extends AbstractHk2InjectionManager {

    // Keeps all binders and bindings added to the InjectionManager during the bootstrap.
    private final AbstractBinder bindings = new AbstractBinder() {
        @Override
        protected void configure() {
        }
    };

    // Keeps DI provider specific object for registration.
    private final List<org.glassfish.hk2.utilities.Binder> providers = new ArrayList<>();

    /**
     * Constructor without parent and initial binder.
     */
    DelayedHk2InjectionManager() {
        super();
    }

    /**
     * Constructor with parent.
     *
     * @param parent parent of type {@link org.glassfish.jersey.internal.inject.InjectionManager} or {@link ServiceLocator}.
     */
    DelayedHk2InjectionManager(Object parent) {
        super(parent);
    }

    /**
     * Constructor with initial binder.
     *
     * @param binder initial binder which will be immediately registered.
     */
    DelayedHk2InjectionManager(Binder binder) {
        super(binder);
    }

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
        for (Binding binding : binder.getBindings()) {
            bindings.bind(binding);
        }
    }

    @Override
    public void register(Object provider) throws IllegalArgumentException {
        if (isRegistrable(provider.getClass())) {
            providers.add((org.glassfish.hk2.utilities.Binder) provider);
        } else {
            throw new IllegalArgumentException(LocalizationMessages.HK_2_PROVIDER_NOT_REGISTRABLE(provider.getClass()));
        }
    }

    @Override
    public void completeRegistration() throws IllegalStateException {
        Hk2Helper.bind(getServiceLocator(), bindings);
        ServiceLocatorUtilities.bind(getServiceLocator(), providers.toArray(new org.glassfish.hk2.utilities.Binder[]{}));
    }
}
