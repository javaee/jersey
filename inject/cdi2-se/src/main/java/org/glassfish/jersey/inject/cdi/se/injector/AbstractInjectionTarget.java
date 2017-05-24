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

package org.glassfish.jersey.inject.cdi.se.injector;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * Abstract class which implements all methods from {@link InjectionTarget} by invoking the same methods on the delegate object.
 * Useful super class to extend and override only the needed method.
 *
 * @param <T> type of the injection target.
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
abstract class AbstractInjectionTarget<T> implements InjectionTarget<T> {

    /**
     * Object on which all calls will be delegated.
     *
     * @return injection target.
     */
    abstract InjectionTarget<T> delegate();

    @Override
    public void inject(final T instance, final CreationalContext<T> ctx) {
        delegate().inject(instance, ctx);
    }

    @Override
    public void postConstruct(final T instance) {
        delegate().postConstruct(instance);
    }

    @Override
    public void preDestroy(final T instance) {
        delegate().preDestroy(instance);
    }

    @Override
    public T produce(final CreationalContext<T> ctx) {
        return delegate().produce(ctx);
    }

    @Override
    public void dispose(final T instance) {
        delegate().dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return delegate().getInjectionPoints();
    }
}