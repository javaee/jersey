/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.client.JerseyInvocation;
import org.glassfish.jersey.client.rx.spi.AbstractRxInvoker;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Implementation of Reactive Invoker for {@code Observable}. If no executor service is provided the JAX-RS Async client is used
 * to retrieve data when a subscriber is subscribed. When an executor service is provided a sync call is invoked on a thread
 * provided on from this service.
 *
 * @author Michal Gajdos
 * @since 2.13
 */
final class JerseyRxObservableInvoker extends AbstractRxInvoker<Observable> implements RxObservableInvoker {

    JerseyRxObservableInvoker(final Invocation.Builder builder, final ExecutorService executor) {
        super(builder, executor);
    }

    @Override
    public <T> Observable<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
        return method(name, entity, new GenericType<T>(responseType) {});
    }

    @Override
    public <T> Observable<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
        if (getExecutorService() == null) {
            // Invoke as async JAX-RS client request.
            return Observable.create(new Observable.OnSubscribe<T>() {
                @Override
                public void call(final Subscriber<? super T> subscriber) {
                    final CompositeSubscription parent = new CompositeSubscription();
                    subscriber.add(parent);

                    final JerseyInvocation invocation = (JerseyInvocation) getBuilder().build(name, entity);

                    // return a Subscription that wraps the Future to make sure it can be cancelled
                    parent.add(Subscriptions.from(invocation.submit(responseType, new InvocationCallback<T>() {
                        @Override
                        public void completed(final T entity) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(entity);
                            }
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onCompleted();
                            }
                        }

                        @Override
                        public void failed(final Throwable throwable) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(throwable);
                            }
                        }
                    })));
                }
            });
        } else {
            // Invoke as sync JAX-RS client request and subscribe/observe on a scheduler initialized with executor service.
            final Scheduler scheduler = Schedulers.from(getExecutorService());

            return Observable.create(new Observable.OnSubscribe<T>() {
                @Override
                public void call(final Subscriber<? super T> subscriber) {
                    if (!subscriber.isUnsubscribed()) {
                        try {
                            final T response = getBuilder().method(name, entity, responseType);

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
                }
            }).subscribeOn(scheduler).observeOn(scheduler);
        }
    }
}
