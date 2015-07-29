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

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.rx.spi.RxInvokerProvider;
import org.glassfish.jersey.internal.ServiceFinder;

/**
 * Jersey implementation of {@link org.glassfish.jersey.client.rx.RxInvocationBuilder reactive client request invocation builder}
 * contract, extension of JAX-RS {@link javax.ws.rs.client.Invocation.Builder client request invocation builder} contract.
 *
 * @param <RX> the concrete reactive invocation type.
 * @author Michal Gajdos
 */
final class JerseyRxInvocationBuilder<RX extends RxInvoker> implements RxInvocationBuilder<RX> {

    private static final Set<RxInvokerProvider> INVOKER_PROVIDERS = new HashSet<>();
    private static final Map<Class<? extends RxInvoker>, RxInvokerProvider> PROVIDER_MAP = new IdentityHashMap<>();

    static {
        for (final RxInvokerProvider provider : ServiceFinder.find(RxInvokerProvider.class)) {
            INVOKER_PROVIDERS.add(provider);
        }
    }

    private final Class<RX> invokerType;
    private final ExecutorService executor;

    private RX invoker;
    private Invocation.Builder builder;

    JerseyRxInvocationBuilder(final Invocation.Builder builder, final Class<RX> invokerType,
                              final ExecutorService executor) {
        this.builder = builder;
        this.executor = executor;
        this.invokerType = invokerType;
    }

    @Override
    public RX rx() {
        if (invoker == null) {
            invoker = rx(invokerType, executor);
        }
        return invoker;
    }

    @Override
    public RX rx(final ExecutorService executorService) {
        return rx(invokerType, executorService);
    }

    private <CX extends RxInvoker> CX rx(final Class<CX> customType, final ExecutorService executor) {
        final RxInvokerProvider provider = PROVIDER_MAP.get(customType);
        RxInvoker customInvoker = null;

        if (provider == null) {
            for (final RxInvokerProvider invokerProvider : INVOKER_PROVIDERS) {
                customInvoker = invokerProvider.getInvoker(customType, builder, executor);

                if (customInvoker != null) {
                    PROVIDER_MAP.put(customType, invokerProvider);
                    break;
                }
            }
        } else {
            customInvoker = provider.getInvoker(customType, builder, executor);
        }

        return customType.cast(customInvoker);
    }

    @Override
    public JerseyRxInvocationBuilder<RX> accept(final String... mediaTypes) {
        builder = builder.accept(mediaTypes);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> accept(final MediaType... mediaTypes) {
        builder = builder.accept(mediaTypes);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> acceptLanguage(final Locale... locales) {
        builder = builder.acceptLanguage(locales);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> acceptLanguage(final String... locales) {
        builder = builder.acceptLanguage(locales);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> acceptEncoding(final String... encodings) {
        builder = builder.acceptEncoding(encodings);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> cookie(final Cookie cookie) {
        builder = builder.cookie(cookie);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> cookie(final String name, final String value) {
        builder = builder.cookie(name, value);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> cacheControl(final CacheControl cacheControl) {
        builder = builder.cacheControl(cacheControl);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> header(final String name, final Object value) {
        builder = builder.header(name, value);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> headers(final MultivaluedMap<String, Object> headers) {
        builder = builder.headers(headers);
        return this;
    }

    @Override
    public JerseyRxInvocationBuilder<RX> property(final String name, final Object value) {
        builder = builder.property(name, value);
        return this;
    }

    @Override
    public Response get() {
        return builder.get();
    }

    @Override
    public <T> T get(final Class<T> responseType) {
        return builder.get(responseType);
    }

    @Override
    public <T> T get(final GenericType<T> responseType) {
        return builder.get(responseType);
    }

    @Override
    public Response put(final Entity<?> entity) {
        return builder.put(entity);
    }

    @Override
    public <T> T put(final Entity<?> entity, final Class<T> responseType) {
        return builder.put(entity, responseType);
    }

    @Override
    public <T> T put(final Entity<?> entity, final GenericType<T> responseType) {
        return builder.put(entity, responseType);
    }

    @Override
    public Response post(final Entity<?> entity) {
        return builder.post(entity);
    }

    @Override
    public <T> T post(final Entity<?> entity, final Class<T> responseType) {
        return builder.post(entity, responseType);
    }

    @Override
    public <T> T post(final Entity<?> entity, final GenericType<T> responseType) {
        return builder.post(entity, responseType);
    }

    @Override
    public Response delete() {
        return builder.delete();
    }

    @Override
    public <T> T delete(final Class<T> responseType) {
        return builder.delete(responseType);
    }

    @Override
    public <T> T delete(final GenericType<T> responseType) {
        return builder.delete(responseType);
    }

    @Override
    public Response head() {
        return builder.head();
    }

    @Override
    public Response options() {
        return builder.options();
    }

    @Override
    public <T> T options(final Class<T> responseType) {
        return builder.options(responseType);
    }

    @Override
    public <T> T options(final GenericType<T> responseType) {
        return builder.options(responseType);
    }

    @Override
    public Response trace() {
        return builder.trace();
    }

    @Override
    public <T> T trace(final Class<T> responseType) {
        return builder.trace(responseType);
    }

    @Override
    public <T> T trace(final GenericType<T> responseType) {
        return builder.trace(responseType);
    }

    @Override
    public Response method(final String name) {
        return builder.method(name);
    }

    @Override
    public <T> T method(final String name, final Class<T> responseType) {
        return builder.method(name, responseType);
    }

    @Override
    public <T> T method(final String name, final GenericType<T> responseType) {
        return builder.method(name, responseType);
    }

    @Override
    public Response method(final String name, final Entity<?> entity) {
        return builder.method(name, entity);
    }

    @Override
    public <T> T method(final String name, final Entity<?> entity, final Class<T> responseType) {
        return builder.method(name, entity, responseType);
    }

    @Override
    public <T> T method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
        return builder.method(name, entity, responseType);
    }

    @Override
    public Invocation build(final String method) {
        return builder.build(method);
    }

    @Override
    public Invocation build(final String method, final Entity<?> entity) {
        return builder.build(method, entity);
    }

    @Override
    public Invocation buildGet() {
        return builder.buildGet();
    }

    @Override
    public Invocation buildDelete() {
        return builder.buildDelete();
    }

    @Override
    public Invocation buildPost(final Entity<?> entity) {
        return builder.buildPost(entity);
    }

    @Override
    public Invocation buildPut(final Entity<?> entity) {
        return builder.buildPut(entity);
    }

    @Override
    public AsyncInvoker async() {
        return builder.async();
    }
}
