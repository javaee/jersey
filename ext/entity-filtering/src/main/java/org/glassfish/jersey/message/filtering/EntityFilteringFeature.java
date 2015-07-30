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

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * {@link Feature} used to add support for Entity Data Filtering feature for entity-filtering annotations based on
 * {@link EntityFiltering} meta-annotation.
 *
 * @author Michal Gajdos
 */
public final class EntityFilteringFeature implements Feature {

    /**
     * Defines one or more annotations that should be used as entity-filtering scope when reading/writing an entity.
     * <p>
     * The property can be used on client to define the scope as well as on server to override the scope derived from current
     * request processing context (resource methods / resource classes).
     * </p>
     * <p>
     * If the property is set, the specified annotations will be used to create (override) entity-filtering scope.
     * </p>
     * <p>
     * The property value MUST be an instance of {@link java.lang.annotation.Annotation} or {@code Annotation[]} array. To obtain
     * the annotation instances refer to the {@link EntityFiltering} for requirements on creating entity-filtering annotations.
     * </p>
     * <p>
     * A default value is not set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see EntityFiltering
     */
    public static final String ENTITY_FILTERING_SCOPE = "jersey.config.entityFiltering.scope";

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        if (!config.isRegistered(EntityFilteringProcessor.class)) {
            // Binder (FilteringObjectProvider/FilteringGraphTransformer).
            if (!config.isRegistered(EntityFilteringBinder.class)) {
                context.register(new EntityFilteringBinder());
            }

            // Entity Processors.
            context.register(EntityFilteringProcessor.class);
            if (!config.isRegistered(DefaultEntityProcessor.class)) {
                context.register(DefaultEntityProcessor.class);
            }

            // Scope Providers.
            context.register(EntityFilteringScopeResolver.class);

            // Scope Resolver.
            if (RuntimeType.SERVER == config.getRuntimeType()) {
                context.register(ServerScopeProvider.class);
            } else {
                context.register(CommonScopeProvider.class);
            }

            return true;
        }
        return false;
    }

    /**
     * Return {@code true} whether at least one of the entity filtering features is registered in the given config.
     *
     * @param config config to be examined for presence of entity filtering feature.
     * @return {@code true} if entity filtering is enabled for given config, {@code false} otherwise.
     */
    public static boolean enabled(final Configuration config) {
        return config.isRegistered(EntityFilteringFeature.class)
                || config.isRegistered(SecurityEntityFilteringFeature.class)
                || config.isRegistered(SelectableEntityFilteringFeature.class);
    }
}
