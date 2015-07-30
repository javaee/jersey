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

import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

/**
 * RxClient is the main entry point to the fluent API used to build and execute (reactive) client requests in order to consume
 * responses returned.
 * <p/>
 * The Reactive Client APIs are enhanced with {@link RxInvocationBuilder#rx() rx(...)} methods on client invocation builder. These
 * methods provide an entry point to invoke reactive requests. {@code RxClient} extension overrides methods of JAX-RS client to
 * give user a way to access the reactive contracts.
 *
 * @param <RX> the concrete reactive invocation type.
 * @author Michal Gajdos
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see javax.ws.rs.client.Client Client
 * @see org.glassfish.jersey.client.rx.RxInvoker RxInvoker
 * @since 2.13
 */
public interface RxClient<RX extends RxInvoker> extends Client {

    @Override
    public RxWebTarget<RX> target(String uri);

    @Override
    public RxWebTarget<RX> target(URI uri);

    @Override
    public RxWebTarget<RX> target(UriBuilder uriBuilder);

    @Override
    public RxWebTarget<RX> target(Link link);

    @Override
    public RxInvocationBuilder<RX> invocation(Link link);

    @Override
    public RxClient<RX> property(String name, Object value);

    @Override
    public RxClient<RX> register(Class<?> componentClass);

    @Override
    public RxClient<RX> register(Class<?> componentClass, int priority);

    @Override
    public RxClient<RX> register(Class<?> componentClass, Class<?>... contracts);

    @Override
    public RxClient<RX> register(Class<?> componentClass, Map<Class<?>, Integer> contracts);

    @Override
    public RxClient<RX> register(Object component);

    @Override
    public RxClient<RX> register(Object component, int priority);

    @Override
    public RxClient<RX> register(Object component, Class<?>... contracts);

    @Override
    public RxClient<RX> register(Object component, Map<Class<?>, Integer> contracts);
}
