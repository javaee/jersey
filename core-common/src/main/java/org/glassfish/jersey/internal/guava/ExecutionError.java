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

/**
 * {@link Error} variant of {@link java.util.concurrent.ExecutionException}. As
 * with {@code ExecutionException}, the error's {@linkplain #getCause() cause}
 * comes from a failed task, possibly run in another thread. That cause should
 * itself be an {@code Error}; if not, use {@code ExecutionException} or {@link
 * UncheckedExecutionException}. This allows the client code to continue to
 * distinguish between exceptions and errors, even when they come from other
 * threads.
 *
 * @author Chris Povirk
 * @since 10.0
 */
public class ExecutionError extends Error {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new instance with the given cause.
     */
    public ExecutionError(Error cause) {
        super(cause);
    }
}
