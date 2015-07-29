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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

/**
 * Uniform interface for reactive invocation of HTTP methods.
 * <p/>
 * The {@code RxInvoker} is intended to be extended and not used directly to preserve type safety. Extensions are supposed to
 * override all methods present in this interface specifying the actual return types. For example, reactive invoker providing
 * support for {@code CompletionStage} from Java 8 the overridden methods should look like:
 * <pre>
 * &#64;Override
 * public CompletionStage&lt;Response&gt; get();
 *
 * &#64;Override
 * public &lt;T&gt; CompletionStage&lt;T&gt; get(Class&lt;T&gt; responseType);</pre>
 *
 * Instances of {@code RxInvoker} extensions are created using the
 * {@link org.glassfish.jersey.client.rx.spi.RxInvokerProvider RxInvokerProvider} SPI contract.
 *
 * @param <T> the asynchronous/event-based completion aware type. The given type should be parametrized with the actual
 *            response type.
 * @author Michal Gajdos
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see javax.ws.rs.client.SyncInvoker SyncInvoker
 * @see org.glassfish.jersey.client.rx.spi.RxInvokerProvider RxInvokerProvider
 * @since 2.13
 */
public interface RxInvoker<T> {

    /**
     * Invoke HTTP GET method for the current request.
     *
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T get();

    /**
     * Invoke HTTP GET method for the current request.
     *
     * @param responseType Java type the response entity will be converted to.
     * @param <R>          response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T get(Class<R> responseType);

    /**
     * Invoke HTTP GET method for the current request.
     *
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @param <R>          generic response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T get(GenericType<R> responseType);

    /**
     * Invoke HTTP PUT method for the current request.
     *
     * @param entity request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *               Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *               {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *               the entity variant information.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T put(Entity<?> entity);

    /**
     * Invoke HTTP PUT method for the current request.
     *
     * @param entity       request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *                     Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *                     {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *                     the entity variant information.
     * @param responseType Java type the response entity will be converted to.
     * @param <R>          response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T put(Entity<?> entity, Class<R> responseType);

    /**
     * Invoke HTTP PUT method for the current request.
     *
     * @param entity       request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *                     Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *                     {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *                     the entity variant information.
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @param <R>          generic response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T put(Entity<?> entity, GenericType<R> responseType);

    /**
     * Invoke HTTP POST method for the current request.
     *
     * @param entity request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *               Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *               {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *               the entity variant information.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T post(Entity<?> entity);

    /**
     * Invoke HTTP POST method for the current request.
     *
     * @param entity       request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *                     Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *                     {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *                     the entity variant information.
     * @param responseType Java type the response entity will be converted to.
     * @param <R>          response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T post(Entity<?> entity, Class<R> responseType);

    /**
     * Invoke HTTP POST method for the current request.
     *
     * @param entity       request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *                     Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *                     {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *                     the entity variant information.
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @param <R>          generic response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T post(Entity<?> entity, GenericType<R> responseType);

    /**
     * Invoke HTTP DELETE method for the current request.
     *
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T delete();

    /**
     * Invoke HTTP DELETE method for the current request.
     *
     * @param responseType Java type the response entity will be converted to.
     * @param <R>          response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T delete(Class<R> responseType);

    /**
     * Invoke HTTP DELETE method for the current request.
     *
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @param <R>          generic response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T delete(GenericType<R> responseType);

    /**
     * Invoke HTTP HEAD method for the current request.
     *
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T head();

    /**
     * Invoke HTTP OPTIONS method for the current request.
     *
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T options();

    /**
     * Invoke HTTP OPTIONS method for the current request.
     *
     * @param responseType Java type the response entity will be converted to.
     * @param <R>          response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T options(Class<R> responseType);

    /**
     * Invoke HTTP OPTIONS method for the current request.
     *
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @param <R>          generic response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T options(GenericType<R> responseType);

    /**
     * Invoke HTTP TRACE method for the current request.
     *
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T trace();

    /**
     * Invoke HTTP OPTIONS method for the current request.
     *
     * @param responseType Java type the response entity will be converted to.
     * @param <R>          response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T trace(Class<R> responseType);

    /**
     * Invoke HTTP TRACE method for the current request.
     *
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @param <R>          generic response entity type.
     * @return invocation response wrapped in the completion aware type.
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T trace(GenericType<R> responseType);

    /**
     * Invoke an arbitrary method for the current request.
     *
     * @param name method name.
     * @return invocation response wrapped in the completion aware type..
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T method(String name);

    /**
     * Invoke an arbitrary method for the current request.
     *
     * @param name         method name.
     * @param responseType Java type the response entity will be converted to.
     * @param <R>          response entity type.
     * @return invocation response wrapped in the completion aware type..
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T method(String name, Class<R> responseType);

    /**
     * Invoke an arbitrary method for the current request.
     *
     * @param name         method name.
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @param <R>          generic response entity type.
     * @return invocation response wrapped in the completion aware type..
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T method(String name, GenericType<R> responseType);

    /**
     * Invoke an arbitrary method for the current request.
     *
     * @param name         method name.
     * @param entity       request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *                     Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *                     {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *                     the entity variant information.
     * @return invocation response wrapped in the completion aware type..
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     */
    public T method(String name, Entity<?> entity);

    /**
     * Invoke an arbitrary method for the current request.
     *
     * @param name         method name.
     * @param entity       request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *                     Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *                     {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *                     the entity variant information.
     * @param responseType Java type the response entity will be converted to.
     * @param <R>          response entity type.
     * @return invocation response wrapped in the completion aware type..
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T method(String name, Entity<?> entity, Class<R> responseType);

    /**
     * Invoke an arbitrary method for the current request.
     *
     * @param name         method name.
     * @param entity       request entity, including it's full {@link javax.ws.rs.core.Variant} information.
     *                     Any variant-related HTTP headers previously set (namely {@code Content-Type},
     *                     {@code Content-Language} and {@code Content-Encoding}) will be overwritten using
     *                     the entity variant information.
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @param <R>          generic response entity type.
     * @return invocation response wrapped in the completion aware type..
     * @throws javax.ws.rs.client.ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a
     *                                                        filter or during conversion of the response entity data to an
     *                                                        instance of a particular Java type).
     * @throws javax.ws.rs.ProcessingException                in case the request processing or subsequent I/O operation fails.
     * @throws javax.ws.rs.WebApplicationException            in case the response status code of the response returned by the
     *                                                        server is not
     *                                                        {@link javax.ws.rs.core.Response.Status.Family#SUCCESSFUL
     *                                                        successful} and the specified response type is not
     *                                                        {@link javax.ws.rs.core.Response}.
     */
    public <R> T method(String name, Entity<?> entity, GenericType<R> responseType);
}
