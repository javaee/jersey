/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.core.MultivaluedMap;

/**
 * Extract value of the parameter by returning the first string parameter value
 * found in the list of string parameter values.
 * <p />
 * This class can be seen as a special, optimized, case of {@link SingleValueExtractor}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class SingleStringValueExtractor implements MultivaluedParameterExtractor<String> {

    private final String paramName;
    private final String defaultValue;

    /**
     * Create new single string value extractor.
     *
     * @param parameterName string parameter name.
     * @param defaultValue  default value.
     */
    public SingleStringValueExtractor(String parameterName, String defaultValue) {
        this.paramName = parameterName;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getName() {
        return paramName;
    }

    @Override
    public String getDefaultValueString() {
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation return s the first String value found in the list of
     * potential multiple string parameter values. Any other values in the multi-value
     * list will be ignored.
     *
     * @param parameters map of parameters.
     * @return extracted single string parameter value.
     */
    @Override
    public String extract(MultivaluedMap<String, String> parameters) {
        String value = parameters.getFirst(paramName);
        return (value != null) ? value : defaultValue;
    }
}
