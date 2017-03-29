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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * A comparator, with additional methods to support common operations. This is
 * an "enriched" version of {@code Comparator}, in the same sense that {@link
 * FluentIterable} is an enriched {@link Iterable}.
 * <p>
 * <p>The common ways to get an instance of {@code Ordering} are:
 * <p>
 * <ul>
 * <li>Subclass it and implement {@link #compare} instead of implementing
 * {@link Comparator} directly
 * <li>Pass a <i>pre-existing</i> {@link Comparator} instance to {@link
 * #from(Comparator)}
 * <li>Use the natural ordering, {@link Ordering#natural}
 * </ul>
 * <p>
 * <p>Then you can use the <i>chaining</i> methods to get an altered version of
 * that {@code Ordering}, including:
 * <p>
 * <ul>
 * <li>{@link #reverse}
 * <li>{@link #compound(Comparator)}
 * <li>{@link #onResultOf(Function)}
 * <li>{@link #nullsFirst} / {@link #nullsLast}
 * </ul>
 * <p>
 * <p>Finally, use the resulting {@code Ordering} anywhere a {@link Comparator}
 * is required, or use any of its special operations, such as:</p>
 * <p>
 * <ul>
 * <li>{@link #immutableSortedCopy}
 * <li>{@link #isOrdered} / {@link #isStrictlyOrdered}
 * <li>{@link #min} / {@link #max}
 * </ul>
 * <p>
 * <p>Except as noted, the orderings returned by the factory methods of this
 * class are serializable if and only if the provided instances that back them
 * are. For example, if {@code ordering} and {@code function} can themselves be
 * serialized, then {@code ordering.onResultOf(function)} can as well.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/OrderingExplained">
 * {@code Ordering}</a>.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
public abstract class Ordering<T> implements Comparator<T> {
    // Natural order

    // Never make these public
    static final int LEFT_IS_GREATER = 1;

    // Static factories
    static final int RIGHT_IS_GREATER = -1;

    /**
     * Constructs a new instance of this class (only invokable by the subclass
     * constructor, typically implicit).
     */
    Ordering() {
    }

    // Instance-based factories (and any static equivalents)

    /**
     * Returns a serializable ordering that uses the natural order of the values.
     * The ordering throws a {@link NullPointerException} when passed a null
     * parameter.
     * <p>
     * <p>The type specification is {@code <C extends Comparable>}, instead of
     * the technically correct {@code <C extends Comparable<? super C>>}, to
     * support legacy types from before Java 5.
     */
    @SuppressWarnings("unchecked") // TODO(kevinb): right way to explain this??
    public static <C extends Comparable> Ordering<C> natural() {
        return (Ordering<C>) NaturalOrdering.INSTANCE;
    }

    /**
     * Returns an ordering based on an <i>existing</i> comparator instance. Note
     * that it is unnecessary to create a <i>new</i> anonymous inner class
     * implementing {@code Comparator} just to pass it in here. Instead, simply
     * subclass {@code Ordering} and implement its {@code compare} method
     * directly.
     *
     * @param comparator the comparator that defines the order
     * @return comparator itself if it is already an {@code Ordering}; otherwise
     * an ordering that wraps that comparator
     */
    public static <T> Ordering<T> from(Comparator<T> comparator) {
        return (comparator instanceof Ordering)
                ? (Ordering<T>) comparator
                : new ComparatorOrdering<T>(comparator);
    }

    /**
     * Returns the reverse of this ordering; the {@code Ordering} equivalent to
     * {@link Collections#reverseOrder(Comparator)}.
     */
    // type parameter <S> lets us avoid the extra <String> in statements like:
    // Ordering<String> o = Ordering.<String>natural().reverse();
    public <S extends T> Ordering<S> reverse() {
        return new ReverseOrdering<S>(this);
    }

    /**
     * Returns an ordering that treats {@code null} as less than all other values
     * and uses {@code this} to compare non-null values.
     */
    // type parameter <S> lets us avoid the extra <String> in statements like:
    // Ordering<String> o = Ordering.<String>natural().nullsFirst();
    <S extends T> Ordering<S> nullsFirst() {
        return new NullsFirstOrdering<S>(this);
    }

    /**
     * Returns an ordering that treats {@code null} as greater than all other
     * values and uses this ordering to compare non-null values.
     */
    // type parameter <S> lets us avoid the extra <String> in statements like:
    // Ordering<String> o = Ordering.<String>natural().nullsLast();
    <S extends T> Ordering<S> nullsLast() {
        return new NullsLastOrdering<S>(this);
    }

    // Regular instance methods

    // Override to add @Nullable
    @Override
    public abstract int compare(T left, T right);

    /**
     * Returns the least of the specified values according to this ordering. If
     * there are multiple least values, the first of those is returned. The
     * iterator will be left exhausted: its {@code hasNext()} method will return
     * {@code false}.
     *
     * @param iterator the iterator whose minimum element is to be determined
     * @throws NoSuchElementException if {@code iterator} is empty
     * @throws ClassCastException     if the parameters are not <i>mutually
     *                                comparable</i> under this ordering.
     * @since 11.0
     */
    <E extends T> E min(Iterator<E> iterator) {
        // let this throw NoSuchElementException as necessary
        E minSoFar = iterator.next();

        while (iterator.hasNext()) {
            minSoFar = min(minSoFar, iterator.next());
        }

        return minSoFar;
    }

    /**
     * Returns the least of the specified values according to this ordering. If
     * there are multiple least values, the first of those is returned.
     *
     * @param iterable the iterable whose minimum element is to be determined
     * @throws NoSuchElementException if {@code iterable} is empty
     * @throws ClassCastException     if the parameters are not <i>mutually
     *                                comparable</i> under this ordering.
     */
    <E extends T> E min(Iterable<E> iterable) {
        return min(iterable.iterator());
    }

    /**
     * Returns the lesser of the two values according to this ordering. If the
     * values compare as 0, the first is returned.
     * <p>
     * <p><b>Implementation note:</b> this method is invoked by the default
     * implementations of the other {@code min} overloads, so overriding it will
     * affect their behavior.
     *
     * @param a value to compare, returned if less than or equal to b.
     * @param b value to compare.
     * @throws ClassCastException if the parameters are not <i>mutually
     *                            comparable</i> under this ordering.
     */
    <E extends T> E min(E a, E b) {
        return (compare(a, b) <= 0) ? a : b;
    }

    /**
     * Returns the least of the specified values according to this ordering. If
     * there are multiple least values, the first of those is returned.
     *
     * @param a    value to compare, returned if less than or equal to the rest.
     * @param b    value to compare
     * @param c    value to compare
     * @param rest values to compare
     * @throws ClassCastException if the parameters are not <i>mutually
     *                            comparable</i> under this ordering.
     */
    <E extends T> E min(
            E a, E b, E c, E... rest) {
        E minSoFar = min(min(a, b), c);

        for (E r : rest) {
            minSoFar = min(minSoFar, r);
        }

        return minSoFar;
    }

    /**
     * Returns the greatest of the specified values according to this ordering. If
     * there are multiple greatest values, the first of those is returned. The
     * iterator will be left exhausted: its {@code hasNext()} method will return
     * {@code false}.
     *
     * @param iterator the iterator whose maximum element is to be determined
     * @throws NoSuchElementException if {@code iterator} is empty
     * @throws ClassCastException     if the parameters are not <i>mutually
     *                                comparable</i> under this ordering.
     * @since 11.0
     */
    <E extends T> E max(Iterator<E> iterator) {
        // let this throw NoSuchElementException as necessary
        E maxSoFar = iterator.next();

        while (iterator.hasNext()) {
            maxSoFar = max(maxSoFar, iterator.next());
        }

        return maxSoFar;
    }

    /**
     * Returns the greatest of the specified values according to this ordering. If
     * there are multiple greatest values, the first of those is returned.
     *
     * @param iterable the iterable whose maximum element is to be determined
     * @throws NoSuchElementException if {@code iterable} is empty
     * @throws ClassCastException     if the parameters are not <i>mutually
     *                                comparable</i> under this ordering.
     */
    <E extends T> E max(Iterable<E> iterable) {
        return max(iterable.iterator());
    }

    /**
     * Returns the greater of the two values according to this ordering. If the
     * values compare as 0, the first is returned.
     * <p>
     * <p><b>Implementation note:</b> this method is invoked by the default
     * implementations of the other {@code max} overloads, so overriding it will
     * affect their behavior.
     *
     * @param a value to compare, returned if greater than or equal to b.
     * @param b value to compare.
     * @throws ClassCastException if the parameters are not <i>mutually
     *                            comparable</i> under this ordering.
     */
    <E extends T> E max(E a, E b) {
        return (compare(a, b) >= 0) ? a : b;
    }

    /**
     * Returns the greatest of the specified values according to this ordering. If
     * there are multiple greatest values, the first of those is returned.
     *
     * @param a    value to compare, returned if greater than or equal to the rest.
     * @param b    value to compare
     * @param c    value to compare
     * @param rest values to compare
     * @throws ClassCastException if the parameters are not <i>mutually
     *                            comparable</i> under this ordering.
     */
    <E extends T> E max(
            E a, E b, E c, E... rest) {
        E maxSoFar = max(max(a, b), c);

        for (E r : rest) {
            maxSoFar = max(maxSoFar, r);
        }

        return maxSoFar;
    }

}
