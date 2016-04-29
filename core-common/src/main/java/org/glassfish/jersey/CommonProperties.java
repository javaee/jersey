/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.internal.util.PropertiesClass;
import org.glassfish.jersey.internal.util.PropertiesHelper;

/**
 * Common (server/client) Jersey configuration properties.
 *
 * @author Michal Gajdos
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
@PropertiesClass
public final class CommonProperties {

    private static final Map<String, String> LEGACY_FALLBACK_MAP = new HashMap<String, String>();

    static {
        LEGACY_FALLBACK_MAP.put(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER_CLIENT,
                "jersey.config.contentLength.buffer.client");
        LEGACY_FALLBACK_MAP.put(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER_SERVER,
                "jersey.config.contentLength.buffer.server");
        LEGACY_FALLBACK_MAP.put(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE_CLIENT,
                "jersey.config.disableAutoDiscovery.client");
        LEGACY_FALLBACK_MAP.put(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE_SERVER,
                "jersey.config.disableAutoDiscovery.server");
        LEGACY_FALLBACK_MAP.put(CommonProperties.JSON_PROCESSING_FEATURE_DISABLE_CLIENT,
                "jersey.config.disableJsonProcessing.client");
        LEGACY_FALLBACK_MAP.put(CommonProperties.JSON_PROCESSING_FEATURE_DISABLE_SERVER,
                "jersey.config.disableJsonProcessing.server");
        LEGACY_FALLBACK_MAP.put(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE_CLIENT,
                "jersey.config.disableMetainfServicesLookup.client");
        LEGACY_FALLBACK_MAP.put(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE_SERVER,
                "jersey.config.disableMetainfServicesLookup.server");
        LEGACY_FALLBACK_MAP.put(CommonProperties.MOXY_JSON_FEATURE_DISABLE_CLIENT,
                "jersey.config.disableMoxyJson.client");
        LEGACY_FALLBACK_MAP.put(CommonProperties.MOXY_JSON_FEATURE_DISABLE_SERVER,
                "jersey.config.disableMoxyJson.server");
    }

    /**
     * If {@code true} then disable feature auto discovery globally on client/server.
     * <p>
     * By default auto discovery is automatically enabled. The value of this property may be overridden by the client/server
     * variant of this property.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String FEATURE_AUTO_DISCOVERY_DISABLE = "jersey.config.disableAutoDiscovery";

    /**
     * Client-specific version of {@link CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE}.
     *
     * If present, it overrides the generic one for the client environment.
     * @since 2.8
     */
    public static final String FEATURE_AUTO_DISCOVERY_DISABLE_CLIENT = "jersey.config.client.disableAutoDiscovery";

    /**
     * Server-specific version of {@link CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE}.
     *
     * If present, it overrides the generic one for the server environment.
     * @since 2.8
     */
    public static final String FEATURE_AUTO_DISCOVERY_DISABLE_SERVER = "jersey.config.server.disableAutoDiscovery";

    /**
     * If {@code true} then disable configuration of Json Processing (JSR-353) feature.
     * <p>
     * By default Json Processing is automatically enabled. The value of this property may be overridden by the client/server
     * variant of this property.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String JSON_PROCESSING_FEATURE_DISABLE = "jersey.config.disableJsonProcessing";

    /**
     * Client-specific version of {@link CommonProperties#JSON_PROCESSING_FEATURE_DISABLE}.
     *
     * If present, it overrides the generic one for the client environment.
     * @since 2.8
     */
    public static final String JSON_PROCESSING_FEATURE_DISABLE_CLIENT = "jersey.config.client.disableJsonProcessing";

    /**
     * Server-specific version of {@link CommonProperties#JSON_PROCESSING_FEATURE_DISABLE}.
     *
     * If present, it overrides the generic one for the server environment.
     * @since 2.8
     */
    public static final String JSON_PROCESSING_FEATURE_DISABLE_SERVER = "jersey.config.server.disableJsonProcessing";

    /**
     * If {@code true} then disable META-INF/services lookup globally on client/server.
     * <p>
     * By default Jersey looks up SPI implementations described by META-INF/services/* files.
     * Then you can register appropriate provider classes by {@link javax.ws.rs.core.Application}.
     * </p>
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.1
     */
    public static final String METAINF_SERVICES_LOOKUP_DISABLE = "jersey.config.disableMetainfServicesLookup";

    /**
     * Client-specific version of {@link CommonProperties#METAINF_SERVICES_LOOKUP_DISABLE}.
     *
     * If present, it overrides the generic one for the client environment.
     * @since 2.8
     */
    public static final String METAINF_SERVICES_LOOKUP_DISABLE_CLIENT = "jersey.config.client.disableMetainfServicesLookup";

    /**
     * Server-specific version of {@link CommonProperties#METAINF_SERVICES_LOOKUP_DISABLE}.
     *
     * If present, it overrides the generic one for the server environment.
     * @since 2.8
     */
    public static final String METAINF_SERVICES_LOOKUP_DISABLE_SERVER = "jersey.config.server.disableMetainfServicesLookup";

    /**
     * If {@code true} then disable configuration of MOXy Json feature.
     * <p>
     * By default MOXy Json is automatically enabled. The value of this property may be overridden by the client/server
     * variant of this property.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String MOXY_JSON_FEATURE_DISABLE = "jersey.config.disableMoxyJson";

    /**
     * Client-specific version of {@link CommonProperties#MOXY_JSON_FEATURE_DISABLE}.
     *
     * If present, it overrides the generic one for the client environment.
     * @since 2.8
     */
    public static final String MOXY_JSON_FEATURE_DISABLE_CLIENT = "jersey.config.client.disableMoxyJson";

    /**
     * Server-specific version of {@link CommonProperties#MOXY_JSON_FEATURE_DISABLE}.
     *
     * If present, it overrides the generic one for the server environment.
     * @since 2.8
     */
    public static final String MOXY_JSON_FEATURE_DISABLE_SERVER = "jersey.config.server.disableMoxyJson";

    /**
     * An integer value that defines the buffer size used to buffer the outbound message entity in order to
     * determine its size and set the value of HTTP <tt>{@value javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH}</tt> header.
     * <p>
     * If the entity size exceeds the configured buffer size, the buffering would be cancelled and the entity size
     * would not be determined. Value less or equal to zero disable the buffering of the entity at all.
     * </p>
     * The value of this property may be overridden by the client/server variant of this property by defining the suffix
     * to this property "<tt>.server</tt>" or "<tt>.client</tt>"
     * (<tt>{@value}.server</tt> or  <tt>{@value}.client</tt>).
     * <p>
     * The default value is <tt>8192</tt>.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String OUTBOUND_CONTENT_LENGTH_BUFFER = "jersey.config.contentLength.buffer";

    /**
     * Client-specific version of {@link CommonProperties#OUTBOUND_CONTENT_LENGTH_BUFFER}.
     *
     * If present, it overrides the generic one for the client environment.
     * @since 2.8
     */
    public static final String OUTBOUND_CONTENT_LENGTH_BUFFER_CLIENT = "jersey.config.client.contentLength.buffer";

    /**
     * Server-specific version of {@link CommonProperties#OUTBOUND_CONTENT_LENGTH_BUFFER}.
     *
     * If present, it overrides the generic one for the server environment.
     * @since 2.8
     */
    public static final String OUTBOUND_CONTENT_LENGTH_BUFFER_SERVER = "jersey.config.server.contentLength.buffer";

    /**
     * Prevent instantiation.
     */
    private CommonProperties() {
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the actual property value type is not compatible with the specified type, the method will
     * return {@code null}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param propertyName  Name of the property.
     * @param type          Type to retrieve the value as.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static Object getValue(final Map<String, ?> properties, final String propertyName, final Class<?> type) {
        return PropertiesHelper.getValue(properties, propertyName, type, CommonProperties.LEGACY_FALLBACK_MAP);
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with {@code defaultValue} type,
     * the specified {@code defaultValue} is returned. Calling this method is equivalent to calling
     * {@code CommonProperties.getValue(properties, key, defaultValue, (Class&lt;T&gt;) defaultValue.getClass())}
     *
     * @param properties    Map of properties to get the property value from.
     * @param propertyName  Name of the property.
     * @param defaultValue  Default value if property is not registered
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties, final String propertyName, final T defaultValue) {
        return PropertiesHelper.getValue(properties, propertyName, defaultValue, CommonProperties.LEGACY_FALLBACK_MAP);
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with {@code defaultValue} type,
     * the specified {@code defaultValue} is returned. Calling this method is equivalent to calling
     * {@code CommonProperties.getValue(properties, runtimeType, key, defaultValue, (Class&lt;T&gt;) defaultValue.getClass())}
     *
     * @param properties    Map of properties to get the property value from.
     * @param runtime       Runtime type which is used to check whether there is a property with the same
     *                      {@code key} but post-fixed by runtime type (<tt>.server</tt>
     *                      or {@code .client}) which would override the {@code key} property.
     * @param propertyName  Name of the property.
     * @param defaultValue  Default value if property is not registered
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties,
                                 final RuntimeType runtime,
                                 final String propertyName,
                                 final T defaultValue) {
        return PropertiesHelper.getValue(properties, runtime, propertyName, defaultValue, CommonProperties.LEGACY_FALLBACK_MAP);
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with the specified value type,
     * returns {@code defaultValue}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param runtime       Runtime type which is used to check whether there is a property with the same
     *                      {@code key} but post-fixed by runtime type (<tt>.server</tt>
     *                      or {@code .client}) which would override the {@code key} property.
     * @param propertyName  Name of the property.
     * @param defaultValue  Default value if property is not registered
     * @param type          Type to retrieve the value as.
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties,
                                 final RuntimeType runtime,
                                 final String propertyName,
                                 final T defaultValue,
                                 final Class<T> type) {
        return PropertiesHelper.getValue(properties, runtime, propertyName, defaultValue, type,
                CommonProperties.LEGACY_FALLBACK_MAP);
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the actual property value type is not compatible with the specified type, the method will
     * return {@code null}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param runtime       Runtime type which is used to check whether there is a property with the same
     *                      {@code key} but post-fixed by runtime type (<tt>.server</tt>
     *                      or {@code .client}) which would override the {@code key} property.
     * @param propertyName  Name of the property.
     * @param type          Type to retrieve the value as.
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties,
                                 final RuntimeType runtime,
                                 final String propertyName,
                                 final Class<T> type) {
        return PropertiesHelper.getValue(properties, runtime, propertyName, type, CommonProperties.LEGACY_FALLBACK_MAP);
    }
}
