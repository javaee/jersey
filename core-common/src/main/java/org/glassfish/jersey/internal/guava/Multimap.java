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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A collection that maps keys to values, similar to {@link Map}, but in which
 * each key may be associated with <i>multiple</i> values. You can visualize the
 * contents of a multimap either as a map from keys to <i>nonempty</i>
 * collections of values:
 * <p>
 * <ul>
 * <li>a → 1, 2
 * <li>b → 3
 * </ul>
 * <p>
 * ... or as a single "flattened" collection of key-value pairs:
 * <p>
 * <ul>
 * <li>a → 1
 * <li>a → 2
 * <li>b → 3
 * </ul>
 * <p>
 * <p><b>Important:</b> although the first interpretation resembles how most
 * multimaps are <i>implemented</i>, the design of the {@code Multimap} API is
 * based on the <i>second</i> form. So, using the multimap shown above as an
 * example, the {@link #size} is {@code 3}, not {@code 2}, and the {@link
 * #values} collection is {@code [1, 2, 3]}, not {@code [[1, 2], [3]]}. For
 * those times when the first style is more useful, use the multimap's {@link
 * #asMap} view (or create a {@code Map<K, Collection<V>>} in the first place).
 * <p>
 * <h3>Example</h3>
 * <p>
 * <p>The following code: <pre>   {@code
 * <p>
 *   ListMultimap<String, String> multimap = ArrayListMultimap.create();
 *   for (President pres : US_PRESIDENTS_IN_ORDER) {
 *     multimap.put(pres.firstName(), pres.lastName());
 *   }
 *   for (String firstName : multimap.keySet()) {
 *     List<String> lastNames = multimap.get(firstName);
 *     out.println(firstName + ": " + lastNames);
 *   }}</pre>
 * <p>
 * ... produces output such as: <pre>   {@code
 * <p>
 *   Zachary: [Taylor]
 *   John: [Adams, Adams, Tyler, Kennedy]  // Remember, Quincy!
 *   George: [Washington, Bush, Bush]
 *   Grover: [Cleveland, Cleveland]        // Two, non-consecutive terms, rep'ing NJ!
 *   ...}</pre>
 * <p>
 * <h3>Views</h3>
 * <p>
 * <p>Much of the power of the multimap API comes from the <i>view
 * collections</i> it provides. These always reflect the latest state of the
 * multimap itself. When they support modification, the changes are
 * <i>write-through</i> (they automatically update the backing multimap). These
 * view collections are:
 * <p>
 * <ul>
 * <li>{@link #asMap}, mentioned above</li>
 * <li>{@link #keys}, {@link #keySet}, {@link #values}, {@link #entries}, which
 * are similar to the corresponding view collections of {@link Map}
 * <li>and, notably, even the collection returned by {@link #get get(key)} is an
 * active view of the values corresponding to {@code key}
 * </ul>
 * <p>
 * <p>The collections returned by the {@link #replaceValues replaceValues} and
 * {@link #removeAll removeAll} methods, which contain values that have just
 * been removed from the multimap, are naturally <i>not</i> views.
 * <p>
 * <h3>Subinterfaces</h3>
 * <p>
 * <p>Instead of using the {@code Multimap} interface directly, prefer the
 * subinterfaces {@link ListMultimap} and {@link SetMultimap}. These take their
 * names from the fact that the collections they return from {@code get} behave
 * like (and, of course, implement) {@link List} and {@link Set}, respectively.
 * <p>
 * <p>For example, the "presidents" code snippet above used a {@code
 * ListMultimap}; if it had used a {@code SetMultimap} instead, two presidents
 * would have vanished, and last names might or might not appear in
 * chronological order.
 * <p>
 * <p><b>Warning:</b> instances of type {@code Multimap} may not implement
 * {@link Object#equals} in the way you expect.  Multimaps containing the same
 * key-value pairs, even in the same order, may or may not be equal and may or
 * may not have the same {@code hashCode}. The recommended subinterfaces
 * provide much stronger guarantees.
 * <p>
 * <h3>Comparison to a map of collections</h3>
 * <p>
 * <p>Multimaps are commonly used in places where a {@code Map<K,
 * Collection<V>>} would otherwise have appeared. The differences include:
 * <p>
 * <ul>
 * <li>There is no need to populate an empty collection before adding an entry
 * with {@link #put put}.
 * <li>{@code get} never returns {@code null}, only an empty collection.
 * <li>A key is contained in the multimap if and only if it maps to at least
 * one value. Any operation that causes a key to have zero associated
 * values has the effect of <i>removing</i> that key from the multimap.
 * <li>The total entry count is available as {@link #size}.
 * <li>Many complex operations become easier; for example, {@code
 * Collections.min(multimap.values())} finds the smallest value across all
 * keys.
 * </ul>
 * <p>
 * <h3>Implementations</h3>
 * <p>
 * <p>As always, prefer the immutable implementations, {@link
 * ImmutableListMultimap} and {@link ImmutableSetMultimap}. General-purpose
 * mutable implementations are listed above under "All Known Implementing
 * Classes". You can also create a <i>custom</i> multimap, backed by any {@code
 * Map} and {@link Collection} types, using the {@link Multimaps#newMultimap
 * Multimaps.newMultimap} family of methods. Finally, another popular way to
 * obtain a multimap is using {@link Multimaps#index Multimaps.index}. See
 * the {@link Multimaps} class for these and other static utilities related
 * to multimaps.
 * <p>
 * <h3>Other Notes</h3>
 * <p>
 * <p>As with {@code Map}, the behavior of a {@code Multimap} is not specified
 * if key objects already present in the multimap change in a manner that
 * affects {@code equals} comparisons.  Use caution if mutable objects are used
 * as keys in a {@code Multimap}.
 * <p>
 * <p>All methods that modify the multimap are optional. The view collections
 * returned by the multimap may or may not be modifiable. Any modification
 * method that is not supported will throw {@link
 * UnsupportedOperationException}.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/NewCollectionTypesExplained#Multimap">
 * {@code Multimap}</a>.
 *
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
public interface Multimap<K, V> {
    // Query Operations

    /**
     * Returns the number of key-value pairs in this multimap.
     * <p>
     * <p><b>Note:</b> this method does not return the number of <i>distinct
     * keys</i> in the multimap, which is given by {@code keySet().size()} or
     * {@code asMap().size()}. See the opening section of the {@link Multimap}
     * class documentation for clarification.
     */
    int size();

    /**
     * Returns {@code true} if this multimap contains at least one key-value pair
     * with the key {@code key}.
     */
    boolean containsKey(Object key);

    /**
     * Returns {@code true} if this multimap contains at least one key-value pair
     * with the value {@code value}.
     */
    boolean containsValue(Object value);

    /**
     * Returns {@code true} if this multimap contains at least one key-value pair
     * with the key {@code key} and the value {@code value}.
     */
    boolean containsEntry(Object key, Object value);

    // Modification Operations

    /**
     * Stores a key-value pair in this multimap.
     * <p>
     * <p>Some multimap implementations allow duplicate key-value pairs, in which
     * case {@code put} always adds a new key-value pair and increases the
     * multimap size by 1. Other implementations prohibit duplicates, and storing
     * a key-value pair that's already in the multimap has no effect.
     *
     * @return {@code true} if the method increased the size of the multimap, or
     * {@code false} if the multimap already contained the key-value pair and
     * doesn't allow duplicates
     */
    boolean put(K key, V value);

    /**
     * Removes a single key-value pair with the key {@code key} and the value
     * {@code value} from this multimap, if such exists. If multiple key-value
     * pairs in the multimap fit this description, which one is removed is
     * unspecified.
     *
     * @return {@code true} if the multimap changed
     */
    boolean remove(Object key, Object value);

    // Bulk Operations

    /**
     * Stores a key-value pair in this multimap for each of {@code values}, all
     * using the same key, {@code key}. Equivalent to (but expected to be more
     * efficient than): <pre>   {@code
     * <p>
     *   for (V value : values) {
     *     put(key, value);
     *   }}</pre>
     * <p>
     * <p>In particular, this is a no-op if {@code values} is empty.
     *
     * @return {@code true} if the multimap changed
     */
    boolean putAll(K key, Iterable<? extends V> values);

    /**
     * Removes all values associated with the key {@code key}.
     * <p>
     * <p>Once this method returns, {@code key} will not be mapped to any values,
     * so it will not appear in {@link #keySet()}, {@link #asMap()}, or any other
     * views.
     *
     * @return the values that were removed (possibly empty). The returned
     * collection <i>may</i> be modifiable, but updating it will have no
     * effect on the multimap.
     */
    Collection<V> removeAll(Object key);

    /**
     * Removes all key-value pairs from the multimap, leaving it {@linkplain
     * #isEmpty empty}.
     */
    void clear();

    // Views

    /**
     * Returns a view collection of the values associated with {@code key} in this
     * multimap, if any. Note that when {@code containsKey(key)} is false, this
     * returns an empty collection, not {@code null}.
     * <p>
     * <p>Changes to the returned collection will update the underlying multimap,
     * and vice versa.
     */
    Collection<V> get(K key);

    /**
     * Returns a view collection of all <i>distinct</i> keys contained in this
     * multimap. Note that the key set contains a key if and only if this multimap
     * maps that key to at least one value.
     * <p>
     * <p>Changes to the returned set will update the underlying multimap, and
     * vice versa. However, <i>adding</i> to the returned set is not possible.
     */
    Set<K> keySet();

    /**
     * Returns a view collection containing the <i>value</i> from each key-value
     * pair contained in this multimap, without collapsing duplicates (so {@code
     * values().size() == size()}).
     * <p>
     * <p>Changes to the returned collection will update the underlying multimap,
     * and vice versa. However, <i>adding</i> to the returned collection is not
     * possible.
     */
    Collection<V> values();

    /**
     * Returns a view collection of all key-value pairs contained in this
     * multimap, as {@link Map.Entry} instances.
     * <p>
     * <p>Changes to the returned collection or the entries it contains will
     * update the underlying multimap, and vice versa. However, <i>adding</i> to
     * the returned collection is not possible.
     */
    Collection<Map.Entry<K, V>> entries();

    /**
     * Returns a view of this multimap as a {@code Map} from each distinct key
     * to the nonempty collection of that key's associated values. Note that
     * {@code this.asMap().get(k)} is equivalent to {@code this.get(k)} only when
     * {@code k} is a key contained in the multimap; otherwise it returns {@code
     * null} as opposed to an empty collection.
     * <p>
     * <p>Changes to the returned map or the collections that serve as its values
     * will update the underlying multimap, and vice versa. The map does not
     * support {@code put} or {@code putAll}, nor do its entries support {@link
     * Map.Entry#setValue setValue}.
     */
    Map<K, Collection<V>> asMap();

    // Comparison and hashing

    /**
     * Compares the specified object with this multimap for equality. Two
     * multimaps are equal when their map views, as returned by {@link #asMap},
     * are also equal.
     * <p>
     * <p>In general, two multimaps with identical key-value mappings may or may
     * not be equal, depending on the implementation. For example, two
     * {@link SetMultimap} instances with the same key-value mappings are equal,
     * but equality of two {@link ListMultimap} instances depends on the ordering
     * of the values for each key.
     * <p>
     * <p>A non-empty {@link SetMultimap} cannot be equal to a non-empty
     * {@link ListMultimap}, since their {@link #asMap} views contain unequal
     * collections as values. However, any two empty multimaps are equal, because
     * they both have empty {@link #asMap} views.
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns the hash code for this multimap.
     * <p>
     * <p>The hash code of a multimap is defined as the hash code of the map view,
     * as returned by {@link Multimap#asMap}.
     * <p>
     * <p>In general, two multimaps with identical key-value mappings may or may
     * not have the same hash codes, depending on the implementation. For
     * example, two {@link SetMultimap} instances with the same key-value
     * mappings will have the same {@code hashCode}, but the {@code hashCode}
     * of {@link ListMultimap} instances depends on the ordering of the values
     * for each key.
     */
    @Override
    int hashCode();
}
