/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.InvocationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.util.ReflectionHelper;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Jersey implementation of {@link javax.ws.rs.client.Invocation JAX-RS client-side
 * request invocation} contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyInvocation implements javax.ws.rs.client.Invocation {

    private final ClientRequest requestContext;

    private JerseyInvocation(Builder builder) {
        this.requestContext = new ClientRequest(builder.requestContext);
    }

    /**
     * Jersey-specific {@link javax.ws.rs.client.Invocation.Builder client invocation builder}.
     */
    public static class Builder implements javax.ws.rs.client.Invocation.Builder {

        private final ClientRequest requestContext;

        /**
         * Create new Jersey-specific client invocation builder.
         *
         * @param uri           invoked request URI.
         * @param configuration Jersey client configuration.
         */
        protected Builder(URI uri, ClientConfig configuration) {
            this.requestContext = new ClientRequest(uri, configuration, new MapPropertiesDelegate());
        }

        /**
         * Returns a reference to the mutable request context to be invoked.
         *
         * @return mutable request context to be invoked.
         */
        ClientRequest request() {
            return requestContext;
        }

        private void storeEntity(Entity<?> entity) {
            if (entity != null) {
                requestContext.variant(entity.getVariant());
                requestContext.setEntity(entity.getEntity());
            }
        }

        @Override
        public JerseyInvocation build(String method) {
            requestContext.setMethod(method);
            return new JerseyInvocation(this);
        }

        @Override
        public JerseyInvocation build(String method, Entity<?> entity) {
            requestContext.setMethod(method);
            storeEntity(entity);
            return new JerseyInvocation(this);
        }

        @Override
        public JerseyInvocation buildGet() {
            requestContext.setMethod("GET");
            return new JerseyInvocation(this);
        }

        @Override
        public JerseyInvocation buildDelete() {
            requestContext.setMethod("DELETE");
            return new JerseyInvocation(this);
        }

        @Override
        public JerseyInvocation buildPost(Entity<?> entity) {
            requestContext.setMethod("POST");
            storeEntity(entity);
            return new JerseyInvocation(this);
        }

        @Override
        public JerseyInvocation buildPut(Entity<?> entity) {
            requestContext.setMethod("PUT");
            storeEntity(entity);
            return new JerseyInvocation(this);
        }

        @Override
        public javax.ws.rs.client.AsyncInvoker async() {
            return new AsyncInvoker(this);
        }

        @Override
        public Builder acceptLanguage(Locale... locales) {
            requestContext.acceptLanguage(locales);
            return this;
        }

        @Override
        public Builder acceptLanguage(String... locales) {
            requestContext.acceptLanguage(locales);
            return this;
        }

        @Override
        public Builder cookie(Cookie cookie) {
            requestContext.cookie(cookie);
            return this;
        }

        @Override
        public Builder allow(String... methods) {
            throw new UnsupportedOperationException("Method will be removed from the API since it is response-only.");
        }

        @Override
        public Builder allow(Set<String> methods) {
            throw new UnsupportedOperationException("Method will be removed from the API since it is response-only.");
        }

        @Override
        public Builder cacheControl(CacheControl cacheControl) {
            requestContext.cacheControl(cacheControl);
            return this;
        }

        @Override
        public Builder header(String name, Object value) {
            requestContext.getHeaders().add(name, value);
            return this;
        }

        @Override
        public Invocation.Builder headers(MultivaluedMap<String, Object> headers) {
            requestContext.replaceHeaders(headers);
            return this;
        }

        @Override
        public ClientConfig configuration() {
            return requestContext.getConfiguration();
        }

        @Override
        public Response get() throws InvocationException {
            return method("GET");
        }

        @Override
        public <T> T get(Class<T> responseType) throws InvocationException {
            return method("GET", responseType);
        }

        @Override
        public <T> T get(GenericType<T> responseType) throws InvocationException {
            return method("GET", responseType);
        }

        @Override
        public Response put(Entity<?> entity) throws InvocationException {
            return method("PUT", entity);
        }

        @Override
        public <T> T put(Entity<?> entity, Class<T> responseType) throws InvocationException {
            return method("PUT", entity, responseType);
        }

        @Override
        public <T> T put(Entity<?> entity, GenericType<T> responseType) throws InvocationException {
            return method("PUT", entity, responseType);
        }

        @Override
        public Response post(Entity<?> entity) throws InvocationException {
            return method("POST", entity);
        }

        @Override
        public <T> T post(Entity<?> entity, Class<T> responseType) throws InvocationException {
            return method("POST", entity, responseType);
        }

        @Override
        public <T> T post(Entity<?> entity, GenericType<T> responseType) throws InvocationException {
            return method("POST", entity, responseType);
        }

        @Override
        public Response delete() throws InvocationException {
            return method("DELETE");
        }

        @Override
        public <T> T delete(Class<T> responseType) throws InvocationException {
            return method("DELETE", responseType);
        }

        @Override
        public <T> T delete(GenericType<T> responseType) throws InvocationException {
            return method("DELETE", responseType);
        }

        @Override
        public Response head() throws InvocationException {
            return method("HEAD");
        }

        @Override
        public Response options() throws InvocationException {
            return method("OPTIONS");
        }

        @Override
        public <T> T options(Class<T> responseType) throws InvocationException {
            return method("OPTIONS", responseType);
        }

        @Override
        public <T> T options(GenericType<T> responseType) throws InvocationException {
            return method("OPTIONS", responseType);
        }

        @Override
        public Response trace(Entity<?> entity) throws InvocationException {
            return method("TRACE", entity);
        }

        @Override
        public <T> T trace(Entity<?> entity, Class<T> responseType) throws InvocationException {
            return method("TRACE", entity, responseType);
        }

        @Override
        public <T> T trace(Entity<?> entity, GenericType<T> responseType) throws InvocationException {
            return method("TRACE", entity, responseType);
        }

        @Override
        public Response method(String name) throws InvocationException {
            requestContext.setMethod(name);
            return new JerseyInvocation(this).invoke();
        }

        @Override
        public <T> T method(String name, Class<T> responseType) throws InvocationException {
            requestContext.setMethod(name);
            return new JerseyInvocation(this).invoke(responseType);
        }

        @Override
        public <T> T method(String name, GenericType<T> responseType) throws InvocationException {
            requestContext.setMethod(name);
            return new JerseyInvocation(this).invoke(responseType);
        }

        @Override
        public Response method(String name, Entity<?> entity) throws InvocationException {
            requestContext.setMethod(name);
            storeEntity(entity);
            return new JerseyInvocation(this).invoke();
        }

        @Override
        public <T> T method(String name, Entity<?> entity, Class<T> responseType) throws InvocationException {
            requestContext.setMethod(name);
            storeEntity(entity);
            return new JerseyInvocation(this).invoke(responseType);
        }

        @Override
        public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) throws InvocationException {
            requestContext.setMethod(name);
            storeEntity(entity);
            return new JerseyInvocation(this).invoke(responseType);
        }
    }

    private static class AsyncInvoker implements javax.ws.rs.client.AsyncInvoker {

        private final JerseyInvocation.Builder builder;

        private AsyncInvoker(JerseyInvocation.Builder request) {
            this.builder = request;
            this.builder.requestContext.setAsynchronous(true);
        }

        @Override
        public Future<Response> get() throws InvocationException {
            return method("GET");
        }

        @Override
        public <T> Future<T> get(Class<T> responseType) throws InvocationException {
            return method("GET", responseType);
        }

        @Override
        public <T> Future<T> get(GenericType<T> responseType) throws InvocationException {
            return method("GET", responseType);
        }

        @Override
        public <T> Future<T> get(InvocationCallback<T> callback) {
            return method("GET", callback);
        }

        @Override
        public Future<Response> put(Entity<?> entity) throws InvocationException {
            return method("PUT", entity);
        }

        @Override
        public <T> Future<T> put(Entity<?> entity, Class<T> responseType) throws InvocationException {
            return method("PUT", entity, responseType);
        }

        @Override
        public <T> Future<T> put(Entity<?> entity, GenericType<T> responseType) throws InvocationException {
            return method("PUT", entity, responseType);
        }

        @Override
        public <T> Future<T> put(Entity<?> entity, InvocationCallback<T> callback) {
            return method("PUT", entity, callback);
        }

        @Override
        public Future<Response> post(Entity<?> entity) throws InvocationException {
            return method("POST", entity);
        }

        @Override
        public <T> Future<T> post(Entity<?> entity, Class<T> responseType) throws InvocationException {
            return method("POST", entity, responseType);
        }

        @Override
        public <T> Future<T> post(Entity<?> entity, GenericType<T> responseType) throws InvocationException {
            return method("POST", entity, responseType);
        }

        @Override
        public <T> Future<T> post(Entity<?> entity, InvocationCallback<T> callback) {
            return method("POST", entity, callback);
        }

        @Override
        public Future<Response> delete() throws InvocationException {
            return method("DELETE");
        }

        @Override
        public <T> Future<T> delete(Class<T> responseType) throws InvocationException {
            return method("DELETE", responseType);
        }

        @Override
        public <T> Future<T> delete(GenericType<T> responseType) throws InvocationException {
            return method("DELETE", responseType);
        }

        @Override
        public <T> Future<T> delete(InvocationCallback<T> callback) {
            return method("DELETE", callback);
        }

        @Override
        public Future<Response> head() throws InvocationException {
            return method("HEAD");
        }

        @Override
        public Future<Response> head(InvocationCallback<Response> callback) {
            return method("HEAD", callback);
        }

        @Override
        public Future<Response> options() throws InvocationException {
            return method("OPTIONS");
        }

        @Override
        public <T> Future<T> options(Class<T> responseType) throws InvocationException {
            return method("OPTIONS", responseType);
        }

        @Override
        public <T> Future<T> options(GenericType<T> responseType) throws InvocationException {
            return method("OPTIONS", responseType);
        }

        @Override
        public <T> Future<T> options(InvocationCallback<T> callback) {
            return method("OPTIONS", callback);
        }

        @Override
        public Future<Response> trace(Entity<?> entity) throws InvocationException {
            return method("TRACE", entity);
        }

        @Override
        public <T> Future<T> trace(Entity<?> entity, Class<T> responseType) throws InvocationException {
            return method("TRACE", entity, responseType);
        }

        @Override
        public <T> Future<T> trace(Entity<?> entity, GenericType<T> responseType) throws InvocationException {
            return method("TRACE", entity, responseType);
        }

        @Override
        public <T> Future<T> trace(Entity<?> entity, InvocationCallback<T> callback) {
            return method("TRACE", entity, callback);
        }

        @Override
        public Future<Response> method(String name) throws InvocationException {
            builder.requestContext.setMethod(name);
            return new JerseyInvocation(builder).submit();
        }

        @Override
        public <T> Future<T> method(String name, Class<T> responseType) throws InvocationException {
            builder.requestContext.setMethod(name);
            return new JerseyInvocation(builder).submit(responseType);
        }

        @Override
        public <T> Future<T> method(String name, GenericType<T> responseType) throws InvocationException {
            builder.requestContext.setMethod(name);
            return new JerseyInvocation(builder).submit(responseType);
        }

        @Override
        public <T> Future<T> method(String name, InvocationCallback<T> callback) {
            builder.requestContext.setMethod(name);
            return new JerseyInvocation(builder).submit(callback);
        }

        @Override
        public Future<Response> method(String name, Entity<?> entity) throws InvocationException {
            builder.requestContext.setMethod(name);
            builder.storeEntity(entity);
            return new JerseyInvocation(builder).submit();
        }

        @Override
        public <T> Future<T> method(String name, Entity<?> entity, Class<T> responseType) throws InvocationException {
            builder.requestContext.setMethod(name);
            builder.storeEntity(entity);
            return new JerseyInvocation(builder).submit(responseType);
        }

        @Override
        public <T> Future<T> method(String name, Entity<?> entity, GenericType<T> responseType) throws InvocationException {
            builder.requestContext.setMethod(name);
            builder.storeEntity(entity);
            return new JerseyInvocation(builder).submit(responseType);
        }

        @Override
        public <T> Future<T> method(String name, Entity<?> entity, InvocationCallback<T> callback) {
            builder.requestContext.setMethod(name);
            builder.storeEntity(entity);
            return new JerseyInvocation(builder).submit(callback);
        }
    }

    @Override
    public Response invoke() throws InvocationException {
        return retrieveResponse(submit());
    }

    @Override
    public <T> T invoke(final Class<T> responseType) throws InvocationException {
        return retrieveResponse(submit(responseType));
    }

    @Override
    public <T> T invoke(final GenericType<T> responseType) throws InvocationException {
        return retrieveResponse(submit(responseType));
    }

    private <T> T retrieveResponse(Future<T> responseFuture) {
        try {
            return responseFuture.get();
        } catch (InterruptedException ex) {
            throw new ClientException(ex);
        } catch (ExecutionException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof ClientException) {
                throw (ClientException) cause;
            } else {
                throw new ClientException(cause);
            }
        }
    }

    @Override
    public Future<Response> submit() {
        final SettableFuture<Response> responseFuture = SettableFuture.create();
        configuration().submit(requestContext, new InvocationCallback<Response>() {

            @Override
            public void completed(Response response) {
                responseFuture.set(response);
            }

            @Override
            public void failed(InvocationException error) {
                responseFuture.setException(error);
            }
        });

        return responseFuture;
    }

    @Override
    public <T> Future<T> submit(final Class<T> responseType) {
        final SettableFuture<T> responseFuture = SettableFuture.create();
        configuration().submit(requestContext, new InvocationCallback<Response>() {

            @Override
            public void completed(Response response) {
                if (responseType == Response.class) {
                    responseFuture.set(responseType.cast(response));
                    return;
                }

                if (response.getStatus() < 300) {
                    try {
                        T entity = response.readEntity(responseType);
                        responseFuture.set(entity);
                    } catch (Exception e) {
                        failed(e instanceof InvocationException ? (InvocationException) e
                                : new InvocationException(e.getMessage(), e));
                    }
                } else {
                    failed(convertToException(response));
                }
            }

            @Override
            public void failed(InvocationException error) {
                responseFuture.setException(error);
            }
        });

        return responseFuture;
    }

    @Override
    public <T> Future<T> submit(final GenericType<T> responseType) {
        final SettableFuture<T> responseFuture = SettableFuture.create();
        configuration().submit(requestContext, new InvocationCallback<Response>() {

            @Override
            public void completed(Response response) {
                if (response.getStatus() < 300) {
                    try {
                        responseFuture.set(response.readEntity(responseType));
                    } catch (Exception e) {
                        failed(new InvocationException(LocalizationMessages.UNEXPECTED_ERROR_RESPONSE_PROCESSING(), e));
                    }
                } else {
                    failed(convertToException(response));
                }
            }

            @Override
            public void failed(InvocationException error) {
                responseFuture.setException(error);
            }
        });

        return responseFuture;
    }

    @Override
    public <T> Future<T> submit(final InvocationCallback<T> callback) {
        final SettableFuture<T> responseFuture = SettableFuture.create();

        final Type callbackType = ReflectionHelper.getTypeArgument(callback.getClass(), 0);
        final Class<T> rawType = ReflectionHelper.erasure(callbackType);

        configuration().submit(requestContext, new InvocationCallback<Response>() {

            @Override
            public void completed(Response response) {
                if (response.getStatus() < 300) {
                    final T result;
                    if (rawType == Response.class) {
                        result = rawType.cast(response);
                    } else {
                        result = response.readEntity(new GenericType<T>(callbackType));
                    }
                    responseFuture.set(result);
                    callback.completed(result);
                } else {
                    failed(convertToException(response));
                }
            }

            @Override
            public void failed(InvocationException error) {
                responseFuture.setException(error);
                callback.failed(error);
            }
        });

        return responseFuture;
    }

    private InvocationException convertToException(Response response) {
        try {
            return new InvocationException(response, true);
        } catch (Throwable t) {
            return new InvocationException(LocalizationMessages.RESPONSE_TO_EXCEPTION_CONVERSION_FAILED(), t);
        }
    }

    @Override
    public ClientConfig configuration() {
        return requestContext.getConfiguration();
    }

    /**
     * Returns a reference to the mutable request context to be invoked.
     *
     * @return mutable request context to be invoked.
     */
    ClientRequest request() {
        return requestContext;
    }
}
