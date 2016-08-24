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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * This class contains static utility methods that operate on or return objects
 * of type {@code Iterable}. Except as noted, each method has a corresponding
 * {@link Iterator}-based method in the {@link Iterators} class.
 * <p>
 * <p><i>Performance notes:</i> Unless otherwise noted, all of the iterables
 * produced in this class are <i>lazy</i>, which means that their iterators
 * only advance the backing iteration when absolutely necessary.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Iterables">
 * {@code Iterables}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
final class Iterables {
    private Iterables() {
    }

    /**
     * Returns the first element in {@code iterable} or {@code defaultValue} if
     * the iterable is empty.  The {@link Iterators} analog to this method is
     * {@link Iterators#getNext}.
     * <p>
     * <p>If no default value is desired (and the caller instead wants a
     * {@link NoSuchElementException} to be thrown), it is recommended that
     * {@code iterable.iterator().next()} is used instead.
     *
     * @param defaultValue the default value to return if the iterable is empty
     * @return the first element of {@code iterable} or the default value
     * @since 7.0
     */
    public static <T> T getFirst(Iterable<? extends T> iterable, T defaultValue) {
        return Iterators.getNext(iterable.iterator(), defaultValue);
    }

}
