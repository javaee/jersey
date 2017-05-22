/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.model.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSink;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ParamValueFactoryWithSource;
import org.glassfish.jersey.server.spi.internal.ParameterValueHelper;
import org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

/**
 * An implementation of {@link ResourceMethodDispatcher.Provider} that
 * creates instances of {@link ResourceMethodDispatcher}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class JavaResourceMethodDispatcherProvider implements ResourceMethodDispatcher.Provider {

    private final Collection<ValueParamProvider> allValueProviders;

    JavaResourceMethodDispatcherProvider(Collection<ValueParamProvider> allValueProviders) {
        this.allValueProviders = allValueProviders;
    }

    @Override
    public ResourceMethodDispatcher create(final Invocable resourceMethod,
            final InvocationHandler invocationHandler,
            final ConfiguredValidator validator) {
        final List<ParamValueFactoryWithSource<?>> valueProviders =
                ParameterValueHelper.createValueProviders(allValueProviders, resourceMethod);
        final Class<?> returnType = resourceMethod.getHandlingMethod().getReturnType();

        ResourceMethodDispatcher resourceMethodDispatcher = null;
        if (Response.class.isAssignableFrom(returnType)) {
            resourceMethodDispatcher =
                    new ResponseOutInvoker(resourceMethod, invocationHandler, valueProviders, validator);
        } else if (returnType != void.class) {
            if (returnType == Object.class || GenericEntity.class.isAssignableFrom(returnType)) {
                resourceMethodDispatcher =
                        new ObjectOutInvoker(resourceMethod, invocationHandler, valueProviders, validator);
            } else {
                resourceMethodDispatcher =
                        new TypeOutInvoker(resourceMethod, invocationHandler, valueProviders, validator);
            }
        } else {
            // return type is void
            int i = 0;
            for (final Parameter parameter : resourceMethod.getParameters()) {
                if (SseEventSink.class.equals(parameter.getRawType())) {
                    resourceMethodDispatcher =
                            new SseEventSinkInvoker(resourceMethod, invocationHandler, valueProviders, validator, i);
                    break;
                }
                i++;
            }

            if (resourceMethodDispatcher == null) {
                resourceMethodDispatcher = new VoidOutInvoker(resourceMethod, invocationHandler, valueProviders, validator);
            }
        }

        return resourceMethodDispatcher;
    }

    private abstract static class AbstractMethodParamInvoker extends AbstractJavaResourceMethodDispatcher {

        private final List<ParamValueFactoryWithSource<?>> valueProviders;

        AbstractMethodParamInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, validator);
            this.valueProviders = valueProviders;
        }

        final Object[] getParamValues(ContainerRequest request) {
            return ParameterValueHelper.getParameterValues(valueProviders, request);
        }
    }

    private static final class SseEventSinkInvoker extends AbstractMethodParamInvoker {

        private final int parameterIndex;

        SseEventSinkInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator,
                final int parameterIndex) {
            super(resourceMethod, handler, valueProviders, validator);
            this.parameterIndex = parameterIndex;
        }

        @Override
        protected Response doDispatch(final Object resource, final ContainerRequest request) throws ProcessingException {
            final Object[] paramValues = getParamValues(request);
            invoke(request, resource, paramValues);

            final SseEventSink eventSink = (SseEventSink) paramValues[parameterIndex];

            if (eventSink == null) {
                throw new IllegalArgumentException("SseEventSink parameter detected, but not found.");
            }
            return Response.ok().entity(eventSink).build();
        }
    }

    private static final class VoidOutInvoker extends AbstractMethodParamInvoker {

        VoidOutInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, valueProviders, validator);
        }

        @Override
        protected Response doDispatch(final Object resource, final ContainerRequest containerRequest) throws ProcessingException {
            invoke(containerRequest, resource, getParamValues(containerRequest));
            return Response.noContent().build();
        }
    }

    private static final class ResponseOutInvoker extends AbstractMethodParamInvoker {

        ResponseOutInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, valueProviders, validator);
        }

        @Override
        protected Response doDispatch(Object resource, final ContainerRequest containerRequest) throws ProcessingException {
            return Response.class.cast(invoke(containerRequest, resource, getParamValues(containerRequest)));
        }
    }

    private static final class ObjectOutInvoker extends AbstractMethodParamInvoker {

        ObjectOutInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, valueProviders, validator);
        }

        @Override
        protected Response doDispatch(final Object resource, final ContainerRequest containerRequest) throws ProcessingException {
            final Object o = invoke(containerRequest, resource, getParamValues(containerRequest));

            if (o instanceof Response) {
                return Response.class.cast(o);
            } else if (o != null) {
                return Response.ok().entity(o).build();
            } else {
                return Response.noContent().build();
            }
        }
    }

    private static final class TypeOutInvoker extends AbstractMethodParamInvoker {

        private final Type t;

        TypeOutInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, valueProviders, validator);
            this.t = resourceMethod.getHandlingMethod().getGenericReturnType();
        }

        @Override
        protected Response doDispatch(final Object resource, final ContainerRequest containerRequest) throws ProcessingException {
            final Object o = invoke(containerRequest, resource, getParamValues(containerRequest));
            if (o != null) {
                if (o instanceof Response) {
                    return Response.class.cast(o);
                }
                return Response.ok().entity(o).build();
            } else {
                return Response.noContent().build();
            }
        }
    }
}
