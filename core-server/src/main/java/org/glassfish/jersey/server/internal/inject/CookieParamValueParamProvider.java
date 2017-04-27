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

import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.CookieParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.ExtractorException;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.model.Parameter;

/**
 * Value factory provider supporting the {@link CookieParam} injection annotation.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
final class CookieParamValueParamProvider extends AbstractValueParamProvider {

    private static final class CookieParamValueProvider implements Function<ContainerRequest, Object> {

        private final MultivaluedParameterExtractor<?> extractor;

        CookieParamValueProvider(MultivaluedParameterExtractor<?> extractor) {
            this.extractor = extractor;
        }

        @Override
        public Object apply(ContainerRequest containerRequest) {
            // TODO: cache?
            MultivaluedMap<String, String> cookies = new MultivaluedStringMap();

            for (Map.Entry<String, Cookie> e : containerRequest.getCookies().entrySet()) {
                cookies.putSingle(e.getKey(), e.getValue().getValue());
            }

            try {
                return extractor.extract(cookies);
            } catch (ExtractorException ex) {
                throw new ParamException.CookieParamException(ex.getCause(),
                        extractor.getName(), extractor.getDefaultValueString());
            }
        }
    }

    private static final class CookieTypeParamValueProvider  implements Function<ContainerRequest, Cookie> {

        private final String name;

        CookieTypeParamValueProvider(String name) {
            this.name = name;
        }

        @Override
        public Cookie apply(ContainerRequest containerRequest) {
            return containerRequest.getCookies().get(name);
        }
    }

    /**
     * {@link CookieParam} annotation value factory provider injection constructor.
     *
     * @param mpep            multivalued parameter extractor provider.
     */
    public CookieParamValueParamProvider(Provider<MultivaluedParameterExtractorProvider> mpep) {
        super(mpep, Parameter.Source.COOKIE);
    }

    @Override
    public Function<ContainerRequest, ?> createValueProvider(Parameter parameter) {
        String parameterName = parameter.getSourceName();
        if (parameterName == null || parameterName.length() == 0) {
            // Invalid cookie parameter name
            return null;
        }

        if (parameter.getRawType() == Cookie.class) {
            return new CookieTypeParamValueProvider(parameterName);
        } else {
            MultivaluedParameterExtractor e = get(parameter);
            if (e == null) {
                return null;
            }
            return new CookieParamValueProvider(e);
        }
    }
}
