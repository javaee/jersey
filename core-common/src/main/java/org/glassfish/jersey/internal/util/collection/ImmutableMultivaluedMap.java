/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.internal.util.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * An immutable view of a {@link MultivaluedMap}.
 *
 * @param <K> the key
 * @param <V> the value
 * @author Gili Tzabari
 * @author Michal Gajdos
 */
public class ImmutableMultivaluedMap<K, V> implements MultivaluedMap<K, V> {

    /**
     * Returns an empty immutable map.
     *
     * @return an empty immutable map.
     */
    public static <K, V> ImmutableMultivaluedMap<K, V> empty() {
        return new ImmutableMultivaluedMap<K, V>(new MultivaluedHashMap<K, V>());
    }

    private final MultivaluedMap<K, V> delegate;

    /**
     * Creates a new ImmutableMultivaluedMap.
     *
     * @param delegate the underlying MultivaluedMap
     */
    public ImmutableMultivaluedMap(final MultivaluedMap<K, V> delegate) {
        if (delegate == null) {
            throw new NullPointerException("ImmutableMultivaluedMap delegate must not be 'null'.");
        }
        this.delegate = delegate;
    }

    @Override
    public boolean equalsIgnoreValueOrder(final MultivaluedMap<K, V> otherMap) {
        return delegate.equalsIgnoreValueOrder(otherMap);
    }

    @Override
    public void putSingle(final K key, final V value) {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public void add(final K key, final V value) {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public V getFirst(final K key) {
        return delegate.getFirst(key);
    }

    @Override
    public void addAll(final K key, final V... newValues) {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public void addAll(final K key, final List<V> valueList) {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public void addFirst(final K key, final V value) {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public List<V> get(final Object key) {
        return delegate.get(key);
    }

    @Override
    public List<V> put(final K key, final List<V> value) {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public List<V> remove(final Object key) {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public void putAll(final Map<? extends K, ? extends List<V>> m) {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This MultivaluedMap implementation is immutable.");
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }

    @Override
    public Collection<List<V>> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImmutableMultivaluedMap)) {
            return false;
        }

        final ImmutableMultivaluedMap that = (ImmutableMultivaluedMap) o;

        if (!delegate.equals(that.delegate)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
