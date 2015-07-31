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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.glassfish.jersey.message.filtering.spi.EntityGraph;
import org.glassfish.jersey.message.filtering.spi.EntityProcessorContext;

/**
 * Default {@link EntityProcessorContext entity processor context} implementation.
 *
 * @author Michal Gajdos
 */
final class EntityProcessorContextImpl implements EntityProcessorContext {

    private final Type type;

    private final Class<?> clazz;

    private final Field field;

    private final Method method;

    private final EntityGraph graph;

    /**
     * Create entity processor context for processing entity classes.
     *
     * @param type {@link Type#CLASS_READER} or {@link Type#CLASS_WRITER}.
     * @param clazz entity class.
     * @param graph entity-filtering graph associated with entity class.
     */
    public EntityProcessorContextImpl(final Type type, final Class<?> clazz, final EntityGraph graph) {
        this(type, clazz, null, null, graph);
    }

    /**
     * Create entity processor context for processing entity properties.
     *
     * @param type {@link Type#PROPERTY_READER} or {@link Type#PROPERTY_WRITER}.
     * @param field entity property field.
     * @param method entity property accessor.
     * @param graph entity-filtering graph associated with entity class.
     */
    public EntityProcessorContextImpl(final Type type, final Field field, final Method method, final EntityGraph graph) {
        this(type, null, field, method, graph);
    }

    /**
     * Create entity processor context for processing entity accessors.
     *
     * @param type {@link Type#METHOD_READER} or {@link Type#METHOD_WRITER}.
     * @param method entity property accessor.
     * @param graph entity-filtering graph associated with entity class.
     */
    public EntityProcessorContextImpl(final Type type, final Method method, final EntityGraph graph) {
        this(type, null, null, method, graph);
    }

    /**
     * Create entity processor context for processing entity accessors.
     *
     * @param type type on entity processor context.
     * @param clazz entity class.
     * @param field entity property field.
     * @param method entity property method.
     * @param graph entity-filtering graph associated with entity class.
     */
    public EntityProcessorContextImpl(final Type type, final Class<?> clazz, final Field field, final Method method,
                                      final EntityGraph graph) {
        this.type = type;
        this.clazz = clazz;
        this.field = field;
        this.method = method;
        this.graph = graph;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Class<?> getEntityClass() {
        return clazz;
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public EntityGraph getEntityGraph() {
        return graph;
    }
}
