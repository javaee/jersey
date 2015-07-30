/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.routing;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.ComponentModelValidator;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.ModelValidationException;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.ResourceModelComponent;
import org.glassfish.jersey.server.model.internal.ModelErrors;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.cache.CacheBuilder;
import jersey.repackaged.com.google.common.cache.CacheLoader;
import jersey.repackaged.com.google.common.cache.LoadingCache;

/**
 * Base for sub-resource locator runtime model builder.
 *
 * @author Michal Gajdos
 */
final class RuntimeLocatorModelBuilder {

    private static final Logger LOGGER = Logger.getLogger(RuntimeLocatorModelBuilder.class.getName());

    private final ServiceLocator locator;
    private final Configuration config;
    private final RuntimeModelBuilder runtimeModelBuilder;
    private final JerseyResourceContext resourceContext;

    private final LoadingCache<LocatorCacheKey, LocatorRouting> cache;

    // Configuration.
    private final boolean disableValidation;
    private final boolean ignoreValidationErrors;
    private final boolean enableJerseyResourceCaching;

    /**
     * Create a new instance of the runtime model builder for sub-resource locators.
     *
     * @param locator             HK2 service locator.
     * @param config              configuration of the application.
     * @param resourceContext     resource context to bind sub-resource locator singleton instances.
     * @param runtimeModelBuilder runtime model builder to build routers for locator models.
     */
    RuntimeLocatorModelBuilder(final ServiceLocator locator,
                               final Configuration config,
                               final JerseyResourceContext resourceContext,
                               final RuntimeModelBuilder runtimeModelBuilder) {

        this.locator = locator;
        this.config = config;
        this.runtimeModelBuilder = runtimeModelBuilder;
        this.resourceContext = resourceContext;

        // Configuration.
        this.disableValidation = ServerProperties.getValue(config.getProperties(),
                ServerProperties.RESOURCE_VALIDATION_DISABLE,
                Boolean.FALSE,
                Boolean.class);
        this.ignoreValidationErrors = ServerProperties.getValue(config.getProperties(),
                ServerProperties.RESOURCE_VALIDATION_IGNORE_ERRORS,
                Boolean.FALSE,
                Boolean.class);
        this.enableJerseyResourceCaching = ServerProperties.getValue(config.getProperties(),
                ServerProperties.SUBRESOURCE_LOCATOR_CACHE_JERSEY_RESOURCE_ENABLED,
                Boolean.FALSE,
                Boolean.class);

        final int size = ServerProperties.getValue(config.getProperties(),
                ServerProperties.SUBRESOURCE_LOCATOR_CACHE_SIZE,
                ServerProperties.SUBRESOURCE_LOCATOR_DEFAULT_CACHE_SIZE,
                Integer.class);
        final int age = ServerProperties.getValue(config.getProperties(),
                ServerProperties.SUBRESOURCE_LOCATOR_CACHE_AGE,
                -1,
                Integer.class);

        // Cache.
        final CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (size > 0) {
            // Size eviction policy.
            cacheBuilder.maximumSize(size);
        } else {
            LOGGER.log(Level.CONFIG, LocalizationMessages.SUBRES_LOC_CACHE_INVALID_SIZE(size,
                    ServerProperties.SUBRESOURCE_LOCATOR_DEFAULT_CACHE_SIZE));

            // Invalid value. Fallback to the default value.
            cacheBuilder.maximumSize(ServerProperties.SUBRESOURCE_LOCATOR_DEFAULT_CACHE_SIZE);
        }
        if (age > 0) {
            // Age eviction policy.
            cacheBuilder.expireAfterAccess(age, TimeUnit.SECONDS);
        }
        cache = cacheBuilder.build(new CacheLoader<LocatorCacheKey, LocatorRouting>() {
            @Override
            public LocatorRouting load(final LocatorCacheKey key) throws Exception {
                return key.clazz != null ? createRouting(key.clazz) : buildRouting(key.resource);
            }
        });
    }

    /**
     * Obtain a sub-resource locator router for given resource method.
     *
     * @param resourceMethod resource method to obtain the router for.
     * @return sub-resource locator router.
     */
    Router getRouter(final ResourceMethod resourceMethod) {
        return new SubResourceLocatorRouter(locator, resourceMethod, resourceContext, this);
    }

    /**
     * Build (or obtain from cache) a resource model and router for given sub-resource locator class.
     *
     * @param locatorClass sub-resource locator class to built model and router for.
     * @return [locator, router] pair with built model and router for sub-resource locator.
     */
    LocatorRouting getRouting(final Class<?> locatorClass) {
        try {
            return cache.get(new LocatorCacheKey(locatorClass));
        } catch (final ExecutionException ee) {
            LOGGER.log(Level.FINE, LocalizationMessages.SUBRES_LOC_CACHE_LOAD_FAILED(locatorClass), ee);
            return createRouting(locatorClass);
        }
    }

