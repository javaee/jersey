/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.glassfish.jersey.internal.guava;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Skeletal, implementation-agnostic implementation of the {@link Table} interface.
 *
 * @author Louis Wasserman
 */
abstract class AbstractTable<R, C, V> implements Table<R, C, V> {

    private transient Set<Cell<R, C, V>> cellSet;

    @Override
    public boolean containsRow(Object rowKey) {
        return Maps.safeContainsKey(rowMap(), rowKey);
    }

    @Override
    public boolean containsColumn(Object columnKey) {
        return Maps.safeContainsKey(columnMap(), columnKey);
    }

    @Override
    public Set<R> rowKeySet() {
        return rowMap().keySet();
    }

    @Override
    public Set<C> columnKeySet() {
        return columnMap().keySet();
    }

    @Override
    public boolean containsValue(Object value) {
        for (Map<C, V> row : rowMap().values()) {
            if (row.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Object rowKey, Object columnKey) {
        Map<C, V> row = Maps.safeGet(rowMap(), rowKey);
        return row != null && Maps.safeContainsKey(row, columnKey);
    }

    @Override
    public V get(Object rowKey, Object columnKey) {
        Map<C, V> row = Maps.safeGet(rowMap(), rowKey);
        return (row == null) ? null : Maps.safeGet(row, columnKey);
    }

    @Override
    public void clear() {
        Iterators.clear(cellSet().iterator());
    }

    @Override
    public V remove(Object rowKey, Object columnKey) {
        Map<C, V> row = Maps.safeGet(rowMap(), rowKey);
        return (row == null) ? null : Maps.safeRemove(row, columnKey);
    }

    @Override
    public V put(R rowKey, C columnKey, V value) {
        return row(rowKey).put(columnKey, value);
    }

    @Override
    public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        for (Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
            put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }
    }

    @Override
    public Set<Cell<R, C, V>> cellSet() {
        Set<Cell<R, C, V>> result = cellSet;
        return (result == null) ? cellSet = createCellSet() : result;
    }

    private Set<Cell<R, C, V>> createCellSet() {
        return new CellSet();
    }

    abstract Iterator<Cell<R, C, V>> cellIterator();

    @Override
    public boolean equals(Object obj) {
        return Tables.equalsImpl(this, obj);
    }

    @Override
    public int hashCode() {
        return cellSet().hashCode();
    }

    /**
     * Returns the string representation {@code rowMap().toString()}.
     */
    @Override
    public String toString() {
        return rowMap().toString();
    }

    private class CellSet extends AbstractSet<Cell<R, C, V>> {
        @Override
        public boolean contains(Object o) {
            if (o instanceof Cell) {
                Cell<?, ?, ?> cell = (Cell<?, ?, ?>) o;
                Map<C, V> row = Maps.safeGet(rowMap(), cell.getRowKey());
                return row != null && Collections2.safeContains(
                        row.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Cell) {
                Cell<?, ?, ?> cell = (Cell<?, ?, ?>) o;
                Map<C, V> row = Maps.safeGet(rowMap(), cell.getRowKey());
                return row != null && Collections2.safeRemove(
                        row.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
            }
            return false;
        }

        @Override
        public void clear() {
            AbstractTable.this.clear();
        }

        @Override
        public Iterator<Cell<R, C, V>> iterator() {
            return cellIterator();
        }

        @Override
        public int size() {
            return AbstractTable.this.size();
        }
    }

}
