/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.validation;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;

import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.Path;
import javax.validation.TraversableResolver;
import javax.validation.Validation;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.validation.ValidationConfiguration;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class CustomConfigValidationTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        final ResourceConfig resourceConfig = new ResourceConfig(CustomConfigResource.class);

        resourceConfig.register(MoxyXmlFeature.class);
        resourceConfig.register(ValidationConfigurationContextResolver.class);

        resourceConfig.property(ServerProperties.FEATURE_OUTPUT_VALIDATION_ERROR_ENTITY, true);

        return resourceConfig;
    }

    @Override
    protected void configureClient(final ClientConfig clientConfig) {
        super.configureClient(clientConfig);
        clientConfig.register(MoxyXmlFeature.class);
    }

    @Test
    public void testPositive() throws Exception {
        final Response response = target("customconfigvalidation").
                path("ok").
                request().
                post(Entity.entity(new CustomBean(), MediaType.APPLICATION_XML_TYPE));

        assertEquals(200, response.getStatus());
        assertEquals("ok", response.readEntity(CustomBean.class).getPath());
    }

    @Test
    public void testParameterName() throws Exception {
        final Response response = target("customconfigvalidation").
                path("ok").
                request().
                post(Entity.entity(null, MediaType.APPLICATION_XML_TYPE));

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertFalse(message.contains("arg1"));
        assertTrue(message.contains("beanParameter"));
    }

    @Test
    public void testMessageInterpolator() throws Exception {
        final Response response = target("customconfigvalidation").
                path("ok").
                request().
                post(Entity.entity(null, MediaType.APPLICATION_XML_TYPE));

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertFalse(message.contains("may not be null"));
        assertTrue(message.contains("message"));
    }

    @Test
    public void testTraversableResolver() throws Exception {
        final Response response = target("customconfigvalidation/").
                request().
                post(Entity.entity(new CustomBean(), MediaType.APPLICATION_XML_TYPE));

        assertEquals(200, response.getStatus());
        // return value passed validation because of "corrupted" traversableresolver
        assertEquals(null, response.readEntity(CustomBean.class).getPath());
    }

    public static class ValidationConfigurationContextResolver implements ContextResolver<ValidationConfiguration> {

        private final ValidationConfiguration config;

        public ValidationConfigurationContextResolver() {
            config = new ValidationConfiguration();

            // ConstraintValidatorFactory is set by default.
            config.setMessageInterpolator(new CustomMessageInterpolator());
            config.setParameterNameProvider(new CustomParameterNameProvider());
            config.setTraversableResolver(new CustomTraversableResolver());
        }

        @Override
        public ValidationConfiguration getContext(final Class<?> type) {
            return ValidationConfiguration.class.isAssignableFrom(type) ? config : null;
        }
    }

    private static class CustomMessageInterpolator implements MessageInterpolator {

        @Override
        public String interpolate(final String messageTemplate, final Context context) {
            return "message";
        }

        @Override
        public String interpolate(final String messageTemplate, final Context context, final Locale locale) {
            return "localized message";
        }
    }

    private static class CustomParameterNameProvider implements ParameterNameProvider {

        private final ParameterNameProvider nameProvider;

        public CustomParameterNameProvider() {
            nameProvider = Validation.byDefaultProvider().configure().getDefaultParameterNameProvider();
        }

        @Override
        public List<String> getParameterNames(final Constructor<?> constructor) {
            return nameProvider.getParameterNames(constructor);
        }

        @Override
        public List<String> getParameterNames(final Method method) {
            try {
                final Method post = CustomConfigResource.class.getMethod("post", String.class, CustomBean.class);

                if (method.equals(post)) {
                    return Arrays.asList("path", "beanParameter");
                }
            } catch (NoSuchMethodException e) {
                // Do nothing.
            }
            return nameProvider.getParameterNames(method);
        }
    }

    private static class CustomTraversableResolver implements TraversableResolver {

        @Override
        public boolean isReachable(final Object traversableObject,
                                   final Path.Node traversableProperty,
                                   final Class<?> rootBeanType,
                                   final Path pathToTraversableObject,
                                   final ElementType elementType) {
            return false;
        }

        @Override
        public boolean isCascadable(final Object traversableObject,
                                    final Path.Node traversableProperty,
                                    final Class<?> rootBeanType,
                                    final Path pathToTraversableObject,
                                    final ElementType elementType) {
            return false;
        }
    }
}
