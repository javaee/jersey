/*
 * Copyright (C) 2008 The Guava Authors
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of {@link Table} using hash tables.
 * <p>
 * <p>The views returned by {@link #column}, {@link #columnKeySet()}, and {@link
 * #columnMap()} have iterators that don't support {@code remove()}. Otherwise,
 * all optional operations are supported. Null row keys, columns keys, and
 * values are not supported.
 * <p>
 * <p>Lookups by row key are often faster than lookups by column key, because
 * the data is stored in a {@code Map<R, Map<C, V>>}. A method call like {@code
 * column(columnKey).get(rowKey)} still runs quickly, since the row key is
 * provided. However, {@code column(columnKey).size()} takes longer, since an
 * iteration across all row keys occurs.
 * <p>
 * <p>Note that this implementation is not synchronized. If multiple threads
 * access this table concurrently and one of the threads modifies the table, it
 * must be synchronized externally.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/NewCollectionTypesExplained#Table">
 * {@code Table}</a>.
 *
 * @author Jared Levy
 * @since 7.0
 */
public class HashBasedTable<R, C, V> extends StandardTable<R, C, V> {
    private static final long serialVersionUID = 0;

    private HashBasedTable(Map<R, Map<C, V>> backingMap, Factory<C, V> factory) {
        super(backingMap, factory);
    }

    /**
     * Creates an empty {@code HashBasedTable}.
     */
    public static <R, C, V> HashBasedTable<R, C, V> create() {
        return new HashBasedTable<R, C, V>(
                new HashMap<R, Map<C, V>>(), new Factory<C, V>(0));
    }

    // Overriding so NullPointerTester test passes.

    @Override
    public boolean contains(
            Object rowKey, Object columnKey) {
        return super.contains(rowKey, columnKey);
    }

    @Override
    public boolean containsColumn(Object columnKey) {
        return super.containsColumn(columnKey);
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(value);
    }

    @Override
    public V get(Object rowKey, Object columnKey) {
        return super.get(rowKey, columnKey);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    private static class Factory<C, V>
            implements Supplier<Map<C, V>>, Serializable {
        private static final long serialVersionUID = 0;
        final int expectedSize;

        Factory(int expectedSize) {
            this.expectedSize = expectedSize;
        }

        @Override
        public Map<C, V> get() {
            return Maps.newHashMapWithExpectedSize(expectedSize);
        }
    }
}
