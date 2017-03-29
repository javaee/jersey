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

import java.io.Serializable;
import java.util.Comparator;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * An ordering for a pre-existing comparator.
 */
final class ComparatorOrdering<T> extends Ordering<T> implements Serializable {
    private static final long serialVersionUID = 0;
    private final Comparator<T> comparator;

    ComparatorOrdering(Comparator<T> comparator) {
        this.comparator = checkNotNull(comparator);
    }

    @Override
    public int compare(T a, T b) {
        return comparator.compare(a, b);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ComparatorOrdering) {
            ComparatorOrdering<?> that = (ComparatorOrdering<?>) object;
            return this.comparator.equals(that.comparator);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return comparator.hashCode();
    }

    @Override
    public String toString() {
        return comparator.toString();
    }
}
