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
package org.glassfish.jersey.server;

import java.util.Map;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.util.PropertiesClass;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.PropertyAlias;


/**
 * Jersey server-side configuration properties.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @author Michal Gajdos
 * @author Martin Matula
 */
@PropertiesClass
public final class ServerProperties {

    /**
     * Defines one or more packages that contain application-specific resources and
     * providers.
     *
     * If the property is set, the specified packages will be scanned for
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
     * Sets the recursion strategy for package scanning.
     *
     * The value of {@code true} indicates
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
     * providers.
     *
     * If the property is set, the specified class-path will be scanned
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
     * and providers.
     *
     * If the property is set, the specified classes will be instantiated
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
     *
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
     *
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
     *
     * This property is used by {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter} to determine
     * where it should look for method override information (e.g. request header or query parameters).
     * {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source} enum lists the allowed property
     * values.
     * </p>
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
     *
     * If this property is not set the default wadl generator will be used for generating wadl.
     * <p>
     * The type of this property must be a subclass or an instance of a subclass of
     * {@link org.glassfish.jersey.server.wadl.config.WadlGeneratorConfig}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String WADL_GENERATOR_CONFIG = "jersey.config.server.wadl.generatorConfig";

    /**
     * If {@code true} then disable WADL generation.
     *
     * By default WADL generation is automatically enabled, if JAXB is present in the classpath.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String WADL_FEATURE_DISABLE = "jersey.config.server.wadl.disableWadl";

    /**
     * If {@code true} then disable Bean Validation support.
     *
     * By default Bean Validation (JSR-349) is automatically enabled, if {@code org.glassfish.jersey.ext::jersey-bean-validation}
     * Jersey module is present in the classpath.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
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
     * </p>
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
     * If set to {@code true} then a container will ignore multiple slashes between a port and a context path and will resolve it
     * as URI with only one slash.
     * <p>
     * The default value is {@code false}. This means that in case of multiple slashes a container will match with the address
     * with the same number of slashes before a context path.
     * </p>
     * <p>
     * The property influences only Grizzly Container from containers which are supported by Jersey. Other containers have
     * limited or disabled usage of a context path.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String REDUCE_CONTEXT_PATH_SLASHES_ENABLED
            = "jersey.config.server.reduceContextPathSlashes.enabled";

    /**
     * If {@code true} then disable auto discovery on server.
     *
     * By default auto discovery is automatically enabled if global property
     * {@value org.glassfish.jersey.CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE} is not disabled. If set then the server
     * property value overrides the global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * <p>
     * This constant is an alias for {@link CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE_SERVER}
     * </p>
     *
     * @see org.glassfish.jersey.CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE
     */
    @PropertyAlias
    public static final String FEATURE_AUTO_DISCOVERY_DISABLE = CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE_SERVER;

    /**
     * An integer value that defines the buffer size used to buffer server-side response entity in order to
     * determine its size and set the value of HTTP <tt>{@value javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH}</tt> header.
     * <p>
     * If the entity size exceeds the configured buffer size, the buffering would be cancelled and the entity size
     * would not be determined. Value less or equal to zero disable the buffering of the entity at all.
     * </p>
     * This property can be used on the server side to override the outbound message buffer size value - default or the global
     * custom value set using the {@value org.glassfish.jersey.CommonProperties#OUTBOUND_CONTENT_LENGTH_BUFFER} global property.
     * <p>
     * The default value is <tt>8192</tt>.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * <p>This constant is an alias for {@link CommonProperties#OUTBOUND_CONTENT_LENGTH_BUFFER_SERVER}</p>
     *
     * @since 2.2
     */
    @PropertyAlias
    public static final String OUTBOUND_CONTENT_LENGTH_BUFFER = CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER_SERVER;

