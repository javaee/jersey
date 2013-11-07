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
package org.glassfish.jersey.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.glassfish.jersey.process.internal.ExecutorsFactory;
import org.glassfish.jersey.spi.RequestExecutorsProvider;
import org.glassfish.jersey.spi.ResponseExecutorsProvider;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * {@link org.glassfish.jersey.process.internal.ExecutorsFactory Executors factory} used on the server side.
 *
 * The class returns the {@link java.util.concurrent.ExecutorService requesting executor} based on the request
 * data.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
class ServerExecutorsFactory extends ExecutorsFactory<ContainerRequest> {
    private final ExecutorService requestingExecutor;
    private final ExecutorService respondingExecutor;


    /**
     * Creates a new instance.
     *
     * @param locator HK2 service locator.
     */
    @Inject
    public ServerExecutorsFactory(ServiceLocator locator) {
        super(locator);
        this.requestingExecutor = getInitialRequestingExecutor(new RequestExecutorsProvider() {

            @Override
            public ExecutorService getRequestingExecutor() {
                return Executors.newCachedThreadPool(
                        new ThreadFactoryBuilder().setNameFormat("jersey-server-managed-async-executor-%d").build());
            }
        });
        this.respondingExecutor = getInitialRespondingExecutor(new ResponseExecutorsProvider() {

            @Override
            public ExecutorService getRespondingExecutor() {
                return MoreExecutors.sameThreadExecutor();
            }
        });
    }

    @Override
    public ExecutorService getRequestingExecutor(ContainerRequest request) {
        return requestingExecutor;
    }

    @Override
    public ExecutorService getRespondingExecutor(ContainerRequest request) {
        return respondingExecutor;
    }

    @Override
    public void shutdown() {
        requestingExecutor.shutdownNow();
        respondingExecutor.shutdownNow();
    }
}
