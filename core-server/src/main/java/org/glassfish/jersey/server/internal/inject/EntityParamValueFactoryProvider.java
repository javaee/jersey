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
package org.glassfish.jersey.server.internal.inject;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.Parameter;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Provides injection of {@link Request} entity value or {@link Request} instance
 * itself.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
class EntityParamValueFactoryProvider extends AbstractValueFactoryProvider {

    /**
     * Creates new instance initialized with parameters.
     *
     * @param mpep     Injected multivaluedParameterExtractor provider.
     * @param injector Injected HK2 injector.
     */
    @Inject
    EntityParamValueFactoryProvider(MultivaluedParameterExtractorProvider mpep, ServiceLocator injector) {
        super(mpep, injector, Parameter.Source.ENTITY);
    }

    private static class EntityValueFactory extends AbstractContainerRequestValueFactory<Object> {

        private final Parameter parameter;

        public EntityValueFactory(Parameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Object provide() {
            final ContainerRequest requestContext = getContainerRequest();

            final Class<?> rawType = parameter.getRawType();

            Object value;
            if ((Request.class.isAssignableFrom(rawType) || ContainerRequestContext.class.isAssignableFrom(rawType))
                    && rawType.isInstance(requestContext)) {
                value = requestContext;
            } else {
                value = requestContext.readEntity(rawType, parameter.getType(), parameter.getAnnotations());
                if (rawType.isPrimitive() && value == null) {
                    throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                            .entity(LocalizationMessages.ERROR_PRIMITIVE_TYPE_NULL()).build());
                }
            }
            return value;

        }
    }

    @Override
    protected Factory<?> createValueFactory(Parameter parameter) {
        return new EntityValueFactory(parameter);
    }
}
