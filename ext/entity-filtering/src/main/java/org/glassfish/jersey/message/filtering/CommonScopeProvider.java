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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.message.filtering.internal.LocalizationMessages;
import org.glassfish.jersey.message.filtering.spi.FilteringHelper;
import org.glassfish.jersey.message.filtering.spi.ScopeProvider;
import org.glassfish.jersey.message.filtering.spi.ScopeResolver;
import org.glassfish.jersey.model.internal.RankedComparator;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Default implementation of {@link ScopeProvider scope provider}. This class can be used on client to retrieve
 * entity-filtering scopes from given entity annotations or injected {@link Configuration configuration}. Class can also serve
 * as a base class for server-side implementations.
 *
 * @author Michal Gajdos
 * @see ServerScopeProvider
 */
@Singleton
class CommonScopeProvider implements ScopeProvider {

    private static final Logger LOGGER = Logger.getLogger(CommonScopeProvider.class.getName());

    private final List<ScopeResolver> resolvers;
    private final Configuration config;

    /**
     * Create new common scope provider with injected {@link Configuration configuration} and
     * {@link ServiceLocator HK2 service locator}.
     */
    @Inject
    public CommonScopeProvider(final Configuration config, final ServiceLocator serviceLocator) {
        this.config = config;

        this.resolvers = Lists.newArrayList(Providers.getAllProviders(
                serviceLocator, ScopeResolver.class, new RankedComparator<ScopeResolver>()));
    }

    @Override
    public Set<String> getFilteringScopes(final Annotation[] entityAnnotations, final boolean defaultIfNotFound) {
        Set<String> filteringScopes = Sets.newHashSet();

        // Entity Annotations.
        filteringScopes.addAll(getFilteringScopes(entityAnnotations));

        if (filteringScopes.isEmpty()) {
            // Configuration.
            filteringScopes.addAll(getFilteringScopes(config));
        }

        // Use default scope if not in other scope.
        return returnFilteringScopes(filteringScopes, defaultIfNotFound);
    }

    /**
     * Return the default entity-filtering scope if the given set of scopes is empty and the processing should fallback to the
     * default.
     *
     * @param filteringScopes       entity-filtering scopes to be examined.
     * @param returnDefaultFallback {@code true} if the default entity-filtering scope should be returned if the given scopes
     *                              are empty, {@code false} otherwise.
     * @return entity-filtering scopes.
     */
    protected Set<String> returnFilteringScopes(final Set<String> filteringScopes, final boolean returnDefaultFallback) {
        return returnDefaultFallback && filteringScopes.isEmpty() ? FilteringHelper.getDefaultFilteringScope() : filteringScopes;
    }

    /**
     * Get entity-filtering scopes from all available {@link ScopeResolver scope resolvers} for given annotations.
     *
     * @param annotations annotations to retrieve entity-filtering scopes from.
     * @return entity-filtering scopes or an empty set if none scope can be resolved.
     */
    protected Set<String> getFilteringScopes(final Annotation[] annotations) {
        Set<String> filteringScopes = Sets.newHashSet();
        for (final ScopeResolver provider : resolvers) {
            mergeFilteringScopes(filteringScopes, provider.resolve(annotations));
        }
        return filteringScopes;
    }

    /**
     * Get entity-filtering scopes from {@link Configuration}.
     *
     * @param config configuration the entity-filtering scopes are obtained from.
     * @return entity-filtering scopes or an empty set if none scope can be resolved.
     */
    private Set<String> getFilteringScopes(final Configuration config) {
        final Object property = config.getProperty(EntityFilteringFeature.ENTITY_FILTERING_SCOPE);

        Set<String> filteringScopes = Collections.emptySet();
        if (property != null) {
            if (property instanceof Annotation) {
                filteringScopes = getFilteringScopes(new Annotation[] {(Annotation) property});
            } else if (property instanceof Annotation[]) {
                filteringScopes = getFilteringScopes((Annotation[]) property);
            } else {
                LOGGER.log(Level.CONFIG, LocalizationMessages.ENTITY_FILTERING_SCOPE_NOT_ANNOTATIONS(property));
            }
        }
        return filteringScopes;
    }

    /**
     * Merge two sets of entity-filtering scopes.
     *
     * @param filteringScopes existing entity-filtering scopes.
     * @param resolvedScopes entity-filtering scopes to be added to the existing ones.
     */
    protected void mergeFilteringScopes(final Set<String> filteringScopes, final Set<String> resolvedScopes) {
        if (!filteringScopes.isEmpty() && !resolvedScopes.isEmpty()) {
            LOGGER.log(Level.FINE, LocalizationMessages.MERGING_FILTERING_SCOPES());
        }

        filteringScopes.addAll(resolvedScopes);
    }
}
