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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import static org.glassfish.jersey.internal.guava.Preconditions.checkElementIndex;
import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;
import static org.glassfish.jersey.internal.guava.Preconditions.checkPositionIndex;
import static org.glassfish.jersey.internal.guava.Preconditions.checkPositionIndexes;
import static org.glassfish.jersey.internal.guava.Preconditions.checkState;

/**
 * Static utility methods pertaining to {@link List} instances. Also see this
 * class's counterparts {@link Sets}, {@link Maps} and {@link Queues}.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Lists">
 * {@code Lists}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0 (imported from Google Collections Library)
 */
public final class Lists {
    private Lists() {
    }

    // ArrayList

    /**
     * Creates a <i>mutable</i>, empty {@code ArrayList} instance (for Java 6 and
     * earlier).
     * <p>
     * <p><b>Note:</b> if mutability is not required, use {@link
     * ImmutableList#of()} instead.
     * <p>
     * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and
     * should be treated as deprecated. Instead, use the {@code ArrayList}
     * {@linkplain ArrayList#ArrayList() constructor} directly, taking advantage
     * of the new <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
     */
    private static <E> ArrayList<E> newArrayList() {
        return new ArrayList<E>();
    }

    /**
     * Creates a <i>mutable</i> {@code ArrayList} instance containing the given
     * elements; a very thin shortcut for creating an empty list then calling
     * {@link Iterables#addAll}.
     * <p>
     * <p><b>Note:</b> if mutability is not required and the elements are
     * non-null, use {@link ImmutableList#copyOf(Iterable)} instead. (Or, change
     * {@code elements} to be a {@link FluentIterable} and call
     * {@code elements.toList()}.)
     * <p>
     * <p><b>Note for Java 7 and later:</b> if {@code elements} is a {@link
     * Collection}, you don't need this method. Use the {@code ArrayList}
     * {@linkplain ArrayList#ArrayList(Collection) constructor} directly, taking
     * advantage of the new <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
     */
    public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
        checkNotNull(elements); // for GWT
        // Let ArrayList's sizing logic work, if possible
        return (elements instanceof Collection)
                ? new ArrayList<E>(Collections2.cast(elements))
                : newArrayList(elements.iterator());
    }

    /**
     * Creates a <i>mutable</i> {@code ArrayList} instance containing the given
     * elements; a very thin shortcut for creating an empty list and then calling
     * {@link Iterators#addAll}.
     * <p>
     * <p><b>Note:</b> if mutability is not required and the elements are
     * non-null, use {@link ImmutableList#copyOf(Iterator)} instead.
     */
    public static <E> ArrayList<E> newArrayList(Iterator<? extends E> elements) {
        ArrayList<E> list = newArrayList();
        Iterators.addAll(list, elements);
        return list;
    }

    /**
     * Returns a reversed view of the specified list. For example, {@code
     * Lists.reverse(Arrays.asList(1, 2, 3))} returns a list containing {@code 3,
     * 2, 1}. The returned list is backed by this list, so changes in the returned
     * list are reflected in this list, and vice-versa. The returned list supports
     * all of the optional list operations supported by this list.
     * <p>
     * <p>The returned list is random-access if the specified list is random
     * access.
     *
     * @since 7.0
     */
    private static <T> List<T> reverse(List<T> list) {
        if (list instanceof ReverseList) {
            return ((ReverseList<T>) list).getForwardList();
        } else {
            return new ReverseList<T>(list);
        }
    }

    /**
     * An implementation of {@link List#equals(Object)}.
     */
    static boolean equalsImpl(List<?> list, Object object) {
        if (object == checkNotNull(list)) {
            return true;
        }
        if (!(object instanceof List)) {
            return false;
        }

        List<?> o = (List<?>) object;

        return list.size() == o.size()
                && Iterators.elementsEqual(list.iterator(), o.iterator());
    }

    /**
     * An implementation of {@link List#indexOf(Object)}.
     */
    static int indexOfImpl(List<?> list, Object element) {
        ListIterator<?> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            if (Objects.equals(element, listIterator.next())) {
                return listIterator.previousIndex();
            }
        }
        return -1;
    }

    /**
     * An implementation of {@link List#lastIndexOf(Object)}.
     */
    static int lastIndexOfImpl(List<?> list, Object element) {
        ListIterator<?> listIterator = list.listIterator(list.size());
        while (listIterator.hasPrevious()) {
            if (Objects.equals(element, listIterator.previous())) {
                return listIterator.nextIndex();
            }
        }
        return -1;
    }

    private static class ReverseList<T> extends AbstractList<T> {
        private final List<T> forwardList;

        ReverseList(List<T> forwardList) {
            this.forwardList = checkNotNull(forwardList);
        }

        List<T> getForwardList() {
            return forwardList;
        }

        private int reverseIndex(int index) {
            int size = size();
            checkElementIndex(index, size);
            return (size - 1) - index;
        }

        private int reversePosition(int index) {
            int size = size();
            checkPositionIndex(index, size);
            return size - index;
        }

        @Override
        public void add(int index, T element) {
            forwardList.add(reversePosition(index), element);
        }

        @Override
        public void clear() {
            forwardList.clear();
        }

        @Override
        public T remove(int index) {
            return forwardList.remove(reverseIndex(index));
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            subList(fromIndex, toIndex).clear();
        }

        @Override
        public T set(int index, T element) {
            return forwardList.set(reverseIndex(index), element);
        }

        @Override
        public T get(int index) {
            return forwardList.get(reverseIndex(index));
        }

        @Override
        public int size() {
            return forwardList.size();
        }

        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            checkPositionIndexes(fromIndex, toIndex, size());
            return reverse(forwardList.subList(
                    reversePosition(toIndex), reversePosition(fromIndex)));
        }

        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            int start = reversePosition(index);
            final ListIterator<T> forwardIterator = forwardList.listIterator(start);
            return new ListIterator<T>() {

                boolean canRemoveOrSet;

                @Override
                public void add(T e) {
                    forwardIterator.add(e);
                    forwardIterator.previous();
                    canRemoveOrSet = false;
                }

                @Override
                public boolean hasNext() {
                    return forwardIterator.hasPrevious();
                }

                @Override
                public boolean hasPrevious() {
                    return forwardIterator.hasNext();
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    canRemoveOrSet = true;
                    return forwardIterator.previous();
                }

                @Override
                public int nextIndex() {
                    return reversePosition(forwardIterator.nextIndex());
                }

                @Override
                public T previous() {
                    if (!hasPrevious()) {
                        throw new NoSuchElementException();
                    }
                    canRemoveOrSet = true;
                    return forwardIterator.next();
                }

                @Override
                public int previousIndex() {
                    return nextIndex() - 1;
                }

                @Override
                public void remove() {
                    CollectPreconditions.checkRemove(canRemoveOrSet);
                    forwardIterator.remove();
                    canRemoveOrSet = false;
                }

                @Override
                public void set(T e) {
                    checkState(canRemoveOrSet);
                    forwardIterator.set(e);
                }
            };
        }
    }
}
