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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.PathSegment;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.ExtractorException;
import org.glassfish.jersey.server.ParamException.PathParamException;
import org.glassfish.jersey.server.model.Parameter;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * {@link PathParam &#64;PathParam} injection value factory provider.
 *
 * @author Paul Sandoz
 */
@Singleton
final class PathParamValueFactoryProvider extends AbstractValueFactoryProvider {

    /**
     * {@link PathParam &#64;PathParam} injection resolver.
     */
    @Singleton
    static final class InjectionResolver extends ParamInjectionResolver<PathParam> {

        /**
         * Create new {@link PathParam &#64;PathParam} injection resolver.
         */
        public InjectionResolver() {
            super(PathParamValueFactoryProvider.class);
        }
    }

    private static final class PathParamValueFactory extends AbstractContainerRequestValueFactory<Object> {

        private final MultivaluedParameterExtractor<?> extractor;
        private final boolean decode;

        PathParamValueFactory(MultivaluedParameterExtractor<?> extractor, boolean decode) {
            this.extractor = extractor;
            this.decode = decode;
        }

        @Override
        public Object provide() {
            try {
                return extractor.extract(getContainerRequest().getUriInfo().getPathParameters(decode));
            } catch (ExtractorException e) {
                throw new PathParamException(e.getCause(), extractor.getName(), extractor.getDefaultValueString());
            }
        }
    }

    private static final class PathParamPathSegmentValueFactory extends AbstractContainerRequestValueFactory<PathSegment> {

        private final String name;
        private final boolean decode;

        PathParamPathSegmentValueFactory(String name, boolean decode) {
            this.name = name;
            this.decode = decode;
        }

        @Override
        public PathSegment provide() {
            List<PathSegment> ps = getContainerRequest().getUriInfo().getPathSegments(name, decode);
            if (ps.isEmpty()) {
                return null;
            }
            return ps.get(ps.size() - 1);
        }
    }

    private static final class PathParamListPathSegmentValueFactory
            extends AbstractContainerRequestValueFactory<List<PathSegment>> {

        private final String name;
        private final boolean decode;

        PathParamListPathSegmentValueFactory(String name, boolean decode) {
            this.name = name;
            this.decode = decode;
        }

        @Override
        public List<PathSegment> provide() {
            return getContainerRequest().getUriInfo().getPathSegments(name, decode);
        }
    }

    /**
     * Injection constructor.
     *
     * @param mpep    multivalued map parameter extractor provider.
     * @param locator HK2 service locator.
     */
    @Inject
    public PathParamValueFactoryProvider(MultivaluedParameterExtractorProvider mpep, ServiceLocator locator) {
        super(mpep, locator, Parameter.Source.PATH);
    }

    @Override
    public AbstractContainerRequestValueFactory<?> createValueFactory(Parameter parameter) {
        String parameterName = parameter.getSourceName();
        if (parameterName == null || parameterName.length() == 0) {
            // Invalid URI parameter name
            return null;
        }

        final Class<?> rawParameterType = parameter.getRawType();
        if (rawParameterType == PathSegment.class) {
            return new PathParamPathSegmentValueFactory(parameterName, !parameter.isEncoded());
        } else if (rawParameterType == List.class && parameter.getType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) parameter.getType();
            Type[] targs = pt.getActualTypeArguments();
            if (targs.length == 1 && targs[0] == PathSegment.class) {
                return new PathParamListPathSegmentValueFactory(
                        parameterName, !parameter.isEncoded());
            }
        }

        MultivaluedParameterExtractor<?> e = get(parameter);
        if (e == null) {
            return null;
        }

        return new PathParamValueFactory(e, !parameter.isEncoded());
    }
}
