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

import org.glassfish.jersey.message.filtering.spi.EntityGraph;
import org.glassfish.jersey.message.filtering.spi.ScopeProvider;

import jersey.repackaged.com.google.common.collect.HashBasedTable;
import jersey.repackaged.com.google.common.collect.HashMultimap;
import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.collect.Table;

/**
 * Default implementation of {@link EntityGraph}.
 *
 * @author Michal Gajdos
 */
final class EntityGraphImpl implements EntityGraph {

    private final Class<?> entityClass;

    private final Set<String> globalScopes;
    private final Set<String> localScopes;

    // <FilteringScope, FieldName>
    private final HashMultimap<String, String> fields;
    // <FilteringScope, FieldName, Class>
    private final Table<String, String, Class<?>> subgraphs;

    /**
     * Create an entity graph for given class.
     *
     * @param entityClass entity class the graph should be created for.
     */
    public EntityGraphImpl(final Class<?> entityClass) {
        this.entityClass = entityClass;

        this.fields = HashMultimap.create();
        this.subgraphs = HashBasedTable.create();

        this.globalScopes = Sets.newHashSet();
        this.localScopes = Sets.newHashSet();
    }

    @Override
    public EntityGraphImpl addField(final String fieldName) {
        return addField(fieldName, globalScopes);
    }

    @Override
    public EntityGraphImpl addField(final String fieldName, final String... filteringScopes) {
        return addField(fieldName, Sets.newHashSet(filteringScopes));
    }

    @Override
    public EntityGraphImpl addField(final String fieldName, final Set<String> filteringScopes) {
        for (final String filteringScope : filteringScopes) {
            createFilteringScope(filteringScope);
            fields.get(filteringScope).add(fieldName);
        }

        return this;
    }

    @Override
    public EntityGraphImpl addFilteringScopes(final Set<String> filteringScopes) {
        this.globalScopes.addAll(filteringScopes);
        return this;
    }

    @Override
    public EntityGraphImpl addSubgraph(final String fieldName, final Class<?> fieldClass) {
        return addSubgraph(fieldName, fieldClass, globalScopes);
    }

    @Override
    public EntityGraphImpl addSubgraph(final String fieldName, final Class<?> fieldClass, final String... filteringScopes) {
        return addSubgraph(fieldName, fieldClass, Sets.newHashSet(filteringScopes));
    }

    @Override
    public EntityGraphImpl addSubgraph(final String fieldName, final Class<?> fieldClass, final Set<String> filteringScopes) {
        for (final String filteringScope : filteringScopes) {
            createFilteringScope(filteringScope);
            subgraphs.put(filteringScope, fieldName, fieldClass);
        }

        return this;
    }

    @Override
    public Class<?> getEntityClass() {
        return entityClass;
    }

    @Override
    public Set<String> getFields(final String filteringScope) {
        return fields.containsKey(filteringScope)
                ? Collections.unmodifiableSet(fields.get(filteringScope)) : Collections.<String>emptySet();
    }

    @Override
    public Set<String> getFields(final String... filteringScopes) {
        return filteringScopes.length == 0 ? Collections.<String>emptySet()
                : (filteringScopes.length == 1 ? getFields(filteringScopes[0]) : getFields(Sets.newHashSet(filteringScopes)));
    }

    @Override
    public Set<String> getFields(final Set<String> filteringScopes) {
        final Set<String> matched = Sets.newHashSet();

        for (final String filteringContext : filteringScopes) {
            matched.addAll(fields.get(filteringContext));
        }

        return matched;
    }

    @Override
    public Set<String> getFilteringScopes() {
        return Collections.unmodifiableSet(Sets.union(globalScopes, localScopes));
    }

    @Override
    public Set<String> getClassFilteringScopes() {
        return Collections.unmodifiableSet(globalScopes);
    }

    @Override
    public Map<String, Class<?>> getSubgraphs(final String filteringScope) {
        return subgraphs.containsRow(filteringScope)
                ? Collections.unmodifiableMap(subgraphs.row(filteringScope)) : Collections.<String, Class<?>>emptyMap();
    }

    @Override
    public Map<String, Class<?>> getSubgraphs(final String... filteringScopes) {
        return filteringScopes.length == 0
                ? Collections.<String, Class<?>>emptyMap()
                : (filteringScopes.length == 1
                           ? getSubgraphs(filteringScopes[0])
                           : getSubgraphs(Sets.newHashSet(filteringScopes)));
    }

    @Override
    public Map<String, Class<?>> getSubgraphs(final Set<String> filteringScopes) {
        final Map<String, Class<?>> matched = Maps.newHashMap();

        for (final String filteringContext : filteringScopes) {
            matched.putAll(subgraphs.row(filteringContext));
        }

        return matched;
    }

    @Override
    public boolean presentInScopes(final String name) {
        return fields.containsValue(name) || subgraphs.containsColumn(name);
    }

    @Override
    public boolean presentInScope(final String field, final String filteringScope) {
        return fields.containsEntry(filteringScope, field) || subgraphs.contains(filteringScope, field);
    }

    @Override
    public EntityGraphImpl remove(final String fieldName) {
        for (final String scope : getFilteringScopes()) {
            if (fields.containsEntry(scope, fieldName)) {
                fields.remove(scope, fieldName);
            }
            if (subgraphs.containsColumn(fieldName)) {
                subgraphs.remove(scope, fieldName);
            }
        }
        return this;
    }

    /**
     * Create a new entity-filtering scope based on the {@link ScopeProvider#DEFAULT_SCOPE default one}.
     *
     * @param filteringScope entity-filtering scope to be created.
     */
    private void createFilteringScope(final String filteringScope) {
        // Do not create a scope if it already exists.
        if (!getFilteringScopes().contains(filteringScope)) {
            // Copy contents of default scope into the new one.
            if (localScopes.contains(ScopeProvider.DEFAULT_SCOPE)) {
                fields.putAll(filteringScope, fields.get(ScopeProvider.DEFAULT_SCOPE));

                final Map<String, Class<?>> row = subgraphs.row(ScopeProvider.DEFAULT_SCOPE);
                for (final Map.Entry<String, Class<?>> entry : row.entrySet()) {
                    subgraphs.put(filteringScope, entry.getKey(), entry.getValue());
                }
            }
            localScopes.add(filteringScope);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EntityGraphImpl that = (EntityGraphImpl) o;

        return entityClass.equals(that.entityClass)
                && fields.equals(that.fields)
                && globalScopes.equals(that.globalScopes)
                && localScopes.equals(that.localScopes)
                && subgraphs.equals(that.subgraphs);
    }

    @Override
    public int hashCode() {
        int result = entityClass.hashCode();
        result = 53 * result + globalScopes.hashCode();
        result = 53 * result + localScopes.hashCode();
        result = 53 * result + fields.hashCode();
        result = 53 * result + subgraphs.hashCode();
        return result;
    }
}
