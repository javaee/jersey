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

package org.glassfish.jersey.client.rx.guava;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.rx.Rx;
import org.glassfish.jersey.client.rx.RxClient;
import org.glassfish.jersey.client.rx.RxWebTarget;

/**
 * Main entry point to the Reactive Client API used to bootstrap {@link org.glassfish.jersey.client.rx.RxClient reactive client}
 * or {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instances based on Guava's
 * {@link com.google.common.util.concurrent.ListenableFuture listenable future}.
 *
 * @author Michal Gajdos
 * @see org.glassfish.jersey.client.rx.Rx
 * @since 2.13
 */
public final class RxListenableFuture {

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance parametrized with invoker based on
     * the {@link com.google.common.util.concurrent.ListenableFuture listenable future} from Guava. Reactive requests,
     * invoked using {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)} methods, are executed via
     * {@link java.util.concurrent.Executors#newCachedThreadPool() "new thread pool" service}.
     * <p/>
     * Instance is initialized with a JAX-RS client created using the default client builder implementation class provided by the
     * JAX-RS implementation provider.
     *
     * @return new reactive client extension.
     * @see Rx#newClient(Class)
     */
    public static RxClient<RxListenableFutureInvoker> newClient() {
        return Rx.newClient(RxListenableFutureInvoker.class);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance parametrized with invoker based on
     * the {@link com.google.common.util.concurrent.ListenableFuture listenable future} from Guava. Reactive requests,
     * invoked using {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)} method, are executed via given
     * {@link java.util.concurrent.ExecutorService executor service}.
     * <p/>
     * Instance is initialized with a JAX-RS client created using the default client builder implementation class provided by the
     * JAX-RS implementation provider.
     *
     * @param executor the executor service to execute reactive requests.
     * @return new reactive client extension.
     * @see Rx#newClient(Class, java.util.concurrent.ExecutorService)
     */
    public static RxClient<RxListenableFutureInvoker> newClient(final ExecutorService executor) {
        return Rx.newClient(RxListenableFutureInvoker.class, executor);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance initialized with given JAX-RS client
     * instance and parametrized with invoker based on the {@link com.google.common.util.concurrent.ListenableFuture
     * listenable future} from Guava. Reactive requests, invoked using
     * {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)} methods, are executed via
     * {@link java.util.concurrent.Executors#newCachedThreadPool() "new thread pool" service}.
     *
     * @param client the JAX-RS client used to initialize new reactive client extension.
     * @return new reactive client extension.
     * @see Rx#from(javax.ws.rs.client.Client, Class)
     */
    public static RxClient<RxListenableFutureInvoker> from(final Client client) {
        return Rx.from(client, RxListenableFutureInvoker.class);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance initialized with given JAX-RS client
     * instance and parametrized with invoker based on the {@link com.google.common.util.concurrent.ListenableFuture
     * listenable future} from Guava. Reactive requests, invoked using
     * {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)} method, are executed via given
     * {@link java.util.concurrent.ExecutorService executor service}.
     *
     * @param client   the JAX-RS client used to initialize new reactive client extension.
     * @param executor the executor service to execute reactive requests.
     * @return new reactive client extension.
     * @see Rx#from(javax.ws.rs.client.Client, Class, java.util.concurrent.ExecutorService)
     */
    public static RxClient<RxListenableFutureInvoker> from(final Client client, final ExecutorService executor) {
        return Rx.from(client, RxListenableFutureInvoker.class, executor);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instance initialized with given
     * JAX-RS client web target instance and parametrized with invoker based on the
     * {@link com.google.common.util.concurrent.ListenableFuture listenable future} from Guava. Reactive requests, invoked using
     * {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)} methods, are executed via
     * {@link java.util.concurrent.Executors#newCachedThreadPool() "new thread pool" service}.
     *
     * @param target the JAX-RS client target used to initialize new reactive client target extension.
     * @return new reactive client target extension.
     * @see Rx#from(javax.ws.rs.client.WebTarget, Class)
     */
    public static RxWebTarget<RxListenableFutureInvoker> from(final WebTarget target) {
        return Rx.from(target, RxListenableFutureInvoker.class);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instance initialized with given
     * JAX-RS client web target instance and parametrized with invoker based on the
     * {@link com.google.common.util.concurrent.ListenableFuture listenable future} from Guava. Reactive requests, invoked using
     * {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)} method, are executed via given
     * {@link java.util.concurrent.ExecutorService executor service}.
     *
     * @param target   the JAX-RS client target used to initialize new reactive client target extension.
     * @param executor the executor service to execute reactive requests.
     * @return new reactive client target extension.
     * @see Rx#from(javax.ws.rs.client.WebTarget, Class, java.util.concurrent.ExecutorService)
     */
    public static RxWebTarget<RxListenableFutureInvoker> from(final WebTarget target, final ExecutorService executor) {
        return Rx.from(target, RxListenableFutureInvoker.class, executor);
    }

    /**
     * Prevent instantiation.
     */
    private RxListenableFuture() {
        throw new AssertionError("No instances allowed.");
    }
}
