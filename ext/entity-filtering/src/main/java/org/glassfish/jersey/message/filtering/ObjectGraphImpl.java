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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.jersey.message.filtering.spi.EntityGraph;
import org.glassfish.jersey.message.filtering.spi.ObjectGraph;
import org.glassfish.jersey.message.filtering.spi.ScopeProvider;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Default implementation of {@link ObjectGraph}.
 *
 * @author Michal Gajdos
 */
final class ObjectGraphImpl implements ObjectGraph {

    private final Set<String> filteringScopes;

    private final Map<Class<?>, EntityGraph> classToGraph;
    private final EntityGraph graph;

    private Set<String> fields;
    private Map<String, ObjectGraph> subgraphs;

    ObjectGraphImpl(final Map<Class<?>, EntityGraph> classToGraph, final EntityGraph graph, final Set<String> filteringScopes) {
        this.filteringScopes = filteringScopes;

        this.classToGraph = classToGraph;
        this.graph = graph;
    }

    @Override
    public Class<?> getEntityClass() {
        return graph.getEntityClass();
    }

    @Override
    public Set<String> getFields() {
        return getFields(null);
    }

    @Override
    public Set<String> getFields(final String parent) {
        final Set<String> childFilteringScopes = getFilteringScopes(parent);
        if (fields == null) {
            fields = graph.getFields(Sets.union(childFilteringScopes, Collections.singleton(ScopeProvider.DEFAULT_SCOPE)));
        }
        return fields;
    }

    @Override
    public Map<String, ObjectGraph> getSubgraphs() {
        return getSubgraphs(null);
    }

    @Override
    public Map<String, ObjectGraph> getSubgraphs(final String parent) {
        final Set<String> childFilteringScopes = getFilteringScopes(parent);

        if (subgraphs == null) {
            final Map<String, Class<?>> contextSubgraphs = graph.getSubgraphs(childFilteringScopes);
            contextSubgraphs.putAll(graph.getSubgraphs(ScopeProvider.DEFAULT_SCOPE));

            subgraphs = Maps.transformValues(contextSubgraphs, new Function<Class<?>, ObjectGraph>() {

                @Override
                public ObjectGraph apply(final Class<?> clazz) {
                    final EntityGraph entityGraph = classToGraph.get(clazz);

                    return entityGraph == null
                        ? new EmptyObjectGraph(clazz)
                        : new ObjectGraphImpl(classToGraph, entityGraph, filteringScopes);
                }
            });
        }
        return subgraphs;
    }

    private Set<String> getFilteringScopes(final String parent) {
        Set<String> childFilteringScopes = new HashSet<>();
        if (filteringScopes.contains(SelectableScopeResolver.DEFAULT_SCOPE) || parent == null) {
            childFilteringScopes = filteringScopes;
        } else {
            for (final String filteringScope : filteringScopes) {
                final Pattern p = Pattern.compile(SelectableScopeResolver.PREFIX + parent + "\\.(\\w+)(\\.\\w+)*$");
                final Matcher m = p.matcher(filteringScope);
                if (m.matches()) {
                    childFilteringScopes.add(SelectableScopeResolver.PREFIX + m.group(1));
                } else {
                    childFilteringScopes.add(filteringScope);
                }
            }
        }
        return childFilteringScopes;
    }

}
