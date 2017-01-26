/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.rx.guava;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.client.AbstractRxInvoker;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Implementation of Reactive Invoker for {@code ListenableFuture}.
 *
 * @author Michal Gajdos
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @since 2.13
 */
final class JerseyRxListenableFutureInvoker extends AbstractRxInvoker<ListenableFuture> implements RxListenableFutureInvoker {

    private static final LazyValue<ListeningExecutorService> DEFAULT_EXECUTOR_SERVICE =
            Values.lazy(new Value<ListeningExecutorService>() {
                @Override
                public ListeningExecutorService get() {
                    return MoreExecutors.newDirectExecutorService();
                }
            });

    private final ListeningExecutorService service;

    JerseyRxListenableFutureInvoker(final SyncInvoker syncInvoker, final ExecutorService executor) {
        super(syncInvoker, executor);

        if (executor == null) {
            // TODO: use JAX-RS client scheduler
            // TODO: https://java.net/jira/browse/JAX_RS_SPEC-523
            service = DEFAULT_EXECUTOR_SERVICE.get();
        } else {
            service = MoreExecutors.listeningDecorator(executor);
        }
    }

    @Override
    public <T> ListenableFuture<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
        return service.submit(() -> getSyncInvoker().method(name, entity, responseType));
    }

    @Override
    public <T> ListenableFuture<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
        return service.submit(() -> getSyncInvoker().method(name, entity, responseType));
    }
}
