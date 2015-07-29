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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.glassfish.jersey.message.filtering.spi.FilteringHelper;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Utility methods for Entity Data Filtering.
 *
 * @author Michal Gajdos
 */
final class EntityFilteringHelper {

    /**
     * Get entity-filtering scopes from given annotations. Scopes are only derived from entity-filtering annotations.
     *
     * @param annotations list of arbitrary annotations.
     * @return a set of entity-filtering scopes.
     */
    public static Set<String> getFilteringScopes(final Annotation[] annotations) {
        return getFilteringScopes(annotations, true);
    }

    /**
     * Get entity-filtering scopes from given annotations. Scopes are only derived from entity-filtering annotations.
     *
     * @param annotations list of arbitrary annotations.
     * @param filter {@code true} whether the given annotation should be reduced to only entity-filtering annotations,
     * {@code false} otherwise.
     * @return a set of entity-filtering scopes.
     */
    public static Set<String> getFilteringScopes(Annotation[] annotations, final boolean filter) {
        if (annotations.length == 0) {
            return Collections.emptySet();
        }

        final Set<String> contexts = Sets.newHashSetWithExpectedSize(annotations.length);

        annotations = filter ? getFilteringAnnotations(annotations) : annotations;
        for (final Annotation annotation : annotations) {
            contexts.add(annotation.annotationType().getName());
        }

        return contexts;
    }

    /**
     * Filter given annotations and return only entity-filtering ones.
     *
     * @param annotations list of arbitrary annotations.
     * @return entity-filtering annotations or an empty array.
     */
    public static Annotation[] getFilteringAnnotations(final Annotation[] annotations) {
        if (annotations == null || annotations.length == 0) {
            return FilteringHelper.EMPTY_ANNOTATIONS;
        }

        final List<Annotation> filteringAnnotations = Lists.newArrayListWithExpectedSize(annotations.length);

        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();

            for (final Annotation metaAnnotation : annotationType.getDeclaredAnnotations()) {
                if (metaAnnotation instanceof EntityFiltering) {
                    filteringAnnotations.add(annotation);
                }
            }
        }

        return filteringAnnotations.toArray(new Annotation[filteringAnnotations.size()]);
    }

    public static <T extends Annotation> T getAnnotation(final Annotation[] annotations, final Class<T> clazz) {
        for (final Annotation annotation : annotations) {
            if (annotation.annotationType().getClass().isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (T) annotation;
            }
        }
        return null;
    }

    /**
     * Prevent instantiation.
     */
    private EntityFilteringHelper() {
    }
}
