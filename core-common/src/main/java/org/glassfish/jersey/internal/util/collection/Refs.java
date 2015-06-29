/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.util.collection;

import jersey.repackaged.com.google.common.base.MoreObjects;

/**
 * A collection of {@link Ref reference} factory & utility methods.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class Refs {

    private Refs() {
        // preventing instantiation
    }

    /**
     * Immutable {@link Ref} implementation.
     */
    private static final class ImmutableRefImpl<T> implements Ref<T> {

        private final T reference;

        public ImmutableRefImpl(final T value) {
            this.reference = value;
        }

        @Override
        public T get() {
            return reference;
        }

        @Override
        public void set(final T value) throws IllegalStateException {
            throw new IllegalStateException("This implementation of Ref interface is immutable.");
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("reference", reference).toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Ref)) {
                return false;
            }

            Object otherRef = ((Ref) obj).get();
            return this.reference == otherRef || (this.reference != null && this.reference.equals(otherRef));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + (this.reference != null ? this.reference.hashCode() : 0);
            return hash;
        }
    }

    /**
     * Default (mutable) {@link Ref} implementation. This implementation is not thread-safe.
     */
    private static final class DefaultRefImpl<T> implements Ref<T> {

        private T reference;

        public DefaultRefImpl() {
            this.reference = null;
        }

        public DefaultRefImpl(final T value) {
            this.reference = value;
        }

        @Override
        public T get() {
            return reference;
        }

        @Override
        public void set(final T value) throws IllegalStateException {
            this.reference = value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("reference", reference).toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Ref)) {
                return false;
            }

            Object otherRef = ((Ref) obj).get();
            T ref = this.reference;
            return ref == otherRef || (ref != null && ref.equals(otherRef));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + (this.reference != null ? this.reference.hashCode() : 0);
            return hash;
        }
    }

    /**
     * Thread-safe {@link Ref} implementation.
     */
    private static final class ThreadSafeRefImpl<T> implements Ref<T> {

        private volatile T reference;

        public ThreadSafeRefImpl() {
            this.reference = null;
        }

        public ThreadSafeRefImpl(final T value) {
            this.reference = value;
        }

        @Override
        public T get() {
            return reference;
        }

        @Override
        public void set(final T value) throws IllegalStateException {
            this.reference = value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("reference", reference).toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Ref)) {
                return false;
            }

            Object otherRef = ((Ref) obj).get();
            T localRef = this.reference;
            return localRef == otherRef || (localRef != null && localRef.equals(otherRef));
        }

        @Override
        public int hashCode() {
            T localRef = this.reference;
            int hash = 5;
            hash = 47 * hash + (localRef != null ? localRef.hashCode() : 0);
            return hash;
        }
    }

    /**
     * Construct a new mutable {@link Ref} instance referencing the given
     * input value.
     *
     * @param <T>   type of the referenced instance.
     * @param value value of the newly constructed reference.
     * @return a new mutable {@link Ref} instance referencing the given
     *         input value.
     */
    public static <T> Ref<T> of(final T value) {
        return new DefaultRefImpl<T>(value);
    }

    /**
     * Construct a new empty mutable {@link Ref} instance.
     *
     * @param <T> type of the referenced instance.
     * @return a new mutable empty {@link Ref} instance.
     */
    public static <T> Ref<T> emptyRef() {
        return new DefaultRefImpl<T>();
    }

    /**
     * Construct a new empty mutable thread-safe {@link Ref} instance.
     *
     * @param <T> type of the referenced instance.
     * @return a new mutable empty thread-safe {@link Ref} instance.
     * @since 2.8
     */
    public static <T> Ref<T> threadSafe() {
        return new ThreadSafeRefImpl<T>();
    }

    /**
     * Construct a new mutable thread-safe {@link Ref} instance referencing the given
     * input value.
     *
     * @param <T> type of the referenced instance.
     * @param value value of the newly constructed reference.
     * @return a new mutable thread-safe {@link Ref} instance  referencing the given
     *         input value.
     * @since 2.8
     */
    public static <T> Ref<T> threadSafe(final T value) {
        return new ThreadSafeRefImpl<T>(value);
    }

    /**
     * Construct a new immutable {@link Ref} instance referencing the given
     * input value.
     * <p/>
     * Invoking a {@link Ref#set(java.lang.Object)} on the returned instance
     * will result in a {@link IllegalStateException} being thrown.
     *
     * @param <T>   type of the referenced instance.
     * @param value value of the newly constructed reference.
     * @return a new immutable {@link Ref} instance referencing the given
     *         input value.
     */
    public static <T> Ref<T> immutableRef(final T value) {
        return new ImmutableRefImpl<T>(value);
    }
}
