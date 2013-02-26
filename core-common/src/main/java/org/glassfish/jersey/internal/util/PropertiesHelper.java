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
package org.glassfish.jersey.internal.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import javax.ws.rs.core.Configuration;

/**
 * Helper class containing convenience methods for reading
 * {@code org.glassfish.jersey.server.ResourceConfig} and {@link javax.ws.rs.core.Configuration} properties.
 *
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class PropertiesHelper {
    /**
     * Returns value of a specified property. If the property is not set or the real value type is not compatible with
     * defaultValue type, the specified defaultValue is returned. Calling this method is equivalent to calling
     * {@code PropertyHelper.getValue(properties, key, defaultValue, (Class<T>) defaultValue.getClass())}
     *
     * @param properties Map of properties to get the property value from.
     * @param key Name of the property.
     * @param defaultValue Default value to be returned if the specified property is not set or cannot be read.
     * @param <T> Type of the property value.
     * @return Value of the property or defaultValue.
     */
    public static <T> T getValue(Map<String, ?> properties, String key, T defaultValue) {
        return getValue(properties, key, defaultValue, (Class<T>) defaultValue.getClass());
    }

    /**
     * Returns value of a specified property. If the property is not set or the real value type is not compatible with
     * the specified value type, returns defaultValue.
     *
     * @param properties Map of properties to get the property value from.
     * @param key Name of the property.
     * @param defaultValue Default value of the property.
     * @param type Type to retrieve the value as.
     * @param <T> Type of the property value.
     * @return Value of the property or null.
     */
    public static <T> T getValue(Map<String, ?> properties, String key, T defaultValue, Class<T> type) {
        T value = getValue(properties, key, type);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Returns value of a specified property. If the property is not set or the real value type is not compatible with
     * the specified value type, returns null.
     *
     * @param properties Map of properties to get the property value from.
     * @param key Name of the property.
     * @param type Type to retrieve the value as.
     * @param <T> Type of the property value.
     * @return Value of the property or null.
     */
    public static <T> T getValue(Map<String, ?> properties, String key, Class<T> type) {
        Object value = properties.get(key);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            // TODO: Move string value readers from server to common and utilize them here
            final Constructor constructor = ReflectionHelper.getStringConstructor(type);
            if (constructor != null) {
                try {
                    return type.cast(constructor.newInstance(value));
                } catch (Exception e) {
                    // calling the constructor wasn't successful - ignore and try valueOf()
                }
            }

            final Method valueOf = ReflectionHelper.getValueOfStringMethod(type);
            if (valueOf != null) {
                try {
                    return type.cast(valueOf.invoke(null, value));
                } catch (Exception e) {
                    // calling valueOf wasn't successful
                }
            }

            // at this point we don't know what to return -> return null
            // TODO: should also log warning
            return null;
        }

        return type.cast(value);
    }

    /**
     * Get the value of the property with a given name converted to {@code boolean}. Returns {@code false} if the value is
     * not convertible.
     *
     * @param properties key-value map of properties.
     * @param name property name.
     * @return {@code boolean} property value or {@code false} if the property is not convertible.
     */
    public static boolean isProperty(final Map<String, Object> properties, final String name) {
        return properties.containsKey(name) && isProperty(properties.get(name));
    }

    /**
     * Get the value of the property converted to {@code boolean}. Returns {@code false} if the value is not convertible.
     *
     * @param value property value.
     * @return {@code boolean} property value or {@code false} if the property is not convertible.
     */
    public static boolean isProperty(final Object value) {
        if (value instanceof Boolean) {
            return Boolean.class.cast(value);
        } else {
            return value != null && Boolean.parseBoolean(value.toString());
        }
    }

    /**
     * Determine whether a Jersey feature ({@link javax.ws.rs.core.Feature}/
     * {@link org.glassfish.jersey.internal.spi.AutoDiscoverable}) is disabled based on given global property name and it's
     * client/server variants. If runtime (client/server) variant of the global property is set then the value of this property is
     * returned, otherwise the return value is value of the global property.
     * <p/>
     * Client/Server variant of the property is derived using this pattern:
     * {@code globalPropertyName + '.' + config.getRuntimeType().name().toLowerCase()}
     *
     * @param config configuration to check the property.
     * @param globalPropertyName global property name to be checked and to derive client/server variant of the property.
     * @return {@code true} if the feature is disabled by the property value, {@code false} otherwise.
     * @see org.glassfish.jersey.CommonProperties
     */
    public static boolean isFeatureDisabledByProperty(final Configuration config, final String globalPropertyName) {
        // Runtime property.
        final Object runtimeProperty = config.getProperty(globalPropertyName + '.' + config.getRuntimeType().name().toLowerCase());
        if (runtimeProperty != null) {
            return isProperty(runtimeProperty);
        }

        // Global.
        return isProperty(config.getProperty(globalPropertyName));
    }
}
