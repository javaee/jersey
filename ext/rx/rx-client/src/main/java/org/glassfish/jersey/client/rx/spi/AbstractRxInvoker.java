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

package org.glassfish.jersey.client.rx.spi;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.rx.RxInvoker;

/**
 * Default implementation of {@link org.glassfish.jersey.client.rx.RxInvoker reactive invoker}. Extensions of this class are
 * supposed to implement {@link #method(String, javax.ws.rs.client.Entity, Class)} and
 * {@link #method(String, javax.ws.rs.client.Entity, javax.ws.rs.core.GenericType)} methods to which implementations of the rest
 * of the methods from the contract delegate to.
 *
 * @param <T> the asynchronous/event-based completion aware type. The given type should be parametrized with the actual
 *            response type.
 * @author Michal Gajdos
 * @since 2.13
 */
public abstract class AbstractRxInvoker<T> implements RxInvoker<T> {

    private final Invocation.Builder builder;
    private final ExecutorService executorService;

    public AbstractRxInvoker(final Invocation.Builder builder, final ExecutorService executor) {
        if (builder == null) {
            throw new IllegalArgumentException("Invocation builder cannot be null.");
        }

        this.builder = builder;
        this.executorService = executor;
    }

    /**
     * Return invocation builder this reactive invoker was initialized with.
     *
     * @return non-null invocation builder.
     */
    protected Invocation.Builder getBuilder() {
        return builder;
    }

    /**
     * Return executorService service this reactive invoker was initialized with.
     *
     * @return executorService service instance or {@code null}.
     */
    protected ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public T get() {
        return method("GET");
    }

    @Override
    public <R> T get(final Class<R> responseType) {
        return method("GET", responseType);
    }

    @Override
    public <R> T get(final GenericType<R> responseType) {
        return method("GET", responseType);
    }

    @Override
    public T put(final Entity<?> entity) {
        return method("PUT", entity);
    }

    @Override
    public <R> T put(final Entity<?> entity, final Class<R> clazz) {
        return method("PUT", entity, clazz);
    }

    @Override
    public <R> T put(final Entity<?> entity, final GenericType<R> type) {
        return method("PUT", entity, type);
    }

    @Override
    public T post(final Entity<?> entity) {
        return method("POST", entity);
    }

    @Override
    public <R> T post(final Entity<?> entity, final Class<R> clazz) {
        return method("POST", entity, clazz);
    }

    @Override
    public <R> T post(final Entity<?> entity, final GenericType<R> type) {
        return method("POST", entity, type);
    }

    @Override
    public T delete() {
        return method("DELETE");
    }

    @Override
    public <R> T delete(final Class<R> responseType) {
        return method("DELETE", responseType);
    }

    @Override
    public <R> T delete(final GenericType<R> responseType) {
        return method("DELETE", responseType);
    }

    @Override
    public T head() {
        return method("HEAD");
    }

    @Override
    public T options() {
        return method("OPTIONS");
    }

    @Override
    public <R> T options(final Class<R> responseType) {
        return method("OPTIONS", responseType);
    }

    @Override
    public <R> T options(final GenericType<R> responseType) {
        return method("OPTIONS", responseType);
    }

    @Override
    public T trace() {
        return method("TRACE");
    }

    @Override
    public <R> T trace(final Class<R> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public <R> T trace(final GenericType<R> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public T method(final String name) {
        return method(name, Response.class);
    }

    @Override
    public <R> T method(final String name, final Class<R> responseType) {
        return method(name, null, responseType);
    }

    @Override
    public <R> T method(final String name, final GenericType<R> responseType) {
        return method(name, null, responseType);
    }

    @Override
    public T method(final String name, final Entity<?> entity) {
        return method(name, entity, Response.class);
    }
}
