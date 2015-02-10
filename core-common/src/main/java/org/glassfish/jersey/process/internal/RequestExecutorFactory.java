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

package org.glassfish.jersey.process.internal;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.spi.RequestExecutorProvider;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * {@link org.glassfish.jersey.spi.RequestExecutorProvider Request executors provider} aggregator used by
 * client and server-side run-times to provide support for pluggable managed/async executor services.
 * <p>
 * Instances of {@code RequestExecutorFactory} use the configured request executors provider to
 * lazily retrieve and cache an instance of {@link java.util.concurrent.ExecutorService request executor service}
 * when {@link #getExecutor()} method is invoked. The retrieved executor service is then cached and returned
 * from the {@code getExecutor()} method on subsequent calls.
 * </p>
 * <p>
 * It is expected that the {@code ExecutorFactory}'s {@link #close()} method will be called on any
 * executor factory instance that is no longer use. Upon the call to {@code close()} method, the factory
 * will release the cached
 * </p>
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class RequestExecutorFactory {
    private static final Logger LOGGER = Logger.getLogger(RequestExecutorFactory.class.getName());
    private final RequestExecutorProvider executorProvider;
    private final LazyValue<ExecutorService> executorValue;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates new instance.
     *
     * @param locator Injected HK2 locator.
     * @param args    additional arguments that will be passed into the
     *                {@link #getDefaultProvider(Object...)} when/if invoked.
     */
    public RequestExecutorFactory(final ServiceLocator locator, Object... args) {
        final Iterator<RequestExecutorProvider> providersIterator =
                Providers.getAllProviders(locator, RequestExecutorProvider.class).iterator();
        if (providersIterator.hasNext()) {
            executorProvider = providersIterator.next();
            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.config(LocalizationMessages.USING_CUSTOM_REQUEST_EXECUTOR_PROVIDER(
                        executorProvider.getClass().getName()));

                if (providersIterator.hasNext()) {
                    StringBuilder msg = new StringBuilder(providersIterator.next().getClass().getName());
                    while (providersIterator.hasNext()) {
                        msg.append(", ").append(providersIterator.next().getClass().getName());
                    }
                    LOGGER.config(LocalizationMessages.IGNORED_CUSTOM_REQUEST_EXECUTOR_PROVIDERS(msg.toString()));
                }
            }
        } else {
            executorProvider = getDefaultProvider(args);
            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.config(LocalizationMessages.USING_DEFAULT_REQUEST_EXECUTOR_PROVIDER(
                        executorProvider.getClass().getName()));
            }
        }
        executorValue = Values.lazy(new Value<ExecutorService>() {
            @Override
            public ExecutorService get() {
                return executorProvider.getRequestingExecutor();
            }
        });
    }

    /**
     * Get the default request executor provider.
     * <p>
     * This method is invoked from the {@code RequestExecutorFactory} constructor if no custom
     * {@link org.glassfish.jersey.spi.RequestExecutorProvider} registration is found.
     * The returned default provider will be then used to provide the default request executor
     * implementation.
     * </p>
     * <p>
     * Concrete implementations of this class are expected to provide implementation of this
     * method. Note that since the method is used from the {@code RequestExecutorFactory} constructor,
     * the implementation of this method must not rely on initialization of any non-static fields
     * of the overriding sub-class. Instead, the necessary initialization arguments may be passed
     * via super constructor.
     * </p>
     *
     * @param initArgs initialization arguments passed via
     *                 {@link #RequestExecutorFactory(org.glassfish.hk2.api.ServiceLocator, Object...) constructor}
     *                 of this class.
     * @return default request executor provider to be used if no custom provider has been registered.
     */
    protected abstract RequestExecutorProvider getDefaultProvider(Object... initArgs);

    /**
     * Get the request processing executor using the underlying {@link RequestExecutorProvider} configured
     * in the executor factory instance.
     *
     * @return request processing executor. Must not return {@code null}.
     * @throws java.lang.IllegalStateException in case the factory instance has been {@link #close() closed}
     *                                         already.
     */
    public final ExecutorService getExecutor() {
        if (closed.get()) {
            throw new IllegalStateException(LocalizationMessages.REQUEST_EXECUTOR_FACTORY_CLOSED(this.getClass().getName()));
        }
        return executorValue.get();
    }

    /**
     * Close the executor factory and release any associated executor service resources.
     */
    public final void close() {
        if (closed.compareAndSet(false, true)) {
            if (executorValue.isInitialized()) {
                executorProvider.releaseRequestingExecutor(executorValue.get());
            }
        }
    }
}
