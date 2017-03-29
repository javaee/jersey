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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link Multimap} using hash tables.
 * <p>
 * <p>The multimap does not store duplicate key-value pairs. Adding a new
 * key-value pair equal to an existing key-value pair has no effect.
 * <p>
 * <p>Keys and values may be null. All optional multimap methods are supported,
 * and all returned views are modifiable.
 * <p>
 * <p>This class is not threadsafe when any concurrent operations update the
 * multimap. Concurrent read operations will work correctly. To allow concurrent
 * update operations, wrap your multimap with a call to {@link
 * Multimaps#synchronizedSetMultimap}.
 *
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
public final class HashMultimap<K, V> extends AbstractSetMultimap<K, V> {
    private static final int DEFAULT_VALUES_PER_KEY = 2;
    private static final long serialVersionUID = 0;
    private transient int expectedValuesPerKey = DEFAULT_VALUES_PER_KEY;

    private HashMultimap() {
        super(new HashMap<K, Collection<V>>());
    }

    /**
     * Creates a new, empty {@code HashMultimap} with the default initial
     * capacities.
     */
    public static <K, V> HashMultimap<K, V> create() {
        return new HashMultimap<K, V>();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>Creates an empty {@code HashSet} for a collection of values for one key.
     *
     * @return a new {@code HashSet} containing a collection of values for one key
     */
    @Override
    Set<V> createCollection() {
        return Sets.newHashSetWithExpectedSize(expectedValuesPerKey);
    }

    /**
     * @serialData expectedValuesPerKey, number of distinct keys, and then for
     * each distinct key: the key, number of values for that key, and the
     * key's values
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(expectedValuesPerKey);
        Serialization.writeMultimap(this, stream);
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        expectedValuesPerKey = stream.readInt();
        int distinctKeys = Serialization.readCount(stream);
        Map<K, Collection<V>> map = Maps.newHashMapWithExpectedSize(distinctKeys);
        setMap(map);
        Serialization.populateMultimap(this, stream, distinctKeys);
    }
}
