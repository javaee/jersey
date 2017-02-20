/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.rx.Rx;
import org.glassfish.jersey.client.rx.RxClient;
import org.glassfish.jersey.client.rx.RxWebTarget;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Main entry point to the Reactive Client API used to bootstrap {@link org.glassfish.jersey.client.rx.RxClient reactive client}
 * or {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instances based on Project Reactor's
 * {@link Mono}.
 *
 * @author Adam Richeimer
 * @see org.glassfish.jersey.client.rx.Rx
 */
public final class RxReactor {

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance parameterized with invoker based on
     * the {@link Mono} from Project Reactor.
     * <p/>
     * Instance is initialized with a JAX-RS client created using the default client builder implementation class provided by the
     * JAX-RS implementation provider.
     *
     * @return new reactive client extension.
     * @see Rx#newClient(Class)
     */
    public static RxClient<RxReactorInvoker> newClient() {
        return Rx.newClient(RxReactorInvoker.class);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance parameterized with invoker based on
     * the {@link Mono} from Project Reactor. Reactive requests, invoked using
     * {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)}, operate on a {@link Scheduler} initialized with
     * provided {@link java.util.concurrent.ExecutorService executor service}.
     * <p/>
     * Instance is initialized with a JAX-RS client created using the default client builder implementation class provided by the
     * JAX-RS implementation provider.
     *
     * @param executorService the executor service to execute reactive requests.
     * @return new reactive client extension.
     * @see Rx#newClient(Class)
     */
    public static RxClient<RxReactorInvoker> newClient(final ExecutorService executorService) {
        return Rx.newClient(RxReactorInvoker.class, executorService);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance initialized with given JAX-RS client
     * instance and parameterized with invoker based on the {@link Mono} from Project Reactor.
     *
     * @param client the JAX-RS client used to initialize new reactive client extension.
     * @return new reactive client extension.
     * @see Rx#from(javax.ws.rs.client.Client, Class)
     */
    public static RxClient<RxReactorInvoker> from(final Client client) {
        return Rx.from(client, RxReactorInvoker.class);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxClient reactive client} instance initialized with given JAX-RS client
     * instance and parameterized with invoker based on the {@link Mono} from Project Reactor. Reactive requests,
     * invoked using {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)}, operate on a {@link Scheduler}
     * initialized with provided {@link java.util.concurrent.ExecutorService executor service}.
     *
     * @param client the JAX-RS client used to initialize new reactive client extension.
     * @param executorService the executor service to execute reactive requests.
     * @return new reactive client extension.
     * @see Rx#from(javax.ws.rs.client.Client, Class)
     */
    public static RxClient<RxReactorInvoker> from(final Client client, final ExecutorService executorService) {
        return Rx.from(client, RxReactorInvoker.class, executorService);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instance initialized with given
     * JAX-RS client web target instance and parameterized with invoker based on the {@link Mono} from Project Reactor.
     *
     * @param target the JAX-RS client target used to initialize new reactive client target extension.
     * @return new reactive client target extension.
     * @see Rx#from(javax.ws.rs.client.WebTarget, Class)
     */
    public static RxWebTarget<RxReactorInvoker> from(final WebTarget target) {
        return Rx.from(target, RxReactorInvoker.class);
    }

    /**
     * Create a new {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target} instance initialized with given
     * JAX-RS client web target instance and parameterized with invoker based on the {@link Mono} from Project Reactor.
     * Reactive requests, invoked using {@link org.glassfish.jersey.client.rx.RxInvocationBuilder#rx() rx(...)}, operate on a
     * {@link Scheduler} initialized with provided {@link java.util.concurrent.ExecutorService executor service}.
     *
     * @param target the JAX-RS client target used to initialize new reactive client target extension.
     * @param executorService the executor service to execute reactive requests.
     * @return new reactive client target extension.
     * @see Rx#from(javax.ws.rs.client.WebTarget, Class)
     */
    public static RxWebTarget<RxReactorInvoker> from(final WebTarget target, final ExecutorService executorService) {
        return Rx.from(target, RxReactorInvoker.class, executorService);
    }

    /**
     * Prevent instantiation.
     */
    private RxReactor() {
        throw new AssertionError("No instances allowed.");
    }
}
