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
package org.glassfish.jersey.servlet;

/**
 * Jersey servlet container configuration properties.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ServletProperties {

    /**
     * If set the regular expression is used to match an incoming servlet path URI
     * to some web page content such as static resources or JSPs to be handled
     * by the underlying servlet engine.
     * <p />
     * The property is only applicable when {@link ServletContainer Jersey servlet
     * container} is configured to run as a {@link javax.servlet.Filter}, otherwise
     * this property will be ignored. If a servlet path matches this regular
     * expression then the filter forwards the request to the next filter in the
     * filter chain so that the underlying servlet engine can process the request
     * otherwise Jersey will process the request. For example if you set the value
     * to {@code /(image|css)/.*} then you can serve up images and CSS files
     * for your Implicit or Explicit Views while still processing your JAX-RS
     * resources.
     * <p />
     * The type of this property must be a String and the value must be a valid
     * regular expression.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    // TODO: implement support.
    public static final String FILTER_STATIC_CONTENT_REGEXP = "jersey.config.servlet.filter.staticContentPath";
    /**
     * If set to {@code true} and a 404 response with no entity body is returned
     * from either the runtime or the application then the runtime forwards the
     * request to the next filter in the filter chain. This enables another filter
     * or the underlying servlet engine to process the request. Before the request
     * is forwarded the response status is set to 200.
     * <p>
     * This property is an alternative to setting a {@link #FILTER_STATIC_CONTENT_REGEXP
     * static content regular expression} and requires less configuration. However,
     * application code, such as methods corresponding to sub-resource locators,
     * may be invoked when this feature is enabled.
     * <p />
     * The property is only applicable when {@link ServletContainer Jersey servlet
     * container} is configured to run as a {@link javax.servlet.Filter}, otherwise
     * this property will be ignored.
     * <p />
     * Application code, such as methods corresponding to sub-resource locators
     * may be invoked when this feature is enabled.
     * <p>
     * The default value is {@code false}.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    // TODO: implement support.
    public static final String FILTER_FORWARD_ON_404 = "jersey.config.servlet.filter.forwardOnNotFound";
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
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    // TODO: implement support.
    public static final String FILTER_CONTEXT_PATH = "jersey.config.servlet.filter.contextPath";

    private ServletProperties() {
        // prevents instantiation
    }
}
