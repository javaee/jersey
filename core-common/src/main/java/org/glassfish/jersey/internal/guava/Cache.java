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

import java.util.concurrent.Callable;

/**
 * A semi-persistent mapping from keys to values. Cache entries are manually added using
 * {@link #get(Object, Callable)} or {@link #put(Object, Object)}, and are stored in the cache until
 * either evicted or manually invalidated.
 * <p>
 * <p>Implementations of this interface are expected to be thread-safe, and can be safely accessed
 * by multiple concurrent threads.
 * <p>
 * <p>Note that while this class is still annotated as {@link Beta}, the API is frozen from a
 * consumer's standpoint. In other words existing methods are all considered {@code non-Beta} and
 * won't be changed without going through an 18 month deprecation cycle; however new methods may be
 * added at any time.
 *
 * @author Charles Fry
 * @since 10.0
 */
public interface Cache<K, V> {

    /**
     * Returns the value associated with {@code key} in this cache, or {@code null} if there is no
     * cached value for {@code key}.
     *
     * @since 11.0
     */
    V getIfPresent(Object key);

    /**
     * Associates {@code value} with {@code key} in this cache. If the cache previously contained a
     * value associated with {@code key}, the old value is replaced by {@code value}.
     * <p>
     * <p>Prefer {@link #get(Object, Callable)} when using the conventional "if cached, return;
     * otherwise create, cache and return" pattern.
     *
     * @since 11.0
     */
    void put(K key, V value);

}
