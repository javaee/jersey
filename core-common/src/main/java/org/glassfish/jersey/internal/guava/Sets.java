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
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * Static utility methods pertaining to {@link Set} instances. Also see this
 * class's counterparts {@link Lists}, {@link Maps} and {@link Queues}.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Sets">
 * {@code Sets}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Chris Povirk
 * @since 2.0 (imported from Google Collections Library)
 */
public final class Sets {
    private Sets() {
    }

    /**
     * Creates a <i>mutable</i>, empty {@code HashSet} instance.
     * <p>
     * <p><b>Note:</b> if mutability is not required, use {@link
     * ImmutableSet#of()} instead.
     * <p>
     * <p><b>Note:</b> if {@code E} is an {@link Enum} type, use {@link
     * EnumSet#noneOf} instead.
     *
     * @return a new, empty {@code HashSet}
     */
    public static <E> HashSet<E> newHashSet() {
        return new HashSet<E>();
    }

    // HashSet

    /**
     * Creates a {@code HashSet} instance, with a high enough "initial capacity"
     * that it <i>should</i> hold {@code expectedSize} elements without growth.
     * This behavior cannot be broadly guaranteed, but it is observed to be true
     * for OpenJDK 1.6. It also can't be guaranteed that the method isn't
     * inadvertently <i>oversizing</i> the returned set.
     *
     * @param expectedSize the number of elements you expect to add to the
     *                     returned set
     * @return a new, empty {@code HashSet} with enough capacity to hold {@code
     * expectedSize} elements without resizing
     * @throws IllegalArgumentException if {@code expectedSize} is negative
     */
    public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
        return new HashSet<E>(Maps.capacity(expectedSize));
    }

    // LinkedHashSet

    // TreeSet

    /**
     * An implementation for {@link Set#hashCode()}.
     */
    static int hashCodeImpl(Set<?> s) {
        int hashCode = 0;
        for (Object o : s) {
            hashCode += o != null ? o.hashCode() : 0;

            hashCode = ~~hashCode;
            // Needed to deal with unusual integer overflow in GWT.
        }
        return hashCode;
    }

    /**
     * An implementation for {@link Set#equals(Object)}.
     */
    static boolean equalsImpl(Set<?> s, Object object) {
        if (s == object) {
            return true;
        }
        if (object instanceof Set) {
            Set<?> o = (Set<?>) object;

            try {
                return s.size() == o.size() && s.containsAll(o);
            } catch (NullPointerException ignored) {
                return false;
            } catch (ClassCastException ignored) {
                return false;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of the specified navigable set. This method
     * allows modules to provide users with "read-only" access to internal
     * navigable sets. Query operations on the returned set "read through" to the
     * specified set, and attempts to modify the returned set, whether direct or
     * via its collection views, result in an
     * {@code UnsupportedOperationException}.
     * <p>
     * <p>The returned navigable set will be serializable if the specified
     * navigable set is serializable.
     *
     * @param set the navigable set for which an unmodifiable view is to be
     *            returned
     * @return an unmodifiable view of the specified navigable set
     * @since 12.0
     */
    public static <E> NavigableSet<E> unmodifiableNavigableSet(
            NavigableSet<E> set) {
        return new UnmodifiableNavigableSet<E>(set);
    }

    /**
     * Remove each element in an iterable from a set.
     */
    static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
        boolean changed = false;
        while (iterator.hasNext()) {
            changed |= set.remove(iterator.next());
        }
        return changed;
    }

    static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
        checkNotNull(collection); // for GWT
    /*
     * AbstractSet.removeAll(List) has quadratic behavior if the list size
     * is just less than the set's size.  We augment the test by
     * assuming that sets have fast contains() performance, and other
     * collections don't.  See
     * http://code.google.com/p/guava-libraries/issues/detail?id=1013
     */
        if (collection instanceof Set && collection.size() > set.size()) {
            return Iterators.removeAll(set.iterator(), collection);
        } else {
            return removeAllImpl(set, collection.iterator());
        }
    }

    /**
     * {@link AbstractSet} substitute without the potentially-quadratic
     * {@code removeAll} implementation.
     */
    abstract static class ImprovedAbstractSet<E> extends AbstractSet<E> {
        @Override
        public boolean removeAll(Collection<?> c) {
            return removeAllImpl(this, c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return super.retainAll(checkNotNull(c)); // GWT compatibility
        }
    }

    static final class UnmodifiableNavigableSet<E>
            extends ForwardingSortedSet<E> implements NavigableSet<E>, Serializable {
        private static final long serialVersionUID = 0;
        private final NavigableSet<E> delegate;
        private transient UnmodifiableNavigableSet<E> descendingSet;

        UnmodifiableNavigableSet(NavigableSet<E> delegate) {
            this.delegate = checkNotNull(delegate);
        }

        @Override
        protected SortedSet<E> delegate() {
            return Collections.unmodifiableSortedSet(delegate);
        }

        @Override
        public E lower(E e) {
            return delegate.lower(e);
        }

        @Override
        public E floor(E e) {
            return delegate.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return delegate.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return delegate.higher(e);
        }

        @Override
        public E pollFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollLast() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            UnmodifiableNavigableSet<E> result = descendingSet;
            if (result == null) {
                result = descendingSet = new UnmodifiableNavigableSet<E>(
                        delegate.descendingSet());
                result.descendingSet = this;
            }
            return result;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return Iterators.unmodifiableIterator(delegate.descendingIterator());
        }

        @Override
        public NavigableSet<E> subSet(
                E fromElement,
                boolean fromInclusive,
                E toElement,
                boolean toInclusive) {
            return unmodifiableNavigableSet(delegate.subSet(
                    fromElement,
                    fromInclusive,
                    toElement,
                    toInclusive));
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return unmodifiableNavigableSet(delegate.headSet(toElement, inclusive));
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return unmodifiableNavigableSet(
                    delegate.tailSet(fromElement, inclusive));
        }
    }

}
