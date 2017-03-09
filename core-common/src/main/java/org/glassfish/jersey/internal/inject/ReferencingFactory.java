/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;

import javax.inject.Provider;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;

/**
 * Factory that provides injection of the referenced instance.
 *
 * @param <T>
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class ReferencingFactory<T> implements Supplier<T> {

    private static class EmptyReferenceFactory<T> implements Supplier<Ref<T>> {

        @Override
        public Ref<T> get() {
            return Refs.emptyRef();
        }
    }

    private static class InitializedReferenceFactory<T> implements Supplier<Ref<T>> {

        private final T initialValue;

        public InitializedReferenceFactory(T initialValue) {
            this.initialValue = initialValue;
        }

        @Override
        public Ref<T> get() {
            return Refs.of(initialValue);
        }
    }

    private final Provider<Ref<T>> referenceFactory;

    /**
     * Create new referencing injection factory.
     *
     * @param referenceFactory reference provider backing the factory.
     */
    public ReferencingFactory(Provider<Ref<T>> referenceFactory) {
        this.referenceFactory = referenceFactory;
    }

    @Override
    public T get() {
        return referenceFactory.get().get();
    }

    /**
     * Get a reference factory providing an empty reference.
     *
     * @param <T> reference type.
     * @return reference factory providing an empty reference.
     */
    public static <T> Supplier<Ref<T>> referenceFactory() {
        return new EmptyReferenceFactory<>();
    }

    /**
     * Get a reference factory providing an initialized reference.
     *
     * @param <T>          reference type.
     * @param initialValue initial value stored in the reference provided
     *                     by the returned factory.
     * @return reference factory providing a reference initialized with an
     *         {@code initialValue}.
     */
    public static <T> Supplier<Ref<T>> referenceFactory(T initialValue) {
        if (initialValue == null) {
            return new EmptyReferenceFactory<>();
        }

        return new InitializedReferenceFactory<>(initialValue);
    }
}
