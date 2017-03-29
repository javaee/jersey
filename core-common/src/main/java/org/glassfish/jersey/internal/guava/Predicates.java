/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.jersey.internal.guava;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * Static utility methods pertaining to {@code Predicate} instances.
 * <p>
 * <p>All methods returns serializable predicates as long as they're given
 * serializable parameters.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/FunctionalExplained">the
 * use of {@code Predicate}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
public final class Predicates {
    private static final Joiner COMMA_JOINER = Joiner.on();

    // TODO(kevinb): considering having these implement a VisitablePredicate
    // interface which specifies an accept(PredicateVisitor) method.

    private Predicates() {
    }

    /**
     * Returns a predicate that always evaluates to {@code true}.
     */
    public static <T> Predicate<T> alwaysTrue() {
        return ObjectPredicate.ALWAYS_TRUE.withNarrowedType();
    }

    /**
     * Returns a predicate that evaluates to {@code true} if the object reference
     * being tested is null.
     */
    private static <T> Predicate<T> isNull() {
        return ObjectPredicate.IS_NULL.withNarrowedType();
    }

    /**
     * Returns a predicate that evaluates to {@code true} if the given predicate
     * evaluates to {@code false}.
     */
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return new NotPredicate<T>(predicate);
    }

    /**
     * Returns a predicate that evaluates to {@code true} if the object being
     * tested {@code equals()} the given target or both are null.
     */
    public static <T> Predicate<T> equalTo(T target) {
        return (target == null)
                ? Predicates.isNull()
                : new IsEqualToPredicate<T>(target);
    }

    /**
     * Returns a predicate that evaluates to {@code true} if the object reference
     * being tested is a member of the given collection. It does not defensively
     * copy the collection passed in, so future changes to it will alter the
     * behavior of the predicate.
     * <p>
     * <p>This method can technically accept any {@code Collection<?>}, but using
     * a typed collection helps prevent bugs. This approach doesn't block any
     * potential users since it is always possible to use {@code
     * Predicates.<Object>in()}.
     *
     * @param target the collection that may contain the function input
     */
    public static <T> Predicate<T> in(Collection<? extends T> target) {
        return new InPredicate<T>(target);
    }

    // End public API, begin private implementation classes.

    /**
     * Returns the composition of a function and a predicate. For every {@code x},
     * the generated predicate returns {@code predicate(function(x))}.
     *
     * @return the composition of the provided function and predicate
     */
    public static <A, B> Predicate<A> compose(
            Predicate<B> predicate, Function<A, ? extends B> function) {
        return new CompositionPredicate<A, B>(predicate, function);
    }

    // Package private for GWT serialization.
    enum ObjectPredicate implements Predicate<Object> {
        /**
         * @see Predicates#alwaysTrue()
         */
        ALWAYS_TRUE {
            @Override
            public boolean test(Object o) {
                return true;
            }

            @Override
            public String toString() {
                return "Predicates.alwaysTrue()";
            }
        },
        /**
         * @see Predicates#isNull()
         */
        IS_NULL {
            @Override
            public boolean test(Object o) {
                return o == null;
            }

            @Override
            public String toString() {
                return "Predicates.isNull()";
            }
        };

        @SuppressWarnings("unchecked")
            // safe contravariant cast
        <T> Predicate<T> withNarrowedType() {
            return (Predicate<T>) this;
        }
    }

    /**
     * @see Predicates#not(Predicate)
     */
    private static class NotPredicate<T> implements Predicate<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Predicate<T> predicate;

        NotPredicate(Predicate<T> predicate) {
            this.predicate = Preconditions.checkNotNull(predicate);
        }

        @Override
        public boolean test(T t) {
            return !predicate.test(t);
        }

        @Override
        public int hashCode() {
            return ~predicate.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NotPredicate) {
                NotPredicate<?> that = (NotPredicate<?>) obj;
                return predicate.equals(that.predicate);
            }
            return false;
        }

        @Override
        public String toString() {
            return "Predicates.not(" + predicate.toString() + ")";
        }
    }

    /**
     * @see Predicates#equalTo(Object)
     */
    private static class IsEqualToPredicate<T>
            implements Predicate<T>, Serializable {
        private static final long serialVersionUID = 0;
        private final T target;

        private IsEqualToPredicate(T target) {
            this.target = target;
        }

        @Override
        public boolean test(T t) {
            return target.equals(t);
        }

        @Override
        public int hashCode() {
            return target.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IsEqualToPredicate) {
                IsEqualToPredicate<?> that = (IsEqualToPredicate<?>) obj;
                return target.equals(that.target);
            }
            return false;
        }

        @Override
        public String toString() {
            return "Predicates.equalTo(" + target + ")";
        }
    }

    /**
     * @see Predicates#in(Collection)
     */
    private static class InPredicate<T> implements Predicate<T>, Serializable {
        private static final long serialVersionUID = 0;
        private final Collection<?> target;

        private InPredicate(Collection<?> target) {
            this.target = Preconditions.checkNotNull(target);
        }

        @Override
        public boolean test(T t) {
            try {
                return target.contains(t);
            } catch (NullPointerException e) {
                return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof InPredicate) {
                InPredicate<?> that = (InPredicate<?>) obj;
                return target.equals(that.target);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return target.hashCode();
        }

        @Override
        public String toString() {
            return "Predicates.in(" + target + ")";
        }
    }

    /**
     * @see Predicates#compose(Predicate, Function)
     */
    private static class CompositionPredicate<A, B>
            implements Predicate<A>, Serializable {
        private static final long serialVersionUID = 0;
        final Predicate<B> p;
        final Function<A, ? extends B> f;

        private CompositionPredicate(Predicate<B> p, Function<A, ? extends B> f) {
            this.p = Preconditions.checkNotNull(p);
            this.f = Preconditions.checkNotNull(f);
        }

        @Override
        public boolean test(A a) {
            return p.test(f.apply(a));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CompositionPredicate) {
                CompositionPredicate<?, ?> that = (CompositionPredicate<?, ?>) obj;
                return f.equals(that.f) && p.equals(that.p);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return f.hashCode() ^ p.hashCode();
        }

        @Override
        public String toString() {
            return p.toString() + "(" + f.toString() + ")";
        }
    }

}
