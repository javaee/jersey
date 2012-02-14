/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.inject;

import org.glassfish.hk2.Binder;
import org.glassfish.hk2.BinderFactory;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.NamedBinder;
import org.glassfish.hk2.TypeLiteral;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Skeleton implementation of injection module with convenience methods for
 * binding definitions.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractModule implements Module, BinderFactory {

    private transient BinderFactory binderFactory;

    @Override
    public void configure(BinderFactory binderFactory) {
        checkState(this.binderFactory == null, "Recursive configuration call detected.");

        this.binderFactory = checkNotNull(binderFactory, "binderFactory");
        try {
            configure();
        } finally {
            this.binderFactory = null;
        }
    }

    /**
     * Implement to provide module binding definitions using the exposed binding
     * methods.
     */
    protected abstract void configure();

    /**
     * Get the active {@link BinderFactory binder factory} instance used for
     * binding configuration. This method can only be called from within the
     * scope of the {@link #configure()} method.
     *
     * @return binder factory instance used for binding configuration.
     * @throws IllegalStateException in case the method is not called from within
     *     an active call to {@link #configure()} method.
     */
    private BinderFactory binderFactory() {
        checkState(binderFactory != null, "Binder factory accessed from outside of an active module configuration scope.");
        return binderFactory;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public final BinderFactory inParent() {
        return binderFactory().inParent();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public final Binder<Object> bind(String contractName) {
        return binderFactory().bind(contractName);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public final Binder<Object> bind(String... contractNames) {
        return binderFactory().bind(contractNames);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public final <T> Binder<T> bind(Class<T> contract, Class<?>... contracts) {
        return binderFactory().bind(contract, contracts);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public final <T> Binder<T> bind(TypeLiteral<T> typeLiteral) {
        return binderFactory().bind(typeLiteral);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public final NamedBinder<Object> bind() {
        return binderFactory().bind();
    }

    /**
     * Adds all binding definitions from the modules to the binding configuration.
     *
     * @param modules modules whose binding definitions should be configured.
     */
    public final void install(Module... modules) {
        for (Module module : modules) {
            module.configure(this);
        }
    }
}
