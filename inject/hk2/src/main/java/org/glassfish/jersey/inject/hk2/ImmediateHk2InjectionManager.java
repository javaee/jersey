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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

/**
 * Implementation of {@link org.glassfish.jersey.internal.inject.InjectionManager} that is able to register and inject services.
 *
 * @author Petr Bouda
 */
public class ImmediateHk2InjectionManager extends AbstractHk2InjectionManager {

    /**
     * Constructor with parent.
     *
     * @param parent parent of type {@link org.glassfish.jersey.internal.inject.InjectionManager} or {@link ServiceLocator}.
     */
    ImmediateHk2InjectionManager(Object parent) {
        super(parent);
    }

    @Override
    public void completeRegistration() throws IllegalStateException {
        // No-op method.
    }

    @Override
    public void register(Binding binding) {
        Hk2Helper.bind(getServiceLocator(), binding);
    }

    @Override
    public void register(Iterable<Binding> descriptors) {
        Hk2Helper.bind(getServiceLocator(), descriptors);
    }

    @Override
    public void register(Binder binder) {
        Hk2Helper.bind(this, binder);
    }

    @Override
    public void register(Object provider) {
        if (isRegistrable(provider.getClass())) {
            ServiceLocatorUtilities.bind(getServiceLocator(), (org.glassfish.hk2.utilities.Binder) provider);
        } else {
            throw new IllegalArgumentException(LocalizationMessages.HK_2_PROVIDER_NOT_REGISTRABLE(provider.getClass()));
        }
    }
}
