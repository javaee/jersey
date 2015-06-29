/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.spi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PreDestroy;

/**
 * Default implementation of the Jersey {@link org.glassfish.jersey.spi.ScheduledExecutorServiceProvider
 * scheduled executor service provider SPI}.
 * <p>
 * This provider creates and provisions a shared {@link java.util.concurrent.ScheduledThreadPoolExecutor} instance
 * using the customizable {@link #getCorePoolSize() core threads}, {@link #getBackingThreadFactory() backing thread factory}
 * and {@link #getRejectedExecutionHandler() rejected task handler} values. Subclasses may override the respective methods
 * to customize the parameters of the provisioned scheduler.
 * </p>
 * <p>
 * When Jersey runtime no longer requires the use of a provided executor service instance, it invokes the provider's
 * {@link #dispose} method to signal the provider that the executor service instance can be disposed of. In this method,
 * provider is free to implement the proper shut-down logic for the disposed executor service instance and perform other
 * necessary cleanup. Yet, some providers may wish to implement a shared executor service strategy. In such case,
 * it may not be desirable to shut down the released executor service in the {@link #dispose} method. Instead, to perform the
 * eventual shut-down procedure, the provider may either rely on an explicit invocation of it's specific clean-up method.
 * Since all Jersey providers operate in a <em>container</em> environment, a good clean-up strategy for a shared executor
 * service provider implementation is to expose a {@link javax.annotation.PreDestroy &#64;PreDestroy}-annotated method
 * that will be invoked for all instances managed by the container, before the container shuts down.
 * </p>
 * <p>
 * IMPORTANT: Please note that any pre-destroy methods may not be invoked for instances created outside of the container
 * and later registered within the container. Pre-destroy methods are only guaranteed to be invoked for those instances
 * that are created and managed by the container.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.18
 */
public class ScheduledThreadPoolExecutorProvider extends AbstractThreadPoolProvider<ScheduledThreadPoolExecutor>
        implements ScheduledExecutorServiceProvider {

    /**
     * Create a new instance of the scheduled thread pool executor provider.
     *
     * @param name provider name. The name will be used to name the threads created & used by the
     *             provisioned scheduled thread pool executor.
     */
    public ScheduledThreadPoolExecutorProvider(final String name) {
        super(name);
    }

    @Override
    public ScheduledExecutorService getExecutorService() {
        return super.getExecutor();
    }

    @Override
    protected ScheduledThreadPoolExecutor createExecutor(
            final int corePoolSize, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
        return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory, handler);
    }

    @Override
    public void dispose(final ExecutorService executorService) {
        // NO-OP.
    }

    /**
     * Container pre-destroy handler method.
     * <p>
     * Invoking the method {@link #close() closes} this provider.
     * </p>
     */
    @PreDestroy
    public void preDestroy() {
        close();
    }

}
