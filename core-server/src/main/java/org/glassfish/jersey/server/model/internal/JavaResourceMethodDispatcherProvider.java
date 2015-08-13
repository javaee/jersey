/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import javax.inject.Inject;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.internal.ParamValueFactoryWithSource;
import org.glassfish.jersey.server.spi.internal.ParameterValueHelper;
import org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * An implementation of {@link ResourceMethodDispatcher.Provider} that
 * creates instances of {@link ResourceMethodDispatcher}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class JavaResourceMethodDispatcherProvider implements ResourceMethodDispatcher.Provider {

    @Inject
    private ServiceLocator serviceLocator;

    @Override
    public ResourceMethodDispatcher create(final Invocable resourceMethod,
            final InvocationHandler invocationHandler,
            final ConfiguredValidator validator) {
        final List<ParamValueFactoryWithSource<?>> valueProviders =
                ParameterValueHelper.createValueProviders(serviceLocator, resourceMethod);
        final Class<?> returnType = resourceMethod.getHandlingMethod().getReturnType();

        ResourceMethodDispatcher resourceMethodDispatcher;
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
            resourceMethodDispatcher
                    = new VoidOutInvoker(resourceMethod, invocationHandler, valueProviders, validator);
        }

        // Inject validator.
        serviceLocator.inject(resourceMethodDispatcher);

        return resourceMethodDispatcher;
    }

    /**
     * Get the application-configured HK2 service locator.
     *
     * @return application-configured HK2 service locator.
     */
    final ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    private abstract static class AbstractMethodParamInvoker extends AbstractJavaResourceMethodDispatcher {

        private final List<ParamValueFactoryWithSource<?>> valueProviders;

        public AbstractMethodParamInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, validator);
            this.valueProviders = valueProviders;
        }

        final Object[] getParamValues() {
            return ParameterValueHelper.getParameterValues(valueProviders);
        }
    }

    private static final class VoidOutInvoker extends AbstractMethodParamInvoker {

        public VoidOutInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, valueProviders, validator);
        }

        @Override
        protected Response doDispatch(final Object resource, final ContainerRequest containerRequest) throws ProcessingException {
            invoke(containerRequest, resource, getParamValues());
            return Response.noContent().build();
        }
    }

    private static final class ResponseOutInvoker extends AbstractMethodParamInvoker {

        public ResponseOutInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, valueProviders, validator);
        }

        @Override
        protected Response doDispatch(Object resource, final ContainerRequest containerRequest) throws ProcessingException {
            return Response.class.cast(invoke(containerRequest, resource, getParamValues()));
        }
    }

    private static final class ObjectOutInvoker extends AbstractMethodParamInvoker {

        public ObjectOutInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, valueProviders, validator);
        }

        @Override
        protected Response doDispatch(final Object resource, final ContainerRequest containerRequest) throws ProcessingException {
            final Object o = invoke(containerRequest, resource, getParamValues());

            if (o instanceof Response) {
                return Response.class.cast(o);
//            } else if (o instanceof JResponse) {
//                context.getResponseContext().setResponse(((JResponse)o).toResponse());
            } else if (o != null) {
                return Response.ok().entity(o).build();
            } else {
                return Response.noContent().build();
            }
        }
    }

    private static final class TypeOutInvoker extends AbstractMethodParamInvoker {

        private final Type t;

        public TypeOutInvoker(
                final Invocable resourceMethod,
                final InvocationHandler handler,
                final List<ParamValueFactoryWithSource<?>> valueProviders,
                final ConfiguredValidator validator) {
            super(resourceMethod, handler, valueProviders, validator);
            this.t = resourceMethod.getHandlingMethod().getGenericReturnType();
        }

        @Override
        protected Response doDispatch(final Object resource, final ContainerRequest containerRequest) throws ProcessingException {
            final Object o = invoke(containerRequest, resource, getParamValues());
            if (o != null) {

                Response response = Response.ok().entity(o).build();
                // TODO set the method return Java type to the proper context.
//                Response r = new ResponseBuilderImpl().
//                        entityWithType(o, t).status(200).build();
                return response;
            } else {
                return Response.noContent().build();
            }
        }
    }
}