    /**
     * Build (or obtain from cache) a resource model and router for given sub-resource locator
     * {@link org.glassfish.jersey.server.model.Resource resource}.
     *
     * @param subresource sub-resource locator resource to built model and router for.
     * @return [locator, router] pair with built model and router for sub-resource locator.
     */
    LocatorRouting getRouting(final Resource subresource) {
        if (enableJerseyResourceCaching) {
            try {
                return cache.get(new LocatorCacheKey(subresource));
            } catch (final ExecutionException ee) {
                LOGGER.log(Level.FINE, LocalizationMessages.SUBRES_LOC_CACHE_LOAD_FAILED(subresource), ee);
                return buildRouting(subresource);
            }
        } else {
            return buildRouting(subresource);
        }
    }

    /**
     * Check if the model builder contains a cached [locator, router] pair for a given sub-resource locator class.
     *
     * @param srlClass sub-resource locator class.
     * @return {@code true} if the [locator, router] pair  for the sub-resource locator class is present in the cache,
     * {@code false} otherwise.
     */
    boolean isCached(final Class<?> srlClass) {
        return cache.getIfPresent(srlClass) != null;
    }

    private LocatorRouting createRouting(final Class<?> locatorClass) {
        Resource.Builder builder = Resource.builder(locatorClass, disableValidation);
        if (builder == null) {
            // resource is empty - do not throw 404, wait if ModelProcessors add any method
            builder = Resource.builder().name(locatorClass.getName());
        }

        return buildRouting(builder.build());
    }

    private LocatorRouting buildRouting(final Resource subResource) {
        final ResourceModel model = new ResourceModel.Builder(true).addResource(subResource).build();
        final ResourceModel enhancedModel = enhance(model);

        if (!disableValidation) {
            validateResource(enhancedModel);
        }

        final Resource enhancedLocator = enhancedModel.getResources().get(0);
        for (final Class<?> handlerClass : enhancedLocator.getHandlerClasses()) {
            resourceContext.bindResource(handlerClass);
        }

        return new LocatorRouting(enhancedModel,
                runtimeModelBuilder.buildModel(enhancedModel.getRuntimeResourceModel(), true));
    }

    private void validateResource(final ResourceModelComponent component) {
        Errors.process(new Runnable() {
            @Override
            public void run() {
                final ComponentModelValidator validator = new ComponentModelValidator(locator);
                validator.validate(component);

                if (Errors.fatalIssuesFound() && !ignoreValidationErrors) {
                    throw new ModelValidationException(LocalizationMessages.ERROR_VALIDATION_SUBRESOURCE(), ModelErrors
                            .getErrorsAsResourceModelIssues());
                }
            }
        });
    }

    private ResourceModel enhance(ResourceModel subResourceModel) {
        final Iterable<RankedProvider<ModelProcessor>> allRankedProviders = Providers
                .getAllRankedProviders(locator, ModelProcessor.class);
        final Iterable<ModelProcessor> modelProcessors = Providers
                .sortRankedProviders(new RankedComparator<ModelProcessor>(), allRankedProviders);

        for (final ModelProcessor modelProcessor : modelProcessors) {
            subResourceModel = modelProcessor.processSubResource(subResourceModel, config);
            validateSubResource(subResourceModel);
        }
        return subResourceModel;
    }

    private void validateSubResource(final ResourceModel subResourceModel) {
        if (subResourceModel.getResources().size() != 1) {
            throw new ProcessingException(LocalizationMessages.ERROR_SUB_RESOURCE_LOCATOR_MORE_RESOURCES(subResourceModel
                    .getResources().size()));
        }
    }

    private static class LocatorCacheKey {

        private final Class<?> clazz;
        private final Resource resource;

        public LocatorCacheKey(final Class<?> clazz) {
            this(clazz, null);
        }

        public LocatorCacheKey(final Resource resource) {
            this(null, resource);
        }

        private LocatorCacheKey(final Class<?> clazz, final Resource resource) {
            this.clazz = clazz;
            this.resource = resource;
        }

        @Override
        @SuppressWarnings("RedundantIfStatement")
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final LocatorCacheKey that = (LocatorCacheKey) o;

            if (clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) {
                return false;
            }
            if (resource != null ? !resource.equals(that.resource) : that.resource != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = clazz != null ? clazz.hashCode() : 0;
            result = 31 * result + (resource != null ? resource.hashCode() : 0);
            return result;
        }
    }
}
