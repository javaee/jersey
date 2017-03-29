/*
 * Copyright (C) 2011 The Guava Authors
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

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * A semi-persistent mapping from keys to values. Values are automatically loaded by the cache,
 * and are stored in the cache until either evicted or manually invalidated.
 * <p>
 * <p>Implementations of this interface are expected to be thread-safe, and can be safely accessed
 * by multiple concurrent threads.
 * <p>
 * <p>When evaluated as a {@link Function}, a cache yields the same result as invoking
 * {@link #getUnchecked}.
 * <p>
 * <p>Note that while this class is still annotated as {@link Beta}, the API is frozen from a
 * consumer's standpoint. In other words existing methods are all considered {@code non-Beta} and
 * won't be changed without going through an 18 month deprecation cycle; however new methods may be
 * added at any time.
 *
 * @author Charles Fry
 * @since 11.0
 */
public interface LoadingCache<K, V> extends Cache<K, V>, Function<K, V> {

    /**
     * Returns the value associated with {@code key} in this cache, first loading that value if
     * necessary. No observable state associated with this cache is modified until loading completes.
     * <p>
     * <p>If another call to {@link #get} or {@link #getUnchecked} is currently loading the value for
     * {@code key}, simply waits for that thread to finish and returns its loaded value. Note that
     * multiple threads can concurrently load values for distinct keys.
     * <p>
     * <p>Caches loaded by a {@link CacheLoader} will call {@link CacheLoader#load} to load new values
     * into the cache. Newly loaded values are added to the cache using
     * {@code Cache.asMap().putIfAbsent} after loading has completed; if another value was associated
     * with {@code key} while the new value was loading then a removal notification will be sent for
     * the new value.
     * <p>
     * <p>If the cache loader associated with this cache is known not to throw checked
     * exceptions, then prefer {@link #getUnchecked} over this method.
     *
     * @throws ExecutionException          if a checked exception was thrown while loading the value. ({@code
     *                                     ExecutionException} is thrown <a
     *                                     href="http://code.google.com/p/guava-libraries/wiki/CachesExplained#Interruption">even if
     *                                     computation was interrupted by an {@code InterruptedException}</a>.)
     * @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
     *                                     value
     * @throws ExecutionError              if an error was thrown while loading the value
     */
    V get(K key) throws ExecutionException;

    /**
     * @throws UncheckedExecutionException if an exception was thrown while loading the value. (As
     *                                     described in the documentation for {@link #getUnchecked}, {@code LoadingCache} should be
     *                                     used as a {@code Function} only with cache loaders that throw only unchecked exceptions.)
     * @deprecated Provided to satisfy the {@code Function} interface; use {@link #get} or
     * {@link #getUnchecked} instead.
     */
    @Deprecated
    @Override
    V apply(K key);

}
