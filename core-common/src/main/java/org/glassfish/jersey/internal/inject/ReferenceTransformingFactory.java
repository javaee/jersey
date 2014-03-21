/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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

import javax.inject.Provider;

import org.glassfish.jersey.internal.util.collection.Ref;

import org.glassfish.hk2.api.Factory;

/**
 * An abstract injection factory that provides injection of an instance of target type {@code T}
 * by {@link org.glassfish.jersey.internal.inject.ReferenceTransformingFactory.Transformer transforming}
 * a value of injected source reference {@link org.glassfish.jersey.internal.util.collection.Ref Ref&lt;S&gt;}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 *
 * @param <S> the type of the injected source type {@link Ref reference}.
 * @param <T> the type of provided entity.
 */
public abstract class ReferenceTransformingFactory<S, T> implements Factory<T> {
    /**
     * Transforming function responsible for transforming an instance of source type {@code S} into an instance of
     * target type {@code T}.
     *
     * @param <S> source type.
     * @param <T> target type.
     */
    public static interface Transformer<S, T> {
        /**
         * Transform an instance of source type into an instance of target type.
         *
         * @param value instance of source type.
         * @return instance of target type.
         */
        T transform(S value);
    }

    private final Provider<Ref<S>> refProvider;
    private final Transformer<S, T> transformer;

    /**
     * Initialize reference transforming factory.
     *
     * @param refProvider source type reference provider.
     * @param transformer source to target type transforming function.
     */
    protected ReferenceTransformingFactory(final Provider<Ref<S>> refProvider, final Transformer<S, T> transformer) {
        this.refProvider = refProvider;
        this.transformer = transformer;
    }

    @Override
    public T provide() {
        return transformer.transform(refProvider.get().get());
    }

    @Override
    public void dispose(T instance) {
        // not used
    }
}
