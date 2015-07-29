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

package org.glassfish.jersey.message.filtering.spi;

import java.util.Map;
import java.util.Set;

/**
 * Class available to {@link EntityProcessor entity-filtering processors} providing means to add/remove entity-filtering scopes
 * (e.g. based on annotations) for entity members.
 * <p/>
 * Differences between this class and {@link ObjectGraph object graph}:
 * <ul>
 * <li>{@code EntityGraph} can be modified, {@code ObjectGraph} is read-only.</li>
 * <li>{@code EntityGraph} contains information about all entity-filtering scopes found during inspecting an entity class,
 * {@code ObjectGraph} provides information about entity to create a filtering object for a subset of these scopes
 * (which are determined from the current context).</li>
 * </ul>
 * <p/>
 * Note: Definition of entity-filtering scopes can be found in {@link ScopeResolver}.
 *
 * @author Michal Gajdos
 * @see ScopeResolver
 */
public interface EntityGraph {

    /**
     * Add a field into this graph for all existing entity-filtering scopes.
     *
     * @param fieldName name of the field to be added.
     * @return an entity-filtering graph instance.
     */
    public EntityGraph addField(final String fieldName);

    /**
     * Add a field into this graph for given list of entity-filtering scopes.
     *
     * @param fieldName name of the field to be added.
     * @param filteringScopes entity-filtering scopes for the field.
     * @return an entity-filtering graph instance.
     */
    public EntityGraph addField(final String fieldName, final String... filteringScopes);

    /**
     * Add a field into this graph for given set of entity-filtering scopes.
     *
     * @param fieldName name of the field to be added.
     * @param filteringScopes entity-filtering scopes for the field.
     * @return an entity-filtering graph instance.
     */
    public EntityGraph addField(final String fieldName, final Set<String> filteringScopes);

    /**
     * Add a subgraph into this graph for all existing entity-filtering scopes.
     *
     * @param fieldName name of the subgraph field to be added.
     * @param fieldClass entity class representing the subgraph.
     * @return an entity-filtering graph instance.
     */
    public EntityGraph addSubgraph(final String fieldName, final Class<?> fieldClass);

    /**
     * Add a subgraph into this graph for given list of entity-filtering scopes.
     *
     * @param fieldName name of the subgraph field to be added.
     * @param fieldClass entity class representing the subgraph.
     * @param filteringScopes entity-filtering scopes for the subgraph.
     * @return an entity-filtering graph instance.
     */
    public EntityGraph addSubgraph(final String fieldName, final Class<?> fieldClass, final String... filteringScopes);

    /**
     * Add a subgraph into this graph for given set of entity-filtering scopes.
     *
     * @param fieldName name of the subgraph field to be added.
     * @param fieldClass entity class representing the subgraph.
     * @param filteringScopes entity-filtering scopes for the subgraph.
     * @return an entity-filtering graph instance.
     */
    public EntityGraph addSubgraph(final String fieldName, final Class<?> fieldClass, final Set<String> filteringScopes);

    /**
     * Add a set of entity-filtering scopes to this graph.
     *
     * @param filteringScopes entity-filtering scopes to be added.
     * @return an entity-filtering graph instance.
     */
    public EntityGraph addFilteringScopes(final Set<String> filteringScopes);

    /**
     * Determines whether a field/subgraph is present in ANY of the given scopes. If no scopes are given the return value
     * determines whether the field is present in any scope.
     *
     * @param field field to be checked.
     * @param filteringScope entity-filtering scope.
     * @return {@code true} if field is present in the given scope, {@code false} otherwise.
     */
    public boolean presentInScope(final String field, final String filteringScope);

    /**
     * Determines whether a field/subgraph is present in ANY of the existing scopes.
     *
     * @param field field to be checked.
     * @return {@code true} if field is present in ANY of the existing scopes, {@code false} otherwise.
     */
    public boolean presentInScopes(final String field);

    /**
     * Get an entity class this graph is created for.
     *
     * @return an entity class.
     */
    public Class<?> getEntityClass();

    /**
     * Get fields for given entity-filtering scope.
     *
     * @param filteringScope scope the returned fields have to be in.
     * @return set of fields present in given scope.
     */
    public Set<String> getFields(final String filteringScope);

    /**
     * Get fields for given entity-filtering scopes.
     *
     * @param filteringScopes scopes the returned fields have to be in.
     * @return set of fields present in given scopes.
     */
    public Set<String> getFields(final String... filteringScopes);

    /**
     * Get fields for given entity-filtering scopes.
     *
     * @param filteringScopes scopes the returned fields have to be in.
     * @return set of fields present in given scopes.
     */
    public Set<String> getFields(final Set<String> filteringScopes);

    /**
     * Get all available entity-filtering scopes.
     *
     * @return all available entity-filtering scopes.
     */
    public Set<String> getFilteringScopes();

    /**
     * Get all available entity-filtering scopes defined on a class.
     *
     * @return all available entity-filtering scopes.
     */
    public Set<String> getClassFilteringScopes();

    /**
     * Get subgraphs for given entity-filtering scope.
     *
     * @param filteringScope scope the returned subgraphs have to be in.
     * @return map of subgraphs present in given scope.
     */
    public Map<String, Class<?>> getSubgraphs(final String filteringScope);

    /**
     * Get subgraphs for given entity-filtering scopes.
     *
     * @param filteringScopes scopes the returned subgraphs have to be in.
     * @return map of subgraphs present in given scopes.
     */
    public Map<String, Class<?>> getSubgraphs(final String... filteringScopes);

    /**
     * Get subgraphs for given entity-filtering scopes.
     *
     * @param filteringScopes scopes the returned subgraphs have to be in.
     * @return map of subgraphs present in given scopes.
     */
    public Map<String, Class<?>> getSubgraphs(final Set<String> filteringScopes);

    /**
     * Remove a field/subgraph from the graph (all entity-filtering scopes).
     *
     * @param name name of the field/subgraph to be removed.
     * @return an entity-filtering graph instance.
     */
    public EntityGraph remove(final String name);
}
