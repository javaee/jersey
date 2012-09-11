/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.routing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModelIssue;
import org.glassfish.jersey.server.spi.internal.ParameterValueHelper;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * An methodAcceptorPair to accept sub-resource requests.
 * It first retrieves the sub-resource instance by invoking the given model method.
 * Then the {@link RuntimeModelBuilder} is used to generate corresponding methodAcceptorPair.
 * Finally the generated methodAcceptorPair is invoked to return the request methodAcceptorPair chain.
 * <p/>
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
class SubResourceLocatorRouter implements Router {

    private final ServiceLocator locator;
    private final ResourceMethod locatorModel;
    private final List<Factory<?>> valueProviders;
    private final RuntimeModelBuilder runtimeModelBuilderOriginal;
    private final JerseyResourceContext resourceContext;

    /**
     * Create a new sub-resource locator router.
     *
     * @param locator     HK2 locator.
     * @param runtimeModelBuilderOriginal original runtime model builder.
     * @param locatorModel resource locator method model.
     */
    public SubResourceLocatorRouter(
            final ServiceLocator locator,
            final RuntimeModelBuilder runtimeModelBuilderOriginal,
            final ResourceMethod locatorModel) {
        this.locator = locator;
        this.runtimeModelBuilderOriginal = runtimeModelBuilderOriginal;
        this.locatorModel = locatorModel;
        this.valueProviders = ParameterValueHelper.createValueProviders(locator, locatorModel.getInvocable());
        this.resourceContext = locator.getService(JerseyResourceContext.class);
    }

    @Override
    public Continuation apply(final ContainerRequest request) {
        final RoutingContext routingCtx = Injections.getOrCreate(locator, RoutingContext.class);

        Object subResource = getResource(routingCtx);
        if (subResource == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        if (subResource.getClass().isAssignableFrom(Class.class)) {
            final Class<?> clazz = (Class<?>) subResource;
            resourceContext.bindResource(clazz);
            subResource = Injections.getOrCreate(locator, clazz);
        }
        resourceContext.bindResourceIfSingleton(subResource);

        final Resource subResourceModel;
        // TODO: what to do with the issues?
        subResourceModel = Resource.builder(subResource, new LinkedList<ResourceModelIssue>()).build();

        final RuntimeModelBuilder runtimeModelBuilder = runtimeModelBuilderOriginal.copy();
        runtimeModelBuilder.process(subResourceModel, true);

        // TODO: implement generated sub-resource methodAcceptorPair caching
        routingCtx.pushMatchedResource(subResource);
        Router subResourceAcceptor = runtimeModelBuilder.buildModel(true);
        return Continuation.of(request, subResourceAcceptor);
    }

    private Object getResource(RoutingContext routingCtx) {
        final Object resource = routingCtx.peekMatchedResource();
        try {
            Method handlingMethod = locatorModel.getInvocable().getHandlingMethod();
            return handlingMethod.invoke(resource, ParameterValueHelper.getParameterValues(valueProviders));
        } catch (IllegalAccessException ex) {
            throw new ProcessingException("Resource Java method invocation error.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof ProcessingException) {
                throw (ProcessingException) cause;
            }
            // exception cause potentially mappable
            throw new MappableException(cause);
        } catch (UndeclaredThrowableException ex) {
            throw new ProcessingException("Resource Java method invocation error.", ex);
        } catch (ProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            // exception potentially mappable
            throw new MappableException(ex);
        } catch (Throwable t) {
            throw new ProcessingException(t);
        }
    }
}
