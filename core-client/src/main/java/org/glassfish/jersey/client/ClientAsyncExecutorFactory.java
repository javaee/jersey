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

package org.glassfish.jersey.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;
import org.glassfish.jersey.process.internal.RequestExecutorFactory;
import org.glassfish.jersey.spi.RequestExecutorProvider;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * {@link org.glassfish.jersey.process.internal.RequestExecutorFactory Executors factory}
 * used on the client side for asynchronous request processing.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class ClientAsyncExecutorFactory extends RequestExecutorFactory {

    private static final Logger LOGGER = Logger.getLogger(ClientAsyncExecutorFactory.class.getName());

    /**
     * Creates a new instance.
     *
     * @param locator               HK2 service locator.
     * @param defaultThreadPoolSize size of the default executor thread pool (if used).
     *                              Zero or negative values are ignored and a
     *                              {@link java.util.concurrent.Executors#newCachedThreadPool() cached thread pool}
     *                              is created in such case instead.
     */
    public ClientAsyncExecutorFactory(final ServiceLocator locator, final int defaultThreadPoolSize) {
        super(locator, defaultThreadPoolSize);
    }

    @Override
    protected RequestExecutorProvider getDefaultProvider(final Object... initArgs) {

        return new RequestExecutorProvider() {

            @Override
            public ExecutorService getRequestingExecutor() {
                int poolSize = 0;
                if (initArgs != null && initArgs.length > 0 && initArgs[0] instanceof Integer) {
                    poolSize = (Integer) initArgs[0];
                    if (poolSize <= 0) {
                        LOGGER.config(LocalizationMessages.IGNORED_ASYNC_THREADPOOL_SIZE(poolSize));
                    }
                }

                final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                        .setNameFormat("jersey-client-async-executor-%d")
                        .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                        .build();

                if (poolSize > 0) {
                    LOGGER.config(LocalizationMessages.USING_FIXED_ASYNC_THREADPOOL(poolSize));
                    return Executors.newFixedThreadPool(poolSize, threadFactory);
                } else {
                    return Executors.newCachedThreadPool(threadFactory);
                }
            }

            @Override
            public void releaseRequestingExecutor(final ExecutorService executor) {
                executor.shutdownNow();
            }
        };
    }
}
