/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.message.filtering;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.glassfish.jersey.internal.util.collection.DataStructures;
import org.glassfish.jersey.message.filtering.spi.EntityGraph;
import org.glassfish.jersey.message.filtering.spi.EntityGraphProvider;
import org.glassfish.jersey.message.filtering.spi.ObjectGraph;

/**
 * Provides {@link EntityGraph entity graph} and {@link ObjectGraph object graph} instances.
 *
 * @author Michal Gajdos
 */
final class EntityGraphProviderImpl implements EntityGraphProvider {

    private final ConcurrentMap<Class<?>, EntityGraph> writerClassToGraph = DataStructures.createConcurrentMap();
    private final ConcurrentMap<Class<?>, EntityGraph> readerClassToGraph = DataStructures.createConcurrentMap();

    @Override
    public EntityGraph getOrCreateEntityGraph(final Class<?> entityClass, final boolean forWriter) {
        final ConcurrentMap<Class<?>, EntityGraph> classToGraph = forWriter ? writerClassToGraph : readerClassToGraph;

        if (!classToGraph.containsKey(entityClass)) {
            classToGraph.putIfAbsent(entityClass, new EntityGraphImpl(entityClass));
        }
        return classToGraph.get(entityClass);
    }

    @Override
    public EntityGraph getOrCreateEmptyEntityGraph(final Class<?> entityClass, final boolean forWriter) {
        final ConcurrentMap<Class<?>, EntityGraph> classToGraph = forWriter ? writerClassToGraph : readerClassToGraph;

        if (!classToGraph.containsKey(entityClass)
                || !(classToGraph.get(entityClass) instanceof EmptyEntityGraphImpl)) {
            classToGraph.put(entityClass, new EmptyEntityGraphImpl(entityClass));
        }
        return classToGraph.get(entityClass);
    }

    /**
     * Return an unmodifiable map of entity graphs for reader/writer.
     *
     * @param forWriter flag determining whether the returned map should be for writer/reader.
     * @return an unmodifiable map of entity graphs.
     */
    public Map<Class<?>, EntityGraph> asMap(final boolean forWriter) {
        return Collections.unmodifiableMap(forWriter ? writerClassToGraph : readerClassToGraph);
    }

    @Override
    public boolean containsEntityGraph(final Class<?> entityClass, final boolean forWriter) {
        return forWriter ? writerClassToGraph.containsKey(entityClass) : readerClassToGraph.containsKey(entityClass);
    }

    @Override
    public ObjectGraph createObjectGraph(final Class<?> entityClass, final Set<String> filteringScopes,
                                         final boolean forWriter) {
        final Map<Class<?>, EntityGraph> classToGraph = forWriter ? writerClassToGraph : readerClassToGraph;
        final EntityGraph entityGraph = classToGraph.get(entityClass);

        return entityGraph == null
                ? new EmptyObjectGraph(entityClass)
                : new ObjectGraphImpl(classToGraph, entityGraph, filteringScopes);
    }
}
