/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.AbstractMap;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * Collections utils, which provide transforming views for {@link List} and {@link Map}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class Views {

    private Views() {
        // prevent instantiation.
    }

    /**
     * Create a {@link List} view, which transforms the values of provided original list.
     * <p>
     * Removing elements from the view is supported, adding and setting isn't and
     * throws {@link UnsupportedOperationException} when invoked.
     *
     * @param originalList original list.
     * @param transformer  transforming functions.
     * @param <T>          transformed type parameter.
     * @param <R>          type of the element from provided list.
     * @return transformed list view.
     */
    public static <T, R> List<T> listView(List<R> originalList, Function<R, T> transformer) {
        return new AbstractSequentialList<T>() {
            @Override
            public ListIterator<T> listIterator(int index) {
                return new ListIterator<T>() {

                    final ListIterator<R> iterator = originalList.listIterator(index);

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        return transformer.apply(iterator.next());
                    }

                    @Override
                    public boolean hasPrevious() {
                        return iterator.hasPrevious();
                    }

                    @Override
                    public T previous() {
                        return transformer.apply(iterator.previous());
                    }

                    @Override
                    public int nextIndex() {
                        return iterator.nextIndex();
                    }

                    @Override
                    public int previousIndex() {
                        return iterator.previousIndex();
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }

                    @Override
                    public void set(T t) {
                        throw new UnsupportedOperationException("Not supported.");
                    }

                    @Override
                    public void add(T t) {
                        throw new UnsupportedOperationException("Not supported.");
                    }
                };
            }

            @Override
            public int size() {
                return originalList.size();
            }
        };
    }

    /**
     * Create a {@link Map} view, which transforms the values of provided original map.
     * <p>
     * Removing elements from the map view is supported, adding and setting isn't and
     * throws {@link UnsupportedOperationException} when invoked.
     *
     * @param originalMap       provided map.
     * @param valuesTransformer values transformer.
     * @param <K>               key type.
     * @param <V>               transformed value type.
     * @param <O>               original value type.
     * @return transformed map view.
     */
    public static <K, V, O> Map<K, V> mapView(Map<K, O> originalMap, Function<O, V> valuesTransformer) {
        return new AbstractMap<K, V>() {
            @Override
            public Set<Entry<K, V>> entrySet() {
                return new AbstractSet<Entry<K, V>>() {


                    Set<Entry<K, O>> originalSet = originalMap.entrySet();
                    Iterator<Entry<K, O>> original = originalSet.iterator();

                    @Override
                    public Iterator<Entry<K, V>> iterator() {
                        return new Iterator<Entry<K, V>>() {
                            @Override
                            public boolean hasNext() {
                                return original.hasNext();
                            }

                            @Override
                            public Entry<K, V> next() {

                                Entry<K, O> next = original.next();

                                return new Entry<K, V>() {
                                    @Override
                                    public K getKey() {
                                        return next.getKey();
                                    }

                                    @Override
                                    public V getValue() {
                                        return valuesTransformer.apply(next.getValue());
                                    }

                                    @Override
                                    public V setValue(V value) {
                                        throw new UnsupportedOperationException("Not supported.");
                                    }
                                };
                            }

                            @Override
                            public void remove() {
                                original.remove();
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return originalSet.size();
                    }
                };
            }
        };
    }

    /**
     * Create a view of an union of provided sets.
     * <p>
     * View is updated whenever any of the provided set changes.
     *
     * @param set1 first set.
     * @param set2 second set.
     * @param <E>  set item type.
     * @return union view of given sets.
     */
    public static <E> Set<E> setUnionView(final Set<? extends E> set1, final Set<? extends E> set2) {
        checkNotNull(set1, "set1");
        checkNotNull(set2, "set2");

        return new AbstractSet<E>() {
            @Override
            public Iterator<E> iterator() {
                return getUnion(set1, set2).iterator();
            }

            @Override
            public int size() {
                return getUnion(set1, set2).size();
            }

            private Set<E> getUnion(Set<? extends E> set1, Set<? extends E> set2) {
                HashSet<E> hashSet = new HashSet<>(set1);
                hashSet.addAll(set2);
                return hashSet;
            }
        };
    }

    /**
     * Create a view of a difference of provided sets.
     * <p>
     * View is updated whenever any of the provided set changes.
     *
     * @param set1 first set.
     * @param set2 second set.
     * @param <E>  set item type.
     * @return union view of given sets.
     */
    public static <E> Set<E> setDiffView(final Set<? extends E> set1, final Set<? extends E> set2) {
        checkNotNull(set1, "set1");
        checkNotNull(set2, "set2");

        return new AbstractSet<E>() {
            @Override
            public Iterator<E> iterator() {
                return getDiff(set1, set2).iterator();
            }

            @Override
            public int size() {
                return getDiff(set1, set2).size();
            }

            private Set<E> getDiff(Set<? extends E> set1, Set<? extends E> set2) {
                HashSet<E> hashSet = new HashSet<>();

                hashSet.addAll(set1);
                hashSet.addAll(set2);

                return hashSet.stream().filter(new Predicate<E>() {
                    @Override
                    public boolean test(E e) {
                        return set1.contains(e) && !set2.contains(e);
                    }
                }).collect(Collectors.toSet());
            }
        };
    }
}
