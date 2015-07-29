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

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Common {@link EntityProcessor entity processor} supposed to be used as a base class for custom implementations. Provides
 * convenient methods for adding entity-filtering scopes to {@link EntityGraph entity graph} as well as a common implementation
 * of {@link #process(org.glassfish.jersey.message.filtering.spi.EntityProcessorContext)}.
 *
 * @author Michal Gajdos
 */
public abstract class AbstractEntityProcessor implements EntityProcessor {

    @Override
    public EntityProcessor.Result process(final EntityProcessorContext context) {
        switch (context.getType()) {
            case CLASS_READER:
            case CLASS_WRITER:
                return process(null, null, FilteringHelper.EMPTY_ANNOTATIONS, context.getEntityClass().getDeclaredAnnotations(),
                        context.getEntityGraph());

            case PROPERTY_READER:
            case PROPERTY_WRITER:
            case METHOD_READER:
            case METHOD_WRITER:
                final Field field = context.getField();
                final Method method = context.getMethod();

                final boolean isProperty = field != null;
                String fieldName;
                Type fieldType;

                if (isProperty) {
                    fieldName = field.getName();
                    fieldType = field.getGenericType();
                } else {
                    fieldName = ReflectionHelper.getPropertyName(method);
                    fieldType = ReflectionHelper.isGetter(method) ? method.getGenericReturnType() : method
                            .getGenericParameterTypes()[0];
                }

                return process(fieldName, FilteringHelper.getEntityClass(fieldType), getAnnotations(field),
                        getAnnotations(method), context.getEntityGraph());

            default:
                // NOOP.
        }
        return EntityProcessor.Result.SKIP;
    }

    private Annotation[] getAnnotations(final AccessibleObject accessibleObject) {
        return accessibleObject == null ? FilteringHelper.EMPTY_ANNOTATIONS : accessibleObject.getDeclaredAnnotations();
    }

    /**
     * Method is called from the default implementation of
     * {@link #process(org.glassfish.jersey.message.filtering.spi.EntityProcessorContext)} and is supposed to be overridden by
     * custom implementations of this class.
     *
     * @param fieldName name of the field (can be {@code null}).
     * @param fieldClass class of the field (can be {@code null}).
     * @param fieldAnnotations annotations associated with the field (cannot be {@code null}).
     * @param annotations annotations associated with class/accessor (cannot be {@code null}).
     * @param graph entity graph to be processed.
     * @return result of the processing (default is {@link Result#SKIP}).
     */
    protected Result process(final String fieldName, final Class<?> fieldClass, final Annotation[] fieldAnnotations,
                             final Annotation[] annotations, final EntityGraph graph) {
        return Result.SKIP;
    }

    /**
     * Add entity-filtering scopes of a field to an entity-graph. The method determines whether the field should be added as a
     * simple field or a subgraph.
     *
     * @param field name of a field to be added to the graph.
     * @param fieldClass class of the field.
     * @param filteringScopes entity-filtering scopes the field will be associated with in the graph.
     * @param graph entity graph the field will be added to.
     */
    protected final void addFilteringScopes(final String field, final Class<?> fieldClass, final Set<String> filteringScopes,
                                            final EntityGraph graph) {
        if (!filteringScopes.isEmpty()) {
            if (FilteringHelper.filterableEntityClass(fieldClass)) {
                graph.addSubgraph(field, fieldClass, filteringScopes);
            } else {
                graph.addField(field, filteringScopes);
            }
        }
    }

    /**
     * Add entity-filtering scopes into given graph. This method should be called only in class-level context.
     *
     * @param filteringScopes entity-filtering scopes to be added to graph.
     * @param graph entity graph to be enhanced by new scopes.
     */
    protected final void addGlobalScopes(final Set<String> filteringScopes, final EntityGraph graph) {
        graph.addFilteringScopes(filteringScopes);
    }
}
