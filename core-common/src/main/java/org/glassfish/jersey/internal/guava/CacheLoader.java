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

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * Computes or retrieves values, based on a key, for use in populating a {@link LoadingCache}.
 * <p>
 * <p>Most implementations will only need to implement {@link #load}. Other methods may be
 * overridden as desired.
 * <p>
 * <p>Usage example: <pre>   {@code
 * <p>
 *   CacheLoader<Key, Graph> loader = new CacheLoader<Key, Graph>() {
 *     public Graph load(Key key) throws AnyException {
 *       return createExpensiveGraph(key);
 *     }
 *   };
 *   LoadingCache<Key, Graph> cache = CacheBuilder.newBuilder().build(loader);}</pre>
 *
 * @author Charles Fry
 * @since 10.0
 */
public abstract class CacheLoader<K, V> {
    /**
     * Constructor for use by subclasses.
     */
    protected CacheLoader() {
    }

    /**
     * Computes or retrieves the value corresponding to {@code key}.
     *
     * @param key the non-null key whose value should be loaded
     * @return the value associated with {@code key}; <b>must not be null</b>
     * @throws Exception            if unable to load the result
     * @throws InterruptedException if this method is interrupted. {@code InterruptedException} is
     *                              treated like any other {@code Exception} in all respects except that, when it is caught,
     *                              the thread's interrupt status is set
     */
    public abstract V load(K key) throws Exception;

    /**
     * Computes or retrieves a replacement value corresponding to an already-cached {@code key}. This
     * method is called when an existing cache entry is refreshed by
     * {@link CacheBuilder#refreshAfterWrite}, or through a call to {@link LoadingCache#refresh}.
     * <p>
     * <p>This implementation synchronously delegates to {@link #load}. It is recommended that it be
     * overridden with an asynchronous implementation when using
     * {@link CacheBuilder#refreshAfterWrite}.
     * <p>
     * <p><b>Note:</b> <i>all exceptions thrown by this method will be logged and then swallowed</i>.
     *
     * @param key      the non-null key whose value should be loaded
     * @param oldValue the non-null old value corresponding to {@code key}
     * @return the future new value associated with {@code key};
     * <b>must not be null, must not return null</b>
     * @throws Exception            if unable to reload the result
     * @throws InterruptedException if this method is interrupted. {@code InterruptedException} is
     *                              treated like any other {@code Exception} in all respects except that, when it is caught,
     *                              the thread's interrupt status is set
     * @since 11.0
     */
    public ListenableFuture<V> reload(K key, V oldValue) throws Exception {
        checkNotNull(key);
        checkNotNull(oldValue);
        return Futures.immediateFuture(load(key));
    }

    /**
     * Thrown to indicate that an invalid response was returned from a call to {@link CacheLoader}.
     *
     * @since 11.0
     */
    public static final class InvalidCacheLoadException extends RuntimeException {
        public InvalidCacheLoadException(String message) {
            super(message);
        }
    }
}
