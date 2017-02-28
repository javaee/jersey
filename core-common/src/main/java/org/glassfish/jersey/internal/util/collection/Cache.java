/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Cache implementation that relies on FutureTask.
 * Desired value will only be computed once and computed value stored in the cache.
 * The implementation is based on an example from the "Java Concurrency in Practice" book
 * authored by Brian Goetz and company.
 *
 * @param <K> The type of the key of the cache
 * @param <V> The type of the values in the cache
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class Cache<K, V> implements Function<K, V> {

    private static final CycleHandler<Object> EMPTY_HANDLER = key -> { };
    private final CycleHandler<K> cycleHandler;
    private final ConcurrentHashMap<K, OriginThreadAwareFuture> cache = new ConcurrentHashMap<>();
    private final Function<K, V> computable;

    /**
     * Create new cache with given computable to compute values.
     * Detected cycles will be ignored as there is a no-op cycle handler registered by default.
     *
     * @param computable function generated the new value.
     */
    @SuppressWarnings("unchecked")
    public Cache(Function<K, V> computable) {
        this(computable, (CycleHandler<K>) EMPTY_HANDLER);
    }

    /**
     * Create new cache with given computable and cycle handler.
     *
     * @param computable   function generated the new value.
     * @param cycleHandler handler used if the thread cycle is met.
     */
    public Cache(Function<K, V> computable, CycleHandler<K> cycleHandler) {
        this.computable = computable;
        this.cycleHandler = cycleHandler;
    }

    @Override
    public V apply(final K key) {
        while (true) {
            OriginThreadAwareFuture f = cache.get(key);
            if (f == null) {
                OriginThreadAwareFuture ft = new OriginThreadAwareFuture(key);

                f = cache.putIfAbsent(key, ft);
                if (f == null) {
                    f = ft;
                    ft.run();
                }
            } else {
                final long tid = f.threadId;
                if ((tid != -1) && (Thread.currentThread().getId() == f.threadId)) {
                    cycleHandler.handleCycle(key);
                }
            }
            try {
                return f.get();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            } catch (ExecutionException ex) {
                cache.remove(key);  // otherwise the exception would be remembered
                Throwable cause = ex.getCause();
                if (cause == null) {
                    throw new RuntimeException(ex);
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            }
        }
    }

    /**
     * Empty cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Returns true if the key has already been cached.
     *
     * @param key item key.
     * @return true if given key is present in the cache.
     */
    public boolean containsKey(final K key) {
        return cache.containsKey(key);
    }

    /**
     * Remove item from the cache.
     *
     * @param key item key.
     */
    public void remove(final K key) {
        cache.remove(key);
    }

    /**
     * Returns the size of the cache
     *
     * @return The number of elements in the cache
     */
    public int size() {
        return cache.size();
    }

    /**
     * Should a cycle be detected during computation of a value
     * for given key, this interface allows client code to register
     * a callback that would get invoked in such a case.
     *
     * @param <K> Key type.
     */
    public interface CycleHandler<K> {

        /**
         * Handle cycle that was detected while computing a cache value
         * for given key. This method would typically just throw a runtime exception.
         *
         * @param key instance that caused the cycle.
         */
        void handleCycle(K key);
    }

    /**
     * Helper class, that remembers the future task origin thread, so that cycles could be detected.
     * If any thread starts computation for given key and the same thread requests the computed value
     * before the computation stops, a cycle is detected and registered cycle handler is called.
     */
    private class OriginThreadAwareFuture implements Future<V> {
        private final FutureTask<V> future;
        private volatile long threadId;

        OriginThreadAwareFuture(K key) {
            this.threadId = Thread.currentThread().getId();
            Callable<V> eval = () -> {
                try {
                    return computable.apply(key);
                } finally {
                    threadId = -1;
                }
            };
            this.future = new FutureTask<>(eval);
        }

        @Override
        public int hashCode() {
            return future.hashCode();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            final OriginThreadAwareFuture other = (OriginThreadAwareFuture) obj;
            if (this.future != other.future && (this.future == null || !this.future.equals(other.future))) {
                return false;
            }
            return true;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }

        public void run() {
            future.run();
        }
    }
}
