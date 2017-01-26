/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.rx.rxjava2;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.client.AbstractRxInvoker;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * Implementation of Reactive Invoker for {@code Flowable}. If no executor service is provided the JAX-RS Async client is used
 * to retrieve data when a subscriber is subscribed. When an executor service is provided a sync call is invoked on a thread
 * provided on from this service.
 *
 * @author Michal Gajdos
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @since 2.16
 */
final class JerseyRxFlowableInvoker extends AbstractRxInvoker<Flowable> implements RxFlowableInvoker {

    JerseyRxFlowableInvoker(SyncInvoker syncInvoker, ExecutorService executor) {
        super(syncInvoker, executor);
    }

    @Override
    public <T> Flowable<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
        return method(name, entity, new GenericType<T>(responseType) { });
    }

    @Override
    public <T> Flowable<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
        final Scheduler scheduler;

        if (getExecutorService() != null) {
            scheduler = Schedulers.from(getExecutorService());
        } else {
            // TODO: use JAX-RS client scheduler
            // TODO: https://java.net/jira/browse/JAX_RS_SPEC-523
            scheduler = Schedulers.io();
        }

        // Invoke as sync JAX-RS client request and subscribe/observe on a scheduler initialized with executor service.
        return Flowable.create(new FlowableOnSubscribe<T>() {
            @Override
            public void subscribe(FlowableEmitter<T> flowableEmitter) throws Exception {
                    try {
                        final T response = getSyncInvoker().method(name, entity, responseType);
                        flowableEmitter.onNext(response);
                        flowableEmitter.onComplete();
                    } catch (final Throwable throwable) {
                        flowableEmitter.onError(throwable);
                    }
            }
        }, BackpressureStrategy.DROP).subscribeOn(scheduler).observeOn(scheduler);
    }
}