    /**
     * If {@code true} then disable configuration of Json Processing (JSR-353) feature on server.
     *
     * By default Json Processing is automatically enabled if global property
     * {@value org.glassfish.jersey.CommonProperties#JSON_PROCESSING_FEATURE_DISABLE} is not disabled. If set then the server
     * property value overrides the global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * <p>This constant is an alias for {@link CommonProperties#JSON_PROCESSING_FEATURE_DISABLE_SERVER}</p>
     *
     * @see org.glassfish.jersey.CommonProperties#JSON_PROCESSING_FEATURE_DISABLE
     */
    @PropertyAlias
    public static final String JSON_PROCESSING_FEATURE_DISABLE = CommonProperties.JSON_PROCESSING_FEATURE_DISABLE_SERVER;

    /**
     * If {@code true} then disable META-INF/services lookup on server.
     *
     * By default Jersey looks up SPI implementations described by META-INF/services/* files.
     * Then you can register appropriate provider classes by {@link javax.ws.rs.core.Application}.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * <p>This constant is an alias for {@link CommonProperties#METAINF_SERVICES_LOOKUP_DISABLE_SERVER}</p>
     *
     * @see org.glassfish.jersey.CommonProperties#METAINF_SERVICES_LOOKUP_DISABLE
     * @since 2.1
     */
    @PropertyAlias
    public static final String METAINF_SERVICES_LOOKUP_DISABLE = CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE_SERVER;

    /**
     * If {@code true} then disable configuration of MOXy Json feature on server.
     *
     * By default MOXy Json is automatically enabled if global property
     * {@value org.glassfish.jersey.CommonProperties#MOXY_JSON_FEATURE_DISABLE} is not disabled. If set then the server
     * property value overrides the global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * <p>This constant is an alias for {@link CommonProperties#MOXY_JSON_FEATURE_DISABLE_SERVER}</p>
     *
     * @see org.glassfish.jersey.CommonProperties#MOXY_JSON_FEATURE_DISABLE
     */
    @PropertyAlias
    public static final String MOXY_JSON_FEATURE_DISABLE = CommonProperties.MOXY_JSON_FEATURE_DISABLE_SERVER;

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

    /**
     * If {@code true} then application monitoring will be enabled.
     *
     * This will enable the possibility
     * of injecting {@link org.glassfish.jersey.server.monitoring.ApplicationInfo} into resource and providers.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.12
     */
    public static final String MONITORING_ENABLED = "jersey.config.server.monitoring.enabled";

    /**
     * If {@code true} then calculation of monitoring statistics will be enabled.
     *
     * This will enable the possibility
     * of injecting {@link org.glassfish.jersey.server.monitoring.MonitoringStatistics} into resource and providers
     * and also the registered listeners
     * implementing {@link org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener} will be called
     * when statistics are available.
     * Monitoring statistics extends basic monitoring feature. Therefore when enabled,
     * the monitoring gets automatically enabled too (the same result as setting the property
     * {@link #MONITORING_ENABLED} {@code true}).
     * Enabling statistics has negative performance impact and therefore should
     * be enabled only when needed.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.server.monitoring.MonitoringStatistics
     * @see org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener
     * @see #MONITORING_ENABLED
     */
    public static final String MONITORING_STATISTICS_ENABLED = "jersey.config.server.monitoring.statistics.enabled";

    /**
     * If {@code true} then Jersey will expose MBeans with monitoring statistics.
     *
     * Exposed JMX MBeans are based
     * on {@link org.glassfish.jersey.server.monitoring.MonitoringStatistics} and therefore when they are enabled,
     * also the calculation of monitoring statistics needs to be enabled. Therefore if this property is {@code true}
     * the calculation of monitoring statistics is automatically enabled (the same result as setting the property
     * {@link #MONITORING_STATISTICS_ENABLED} to {@code true}).
     * <p/>
     * Enabling statistics MBeans has negative
     * performance impact and therefore should be enabled only when needed.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #MONITORING_STATISTICS_ENABLED
     */
    public static final String MONITORING_STATISTICS_MBEANS_ENABLED = "jersey.config.server.monitoring.statistics.mbeans.enabled";

    /**
     * Interval (in {@code ms}) indicating how often will be monitoring statistics refreshed and
     * {@link org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener#onStatistics(org.glassfish.jersey.server.monitoring.MonitoringStatistics) onStatistics}
     * method called.
     * <p/>
     * The default value is {@code 500}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     * <p/>
     *
     * @since 2.10
     */
    public static final String MONITORING_STATISTICS_REFRESH_INTERVAL =
            "jersey.config.server.monitoring.statistics.refresh.interval";

