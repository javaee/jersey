/*
 * Copyright (C) 2009 The Guava Authors
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

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.glassfish.jersey.internal.guava.Preconditions.checkArgument;
import static org.glassfish.jersey.internal.guava.Preconditions.checkState;

/**
 * <p>A builder of {@link LoadingCache} and {@link Cache} instances having any combination of the
 * following features:
 * <p>
 * <ul>
 * <li>automatic loading of entries into the cache
 * <li>least-recently-used eviction when a maximum size is exceeded
 * <li>time-based expiration of entries, measured since last access or last write
 * <li>keys automatically wrapped in {@linkplain WeakReference weak} references
 * <li>values automatically wrapped in {@linkplain WeakReference weak} or
 * {@linkplain SoftReference soft} references
 * <li>notification of evicted (or otherwise removed) entries
 * <li>accumulation of cache access statistics
 * </ul>
 * <p>
 * <p>These features are all optional; caches can be created using all or none of them. By default
 * cache instances created by {@code CacheBuilder} will not perform any type of eviction.
 * <p>
 * <p>Usage example: <pre>   {@code
 * <p>
 *   LoadingCache<Key, Graph> graphs = CacheBuilder.newBuilder()
 *       .maximumSize(10000)
 *       .expireAfterWrite(10, TimeUnit.MINUTES)
 *       .removalListener(MY_LISTENER)
 *       .build(
 *           new CacheLoader<Key, Graph>() {
 *             public Graph load(Key key) throws AnyException {
 *               return createExpensiveGraph(key);
 *             }
 *           });}</pre>
 * <p>
 * <p>Or equivalently, <pre>   {@code
 * <p>
 *   // In real life this would come from a command-line flag or config file
 *   String spec = "maximumSize=10000,expireAfterWrite=10m";
 * <p>
 *   LoadingCache<Key, Graph> graphs = CacheBuilder.from(spec)
 *       .removalListener(MY_LISTENER)
 *       .build(
 *           new CacheLoader<Key, Graph>() {
 *             public Graph load(Key key) throws AnyException {
 *               return createExpensiveGraph(key);
 *             }
 *           });}</pre>
 * <p>
 * <p>The returned cache is implemented as a hash table with similar performance characteristics to
 * {@link ConcurrentHashMap}. It implements all optional operations of the {@link LoadingCache} and
 * {@link Cache} interfaces. The {@code asMap} view (and its collection views) have <i>weakly
 * consistent iterators</i>. This means that they are safe for concurrent use, but if other threads
 * modify the cache after the iterator is created, it is undefined which of these changes, if any,
 * are reflected in that iterator. These iterators never throw {@link
 * ConcurrentModificationException}.
 * <p>
 * <p><b>Note:</b> by default, the returned cache uses equality comparisons (the
 * {@link Object#equals equals} method) to determine equality for keys or values. However, if
 * {@link #weakKeys} was specified, the cache uses identity ({@code ==})
 * comparisons instead for keys. Likewise, if {@link #weakValues} or {@link #softValues} was
 * specified, the cache uses identity comparisons for values.
 * <p>
 * <p>Entries are automatically evicted from the cache when any of
 * {@linkplain #maximumSize(long) maximumSize}, {@linkplain #maximumWeight(long) maximumWeight},
 * {@linkplain #expireAfterWrite expireAfterWrite},
 * {@linkplain #expireAfterAccess expireAfterAccess}, {@linkplain #weakKeys weakKeys},
 * {@linkplain #weakValues weakValues}, or {@linkplain #softValues softValues} are requested.
 * <p>
 * <p>If {@linkplain #maximumSize(long) maximumSize} or
 * {@linkplain #maximumWeight(long) maximumWeight} is requested entries may be evicted on each cache
 * modification.
 * <p>
 * <p>If {@linkplain #expireAfterWrite expireAfterWrite} or
 * {@linkplain #expireAfterAccess expireAfterAccess} is requested entries may be evicted on each
 * cache modification, on occasional cache accesses, or on calls to {@link Cache#cleanUp}. Expired
 * entries may be counted by {@link Cache#size}, but will never be visible to read or write
 * operations.
 * <p>
 * <p>If {@linkplain #weakKeys weakKeys}, {@linkplain #weakValues weakValues}, or
 * {@linkplain #softValues softValues} are requested, it is possible for a key or value present in
 * the cache to be reclaimed by the garbage collector. Entries with reclaimed keys or values may be
 * removed from the cache on each cache modification, on occasional cache accesses, or on calls to
 * {@link Cache#cleanUp}; such entries may be counted in {@link Cache#size}, but will never be
 * visible to read or write operations.
 * <p>
 * <p>Certain cache configurations will result in the accrual of periodic maintenance tasks which
 * will be performed during write operations, or during occasional read operations in the absence of
 * writes. The {@link Cache#cleanUp} method of the returned cache will also perform maintenance, but
 * calling it should not be necessary with a high throughput cache. Only caches built with
 * {@linkplain #removalListener removalListener}, {@linkplain #expireAfterWrite expireAfterWrite},
 * {@linkplain #expireAfterAccess expireAfterAccess}, {@linkplain #weakKeys weakKeys},
 * {@linkplain #weakValues weakValues}, or {@linkplain #softValues softValues} perform periodic
 * maintenance.
 * <p>
 * <p>The caches produced by {@code CacheBuilder} are serializable, and the deserialized caches
 * retain all the configuration properties of the original cache. Note that the serialized form does
 * <i>not</i> include cache contents, but only configuration.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CachesExplained">caching</a> for a higher-level
 * explanation.
 *
 * @param <K> the base key type for all caches created by this builder
 * @param <V> the base value type for all caches created by this builder
 * @author Charles Fry
 * @author Kevin Bourrillion
 * @since 10.0
 */
