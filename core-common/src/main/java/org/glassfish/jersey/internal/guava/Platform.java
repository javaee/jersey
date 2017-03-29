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

import java.lang.reflect.Array;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Function;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 *
 * @author Jesse Wilson
 */
final class Platform {
    private Platform() {
    }

    /**
     * Calls {@link System#nanoTime()}.
     */
    static long systemNanoTime() {
        return System.nanoTime();
    }

    /**
     * Returns a new array of the given length with the same type as a reference
     * array.
     *
     * @param reference any array of the desired type
     * @param length    the length of the new array
     */
    static <T> T[] newArray(T[] reference, int length) {
        Class<?> type = reference.getClass().getComponentType();

        // the cast is safe because
        // result.getClass() == reference.getClass().getComponentType()
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, length);
        return result;
    }
}
