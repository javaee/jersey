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

import javax.ws.rs.CookieParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.ExtractorException;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.model.Parameter;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Value factory provider supporting the {@link CookieParam} injection annotation.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
final class CookieParamValueSupplierProvider extends AbstractValueSupplierProvider {

    /**
     * {@link CookieParam} annotation value factory provider injection constructor.
     *
     * @param mpep     multivalued parameter extractor provider.
     * @param injector injector instance.
     */
    @Inject
    public CookieParamValueSupplierProvider(MultivaluedParameterExtractorProvider mpep, ServiceLocator injector) {
        super(mpep, injector, Parameter.Source.COOKIE);
    }

    @Override
    public AbstractRequestDerivedValueSupplier<?> createValueSupplier(
            Parameter parameter,
            Provider<ContainerRequest> requestProvider) {

        String parameterName = parameter.getSourceName();
        if (parameterName == null || parameterName.length() == 0) {
            // Invalid cookie parameter name
            return null;
        }

        if (parameter.getRawType() == Cookie.class) {
            return new CookieTypeParamValueSupplier(parameterName, requestProvider);
        } else {
            MultivaluedParameterExtractor e = get(parameter);
            if (e == null) {
                return null;
            }
            return new CookieParamValueSupplier(e, requestProvider);
        }
    }

    /**
     * Injection resolver for {@link CookieParam} annotation.
     */
    @Singleton
    static final class InjectionResolver extends ParamInjectionResolver<CookieParam> {

        /**
         * Create new {@link CookieParam} annotation injection resolver.
         */
        public InjectionResolver() {
            super(CookieParamValueSupplierProvider.class);
        }
    }

    private static final class CookieParamValueSupplier extends AbstractRequestDerivedValueSupplier<Object> {

        private final MultivaluedParameterExtractor<?> extractor;

        CookieParamValueSupplier(MultivaluedParameterExtractor<?> extractor, Provider<ContainerRequest> requestProvider) {
            super(requestProvider);

            this.extractor = extractor;
        }

        @Override
        public Object get() {
            // TODO: cache?
            MultivaluedMap<String, String> cookies = new MultivaluedStringMap();

            for (Map.Entry<String, Cookie> e : getRequest().getCookies().entrySet()) {
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

    private static final class CookieTypeParamValueSupplier extends AbstractRequestDerivedValueSupplier<Cookie> {

        private final String name;

        CookieTypeParamValueSupplier(String name, Provider<ContainerRequest> requestProvider) {
            super(requestProvider);

            this.name = name;
        }

        @Override
        public Cookie get() {
            return getRequest().getCookies().get(name);
        }
    }
}
