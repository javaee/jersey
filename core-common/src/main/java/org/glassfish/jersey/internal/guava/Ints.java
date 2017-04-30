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

import java.util.Arrays;

/**
 * Static utility methods pertaining to {@code int} primitives, that are not
 * already found in either {@link Integer} or {@link Arrays}.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/PrimitivesExplained">
 * primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
final class Ints {
    /**
     * The largest power of two that can be represented as an {@code int}.
     *
     * @since 10.0
     */
    public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);
    private static final byte[] asciiDigits = new byte[128];

    static {
        Arrays.fill(asciiDigits, (byte) -1);
        for (int i = 0; i <= 9; i++) {
            asciiDigits['0' + i] = (byte) i;
        }
        for (int i = 0; i <= 26; i++) {
            asciiDigits['A' + i] = (byte) (10 + i);
            asciiDigits['a' + i] = (byte) (10 + i);
        }
    }

    private Ints() {
    }

    /**
     * Returns the {@code int} nearest in value to {@code value}.
     *
     * @param value any {@code long} value
     * @return the same value cast to {@code int} if it is in the range of the
     * {@code int} type, {@link Integer#MAX_VALUE} if it is too large,
     * or {@link Integer#MIN_VALUE} if it is too small
     */
    public static int saturatedCast(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

}
