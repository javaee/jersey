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
import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * Jersey implementation of {@link org.glassfish.jersey.client.rx.RxClient reactive client}, extension of JAX-RS
 * {@link javax.ws.rs.client.Client client} contract.
 *
 * @param <RX> the concrete reactive invocation type.
 * @author Michal Gajdos
 */
final class JerseyRxClient<RX extends RxInvoker> implements RxClient<RX> {

    private final Class<RX> invokerType;
    private final ExecutorService executor;

    private Client client;

    JerseyRxClient(final Client client, final Class<RX> invokerType, final ExecutorService executor) {
        this.client = client;
        this.invokerType = invokerType;
        this.executor = executor;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public JerseyRxWebTarget<RX> target(final String uri) {
        return new JerseyRxWebTarget<>(client.target(uri), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> target(final URI uri) {
        return new JerseyRxWebTarget<>(client.target(uri), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> target(final UriBuilder uriBuilder) {
        return new JerseyRxWebTarget<>(client.target(uriBuilder), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> target(final Link link) {
        return new JerseyRxWebTarget<>(client.target(link), invokerType, executor);
    }

    @Override
    public JerseyRxInvocationBuilder<RX> invocation(final Link link) {
        return new JerseyRxInvocationBuilder<>(client.invocation(link), invokerType, executor);
    }

    @Override
    public SSLContext getSslContext() {
        return client.getSslContext();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return client.getHostnameVerifier();
    }

    @Override
    public Configuration getConfiguration() {
        return client.getConfiguration();
    }

    @Override
    public JerseyRxClient<RX> property(final String name, final Object value) {
        client = client.property(name, value);
        return this;
    }

    @Override
    public JerseyRxClient<RX> register(final Class<?> componentClass) {
        client = client.register(componentClass);
        return this;
    }

    @Override
    public JerseyRxClient<RX> register(final Class<?> componentClass, final int priority) {
        client = client.register(componentClass, priority);
        return this;
    }

    @Override
    public JerseyRxClient<RX> register(final Class<?> componentClass, final Class<?>... contracts) {
        client = client.register(componentClass, contracts);
        return this;
    }

    @Override
    public JerseyRxClient<RX> register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
        client = client.register(componentClass, contracts);
        return this;
    }

    @Override
    public JerseyRxClient<RX> register(final Object component) {
        client = client.register(component);
        return this;
    }

    @Override
    public JerseyRxClient<RX> register(final Object component, final int priority) {
        client = client.register(component, priority);
        return this;
    }

    @Override
    public JerseyRxClient<RX> register(final Object component, final Class<?>... contracts) {
        client = client.register(component, contracts);
        return this;
    }

    @Override
    public JerseyRxClient<RX> register(final Object component, final Map<Class<?>, Integer> contracts) {
        client = client.register(component, contracts);
        return this;
    }
}
