/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.internal.util.collection;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * A implementation similar to {@link java.util.HashMap} but supports the
 * comparison of keys using a {@link KeyComparator}.
 *
 * @param <K> Type of keys
 * @param <V> Type of values
 * @author Paul Sandoz
 */
@SuppressWarnings("unchecked")
public class KeyComparatorHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {

    private static final long serialVersionUID = 3000273665665137463L;
    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;
    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    transient Entry<K, V>[] table;
    /**
     * The number of key-value mappings contained in this identity hash map.
     */
    transient int size;
    /**
     * The next ss value at which to resize (capacity * load factor).
     *
     * @serial
     */
    int threshold;
    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;
    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient volatile int modCount;

    final KeyComparator<K> keyComparator;

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param initialCapacity The initial capacity.
     * @param loadFactor      The load factor.
     * @param keyComparator   the map key comparator.
     * @throws IllegalArgumentException if the initial capacity is negative
     *                                  or the load factor is nonpositive.
     */
    @SuppressWarnings("unchecked")
    public KeyComparatorHashMap(int initialCapacity, final float loadFactor, final KeyComparator<K> keyComparator) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException(LocalizationMessages.ILLEGAL_INITIAL_CAPACITY(initialCapacity));
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException(LocalizationMessages.ILLEGAL_LOAD_FACTOR(loadFactor));
        }

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity) {
            capacity <<= 1;
        }

        this.loadFactor = loadFactor;
        threshold = (int) (capacity * loadFactor);
        table = new Entry[capacity];
        this.keyComparator = keyComparator;

        init();
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity.
     * @param keyComparator   the map key comparator.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    @SuppressWarnings("unchecked")
    public KeyComparatorHashMap(final int initialCapacity, final KeyComparator<K> keyComparator) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, keyComparator);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     *
     * @param keyComparator the map key comparator.
     */
    public KeyComparatorHashMap(final KeyComparator<K> keyComparator) {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, keyComparator);
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param m             the map whose mappings are to be placed in this map.
     * @param keyComparator the comparator
     * @throws NullPointerException if the specified map is null.
     */
    public KeyComparatorHashMap(final Map<? extends K, ? extends V> m, final KeyComparator<K> keyComparator) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR, keyComparator);
        putAllForCreate(m);
    }

    /**
     * Get the number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).
     *
     * @return return the modification count.
     */
    public int getModCount() {
        return modCount;
    }

    // internal utilities

    /**
     * Initialization hook for subclasses.
     * <p/>
     * This method is called in all pseudo-constructors (clone, readObject)
     * after HashMap has been initialized but before any entries have
     * been inserted.  (In the absence of this method, readObject would
     * require explicit knowledge of subclasses.)
     */
    void init() {
    }

    /**
     * Value representing null keys inside tables.
     */
    static final Object NULL_KEY = new Object();

    /**
     * Returns internal representation for key. Use NULL_KEY if key is null.
     */
    static <T> T maskNull(final T key) {
        return key == null ? (T) NULL_KEY : key;
    }

    static <T> boolean isNull(final T key) {
        return key == NULL_KEY;
    }

    /**
     * Returns key represented by specified internal representation.
     */
    static <T> T unmaskNull(final T key) {
        return key == NULL_KEY ? null : key;
    }

    /**
     * Returns a hash value for the specified object.  In addition to
     * the object's own hashCode, this method applies a "supplemental
     * hash function," which defends against poor quality hash functions.
     * This is critical because HashMap uses power-of two length
     * hash tables.<p>
     * <p/>
     * The shift distances in this function were chosen as the result
     * of an automated search over the entire four-dimensional search space.
     */
    static int hash(final Object x) {
        int h = x.hashCode();

        h += ~(h << 9);
        h ^= (h >>> 14);
        h += (h << 4);
        h ^= (h >>> 10);
        return h;
    }

    /**
     * Check for equality of non-null reference x and possibly-null y.
     */
    static boolean eq(final Object x, final Object y) {
        return x == y || x.equals(y);
    }

    /**
     * Returns index for hash code h.
     */
    static int indexFor(final int h, final int length) {
        return h & (length - 1);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    int keyComparatorHash(final K k) {
        return isNull(k) ? hash(k.hashCode()) : hash(keyComparator.hash(k));
    }

    int hash(int h) {
        h += ~(h << 9);
        h ^= (h >>> 14);
        h += (h << 4);
        h ^= (h >>> 10);
        return h;
    }

    /**
     * Check for equality of non-null reference x and possibly-null y.
     */
    boolean keyComparatorEq(final K x, final K y) {
        if (isNull(x)) {
            return x == y;
        } else if (isNull(y)) {
            return x == y;
        } else {
            return x == y || keyComparator.equals(x, y);
        }
    }

    /**
     * Returns the value to which the specified key is mapped in this identity
     * hash map, or <tt>null</tt> if the map contains no mapping for this key.
     * A return value of <tt>null</tt> does not <i>necessarily</i> indicate
     * that the map contains no mapping for the key; it is also possible that
     * the map explicitly maps the key to <tt>null</tt>. The
     * <tt>containsKey</tt> method may be used to distinguish these two cases.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value to which this map maps the specified key, or
     * <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(Object, Object)
     */
    @Override
    public V get(final Object key) {
        final K k = (K) maskNull(key);
        final int hash = keyComparatorHash(k);
        final int i = indexFor(hash, table.length);
        Entry<K, V> e = table[i];
        while (true) {
            if (e == null) {
                return null;
            }
            if (e.hash == hash && keyComparatorEq(k, e.key)) {
                return e.value;
            }
            e = e.next;
        }
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param key The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    @Override
    public boolean containsKey(final Object key) {
        final K k = (K) maskNull(key);
        final int hash = keyComparatorHash(k);
        final int i = indexFor(hash, table.length);
        Entry<K, V> e = table[i];
        while (e != null) {
            if (e.hash == hash && keyComparatorEq(k, e.key)) {
                return true;
            }
            e = e.next;
        }
        return false;
    }

    /**
     * Returns the entry associated with the specified key in the
     * HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    Entry<K, V> getEntry(final K key) {
        final K k = maskNull(key);
        final int hash = keyComparatorHash(k);
        final int i = indexFor(hash, table.length);
        Entry<K, V> e = table[i];
        while (e != null && !(e.hash == hash && keyComparatorEq(k, e.key))) {
            e = e.next;
        }
        return e;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key   key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     * if there was no mapping for key.  A <tt>null</tt> return can
     * also indicate that the HashMap previously associated
     * <tt>null</tt> with the specified key.
     */
    @Override
    public V put(final K key, final V value) {
        final K k = maskNull(key);
        final int hash = keyComparatorHash(k);
        final int i = indexFor(hash, table.length);

        for (Entry<K, V> e = table[i]; e != null; e = e.next) {
            if (e.hash == hash && keyComparatorEq(k, e.key)) {
                final V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
            }
        }

        modCount++;
        addEntry(hash, k, value, i);
        return null;
    }

    /**
     * This method is used instead of put by constructors and
     * pseudoconstructors (clone, readObject).  It does not resize the table,
     * check for comodification, etc.  It calls createEntry rather than
     * addEntry.
     */
    private void putForCreate(final K key, final V value) {
        final K k = maskNull(key);
        final int hash = keyComparatorHash(k);
        final int i = indexFor(hash, table.length);

        /**
         * Look for preexisting entry for key.  This will never happen for
         * clone or de-serialize.  It will only happen for construction if the
         * input Map is a sorted map whose ordering is inconsistent w/ equals.
         */
        for (Entry<K, V> e = table[i]; e != null; e = e.next) {
            if (e.hash == hash && keyComparatorEq(k, e.key)) {
                e.value = value;
                return;
            }
        }

        createEntry(hash, k, value, i);
    }

    private void putAllForCreate(final Map<? extends K, ? extends V> m) {
        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            putForCreate(e.getKey(), e.getValue());
        }
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     * <p/>
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *                    must be greater than current capacity unless current
     *                    capacity is MAXIMUM_CAPACITY (in which case value
     *                    is irrelevant).
     */
    void resize(final int newCapacity) {
        final Entry<K, V>[] oldTable = table;
        final int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        final Entry<K, V>[] newTable = new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int) (newCapacity * loadFactor);
    }

    /**
     * Transfer all entries from current table to newTable.
     */
    void transfer(final Entry<K, V>[] newTable) {
        final Entry<K, V>[] src = table;
        final int newCapacity = newTable.length;
        for (int j = 0; j < src.length; j++) {
            Entry<K, V> e = src[j];
            if (e != null) {
                src[j] = null;
                do {
                    final Entry<K, V> next = e.next;
                    final int i = indexFor(e.hash, newCapacity);
                    e.next = newTable[i];
                    newTable[i] = e;
                    e = next;
                } while (e != null);
            }
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map
     * These mappings will replace any mappings that
     * this map had for any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map.
     * @throws NullPointerException if the specified map is null.
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        final int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0) {
            return;
        }

        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.ss() + ss) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int) (numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY) {
                targetCapacity = MAXIMUM_CAPACITY;
            }
            int newCapacity = table.length;
            while (newCapacity < targetCapacity) {
                newCapacity <<= 1;
            }
            if (newCapacity > table.length) {
                resize(newCapacity);
            }
        }

        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     * if there was no mapping for key.  A <tt>null</tt> return can
     * also indicate that the map previously associated <tt>null</tt>
     * with the specified key.
     */
    @Override
    public V remove(final Object key) {
        final Entry<K, V> e = removeEntryForKey(key);
        return (e == null ? null : e.value);
    }

    /**
     * Removes and returns the entry associated with the specified key
     * in the HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    Entry<K, V> removeEntryForKey(final Object key) {
        final K k = (K) maskNull(key);
        final int hash = keyComparatorHash(k);
        final int i = indexFor(hash, table.length);
        Entry<K, V> prev = table[i];
        Entry<K, V> e = prev;

        while (e != null) {
            final Entry<K, V> next = e.next;
            if (e.hash == hash && keyComparatorEq(k, e.key)) {
                modCount++;
                size--;
                if (prev == e) {
                    table[i] = next;
                } else {
                    prev.next = next;
                }
                e.recordRemoval(this);
                return e;
            }
            prev = e;
            e = next;
        }

        return e;
    }

    /**
     * Special version of remove for EntrySet.
     */
    Entry<K, V> removeMapping(final Object o) {
        if (!(o instanceof Map.Entry)) {
            return null;
        }

        final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
        final K k = maskNull(entry.getKey());
        final int hash = keyComparatorHash(k);
        final int i = indexFor(hash, table.length);
        Entry<K, V> prev = table[i];
        Entry<K, V> e = prev;

        while (e != null) {
            final Entry<K, V> next = e.next;
            if (e.hash == hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e) {
                    table[i] = next;
                } else {
                    prev.next = next;
                }
                e.recordRemoval(this);
                return e;
            }
            prev = e;
            e = next;
        }

        return e;
    }

    /**
     * Removes all mappings from this map.
     */
    @Override
    public void clear() {
        modCount++;
        final Entry[] tab = table;
        for (int i = 0; i < tab.length; i++) {
            tab[i] = null;
        }
        size = 0;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     */
    @Override
    public boolean containsValue(final Object value) {
        if (value == null) {
            return containsNullValue();
        }

        final Entry[] tab = table;
        for (int i = 0; i < tab.length; i++) {
            for (Entry e = tab[i]; e != null; e = e.next) {
                if (value.equals(e.value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Special-case code for containsValue with null argument
     */
    private boolean containsNullValue() {
        final Entry[] tab = table;
        for (final Entry aTab : tab) {
            for (Entry e = aTab; e != null; e = e.next) {
                if (e.value == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    @Override
    public Object clone() {
        KeyComparatorHashMap<K, V> result = null;

        try {
            result = (KeyComparatorHashMap<K, V>) super.clone();

            result.table = new Entry[table.length];
            result.entrySet = null;
            result.modCount = 0;
            result.size = 0;
            result.init();
            result.putAllForCreate(this);
        } catch (final CloneNotSupportedException e) {
            // assert false;
        }

        return result;
    }

    static class Entry<K, V> implements Map.Entry<K, V> {

        final K key;
        V value;
        final int hash;
        Entry<K, V> next;

        /**
         * Create new entry.
         */
        Entry(final int h, final K k, final V v, final Entry<K, V> n) {
            value = v;
            next = n;
            key = k;
            hash = h;
        }

        @Override
        public K getKey() {
            return KeyComparatorHashMap.<K>unmaskNull(key);
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(final V newValue) {
            final V oldValue = value;
            value = newValue;
            return oldValue;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry e = (Map.Entry) o;
            final Object k1 = getKey();
            final Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                final Object v1 = getValue();
                final Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (key == NULL_KEY ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }

        /**
         * This method is invoked whenever the value in an entry is
         * overwritten by an invocation of put(k,v) for a key k that's already
         * in the HashMap.
         */
        void recordAccess(final KeyComparatorHashMap<K, V> m) {
        }

        /**
         * This method is invoked whenever the entry is
         * removed from the table.
         */
        void recordRemoval(final KeyComparatorHashMap<K, V> m) {
        }
    }

    /**
     * Add a new entry with the specified key, value and hash code to
     * the specified bucket.  It is the responsibility of this
     * method to resize the table if appropriate.
     * <p/>
     * Subclass overrides this to alter the behavior of put method.
     */
    void addEntry(final int hash, final K key, final V value, final int bucketIndex) {
        final Entry<K, V> e = table[bucketIndex];
        table[bucketIndex] = new Entry<K, V>(hash, key, value, e);
        if (size++ >= threshold) {
            resize(2 * table.length);
        }
    }

    /**
     * Like addEntry except that this version is used when creating entries
     * as part of Map construction or "pseudo-construction" (cloning,
     * deserialization).  This version needn't worry about resizing the table.
     * <p/>
     * Subclass overrides this to alter the behavior of HashMap(Map),
     * clone, and readObject.
     */
    void createEntry(final int hash, final K key, final V value, final int bucketIndex) {
        final Entry<K, V> e = table[bucketIndex];
        table[bucketIndex] = new Entry<K, V>(hash, key, value, e);
        size++;
    }

    private abstract class HashIterator<E> implements Iterator<E> {

        Entry<K, V> next;    // next entry to return
        int expectedModCount;    // For fast-fail
        int index;        // current slot
        Entry<K, V> current;    // current entry

        HashIterator() {
            expectedModCount = modCount;
            final Entry<K, V>[] t = table;
            int i = t.length;
            Entry<K, V> n = null;
            if (size != 0) { // advance to first entry
                //noinspection StatementWithEmptyBody
                while (i > 0 && (n = t[--i]) == null) {
                }
            }
            next = n;
            index = i;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        Entry<K, V> nextEntry() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            final Entry<K, V> e = next;
            if (e == null) {
                throw new NoSuchElementException();
            }

            Entry<K, V> n = e.next;
            final Entry<K, V>[] t = table;
            int i = index;
            while (n == null && i > 0) {
                n = t[--i];
            }
            index = i;
            next = n;
            return current = e;
        }

        @Override
        public void remove() {
            if (current == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            final K k = current.key;
            current = null;
            KeyComparatorHashMap.this.removeEntryForKey(k);
            expectedModCount = modCount;
        }
    }

    private class ValueIterator extends HashIterator<V> {

        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends HashIterator<K> {

        @Override
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator<Map.Entry<K, V>> {

        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    // Subclass overrides these to alter behavior of views' iterator() method
    Iterator<K> newKeyIterator() {
        return new KeyIterator();
    }

    Iterator<V> newValueIterator() {
        return new ValueIterator();
    }

    Iterator<Map.Entry<K, V>> newEntryIterator() {
        return new EntryIterator();
    }

    // Views
    private transient Set<Map.Entry<K, V>> entrySet = null;

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        final Set<Map.Entry<K, V>> es = entrySet;
        return (es != null ? es : (entrySet = (Set<Map.Entry<K, V>>) new EntrySet()));
    }

    private class EntrySet extends AbstractSet/*<Map.Entry<K,V>>*/ {

        @Override
        public Iterator/*<Map.Entry<K,V>>*/ iterator() {
            return newEntryIterator();
        }

        @Override
        public boolean contains(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<K, V> e = (Map.Entry<K, V>) o;
            final Entry<K, V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }

        @Override
        public boolean remove(final Object o) {
            return removeMapping(o) != null;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            KeyComparatorHashMap.this.clear();
        }
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     * bucket array) is emitted (int), followed  by the
     * <i>ss</i> of the HashMap (the number of key-value
     * mappings), followed by the key (Object) and value (Object)
     * for each key-value mapping represented by the HashMap
     * The key-value mappings are emitted in the order that they
     * are returned by <tt>entrySet().iterator()</tt>.
     */
    private void writeObject(final java.io.ObjectOutputStream s) throws IOException {
        final Iterator<Map.Entry<K, V>> i = entrySet().iterator();

        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();

        // Write out number of buckets
        s.writeInt(table.length);

        // Write out ss (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        while (i.hasNext()) {
            final Map.Entry<K, V> e = i.next();
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    /**
     * Reconstitute the <tt>HashMap</tt> instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(final java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        // Read in the threshold, loadfactor, and any hidden stuff
        s.defaultReadObject();

        // Read in number of buckets and allocate the bucket array;
        final int numBuckets = s.readInt();
        table = new Entry[numBuckets];

        init();  // Give subclass a chance to do its thing.

        // Read in ss (number of Mappings)
        final int ss = s.readInt();

        // Read the keys and values, and put the mappings in the HashMap
        for (int i = 0; i < ss; i++) {
            final K key = (K) s.readObject();
            final V value = (V) s.readObject();
            putForCreate(key, value);
        }
    }

    // These methods are used when serializing HashSets
    int capacity() {
        return table.length;
    }

    float loadFactor() {
        return loadFactor;
    }
}
