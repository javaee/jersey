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

import java.util.Comparator;
import java.util.SortedSet;

/**
 * A sorted set which forwards all its method calls to another sorted set.
 * Subclasses should override one or more methods to modify the behavior of the
 * backing sorted set as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 * <p>
 * <p><i>Warning:</i> The methods of {@code ForwardingSortedSet} forward
 * <i>indiscriminately</i> to the methods of the delegate. For example,
 * overriding {@link #add} alone <i>will not</i> change the behavior of {@link
 * #addAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code addAll} as well, either providing your own implementation, or
 * delegating to the provided {@code standardAddAll} method.
 * <p>
 * <p>Each of the {@code standard} methods, where appropriate, uses the set's
 * comparator (or the natural ordering of the elements, if there is no
 * comparator) to test element equality. As a result, if the comparator is not
 * consistent with equals, some of the standard implementations may violate the
 * {@code Set} contract.
 * <p>
 * <p>The {@code standard} methods and the collection views they return are not
 * guaranteed to be thread-safe, even when all of the methods that they depend
 * on are thread-safe.
 *
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0 (imported from Google Collections Library)
 */
public abstract class ForwardingSortedSet<E> extends ForwardingSet<E>
        implements SortedSet<E> {

    /**
     * Constructor for use by subclasses.
     */
    ForwardingSortedSet() {
    }

    @Override
    protected abstract SortedSet<E> delegate();

    @Override
    public Comparator<? super E> comparator() {
        return delegate().comparator();
    }

    @Override
    public E first() {
        return delegate().first();
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return delegate().headSet(toElement);
    }

    @Override
    public E last() {
        return delegate().last();
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return delegate().subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return delegate().tailSet(fromElement);
    }

}