public final class CacheBuilder<K, V> {

    public static final Ticker NULL_TICKER = new Ticker() {
        @Override
        public long read() {
            return 0;
        }
    };
    static final int UNSET_INT = -1;
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
    static final int DEFAULT_EXPIRATION_NANOS = 0;
    static final int DEFAULT_REFRESH_NANOS = 0;
    private final int initialCapacity = UNSET_INT;
    private final int concurrencyLevel = UNSET_INT;
    private long maximumSize = UNSET_INT;
    private final long maximumWeight = UNSET_INT;
    private final long expireAfterWriteNanos = UNSET_INT;
    private long expireAfterAccessNanos = UNSET_INT;
    private final long refreshNanos = UNSET_INT;

    // TODO(fry): make constructor private and update tests to use newBuilder
    private CacheBuilder() {
    }

    /**
     * Constructs a new {@code CacheBuilder} instance with default settings, including strong keys,
     * strong values, and no automatic eviction of any kind.
     */
    public static CacheBuilder<Object, Object> newBuilder() {
        return new CacheBuilder<Object, Object>();
    }

    int getConcurrencyLevel() {
        return (concurrencyLevel == UNSET_INT) ? DEFAULT_CONCURRENCY_LEVEL : concurrencyLevel;
    }

    /**
     * Specifies the maximum number of entries the cache may contain. Note that the cache <b>may evict
     * an entry before this limit is exceeded</b>. As the cache size grows close to the maximum, the
     * cache evicts entries that are less likely to be used again. For example, the cache may evict an
     * entry because it hasn't been used recently or very often.
     * <p>
     * <p>When {@code size} is zero, elements will be evicted immediately after being loaded into the
     * cache. This can be useful in testing, or to disable caching temporarily without a code change.
     * <p>
     * <p>This feature cannot be used in conjunction with {@link #maximumWeight}.
     *
     * @param size the maximum size of the cache
     * @throws IllegalArgumentException if {@code size} is negative
     * @throws IllegalStateException    if a maximum size or weight was already set
     */
    public CacheBuilder<K, V> maximumSize(long size) {
        checkState(this.maximumSize == UNSET_INT, "maximum size was already set to %s",
                this.maximumSize);
        checkState(this.maximumWeight == UNSET_INT, "maximum weight was already set to %s",
                this.maximumWeight);
        checkArgument(size >= 0, "maximum size must not be negative");
        this.maximumSize = size;
        return this;
    }

