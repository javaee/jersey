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
package org.glassfish.jersey.examples.flight;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ContextResolver;

import org.glassfish.jersey.examples.flight.internal.DataStore;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.mvc.beanvalidation.MvcBeanValidationFeature;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;

/**
 * Flight management demo JAX-RS application.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@ApplicationPath("api")
public class FlightDemoApp extends ResourceConfig {
    public FlightDemoApp() {
        // Generate data
        DataStore.generateData();

        // Select packages to scan for resources and providers
        packages("org.glassfish.jersey.examples.flight.resources",
                "org.glassfish.jersey.examples.flight.providers");

        // configure MOXy providers
        // XML
        register(MoxyXmlFeature.class);
        property(MessageProperties.XML_FORMAT_OUTPUT, true);
        // JSON
        register(createMoxyJsonResolver());

        // Enable on-demand tracing
        property(ServerProperties.TRACING, "ON_DEMAND");

        // Enable monitoring MBeans
        property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);

        // Support for HTTP method override via query parameter
        register(new HttpMethodOverrideFilter(HttpMethodOverrideFilter.Source.QUERY));

        // Propagate validation errors to client
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        // Enable support for role-based authorization
        register(RolesAllowedDynamicFeature.class);

        // Enable JSON entity filtering
        register(EntityFilteringFeature.class);

        // Enable MVC FreeMarker templating engine
        register(FreemarkerMvcFeature.class);
        property(FreemarkerMvcFeature.TEMPLATE_BASE_PATH, "freemarker");
        register(MvcBeanValidationFeature.class);

        // Enable SSE support
        register(SseFeature.class);
    }

    /**
     * Create {@link ContextResolver} for {@link MoxyJsonConfig} for this application.
     *
     * @return {@code MoxyJsonConfig} context resolver.
     */
    public static ContextResolver<MoxyJsonConfig> createMoxyJsonResolver() {
        final MoxyJsonConfig moxyJsonConfig = new MoxyJsonConfig()
                .setFormattedOutput(true)
                .setNamespaceSeparator(':');
        return moxyJsonConfig.resolver();
    }
}
