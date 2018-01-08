/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.servlet;

import org.glassfish.jersey.internal.util.PropertiesClass;

/**
 * Jersey servlet container configuration properties.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@PropertiesClass
public final class ServletProperties {

    /**
     * If set, indicates the URL pattern of the Jersey servlet filter context path.
     * <p>
     * If the URL pattern of a filter is set to a base path and a wildcard,
     * such as "/base/*", then this property can be used to declare a filter
     * context path that behaves in the same manner as the Servlet context
     * path for determining the base URI of the application. (Note that with
     * the Servlet 2.x API it is not possible to determine the URL pattern
     * without parsing the {@code web.xml}, hence why this property is necessary.)
     * <p>
     * The property is only applicable when {@link ServletContainer Jersey servlet
     * container} is configured to run as a {@link javax.servlet.Filter}, otherwise this property
     * will be ignored.
     * <p>
     * The value of the property may consist of one or more path segments separate by
     * {@code '/'}.
     * <p></p>
     * A default value is not set.
     * <p></p>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String FILTER_CONTEXT_PATH = "jersey.config.servlet.filter.contextPath";

    /**
     * If set to {@code true} and a 404 response with no entity body is returned
     * from either the runtime or the application then the runtime forwards the
     * request to the next filter in the filter chain. This enables another filter
     * or the underlying servlet engine to process the request. Before the request
     * is forwarded the response status is set to 200.
     * <p>
     * This property is an alternative to setting a {@link #FILTER_STATIC_CONTENT_REGEX
     * static content regular expression} and requires less configuration. However,
     * application code, such as methods corresponding to sub-resource locators,
     * may be invoked when this feature is enabled.
     * <p></p>
     * The property is only applicable when {@link ServletContainer Jersey servlet
     * container} is configured to run as a {@link javax.servlet.Filter}, otherwise
     * this property will be ignored.
     * <p></p>
     * Application code, such as methods corresponding to sub-resource locators
     * may be invoked when this feature is enabled.
     * <p>
     * The default value is {@code false}.
     * <p></p>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String FILTER_FORWARD_ON_404 = "jersey.config.servlet.filter.forwardOn404";

    /**
     * If set the regular expression is used to match an incoming servlet path URI
     * to some web page content such as static resources or JSPs to be handled
     * by the underlying servlet engine.
     * <p></p>
     * The property is only applicable when {@link ServletContainer Jersey servlet
     * container} is configured to run as a {@link javax.servlet.Filter}, otherwise
     * this property will be ignored. If a servlet path matches this regular
     * expression then the filter forwards the request to the next filter in the
     * filter chain so that the underlying servlet engine can process the request
     * otherwise Jersey will process the request. For example if you set the value
     * to {@code /(image|css)/.*} then you can serve up images and CSS files
     * for your Implicit or Explicit Views while still processing your JAX-RS
     * resources.
     * <p></p>
     * The type of this property must be a String and the value must be a valid
     * regular expression.
     * <p></p>
     * A default value is not set.
     * <p></p>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String FILTER_STATIC_CONTENT_REGEX = "jersey.config.servlet.filter.staticContentRegex";

    /**
     * Application configuration initialization property whose value is a fully
     * qualified class name of a class that implements {@link javax.ws.rs.core.Application}.
     * <p></p>
     * A default value is not set.
     * <p></p>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    // TODO implement generic support
    public static final String JAXRS_APPLICATION_CLASS = "javax.ws.rs.Application";

    /**
     * Indicates that Jersey should scan the whole web app for application-specific resources and
     * providers. If the property is present and the value is not {@code false}, the whole web app
     * will be scanned for JAX-RS root resources (annotated with {@link javax.ws.rs.Path @Path})
     * and providers (annotated with {@link javax.ws.rs.ext.Provider @Provider}).
     * <p></p>
     * The property value MUST be an instance of {@link String}. The allowed values are {@code true}
     * and {@code false}.
     * <p></p>
     * A default value is not set.
     * <p></p>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String PROVIDER_WEB_APP = "jersey.config.servlet.provider.webapp";

    /**
     * If {@code true} then query parameters will not be treated as form parameters (e.g. injectable using
     * {@link javax.ws.rs.FormParam}) in case a Form request is processed by server.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.16
     */
    public static final String QUERY_PARAMS_AS_FORM_PARAMS_DISABLED = "jersey.config.servlet.form.queryParams.disabled";

    /**
     * Identifies the object that will be used as a parent {@code HK2 ServiceLocator} in the Jersey
     * {@link WebComponent}.
     * <p></p>
     * This property gives a possibility to use HK2 services that are registered and/or created
     * outside of the Jersey server context.
     * <p></p>
     * By default this property is not set.
     * <p></p>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String SERVICE_LOCATOR = "jersey.config.servlet.context.serviceLocator";

    private ServletProperties() {
        // prevents instantiation
    }
}
