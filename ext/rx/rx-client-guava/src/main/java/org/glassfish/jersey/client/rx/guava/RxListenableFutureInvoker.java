/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Reactive invoker providing support for {@link com.google.common.util.concurrent.ListenableFuture ListenableFuture} from Guava.
 *
 * @author Michal Gajdos
 * @since 2.13
 */
public interface RxListenableFutureInvoker extends RxInvoker<ListenableFuture> {

    @Override
    public ListenableFuture<Response> get();

    @Override
    public <T> ListenableFuture<T> get(Class<T> responseType);

    @Override
    public <T> ListenableFuture<T> get(GenericType<T> responseType);

    @Override
    public ListenableFuture<Response> put(Entity<?> entity);

    @Override
    public <T> ListenableFuture<T> put(Entity<?> entity, Class<T> clazz);

    @Override
    public <T> ListenableFuture<T> put(Entity<?> entity, GenericType<T> type);

    @Override
    public ListenableFuture<Response> post(Entity<?> entity);

    @Override
    public <T> ListenableFuture<T> post(Entity<?> entity, Class<T> clazz);

    @Override
    public <T> ListenableFuture<T> post(Entity<?> entity, GenericType<T> type);

    @Override
    public ListenableFuture<Response> delete();

    @Override
    public <T> ListenableFuture<T> delete(Class<T> responseType);

    @Override
    public <T> ListenableFuture<T> delete(GenericType<T> responseType);

    @Override
    public ListenableFuture<Response> head();

    @Override
    public ListenableFuture<Response> options();

    @Override
    public <T> ListenableFuture<T> options(Class<T> responseType);

    @Override
    public <T> ListenableFuture<T> options(GenericType<T> responseType);

    @Override
    public ListenableFuture<Response> trace();

    @Override
    public <T> ListenableFuture<T> trace(Class<T> responseType);

    @Override
    public <T> ListenableFuture<T> trace(GenericType<T> responseType);

    @Override
    public ListenableFuture<Response> method(String name);

    @Override
    public <T> ListenableFuture<T> method(String name, Class<T> responseType);

    @Override
    public <T> ListenableFuture<T> method(String name, GenericType<T> responseType);

    @Override
    public ListenableFuture<Response> method(String name, Entity<?> entity);

    @Override
    public <T> ListenableFuture<T> method(String name, Entity<?> entity, Class<T> responseType);

    @Override
    public <T> ListenableFuture<T> method(String name, Entity<?> entity, GenericType<T> responseType);
}
