/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.validation.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Configuration;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;

import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.glassfish.jersey.server.spi.ValidationInterceptor;
import org.glassfish.jersey.server.validation.ValidationConfig;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Bean Validation provider injection binder.
 *
 * @author Michal Gajdos
 */
public final class ValidationBinder extends AbstractBinder {

    private static final Logger LOGGER = Logger.getLogger(ValidationBinder.class.getName());

    @Override
    protected void configure() {
        bindFactory(DefaultConfigurationProvider.class, Singleton.class).to(Configuration.class).in(Singleton.class);

        bindFactory(DefaultValidatorFactoryProvider.class, Singleton.class).to(ValidatorFactory.class).in(Singleton.class);
        bindFactory(DefaultValidatorProvider.class, Singleton.class).to(Validator.class).in(Singleton.class);

        bindFactory(ConfiguredValidatorProvider.class, Singleton.class).to(ConfiguredValidator.class).in(PerLookup.class);

        // Custom Exception Mapper and Writer - registering in binder to make possible for users register their own providers.
        bind(ValidationExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class);
        bind(ValidationErrorMessageBodyWriter.class).to(MessageBodyWriter.class).in(Singleton.class);
    }

    /**
     * Factory providing default {@link javax.validation.Configuration} instance.
     */
    private static class DefaultConfigurationProvider implements Factory<Configuration> {

        private final boolean inOsgi;

        public DefaultConfigurationProvider() {
            this.inOsgi = ReflectionHelper.getOsgiRegistryInstance() != null;
        }

        @Override
        public Configuration provide() {
            try {
                if (!inOsgi) {
                    return Validation.byDefaultProvider().configure();
                } else {
                    return Validation
                            .byDefaultProvider()
                            .providerResolver(new ValidationProviderResolver() {
                                @Override
                                public List<ValidationProvider<?>> getValidationProviders() {
                                    final List<ValidationProvider<?>> validationProviders = new ArrayList<>();

                                    for (final ValidationProvider validationProvider : ServiceFinder
                                            .find(ValidationProvider.class)) {
                                        validationProviders.add(validationProvider);
                                    }

                                    return validationProviders;
                                }
                            })
                            .configure();
                }
            } catch (final ValidationException e) {
                // log and re-trow
                LOGGER.log(Level.FINE, LocalizationMessages.VALIDATION_EXCEPTION_PROVIDER(), e);
                throw e;
            }
        }

        @Override
        public void dispose(final Configuration instance) {
            // NOOP
        }
    }

    /**
     * Factory providing default (un-configured) {@link ValidatorFactory} instance.
     */
    private static class DefaultValidatorFactoryProvider implements Factory<ValidatorFactory> {

        @Inject
        private Configuration config;

        @Override
        public ValidatorFactory provide() {
            return config.buildValidatorFactory();
        }

        @Override
        public void dispose(final ValidatorFactory instance) {
            // NOOP
        }
    }

    /**
     * Factory providing default (un-configured) {@link Validator} instance.
     */
    private static class DefaultValidatorProvider implements Factory<Validator> {

        @Inject
        private ValidatorFactory factory;

        @Override
        public Validator provide() {
            return factory.getValidator();
        }

        @Override
        public void dispose(final Validator instance) {
            // NOOP
        }
    }

    /**
     * Factory providing configured {@link Validator} instance.
     */
    private static class ConfiguredValidatorProvider implements Factory<ConfiguredValidator> {

        @Inject
        private ServiceLocator locator;

        @Inject
        private Configuration validationConfig;
        @Inject
        private ValidatorFactory factory;

        @Context
        private javax.ws.rs.core.Configuration jaxRsConfig;
        @Context
        private Providers providers;
        @Context
        private ResourceContext resourceContext;

        private ConfiguredValidator defaultValidator;

        private final WeakHashMap<ContextResolver<ValidationConfig>, ConfiguredValidator> validatorCache =
                new WeakHashMap<>();

