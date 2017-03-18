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

import java.util.Map.Entry;
import java.util.Objects;

/**
 * A map entry which forwards all its method calls to another map entry.
 * Subclasses should override one or more methods to modify the behavior of the
 * backing map entry as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 * <p>
 * <p><i>Warning:</i> The methods of {@code ForwardingMapEntry} forward
 * <i>indiscriminately</i> to the methods of the delegate. For example,
 * overriding {@link #getValue} alone <i>will not</i> change the behavior of
 * {@link #equals}, which can lead to unexpected behavior. In this case, you
 * should override {@code equals} as well, either providing your own
 * implementation, or delegating to the provided {@code standardEquals} method.
 * <p>
 * <p>Each of the {@code standard} methods, where appropriate, use {@link
 * Objects#equal} to test equality for both keys and values. This may not be
 * the desired behavior for map implementations that use non-standard notions of
 * key equality, such as the entry of a {@code SortedMap} whose comparator is
 * not consistent with {@code equals}.
 * <p>
 * <p>The {@code standard} methods are not guaranteed to be thread-safe, even
 * when all of the methods that they depend on are thread-safe.
 *
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0 (imported from Google Collections Library)
 */
public abstract class ForwardingMapEntry<K, V>
        extends ForwardingObject implements Entry<K, V> {
    // TODO(user): identify places where thread safety is actually lost

    /**
     * Constructor for use by subclasses.
     */
    ForwardingMapEntry() {
    }

    @Override
    protected abstract Entry<K, V> delegate();

    @Override
    public K getKey() {
        return delegate().getKey();
    }

    @Override
    public V getValue() {
        return delegate().getValue();
    }

    @Override
    public V setValue(V value) {
        return delegate().setValue(value);
    }

    @Override
    public boolean equals(Object object) {
        return delegate().equals(object);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    /**
     * A sensible definition of {@link #equals(Object)} in terms of {@link
     * #getKey()} and {@link #getValue()}. If you override either of these
     * methods, you may wish to override {@link #equals(Object)} to forward to
     * this implementation.
     *
     * @since 7.0
     */
    boolean standardEquals(Object object) {
        if (object instanceof Entry) {
            Entry<?, ?> that = (Entry<?, ?>) object;
            return Objects.equals(this.getKey(), that.getKey())
                    && Objects.equals(this.getValue(), that.getValue());
        }
        return false;
    }

}
