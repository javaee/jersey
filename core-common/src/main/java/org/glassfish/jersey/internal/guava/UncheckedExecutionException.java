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
 * Unchecked variant of {@link java.util.concurrent.ExecutionException}. As with
 * {@code ExecutionException}, the exception's {@linkplain #getCause() cause}
 * comes from a failed task, possibly run in another thread.
 * <p>
 * <p>{@code UncheckedExecutionException} is intended as an alternative to
 * {@code ExecutionException} when the exception thrown by a task is an
 * unchecked exception. However, it may also wrap a checked exception in some
 * cases.
 * <p>
 * <p>When wrapping an {@code Error} from another thread, prefer {@link
 * ExecutionError}. When wrapping a checked exception, prefer {@code
 * ExecutionException}.
 *
 * @author Charles Fry
 * @since 10.0
 */
public class UncheckedExecutionException extends RuntimeException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new instance with the given cause.
     */
    public UncheckedExecutionException(Throwable cause) {
        super(cause);
    }
}
