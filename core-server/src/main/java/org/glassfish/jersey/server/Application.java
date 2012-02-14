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
package org.glassfish.jersey.server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import javax.annotation.Nullable;

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.LinearAcceptor;
import org.glassfish.jersey.process.internal.PreMatchRequestFilterAcceptor;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestInvoker.Callback;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.server.model.RuntimeModelProvider;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.DynamicBinderFactory;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.SettableFuture;
import org.glassfish.jersey.process.internal.InflectorNotFoundException;

/**
 * Jersey server-side application.
 *
 * @see {@link ResourceConfig}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class Application implements Inflector<Request, Future<Response>> {

    /**
     * Jersey application builder that provides programmatic API for creating
     * server-side JAX-RS / Jersey applications.
     */
    public interface Builder {

        /**
         * Bind a resource to a path within the application.
         * <p/>
         * TODO elaborate on javadoc.
         *
         * @param path resource path.
         * @return resource builder bound to the {@code path}.
         */
        Builder.BoundBuilder bind(String path);

        /**
         * Build new application based on the defined resource bindings and
         * provider configuration.
         *
         * @return new application instance.
         */
        org.glassfish.jersey.server.Application build();

        /**
         * Application builder used for binding a new resource method to an
         * {@link Inflector Inflector&lt;Request, Response&gt;} responsible for
         * processing requests targeted at the bound path and the particular
         * method(s).
         */
        public interface ResourceMethodBuilder {

            /**
             * Bind previously specified method(s) to provided transformation.
             *
             * @param transformation request to response transformation implemented
             * as an {@link Inflector Inflector&lt;Request, Response&gt;}.
             * @return {@link BoundBuilder parent bound builder} that can be used to
             * add more resource methods or sub-resource locators.
             */
            public Builder.BoundBuilder to(Inflector<Request, Response> transformation);

            public Builder.BoundBuilder to(Class<? extends Inflector<Request, Response>> transformationClass);

            /**
             * Set supported response media types (equivalent of {@link javax.ws.rs.Produces}).
             * Overrides any previously set values.
             *
             * @param mediaTypes supported response media types.
             * @return {@link ResourceMethodBuilder} updated builder instance}.
             */
            public Builder.ResourceMethodBuilder produces(MediaType... mediaTypes);

            /**
             * Set supported request media types (equivalent of {@link javax.ws.rs.Consumes}).
             * Overrides any previously set values.
             *
             * @param mediaTypes supported request media types.
             * @return {@link ResourceMethodBuilder} updated builder instance}.
             */
            public Builder.ResourceMethodBuilder consumes(MediaType... mediaTypes);
        }

        /**
         * Represents a supported resource path to which new resource methods and
         * sub-resource locators can be attached.
         */
        public interface BoundBuilder {

            /**
             * Bind new HTTP methods to the path previously configured in this
             * {@link BoundBuilder builder}.
             * <p/>
             * If any of the specified methods has already been bound earlier, the
             * previous method binding will be overridden.
             *
             * @param methods set of HTTP methods to be bound. Any duplicate values
             * will be automatically discarded.
             * @return configured {@link ResourceMethodBuilder resource method builder}
             * instance.
             */
            public Builder.ResourceMethodBuilder method(String... methods);

            /**
             * Set supported response media types (equivalent of {@link javax.ws.rs.Produces})
             * fir the current path. Overrides any previously set values.
             *
             * @param mediaTypes supported response media types.
             * @return {@link ResourceMethodBuilder} updated builder instance}.
             */
            public Builder.BoundBuilder produces(MediaType... mediaTypes);

            /**
             * Set supported request media types (equivalent of {@link javax.ws.rs.Consumes})
             * for the current path. Overrides any previously set values.
             *
             * @param mediaTypes supported request media types.
             * @return {@link BoundBuilder} updated builder instance}.
             */
            public Builder.BoundBuilder consumes(MediaType... mediaTypes);

            /**
             * Append sub-path to the current path which can be used to bind new
             * sub-resource locators.
             *
             * @param subPath path to be appended to the current path value.
             * @return {@link BoundBuilder updated builder instance} bound the the
             * new path.
             */
            public Builder.BoundBuilder subPath(String subPath);
        }
    }

    /**
     * Create new application builder configured with custom {@link ResourceConfig}.
     *
     * @return new application builder.
     */
    public static Builder builder() {
        return new ApplicationBuilder(null);
    }

    /**
     * Create new application builder configured with custom {@link ResourceConfig}.
     *
     * @param resourceConfig custom {@link ResourceConfig}
     * @return new application builder.
     */
    public static Builder builder(@Nullable ResourceConfig resourceConfig) {
        return new ApplicationBuilder(resourceConfig);
    }

    private class ApplicationModule extends AbstractModule implements Factory<TreeAcceptor> {

        @Override
        public TreeAcceptor get() {
            return Application.this.rootAcceptor;
        }

        @Override
        public void configure() {
            bind(Application.class).toInstance(Application.this);
            bind(TreeAcceptor.class).annotatedWith(Stage.Root.class).toFactory(this);
            bind().to(PreMatchRequestFilterAcceptor.class);
        }
    }

    private static final class References {

        @Inject
        private ServiceProviders.Builder serviceProvidersBuilder;
        @Inject
        private Ref<ResourceConfig> configuration;
        @Inject
        private Ref<ServiceProviders> serviceProviders;
        @Inject
        private Ref<ExceptionMappers> exceptionMappers;
        @Inject
        private Ref<MessageBodyWorkers> messageBodyWorkers;
        @Inject
        private Ref<ContextResolvers> contextRespolvers;
    }
    // FIXME move filter acceptor away from here! It must be part of the root acceptor chain!
    @Inject
    private PreMatchRequestFilterAcceptor preMatchFilterAcceptor;
    @Inject
    private Services services;
    @Inject
    private RequestScope requestScope;
    @Inject
    private RequestInvoker invoker;
    @Inject
    private Factory<RouterModule.RoutingContext> routingContextFactory;
    @Inject
    private Factory<MessageBodyWorkers> mbwFactory;
    @Inject
    private References references;
    //
    private volatile TreeAcceptor rootAcceptor;
    private final ApplicationModule applicationModule = new ApplicationModule();
    //
    private boolean dirtyProviders = true; // set to true to allow lazy initialization of messageBodyWorkers
    private final Monitor providersMonitor = new Monitor();
    private final Monitor.Guard providersConfigChanged = new Monitor.Guard(providersMonitor) {

        @Override
        public boolean isSatisfied() {
            return dirtyProviders;
        }
    };

    /*package*/ Module module() {
        return applicationModule;
    }

    /*package*/ void setModelProvider(RuntimeModelProvider modelProvider) {
        this.rootAcceptor = modelProvider.getRuntimeModel();
    }

    /*package*/ void setRootAcceptor(TreeAcceptor root) {
        this.rootAcceptor = root;
    }

    /**
     * Invokes a request and returns the response {@link Future future}.
     *
     * @param request request data.
     * @return response future.
     */
    @Override
    public Future<Response> apply(Request request) {
        final SettableFuture<Response> responseFuture = SettableFuture.create();
        apply(request, new Callback() {

            @Override
            public void result(Response response) {
                responseFuture.set(response);
            }

            @Override
            public void failure(Throwable exception) {
                responseFuture.set(handleFailure(exception));
            }
        });

        return responseFuture;
    }

    /**
     * Invokes a request and returns the response {@link Future future}.
     *
     * @param request request data.
     * @param callback response callback called when the request transformation is done.
     *     Must not be {@code null}.
     * @return response future.
     */
    public Future<Response> apply(Request request, Callback callback) {
        try {
            requestScope.enter();
            configureProviders();
            // FIXME: This must be moved into the acceptor chain otherwise exception mapping & possibly
            //        other stuff may not work!
            final Pair<Request, Optional<LinearAcceptor>> pair = preMatchFilterAcceptor.apply(request);
            return invoker.apply(pair.left(), callback);
        } finally {
            requestScope.exit();
        }
    }

    /**
     * Invokes a request writes the response to provided {@link ContainerResponseWriter writer}.
     *
     * @param request request data.
     * @param writer where response will be written
     */
    public void apply(final Request request, final ContainerResponseWriter writer) {
        apply(request, new Callback() {

            @Override
            public void result(Response response) {
                writeResponse(writer, request, response);
            }

            @Override
            public void failure(Throwable exception) {
                writeResponse(writer, request, handleFailure(exception));
            }
        });
    }

    private Response handleFailure(Throwable failure) {
        Response.StatusType statusCode = Response.Status.INTERNAL_SERVER_ERROR;
        String message = failure.getMessage();

        if (failure instanceof ProcessingException) {
            if (failure instanceof HeaderValueException) {
                statusCode = Response.Status.BAD_REQUEST;
                // keep exception message
            } else if (failure instanceof InflectorNotFoundException) {
                statusCode = Response.Status.NOT_FOUND;
                message = "Requested resource not found.";
            }

            // TODO better handle other processing exception
        }

        if (statusCode == Response.Status.INTERNAL_SERVER_ERROR) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, message, failure);
        } else {
            Logger.getLogger(Application.class.getName()).log(Level.FINE, message, failure);
        }

        return Response.status(statusCode).entity(message).type("text/plain").build();
    }

    @SuppressWarnings("unchecked")
    private void writeResponse(ContainerResponseWriter writer, Request request, Response response) {
        try {
            final boolean entityExists = response.hasEntity();

            if (response.getStatus() == 200 && !entityExists) {
                response = Response.fromResponse(response).status(204).build();
            }

            final MessageBodyWorkers workers = Requests.getMessageWorkers(request);
            if (entityExists) {
                final Object entity = response.getEntity();
                final RoutingContext routingContext = routingContextFactory.get();

                final MediaType outputType = routingContext.getEffectiveAcceptableType();
                final Annotation[] outputAnnotations = routingContext.getResponseMethodAnnotations();
                Type entityType = routingContext.getResponseMethodType();
                if (entityType == null) {
                    final Type genericSuperclass = entity.getClass().getGenericSuperclass();
                    entityType = (genericSuperclass instanceof ParameterizedType) ? genericSuperclass : entity.getClass();
                }

                final OutputStream os = writer.writeStatusAndHeaders(-1, response);

                final MessageBodyWriter bWriter = workers.getMessageBodyWriter(
                        entity.getClass(), entityType, outputAnnotations, outputType);
                bWriter.writeTo(
                        entity,
                        entity.getClass(),
                        entityType, outputAnnotations, outputType, response.getMetadata(), os);
            } else {
                writer.writeStatusAndHeaders(0, response);
            }
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            throw new MappableException(ex);
        }
    }

    /**
     * Returns {@link Services} relevant to current application.
     *
     * @return {@link Services} instance.
     */
    public Services getServices() {
        return services;
    }

    public ServiceProviders getServiceProviders() {
        configureProviders();
        return references.serviceProviders.get();
    }

    private void configureProviders() {
        if (providersMonitor.enterIf(providersConfigChanged)) {
            try {
                final ResourceConfig rc = references.configuration.get();

                final ServiceProviders providers = references.serviceProvidersBuilder.setProviderClasses(rc.getClasses()).setProviderInstances(rc.getSingletons()).build();
                final MessageBodyWorkers workers = new MessageBodyFactory(providers);
                final ExceptionMappers mappers = new ExceptionMapperFactory(providers);
                final ContextResolvers resolvers = new ContextResolverFactory(providers);

                references.serviceProviders.set(providers);
                references.messageBodyWorkers.set(workers);
                references.exceptionMappers.set(mappers);
                references.contextRespolvers.set(resolvers);

                dirtyProviders = false;
            } finally {
                providersMonitor.leave();
            }
        }
    }

    private void runUpdateInProvidersMonitor(Runnable task) {
        providersMonitor.enter();
        try {
            task.run();
            dirtyProviders = true;
        } finally {
            providersMonitor.leave();
        }
    }

    void setResourceConfig(final ResourceConfig newValue) {
        runUpdateInProvidersMonitor(new Runnable() {

            @Override
            public void run() {
                references.configuration.set(newValue);
            }
        });
    }

    /**
     * Dynamically binds HK2 modules.
     *
     * @see {@link Module}
     *
     * @param modules newly bound modules.
     */
    public void addModules(final Module... modules) {
        runUpdateInProvidersMonitor(new Runnable() {

            @Override
            public void run() {
                final Injector injector = services.forContract(Injector.class).get();

                for (Module module : modules) {
                    injector.inject(module);
                    final DynamicBinderFactory dynamicBinderFactory = services.bindDynamically();
                    module.configure(dynamicBinderFactory);
                    dynamicBinderFactory.commit();
                }
            }
        });
    }
}
