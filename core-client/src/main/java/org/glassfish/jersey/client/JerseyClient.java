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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.InvocationCallback;
import org.glassfish.jersey.process.internal.InvocationContext;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.base.Preconditions.checkState;

/**
 * Jersey implementation of {@link javax.ws.rs.client.Client JAX-RS JerseyClient}
 * contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyClient implements javax.ws.rs.client.Client {

    private static final class References {

        @Inject
        private ServiceProviders.Builder serviceProvidersBuilder;
        @Inject
        private Ref<JerseyConfiguration> configuration;
        @Inject
        private Ref<ServiceProviders> serviceProviders;
        @Inject
        private Ref<ExceptionMappers> exceptionMappers;
        @Inject
        private Ref<MessageBodyWorkers> messageBodyWorkers;
        @Inject
        private Ref<ContextResolvers> contextResolvers;
    }

    /**
     * {@link JerseyClient Jersey client} instance builder.
     */
    public static class Builder {

        private Inflector<Request, Response> transport;
        private final List<Module> customModules = new LinkedList<Module>();

        /**
         * Package-private Jersey client builder constructor used by
         * {@link JerseyClientFactory}.
         */
        Builder() {
        }

        /**
         * Set Jersey client transport.
         *
         * @param transport client transport.
         * @return updated Jersey client builder.
         */
        public Builder transport(Inflector<Request, Response> transport) {
            this.transport = transport;
            return this;
        }

        /**
         * Register custom HK2 modules for the Jersey client.
         * @param modules custom HK2 modules to be registered with the Jersey client.
         * @return updated Jersey client builder.
         */
        public Builder modules(Module... modules) {
            if (modules != null && modules.length > 0) {
                Collections.addAll(this.customModules, modules);
            }
            return this;
        }

        /**
         * Build a new Jersey client.
         *
         * @return new Jersey client.
         */
        public JerseyClient build() {
            return new JerseyClient(new JerseyConfiguration(), transport, customModules);
        }

        /**
         * Build a new Jersey client using an additional custom configuration.
         *
         * @param configuration JAX-RS client configuration for the new Jersey
         *     client.
         * @return new Jersey client.
         */
        public JerseyClient build(javax.ws.rs.client.Configuration configuration) {
            return new JerseyClient(new JerseyConfiguration(configuration), transport, customModules);
        }
    }
    /**
     * JerseyClient configuration
     */
    private final JerseyConfiguration jerseyConfiguration;
    private final AtomicBoolean closedFlag;
    //
    private RequestInvoker invoker;
    private Inflector<Request, Response> transport;
    private RequestScope requestScope;
    private Services services;

    protected JerseyClient(
            final JerseyConfiguration jerseyConfiguration,
            final Inflector<Request, Response> transport,
            final List<Module> customModules) {
        this.jerseyConfiguration = jerseyConfiguration;
        this.closedFlag = new AtomicBoolean(false);
        this.transport = transport;

        initialize(customModules);
    }

    /**
     * Initialize the newly constructed client instance
     *
     * @param customModules list of {@link Module}.
     */
    private void initialize(final List<Module> customModules) {
        final Module[] jerseyModules = new Module[]{
            new ClientModule(transport)
        };

        if (customModules.isEmpty()) {
            this.services = HK2.get().create(null, jerseyModules);
        } else {
            final Object[] customModulesArray = customModules.toArray();

            Module[] modules = new Module[jerseyModules.length + customModulesArray.length];
            System.arraycopy(jerseyModules, 0, modules, 0, jerseyModules.length);
            System.arraycopy(customModulesArray, 0, modules, jerseyModules.length, customModulesArray.length);

            this.services = HK2.get().create(null, modules);
        }

        this.requestScope = services.forContract(RequestScope.class).get();
        this.invoker = services.forContract(RequestInvoker.class).get();
    }

    /*package*/ListenableFuture<Response> submit(final JerseyInvocation invocation,
            final javax.ws.rs.client.InvocationCallback<? super javax.ws.rs.core.Response> callback) {
        return requestScope.runInScope(
                new RequestScope.Producer<ListenableFuture<Response>>() {

                    @Override
                    public ListenableFuture<Response> call() {
                        final Injector injector = services.forContract(Injector.class).get();
                        References refs = injector.inject(References.class);

                        final JerseyConfiguration cfg = invocation.configuration();
                        final ServiceProviders providers = refs.serviceProvidersBuilder
                                .setProviderClasses(cfg.getProviderClasses()).setProviderInstances(cfg.getProviderInstances())
                                .build();
                        final ExceptionMapperFactory mappers = new ExceptionMapperFactory(providers);
                        final MessageBodyWorkers workers = new MessageBodyFactory(providers);
                        final ContextResolvers resolvers = new ContextResolverFactory(providers);

                        refs.configuration.set(cfg);
                        refs.serviceProviders.set(providers);
                        refs.exceptionMappers.set(mappers);
                        refs.messageBodyWorkers.set(workers);
                        refs.contextResolvers.set(resolvers);

                        final Request request = injector.inject(invocation.request());
                        Map<String, Object> properties = new HashMap<String, Object>(configuration().getProperties());
                        properties.putAll(request.getProperties());
                        request.getProperties().putAll(properties);

                        return invoker.apply(request, new InvocationCallback() {

                            @Override
                            public void result(Response response) {
                                callback.completed(response);
                            }

                            @Override
                            public void failure(Throwable exception) {
                                // TODO JAX-RS client callback interface as well as invocation exception
                                // need to be fixed
                                callback.failed(new InvocationException(exception.getMessage(), exception));
                            }

                            @Override
                            public void cancelled() {
                                // TODO implement client-side cancel event logic
                            }

                            @Override
                            public void suspended(long time, TimeUnit unit, InvocationContext context) {
                                // TODO implement client-side suspend event logic
                            }

                            @Override
                            public void suspendTimeoutChanged(long time, TimeUnit unit) {
                                // TODO implement client-side suspend timeout change event logic
                            }

                            @Override
                            public void resumed() {
                                // TODO implement client-side resume event logic
                            }
                        });

                    }
                });

    }

    @Override
    public void close() {
        if (closedFlag.compareAndSet(false, true)) {
            release();
        }
    }

    private void release() {
        // TODO release resources
    }

    /**
     * Check client state.
     *
     * @return {@code true} if current {@link JerseyClient} instance is closed, otherwise {@code false}.
     * @see #close()
     */
    public boolean isClosed() {
        return closedFlag.get();
    }

    private void checkClosed() {
        checkState(!closedFlag.get(), "Client instance has been closed.");
    }

    @Override
    public JerseyConfiguration configuration() {
        checkClosed();
        return jerseyConfiguration;
    }

    @Override
    public Target target(String uri) throws IllegalArgumentException, NullPointerException {
        checkClosed();
        return new Target(uri, this);
    }

    @Override
    public Target target(URI uri) throws NullPointerException {
        checkClosed();
        return new Target(uri, this);
    }

    @Override
    public Target target(UriBuilder uriBuilder) throws NullPointerException {
        checkClosed();
        return new Target(uriBuilder, this);
    }

    @Override
    public Target target(Link link) throws NullPointerException {
        checkClosed();
        return new Target(link, this);
    }

    @Override
    public javax.ws.rs.client.Invocation invocation(Link link) throws NullPointerException, IllegalArgumentException {
        checkClosed();
        String method = link.getMethod();
        if (method == null) {
            throw new IllegalArgumentException("Cannot create invocation from link " + link);
        }
        if (POST.equals(method) || PUT.equals(method)) {
            throw new IllegalArgumentException("Missing entity in invocation created from link " + link);
        }
        Target t = new Target(link, this);
        List<String> ps = link.getProduces();
        JerseyInvocation.Builder ib = t.request(ps.toArray(new String[ps.size()]));
        return ib.build(method);
    }

    @Override
    public javax.ws.rs.client.Invocation invocation(Link link, Entity<?> entity)
            throws NullPointerException, IllegalArgumentException {
        checkClosed();
        String method = link.getMethod();
        if (method == null) {
            throw new IllegalArgumentException("Cannot create invocation from link " + link);
        }
        boolean isCompatible = false;
        for (String mt : link.getConsumes()) {
            if (entity.getMediaType().isCompatible(MediaType.valueOf(mt))) {
                isCompatible = true;
                break;
            }
        }
        if (!isCompatible) {
            throw new IllegalArgumentException("Entity type incompatible with link produces parameter");
        }
        Target t = new Target(link, this);
        List<String> ps = link.getProduces();
        JerseyInvocation.Builder ib = t.request(ps.toArray(new String[ps.size()]));
        return ib.build(method, entity);
    }
}
