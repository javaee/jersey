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
package org.glassfish.jersey.examples.beanvalidation.webapp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;

import javax.validation.ParameterNameProvider;
import javax.validation.Validation;

import org.glassfish.jersey.examples.beanvalidation.webapp.resource.ContactCardResource;
import org.glassfish.jersey.moxy.json.MoxyJsonConfiguration;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.validation.ValidationConfiguration;
import org.glassfish.jersey.server.validation.ValidationFeature;
import org.glassfish.jersey.server.validation.internal.InjectingConstraintValidatorFactory;

/**
 * ContactCard application configuration.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class MyApplication extends ResourceConfig {

    public MyApplication() {
        // Resources.
        packages(ContactCardResource.class.getPackage().getName());

        // Validation.
        register(ValidationFeature.class);
        register(ValidationConfigurationContextResolver.class);

        // Providers - JSON.
        register(MoxyJsonFeature.class);
        register(JsonConfiguration.class);
    }

    /**
     * Custom configuration of validation. This configuration defines custom:
     * <ul>
     *     <li>ConstraintValidationFactory - so that validators are able to inject Jersey providers/resources.</li>
     *     <li>ParameterNameProvider - if method input parameters are invalid, this class returns actual parameter names
     *     instead of the default ones ({@code arg0, arg1, ..})</li>
     * </ul>
     *
     * TODO Add a test when HV-658 is fixed.
     */
    public static class ValidationConfigurationContextResolver implements ContextResolver<ValidationConfiguration> {

        @Context
        private ResourceContext resourceContext;

        @Override
        public ValidationConfiguration getContext(final Class<?> type) {
            final ValidationConfiguration config = new ValidationConfiguration();
            config.setConstraintValidatorFactory(resourceContext.getResource(InjectingConstraintValidatorFactory.class));
            config.setParameterNameProvider(new CustomParameterNameProvider());
            return config;
        }

        private class CustomParameterNameProvider implements ParameterNameProvider {

            private final ParameterNameProvider nameProvider;

            public CustomParameterNameProvider() {
                nameProvider = Validation.byDefaultProvider().configure().getDefaultParameterNameProvider();
            }

            @Override
            public String[] getParameterNames(final Constructor<?> constructor) {
                return nameProvider.getParameterNames(constructor);
            }

            @Override
            public String[] getParameterNames(final Method method) {
                if ("addContact".equals(method.getName())) {
                    return new String[] {"contact"};
                }
                return nameProvider.getParameterNames(method);
            }
        }
    }

    /**
     * Configuration for {@link org.eclipse.persistence.jaxb.rs.MOXyJsonProvider} - outputs formatted JSON.
     */
    public static class JsonConfiguration implements ContextResolver<MoxyJsonConfiguration> {

        @Override
        public MoxyJsonConfiguration getContext(final Class<?> type) {
            final MoxyJsonConfiguration config = new MoxyJsonConfiguration();
            config.setFormattedOutput(true);
            return config;
        }
    }
}
