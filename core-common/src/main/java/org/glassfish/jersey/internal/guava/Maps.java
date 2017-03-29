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

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.glassfish.jersey.internal.guava.Joiner.MapJoiner;

import static org.glassfish.jersey.internal.guava.Preconditions.checkArgument;
import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;
import static org.glassfish.jersey.internal.guava.Predicates.compose;

/**
 * Static utility methods pertaining to {@link Map} instances (including instances of
 * {@link SortedMap}, {@link BiMap}, etc.). Also see this class's counterparts
 * {@link Lists}, {@link Sets} and {@link Queues}.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Maps">
 * {@code Maps}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Isaac Shum
 * @author Louis Wasserman
 * @since 2.0 (imported from Google Collections Library)
 */
public final class Maps {
    private static final MapJoiner STANDARD_JOINER =
            Collections2.STANDARD_JOINER.withKeyValueSeparator();

    private Maps() {
    }

    @SuppressWarnings("unchecked")
    private static <K> Function<Entry<K, ?>, K> keyFunction() {
        return (Function) EntryFunction.KEY;
    }

    @SuppressWarnings("unchecked")
    private static <V> Function<Entry<?, V>, V> valueFunction() {
        return (Function) EntryFunction.VALUE;
    }

    private static <K, V> Iterator<K> keyIterator(Iterator<Entry<K, V>> entryIterator) {
        return Iterators.transform(entryIterator, Maps.keyFunction());
    }

    static <K, V> Iterator<V> valueIterator(Iterator<Entry<K, V>> entryIterator) {
        return Iterators.transform(entryIterator, Maps.valueFunction());
    }

    /**
     * Creates a {@code HashMap} instance, with a high enough "initial capacity"
     * that it <i>should</i> hold {@code expectedSize} elements without growth.
     * This behavior cannot be broadly guaranteed, but it is observed to be true
     * for OpenJDK 1.6. It also can't be guaranteed that the method isn't
     * inadvertently <i>oversizing</i> the returned map.
     *
     * @param expectedSize the number of elements you expect to add to the
     *                     returned map
     * @return a new, empty {@code HashMap} with enough capacity to hold {@code
     * expectedSize} elements without resizing
     * @throws IllegalArgumentException if {@code expectedSize} is negative
     */
    public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(
            int expectedSize) {
        return new HashMap<K, V>(capacity(expectedSize));
    }

