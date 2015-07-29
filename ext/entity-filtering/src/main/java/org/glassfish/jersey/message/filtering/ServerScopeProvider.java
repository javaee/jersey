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
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.util.collection.DataStructures;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.ResourceMethod;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Server-side implementation of {@link org.glassfish.jersey.message.filtering.spi.ScopeProvider scope provider}. In addition to
 * {@link CommonScopeProvider base implementation} this class provides entity-filtering scopes by examining matched resource
 * method and sub-resource locators. This examination comes into play only in case if entity-filtering scopes cannot be found in
 * entity annotations or application configuration.
 *
 * @author Michal Gajdos
 */
@Singleton
@Priority(Priorities.ENTITY_CODER + 200)
@ConstrainedTo(RuntimeType.SERVER)
class ServerScopeProvider extends CommonScopeProvider {

    @Inject
    private Provider<ExtendedUriInfo> uriInfoProvider;

    private final ConcurrentMap<String, Set<String>> uriToContexts;

    /**
     * Create new server scope provider with injected {@link Configuration configuration} and
     * {@link ServiceLocator HK2 service locator}.
     */
    @Inject
    public ServerScopeProvider(final Configuration config, final ServiceLocator serviceLocator) {
        super(config, serviceLocator);
        this.uriToContexts = DataStructures.createConcurrentMap();
    }

    @Override
    public Set<String> getFilteringScopes(final Annotation[] entityAnnotations, final boolean defaultIfNotFound) {
        Set<String> filteringScope = super.getFilteringScopes(entityAnnotations, false);

        if (filteringScope.isEmpty()) {
            final ExtendedUriInfo uriInfo = uriInfoProvider.get();
            final String path = uriInfo.getPath();

            if (uriToContexts.containsKey(path)) {
                return uriToContexts.get(path);
            }

            for (final ResourceMethod method : ServerScopeProvider.getMatchedMethods(uriInfo)) {
                final Invocable invocable = method.getInvocable();

                mergeFilteringScopes(filteringScope,
                        getFilteringScopes(invocable.getHandlingMethod(), invocable.getHandler().getHandlerClass()));

                if (!filteringScope.isEmpty()) {
                    uriToContexts.putIfAbsent(path, filteringScope);
                    return filteringScope;
                }
            }
        }

        // Use default scope if not in other scope.
        return returnFilteringScopes(filteringScope, defaultIfNotFound);
    }

    /**
     * Get entity-filtering scopes from examining annotations present on resource method and resource class.
     *
     * @param resourceMethod matched resource method to be examined.
     * @param resourceClass matched resource class to be examined.
     * @return entity-filtering scopes or an empty set if the scopes cannot be obtained.
     */
    protected Set<String> getFilteringScopes(final Method resourceMethod, final Class<?> resourceClass) {
        // Method annotations first.
        Set<String> scope = getFilteringScopes(resourceMethod.getAnnotations());

        // Class annotations second.
        if (scope.isEmpty()) {
            scope = getFilteringScopes(resourceClass.getAnnotations());
        }

        return scope;
    }

    private static List<ResourceMethod> getMatchedMethods(final ExtendedUriInfo uriInfo) {
        final List<ResourceMethod> matchedResourceLocators = uriInfo.getMatchedResourceLocators();
        final List<ResourceMethod> methods = Lists.newArrayListWithCapacity(1 + matchedResourceLocators.size());

        methods.add(uriInfo.getMatchedResourceMethod());
        methods.addAll(matchedResourceLocators);

        return methods;
    }
}
