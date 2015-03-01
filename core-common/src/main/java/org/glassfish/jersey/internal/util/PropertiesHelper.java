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

package org.glassfish.jersey.internal.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Helper class containing convenience methods for reading
 * {@code org.glassfish.jersey.server.ResourceConfig} and {@link javax.ws.rs.core.Configuration} properties.
 *
 * @author Martin Matula
 */
public final class PropertiesHelper {

    private static final Logger LOGGER = Logger.getLogger(PropertiesHelper.class.getName());

    /**
     * Get system properties.
     *
     * This method delegates to {@link System#getProperties()} while running it in a privileged
     * code block.
     *
     * @return privileged action to obtain system properties.
     */
    public static PrivilegedAction<Properties> getSystemProperties() {
        return new PrivilegedAction<Properties>() {
            @Override
            public Properties run() {
                return System.getProperties();
            }
        };
    }

    /**
     * Get system property.
     *
     * This method delegates to {@link System#getProperty(String)} while running it in a privileged
     * code block.
     *
     * @param name system property name.
     * @return privileged action to obtain system property value that will return {@code null}
     *         if there's no such system property.
     */
    public static PrivilegedAction<String> getSystemProperty(final String name) {
        return new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(name);
            }
        };
    }

    /**
     * Get system property.
     *
     * This method delegates to {@link System#getProperty(String)} while running it in a privileged
     * code block.
     *
     * @param name system property name.
     * @param def  default property value.
     * @return privileged action to obtain system property value that will return the default value
     *         if there's no such system property.
     */
    public static PrivilegedAction<String> getSystemProperty(final String name, final String def) {
        return new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(name, def);
            }
        };
    }

    /**
     * Return value of a specified property. If the property is not set or the real value type is not compatible with
     * defaultValue type, the specified defaultValue is returned. Calling this method is equivalent to calling
     * {@code PropertyHelper.getValue(properties, key, defaultValue, (Class<T>) defaultValue.getClass())}
     *
     * @param properties   Map of properties to get the property value from.
     * @param key          Name of the property.
     * @param defaultValue Default value to be returned if the specified property is not set or cannot be read.
     * @param <T>          Type of the property value.
     * @param legacyMap    Legacy fallback map, where key is the actual property name, value is the old property name
     * @return Value of the property or defaultValue.
     */
    public static <T> T getValue(Map<String, ?> properties, String key, T defaultValue, Map<String, String> legacyMap) {
        return getValue(properties, null, key, defaultValue, legacyMap);
    }


    /**
     * Return value of a specified property. If the property is not set or the real value type is not compatible with
     * defaultValue type, the specified defaultValue is returned. Calling this method is equivalent to calling
     * {@code PropertyHelper.getValue(properties, runtimeType, key, defaultValue, (Class<T>) defaultValue.getClass())}
     *
     * @param properties   Map of properties to get the property value from.
     * @param runtimeType  Runtime type which is used to check whether there is a property with the same
     *                     {@code key} but post-fixed by runtime type (<tt>.server</tt>
     *                     or {@code .client}) which would override the {@code key} property.
     * @param key          Name of the property.
     * @param defaultValue Default value to be returned if the specified property is not set or cannot be read.
     * @param <T>          Type of the property value.
     * @param legacyMap    Legacy fallback map, where key is the actual property name, value is the old property name
     * @return Value of the property or defaultValue.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(Map<String, ?> properties,
                                 RuntimeType runtimeType,
                                 String key,
                                 T defaultValue,
                                 Map<String, String> legacyMap) {
        return getValue(properties, runtimeType, key, defaultValue, (Class<T>) defaultValue.getClass(), legacyMap);
    }

    /**
     * Returns value of a specified property. If the property is not set or the real value type is not compatible with
     * the specified value type, returns defaultValue.
     *
     * @param properties   Map of properties to get the property value from.
     * @param key          Name of the property.
     * @param defaultValue Default value of the property.
     * @param type         Type to retrieve the value as.
     * @param <T>          Type of the property value.
     * @param legacyMap    Legacy fallback map, where key is the actual property name, value is the old property name
     * @return Value of the property or null.
     */
    public static <T> T getValue(Map<String, ?> properties,
                                 String key,
                                 T defaultValue,
                                 Class<T> type,
                                 Map<String, String> legacyMap) {
        return getValue(properties, null, key, defaultValue, type, legacyMap);
    }


    /**
     * Returns value of a specified property. If the property is not set or the real value type is not compatible with
     * the specified value type, returns defaultValue.
     *
     * @param properties   Map of properties to get the property value from.
     * @param runtimeType  Runtime type which is used to check whether there is a property with the same
     *                     {@code key} but post-fixed by runtime type (<tt>.server</tt>
     *                     or {@code .client}) which would override the {@code key} property.
     * @param key          Name of the property.
     * @param defaultValue Default value of the property.
     * @param type         Type to retrieve the value as.
     * @param <T>          Type of the property value.
     * @param legacyMap    Legacy fallback map, where key is the actual property name, value is the old property name
     * @return Value of the property or null.
     */
    public static <T> T getValue(Map<String, ?> properties, RuntimeType runtimeType, String key,
                                 T defaultValue, Class<T> type, Map<String, String> legacyMap) {
        T value = getValue(properties, runtimeType, key, type, legacyMap);
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
     * @param key        Name of the property.
     * @param type       Type to retrieve the value as.
     * @param <T>        Type of the property value.
     * @param legacyMap  Legacy fallback map, where key is the actual property name, value is the old property name
     * @return Value of the property or null.
     */
    public static <T> T getValue(Map<String, ?> properties, String key, Class<T> type, Map<String, String> legacyMap) {
        return getValue(properties, null, key, type, legacyMap);
    }


    /**
     * Returns value of a specified property. If the property is not set or the real value type is not compatible with
     * the specified value type, returns null.
     *
     * @param properties  Map of properties to get the property value from.
     * @param runtimeType Runtime type which is used to check whether there is a property with the same
     *                    {@code key} but post-fixed by runtime type (<tt>.server</tt>
     *                    or {@code .client}) which would override the {@code key} property.
     * @param key         Name of the property.
     * @param type        Type to retrieve the value as.
     * @param <T>         Type of the property value.
     * @return Value of the property or null.
     */
    public static <T> T getValue(Map<String, ?> properties, RuntimeType runtimeType, String key, Class<T> type,
                                 Map<String, String> legacyMap) {
        Object value = null;
        if (runtimeType != null) {
            String runtimeAwareKey = getPropertyNameForRuntime(key, runtimeType);
            if (key.equals(runtimeAwareKey)) {
                // legacy behaviour
                runtimeAwareKey = key + "." + runtimeType.name().toLowerCase();
            }
            value = properties.get(runtimeAwareKey);
        }
        if (value == null) {
            value = properties.get(key);
        }
        if (value == null) {
            value = getLegacyFallbackValue(properties, legacyMap, key);
        }
        if (value == null) {
            return null;
        }

        return convertValue(value, type);
    }

    /**
     * Returns specific property value for given {@link RuntimeType}.
     *
     * Some properties have complementary client and server versions along with a common version (effective for both environments,
     * if the specific one is not set). This methods returns a specific name for the environment (determined by convention),
     * if runtime is not null, the property is a Jersey property name (starts with {@code jersey.config}) and does not contain
     * a runtime specific part already. If those conditions are not met, original property name is returned.
     *
     * @param key property name
     * @param runtimeType runtime type
     * @return runtime-specific property name, where possible, original key in other cases.
     * original key
     */
    public static String getPropertyNameForRuntime(String key, RuntimeType runtimeType) {
        if (runtimeType != null && key.startsWith("jersey.config")) {
            RuntimeType[] types = RuntimeType.values();
            for (RuntimeType type : types) {
                if (key.startsWith("jersey.config." + type.name().toLowerCase())) {
                    return key;
                }
            }
            return key.replace("jersey.config", "jersey.config." + runtimeType.name().toLowerCase());
        }
        return key;
    }

    private static Object getLegacyFallbackValue(Map<String, ?> properties, Map<String, String> legacyFallbackMap, String key) {
        if (legacyFallbackMap == null || !legacyFallbackMap.containsKey(key)) {
            return null;
        }
        String fallbackKey = legacyFallbackMap.get(key);
        Object value = properties.get(fallbackKey);
        if (value != null && LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config(LocalizationMessages.PROPERTIES_HELPER_DEPRECATED_PROPERTY_NAME(fallbackKey, key));
        }
        return value;
    }

    /**
     * Convert {@code Object} value to a value of the specified class type.
     *
     * @param value {@code Object} value to convert.
     * @param type conversion type.
     * @param <T> converted value type.
     * @return value converted to the specified class type.
     */
    public static <T> T convertValue(Object value, Class<T> type) {
        if (!type.isInstance(value)) {
            // TODO: Move string value readers from server to common and utilize them here
            final Constructor constructor = AccessController.doPrivileged(ReflectionHelper.getStringConstructorPA(type));
            if (constructor != null) {
                try {
                    return type.cast(constructor.newInstance(value));
                } catch (Exception e) {
                    // calling the constructor wasn't successful - ignore and try valueOf()
                }
            }

            final Method valueOf = AccessController.doPrivileged(ReflectionHelper.getValueOfStringMethodPA(type));
            if (valueOf != null) {
                try {
                    return type.cast(valueOf.invoke(null, value));
                } catch (Exception e) {
                    // calling valueOf wasn't successful
                }
            }

            // at this point we don't know what to return -> return null
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(LocalizationMessages.PROPERTIES_HELPER_GET_VALUE_NO_TRANSFORM(String.valueOf(value),
                        value.getClass().getName(), type.getName()));
            }

            return null;
        }

        return type.cast(value);
    }

    /**
     * Get the value of the property with a given name converted to {@code boolean}. Returns {@code false} if the value is
     * not convertible.
     *
     * @param properties key-value map of properties.
     * @param name       property name.
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
     * Prevent instantiation.
     */
    private PropertiesHelper() {
    }
}
