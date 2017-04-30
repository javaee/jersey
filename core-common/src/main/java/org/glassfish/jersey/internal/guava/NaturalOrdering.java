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

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * An ordering that uses the natural order of the values.
 */
@SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
final class NaturalOrdering
        extends Ordering<Comparable> implements Serializable {
    static final NaturalOrdering INSTANCE = new NaturalOrdering();
    private static final long serialVersionUID = 0;

    private NaturalOrdering() {
    }

    @Override
    public int compare(Comparable left, Comparable right) {
        checkNotNull(left); // for GWT
        checkNotNull(right);
        return left.compareTo(right);
    }

    @Override
    public <S extends Comparable> Ordering<S> reverse() {
        return (Ordering<S>) ReverseNaturalOrdering.INSTANCE;
    }

    // preserving singleton-ness gives equals()/hashCode() for free
    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "Ordering.natural()";
    }
}
