/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Adapter from Guava {@code ListMultimap} to JAX-RS {@link MultivaluedMap}.
 *
 * @param <K>
 * @param <V>
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ListMultimapAdapter<K, V> implements MultivaluedMap<K, V> {

    private final ListMultimap<K, V> multimap;
    private transient Map<K, List<V>> mapView;

    public ListMultimapAdapter(ListMultimap<K, V> multimap) {
        this.multimap = multimap;
    }

    @Override
    public void putSingle(K key, V value) {
        multimap.replaceValues(key, Collections.singleton(value));
    }

    @Override
    public void add(K key, V value) {
        multimap.put(key, value);
    }

    @Override
    public V getFirst(K key) {
        final Iterator<V> values = multimap.get(key).iterator();
        if (values.hasNext()) {
            return values.next();
        }

        return null;
    }

    @Override
    public int size() {
        return multimap.size();
    }

    @Override
    public boolean isEmpty() {
        return multimap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return multimap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return multimap.containsValue(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<V> get(Object key) {
        if (multimap.containsKey(key)) { // making sure we can cast
            return multimap.get((K) key);
        }
        return null;
    }

    @Override
    public List<V> put(K key, List<V> values) {
        List<V> oldValues = null;
        if (multimap.containsKey(key)) {
            oldValues = multimap.removeAll(key);
        }
        multimap.putAll(key, values);
        return oldValues;
    }

    @Override
    public List<V> remove(Object key) {
        if (multimap.containsKey(key)) {
            return multimap.removeAll(key);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends K, ? extends List<V>> map) {
        for (Entry<? extends K, ? extends List<V>> e : map.entrySet()) {
            multimap.putAll(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        multimap.clear();
    }

    @Override
    public Set<K> keySet() {
        return multimap.keySet();
    }

    @Override
    public Collection<List<V>> values() {
        return mapView().values();
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        return mapView().entrySet();
    }

    private Map<K, List<V>> mapView() {
        Map<K, List<V>> result = mapView;
        return (result == null) ? mapView = Maps.transformValues(multimap.asMap(), new Function<Collection<V>, List<V>>() {

            @Override
            public List<V> apply(Collection<V> input) {
                return (List<V>) input;
            }
        }) : result;
    }
}
