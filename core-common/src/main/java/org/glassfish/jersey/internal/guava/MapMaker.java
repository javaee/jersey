/*
 * Copyright (C) 2009 The Guava Authors
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

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>A builder of {@link ConcurrentMap} instances having any combination of the following features:
 * <p>
 * <ul>
 * <li>keys or values automatically wrapped in {@linkplain WeakReference weak} or {@linkplain
 * SoftReference soft} references
 * <li>notification of evicted (or otherwise removed) entries
 * <li>on-demand computation of values for keys not already present
 * </ul>
 * <p>
 * <p>Usage example: <pre>   {@code
 * <p>
 *   ConcurrentMap<Request, Stopwatch> timers = new MapMaker()
 *       .concurrencyLevel(4)
 *       .weakKeys()
 *       .makeMap();}</pre>
 * <p>
 * <p>These features are all optional; {@code new MapMaker().makeMap()} returns a valid concurrent
 * map that behaves similarly to a {@link ConcurrentHashMap}.
 * <p>
 * <p>The returned map is implemented as a hash table with similar performance characteristics to
 * {@link ConcurrentHashMap}. It supports all optional operations of the {@code ConcurrentMap}
 * interface. It does not permit null keys or values.
 * <p>
 * <p><b>Note:</b> by default, the returned map uses equality comparisons (the {@link Object#equals
 * equals} method) to determine equality for keys or values. However, if {@link #weakKeys} was
 * specified, the map uses identity ({@code ==}) comparisons instead for keys. Likewise, if {@link
 * #weakValues} or {@link #softValues} was specified, the map uses identity comparisons for values.
 * <p>
 * <p>The view collections of the returned map have <i>weakly consistent iterators</i>. This means
 * that they are safe for concurrent use, but if other threads modify the map after the iterator is
 * created, it is undefined which of these changes, if any, are reflected in that iterator. These
 * iterators never throw {@link ConcurrentModificationException}.
 * <p>
 * <p>If {@link #weakKeys}, {@link #weakValues}, or {@link #softValues} are requested, it is
 * possible for a key or value present in the map to be reclaimed by the garbage collector. Entries
 * with reclaimed keys or values may be removed from the map on each map modification or on
 * occasional map accesses; such entries may be counted by {@link Map#size}, but will never be
 * visible to read or write operations. A partially-reclaimed entry is never exposed to the user.
 * Any {@link Map.Entry} instance retrieved from the map's
 * {@linkplain Map#entrySet entry set} is a snapshot of that entry's state at the time of
 * retrieval; such entries do, however, support {@link Map.Entry#setValue}, which simply
 * calls {@link Map#put} on the entry's key.
 * <p>
 * <p>The maps produced by {@code MapMaker} are serializable, and the deserialized maps retain all
 * the configuration properties of the original map. During deserialization, if the original map had
 * used soft or weak references, the entries are reconstructed as they were, but it's not unlikely
 * they'll be quickly garbage-collected before they are ever accessed.
 * <p>
 * <p>{@code new MapMaker().weakKeys().makeMap()} is a recommended replacement for {@link
 * java.util.WeakHashMap}, but note that it compares keys using object identity whereas {@code
 * WeakHashMap} uses {@link Object#equals}.
 *
 * @author Bob Lee
 * @author Charles Fry
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
public final class MapMaker extends GenericMapMaker<Object, Object> {
    static final int UNSET_INT = -1;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
    private static final int DEFAULT_EXPIRATION_NANOS = 0;

    private final int initialCapacity = UNSET_INT;
    private final int concurrencyLevel = UNSET_INT;
    final int maximumSize = UNSET_INT;

    private final long expireAfterWriteNanos = UNSET_INT;
    private final long expireAfterAccessNanos = UNSET_INT;

    Equivalence<Object> getKeyEquivalence() {
        return getKeyStrength().defaultEquivalence();
    }

    int getInitialCapacity() {
        return (initialCapacity == UNSET_INT) ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
    }

    int getConcurrencyLevel() {
        return (concurrencyLevel == UNSET_INT) ? DEFAULT_CONCURRENCY_LEVEL : concurrencyLevel;
    }

    MapMakerInternalMap.Strength getKeyStrength() {
        return MapMakerInternalMap.Strength.STRONG;
    }

    MapMakerInternalMap.Strength getValueStrength() {
        return MapMakerInternalMap.Strength.STRONG;
    }

    long getExpireAfterWriteNanos() {
        return (expireAfterWriteNanos == UNSET_INT) ? DEFAULT_EXPIRATION_NANOS : expireAfterWriteNanos;
    }

    long getExpireAfterAccessNanos() {
        return (expireAfterAccessNanos == UNSET_INT)
                ? DEFAULT_EXPIRATION_NANOS : expireAfterAccessNanos;
    }

    Ticker getTicker() {
        return Ticker.systemTicker();
    }

    /**
     * Returns a string representation for this MapMaker instance. The exact form of the returned
     * string is not specificed.
     */
    @Override
    public String toString() {
        MoreObjects.ToStringHelper s = MoreObjects.toStringHelper(this);
        if (initialCapacity != UNSET_INT) {
            s.add("initialCapacity", initialCapacity);
        }
        if (concurrencyLevel != UNSET_INT) {
            s.add("concurrencyLevel", concurrencyLevel);
        }
        if (maximumSize != UNSET_INT) {
            s.add("maximumSize", maximumSize);
        }
        if (expireAfterWriteNanos != UNSET_INT) {
            s.add("expireAfterWrite", expireAfterWriteNanos + "ns");
        }
        if (expireAfterAccessNanos != UNSET_INT) {
            s.add("expireAfterAccess", expireAfterAccessNanos + "ns");
        }
        return s.toString();
    }

    /**
     * The reason why an entry was removed.
     */
    enum RemovalCause {
        /**
         * The entry was manually removed by the user. This can result from the user invoking
         * {@link Map#remove}, {@link ConcurrentMap#remove}, or {@link java.util.Iterator#remove}.
         */
        EXPLICIT {
        },

        /**
         * The entry itself was not actually removed, but its value was replaced by the user. This can
         * result from the user invoking {@link Map#put}, {@link Map#putAll},
         * {@link ConcurrentMap#replace(Object, Object)}, or
         * {@link ConcurrentMap#replace(Object, Object, Object)}.
         */
        REPLACED {
        },

        /**
         * The entry was removed automatically because its key or value was garbage-collected. This can
         * occur when using {@link #softValues}, {@link #weakKeys}, or {@link #weakValues}.
         */
        COLLECTED {
        },

        /**
         * The entry's expiration timestamp has passed. This can occur when using {@link
         * #expireAfterWrite} or {@link #expireAfterAccess}.
         */
        EXPIRED {
        },

        /**
         * The entry was evicted due to size constraints. This can occur when using {@link
         * #maximumSize}.
         */
        SIZE {
        }

    }

    /**
     * An object that can receive a notification when an entry is removed from a map. The removal
     * resulting in notification could have occured to an entry being manually removed or replaced, or
     * due to eviction resulting from timed expiration, exceeding a maximum size, or garbage
     * collection.
     * <p>
     * <p>An instance may be called concurrently by multiple threads to process different entries.
     * Implementations of this interface should avoid performing blocking calls or synchronizing on
     * shared resources.
     *
     * @param <K> the most general type of keys this listener can listen for; for
     *            example {@code Object} if any key is acceptable
     * @param <V> the most general type of values this listener can listen for; for
     *            example {@code Object} if any key is acceptable
     */
    interface RemovalListener<K, V> {
        /**
         * Notifies the listener that a removal occurred at some point in the past.
         */
        void onRemoval(RemovalNotification<K, V> notification);
    }

    /**
     * A notification of the removal of a single entry. The key or value may be null if it was already
     * garbage collected.
     * <p>
     * <p>Like other {@code Map.Entry} instances associated with MapMaker, this class holds strong
     * references to the key and value, regardless of the type of references the map may be using.
     */
    static final class RemovalNotification<K, V> extends ImmutableEntry<K, V> {
        private static final long serialVersionUID = 0;

        RemovalNotification(K key, V value, RemovalCause cause) {
            super(key, value);
        }

    }

}
