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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.glassfish.jersey.internal.guava.Preconditions.checkArgument;
import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;
import static org.glassfish.jersey.internal.guava.Preconditions.checkState;
import static org.glassfish.jersey.internal.guava.Predicates.in;

/**
 * This class contains static utility methods that operate on or return objects
 * of type {@link Iterator}. Except as noted, each method has a corresponding
 * {@link Iterable}-based method in the {@link Iterables} class.
 * <p>
 * <p><i>Performance notes:</i> Unless otherwise noted, all of the iterators
 * produced in this class are <i>lazy</i>, which means that they only advance
 * the backing iteration when absolutely necessary.
 * <p>
 * <p>See the Guava User Guide section on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Iterables">
 * {@code Iterators}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
public final class Iterators {
    private static final UnmodifiableListIterator<Object> EMPTY_LIST_ITERATOR
            = new UnmodifiableListIterator<Object>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }
    };
    private static final Iterator<Object> EMPTY_MODIFIABLE_ITERATOR =
            new Iterator<Object>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Object next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    CollectPreconditions.checkRemove(false);
                }
            };

    private Iterators() {
    }

    /**
     * Returns the empty iterator.
     * <p>
     * <p>The {@link Iterable} equivalent of this method is {@link
     * ImmutableSet#of()}.
     *
     * @deprecated Use {@code ImmutableSet.<T>of().iterator()} instead; or for
     * Java 7 or later, {@link Collections#emptyIterator}. This method is
     * scheduled for removal in May 2016.
     */
    @Deprecated
    public static <T> UnmodifiableIterator<T> emptyIterator() {
        return emptyListIterator();
    }

    /**
     * Returns the empty iterator.
     * <p>
     * <p>The {@link Iterable} equivalent of this method is {@link
     * ImmutableSet#of()}.
     */
    // Casting to any type is safe since there are no actual elements.
    @SuppressWarnings("unchecked")
    private static <T> UnmodifiableListIterator<T> emptyListIterator() {
        return (UnmodifiableListIterator<T>) EMPTY_LIST_ITERATOR;
    }

    /**
     * Returns the empty {@code Iterator} that throws
     * {@link IllegalStateException} instead of
     * {@link UnsupportedOperationException} on a call to
     * {@link Iterator#remove()}.
     */
    // Casting to any type is safe since there are no actual elements.
    @SuppressWarnings("unchecked")
    static <T> Iterator<T> emptyModifiableIterator() {
        return (Iterator<T>) EMPTY_MODIFIABLE_ITERATOR;
    }

    /**
     * Returns an unmodifiable view of {@code iterator}.
     */
    public static <T> UnmodifiableIterator<T> unmodifiableIterator(
            final Iterator<T> iterator) {
        checkNotNull(iterator);
        if (iterator instanceof UnmodifiableIterator) {
            return (UnmodifiableIterator<T>) iterator;
        }
        return new UnmodifiableIterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        };
    }

    /**
     * Returns the number of elements remaining in {@code iterator}. The iterator
     * will be left exhausted: its {@code hasNext()} method will return
     * {@code false}.
     */
    public static int size(Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    /**
     * Traverses an iterator and removes every element that belongs to the
     * provided collection. The iterator will be left exhausted: its
     * {@code hasNext()} method will return {@code false}.
     *
     * @param removeFrom       the iterator to (potentially) remove elements from
     * @param elementsToRemove the elements to remove
     * @return {@code true} if any element was removed from {@code iterator}
     */
    public static boolean removeAll(
            Iterator<?> removeFrom, Collection<?> elementsToRemove) {
        return removeIf(removeFrom, in(elementsToRemove));
    }

    /**
     * Removes every element that satisfies the provided predicate from the
     * iterator. The iterator will be left exhausted: its {@code hasNext()}
     * method will return {@code false}.
     *
     * @param removeFrom the iterator to (potentially) remove elements from
     * @param predicate  a predicate that determines whether an element should
     *                   be removed
     * @return {@code true} if any elements were removed from the iterator
     * @since 2.0
     */
    public static <T> boolean removeIf(
            Iterator<T> removeFrom, Predicate<? super T> predicate) {
        checkNotNull(predicate);
        boolean modified = false;
        while (removeFrom.hasNext()) {
            if (predicate.test(removeFrom.next())) {
                removeFrom.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Determines whether two iterators contain equal elements in the same order.
     * More specifically, this method returns {@code true} if {@code iterator1}
     * and {@code iterator2} contain the same number of elements and every element
     * of {@code iterator1} is equal to the corresponding element of
     * {@code iterator2}.
     * <p>
     * <p>Note that this will modify the supplied iterators, since they will have
     * been advanced some number of elements forward.
     */
    public static boolean elementsEqual(
            Iterator<?> iterator1, Iterator<?> iterator2) {
        while (iterator1.hasNext()) {
            if (!iterator2.hasNext()) {
                return false;
            }
            Object o1 = iterator1.next();
            Object o2 = iterator2.next();
            if (!Objects.equals(o1, o2)) {
                return false;
            }
        }
        return !iterator2.hasNext();
    }

    /**
     * Adds all elements in {@code iterator} to {@code collection}. The iterator
     * will be left exhausted: its {@code hasNext()} method will return
     * {@code false}.
     *
     * @return {@code true} if {@code collection} was modified as a result of this
     * operation
     */
    public static <T> boolean addAll(
            Collection<T> addTo, Iterator<? extends T> iterator) {
        checkNotNull(addTo);
        checkNotNull(iterator);
        boolean wasModified = false;
        while (iterator.hasNext()) {
            wasModified |= addTo.add(iterator.next());
        }
        return wasModified;
    }

    /**
     * Returns {@code true} if every element returned by {@code iterator}
     * satisfies the given predicate. If {@code iterator} is empty, {@code true}
     * is returned.
     */
    public static <T> boolean all(
            Iterator<T> iterator, Predicate<? super T> predicate) {
        checkNotNull(predicate);
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (!predicate.test(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index in {@code iterator} of the first element that satisfies
     * the provided {@code predicate}, or {@code -1} if the Iterator has no such
     * elements.
     * <p>
     * <p>More formally, returns the lowest index {@code i} such that
     * {@code predicate.apply(Iterators.get(iterator, i))} returns {@code true},
     * or {@code -1} if there is no such index.
     * <p>
     * <p>If -1 is returned, the iterator will be left exhausted: its
     * {@code hasNext()} method will return {@code false}.  Otherwise,
     * the iterator will be set to the element which satisfies the
     * {@code predicate}.
     *
     * @since 2.0
     */
    private static <T> int indexOf(
            Iterator<T> iterator, Predicate<? super T> predicate) {
        checkNotNull(predicate, "predicate");
        for (int i = 0; iterator.hasNext(); i++) {
            T current = iterator.next();
            if (predicate.test(current)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns an iterator that applies {@code function} to each element of {@code
     * fromIterator}.
     * <p>
     * <p>The returned iterator supports {@code remove()} if the provided iterator
     * does. After a successful {@code remove()} call, {@code fromIterator} no
     * longer contains the corresponding element.
     */
    public static <F, T> Iterator<T> transform(final Iterator<F> fromIterator,
                                               final Function<? super F, ? extends T> function) {
        checkNotNull(function);
        return new TransformedIterator<F, T>(fromIterator) {
            @Override
            T transform(F from) {
                return function.apply(from);
            }
        };
    }

    /**
     * Returns the next element in {@code iterator} or {@code defaultValue} if
     * the iterator is empty.  The {@link Iterables} analog to this method is
     * {@link Iterables#getFirst}.
     *
     * @param defaultValue the default value to return if the iterator is empty
     * @return the next element of {@code iterator} or the default value
     * @since 7.0
     */
    public static <T> T getNext(Iterator<? extends T> iterator, T defaultValue) {
        return iterator.hasNext() ? iterator.next() : defaultValue;
    }

    /**
     * Deletes and returns the next value from the iterator, or returns
     * {@code null} if there is no such value.
     */
    static <T> T pollNext(Iterator<T> iterator) {
        if (iterator.hasNext()) {
            T result = iterator.next();
            iterator.remove();
            return result;
        } else {
            return null;
        }
    }

    // Methods only in Iterators, not in Iterables

    /**
     * Clears the iterator using its remove method.
     */
    static void clear(Iterator<?> iterator) {
        checkNotNull(iterator);
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    /**
     * Returns an iterator containing the elements of {@code array} in order. The
     * returned iterator is a view of the array; subsequent changes to the array
     * will be reflected in the iterator.
     * <p>
     * <p><b>Note:</b> It is often preferable to represent your data using a
     * collection type, for example using {@link Arrays#asList(Object[])}, making
     * this method unnecessary.
     * <p>
     * <p>The {@code Iterable} equivalent of this method is either {@link
     * Arrays#asList(Object[])}, {@link ImmutableList#copyOf(Object[])}},
     * or {@link ImmutableList#of}.
     */
    public static <T> UnmodifiableIterator<T> forArray(final T... array) {
        return forArray(array, 0, array.length, 0);
    }

    /**
     * Returns a list iterator containing the elements in the specified range of
     * {@code array} in order, starting at the specified index.
     * <p>
     * <p>The {@code Iterable} equivalent of this method is {@code
     * Arrays.asList(array).subList(offset, offset + length).listIterator(index)}.
     */
    static <T> UnmodifiableListIterator<T> forArray(
            final T[] array, final int offset, int length, int index) {
        checkArgument(length >= 0);
        int end = offset + length;

        // Technically we should give a slightly more descriptive error on overflow
        Preconditions.checkPositionIndexes(offset, end, array.length);
        Preconditions.checkPositionIndex(index, length);
        if (length == 0) {
            return emptyListIterator();
        }

    /*
     * We can't use call the two-arg constructor with arguments (offset, end)
     * because the returned Iterator is a ListIterator that may be moved back
     * past the beginning of the iteration.
     */
        return new AbstractIndexedListIterator<T>(length, index) {
            @Override
            protected T get(int index) {
                return array[offset + index];
            }
        };
    }

    /**
     * Returns an iterator containing only {@code value}.
     * <p>
     * <p>The {@link Iterable} equivalent of this method is {@link
     * Collections#singleton}.
     */
    public static <T> UnmodifiableIterator<T> singletonIterator(
            final T value) {
        return new UnmodifiableIterator<T>() {
            boolean done;

            @Override
            public boolean hasNext() {
                return !done;
            }

            @Override
            public T next() {
                if (done) {
                    throw new NoSuchElementException();
                }
                done = true;
                return value;
            }
        };
    }

    /**
     * Returns a {@code PeekingIterator} backed by the given iterator.
     * <p>
     * <p>Calls to the {@code peek} method with no intervening calls to {@code
     * next} do not affect the iteration, and hence return the same object each
     * time. A subsequent call to {@code next} is guaranteed to return the same
     * object again. For example: <pre>   {@code
     * <p>
     *   PeekingIterator<String> peekingIterator =
     *       Iterators.peekingIterator(Iterators.forArray("a", "b"));
     *   String a1 = peekingIterator.peek(); // returns "a"
     *   String a2 = peekingIterator.peek(); // also returns "a"
     *   String a3 = peekingIterator.next(); // also returns "a"}</pre>
     * <p>
     * <p>Any structural changes to the underlying iteration (aside from those
     * performed by the iterator's own {@link PeekingIterator#remove()} method)
     * will leave the iterator in an undefined state.
     * <p>
     * <p>The returned iterator does not support removal after peeking, as
     * explained by {@link PeekingIterator#remove()}.
     * <p>
     * <p>Note: If the given iterator is already a {@code PeekingIterator},
     * it <i>might</i> be returned to the caller, although this is neither
     * guaranteed to occur nor required to be consistent.  For example, this
     * method <i>might</i> choose to pass through recognized implementations of
     * {@code PeekingIterator} when the behavior of the implementation is
     * known to meet the contract guaranteed by this method.
     * <p>
     * <p>There is no {@link Iterable} equivalent to this method, so use this
     * method to wrap each individual iterator as it is generated.
     *
     * @param iterator the backing iterator. The {@link PeekingIterator} assumes
     *                 ownership of this iterator, so users should cease making direct calls
     *                 to it after calling this method.
     * @return a peeking iterator backed by that iterator. Apart from the
     * additional {@link PeekingIterator#peek()} method, this iterator behaves
     * exactly the same as {@code iterator}.
     */
    public static <T> PeekingIterator<T> peekingIterator(
            Iterator<? extends T> iterator) {
        if (iterator instanceof PeekingImpl) {
            // Safe to cast <? extends T> to <T> because PeekingImpl only uses T
            // covariantly (and cannot be subclassed to add non-covariant uses).
            @SuppressWarnings("unchecked")
            PeekingImpl<T> peeking = (PeekingImpl<T>) iterator;
            return peeking;
        }
        return new PeekingImpl<T>(iterator);
    }

    /**
     * Implementation of PeekingIterator that avoids peeking unless necessary.
     */
    private static class PeekingImpl<E> implements PeekingIterator<E> {

        private final Iterator<? extends E> iterator;
        private boolean hasPeeked;
        private E peekedElement;

        public PeekingImpl(Iterator<? extends E> iterator) {
            this.iterator = checkNotNull(iterator);
        }

        @Override
        public boolean hasNext() {
            return hasPeeked || iterator.hasNext();
        }

        @Override
        public E next() {
            if (!hasPeeked) {
                return iterator.next();
            }
            E result = peekedElement;
            hasPeeked = false;
            peekedElement = null;
            return result;
        }

        @Override
        public void remove() {
            checkState(!hasPeeked, "Can't remove after you've peeked at next");
            iterator.remove();
        }

        @Override
        public E peek() {
            if (!hasPeeked) {
                peekedElement = iterator.next();
                hasPeeked = true;
            }
            return peekedElement;
        }
    }
}
