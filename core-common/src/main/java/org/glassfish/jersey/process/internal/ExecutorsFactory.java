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
package org.glassfish.jersey.process.internal;

import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.spi.RequestExecutorsProvider;
import org.glassfish.jersey.spi.ResponseExecutorsProvider;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Aggregate {@link org.glassfish.jersey.spi.RequestExecutorsProvider request executors provider} and  {@link
 * org.glassfish.jersey.spi.ResponseExecutorsProvider response executors provider} used directly in the {@link RequestInvoker
 * request invoker} to get the pluggable processing executor services.
 *
 * @param <REQUEST> request data type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
@Singleton
public abstract class ExecutorsFactory<REQUEST> {
    private final ServiceLocator locator;

    private static final Logger LOGGER = Logger.getLogger(ExecutorsFactory.class.getName());

    /**
     * Creates new instance.
     *
     * @param locator Injected HK2 locator.
     */
    public ExecutorsFactory(ServiceLocator locator) {
        this.locator = locator;
    }

    /**
     * Convenience method for implementation classes which returns the {@link ExecutorService requesting executor} and logs the
     * details into the log. If there is any custom registered {@link RequestExecutorsProvider request executor provider} its
     * executor will be returned. Otherwise executor of the {@code defaultProvider} is returned. The result is logged.
     *
     * @param defaultProvider Default provider which must return not-null executor. This provider will be used if no custom {@link
     *                        RequestExecutorsProvider request executor provider} is found.
     * @return {@link ExecutorService Requesting executor}.
     */
    protected ExecutorService getInitialRequestingExecutor(RequestExecutorsProvider defaultProvider) {

        for (RequestExecutorsProvider provider : Providers.getAllProviders(locator,
                RequestExecutorsProvider.class)) {
            final ExecutorService requestingExecutor = provider.getRequestingExecutor();
            if (requestingExecutor != null) {
                LOGGER.config(LocalizationMessages.USING_CUSTOM_REQUEST_EXECUTOR(
                        requestingExecutor.getClass().getName(), provider.getClass().getName()));
                return requestingExecutor;
            }
        }

        final ExecutorService defaultExecutor = defaultProvider.getRequestingExecutor();
        LOGGER.config(LocalizationMessages.USING_DEFAULT_REQUEST_EXECUTOR(defaultExecutor));
        return defaultExecutor;
    }


    /**
     * Convenience method for implementation classes which returns the {@link ExecutorService responding executor} and logs
     * the details into the log. If there is any custom registered {@link ResponseExecutorsProvider response executor provider}
     * its executor will be returned. Otherwise executor of the {@code defaultProvider} is returned. The result is logged.
     *
     * @param defaultProvider Default provider which must return not-null executor. This provider will be used if no custom
     *                        {@link ResponseExecutorsProvider response executor provider} is found.
     * @return {@link ExecutorService Responding executor}.
     */
    protected ExecutorService getInitialRespondingExecutor(ResponseExecutorsProvider defaultProvider) {

        for (ResponseExecutorsProvider provider : Providers.getAllProviders(locator,
                ResponseExecutorsProvider.class)) {
            final ExecutorService respondingExecutor = provider.getRespondingExecutor();
            if (respondingExecutor != null) {
                LOGGER.config(LocalizationMessages.USING_CUSTOM_RESPONSE_EXECUTOR(
                        respondingExecutor.getClass().getName(), provider.getClass().getName()));
                return respondingExecutor;
            }
        }

        final ExecutorService defaultExecutor = defaultProvider.getRespondingExecutor();
        LOGGER.config(LocalizationMessages.USING_DEFAULT_RESPONSE_EXECUTOR(defaultExecutor));
        return defaultExecutor;
    }


    /**
     * Returns {@link ExecutorService request executor} for the given {@code Request}.
     * The implementation of the method could return different executor based on the given request data,
     * for example return different executor for asynchronous processing than for synchronous processing.
     *
     * @param request Request object.
     * @return Request executor which will be used for request processing of the given response.
     */
    public abstract ExecutorService getRequestingExecutor(REQUEST request);

    /**
     * Returns {@link ExecutorService response executor} for the given {@code Request}.
     * The implementation of the method could return different executor based on the given request data,
     * for example return different executor for asynchronous processing than for synchronous processing.
     *
     * @param request Request object.
     * @return Response executor which will be used for request processing of the given response.
     */
    public abstract ExecutorService getRespondingExecutor(REQUEST request);


}
