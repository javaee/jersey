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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Provides static methods for serializing collection classes.
 * <p>
 * <p>This class assists the implementation of collection classes. Do not use
 * this class to serialize collections that are defined elsewhere.
 *
 * @author Jared Levy
 */
final class Serialization {
    private Serialization() {
    }

    /**
     * Reads a count corresponding to a serialized map, multiset, or multimap. It
     * returns the size of a map serialized by {@link
     * #writeMap(Map, ObjectOutputStream)}, the number of distinct elements in a
     * multiset serialized by {@link
     * #writeMultiset(Multiset, ObjectOutputStream)}, or the number of distinct
     * keys in a multimap serialized by {@link
     * #writeMultimap(Multimap, ObjectOutputStream)}.
     * <p>
     * <p>The returned count may be used to construct an empty collection of the
     * appropriate capacity before calling any of the {@code populate} methods.
     */
    static int readCount(ObjectInputStream stream) throws IOException {
        return stream.readInt();
    }

    /**
     * Stores the contents of a multimap in an output stream, as part of
     * serialization. It does not support concurrent multimaps whose content may
     * change while the method is running. The {@link Multimap#asMap} view
     * determines the ordering in which data is written to the stream.
     * <p>
     * <p>The serialized output consists of the number of distinct keys, and then
     * for each distinct key: the key, the number of values for that key, and the
     * key's values.
     */
    static <K, V> void writeMultimap(
            Multimap<K, V> multimap, ObjectOutputStream stream) throws IOException {
        stream.writeInt(multimap.asMap().size());
        for (Map.Entry<K, Collection<V>> entry : multimap.asMap().entrySet()) {
            stream.writeObject(entry.getKey());
            stream.writeInt(entry.getValue().size());
            for (V value : entry.getValue()) {
                stream.writeObject(value);
            }
        }
    }

    /**
     * Populates a multimap by reading an input stream, as part of
     * deserialization. See {@link #writeMultimap} for the data format.
     */
    static <K, V> void populateMultimap(
            Multimap<K, V> multimap, ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        int distinctKeys = stream.readInt();
        populateMultimap(multimap, stream, distinctKeys);
    }

    /**
     * Populates a multimap by reading an input stream, as part of
     * deserialization. See {@link #writeMultimap} for the data format. The number
     * of distinct keys is determined by a prior call to {@link #readCount}.
     */
    static <K, V> void populateMultimap(
            Multimap<K, V> multimap, ObjectInputStream stream, int distinctKeys)
            throws IOException, ClassNotFoundException {
        for (int i = 0; i < distinctKeys; i++) {
            @SuppressWarnings("unchecked") // reading data stored by writeMultimap
                    K key = (K) stream.readObject();
            Collection<V> values = multimap.get(key);
            int valueCount = stream.readInt();
            for (int j = 0; j < valueCount; j++) {
                @SuppressWarnings("unchecked") // reading data stored by writeMultimap
                        V value = (V) stream.readObject();
                values.add(value);
            }
        }
    }

}
