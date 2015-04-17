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
package org.glassfish.jersey.server.spi.internal;

import java.lang.reflect.InvocationHandler;
import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.ValidationInterceptor;

/**
 * A resource method dispatcher responsible for consuming a JAX-RS {@link Request request}
 * by invoking the configured {@link Invocable resource method} on a given
 * resource instance and returning the method invocation result in a form of a
 * JAX-RS {@link Response response}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public interface ResourceMethodDispatcher {

    /**
     * Provider interface for creating a {@link ResourceMethodDispatcher resource
     * method dispatcher} instances.
     *
     * A provider examines the model of the Web resource method and
     * determines if an invoker can be created for that Web resource method.
     * <p>
     * Multiple providers can specify the support for different Web resource method
     * patterns, ranging from simple patterns (such as void return and input
     * parameters) to complex patterns that take type URI and query arguments
     * and HTTP request headers as typed parameters.
     * </p>
     * <p>
     * Resource method dispatcher provider implementations can be registered in Jersey application
     * by supplying a custom HK2 {@link org.glassfish.hk2.utilities.Binder} that binds the
     * custom service implementation(s) to the {@code ResourceMethodDispatcher.Provider} contract.
     * </p>
     *
     * @author Paul Sandoz
     * @author Marek Potociar (marek.potociar at oracle.com)
     */
    public static interface Provider {

        /**
         * Create a {@link ResourceMethodDispatcher resource method dispatcher} for
         * a given {@link Invocable invocable resource method}.
         * <p/>
         * If the provider supports the invocable resource method, it will
         * return a new non-null dispatcher instance configured to invoke the supplied
         * invocable resource method via the provided {@link InvocationHandler
         * invocation handler} whenever the
         * {@link #dispatch(Object, org.glassfish.jersey.server.ContainerRequest) dispatch(...)}
         * method is called on that dispatcher instance.
         *
         * @param method  the invocable resource method.
         * @param handler invocation handler to be used for the resource method invocation.
         * @param validator configured validator to be used for validation during resource method invocation
         * @return the resource method dispatcher, or {@code null} if it could not be
         *         created for the given resource method.
         */
        public ResourceMethodDispatcher create(final Invocable method,
                                               final InvocationHandler handler,
                                               final ConfiguredValidator validator);
    }

    /**
     * Reflectively dispatch a request to the underlying {@link Invocable
     * invocable resource method} via the configured {@link InvocationHandler
     * invocation handler} using the provided resource class instance.
     * <p />
     * In summary, the main job of the dispatcher is to convert a request into
     * an array of the Java method input parameters and subsequently convert the
     * returned response of an arbitrary Java type to a JAX-RS {@link Response response}
     * instance.
     * <p />
     * When the method is invoked, the dispatcher will extract the
     * {@link java.lang.reflect.Method Java method} information from the invocable
     * resource method and use the information to retrieve the required input
     * parameters from either the request instance or any other available run-time
     * information. Once the set of input parameter values is computed, the underlying
     * invocation handler instance is invoked to process (invoke) the Java resource
     * method with the computed input parameter values. The returned response is
     * subsequently converted into a JAX-RS {@code Response} type and returned
     * from the dispatcher.
     * <p />
     * It is assumed that the supplied resource implements the invocable method.
     * Dispatcher implementation should not need to do any additional checks in
     * that respect.
     *
     * @param resource the resource class instance.
     * @param request  request to be dispatched.
     * @return {@link Response response} for the dispatched request.
     * @throws ProcessingException (possibly {@link MappableException mappable})
     *                             container exception that will be handled by the Jersey server container.
     */
    public Response dispatch(final Object resource, final ContainerRequest request) throws ProcessingException;
}
