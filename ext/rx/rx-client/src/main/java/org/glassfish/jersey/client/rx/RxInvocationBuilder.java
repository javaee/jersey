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

import java.util.Locale;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * A reactive client request invocation builder.
 * <p/>
 * The builder is enhanced with two methods in addition to the invocation builder from JAX-RS, {@link #rx()} and
 * {@link #rx(java.util.concurrent.ExecutorService)}, giving access to
 * {@link org.glassfish.jersey.client.rx.RxInvoker reactive invoker} that provide means to invoke reactive client requests.
 *
 * @param <RX> the concrete reactive invocation type.
 * @author Michal Gajdos
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see javax.ws.rs.client.Invocation.Builder Invocation.Builder
 * @see org.glassfish.jersey.client.rx.RxInvoker RxInvoker
 * @since 2.13
 */
public interface RxInvocationBuilder<RX extends RxInvoker> extends Invocation.Builder {

    /**
     * Access the reactive request invocation interface to invoke the built request.
     *
     * @return reactive request invocation interface or {@code null} if a invoker for a given type cannot be created.
     */
    public RX rx();

    /**
     * Access the reactive request invocation interface to invoke the built request on a given
     * {@link java.util.concurrent.ExecutorService executor service}.
     *
     * @param executorService the executor service to execute current reactive request.
     * @return reactive request invocation interface or {@code null} if a invoker for a given type cannot be created.
     */
    public RX rx(ExecutorService executorService);

    @Override
    public RxInvocationBuilder<RX> accept(String... mediaTypes);

    @Override
    public RxInvocationBuilder<RX> accept(MediaType... mediaTypes);

    @Override
    public RxInvocationBuilder<RX> acceptLanguage(Locale... locales);

    @Override
    public RxInvocationBuilder<RX> acceptLanguage(String... locales);

    @Override
    public RxInvocationBuilder<RX> acceptEncoding(String... encodings);

    @Override
    public RxInvocationBuilder<RX> cookie(Cookie cookie);

    @Override
    public RxInvocationBuilder<RX> cookie(String name, String value);

    @Override
    public RxInvocationBuilder<RX> cacheControl(CacheControl cacheControl);

    @Override
    public RxInvocationBuilder<RX> header(String name, Object value);

    @Override
    public RxInvocationBuilder<RX> headers(MultivaluedMap<String, Object> headers);

    @Override
    public RxInvocationBuilder<RX> property(String name, Object value);
}
