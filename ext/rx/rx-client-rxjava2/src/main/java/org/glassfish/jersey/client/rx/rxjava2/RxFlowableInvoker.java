/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.rx.rxjava2;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import io.reactivex.Flowable;


/**
 * Reactive invoker providing support for {@link Flowable flowable} from RxJava.
 * <p/>
 * Requests are by default invoked on a separate thread (as JAX-RS Async client requests). This behavior can be overridden by
 * providing a {@link java.util.concurrent.ExecutorService executor service} when client extension is being created.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Michal Gajdos
 * @since 2.26
 */
public interface RxFlowableInvoker extends RxInvoker<Flowable> {

    @Override
    Flowable<Response> get();

    @Override
    <R> Flowable<R> get(Class<R> responseType);

    @Override
    <R> Flowable<R> get(GenericType<R> responseType);

    @Override
    Flowable<Response> put(Entity<?> entity);

    @Override
    <R> Flowable<R> put(Entity<?> entity, Class<R> responseType);

    @Override
    <R> Flowable<R> put(Entity<?> entity, GenericType<R> responseType);

    @Override
    Flowable<Response> post(Entity<?> entity);

    @Override
    <R> Flowable<R> post(Entity<?> entity, Class<R> responseType);

    @Override
    <R> Flowable<R> post(Entity<?> entity, GenericType<R> responseType);

    @Override
    Flowable<Response> delete();

    @Override
    <R> Flowable<R> delete(Class<R> responseType);

    @Override
    <R> Flowable<R> delete(GenericType<R> responseType);

    @Override
    Flowable<Response> head();

    @Override
    Flowable<Response> options();

    @Override
    <R> Flowable<R> options(Class<R> responseType);

    @Override
    <R> Flowable<R> options(GenericType<R> responseType);

    @Override
    Flowable<Response> trace();

    @Override
    <R> Flowable<R> trace(Class<R> responseType);

    @Override
    <R> Flowable<R> trace(GenericType<R> responseType);

    @Override
    Flowable<Response> method(String name);

    @Override
    <R> Flowable<R> method(String name, Class<R> responseType);

    @Override
    <R> Flowable<R> method(String name, GenericType<R> responseType);

    @Override
    Flowable<Response> method(String name, Entity<?> entity);

    @Override
    <R> Flowable<R> method(String name, Entity<?> entity, Class<R> responseType);

    @Override
    <R> Flowable<R> method(String name, Entity<?> entity, GenericType<R> responseType);
}
