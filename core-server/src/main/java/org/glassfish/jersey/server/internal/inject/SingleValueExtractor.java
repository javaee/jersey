/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverter;

import org.glassfish.jersey.internal.inject.ExtractorException;

/**
 * Extract value of the parameter using a single parameter value and the underlying
 * {@link ParamConverter param converter}.
 *
 * @param <T> extracted Java type.
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class SingleValueExtractor<T> extends AbstractParamValueExtractor<T> implements MultivaluedParameterExtractor<T> {

    /**
     * Create new single value extractor.
     *
     * @param converter          string value reader.
     * @param parameterName      string parameter name.
     * @param defaultStringValue default string value.
     */
    public SingleValueExtractor(final ParamConverter<T> converter, final String parameterName, final String defaultStringValue) {
        super(converter, parameterName, defaultStringValue);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation extracts the value of the parameter applying the underlying
     * {@link ParamConverter param converter} to the first value found in the list of potential multiple
     * parameter values. Any other values in the multi-value list will be ignored.
     *
     * @param parameters map of parameters.
     * @return extracted single parameter value.
     */
    @Override
    public T extract(final MultivaluedMap<String, String> parameters) {
        final String value = parameters.getFirst(getName());
        try {
            return fromString((value == null && isDefaultValueRegistered()) ? getDefaultValueString() : value);
        } catch (final WebApplicationException | ProcessingException ex) {
            throw ex;
        } catch (final IllegalArgumentException ex) {
            return defaultValue();
        } catch (final Exception ex) {
            throw new ExtractorException(ex);
        }
    }
}