    /**
     * Specifies that each entry should be automatically removed from the cache once a fixed duration
     * has elapsed after the entry's creation, the most recent replacement of its value, or its last
     * access. Access time is reset by all cache read and write operations (including
     * {@code Cache.asMap().get(Object)} and {@code Cache.asMap().put(K, V)}), but not by operations
     * on the collection-views of {@link Cache#asMap}.
     * <p>
     * <p>When {@code duration} is zero, this method hands off to
     * {@link #maximumSize(long) maximumSize}{@code (0)}, ignoring any otherwise-specificed maximum
     * size or weight. This can be useful in testing, or to disable caching temporarily without a code
     * change.
     * <p>
     * <p>Expired entries may be counted in {@link Cache#size}, but will never be visible to read or
     * write operations. Expired entries are cleaned up as part of the routine maintenance described
     * in the class javadoc.
     *
     * @param duration the length of time after an entry is last accessed that it should be
     *                 automatically removed
     * @param unit     the unit that {@code duration} is expressed in
     * @throws IllegalArgumentException if {@code duration} is negative
     * @throws IllegalStateException    if the time to idle or time to live was already set
     */
    public CacheBuilder<K, V> expireAfterAccess(long duration, TimeUnit unit) {
        checkState(expireAfterAccessNanos == UNSET_INT, "expireAfterAccess was already set to %s ns",
                expireAfterAccessNanos);
        checkArgument(duration >= 0, "duration cannot be negative: %s %s", duration, unit);
        this.expireAfterAccessNanos = unit.toNanos(duration);
        return this;
    }

    long getExpireAfterAccessNanos() {
        return (expireAfterAccessNanos == UNSET_INT)
                ? DEFAULT_EXPIRATION_NANOS : expireAfterAccessNanos;
    }

    /**
     * Builds a cache, which either returns an already-loaded value for a given key or atomically
     * computes or retrieves it using the supplied {@code CacheLoader}. If another thread is currently
     * loading the value for this key, simply waits for that thread to finish and returns its
     * loaded value. Note that multiple threads can concurrently load values for distinct keys.
     * <p>
     * <p>This method does not alter the state of this {@code CacheBuilder} instance, so it can be
     * invoked again to create multiple independent caches.
     *
     * @param loader the cache loader used to obtain new values
     * @return a cache having the requested features
     */
    public <K1 extends K, V1 extends V> LoadingCache<K1, V1> build(
            CacheLoader<? super K1, V1> loader) {
        checkWeightWithWeigher();
        return new LocalCache.LocalLoadingCache<K1, V1>(this, loader);
    }

    /**
     * Builds a cache which does not automatically load values when keys are requested.
     * <p>
     * <p>Consider {@link #build(CacheLoader)} instead, if it is feasible to implement a
     * {@code CacheLoader}.
     * <p>
     * <p>This method does not alter the state of this {@code CacheBuilder} instance, so it can be
     * invoked again to create multiple independent caches.
     *
     * @return a cache having the requested features
     * @since 11.0
     */
    public <K1 extends K, V1 extends V> Cache<K1, V1> build() {
        checkWeightWithWeigher();
        checkNonLoadingCache();
        return new LocalCache.LocalManualCache<K1, V1>(this);
    }

    private void checkNonLoadingCache() {
        checkState(refreshNanos == UNSET_INT, "refreshAfterWrite requires a LoadingCache");
    }

    private void checkWeightWithWeigher() {
        checkState(maximumWeight == UNSET_INT, "maximumWeight requires weigher");
    }

    /**
     * Returns a string representation for this CacheBuilder instance. The exact form of the returned
     * string is not specified.
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
        if (maximumWeight != UNSET_INT) {
            s.add("maximumWeight", maximumWeight);
        }
        if (expireAfterWriteNanos != UNSET_INT) {
            s.add("expireAfterWrite", expireAfterWriteNanos + "ns");
        }
        if (expireAfterAccessNanos != UNSET_INT) {
            s.add("expireAfterAccess", expireAfterAccessNanos + "ns");
        }
        return s.toString();
    }
}
