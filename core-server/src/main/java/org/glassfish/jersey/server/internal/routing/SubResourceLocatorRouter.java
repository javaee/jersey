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
package org.glassfish.jersey.server.internal.routing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedAction;
import java.util.List;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.SubjectSecurityContext;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.spi.internal.ParamValueFactoryWithSource;
import org.glassfish.jersey.server.spi.internal.ParameterValueHelper;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * An methodAcceptorPair to accept sub-resource requests.
 * It first retrieves the sub-resource instance by invoking the given model method.
 * Then the {@link RuntimeLocatorModelBuilder} is used to generate corresponding methodAcceptorPair.
 * Finally the generated methodAcceptorPair is invoked to return the request methodAcceptorPair chain.
 * <p/>
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Michal Gajdos
 * @author Miroslav Fuksa
 */
final class SubResourceLocatorRouter implements Router {

    private final ResourceMethod locatorModel;
    private final List<ParamValueFactoryWithSource<?>> valueProviders;
    private final RuntimeLocatorModelBuilder runtimeLocatorBuilder;
    private final JerseyResourceContext resourceContext;

    private final ServiceLocator locator;

    /**
     * Create a new sub-resource locator router.
     *
     * @param locator                 HK2 locator.
     * @param locatorModel            resource locator method model.
     * @param resourceContext         resource context to bind sub-resource locator singleton instances.
     * @param runtimeLocatorBuilder   original runtime model builder.
     */
    SubResourceLocatorRouter(final ServiceLocator locator,
                             final ResourceMethod locatorModel,
                             final JerseyResourceContext resourceContext,
                             final RuntimeLocatorModelBuilder runtimeLocatorBuilder) {
        this.runtimeLocatorBuilder = runtimeLocatorBuilder;
        this.locatorModel = locatorModel;
        this.resourceContext = resourceContext;
        this.locator = locator;

        this.valueProviders = ParameterValueHelper.createValueProviders(locator, locatorModel.getInvocable());
    }

    @Override
    public Continuation apply(final RequestProcessingContext processingContext) {
        Object subResourceInstance = getResource(processingContext);

        if (subResourceInstance == null) {
            throw new NotFoundException();
        }

        final RoutingContext routingContext = processingContext.routingContext();

        final LocatorRouting routing;
        if (subResourceInstance instanceof Resource) {
            // Caching here is disabled by default. It can be enabled by setting
            // ServerProperties.SUBRESOURCE_LOCATOR_CACHE_JERSEY_RESOURCE_ENABLED to true.
            routing = runtimeLocatorBuilder.getRouting((Resource) subResourceInstance);
        } else {
            Class<?> locatorClass = subResourceInstance.getClass();

            if (locatorClass.isAssignableFrom(Class.class)) {
                // subResourceInstance is class itself
                locatorClass = (Class<?>) subResourceInstance;

                if (!runtimeLocatorBuilder.isCached(locatorClass)) {
                    // If we can't create an instance of the class, don't proceed.
                    subResourceInstance = Injections.getOrCreate(locator, locatorClass);
                }
            }
            routingContext.pushMatchedResource(subResourceInstance);
            resourceContext.bindResourceIfSingleton(subResourceInstance);

            routing = runtimeLocatorBuilder.getRouting(locatorClass);
        }

        routingContext.pushLocatorSubResource(routing.locator.getResources().get(0));
        processingContext.triggerEvent(RequestEvent.Type.SUBRESOURCE_LOCATED);

        return Continuation.of(processingContext, routing.router);
    }

    private Object getResource(final RequestProcessingContext context) {
        final Object resource = context.routingContext().peekMatchedResource();
        final Method handlingMethod = locatorModel.getInvocable().getHandlingMethod();
        final Object[] parameterValues = ParameterValueHelper.getParameterValues(valueProviders);

        context.triggerEvent(RequestEvent.Type.LOCATOR_MATCHED);

        final PrivilegedAction invokeMethodAction = new PrivilegedAction() {
            @Override
            public Object run() {
                try {

                    return handlingMethod.invoke(resource, parameterValues);

                } catch (IllegalAccessException | IllegalArgumentException | UndeclaredThrowableException ex) {
                    throw new ProcessingException(LocalizationMessages.ERROR_RESOURCE_JAVA_METHOD_INVOCATION(), ex);
                } catch (final InvocationTargetException ex) {
                    final Throwable cause = ex.getCause();
                    if (cause instanceof WebApplicationException) {
                        throw (WebApplicationException) cause;
                    }

                    // handle all exceptions as potentially mappable (incl. ProcessingException)
                    throw new MappableException(cause);
                } catch (final Throwable t) {
                    throw new ProcessingException(t);
                }
            }
        };

        final SecurityContext securityContext = context.request().getSecurityContext();
        return (securityContext instanceof SubjectSecurityContext)
                ? ((SubjectSecurityContext) securityContext).doAsSubject(invokeMethodAction) : invokeMethodAction.run();

    }
}
