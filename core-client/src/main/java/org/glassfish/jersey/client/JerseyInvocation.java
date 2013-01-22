/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.process.internal.RequestScope;

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
        validateHttpMethodAndEntity(builder.requestContext);
        this.requestContext = new ClientRequest(builder.requestContext);
    }

    private enum EntityPresence {
        MUST_BE_NULL,
        MUST_BE_PRESENT,
        OPTIONAL
    }

    private static Map<String, EntityPresence> METHODS = initializeMap();

    private static Map<String, EntityPresence> initializeMap() {
        Map<String, EntityPresence> map = new HashMap<String, EntityPresence>();

        map.put("DELETE", EntityPresence.MUST_BE_NULL);
        map.put("GET", EntityPresence.MUST_BE_NULL);
        map.put("HEAD", EntityPresence.MUST_BE_NULL);
        map.put("OPTIONS", EntityPresence.MUST_BE_NULL);
        map.put("POST", EntityPresence.OPTIONAL); // we allow to post null instead of entity
        map.put("PUT", EntityPresence.MUST_BE_PRESENT);
        map.put("TRACE", EntityPresence.MUST_BE_NULL);
        return map;
    }

    private void validateHttpMethodAndEntity(ClientRequest request) {
        final String method = request.getMethod();

        final EntityPresence entityPresence = METHODS.get(method.toUpperCase());
        if (entityPresence == EntityPresence.MUST_BE_NULL && request.hasEntity()) {
            throw new IllegalStateException(LocalizationMessages.ERROR_HTTP_METHOD_ENTITY_NOT_NULL(method));
        } else if (entityPresence == EntityPresence.MUST_BE_PRESENT && !request.hasEntity()) {
            throw new IllegalStateException(LocalizationMessages.ERROR_HTTP_METHOD_ENTITY_NULL(method));
        }
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
                requestContext.setEntityAnnotations(entity.getAnnotations());
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
        public Builder cookie(String name, String value) {
            requestContext.cookie(new Cookie(name, value));
            return this;
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
        public Builder headers(MultivaluedMap<String, Object> headers) {
            requestContext.replaceHeaders(headers);
            return this;
        }

        @Override
        public Response get() throws ClientException {
            return method("GET");
        }

        @Override
        public <T> T get(Class<T> responseType) throws ClientException, WebApplicationException {
            return method("GET", responseType);
        }

        @Override
        public <T> T get(GenericType<T> responseType) throws ClientException, WebApplicationException {
            return method("GET", responseType);
        }

        @Override
        public Response put(Entity<?> entity) throws ClientException {
            return method("PUT", entity);
        }

        @Override
        public <T> T put(Entity<?> entity, Class<T> responseType) throws ClientException, WebApplicationException {
            return method("PUT", entity, responseType);
        }

        @Override
        public <T> T put(Entity<?> entity, GenericType<T> responseType) throws ClientException, WebApplicationException {
            return method("PUT", entity, responseType);
        }

        @Override
        public Response post(Entity<?> entity) throws ClientException {
            return method("POST", entity);
        }

        @Override
        public <T> T post(Entity<?> entity, Class<T> responseType) throws ClientException, WebApplicationException {
            return method("POST", entity, responseType);
        }

        @Override
        public <T> T post(Entity<?> entity, GenericType<T> responseType) throws ClientException, WebApplicationException {
            return method("POST", entity, responseType);
        }

        @Override
        public Response delete() throws ClientException {
            return method("DELETE");
        }

        @Override
        public <T> T delete(Class<T> responseType) throws ClientException, WebApplicationException {
            return method("DELETE", responseType);
        }

        @Override
        public <T> T delete(GenericType<T> responseType) throws ClientException, WebApplicationException {
            return method("DELETE", responseType);
        }

        @Override
        public Response head() throws ClientException {
            return method("HEAD");
        }

        @Override
        public Response options() throws ClientException {
            return method("OPTIONS");
        }

        @Override
        public <T> T options(Class<T> responseType) throws ClientException, WebApplicationException {
            return method("OPTIONS", responseType);
        }

        @Override
        public <T> T options(GenericType<T> responseType) throws ClientException, WebApplicationException {
            return method("OPTIONS", responseType);
        }

        @Override
        public Response trace() throws ClientException {
            return method("TRACE");
        }

        @Override
        public <T> T trace(Class<T> responseType) throws ClientException, WebApplicationException {
            return method("TRACE", responseType);
        }

        @Override
        public <T> T trace(GenericType<T> responseType) throws ClientException, WebApplicationException {
            return method("TRACE", responseType);
        }

        @Override
        public Response method(String name) throws ClientException {
            requestContext.setMethod(name);
            return new JerseyInvocation(this).invoke();
        }

        @Override
        public <T> T method(String name, Class<T> responseType) throws ClientException, WebApplicationException {
            if (responseType == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
            }
            requestContext.setMethod(name);
            return new JerseyInvocation(this).invoke(responseType);
        }

        @Override
        public <T> T method(String name, GenericType<T> responseType) throws ClientException, WebApplicationException {
            if (responseType == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
            }
            requestContext.setMethod(name);
            return new JerseyInvocation(this).invoke(responseType);
        }

        @Override
        public Response method(String name, Entity<?> entity) throws ClientException {
            requestContext.setMethod(name);
            storeEntity(entity);
            return new JerseyInvocation(this).invoke();
        }

        @Override
        public <T> T method(String name, Entity<?> entity, Class<T> responseType)
                throws ClientException, WebApplicationException {
            if (responseType == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
            }
            requestContext.setMethod(name);
            storeEntity(entity);
            return new JerseyInvocation(this).invoke(responseType);
        }

        @Override
        public <T> T method(String name, Entity<?> entity, GenericType<T> responseType)
                throws ClientException, WebApplicationException {
            if (responseType == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
            }
            requestContext.setMethod(name);
            storeEntity(entity);
            return new JerseyInvocation(this).invoke(responseType);
        }

        @Override
        public Builder setProperty(String name, Object value) {
            requestContext.getClientConfig().setProperty(name, value);
            return this;
        }

        @Override
        public Builder register(Class<?> providerClass) {
            requestContext.getClientConfig().register(providerClass);
            return this;
        }

        @Override
        public Builder register(Class<?> providerClass, int bindingPriority) {
            requestContext.getClientConfig().register(providerClass, bindingPriority);
            return this;
        }

        @Override
        public Builder register(Class<?> providerClass, Class<?>... contracts) {
            requestContext.getClientConfig().register(providerClass, contracts);
            return this;
        }

        @Override
        public Builder register(Class<?> providerClass, Map<Class<?>, Integer> contracts) {
            requestContext.getClientConfig().register(providerClass, contracts);
            return this;
        }

        @Override
        public Builder register(Object provider) {
            requestContext.getClientConfig().register(provider);
            return this;
        }

        @Override
        public Builder register(Object provider, int bindingPriority) {
            requestContext.getClientConfig().register(provider, bindingPriority);
            return this;
        }

        @Override
        public Builder register(Object provider, Class<?>... contracts) {
            requestContext.getClientConfig().register(provider, contracts);
            return this;
        }

        @Override
        public Builder register(Object provider, Map<Class<?>, Integer> contracts) {
            requestContext.getClientConfig().register(provider, contracts);
            return this;
        }

        @Override
        public Builder replaceWith(Configuration config) {
            requestContext.getClientConfig().replaceWith(config);
            return this;
        }

        @Override
        public ClientConfig getConfiguration() {
            return requestContext.getClientConfig();
        }
    }

    private static class AsyncInvoker implements javax.ws.rs.client.AsyncInvoker {

        private final JerseyInvocation.Builder builder;

        private AsyncInvoker(JerseyInvocation.Builder request) {
            this.builder = request;
            this.builder.requestContext.setAsynchronous(true);
        }

        @Override
        public Future<Response> get() {
            return method("GET");
        }

        @Override
        public <T> Future<T> get(Class<T> responseType) {
            return method("GET", responseType);
        }

        @Override
        public <T> Future<T> get(GenericType<T> responseType) {
            return method("GET", responseType);
        }

        @Override
        public <T> Future<T> get(InvocationCallback<T> callback) {
            return method("GET", callback);
        }

        @Override
        public Future<Response> put(Entity<?> entity) {
            return method("PUT", entity);
        }

        @Override
        public <T> Future<T> put(Entity<?> entity, Class<T> responseType) {
            return method("PUT", entity, responseType);
        }

        @Override
        public <T> Future<T> put(Entity<?> entity, GenericType<T> responseType) {
            return method("PUT", entity, responseType);
        }

        @Override
        public <T> Future<T> put(Entity<?> entity, InvocationCallback<T> callback) {
            return method("PUT", entity, callback);
        }

        @Override
        public Future<Response> post(Entity<?> entity) {
            return method("POST", entity);
        }

        @Override
        public <T> Future<T> post(Entity<?> entity, Class<T> responseType) {
            return method("POST", entity, responseType);
        }

        @Override
        public <T> Future<T> post(Entity<?> entity, GenericType<T> responseType) {
            return method("POST", entity, responseType);
        }

        @Override
        public <T> Future<T> post(Entity<?> entity, InvocationCallback<T> callback) {
            return method("POST", entity, callback);
        }

        @Override
        public Future<Response> delete() {
            return method("DELETE");
        }

        @Override
        public <T> Future<T> delete(Class<T> responseType) {
            return method("DELETE", responseType);
        }

        @Override
        public <T> Future<T> delete(GenericType<T> responseType) {
            return method("DELETE", responseType);
        }

        @Override
        public <T> Future<T> delete(InvocationCallback<T> callback) {
            return method("DELETE", callback);
        }

        @Override
        public Future<Response> head() {
            return method("HEAD");
        }

        @Override
        public Future<Response> head(InvocationCallback<Response> callback) {
            return method("HEAD", callback);
        }

        @Override
        public Future<Response> options() {
            return method("OPTIONS");
        }

        @Override
        public <T> Future<T> options(Class<T> responseType) {
            return method("OPTIONS", responseType);
        }

        @Override
        public <T> Future<T> options(GenericType<T> responseType) {
            return method("OPTIONS", responseType);
        }

        @Override
        public <T> Future<T> options(InvocationCallback<T> callback) {
            return method("OPTIONS", callback);
        }

        @Override
        public Future<Response> trace() {
            return method("TRACE");
        }

        @Override
        public <T> Future<T> trace(Class<T> responseType) {
            return method("TRACE", responseType);
        }

        @Override
        public <T> Future<T> trace(GenericType<T> responseType) {
            return method("TRACE", responseType);
        }

        @Override
        public <T> Future<T> trace(InvocationCallback<T> callback) {
            return method("TRACE", callback);
        }

        @Override
        public Future<Response> method(String name) {
            builder.requestContext.setMethod(name);
            return new JerseyInvocation(builder).submit();
        }

        @Override
        public <T> Future<T> method(String name, Class<T> responseType) {
            if (responseType == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
            }
            builder.requestContext.setMethod(name);
            return new JerseyInvocation(builder).submit(responseType);
        }

        @Override
        public <T> Future<T> method(String name, GenericType<T> responseType) {
            if (responseType == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
            }
            builder.requestContext.setMethod(name);
            return new JerseyInvocation(builder).submit(responseType);
        }

        @Override
        public <T> Future<T> method(String name, InvocationCallback<T> callback) {
            builder.requestContext.setMethod(name);
            return new JerseyInvocation(builder).submit(callback);
        }

        @Override
        public Future<Response> method(String name, Entity<?> entity) {
            builder.requestContext.setMethod(name);
            builder.storeEntity(entity);
            return new JerseyInvocation(builder).submit();
        }

        @Override
        public <T> Future<T> method(String name, Entity<?> entity, Class<T> responseType) {
            if (responseType == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
            }
            builder.requestContext.setMethod(name);
            builder.storeEntity(entity);
            return new JerseyInvocation(builder).submit(responseType);
        }

        @Override
        public <T> Future<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
            if (responseType == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
            }
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
    public Response invoke() throws ClientException, WebApplicationException {
        final ClientRuntime runtime = request().getClientRuntime();
        final RequestScope requestScope = runtime.getRequestScope();
        return requestScope.runInScope(new Producer<Response>() {
            @Override
            public Response call() throws ClientException {
                return new ScopedJaxrsResponse(runtime.invoke(requestContext), requestScope);
            }
        });
    }

    @Override
    public <T> T invoke(final Class<T> responseType) throws ClientException, WebApplicationException {
        if (responseType == null) {
            throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
        }
        final ClientRuntime runtime = request().getClientRuntime();
        final RequestScope requestScope = runtime.getRequestScope();
        return requestScope.runInScope(new Producer<T>() {
            @Override
            public T call() throws ClientException {
                try {
                    return translate(runtime.invoke(requestContext), requestScope, responseType);
                } catch (ClientException ex) {
                    if (ex.getCause() instanceof WebApplicationException) {
                        throw (WebApplicationException) ex.getCause();
                    }
                    throw ex;
                }
            }
        });
    }

    @Override
    public <T> T invoke(final GenericType<T> responseType) throws ClientException, WebApplicationException {
        if (responseType == null) {
            throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
        }
        final ClientRuntime runtime = request().getClientRuntime();
        final RequestScope requestScope = runtime.getRequestScope();
        return requestScope.runInScope(new Producer<T>() {
            @Override
            public T call() throws ClientException {
                try {
                    return translate(runtime.invoke(requestContext), requestScope, responseType);
                } catch (ClientException ex) {
                    if (ex.getCause() instanceof WebApplicationException) {
                        throw (WebApplicationException) ex.getCause();
                    }
                    throw ex;
                }
            }
        });
    }

    @Override
    public Future<Response> submit() {
        final SettableFuture<Response> responseFuture = SettableFuture.create();
        request().getClientRuntime().submit(requestContext, new ResponseCallback() {

            @Override
            public void completed(ClientResponse response, RequestScope scope) {
                responseFuture.set(new ScopedJaxrsResponse(response, scope));
            }

            @Override
            public void failed(ClientException error) {
                responseFuture.setException(error);
            }
        });

        return responseFuture;
    }

    @Override
    public <T> Future<T> submit(final Class<T> responseType) {
        if (responseType == null) {
            throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
        }
        final SettableFuture<T> responseFuture = SettableFuture.create();
        request().getClientRuntime().submit(requestContext, new ResponseCallback() {

            @Override
            public void completed(ClientResponse response, RequestScope scope) {
                try {
                    responseFuture.set(translate(response, scope, responseType));
                } catch (ClientException ex) {
                    failed(ex);
                }
            }

            @Override
            public void failed(ClientException error) {
                if (error.getCause() instanceof WebApplicationException) {
                    responseFuture.setException(error.getCause());
                } else {
                    responseFuture.setException(error);
                }
            }
        });

        return responseFuture;
    }

    private <T> T translate(ClientResponse response, RequestScope scope, Class<T> responseType)
            throws ClientException {
        if (responseType == Response.class) {
            return responseType.cast(new ScopedJaxrsResponse(response, scope));
        }

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            try {
                return new InboundJaxrsResponse(response).readEntity(responseType);
            } catch (ClientException ex) {
                throw ex;
            } catch (WebApplicationException ex) {
                throw new ClientException(ex);
            } catch (Exception ex) {
                throw new ClientException(LocalizationMessages.UNEXPECTED_ERROR_RESPONSE_PROCESSING(), ex);
            }
        } else {
            throw convertToException(new ScopedJaxrsResponse(response, scope));
        }
    }

    @Override
    public <T> Future<T> submit(final GenericType<T> responseType) {
        if (responseType == null) {
            throw new IllegalArgumentException(LocalizationMessages.RESPONSE_TYPE_IS_NULL());
        }
        final SettableFuture<T> responseFuture = SettableFuture.create();
        request().getClientRuntime().submit(requestContext, new ResponseCallback() {

            @Override
            public void completed(ClientResponse response, RequestScope scope) {
                try {
                    responseFuture.set(translate(response, scope, responseType));
                } catch (ClientException ex) {
                    failed(ex);
                }
            }

            @Override
            public void failed(ClientException error) {
                if (error.getCause() instanceof WebApplicationException) {
                    responseFuture.setException(error.getCause());
                } else {
                    responseFuture.setException(error);
                }
            }
        });

        return responseFuture;
    }

    private <T> T translate(ClientResponse response, RequestScope scope, GenericType<T> responseType)
            throws ClientException {
        if (responseType.getRawType() == Response.class) {
            //noinspection unchecked
            return (T) new ScopedJaxrsResponse(response, scope);
        }

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            try {
                return new InboundJaxrsResponse(response).readEntity(responseType);
            } catch (ClientException ex) {
                throw ex;
            } catch (WebApplicationException ex) {
                throw new ClientException(ex);
            } catch (Exception ex) {
                throw new ClientException(LocalizationMessages.UNEXPECTED_ERROR_RESPONSE_PROCESSING(), ex);
            }
        } else {
            throw convertToException(new ScopedJaxrsResponse(response, scope));
        }
    }

    @Override
    public <T> Future<T> submit(final InvocationCallback<T> callback) {
        final SettableFuture<T> responseFuture = SettableFuture.create();

        try {

            ReflectionHelper.DeclaringClassInterfacePair pair =
                    ReflectionHelper.getClass(callback.getClass(), InvocationCallback.class);
            final Type callbackParamType = ReflectionHelper.getParameterizedTypeArguments(pair)[0];
            final Class<T> callbackParamClass = ReflectionHelper.erasure(callbackParamType);

            final ResponseCallback responseCallback = new ResponseCallback() {

                @Override
                public void completed(ClientResponse response, RequestScope scope) {
                    final T result;
                    if (callbackParamClass == Response.class) {
                        result = callbackParamClass.cast(new ScopedJaxrsResponse(response, scope));
                        responseFuture.set(result);
                        callback.completed(result);
                    } else if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                        result = new InboundJaxrsResponse(response).readEntity(new GenericType<T>(callbackParamType));
                        responseFuture.set(result);
                        callback.completed(result);
                    } else {
                        failed(convertToException(new ScopedJaxrsResponse(response, scope)));
                    }
                }

                @Override
                public void failed(ClientException error) {
                    try {
                        if (error.getCause() instanceof WebApplicationException) {
                            responseFuture.setException(error.getCause());
                        } else {
                            responseFuture.setException(error);
                        }
                    } finally {
                        callback.failed(error);
                    }
                }
            };
            request().getClientRuntime().submit(requestContext, responseCallback);
        } catch (Throwable error) {
            ClientException ce;
            if (error instanceof ClientException) {
                ce = (ClientException) error;
                responseFuture.setException(ce);
            } else if (error instanceof WebApplicationException) {
                ce = new ClientException(error);
                responseFuture.setException(error);
            } else {
                ce = new ClientException(error);
                responseFuture.setException(ce);
            }
            callback.failed(ce);
        }

        return responseFuture;
    }

    @Override
    public JerseyInvocation setProperty(String name, Object value) {
        requestContext.getClientConfig().setProperty(name, value);
        return this;
    }

    @Override
    public JerseyInvocation register(Class<?> providerClass) {
        requestContext.getClientConfig().register(providerClass);
        return this;
    }

    @Override
    public JerseyInvocation register(Class<?> providerClass, int bindingPriority) {
        requestContext.getClientConfig().register(providerClass, bindingPriority);
        return this;
    }

    @Override
    public JerseyInvocation register(Class<?> providerClass, Class<?>... contracts) {
        requestContext.getClientConfig().register(providerClass, contracts);
        return this;
    }

    @Override
    public JerseyInvocation register(Class<?> providerClass, Map<Class<?>, Integer> contracts) {
        requestContext.getClientConfig().register(providerClass, contracts);
        return this;
    }

    @Override
    public JerseyInvocation register(Object provider) {
        requestContext.getClientConfig().register(provider);
        return this;
    }

    @Override
    public JerseyInvocation register(Object provider, int bindingPriority) {
        requestContext.getClientConfig().register(provider, bindingPriority);
        return this;
    }

    @Override
    public JerseyInvocation register(Object provider, Class<?>... contracts) {
        requestContext.getClientConfig().register(provider, contracts);
        return this;
    }

    @Override
    public JerseyInvocation register(Object provider, Map<Class<?>, Integer> contracts) {
        requestContext.getClientConfig().register(provider, contracts);
        return this;
    }

    @Override
    public JerseyInvocation replaceWith(Configuration config) {
        requestContext.getClientConfig().replaceWith(config);
        return this;
    }

    @Override
    public ClientConfig getConfiguration() {
        return requestContext.getClientConfig();
    }

    private ClientException convertToException(Response response) {
        try {
            WebApplicationException webAppException;
            final int statusCode = response.getStatus();
            final Response.Status status = Response.Status.fromStatusCode(statusCode);

            if (status == null) {
                final Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
                webAppException = createExceptionForFamily(response, statusFamily);
            } else switch (status) {
                case BAD_REQUEST:
                    webAppException = new BadRequestException(response);
                    break;
                case UNAUTHORIZED:
                    webAppException = new NotAuthorizedException(response);
                    break;
                case NOT_FOUND:
                    webAppException = new NotFoundException(response);
                    break;
                case METHOD_NOT_ALLOWED:
                    webAppException = new NotAllowedException(response);
                    break;
                case NOT_ACCEPTABLE:
                    webAppException = new NotAcceptableException(response);
                    break;
                case UNSUPPORTED_MEDIA_TYPE:
                    webAppException = new NotSupportedException(response);
                    break;
                case INTERNAL_SERVER_ERROR:
                    webAppException = new InternalServerErrorException(response);
                    break;
                case SERVICE_UNAVAILABLE:
                    webAppException = new ServiceUnavailableException(response);
                    break;
                default:
                    final Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
                    webAppException = createExceptionForFamily(response, statusFamily);
            }

            return new ClientException(webAppException);
        } catch (Throwable t) {
            return new ClientException(LocalizationMessages.RESPONSE_TO_EXCEPTION_CONVERSION_FAILED(), t);
        }
    }

    private WebApplicationException createExceptionForFamily(Response response, Response.Status.Family statusFamily) {
        WebApplicationException webAppException;
        switch (statusFamily) {
            case REDIRECTION:
                webAppException = new RedirectionException(response);
                break;
            case CLIENT_ERROR:
                webAppException = new ClientErrorException(response);
                break;
            case SERVER_ERROR:
                webAppException = new ServerErrorException(response);
                break;
            default:
                webAppException = new WebApplicationException(response);
        }
        return webAppException;
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
