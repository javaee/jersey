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

package org.glassfish.jersey.client.rx.rxjava;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.client.AbstractRxInvoker;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

/**
 * Implementation of Reactive Invoker for {@code Observable}. If no executor service is provided the JAX-RS Async client is used
 * to retrieve data when a subscriber is subscribed. When an executor service is provided a sync call is invoked on a thread
 * provided on from this service.
 *
 * @author Michal Gajdos
 * @since 2.13
 */
final class JerseyRxObservableInvoker extends AbstractRxInvoker<Observable> implements RxObservableInvoker {

    JerseyRxObservableInvoker(final SyncInvoker syncInvoker, final ExecutorService executor) {
        super(syncInvoker, executor);
    }

    @Override
    public <T> Observable<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
        return method(name, entity, new GenericType<T>(responseType) { });
    }

    @Override
    public <T> Observable<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
        final Scheduler scheduler;

        if (getExecutorService() != null) {
            scheduler = Schedulers.from(getExecutorService());
        } else {
            // TODO: use JAX-RS client scheduler
            // TODO: https://java.net/jira/browse/JAX_RS_SPEC-523
            scheduler = Schedulers.io();
        }

        // Invoke as sync JAX-RS client request and subscribe/observe on a scheduler initialized with executor service.
        return Observable.create((Observable.OnSubscribe<T>) subscriber -> {
            if (!subscriber.isUnsubscribed()) {
                try {
                    final T response = getSyncInvoker().method(name, entity, responseType);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(response);
                    }
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                } catch (final Throwable throwable) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(throwable);
                    }
                }
            }
        }).subscribeOn(scheduler).observeOn(scheduler);
    }
}