    /**
     * {@link String} property that defines the application name.
     *
     * The name is an arbitrary user defined name
     * which is used to distinguish between Jersey applications in the case that more applications
     * are deployed on the same runtime (container). The name can be used for example for purposes
     * of monitoring by JMX when name identifies to which application deployed MBeans belong to.
     * The name should be unique in the runtime.
     * <p>
     * The property is ignored
     * if the application name is set programmatically by
     * {@link org.glassfish.jersey.server.ResourceConfig#getApplicationName()}.
     * </p/>
     * <p>
     * There is no default value.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String APPLICATION_NAME = "jersey.config.server.application.name";

    /**
     * Enable tracing support.
     *
     * It allows service developer to get diagnostic information about request processing by Jersey.
     * Those diagnostic/tracing information are returned in response headers ({@code X-Jersey-Tracing-nnn}).
     * The feature should not be switched on on production environment.
     *
     * <p>
     * Allowed values:
     * <ul>
     *     <li>{@code OFF} - tracing support is disabled.</li>
     *     <li>{@code ON_DEMAND} - tracing support is in 'stand by' mode, it is enabled on demand by existence of request HTTP header</li>
     *     <li>{@code ALL} - tracing support is enabled for every request.</li>
     * </ul>
     * Type of the property value is {@code String}. The default value is {@code "OFF"}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.3
     */
    public static final String TRACING = "jersey.config.server.tracing.type";

    /**
     * Set level o tracing information.
     *
     * The property allows to set application default level o diagnostic information.
     * Tracing level can be changed for each request by specifying request HTTP header {@code X-Jersey-Tracing-Threshold}.
     *
     * <p>
     * Allowed values:
     * <ul>
     *     <li>{@code SUMMARY}</li>
     *     <li>{@code TRACE}</li>
     *     <li>{@code VERBOSE}</li>
     * </ul>
     * Type of the property value is {@code String}. The default value is {@code "TRACE"}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #TRACING
     * @since 2.3
     */
    public static final String TRACING_THRESHOLD = "jersey.config.server.tracing.threshold";

    /**
     * Whenever response status is {@code 4xx} or {@code 5xx} it is possible to choose between {@code sendError} or
     * {@code setStatus} on container specific {@code Response} implementation. E.g. on servlet container Jersey
     * can call {@code HttpServletResponse.setStatus(...)} or {@code HttpServletResponse.sendError(...)}.
     * <p>
     * Calling {@code sendError(...)} method usually resets entity, response headers and provide error page for
     * specified status code (e.g. servlet {@code error-page} configuration).
     * However if you want to post-process response (e.g. by servlet filter) the only
     * way to do it is calling {@code setStatus(...)} on container Response object.
     * </p>
     * <p>
     * If property value is {@code true} the method {@code Response.setStatus(...)} is used over default
     * {@code Response.sendError(...)}.
     * </p>
     * <p>
     * Type of the property value is {@code boolean}. The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.5
     */
    public static final String RESPONSE_SET_STATUS_OVER_SEND_ERROR = "jersey.config.server.response.setStatusOverSendError";

    /**
     * If property value is {@code true} then the errors raised during response processing are tried to handled using available
     * {@link org.glassfish.jersey.server.spi.ResponseErrorMapper response error mappers}.
     * <p>
     * Type of the property value is {@code boolean}. The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.8
     */
    public static final String PROCESSING_RESPONSE_ERRORS_ENABLED = "jersey.config.server.exception.processResponseErrors";

    /**
     * An integer value that defines the size of cache for sub-resource locator models. The cache is used to provide better
     * performance for application that uses JAX-RS sub-resource locators.
     * <p>
     * The default value is {@value #SUBRESOURCE_LOCATOR_DEFAULT_CACHE_SIZE}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #SUBRESOURCE_LOCATOR_DEFAULT_CACHE_SIZE
     * @see #SUBRESOURCE_LOCATOR_CACHE_AGE
     * @since 2.16
     */
    public static final String SUBRESOURCE_LOCATOR_CACHE_SIZE = "jersey.config.server.subresource.cache.size";

