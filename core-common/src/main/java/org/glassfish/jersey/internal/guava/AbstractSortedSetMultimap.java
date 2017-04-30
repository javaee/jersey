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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;

/**
 * Basic implementation of the {@link SortedSetMultimap} interface. It's a
 * wrapper around {@link AbstractMapBasedMultimap} that converts the returned
 * collections into sorted sets. The {@link #createCollection} method
 * must return a {@code SortedSet}.
 *
 * @author Jared Levy
 */
abstract class AbstractSortedSetMultimap<K, V>
        extends AbstractSetMultimap<K, V> implements SortedSetMultimap<K, V> {
    private static final long serialVersionUID = 430848587173315748L;

    /**
     * Creates a new multimap that uses the provided map.
     *
     * @param map place to store the mapping from each key to its corresponding
     *            values
     */
    AbstractSortedSetMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    @Override
    abstract SortedSet<V> createCollection();

    @Override
    SortedSet<V> createUnmodifiableEmptyCollection() {
        return Collections.unmodifiableSortedSet(createCollection());
    }

    /**
     * Returns a collection view of all values associated with a key. If no
     * mappings in the multimap have the provided key, an empty collection is
     * returned.
     * <p>
     * <p>Changes to the returned collection will update the underlying multimap,
     * and vice versa.
     * <p>
     * <p>Because a {@code SortedSetMultimap} has unique sorted values for a given
     * key, this method returns a {@link SortedSet}, instead of the
     * {@link Collection} specified in the {@link Multimap} interface.
     */
    @Override
    public SortedSet<V> get(K key) {
        return (SortedSet<V>) super.get(key);
    }

    /**
     * Removes all values associated with a given key. The returned collection is
     * immutable.
     * <p>
     * <p>Because a {@code SortedSetMultimap} has unique sorted values for a given
     * key, this method returns a {@link SortedSet}, instead of the
     * {@link Collection} specified in the {@link Multimap} interface.
     */
    @Override
    public SortedSet<V> removeAll(Object key) {
        return (SortedSet<V>) super.removeAll(key);
    }

    /**
     * Returns a map view that associates each key with the corresponding values
     * in the multimap. Changes to the returned map, such as element removal, will
     * update the underlying multimap. The map does not support {@code setValue}
     * on its entries, {@code put}, or {@code putAll}.
     * <p>
     * <p>When passed a key that is present in the map, {@code
     * asMap().get(Object)} has the same behavior as {@link #get}, returning a
     * live collection. When passed a key that is not present, however, {@code
     * asMap().get(Object)} returns {@code null} instead of an empty collection.
     * <p>
     * <p>Though the method signature doesn't say so explicitly, the returned map
     * has {@link SortedSet} values.
     */
    @Override
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Consequently, the values do not follow their natural ordering or the
     * ordering of the value comparator.
     */
    @Override
    public Collection<V> values() {
        return super.values();
    }
}
