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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.PathSegment;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.ExtractorException;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException.PathParamException;
import org.glassfish.jersey.server.model.Parameter;

/**
 * {@link PathParam &#64;PathParam} injection value provider.
 *
 * @author Paul Sandoz
 */
@Singleton
final class PathParamValueParamProvider extends AbstractValueParamProvider {

    /**
     * Injection constructor.
     *
     * @param mpep multivalued map parameter extractor provider.
     */
    public PathParamValueParamProvider(Provider<MultivaluedParameterExtractorProvider> mpep) {
        super(mpep, Parameter.Source.PATH);
    }

    @Override
    public Function<ContainerRequest, ?> createValueProvider(Parameter parameter) {
        String parameterName = parameter.getSourceName();
        if (parameterName == null || parameterName.length() == 0) {
            // Invalid URI parameter name
            return null;
        }

        final Class<?> rawParameterType = parameter.getRawType();
        if (rawParameterType == PathSegment.class) {
            return new PathParamPathSegmentValueSupplier(parameterName, !parameter.isEncoded());
        } else if (rawParameterType == List.class && parameter.getType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) parameter.getType();
            Type[] targs = pt.getActualTypeArguments();
            if (targs.length == 1 && targs[0] == PathSegment.class) {
                return new PathParamListPathSegmentValueSupplier(parameterName, !parameter.isEncoded());
            }
        }

        MultivaluedParameterExtractor<?> e = get(parameter);
        if (e == null) {
            return null;
        }

        return new PathParamValueProvider(e, !parameter.isEncoded());
    }

    private static final class PathParamValueProvider implements Function<ContainerRequest, Object> {

        private final MultivaluedParameterExtractor<?> extractor;
        private final boolean decode;

        PathParamValueProvider(MultivaluedParameterExtractor<?> extractor, boolean decode) {
            this.extractor = extractor;
            this.decode = decode;
        }

        @Override
        public Object apply(ContainerRequest request) {
            try {
                return extractor.extract(request.getUriInfo().getPathParameters(decode));
            } catch (ExtractorException e) {
                throw new PathParamException(e.getCause(), extractor.getName(), extractor.getDefaultValueString());
            }
        }
    }

    private static final class PathParamPathSegmentValueSupplier implements Function<ContainerRequest, PathSegment> {

        private final String name;
        private final boolean decode;

        PathParamPathSegmentValueSupplier(String name, boolean decode) {
            this.name = name;
            this.decode = decode;
        }

        @Override
        public PathSegment apply(ContainerRequest request) {
            List<PathSegment> ps = request.getUriInfo().getPathSegments(name, decode);
            if (ps.isEmpty()) {
                return null;
            }
            return ps.get(ps.size() - 1);
        }
    }

    private static final class PathParamListPathSegmentValueSupplier implements Function<ContainerRequest, List<PathSegment>> {

        private final String name;
        private final boolean decode;

        PathParamListPathSegmentValueSupplier(String name, boolean decode) {
            this.name = name;
            this.decode = decode;
        }

        @Override
        public List<PathSegment> apply(ContainerRequest request) {
            return request.getUriInfo().getPathSegments(name, decode);
        }
    }
}
