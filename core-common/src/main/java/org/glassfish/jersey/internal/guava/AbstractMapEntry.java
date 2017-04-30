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
 * Implementation of the {@code equals}, {@code hashCode}, and {@code toString}
 * methods of {@code Entry}.
 *
 * @author Jared Levy
 */
abstract class AbstractMapEntry<K, V> implements Entry<K, V> {

    @Override
    public abstract K getKey();

    @Override
    public abstract V getValue();

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Entry) {
            Entry<?, ?> that = (Entry<?, ?>) object;
            return Objects.equals(this.getKey(), that.getKey())
                    && Objects.equals(this.getValue(), that.getValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        K k = getKey();
        V v = getValue();
        return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
    }

    /**
     * Returns a string representation of the form {@code {key}={value}}.
     */
    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }
}
