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
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.message.filtering.spi.EntityGraph;
import org.glassfish.jersey.message.filtering.spi.EntityGraphProvider;
import org.glassfish.jersey.message.filtering.spi.EntityInspector;
import org.glassfish.jersey.message.filtering.spi.EntityProcessor;
import org.glassfish.jersey.message.filtering.spi.EntityProcessorContext;
import org.glassfish.jersey.message.filtering.spi.FilteringHelper;
import org.glassfish.jersey.model.internal.RankedComparator;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Class responsible for inspecting entity classes. This class invokes all available {@link EntityProcessor entity processors} in
 * different {@link EntityProcessorContext contexts}.
 *
 * @author Michal Gajdos
 */
@Singleton
final class EntityInspectorImpl implements EntityInspector {

    private final List<EntityProcessor> entityProcessors;

    @Inject
    private EntityGraphProvider graphProvider;

    /**
     * Constructor for HK2 expecting {@link ServiceLocator} to be injected.
     *
     * @param locator service locator to be injected.
     */
    @Inject
    public EntityInspectorImpl(final ServiceLocator locator) {
        this.entityProcessors = Lists.newArrayList(Providers.getAllProviders(locator, EntityProcessor.class,
                new RankedComparator<EntityProcessor>()));
    }

    @Override
    public void inspect(final Class<?> entityClass, final boolean forWriter) {
        if (!graphProvider.containsEntityGraph(entityClass, forWriter)) {
            final EntityGraph graph = graphProvider.getOrCreateEntityGraph(entityClass, forWriter);
            final Set<Class<?>> inspect = Sets.newHashSet();

            // Class.
            if (!inspectEntityClass(entityClass, graph, forWriter)) {
                // Properties.
                final Map<String, Method> unmatchedAccessors = inspectEntityProperties(entityClass, graph, inspect, forWriter);

                // Setters/Getters without fields.
                inspectStandaloneAccessors(unmatchedAccessors, graph, forWriter);

                // Inspect new classes.
                for (final Class<?> clazz : inspect) {
                    inspect(clazz, forWriter);
                }
            }
        }
    }

    /**
     * Invoke available {@link EntityProcessor}s on given entity class.
     *
     * @param entityClass entity class to be examined.
     * @param graph entity graph to be modified by examination.
     * @param forWriter flag determining whether the class should be examined for reader or writer.
     * @return {@code true} if the inspecting should be roll-backed, {@code false} otherwise.
     */
    private boolean inspectEntityClass(final Class<?> entityClass, final EntityGraph graph, final boolean forWriter) {
        final EntityProcessorContextImpl context = new EntityProcessorContextImpl(
                forWriter ? EntityProcessorContext.Type.CLASS_WRITER : EntityProcessorContext.Type.CLASS_READER,
                entityClass, graph);

        for (final EntityProcessor processor : entityProcessors) {
            final EntityProcessor.Result result = processor.process(context);

            if (EntityProcessor.Result.ROLLBACK == result) {
                graphProvider.getOrCreateEmptyEntityGraph(entityClass, false);
                return true;
            }
        }
        return false;
    }

    /**
     * Invoke available {@link EntityProcessor}s on fields of given entity class. Method returns a map ({@code fieldName},
     * {@code method}) of unprocessed property accessors (getters/setters) and fills {@code inspect} set with entity classes
     * that should be further processed.
     *
     * @param entityClass entity class to obtain properties to be examined.
     * @param graph entity graph to be modified by examination.
     * @param inspect non-null set of classes to-be-examined.
     * @param forWriter flag determining whether the class should be examined for reader or writer.
     * @return map of unprocessed property accessors.
     */
    private Map<String, Method> inspectEntityProperties(final Class<?> entityClass, final EntityGraph graph,
                                                        final Set<Class<?>> inspect, final boolean forWriter) {
        final Field[] fields = AccessController.doPrivileged(ReflectionHelper.getAllFieldsPA(entityClass));
        final Map<String, Method> methods = FilteringHelper.getPropertyMethods(entityClass, forWriter);

        for (final Field field : fields) {
            // Ignore static fields.
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            final String name = field.getName();
            final Class<?> clazz = FilteringHelper.getEntityClass(field.getGenericType());
            final Method method = methods.remove(name);

            final EntityProcessorContextImpl context = new EntityProcessorContextImpl(
                    forWriter ? EntityProcessorContext.Type.PROPERTY_WRITER : EntityProcessorContext.Type.PROPERTY_READER,
                    field, method, graph);

            boolean rollback = false;
            for (final EntityProcessor processor : entityProcessors) {
                final EntityProcessor.Result result = processor.process(context);

                if (EntityProcessor.Result.ROLLBACK == result) {
                    rollback = true;
                    graph.remove(name);
                    break;
                }
            }

            if (!rollback && FilteringHelper.filterableEntityClass(clazz)) {
                inspect.add(clazz);
            }
        }

        return methods;
    }

    /**
     * Invoke available {@link EntityProcessor}s on accessors (getter/setter) that has no match in classes' fields.
     *
     * @param unprocessedAccessors map of unprocessed accessors.
     * @param graph entity graph to be modified by examination.
     * @param forWriter flag determining whether the class should be examined for reader or writer.
     */
    private void inspectStandaloneAccessors(final Map<String, Method> unprocessedAccessors, final EntityGraph graph,
                                            final boolean forWriter) {
        for (final Map.Entry<String, Method> entry : unprocessedAccessors.entrySet()) {
            final EntityProcessorContextImpl context = new EntityProcessorContextImpl(
                    forWriter ? EntityProcessorContext.Type.METHOD_WRITER : EntityProcessorContext.Type.METHOD_READER,
                    entry.getValue(), graph);

            for (final EntityProcessor processor : entityProcessors) {
                final EntityProcessor.Result result = processor.process(context);

                if (EntityProcessor.Result.ROLLBACK == result) {
                    graph.remove(entry.getKey());
                    break;
                }
            }
        }
    }
}
