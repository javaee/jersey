/*
 * Copyright (C) 2007 The Guava Authors
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
 * Static utility methods pertaining to object arrays.
 *
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
final class ObjectArrays {
    static final Object[] EMPTY_ARRAY = new Object[0];

    private ObjectArrays() {
    }

    /**
     * Returns a new array of the given length with the same type as a reference
     * array.
     *
     * @param reference any array of the desired type
     * @param length    the length of the new array
     */
    public static <T> T[] newArray(T[] reference, int length) {
        return Platform.newArray(reference, length);
    }

    /**
     * GWT safe version of Arrays.copyOf.
     */
    static <T> T[] arraysCopyOf(T[] original, int newLength) {
        T[] copy = newArray(original, newLength);
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }
}