    /**
     * Returns a capacity that is sufficient to keep the map from being resized as
     * long as it grows no larger than expectedSize and the load factor is >= its
     * default (0.75).
     */
    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
            return expectedSize + 1;
        }
        if (expectedSize < Ints.MAX_POWER_OF_TWO) {
            return expectedSize + expectedSize / 3;
        }
        return Integer.MAX_VALUE; // any large value
    }

    static <K, V> Iterator<Entry<K, V>> asMapEntryIterator(
            Set<K> set, final Function<? super K, V> function) {
        return new TransformedIterator<K, Entry<K, V>>(set.iterator()) {
            @Override
            Entry<K, V> transform(final K key) {
                return immutableEntry(key, function.apply(key));
            }
        };
    }

    private static <E> Set<E> removeOnlySet(final Set<E> set) {
        return new ForwardingSet<E>() {
            @Override
            protected Set<E> delegate() {
                return set;
            }

            @Override
            public boolean add(E element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> es) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static <E> SortedSet<E> removeOnlySortedSet(final SortedSet<E> set) {
        return new ForwardingSortedSet<E>() {
            @Override
            protected SortedSet<E> delegate() {
                return set;
            }

            @Override
            public boolean add(E element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> es) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SortedSet<E> headSet(E toElement) {
                return removeOnlySortedSet(super.headSet(toElement));
            }

            @Override
            public SortedSet<E> subSet(E fromElement, E toElement) {
                return removeOnlySortedSet(super.subSet(fromElement, toElement));
            }

            @Override
            public SortedSet<E> tailSet(E fromElement) {
                return removeOnlySortedSet(super.tailSet(fromElement));
            }
        };
    }

    /**
     * Returns an immutable map entry with the specified key and value. The {@link
     * Entry#setValue} operation throws an {@link UnsupportedOperationException}.
     * <p>
     * <p>The returned entry is serializable.
     *
     * @param key   the key to be associated with the returned entry
     * @param value the value to be associated with the returned entry
     */
    public static <K, V> Entry<K, V> immutableEntry(
            K key, V value) {


        return new ImmutableEntry<K, V>(key, value);
    }

    static <K> Predicate<Entry<K, ?>> keyPredicateOnEntries(Predicate<? super K> keyPredicate) {
        return compose(keyPredicate, Maps.<K>keyFunction());
    }

    static <V> Predicate<Entry<?, V>> valuePredicateOnEntries(Predicate<? super V> valuePredicate) {
        return compose(valuePredicate, Maps.<V>valueFunction());
    }

    /**
     * Delegates to {@link Map#get}. Returns {@code null} on {@code
     * ClassCastException} and {@code NullPointerException}.
     */
    static <V> V safeGet(Map<?, V> map, Object key) {
        checkNotNull(map);
        try {
            return map.get(key);
        } catch (ClassCastException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Delegates to {@link Map#containsKey}. Returns {@code false} on {@code
     * ClassCastException} and {@code NullPointerException}.
     */
    static boolean safeContainsKey(Map<?, ?> map, Object key) {
        checkNotNull(map);
        try {
            return map.containsKey(key);
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Delegates to {@link Map#remove}. Returns {@code null} on {@code
     * ClassCastException} and {@code NullPointerException}.
     */
    static <V> V safeRemove(Map<?, V> map, Object key) {
        checkNotNull(map);
        try {
            return map.remove(key);
        } catch (ClassCastException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }

    private enum EntryFunction implements Function<Entry<?, ?>, Object> {
        KEY {
            @Override
            public Object apply(Entry<?, ?> entry) {
                return entry.getKey();
            }
        },
        VALUE {
            @Override
            public Object apply(Entry<?, ?> entry) {
                return entry.getValue();
            }
        }
    }

    private static class AsMapView<K, V> extends ImprovedAbstractMap<K, V> {

        final Function<? super K, V> function;
        private final Set<K> set;

        AsMapView(Set<K> set, Function<? super K, V> function) {
            this.set = checkNotNull(set);
            this.function = checkNotNull(function);
        }

        Set<K> backingSet() {
            return set;
        }

        @Override
        public Set<K> createKeySet() {
            return removeOnlySet(backingSet());
        }

        @Override
        Collection<V> createValues() {
            return Collections2.transform(set, function);
        }

        @Override
        public int size() {
            return backingSet().size();
        }

        @Override
        public boolean containsKey(Object key) {
            return backingSet().contains(key);
        }

        @Override
        public V get(Object key) {
            if (Collections2.safeContains(backingSet(), key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                        K k = (K) key;
                return function.apply(k);
            } else {
                return null;
            }
        }

        @Override
        public V remove(Object key) {
            if (backingSet().remove(key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                        K k = (K) key;
                return function.apply(k);
            } else {
                return null;
            }
        }

        @Override
        public void clear() {
            backingSet().clear();
        }

        @Override
        protected Set<Entry<K, V>> createEntrySet() {
            return new EntrySet<K, V>() {
                @Override
                Map<K, V> map() {
                    return Maps.AsMapView.this;
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return asMapEntryIterator(backingSet(), function);
                }
            };
        }
    }

    /**
     * {@code AbstractMap} extension that implements {@link #isEmpty()} as {@code
     * entrySet().isEmpty()} instead of {@code size() == 0} to speed up
     * implementations where {@code size()} is O(n), and it delegates the {@code
     * isEmpty()} methods of its key set and value collection to this
     * implementation.
     */
    abstract static class ImprovedAbstractMap<K, V> extends AbstractMap<K, V> {
        private transient Set<Entry<K, V>> entrySet;
        private transient Set<K> keySet;
        private transient Collection<V> values;

        /**
         * Creates the entry set to be returned by {@link #entrySet()}. This method
         * is invoked at most once on a given map, at the time when {@code entrySet}
         * is first called.
         */
        abstract Set<Entry<K, V>> createEntrySet();

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = entrySet;
            return (result == null) ? entrySet = createEntrySet() : result;
        }

        @Override
        public Set<K> keySet() {
            Set<K> result = keySet;
            return (result == null) ? keySet = createKeySet() : result;
        }

        Set<K> createKeySet() {
            return new KeySet<K, V>(this);
        }

        @Override
        public Collection<V> values() {
            Collection<V> result = values;
            return (result == null) ? values = createValues() : result;
        }

        Collection<V> createValues() {
            return new Values<K, V>(this);
        }
    }

    static class KeySet<K, V> extends Sets.ImprovedAbstractSet<K> {
        final Map<K, V> map;

        KeySet(Map<K, V> map) {
            this.map = checkNotNull(map);
        }

        Map<K, V> map() {
            return map;
        }

        @Override
        public Iterator<K> iterator() {
            return keyIterator(map().entrySet().iterator());
        }

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return map().containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            if (contains(o)) {
                map().remove(o);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            map().clear();
        }
    }

    static class Values<K, V> extends AbstractCollection<V> {
        final Map<K, V> map;

        Values(Map<K, V> map) {
            this.map = checkNotNull(map);
        }

        final Map<K, V> map() {
            return map;
        }

        @Override
        public Iterator<V> iterator() {
            return valueIterator(map().entrySet().iterator());
        }

        @Override
        public boolean remove(Object o) {
            try {
                return super.remove(o);
            } catch (UnsupportedOperationException e) {
                for (Entry<K, V> entry : map().entrySet()) {
                    if (Objects.equals(o, entry.getValue())) {
                        map().remove(entry.getKey());
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                Set<K> toRemove = Sets.newHashSet();
                for (Entry<K, V> entry : map().entrySet()) {
                    if (c.contains(entry.getValue())) {
                        toRemove.add(entry.getKey());
                    }
                }
                return map().keySet().removeAll(toRemove);
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                Set<K> toRetain = Sets.newHashSet();
                for (Entry<K, V> entry : map().entrySet()) {
                    if (c.contains(entry.getValue())) {
                        toRetain.add(entry.getKey());
                    }
                }
                return map().keySet().retainAll(toRetain);
            }
        }

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return map().containsValue(o);
        }

        @Override
        public void clear() {
            map().clear();
        }
    }

    abstract static class EntrySet<K, V>
            extends Sets.ImprovedAbstractSet<Entry<K, V>> {
        abstract Map<K, V> map();

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public void clear() {
            map().clear();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                Object key = entry.getKey();
                V value = Maps.safeGet(map(), key);
                return Objects.equals(value, entry.getValue())
                        && (value != null || map().containsKey(key));
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean remove(Object o) {
            if (contains(o)) {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                return map().keySet().remove(entry.getKey());
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                // if the iterators don't support remove
                return Sets.removeAllImpl(this, c.iterator());
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                // if the iterators don't support remove
                Set<Object> keys = Sets.newHashSetWithExpectedSize(c.size());
                for (Object o : c) {
                    if (contains(o)) {
                        Entry<?, ?> entry = (Entry<?, ?>) o;
                        keys.add(entry.getKey());
                    }
                }
                return map().keySet().retainAll(keys);
            }
        }
    }
}
