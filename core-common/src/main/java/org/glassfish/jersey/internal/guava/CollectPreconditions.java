/*
 * Copyright (C) 2008 The Guava Authors
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

import static org.glassfish.jersey.internal.guava.Preconditions.checkState;

/**
 * Precondition checks useful in collection implementations.
 */
final class CollectPreconditions {

    static int checkNonnegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative but was: " + value);
        }
        return value;
    }

    /**
     * Precondition tester for {@code Iterator.remove()} that throws an exception with a consistent
     * error message.
     */
    static void checkRemove(boolean canRemove) {
        checkState(canRemove, "no calls to next() since the last call to remove()");
    }
}
