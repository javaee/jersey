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

package org.glassfish.jersey.client.rx;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/**
 * Main entry point to the Reactive Client API used to bootstrap {@link org.glassfish.jersey.client.rx.RxClient reactive client}
 * or {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instances.
 *
 * @author Michal Gajdos
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.13
 */
public final class Rx {

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance parametrized with given reactive
     * invocation type. Reactive requests, invoked using {@link RxInvocationBuilder#rx() rx(...)} methods, are executed on the
     * current thread by default.
     * <p/>
     * Instance is initialized with a JAX-RS client created using the default client builder implementation class provided by the
     * JAX-RS implementation provider.
     *
     * @param invokerType the concrete invoker contract.
     * @param <RX>        the concrete reactive invocation type.
     * @return new reactive client extension initialized with new client instance.
     */
    public static <RX extends RxInvoker> RxClient<RX> newClient(final Class<RX> invokerType) {
        return newClient(invokerType, null);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance parametrized with given reactive
     * invocation type. Reactive requests, invoked using {@link RxInvocationBuilder#rx() rx(...)} methods, are executed via given
     * {@link java.util.concurrent.ExecutorService executor service}.
     * <p/>
     * Instance is initialized with a JAX-RS client created using the default client builder implementation class provided by the
     * JAX-RS implementation provider.
     *
     * @param invokerType the concrete invoker contract.
     * @param executor    the executor service to execute reactive requests.
     * @param <RX>        the concrete reactive invocation type.
     * @return new reactive client extension initialized with new client instance.
     */
    public static <RX extends RxInvoker> RxClient<RX> newClient(final Class<RX> invokerType, final ExecutorService executor) {
        return from(ClientBuilder.newClient(), invokerType, executor);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance initialized with given JAX-RS client
     * instance and parametrized with provided reactive invocation type. Reactive requests, invoked using
     * {@link RxInvocationBuilder#rx() rx(...)} methods, are executed on the current thread by default.
     *
     * @param client      the JAX-RS client used to initialize new reactive client extension.
     * @param invokerType the concrete invoker contract.
     * @param <RX>        the concrete reactive invocation type.
     * @return new reactive client extension.
     */
    public static <RX extends RxInvoker> RxClient<RX> from(final Client client, final Class<RX> invokerType) {
        return from(client, invokerType, null);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance initialized with given JAX-RS client
     * instance and parametrized with provided reactive invocation type. Reactive requests, invoked using
     * {@link RxInvocationBuilder#rx() rx(...)} methods, are executed via given
     * {@link java.util.concurrent.ExecutorService executor service}.
     *
     * @param client      the JAX-RS client used to initialize new reactive client extension.
     * @param invokerType the concrete invoker contract.
     * @param executor    the executor service to execute reactive requests.
     * @param <RX>        the concrete reactive invocation type.
     * @return new reactive client extension.
     */
    public static <RX extends RxInvoker> RxClient<RX> from(final Client client, final Class<RX> invokerType,
                                                           final ExecutorService executor) {
        return new JerseyRxClient<>(client, invokerType, executor);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instance initialized with given
     * JAX-RS client web target instance and parametrized with provided reactive invocation type. Reactive requests, invoked using
     * {@link RxInvocationBuilder#rx() rx(...)} methods, are executed on the current thread by default.
     *
     * @param target      the JAX-RS client target used to initialize new reactive client target extension.
     * @param invokerType the concrete invoker contract.
     * @param <RX>        the concrete reactive invocation type.
     * @return new reactive client target extension.
     */
    public static <RX extends RxInvoker> RxWebTarget<RX> from(final WebTarget target, final Class<RX> invokerType) {
        return from(target, invokerType, null);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instance initialized with given
     * JAX-RS client web target instance and parametrized with provided reactive invocation type. Reactive requests, invoked using
     * {@link RxInvocationBuilder#rx() rx(...)} methods, are executed via given
     * {@link java.util.concurrent.ExecutorService executor service}.
     *
     * @param target      the JAX-RS client target used to initialize new reactive client target extension.
     * @param invokerType the concrete invoker contract.
     * @param executor    the executor service to execute reactive requests.
     * @param <RX>        the concrete reactive invocation type.
     * @return new reactive client target extension.
     */
    public static <RX extends RxInvoker> RxWebTarget<RX> from(final WebTarget target, final Class<RX> invokerType,
                                                              final ExecutorService executor) {
        return new JerseyRxWebTarget<>(target, invokerType, executor);
    }

    /**
     * Prevent instantiation.
     */
    private Rx() {
        throw new AssertionError("No instances allowed.");
    }
}



