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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;

import javax.inject.Inject;

import org.glassfish.jersey.internal.ProviderBinder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.InvocationCallback;
import org.glassfish.jersey.process.internal.InvocationContext;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.spi.RequestExecutorsProvider;
import org.glassfish.jersey.spi.ResponseExecutorsProvider;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;

import com.google.common.collect.Sets;
import static com.google.common.base.Preconditions.checkState;

/**
 * Jersey implementation of {@link javax.ws.rs.client.Client JAX-RS JerseyClient}
 * contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyClient implements javax.ws.rs.client.Client {

    /**
     * {@link JerseyClient Jersey client} instance builder.
     */
    public static class Builder {

        private Inflector<ClientRequest, ClientResponse> connector;
        private final List<Binder> customBinders = new LinkedList<Binder>();

        /**
         * Package-private Jersey client builder constructor used by
         * {@link JerseyClientFactory}.
         */
        Builder() {
        }

        /**
         * Set Jersey client transport connector.
         *
         * @param connector client transport connector.
         * @return updated Jersey client builder.
         */
        public Builder transport(Inflector<ClientRequest, ClientResponse> connector) {
            this.connector = connector;
            return this;
        }

        /**
         * Register custom HK2 binders for the Jersey client.
         *
         * @param binders custom HK2 binders to be registered with the Jersey client.
         * @return updated Jersey client builder.
         */
        public Builder binders(Binder... binders) {
            if (binders != null && binders.length > 0) {
                Collections.addAll(this.customBinders, binders);
            }
            return this;
        }

        /**
         * Build a new Jersey client.
         *
         * @return new Jersey client.
         */
        public JerseyClient build() {
            return new JerseyClient(new JerseyConfiguration(), connector, customBinders);
        }

        /**
         * Build a new Jersey client using an additional custom configuration.
         *
         * @param configuration JAX-RS client configuration for the new Jersey
         *                      client.
         * @return new Jersey client.
         */
        public JerseyClient build(javax.ws.rs.client.Configuration configuration) {
            final JerseyConfiguration jerseyConfiguration;
            if (configuration instanceof JerseyConfiguration) {
                jerseyConfiguration = new JerseyConfiguration(configuration);
            } else {
                jerseyConfiguration = (JerseyConfiguration) configuration;
            }
            return new JerseyClient(jerseyConfiguration, connector, customBinders);
        }
    }

    private final JerseyConfiguration configuration;
    private final AtomicBoolean closedFlag;
    private Inflector<ClientRequest, ClientResponse> connector;
    private RequestInvoker<ClientRequest, ClientResponse> invoker;

    @Inject
    private RequestScope requestScope;

    /**
     * Create a new Jersey client instance.
     *
     * @param configuration jersey client configuration.
     * @param connector     transport connector. If {@code null}, the {@link HttpUrlConnector
     *                      default transport} will be used.
     * @param customBinders custom HK2 binders to be registered with the client.
     */
    protected JerseyClient(
            final JerseyConfiguration configuration,
            final Inflector<ClientRequest, ClientResponse> connector,
            final List<Binder> customBinders) {
        this.configuration = configuration;
        this.closedFlag = new AtomicBoolean(false);
        this.connector = (connector == null) ? new HttpUrlConnector() : connector;

        initialize(customBinders);
    }

    /**
     * Initialize the newly constructed client instance.
     *
     * @param customBinders list of custom {@link Binder HK2 binders}.
     */
    private void initialize(final List<Binder> customBinders) {
        final Binder[] jerseyBinders = new Binder[]{
                new ClientBinder()
        };

        final ServiceLocator serviceLocator;
        if (customBinders.isEmpty()) {
            serviceLocator = Injections.createLocator(jerseyBinders);
        } else {
            final Binder[] customBinderArray = customBinders.toArray(new Binder[customBinders.size()]);

            Binder[] binders = new Binder[jerseyBinders.length + customBinderArray.length];
            System.arraycopy(jerseyBinders, 0, binders, 0, jerseyBinders.length);
            System.arraycopy(customBinderArray, 0, binders, jerseyBinders.length, customBinderArray.length);

            serviceLocator = Injections.createLocator(binders);
        }

        final RequestProcessingInitializationStage workersInitializationStage =
                serviceLocator.createAndInitialize(RequestProcessingInitializationStage.class);
        final ClientFilteringStage filteringStage = serviceLocator.createAndInitialize(ClientFilteringStage.class);

        Stage<ClientRequest> rootStage = Stages
                .chain(workersInitializationStage)
                .to(filteringStage)
                .build(Stages.asStage(connector));

        bindExecutors(serviceLocator);
        this.invoker = Injections.getOrCreate(serviceLocator, ClientBinder.RequestInvokerBuilder.class).build(rootStage);
        serviceLocator.inject(this);
    }

    /**
     * Binds {@link RequestExecutorsProvider request executors} and {@link ResponseExecutorsProvider response executors}
     * to all provider interfaces and removes them from the configuration, so that there will not be bound again in the
     * {@link RequestProcessingInitializationStage}.
     *
     * @param locator HK2 service locator.
     */
    private void bindExecutors(ServiceLocator locator) {
        ProviderBinder providerBinder = locator.getService(ProviderBinder.class);
        Set<Class<?>> executors = Sets.newHashSet();
        for (Class<?> clazz : this.configuration.getProviderClasses()) {
            final Set<Class<?>> providerContracts = Providers.getProviderContracts(clazz);
            if (providerContracts.contains(RequestExecutorsProvider.class)
                    || providerContracts.contains(ResponseExecutorsProvider.class)) {
                executors.add(clazz);
            }
        }
        this.configuration.getProviderClasses().removeAll(executors);
        providerBinder.bindClasses(executors);


        Set<Object> executorInstances = Sets.newHashSet();
        for (Object instance : this.configuration.getProviderInstances()) {
            final Set<Class<?>> providerInterfaces = Providers.getProviderContracts(instance.getClass());
            if (providerInterfaces.contains(RequestExecutorsProvider.class)
                    || providerInterfaces.contains(ResponseExecutorsProvider.class)) {
                executorInstances.add(instance);
            }
        }

        this.configuration.getProviderInstances().removeAll(executorInstances);
        providerBinder.bindInstances(executorInstances);
    }

    /**
     * Submit a configured invocation for processing.
     *
     * @param requestContext request context to be processed (invoked).
     * @param callback       callback receiving invocation processing notifications.
     */
    /*package*/ void submit(final ClientRequest requestContext,
                            final javax.ws.rs.client.InvocationCallback<Response> callback) {

        requestScope.runInScope(
                new Runnable() {

                    @Override
                    public void run() {
                        invoker.apply(requestContext, new InvocationCallback<ClientResponse>() {

                            @Override
                            public void result(ClientResponse responseContext) {
                                final InboundJaxrsResponse jaxrsResponse = new InboundJaxrsResponse(responseContext);
                                callback.completed(jaxrsResponse);
                            }

                            @Override
                            public void failure(Throwable exception) {
                                // need to be fixed
                                callback.failed(exception instanceof InvocationException ?
                                        (InvocationException) exception
                                        : new InvocationException(exception.getMessage(), exception));
                            }

                            @Override
                            public void cancelled() {
                                // TODO implement client-side suspend event logic
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
        return configuration;
    }

    @Override
    public WebTarget target(String uri) throws IllegalArgumentException, NullPointerException {
        checkClosed();
        return new WebTarget(uri, this);
    }

    @Override
    public WebTarget target(URI uri) throws NullPointerException {
        checkClosed();
        return new WebTarget(uri, this);
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) throws NullPointerException {
        checkClosed();
        return new WebTarget(uriBuilder, this);
    }

    @Override
    public WebTarget target(Link link) throws NullPointerException {
        checkClosed();
        return new WebTarget(link, this);
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
        WebTarget t = new WebTarget(link, this);
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
        WebTarget t = new WebTarget(link, this);
        List<String> ps = link.getProduces();
        JerseyInvocation.Builder ib = t.request(ps.toArray(new String[ps.size()]));
        return ib.build(method, entity);
    }
}
