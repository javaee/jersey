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
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * Provides static methods that involve a {@code Table}.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Tables">
 * {@code Tables}</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 7.0
 */
public final class Tables {
    private Tables() {
    }

    /**
     * Returns an immutable cell with the specified row key, column key, and
     * value.
     * <p>
     * <p>The returned cell is serializable.
     *
     * @param rowKey    the row key to be associated with the returned cell
     * @param columnKey the column key to be associated with the returned cell
     * @param value     the value to be associated with the returned cell
     */
    public static <R, C, V> Table.Cell<R, C, V> immutableCell(
            R rowKey, C columnKey, V value) {
        return new ImmutableCell<R, C, V>(rowKey, columnKey, value);
    }

    /**
     * Creates a transposed view of a given table that flips its row and column
     * keys. In other words, calling {@code get(columnKey, rowKey)} on the
     * generated table always returns the same value as calling {@code
     * get(rowKey, columnKey)} on the original table. Updating the original table
     * changes the contents of the transposed table and vice versa.
     * <p>
     * <p>The returned table supports update operations as long as the input table
     * supports the analogous operation with swapped rows and columns. For
     * example, in a {@link HashBasedTable} instance, {@code
     * rowKeySet().iterator()} supports {@code remove()} but {@code
     * columnKeySet().iterator()} doesn't. With a transposed {@link
     * HashBasedTable}, it's the other way around.
     */
    private static <R, C, V> Table<C, R, V> transpose(Table<R, C, V> table) {
        return (table instanceof TransposeTable)
                ? ((TransposeTable<R, C, V>) table).original
                : new TransposeTable<C, R, V>(table);
    }

    static boolean equalsImpl(Table<?, ?, ?> table, Object obj) {
        if (obj == table) {
            return true;
        } else if (obj instanceof Table) {
            Table<?, ?, ?> that = (Table<?, ?, ?>) obj;
            return table.cellSet().equals(that.cellSet());
        } else {
            return false;
        }
    }

    static final class ImmutableCell<R, C, V>
            extends AbstractCell<R, C, V> implements Serializable {
        private static final long serialVersionUID = 0;
        private final R rowKey;
        private final C columnKey;
        private final V value;

        ImmutableCell(
                R rowKey, C columnKey, V value) {
            this.rowKey = rowKey;
            this.columnKey = columnKey;
            this.value = value;
        }

        @Override
        public R getRowKey() {
            return rowKey;
        }

        @Override
        public C getColumnKey() {
            return columnKey;
        }

        @Override
        public V getValue() {
            return value;
        }
    }

    abstract static class AbstractCell<R, C, V> implements Table.Cell<R, C, V> {
        // needed for serialization
        AbstractCell() {
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Table.Cell) {
                Table.Cell<?, ?, ?> other = (Table.Cell<?, ?, ?>) obj;
                return Objects.equals(getRowKey(), other.getRowKey())
                        && Objects.equals(getColumnKey(), other.getColumnKey())
                        && Objects.equals(getValue(), other.getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getRowKey(), getColumnKey(), getValue());
        }

        @Override
        public String toString() {
            return "(" + getRowKey() + "," + getColumnKey() + ")=" + getValue();
        }
    }

    private static class TransposeTable<C, R, V> extends AbstractTable<C, R, V> {
        // Will cast TRANSPOSE_CELL to a type that always succeeds
        private static final Function<Cell<?, ?, ?>, Cell<?, ?, ?>> TRANSPOSE_CELL =
                new Function<Cell<?, ?, ?>, Cell<?, ?, ?>>() {
                    @Override
                    public Cell<?, ?, ?> apply(Cell<?, ?, ?> cell) {
                        return immutableCell(
                                cell.getColumnKey(), cell.getRowKey(), cell.getValue());
                    }
                };
        final Table<R, C, V> original;

        TransposeTable(Table<R, C, V> original) {
            this.original = checkNotNull(original);
        }

        @Override
        public void clear() {
            original.clear();
        }

        @Override
        public Map<C, V> column(R columnKey) {
            return original.row(columnKey);
        }

        @Override
        public Set<R> columnKeySet() {
            return original.rowKeySet();
        }

        @Override
        public Map<R, Map<C, V>> columnMap() {
            return original.rowMap();
        }

        @Override
        public boolean contains(
                Object rowKey, Object columnKey) {
            return original.contains(columnKey, rowKey);
        }

        @Override
        public boolean containsColumn(Object columnKey) {
            return original.containsRow(columnKey);
        }

        @Override
        public boolean containsRow(Object rowKey) {
            return original.containsColumn(rowKey);
        }

        @Override
        public boolean containsValue(Object value) {
            return original.containsValue(value);
        }

        @Override
        public V get(Object rowKey, Object columnKey) {
            return original.get(columnKey, rowKey);
        }

        @Override
        public V put(C rowKey, R columnKey, V value) {
            return original.put(columnKey, rowKey, value);
        }

        @Override
        public void putAll(Table<? extends C, ? extends R, ? extends V> table) {
            original.putAll(transpose(table));
        }

        @Override
        public V remove(Object rowKey, Object columnKey) {
            return original.remove(columnKey, rowKey);
        }

        @Override
        public Map<R, V> row(C rowKey) {
            return original.column(rowKey);
        }

        @Override
        public Set<C> rowKeySet() {
            return original.columnKeySet();
        }

        @Override
        public Map<C, Map<R, V>> rowMap() {
            return original.columnMap();
        }

        @Override
        public int size() {
            return original.size();
        }

        @SuppressWarnings("unchecked")
        @Override
        Iterator<Cell<C, R, V>> cellIterator() {
            return Iterators.transform(original.cellSet().iterator(), (Function) TRANSPOSE_CELL);
        }
    }
}
