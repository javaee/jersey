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
package org.glassfish.jersey.server;

import org.glassfish.jersey.CommonProperties;

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
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    // TODO add support for ':' and any (number of consecutive) whitespace(s).
    // TODO implement generic support
    public static final String PROVIDER_PACKAGES = "jersey.config.server.provider.packages";

    /**
     * Sets the recursion strategy for package scanning. The value of {@code true} indicates
     * that the {@link #PROVIDER_PACKAGES list of provided package names} should be scanned
     * recursively including any nested packages. Value of {@code false} indicates that only
     * packages in the list should be scanned. In such case any nested packages will be ignored.
     * <p>
     * The property value MUST be an instance of {@code Boolean} type or a {@code String} convertible
     * to {@code Boolean} type.
     * </p>
     * <p>
     * A default value is {@code true}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #PROVIDER_PACKAGES
     */
    public static final String PROVIDER_SCANNING_RECURSIVE = "jersey.config.server.provider.scanning.recursive";

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
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    // TODO add support for ':' and any (number of consecutive) whitespace(s).
    // TODO implement generic support
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
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    // TODO implement generic support
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
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
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
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String LANGUAGE_MAPPINGS = "jersey.config.server.languageMappings";

    /**
     * Defines configuration of HTTP method overriding.
     * This property is used by {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter} to determine
     * where it should look for method override information (e.g. request header or query parameters).
     * {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source} enum lists the allowed property
     * values.
     * <p>
     * The property value must be an instance of {@link String}, {@code String[]},
     * {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source Source} or
     * {@code Source[]}.
     * Each {@code String} instance represents one or more class names separated by characters declared in
     * common delimiters: {@code " ,;\n"}.
     * </p>
     * <p>
     * The default value is {@code "HEADER, QUERY"}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String HTTP_METHOD_OVERRIDE = "jersey.config.server.httpMethodOverride";

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
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String WADL_GENERATOR_CONFIG = "jersey.config.server.wadl.generatorConfig";

    /**
     * If {@code true} then disable WADL generation.
     * <p>
     * By default WADL generation is automatically enabled, if JAXB is
     * present in the classpath and the auto-discovery feature is enabled or if an appropriate {@link javax.ws.rs.core.Feature
     * feature} is enabled.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #FEATURE_AUTO_DISCOVERY_DISABLE
     * @see org.glassfish.jersey.CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE
     */
    public static final String WADL_FEATURE_DISABLE = "jersey.config.server.wadl.disableWadl";

    /**
     * If {@code true} then disable Bean Validation support.
     * <p>
     * By default Bean Validation (JSR-349) is automatically enabled, if {@code org.glassfish.jersey.ext::jersey-bean-validation}
     * Jersey module is present in the classpath and the auto-discovery support is not disabled (see
     * {@link #FEATURE_AUTO_DISCOVERY_DISABLE}) or &mdash; in case the Jersey auto-discovery support is disabled &mdash; if the
     * {@code org.glassfish.jersey.server.validation.ValidationFeature} is explicitly registered in a JAX-RS/Jersey application.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #FEATURE_AUTO_DISCOVERY_DISABLE
     * @see org.glassfish.jersey.CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE
     */
    public static final String BV_FEATURE_DISABLE = "jersey.config.beanValidation.disable.server";

    /**
     * A Bean Validation (JSR-349) support customization property.
     *
     * If {@code true} the check whether the overriding / implementing methods are annotated with
     * {@link javax.validation.executable.ValidateOnExecution} as well as one of their predecessor (in hierarchy)
     * is disabled.
     * <p>
     * By default this checks is automatically enabled, unless the Bean Validation support is disabled explicitly (see
     * {@link #BV_FEATURE_DISABLE}).
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see javax.validation.executable.ValidateOnExecution
     */
    public static final String BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK =
            "jersey.config.beanValidation.disable.validateOnExecutableCheck.server";

    /**
     * A Bean Validation (JSR-349) support customization property.
     *
     * If set to {@code true} and Bean Validation support has not been explicitly disabled (see
     * {@link #BV_FEATURE_DISABLE}), the validation error information will be sent in the entity of the
     * returned {@link javax.ws.rs.core.Response}.
     * <p>
     * The default value is {@code false}. This means that in case of an error response caused by a Bean Validation
     * error, only a status code is sent in the server {@code Response} by default.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String BV_SEND_ERROR_IN_RESPONSE
            = "jersey.config.beanValidation.enableOutputValidationErrorEntity.server";

    /**
     * If {@code true} then disable auto discovery on server.
     * <p>
     * By default auto discovery is automatically enabled if global property
     * {@value org.glassfish.jersey.CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE} is not disabled. If set then the server
     * property value overrides the global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE
     * @see #BV_FEATURE_DISABLE
     * @see #WADL_FEATURE_DISABLE
     */
    public static final String FEATURE_AUTO_DISCOVERY_DISABLE = CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE + ".server";

    /**
     * If {@code true} then disable configuration of Json Processing (JSR-353) feature on server.
     * <p>
     * By default Json Processing is automatically enabled if global property
     * {@value org.glassfish.jersey.CommonProperties#JSON_PROCESSING_FEATURE_DISABLE} is not disabled. If set then the server
     * property value overrides the global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.CommonProperties#JSON_PROCESSING_FEATURE_DISABLE
     */
    public static final String JSON_PROCESSING_FEATURE_DISABLE = CommonProperties.JSON_PROCESSING_FEATURE_DISABLE + ".server";

    /**
     * If {@code true} then disable configuration of MOXy Json feature on server.
     * <p>
     * By default MOXy Json is automatically enabled if global property
     * {@value org.glassfish.jersey.CommonProperties#MOXY_JSON_FEATURE_DISABLE} is not disabled. If set then the server
     * property value overrides the global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.CommonProperties#MOXY_JSON_FEATURE_DISABLE
     */
    public static final String MOXY_JSON_FEATURE_DISABLE = CommonProperties.MOXY_JSON_FEATURE_DISABLE + ".server";

    /**
     * If {@code true} then the extensive validation of application resource model is disabled.
     *
     * This impacts both the validation of root resources during deployment as well as validation of any sub resources
     * returned from sub-resource locators.
     * <p>
     * This option is typically used for performance purpose. Note however that in case the application resource models are
     * not valid, setting the property to {@code true} can cause invalid behaviour and hard to diagnose issues at runtime.
     * </p>
     * <p>
     * By default the resource validation is run on models which are created either from the supplied resource class or instance
     * as well as on any  directly provided {@link org.glassfish.jersey.server.model.Resource resource} models.
     * </p>
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #RESOURCE_VALIDATION_IGNORE_ERRORS
     */
    public static final String RESOURCE_VALIDATION_DISABLE = "jersey.config.server.resource.validation.disable";

    /**
     * If {@code true} then validation of application resource models does not fail even in case of a fatal
     * validation errors. All resource model validation issues are still output to the log, unless the resource
     * model validation is completely disabled (see {@link #RESOURCE_VALIDATION_DISABLE}).
     *
     * This impacts both the validation of root resources during deployment as well as validation of any sub resources
     * returned from sub-resource locators. The option is typically used during development and testing.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #RESOURCE_VALIDATION_DISABLE
     */
    public static final String RESOURCE_VALIDATION_IGNORE_ERRORS =
            "jersey.config.server.resource.validation.ignoreErrors";

    private ServerProperties() {
        // prevents instantiation
    }
}
