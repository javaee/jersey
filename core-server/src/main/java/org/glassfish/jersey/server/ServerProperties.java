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
package org.glassfish.jersey.server;

/**
 * Jersey server-side configuration properties.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public final class ServerProperties {

    /**
     * Defines one or more packages that contain application-specific resources and
     * providers. If the property is set, the specified packages will be scanned for
     * JAX-RS root resources (annotated with {@link javax.ws.rs.Path @Path}) and
     * providers (annotated with {@link javax.ws.rs.ext.Provider @Provider}).
     * <p>
     * The property value MUST be an instance of {@link String} or {@code String[]}
     * array. Each {@code String} instance represents one or more package names
     * that MUST be separated only by characters declared in common delimiters:
     * {@code " ,;\n"}.
     * </p>
     * <p>
     * A default value is not set.
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    // TODO add support for ':' and any (number of consecutive) whitespace(s).
    // TODO implement generic support
    @SuppressWarnings("HtmlTagCanBeJavadocTag")
    public static final String PROVIDER_PACKAGES = "jersey.config.server.provider.packages";

    /**
     * Defines class-path that contains application-specific resources and
     * providers. If the property is set, the specified class-path will be scanned
     * for JAX-RS root resources (annotated with {@link javax.ws.rs.Path @Path})
     * and providers (annotated with {@link javax.ws.rs.ext.Provider @Provider}).
     * Each path element MUST be an absolute or relative directory, or a Jar file.
     * The contents of a directory, including Java class files, jars files
     * and sub-directories are scanned (recursively).
     * <p>
     * The property value MUST be an instance of {@link String} or {@code String[]}
     * array. Each {@code String} instance represents one or more paths
     * that MUST be separated only by characters declared in common delimiters:
     * {@code " ,;\n"}.
     * </p>
     * <p>
     * A default value is not set.
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    // TODO add support for ':' and any (number of consecutive) whitespace(s).
    // TODO implement generic support
    @SuppressWarnings("HtmlTagCanBeJavadocTag")
    public static final String PROVIDER_CLASSPATH = "jersey.config.server.provider.classpath";

    /**
     * Defines one or more class names that implement application-specific resources
     * and providers. If the property is set, the specified classes will be instantiated
     * and registered as either application JAX-RS root resources (annotated with
     * {@link javax.ws.rs.Path @Path}) or providers (annotated with
     * {@link javax.ws.rs.ext.Provider @Provider}).
     * <p>
     * The property value MUST be an instance of {@link String} or {@code String[]}
     * array. Each {@code String} instance represents one or more class names
     * that MUST be separated only by characters declared in common delimiters:
     * {@code " ,;\n"}.
     * </p>
     * <p>
     * A default value is not set.
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    // TODO implement generic support
    @SuppressWarnings("HtmlTagCanBeJavadocTag")
    public static final String PROVIDER_CLASSNAMES = "jersey.config.server.provider.classnames";

    /**
     * Defines mapping of URI extensions to media types.
     * The property is used by {@link org.glassfish.jersey.server.filter.UriConnegFilter}. See it's javadoc for more
     * information on media type mappings.
     * <p>
     * The property value MUST be an instance of {@link String}, {@code String[]} or {@code Map&lt;String, MediaType&gt;}.
     * Each {@code String} instance represents one or more uri-extension-to-media-type map entries separated by
     * a comma (","). Each map entry is a key-value pair separated by a colon (":").
     * Here is an example of an acceptable String value mapping txt extension to text/plain and xml extension to application/xml:
     * <pre>txt : text/plain, xml : application/xml</pre>
     * </p>
     * <p>
     * A default value is not set.
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    @SuppressWarnings("HtmlTagCanBeJavadocTag")
    public static final String MEDIA_TYPE_MAPPINGS = "jersey.config.server.mediaTypeMappings";

    /**
     * Defines mapping of URI extensions to languages.
     * The property is used by {@link org.glassfish.jersey.server.filter.UriConnegFilter}. See it's javadoc for more
     * information on language mappings.
     * <p>
     * The property value MUST be an instance of {@link String}, {@code String[]} or {@code Map&lt;String, String&gt;}.
     * Each {@code String} instance represents one or more uri-extension-to-language map entries separated by
     * a comma (","). Each map entry is a key-value pair separated by a colon (":").
     * Here is an example of an acceptable String value mapping english extension to "en" value of Content-Language header
     * and french extension to "fr" Content-Language header value:
     * <pre>english : en, french : fr</pre>
     * </p>
     * <p>
     * A default value is not set.
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    @SuppressWarnings("HtmlTagCanBeJavadocTag")
    public static final String LANGUAGE_MAPPINGS = "jersey.config.server.languageMappings";

    /**
     * Defines configuration of HTTP method overriding.
     * This property is used by {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter} to determine
     * where it should look for method override information (e.g. request header or query parameters).
     * {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source} enum lists the allowed property
     * values.
     * <p>
     * The property value must be an instance of {@link String}, {@code String[]},
     * {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source} or
     * {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source[]}.
     * Each {@code String} instance represents one or more class names separated by characters declared in
     * common delimiters: {@code " ,;\n"}.
     * </p>
     * <p>
     *     The default value is {@code "HEADER, QUERY"}.
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    @SuppressWarnings("HtmlTagCanBeJavadocTag")
    public static final String HTTP_METHOD_OVERRIDE = "jersey.config.server.httpMethodOverride";

    /**
     * Defines configuration ({@link org.glassfish.jersey.client.ClientConfig}) for injectable
     * {@link javax.ws.rs.client.WebTarget} instances.
     * <p>
     *     The property value must be an instance of {@code Map&lt;String, }
     *     {@link org.glassfish.jersey.client.ClientConfig}{@code &gt;}.
     *     The key represents {@link org.glassfish.jersey.client.WebTarget} URI ({@link javax.ws.rs.Uri} value) and the
     *     value is the configuration which will be used to create injected {@link javax.ws.rs.client.WebTarget}.
     *
     * </p>
     * <p>
     *     The default value is {@code null} (default configuration will be used).
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    public static final String WEBTARGET_CONFIGURATION = "jersey.config.server.webTargetConfiguration";

    /**
     * If set the wadl generator configuration that provides a {@link org.glassfish.jersey.server.wadl.WadlGenerator}.
     * <p>
     * The type of this property must be a subclass or an instance of a subclass of
     * {@link org.glassfish.jersey.server.wadl.config.WadlGeneratorConfig}.
     * </p>
     * <p>
     * If this property is not set the default wadl generator will be used for generating wadl.
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    public static final String PROPERTY_WADL_GENERATOR_CONFIG = "jersey.config.server.wadl.generatorConfig";

    /**
     * If true then disable WADL generation.
     * <p>
     * By default WADL generation is automatically enabled, if JAXB is
     * present in the classpath.
     * <p>
     * The default value is false.
     * </p>
     * <p>
     * The name of the configuration property is <code>{@value}</code>.
     * </p>
     */
    public static final String FEATURE_DISABLE_WADL = "jersey.config.server.wadl.disableWadl";

    private ServerProperties() {
        // prevents instantiation
    }
}
