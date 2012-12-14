/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Validation;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;

import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.glassfish.jersey.server.validation.ValidationConfiguration;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PerLookup;

/**
 * Bean Validation provider injection binder.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class ValidationBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bindFactory(DefaultValidatorFactoryProvider.class, Singleton.class).to(ValidatorFactory.class).in(PerLookup.class);
        bindFactory(DefaultValidatorProvider.class, Singleton.class).to(Validator.class).in(PerLookup.class);

        bindFactory(ConfiguredValidatorProvider.class, Singleton.class).to(ConfiguredValidator.class).in(PerLookup.class);
    }

    /**
     * Factory providing default (un-configured) {@link ValidatorFactory} instance.
     */
    private static class DefaultValidatorFactoryProvider implements Factory<ValidatorFactory> {

        private final boolean inOsgi;

        public DefaultValidatorFactoryProvider() {
            this.inOsgi = ReflectionHelper.getOsgiRegistryInstance() != null;
        }

        @Override
        public ValidatorFactory provide() {
            if (!inOsgi) {
                return Validation.buildDefaultValidatorFactory();
            } else {
                return Validation.
                        byDefaultProvider().
                        providerResolver(new ValidationProviderResolver() {
                            @Override
                            public List<ValidationProvider<?>> getValidationProviders() {
                                final List<ValidationProvider<?>> validationProviders = new ArrayList<ValidationProvider<?>>();

                                for (final ValidationProvider validationProvider : ServiceFinder.find(ValidationProvider.class)) {
                                    validationProviders.add(validationProvider);
                                }

                                return validationProviders;
                            }
                        }).
                        configure().
                        buildValidatorFactory();
            }
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
        private ValidatorFactory factory;
        @Context
        private Providers providers;
        @Context
        private ResourceContext resourceContext;

        @Override
        public ConfiguredValidator provide() {
            return getConfiguredValidator(factory);
        }

        @Override
        public void dispose(final ConfiguredValidator instance) {
            // NOOP
        }

        private ConfiguredValidator getConfiguredValidator(final ValidatorFactory factory) {
            final ValidatorContext context = factory.usingContext();

            // Default Configuration.
            context.constraintValidatorFactory(resourceContext.getResource(InjectingConstraintValidatorFactory.class));

            // Custom Configuration.
            final ContextResolver<ValidationConfiguration> contextResolver =
                    providers.getContextResolver(ValidationConfiguration.class, MediaType.WILDCARD_TYPE);

            if (contextResolver != null) {
                final ValidationConfiguration config = contextResolver.getContext(ValidationConfiguration.class);

                if (config != null) {
                    // MessageInterpolator
                    if (config.getMessageInterpolator() != null) {
                        context.messageInterpolator(config.getMessageInterpolator());
                    }

                    // TraversableResolver
                    if (config.getTraversableResolver() != null) {
                        context.traversableResolver(config.getTraversableResolver());
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
            }

            return new ConfiguredValidatorImpl(context.getValidator());
        }
    }
}
