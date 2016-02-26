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

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.spi.ThreadPoolExecutorProvider;

/**
 * Default {@link org.glassfish.jersey.spi.ExecutorServiceProvider} used on the client side for asynchronous request processing.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@ClientAsyncExecutor
class DefaultClientAsyncExecutorProvider extends ThreadPoolExecutorProvider {

    private static final Logger LOGGER = Logger.getLogger(DefaultClientAsyncExecutorProvider.class.getName());

    private final LazyValue<Integer> asyncThreadPoolSize;

    /**
     * Creates a new instance.
     *
     * @param poolSize size of the default executor thread pool (if used). Zero or negative values are ignored.
     *                 See also {@link org.glassfish.jersey.client.ClientProperties#ASYNC_THREADPOOL_SIZE}.
     */
    @Inject
    public DefaultClientAsyncExecutorProvider(@Named("ClientAsyncThreadPoolSize") final int poolSize) {
        super("jersey-client-async-executor");

        this.asyncThreadPoolSize = Values.lazy(new Value<Integer>() {
            @Override
            public Integer get() {
                if (poolSize <= 0) {
                    LOGGER.config(LocalizationMessages.IGNORED_ASYNC_THREADPOOL_SIZE(poolSize));
                    // using default
                    return Integer.MAX_VALUE;
                } else {
                    LOGGER.config(LocalizationMessages.USING_FIXED_ASYNC_THREADPOOL(poolSize));
                    return poolSize;
                }
            }
        });
    }

    @Override
    protected int getMaximumPoolSize() {
        return asyncThreadPoolSize.get();
    }

    @Override
    protected int getCorePoolSize() {
        // Mimicking the Executors.newCachedThreadPool and newFixedThreadPool configuration values.
        final Integer maximumPoolSize = getMaximumPoolSize();
        if (maximumPoolSize != Integer.MAX_VALUE) {
            return maximumPoolSize;
        } else {
            return 0;
        }
    }
}
