/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ParamConverter;

import org.glassfish.jersey.internal.inject.ExtractorException;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;
import org.glassfish.jersey.internal.util.collection.Values;

/**
 * Abstract base class for implementing multivalued parameter value extractor
 * logic supplied using {@link ParamConverter parameter converters}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class AbstractParamValueExtractor<T> {

    private final ParamConverter<T> paramConverter;
    private final String parameterName;
    private final String defaultValueString;
    private final UnsafeValue<T, RuntimeException> convertedDefaultValue;

    /**
     * Constructor that initializes common string reader-based parameter extractor
     * data.
     * <p />
     * As part of the initialization, the default value validation is performed
     * based on the presence and value of the {@link ParamConverter.Lazy}
     * annotation on the supplied string value reader class.
     *
     * @param converter          parameter converter.
     * @param parameterName      name of the parameter.
     * @param defaultValueString default parameter value string.
     */
    protected AbstractParamValueExtractor(ParamConverter<T> converter, String parameterName, final String defaultValueString) {
        this.paramConverter = converter;
        this.parameterName = parameterName;
        this.defaultValueString = defaultValueString;


        if (defaultValueString != null) {
            this.convertedDefaultValue = Values.lazy(new UnsafeValue<T, RuntimeException>() {
                @Override
                public T get() throws RuntimeException {
                    return convert(defaultValueString);
                }
            });

            if (!converter.getClass().isAnnotationPresent(ParamConverter.Lazy.class)) {
                // ignore return value - executed just for validation reasons
                convertedDefaultValue.get();
            }
        } else {
            convertedDefaultValue = null;
        }
    }

    /**
     * Get the name of the parameter this extractor belongs to.
     *
     * @return parameter name.
     */
    public String getName() {
        return parameterName;
    }

    /**
     * Get the default string value of the parameter.
     *
     * @return default parameter string value.
     */
    public String getDefaultValueString() {
        return defaultValueString;
    }

    /**
     * Extract parameter value from string using the configured {@link ParamConverter parameter converter}.
     *
     * A {@link WebApplicationException} thrown from the converter is propagated
     * unchanged. Any other exception throws by the converter is wrapped in a new
     * {@link ExtractorException} before rethrowing.
     *
     * @param value parameter string value to be converted/extracted.
     * @return extracted value of a given Java type.
     * @throws WebApplicationException in case the underlying parameter converter throws a {@code WebApplicationException}.
     *                                 The exception is rethrown without a change.
     * @throws ExtractorException      wrapping any other exception thrown by the parameter converter.
     */
    protected final T fromString(String value) {
        T result = convert(value);
        if (result == null) {
            return defaultValue();
        }
        return result;
    }

    private T convert(String value) {
        try {
            return paramConverter.fromString(value);
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception ex) {
            throw new ExtractorException(ex);
        }
    }

    /**
     * Check if there is a default string value registered for the parameter.
     *
     * @return {@code true} if there is a default parameter string value registered, {@code false} otherwise.
     */
    protected final boolean isDefaultValueRegistered() {
        return defaultValueString != null;
    }

    /**
     * Get converted default value.
     *
     * The conversion happens lazily during first call of the method.
     *
     * @return converted default value.
     */
    protected final T defaultValue() {
        if (!isDefaultValueRegistered()) {
            return null;
        }

        return convertedDefaultValue.get();
    }
}
