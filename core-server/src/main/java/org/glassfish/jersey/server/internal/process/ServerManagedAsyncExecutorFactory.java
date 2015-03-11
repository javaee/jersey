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

package org.glassfish.jersey.server.internal.process;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;
import org.glassfish.jersey.process.internal.RequestExecutorFactory;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.RequestExecutorProvider;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * {@link org.glassfish.jersey.process.internal.RequestExecutorFactory Executors factory}
 * used on the server side for managed asynchronous request invocations.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class ServerManagedAsyncExecutorFactory extends RequestExecutorFactory implements ContainerLifecycleListener {

    /**
     * Creates a new instance.
     *
     * @param locator HK2 service locator.
     */
    @Inject
    public ServerManagedAsyncExecutorFactory(ServiceLocator locator) {
        super(locator);
    }

    @Override
    protected RequestExecutorProvider getDefaultProvider(Object... initArgs) {
        return new RequestExecutorProvider() {

            @Override
            public ExecutorService getRequestingExecutor() {
                return Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                        .setNameFormat("jersey-server-managed-async-executor-%d")
                        .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                        .build());
            }

            @Override
            public void releaseRequestingExecutor(ExecutorService executor) {
                executor.shutdownNow();
            }
        };
    }

    @Override
    public void onStartup(Container container) {
        // do nothing
    }

    @Override
    public void onReload(Container container) {
        // do nothing
    }

    @Override
    public void onShutdown(Container container) {
        close();
    }
}
