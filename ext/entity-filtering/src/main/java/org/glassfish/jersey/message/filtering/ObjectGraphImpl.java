/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.message.filtering.spi.EntityGraph;
import org.glassfish.jersey.message.filtering.spi.ObjectGraph;
import org.glassfish.jersey.message.filtering.spi.ScopeProvider;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * Default implementation of {@link ObjectGraph}.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
final class ObjectGraphImpl implements ObjectGraph {

    private final Set<String> filteringScopes;
    private final boolean fallbackToDefault;

    private final Map<Class<?>, EntityGraph> classToGraph;
    private final EntityGraph graph;

    private Set<String> fields;
    private Map<String, ObjectGraph> subgraphs;

    ObjectGraphImpl(final Map<Class<?>, EntityGraph> classToGraph, final EntityGraph graph, final Set<String> filteringScopes,
                    final boolean fallbackToDefault) {
        this.filteringScopes = filteringScopes;
        this.fallbackToDefault = fallbackToDefault;

        this.classToGraph = classToGraph;
        this.graph = graph;
    }

    @Override
    public Class<?> getEntityClass() {
        return graph.getEntityClass();
    }

    @Override
    public Set<String> getFields() {
        if (fields == null) {
            final Set<String> scopeFields = graph.getFields(filteringScopes);
            fields = scopeFields.isEmpty() ? graph.getFields(ScopeProvider.DEFAULT_SCOPE) : scopeFields;
        }
        return fields;
    }

    @Override
    public Map<String, ObjectGraph> getSubgraphs() {
        if (subgraphs == null) {
            Map<String, Class<?>> contextSubgraphs = graph.getSubgraphs(filteringScopes);
            if (contextSubgraphs.isEmpty() && fallbackToDefault) {
                contextSubgraphs = graph.getSubgraphs(ScopeProvider.DEFAULT_SCOPE);
            }

            subgraphs = Maps.transformValues(contextSubgraphs, new Function<Class<?>, ObjectGraph>() {

                @Override
                public ObjectGraphImpl apply(final Class<?> clazz) {
                    return new ObjectGraphImpl(classToGraph, classToGraph.get(clazz), filteringScopes, true);
                }
            });
        }
        return subgraphs;
    }
}