    /**
     * The default sub-resource locator cache size ({@value}).
     *
     * @see #SUBRESOURCE_LOCATOR_CACHE_SIZE
     * @since 2.16
     */
    public static final int SUBRESOURCE_LOCATOR_DEFAULT_CACHE_SIZE = 64;

    /**
     * An integer value that defines the maximum age (in seconds) for cached for sub-resource locator models. The age of an cache
     * entry is defined as the time since the last access (read) to the entry in the cache.
     * <p>
     * Entry aging is not enabled by default.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #SUBRESOURCE_LOCATOR_CACHE_SIZE
     * @since 2.16
     */
    public static final String SUBRESOURCE_LOCATOR_CACHE_AGE = "jersey.config.server.subresource.cache.age";

    /**
     * If {@code true} then Jersey will cache {@link org.glassfish.jersey.server.model.Resource Jersey resources} in addition to
     * caching sub-resource locator classes and instances (which are cached by default).
     * <p/>
     * To make sure the caching is effective in this case you need to return same Jersey Resource instances for same input
     * parameters from resource method. This means that generating new Jersey Resource instances for same input parameters would
     * not have any performance effect and it would only fill-up the cache.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String SUBRESOURCE_LOCATOR_CACHE_JERSEY_RESOURCE_ENABLED =
            "jersey.config.server.subresource.cache.jersey.resource.enabled";

    /**
     * If {@code true} then Jersey will resolve relative URIs in the {@code Location} http header against the
     * request URI according to RFC7231 (new HTTP Specification).
     *
     * <p>
     * This behaviour violates the JAX-RS 2.0 specification (which dates older than RFC7231).
     * If {@link #LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED} is set to {@code true}, value of this property is not taken
     * into account.
     * </p>
     *
     * <p>
     * The default value is {@code false} (JAX-RS 2.0 compliant).
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.23
     */
    public static final String LOCATION_HEADER_RELATIVE_URI_RESOLUTION_RFC7231 =
            "jersey.config.server.headers.location.relative.resolution.rfc7231";

    /**
     * If {@code true} then Jersey will not attempt to resolve relative URIs in the {@code Location} http header against the
     * request URI.
     *
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.22
     */
    public static final String LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED =
            "jersey.config.server.headers.location.relative.resolution.disabled";

    private ServerProperties() {
        // prevents instantiation
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with the specified value type,
     * returns {@code null}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param key           Name of the property.
     * @param type          Type to retrieve the value as.
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties, final String key, final Class<T> type) {
        return PropertiesHelper.getValue(properties, key, type, null);
    }


    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with {@code defaultValue} type,
     * the specified {@code defaultValue} is returned. Calling this method is equivalent to calling
     * {@code ServerProperties.getValue(properties, key, defaultValue, (Class&lt;T&gt;) defaultValue.getClass())}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param key  Name of the property.
     * @param defaultValue  Default value if property is not registered
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties, final String key, final T defaultValue) {
        return PropertiesHelper.getValue(properties, key, defaultValue, null);
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with the specified value type,
     * returns {@code defaultValue}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param key  Name of the property.
     * @param defaultValue  Default value if property is not registered
     * @param type          Type to retrieve the value as.
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties, final String key, final T defaultValue, final Class<T> type) {
        return PropertiesHelper.getValue(properties, key, defaultValue, type, null);
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with the specified value type,
     * returns {@code defaultValue}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param runtimeType   Runtime type which is used to check whether there is a property with the same
     *                      {@code key} but post-fixed by runtime type (<tt>.server</tt>
     *                      or {@code .client}) which would override the {@code key} property.
     * @param key  Name of the property.
     * @param defaultValue  Default value if property is not registered
     * @param type          Type to retrieve the value as.
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties,
                                 final RuntimeType runtimeType,
                                 final String key,
                                 final T defaultValue,
                                 final Class<T> type) {
        return PropertiesHelper.getValue(properties, runtimeType, key, defaultValue, type, null);
    }
}
