/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.resilient.connector;

import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.resilient.connector.ResilientConnectorProvider.HystrixCommandConfigProvider;
import org.glassfish.jersey.resilient.connector.ResilientConnectorProvider.HystrixCommandFallback;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixObservableCommand;

import jersey.repackaged.com.google.common.util.concurrent.SettableFuture;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * <a href="https://github.com/Netflix/Hystrix">Hystrix</a> based resilient transport.
 * <p>
 * This connector delegates the transport invocations to the underlying connector
 * by wrapping it around {@link HystrixCommand} for sync requests and {@link HystrixObservableCommand}
 * for async requests.
 * </p>
 *
 * <p>
 * In case of failures, the connector will execute an instance of {@link HystrixCommandFallback} registered
 * in the context of the request. If no such instance is registered for the request, then it executes the default
 * behavior of Hystrix.
 * </p>
 *
 * @author Joel Chengottusseriyil
 */
class ResilientConnector implements Connector {

    private final Connector delegateConnector;

    private final HystrixCommandConfigProvider hystrixCommandConfigProvider;

    public ResilientConnector(Client client, Configuration runtimeConfig, Connector delegateConnector) {
        this.delegateConnector = delegateConnector;
        this.hystrixCommandConfigProvider = initCommandConfigProvider(runtimeConfig);
    }

    @Override
    public ClientResponse apply(final ClientRequest request) {
        com.netflix.hystrix.HystrixCommand.Setter setter = hystrixCommandConfigProvider.commandConfig(request);
        HystrixCommand<ClientResponse> command = new HystrixCommand<ClientResponse>(setter) {

            @Override
            protected ClientResponse run() throws Exception {
                return delegateConnector.apply(request);
            }

            @Override
            protected ClientResponse getFallback() {
                final HystrixCommandFallback commandFallback = resolveFallback(request);
                if (commandFallback != null) {
                    return commandFallback.execute(request);
                }
                // if no fallback is specified, then let the super class handle it
                return super.getFallback();
            }
        };
        return command.execute();
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        com.netflix.hystrix.HystrixObservableCommand.Setter setter =
                hystrixCommandConfigProvider.observableCommandConfig(request);
        HystrixObservableCommand<ClientResponse> observableCommand = new HystrixObservableCommand<ClientResponse>(setter) {

            @Override
            protected Observable<ClientResponse> construct() {
                return Observable.create(new OnSubscribe<ClientResponse>() {

                    @Override
                    public void call(Subscriber<? super ClientResponse> observer) {
                        delegateConnector.apply(request, new SubscriberAsyncConnectorCallback(observer));
                    }
                });
            }

            @Override
            protected Observable<ClientResponse> resumeWithFallback() {
                final HystrixCommandFallback commandFallback = resolveFallback(request);
                if (commandFallback != null) {
                    // Create an observable which when subscribed to executes the fallback.
                    return Observable.create(new OnSubscribe<ClientResponse>() {

                        @Override
                        public void call(Subscriber<? super ClientResponse> observer) {
                            try {
                                ClientResponse response = commandFallback.execute(request);
                                // notify the observer with fallback response
                                observer.onNext(response);
                                observer.onCompleted();
                            } catch (Throwable e) {
                                observer.onError(e);
                            }
                        }
                    });
                }
                // if no fallback is specified, then let the super class parent handle it
                return super.resumeWithFallback();
            }
        };
        final SettableFuture<ClientResponse> settableFuture = SettableFuture.create();
        OnNext onNext = new OnNext();
        OnError onError = new OnError(settableFuture, callback);
        OnComplete onComplete = new OnComplete(onNext, settableFuture, callback);

        // observe and subscribe with callbacks
        observableCommand.observe().subscribe(onNext, onError, onComplete);
        return settableFuture;
    }

    @Override
    public String getName() {
        return "Resilient_" + delegateConnector.getName();
    }

    @Override
    public void close() {
        delegateConnector.close();
    }

    /**
     * Resolves an instance of {@link HystrixCommandFallback} from the request context using the
     * {@link ResilientConnectorProvider#HYSTRIX_COMMAND_FALLBACK} property.
     *
     * @param requestContext Jersey client request for which the fallback property is being resolved.
     * @return an instance of {@code HystrixCommandFallback} or {@code null}.
     */
    private static HystrixCommandFallback resolveFallback(final ClientRequest requestContext) {
        return requestContext
                .resolveProperty(ResilientConnectorProvider.HYSTRIX_COMMAND_FALLBACK, HystrixCommandFallback.class);
    }

    /**
     * Initializes an instance of {@link HystrixCommandConfigProvider} using the
     * {@link ResilientConnectorProvider#HYSTRIX_COMMAND_CONFIG_PROVIDER} property from the runtime config.
     *
     * @param runtimeConfig runtime configuration of the client.
     * @return an instance of {@code HystrixCommandConfigProvider} or {@link DefaultHystrixCommandConfigProvider}.
     */
    private static HystrixCommandConfigProvider initCommandConfigProvider(Configuration runtimeConfig) {
        Object value = runtimeConfig.getProperty(ResilientConnectorProvider.HYSTRIX_COMMAND_CONFIG_PROVIDER);
        if (value != null) {
            return PropertiesHelper.convertValue(value, HystrixCommandConfigProvider.class);
        }
        return new DefaultHystrixCommandConfigProvider();
    }

    /**
     * {@link AsyncConnectorCallback} implementation that takes the subscriber of the Hystrix observable command,
     * and notifies the subscriber when the response is available.
     */
    private class SubscriberAsyncConnectorCallback implements AsyncConnectorCallback {

        private final Subscriber<? super ClientResponse> observer;

        SubscriberAsyncConnectorCallback(Subscriber<? super ClientResponse> observer) {
            this.observer = observer;
        }

        public void response(ClientResponse response) {
            observer.onNext(response);
            // invoke onComplete on the observer since we have the response
            observer.onCompleted();
        }

        public void failure(Throwable failure) {
            observer.onError(failure);
        }
    }

    /**
     * onNext callback function for the subscriber.
     */
    private static class OnNext implements Action1<ClientResponse> {

        private ClientResponse response;

        @Override
        public void call(ClientResponse response) {
            this.response = response;
        }

        ClientResponse get() {
            return this.response;
        }
    }

    /**
     * onError callback function for the subscriber.
     */
    private static class OnError implements Action1<Throwable> {

        private AsyncConnectorCallback callback;
        private SettableFuture<ClientResponse> settableFuture;

        OnError(SettableFuture<ClientResponse> settableFuture, AsyncConnectorCallback callback) {
            this.settableFuture = settableFuture;
            this.callback = callback;
        }

        @Override
        public void call(Throwable failure) {
            settableFuture.setException(failure);
            callback.failure(failure);
        }
    }

    /**
     * onComplete callback function for the subscriber.
     */
    private static class OnComplete implements Action0 {

        private OnNext onNext;
        private AsyncConnectorCallback callback;
        private SettableFuture<ClientResponse> settableFuture;

        OnComplete(OnNext onNext, SettableFuture<ClientResponse> settableFuture, AsyncConnectorCallback callback) {
            this.onNext = onNext;
            this.settableFuture = settableFuture;
            this.callback = callback;
        }

        @Override
        public void call() {
            settableFuture.set(onNext.get());
            callback.response(onNext.get());
        }
    }
}
