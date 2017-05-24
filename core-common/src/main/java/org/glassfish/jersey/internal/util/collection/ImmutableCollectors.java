/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

/**
 * Set of convenient function regarding a collection immutability. Particularly useful in the conjunction with
 * {@link java.util.stream.Stream}.
 */
public class ImmutableCollectors {

    /**
     * Creates a {@link Collector} of an immutable list for {@link java.util.stream.Stream#collect(Collector)}.
     *
     * @param <T> type of the immutable list.
     * @return collector for immutable list.
     */
    public static <T> Collector<T, List<T>, List<T>> toImmutableList() {
        return Collector.of(ArrayList::new, List::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, Collections::unmodifiableList);
    }

    /**
     * Creates a {@link Collector} of an immutable Set for {@link java.util.stream.Stream#collect(Collector)}.
     *
     * @param <T> type of the immutable set.
     * @return collector for immutable set.
     */
    public static <T> Collector<T, Set<T>, Set<T>> toImmutableSet() {
        return Collector.of(HashSet::new, Set::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, Collections::unmodifiableSet);
    }

    /**
     * Creates a {@link Collector} of an immutable Set for {@link java.util.stream.Stream#collect(Collector)}.
     *
     * @param <T> type of the immutable linked hash set.
     * @return collector for immutable linked hash set.
     */
    public static <T> Collector<T, Set<T>, Set<T>> toImmutableLinkedSet() {
        return Collector.of(LinkedHashSet::new, Set::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, Collections::unmodifiableSet);
    }
}