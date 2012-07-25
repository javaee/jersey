/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

/**
 * A collection of {@link Value Value provider} factory & utility methods.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class Values {
    private static final Value EMPTY = new Value() {
        @Override
        public Object get() {
            return null;
        }
    };

    private Values() {
        // prevents instantiation.
    }

    /**
     * Get an empty {@link Value value provider} whose {@link Value#get() get()}
     * method always returns {@code null}.
     *
     * @param <T> value type.
     * @return empty value provider.
     */
    public static <T> Value<T> empty() {
        //noinspection unchecked
        return (Value<T>) EMPTY;
    }

    /**
     * Get a new constant {@link Value value provider} whose {@link Value#get() get()}
     * method always returns the instance supplied to the {@code value} parameter.
     *
     * In case the supplied value constant is {@code null}, an {@link #empty() empty} value
     * provider is returned.
     *
     * @param <T>   value type.
     * @param value value instance to be provided.
     * @return constant value provider.
     */
    public static <T> Value<T> of(final T value) {
        return (value == null) ? Values.<T>empty() : new InstanceValue<T>(value);
    }

    private static class InstanceValue<T> implements Value<T> {
        private final T value;

        public InstanceValue(final T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return value.equals(((InstanceValue) o).value);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "InstanceValue{value=" + value + '}';
        }
    }

    /**
     * Get a new lazily initialized {@link Value value provider}.
     *
     * The value returned by its {@link Value#get() get()} method is lazily retrieved during a first
     * call to the method from the supplied {@code delegate} value provider and is then cached for
     * a subsequent retrieval.
     * <p>
     * The implementation of the returned lazy value provider is thread-safe and is guaranteed to
     * invoke the {@code get()} method on the supplied {@code delegate} value provider instance at
     * most once.
     * </p>
     * <p>
     * If the supplied value provider is {@code null}, an {@link #empty() empty} value
     * provider is returned.
     * </p>
     *
     * @param <T>      value type.
     * @param delegate value provider delegate that will be used to lazily initialize the value provider.
     * @return lazily initialized value provider.
     */
    public static <T> Value<T> lazy(final Value<T> delegate) {
        return (delegate == null) ? Values.<T>empty() : new LazyValue<T>(delegate);
    }

    private static class LazyValue<T> implements Value<T> {
        private final Object lock;
        private final Value<T> delegate;

        private volatile T value;

        public LazyValue(final Value<T> delegate) {
            this.delegate = delegate;
            this.lock = new Object();
        }

        @Override
        public T get() {
            T result = value;
            if (result == null) {
                synchronized (lock) {
                    result = value;
                    //noinspection ConstantConditions
                    if (result == null) {
                        value = result = delegate.get();
                    }
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return delegate.equals(((LazyValue) o).delegate);
        }

        @Override
        public int hashCode() {
            return delegate != null ? delegate.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "LazyValue{delegate=" + delegate.toString() + '}';
        }
    }
}
