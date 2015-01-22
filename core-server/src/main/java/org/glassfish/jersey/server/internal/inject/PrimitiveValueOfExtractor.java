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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.internal.inject.ExtractorException;

/**
 * Extract primitive parameter value from the {@link MultivaluedMap multivalued parameter map}
 * using one of the {@code valueOf(String)} methods on the primitive Java type wrapper
 * classes.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class PrimitiveValueOfExtractor implements MultivaluedParameterExtractor<Object> {

    private final Method valueOf;
    private final String parameter;
    private final String defaultStringValue;
    private final Object defaultValue;
    private final Object defaultPrimitiveTypeValue;

    /**
     * Create new primitive parameter value extractor.
     *
     * @param valueOf                   {@code valueOf()} method handler.
     * @param parameter                 string parameter value.
     * @param defaultStringValue        default string value.
     * @param defaultPrimitiveTypeValue default primitive type value.
     */
    public PrimitiveValueOfExtractor(Method valueOf, String parameter,
                                     String defaultStringValue, Object defaultPrimitiveTypeValue) {
        this.valueOf = valueOf;
        this.parameter = parameter;
        this.defaultStringValue = defaultStringValue;
        this.defaultValue = (defaultStringValue != null)
                ? getValue(defaultStringValue) : null;
        this.defaultPrimitiveTypeValue = defaultPrimitiveTypeValue;
    }

    @Override
    public String getName() {
        return parameter;
    }

    @Override
    public String getDefaultValueString() {
        return defaultStringValue;
    }

    private Object getValue(String v) {
        try {
            return valueOf.invoke(null, v);
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (target instanceof WebApplicationException) {
                throw (WebApplicationException) target;
            } else {
                throw new ExtractorException(target);
            }
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }
    }

    @Override
    public Object extract(MultivaluedMap<String, String> parameters) {
        String v = parameters.getFirst(parameter);
        if (v != null && !v.trim().isEmpty()) {
            return getValue(v);
        } else if (defaultValue != null) {
            // TODO do we need to clone the default value?
            return defaultValue;
        }

        return defaultPrimitiveTypeValue;
    }
}
