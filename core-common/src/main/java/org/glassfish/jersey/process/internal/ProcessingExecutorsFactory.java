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

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.spi.ProcessingExecutorsProvider;

import org.glassfish.hk2.Services;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * Aggregate {@link ProcessingExecutorsProvider processing executors provider}
 * used directly in the {@link RequestInvoker request invoker} to get the pluggable
 * processing executor services.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Scoped(Singleton.class)
class ProcessingExecutorsFactory implements ProcessingExecutorsProvider {

    private final ExecutorService requestingExecutor;
    private final ExecutorService respondingExecutor;

    ProcessingExecutorsFactory(@Inject Services services) {
        final Set<ProcessingExecutorsProvider> providers = Providers.getProviders(services, ProcessingExecutorsProvider.class);
        requestingExecutor = createRequestingExecutor(providers);
        respondingExecutor = createRespondingExecutor(providers);
    }

    private static ExecutorService createRequestingExecutor(final Set<ProcessingExecutorsProvider> providers) {
        for (ProcessingExecutorsProvider provider : providers) {
            ExecutorService es = provider.getRequestingExecutor();
            if (es != null) {
                return es;
            }
        }

        return MoreExecutors.sameThreadExecutor();
    }

    private static ExecutorService createRespondingExecutor(final Set<ProcessingExecutorsProvider> providers) {
        for (ProcessingExecutorsProvider provider : providers) {
            ExecutorService es = provider.getRespondingExecutor();
            if (es != null) {
                return es;
            }
        }

        return MoreExecutors.sameThreadExecutor();
    }

    // ProcessingExecutorsProvider
    @Override
    public ExecutorService getRequestingExecutor() {
        return requestingExecutor;
    }

    @Override
    public ExecutorService getRespondingExecutor() {
        return respondingExecutor;
    }
}