        @Override
        public ConfiguredValidator provide() {

            // Custom Configuration.
            final ContextResolver<ValidationConfig> contextResolver =
                    providers.getContextResolver(ValidationConfig.class, MediaType.WILDCARD_TYPE);

            if (contextResolver == null) {
                return getDefaultValidator();
            } else {
                if (!validatorCache.containsKey(contextResolver)) {
                    final ValidateOnExecutionHandler validateOnExecutionHandler =
                            new ValidateOnExecutionHandler(validationConfig, !isValidateOnExecutableOverrideCheckDisabled());

                    final ValidatorContext context = getDefaultValidatorContext(validateOnExecutionHandler);
                    final ValidationConfig config = contextResolver.getContext(ValidationConfig.class);

                    if (config != null) {
                        // MessageInterpolator
                        if (config.getMessageInterpolator() != null) {
                            context.messageInterpolator(config.getMessageInterpolator());
                        }

                        // TraversableResolver
                        if (config.getTraversableResolver() != null) {
                            context.traversableResolver(
                                    getTraversableResolver(config.getTraversableResolver(), validateOnExecutionHandler));
                        }

                        // ConstraintValidatorFactory
                        if (config.getConstraintValidatorFactory() != null) {
                            context.constraintValidatorFactory(config.getConstraintValidatorFactory());
                        }

                        // ParameterNameProvider
                        if (config.getParameterNameProvider() != null) {
                            context.parameterNameProvider(config.getParameterNameProvider());
                        }
                    }

                    validatorCache.put(contextResolver,
                            new DefaultConfiguredValidator(context.getValidator(), this.validationConfig,
                                    validateOnExecutionHandler, getValidationInterceptors()));
                }

                return validatorCache.get(contextResolver);
            }
        }

        private Iterable<ValidationInterceptor> getValidationInterceptors() {
            final Iterable<RankedProvider<ValidationInterceptor>> validationInterceptorIterable =
                    org.glassfish.jersey.internal.inject.Providers.getAllRankedProviders(locator, ValidationInterceptor.class);
            return org.glassfish.jersey.internal.inject.Providers.sortRankedProviders(
                    new RankedComparator<ValidationInterceptor>(), validationInterceptorIterable);
        }

        /**
         * Return default validator.
         *
         * @return default validator.
         */
        private ConfiguredValidator getDefaultValidator() {
            if (defaultValidator == null) {
                final ValidateOnExecutionHandler validateOnExecutionHandler =
                        new ValidateOnExecutionHandler(validationConfig, !isValidateOnExecutableOverrideCheckDisabled());
                final Validator validator = getDefaultValidatorContext(validateOnExecutionHandler).getValidator();

                defaultValidator = new DefaultConfiguredValidator(validator, validationConfig,
                        validateOnExecutionHandler, getValidationInterceptors());
            }
            return defaultValidator;
        }

        /**
         * Return default {@link ValidatorContext validator context} able to inject JAX-RS resources/providers.
         *
         * @param handler handler to create traversable resolver for.
         * @return default validator context.
         */
        private ValidatorContext getDefaultValidatorContext(final ValidateOnExecutionHandler handler) {
            final ValidatorContext context = factory.usingContext();

            // Default Configuration.
            context.constraintValidatorFactory(resourceContext.getResource(InjectingConstraintValidatorFactory.class));

            // Traversable Resolver.
            context.traversableResolver(getTraversableResolver(factory.getTraversableResolver(), handler));

            return context;
        }

        /**
         * Create traversable resolver able to process {@link javax.validation.executable.ValidateOnExecution} annotation on
         * beans.
         *
         * @param delegate resolver to be wrapped into the custom traversable resolver.
         * @param handler  handler to create traversable resolver for.
         * @return custom traversable resolver.
         */
        private ValidateOnExecutionTraversableResolver getTraversableResolver(TraversableResolver delegate,
                                                                              final ValidateOnExecutionHandler handler) {
            if (delegate == null) {
                delegate = validationConfig.getDefaultTraversableResolver();
            }

            final boolean validationEnabled = validationConfig.getBootstrapConfiguration().isExecutableValidationEnabled();
            final ValidateOnExecutionTraversableResolver traversableResolver = new
                    ValidateOnExecutionTraversableResolver(delegate, handler, validationEnabled);

            return resourceContext.initResource(traversableResolver);
        }

        private boolean isValidateOnExecutableOverrideCheckDisabled() {
            return PropertiesHelper.isProperty(
                    jaxRsConfig.getProperty(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK));
        }

        @Override
        public void dispose(final ConfiguredValidator instance) {
            // NOOP
        }
    }
}
