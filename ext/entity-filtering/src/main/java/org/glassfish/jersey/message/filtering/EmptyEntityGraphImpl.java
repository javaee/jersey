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

/**
 * {@link EntityGraph} implementation that does not contain any fields/subgraphs. Methods that are supposed to modify the graph
 * would throw an {@link UnsupportedOperationException}.
 *
 * @author Michal Gajdos
 */
final class EmptyEntityGraphImpl implements EntityGraph {

    private final Class<?> clazz;

    @SuppressWarnings("JavaDoc")
    EmptyEntityGraphImpl(final Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public EntityGraph addField(final String fieldName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityGraph addField(final String fieldName, final String... filteringScopes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityGraph addField(final String fieldName, final Set<String> filteringScopes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityGraph addSubgraph(final String fieldName, final Class<?> fieldClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityGraph addSubgraph(final String fieldName, final Class<?> fieldClass, final String... filteringScopes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityGraph addSubgraph(final String fieldName, final Class<?> fieldClass, final Set<String> filteringScopes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getEntityClass() {
        return clazz;
    }

    @Override
    public Set<String> getFields(final String filteringScope) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getFields(final String... filteringScopes) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getFields(final Set<String> filteringScopes) {
        return Collections.emptySet();
    }

    @Override
    public Map<String, Class<?>> getSubgraphs(final String filteringScope) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Class<?>> getSubgraphs(final String... filteringScopes) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Class<?>> getSubgraphs(final Set<String> filteringScopes) {
        return Collections.emptyMap();
    }

    @Override
    public boolean presentInScopes(final String field) {
        return false;
    }

    @Override
    public boolean presentInScope(final String field, String filteringScope) {
        return false;
    }

    @Override
    public EntityGraph remove(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getFilteringScopes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getClassFilteringScopes() {
        return Collections.emptySet();
    }

    @Override
    public EntityGraph addFilteringScopes(final Set<String> filteringScopes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EmptyEntityGraphImpl that = (EmptyEntityGraphImpl) o;

        return clazz.equals(that.clazz);

    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }
}
