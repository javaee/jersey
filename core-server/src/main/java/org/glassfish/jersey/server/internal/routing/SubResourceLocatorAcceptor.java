/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.server.model.IntrospectionModeller;
import org.glassfish.jersey.server.model.ResourceClass;
import org.glassfish.jersey.server.model.SubResourceLocator;
import org.glassfish.jersey.server.spi.internal.MethodParameterHelper;

/**
 * An acceptor to accept sub-resource requests.
 * It first retrieves the sub-resource instance by invoking the given locator method.
 * Then the {@link RuntimeModelFromSubResource} is used to generate corresponding acceptor.
 * Finally the generated acceptor is invoked to return the request acceptor chain.
 *
 * TODO: implement generated sub-resource acceptor caching
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class SubResourceLocatorAcceptor implements TreeAcceptor {

    Services services;
    SubResourceLocator locator;
    final List<Factory<?>> valueProviders;
    Injector injector;
    MessageBodyWorkers workers;

    public SubResourceLocatorAcceptor(Injector injector, Services services, MessageBodyWorkers workers, SubResourceLocator locator) {
        this.injector = injector;
        this.locator = locator;
        this.workers = workers;
        this.services = services;
        valueProviders = MethodParameterHelper.createValueProviders(services, locator);
    }

    @Override
    public Pair<Request, Iterator<TreeAcceptor>> apply(Request data) {
        final RoutingContext routingCtx = injector.inject(RoutingContext.class);
        Object subResource = getResource(routingCtx);
        if (subResource == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        if (subResource.getClass().isAssignableFrom(Class.class)) {
            subResource = services.forContract((Class)subResource).get();
        }
        final RuntimeModelFromSubResource rmBuilder = new RuntimeModelFromSubResource(workers);
        injector.inject(rmBuilder);
        routingCtx.pushMatchedResource(subResource);
        routingCtx.pushLeftHandPath();
        final ResourceClass resourceClass = IntrospectionModeller.createResource(subResource.getClass());
        rmBuilder.process(resourceClass);
        return rmBuilder.getRuntimeModel().apply(data);
    }

    private Object getResource(RoutingContext routingCtx) {
        final Object resource = routingCtx.peekMatchedResource();
        try {
            return locator.getMethod().invoke(resource, MethodParameterHelper.getParameterValues(valueProviders));
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
