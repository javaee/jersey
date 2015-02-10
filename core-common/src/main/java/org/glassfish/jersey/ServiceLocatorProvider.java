/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey;

import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.ServiceLocatorSupplier;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Utility class with static methods that extract {@link org.glassfish.hk2.api.ServiceLocator service locator}
 * from various JAX-RS components. This class can be used when no injection is possible by
 * {@link javax.ws.rs.core.Context} or {@link javax.inject.Inject} annotation due to character of
 * provider but there is a need to get any service from {@link org.glassfish.hk2.api.ServiceLocator}.
 * <p>
 * Injections are not possible for example when a provider is registered as an instance on the client.
 * In this case the runtime will not inject the instance as this instance might be used in other client
 * runtimes too.
 * </p>
 * <p>
 * Example. This is the class using a standard injection:
 * <pre>
 *     public static class MyWriterInterceptor implements WriterInterceptor {
 *         &#64;Inject
 *         MyInjectedService service;
 *
 *         &#64;Override
 *         public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
 *             Something something = service.getSomething();
 *             ...
 *         }
 *
 *     }
 * </pre>
 * </p>
 * <p>
 * If this injection is not possible then this construct can be used:
 * <pre>
 *     public static class MyWriterInterceptor implements WriterInterceptor {
 *
 *         &#64;Override
 *         public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
 *             final ServiceLocator serviceLocator = ServiceLocatorProvider.getServiceLocator(context);
 *             final MyInjectedService service = serviceLocator.getService(MyInjectedService.class);
 *             Something something = service.getSomething();
 *             ...
 *         }
 *     }
 * </pre>
 * </p>
 * <p>
 * Note, that this injection support is intended mostly for injection of custom user types. JAX-RS types
 * are usually available without need for injections in method parameters. However, when injection of custom
 * type is needed it is preferred to use standard injections if it is possible rather than injection support
 * provided by this class.
 * </p>
 * <p>
 * User returned {@code ServiceLocator} only for purpose of getting services (do not change the state of the locator).
 * </p>
 *
 *
 * @author Miroslav Fuksa
 *
 * @since 2.6
 */
public class ServiceLocatorProvider {

    /**
     * Extract and return service locator from {@link javax.ws.rs.ext.WriterInterceptorContext writerInterceptorContext}.
     * The method can be used to inject custom types into a {@link javax.ws.rs.ext.WriterInterceptor}.
     *
     * @param writerInterceptorContext Writer interceptor context.
     *
     * @return Service locator.
     *
     * @throws java.lang.IllegalArgumentException when {@code writerInterceptorContext} is not a default
     * Jersey implementation provided by Jersey as argument in the
     * {@link javax.ws.rs.ext.WriterInterceptor#aroundWriteTo(javax.ws.rs.ext.WriterInterceptorContext)} method.
     */
    public static ServiceLocator getServiceLocator(WriterInterceptorContext writerInterceptorContext) {
        if (!(writerInterceptorContext instanceof ServiceLocatorSupplier)) {
            throw new IllegalArgumentException(
                    LocalizationMessages.ERROR_SERVICE_LOCATOR_PROVIDER_INSTANCE_FEATURE_WRITER_INTERCEPTOR_CONTEXT(
                            writerInterceptorContext.getClass().getName()));
        }
        return ((ServiceLocatorSupplier) writerInterceptorContext).getServiceLocator();
    }

    /**
     * Extract and return service locator from {@link javax.ws.rs.ext.ReaderInterceptorContext readerInterceptorContext}.
     * The method can be used to inject custom types into a {@link javax.ws.rs.ext.ReaderInterceptor}.
     *
     * @param readerInterceptorContext Reader interceptor context.
     *
     * @return Service locator.
     *
     * @throws java.lang.IllegalArgumentException when {@code readerInterceptorContext} is not a default
     * Jersey implementation provided by Jersey as argument in the
     * {@link javax.ws.rs.ext.ReaderInterceptor#aroundReadFrom(javax.ws.rs.ext.ReaderInterceptorContext)} method.

     */
    public static ServiceLocator getServiceLocator(ReaderInterceptorContext readerInterceptorContext) {
        if (!(readerInterceptorContext instanceof ServiceLocatorSupplier)) {
            throw new IllegalArgumentException(
                    LocalizationMessages.ERROR_SERVICE_LOCATOR_PROVIDER_INSTANCE_FEATURE_READER_INTERCEPTOR_CONTEXT(
                            readerInterceptorContext.getClass().getName()));
        }
        return ((ServiceLocatorSupplier) readerInterceptorContext).getServiceLocator();
    }

    /**
     * Extract and return service locator from {@link javax.ws.rs.core.FeatureContext featureContext}.
     * The method can be used to inject custom types into a {@link javax.ws.rs.core.Feature}.
     * <p>
     * Note that features are utilized during initialization phase when not all providers are registered yet.
     * It is undefined which injections are already available in this phase.
     * </p>
     *
     * @param featureContext Feature context.
     *
     * @return Service locator.
     *
     * @throws java.lang.IllegalArgumentException when {@code writerInterceptorContext} is not a default
     * Jersey instance provided by Jersey
     * in {@link javax.ws.rs.core.Feature#configure(javax.ws.rs.core.FeatureContext)} method.
     */
    public static ServiceLocator getServiceLocator(FeatureContext featureContext) {
        if (!(featureContext instanceof ServiceLocatorSupplier)) {
            throw new IllegalArgumentException(
                    LocalizationMessages.ERROR_SERVICE_LOCATOR_PROVIDER_INSTANCE_FEATURE_CONTEXT(
                            featureContext.getClass().getName()));
        }
        return ((ServiceLocatorSupplier) featureContext).getServiceLocator();
    }
}
