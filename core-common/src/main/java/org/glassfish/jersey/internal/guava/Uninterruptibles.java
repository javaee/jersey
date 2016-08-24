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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utilities for treating interruptible operations as uninterruptible.
 * In all cases, if a thread is interrupted during such a call, the call
 * continues to block until the result is available or the timeout elapses,
 * and only then re-interrupts the thread.
 *
 * @author Anthony Zana
 * @since 10.0
 */
final class Uninterruptibles {

    // Implementation Note: As of 3-7-11, the logic for each blocking/timeout
    // methods is identical, save for method being invoked.

    private Uninterruptibles() {
    }

    /**
     * Invokes {@code future.}{@link Future#get() get()} uninterruptibly.
     * To get uninterruptibility and remove checked exceptions, see
     * {@link Futures#getUnchecked}.
     * <p>
     * <p>If instead, you wish to treat {@link InterruptedException} uniformly
     * with other exceptions, see {@link Futures#get(Future, Class) Futures.get}
     * or {@link Futures#makeChecked}.
     *
     * @throws ExecutionException    if the computation threw an exception
     * @throws CancellationException if the computation was cancelled
     */
    public static <V> V getUninterruptibly(Future<V> future)
            throws ExecutionException {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
