/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

    private static final LazyValue EMPTY = new LazyValue() {
        @Override
        public Object get() {
            return null;
        }

        @Override
        public boolean isInitialized() {
            return true;
        }
    };

    private static final LazyUnsafeValue EMPTY_UNSAFE = new LazyUnsafeValue() {
        @Override
        public Object get() {
            return null;
        }

        @Override
        public boolean isInitialized() {
            return true;
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
     * Get an empty {@link UnsafeValue value provider} whose {@link UnsafeValue#get() get()}
     * method always returns {@code null}.
     *
     * @param <T> value type.
     * @return empty unsafe value provider.
     */
    public static <T, E extends Throwable> UnsafeValue<T, E> emptyUnsafe() {
        //noinspection unchecked
        return (UnsafeValue<T, E>) EMPTY_UNSAFE;
    }

    /**
     * Get a new constant {@link Value value provider} whose {@link Value#get() get()}
     * method always returns the instance supplied to the {@code value} parameter.
     * <p/>
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
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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
     * Get a new constant {@link UnsafeValue value provider} whose {@link UnsafeValue#get() get()}
     * method always returns the instance supplied to the {@code value} parameter.
     * <p/>
     * In case the supplied value constant is {@code null}, an {@link #emptyUnsafe() empty} value
     * provider is returned.
     *
     * @param <T>   value type.
     * @param value value instance to be provided.
     * @return constant value provider.
     */
    public static <T, E extends Throwable> UnsafeValue<T, E> unsafe(final T value) {
        return (value == null) ? Values.<T, E>emptyUnsafe() : new InstanceUnsafeValue<T, E>(value);
    }

    private static class InstanceUnsafeValue<T, E extends Throwable> implements UnsafeValue<T, E> {

        private final T value;

        public InstanceUnsafeValue(final T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return value.equals(((InstanceUnsafeValue) o).value);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "InstanceUnsafeValue{value=" + value + '}';
        }
    }

    /**
     * Get a new "throwing" {@link UnsafeValue unsafe value provider} whose {@link UnsafeValue#get() get()}
     * method always throws the exception supplied to the {@code throwable} parameter.
     * <p/>
     * In case the supplied throwable is {@code null}, an {@link NullPointerException} is thrown.
     *
     * @param <T>       value type.
     * @param <E>       exception type.
     * @param throwable throwable instance to be thrown.
     * @return "throwing" unsafe value provider.
     * @throws NullPointerException in case the supplied throwable instance is {@code null}.
     */
    public static <T, E extends Throwable> UnsafeValue<T, E> throwing(final E throwable) {
        if (throwable == null) {
            throw new NullPointerException("Supplied throwable ");
        }

        return new ExceptionValue<T, E>(throwable);
    }

    private static class ExceptionValue<T, E extends Throwable> implements UnsafeValue<T, E> {

        private final E throwable;

        public ExceptionValue(final E throwable) {
            this.throwable = throwable;
        }

        @Override
        public T get() throws E {
            throw throwable;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return throwable.equals(((ExceptionValue) o).throwable);
        }

        @Override
        public int hashCode() {
            return throwable != null ? throwable.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "ExceptionValue{throwable=" + throwable + '}';
        }
    }

    /**
     * Get a new lazily initialized {@link Value value provider}.
     * <p/>
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
    public static <T> LazyValue<T> lazy(final Value<T> delegate) {
        //noinspection unchecked
        return (delegate == null) ? (LazyValue<T>) EMPTY : new LazyValueImpl<T>(delegate);
    }

    /**
     * Get a new eagerly initialized {@link Value value provider}.
     * <p/>
     * The value returned by its {@link Value#get() get()} method is eagerly computed from the supplied
     * {@code delegate} value provider and is then stored in a final field for a subsequent retrieval.
     * <p>
     * The implementation of the returned eager value provider is thread-safe and is guaranteed to
     * invoke the {@code get()} method on the supplied {@code delegate} value provider instance once
     * and only once.
     * </p>
     * <p>
     * If the supplied value provider is {@code null}, an {@link #empty() empty} value
     * provider is returned.
     * </p>
     *
     * @param <T>      value type.
     * @param delegate value provider delegate that will be used to eagerly initialize the value provider.
     * @return eagerly initialized, constant value provider.
     */
    public static <T> Value<T> eager(final Value<T> delegate) {
        return (delegate == null) ? Values.<T>empty() : new EagerValue<T>(delegate);
    }

    private static class EagerValue<T> implements Value<T> {

        private final T result;

        private EagerValue(final Value<T> value) {
            this.result = value.get();
        }

        @Override
        public T get() {
            return result;
        }
    }

    private static class LazyValueImpl<T> implements LazyValue<T> {

        private final Object lock;
        private final Value<T> delegate;

        private volatile Value<T> value;

        public LazyValueImpl(final Value<T> delegate) {
            this.delegate = delegate;
            this.lock = new Object();
        }

        @Override
        public T get() {
            Value<T> result = value;
            if (result == null) {
                synchronized (lock) {
                    result = value;
                    if (result == null) {
                        value = result = Values.of(delegate.get());
                    }
                }
            }
            return result.get();
        }

        @Override
        public boolean isInitialized() {
            return value != null;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return delegate.equals(((LazyValueImpl) o).delegate);
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

    /**
     * Get a new lazily initialized {@link UnsafeValue unsafe value provider}.
     * <p/>
     * The value returned by its {@link UnsafeValue#get() get()} method is lazily retrieved during a first
     * call to the method from the supplied {@code delegate} value provider and is then cached for
     * a subsequent retrieval.
     * <p>
     * In case the call to underlying {@code delegate.get()} throws a throwable, the throwable is cached
     * and thrown on all subsequent retrievals.
     * </p>
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
    public static <T, E extends Throwable> LazyUnsafeValue<T, E> lazy(final UnsafeValue<T, E> delegate) {
        //noinspection unchecked
        return (delegate == null) ? (LazyUnsafeValue<T, E>) EMPTY_UNSAFE : new LazyUnsafeValueImpl<T, E>(delegate);
    }

    private static class LazyUnsafeValueImpl<T, E extends Throwable> implements LazyUnsafeValue<T, E> {

        private final Object lock;
        private final UnsafeValue<T, E> delegate;

        private volatile UnsafeValue<T, E> value;

        public LazyUnsafeValueImpl(final UnsafeValue<T, E> delegate) {
            this.delegate = delegate;
            this.lock = new Object();
        }

        @Override
        public T get() throws E {
            UnsafeValue<T, E> result = value;
            if (result == null) {
                synchronized (lock) {
                    result = value;
                    //noinspection ConstantConditions
                    if (result == null) {
                        try {
                            result = Values.unsafe(delegate.get());
                        } catch (final Throwable e) {
                            //noinspection unchecked
                            result = Values.throwing((E) e);
                        }
                        value = result;
                    }
                }
            }
            return result.get();
        }

        @Override
        public boolean isInitialized() {
            return value != null;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return delegate.equals(((LazyUnsafeValueImpl) o).delegate);
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
