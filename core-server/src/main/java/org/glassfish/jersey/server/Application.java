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
import org.glassfish.jersey.process.internal.InflectorNotFoundException;
import org.glassfish.jersey.process.internal.InvocationCallback;
import org.glassfish.jersey.process.internal.InvocationContext;
import org.glassfish.jersey.process.internal.LinearAcceptor;
import org.glassfish.jersey.process.internal.PreMatchRequestFilterAcceptor;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.server.model.RuntimeModelProvider;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
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
import javax.ws.rs.HttpMethod;

/**
 * Jersey server-side application.
 *
 * Container implementations use the application to instance to process requests
 * by invoking the {@link #apply(Request, ContainerResponseWriter) apply(request, responseWriter)}
 * method on a configured application instance.
 *
 * @see ResourceConfig
 * @see org.glassfish.jersey.server.spi.ContainerProvider
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class Application implements Inflector<Request, Future<Response>> {

    /**
     * Jersey application builder that provides programmatic API for creating
     * server-side JAX-RS / Jersey applications. The programmatic API complements
     * the annotation-based resource API defined by JAX-RS.
     * <p />
     * A typical use case for the programmatic resource binding API is demonstrated
     * by the following example:
     *
     * <pre>  Application.Builder appBuilder = Application.builder();
     *
     *  appBuilder.bind("a")
     *     .method("GET").to(new Inflector&lt;Request, Response&gt;() {
     *          &#64;Override
     *          public Response apply(Request data) {
     *              // ...GET "/a" request method processing
     *          }
     *      })
     *      .method("HEAD", "OPTIONS").to(new Inflector&lt;Request, Response&gt;() {
     *          &#64;Override
     *          public Response apply(Request data) {
     *              // ...HEAD & OPTIONS "/a" request methods processing
     *          }
     *      });
     *  appBuilder
     *     .bind("b")
     *         .method("GET").to(new Inflector&lt;Request, Response&gt;() {
     *              &#64;Override
     *              public Response apply(Request data) {
     *                  // ...GET "/b" request method processing
     *              }
     *          })
     *          .subPath("c")
     *             .method("GET").to(new Inflector&lt;Request, Response&gt;() {
     *                  &#64;Override
     *                  public Response apply(Request data) {
     *                      // ...GET "/b/c" request method processing
     *                  }
     *              });
     *
     *  appBuilder.build();</pre>
     *
     * The application built in the example above is equivalent to an
     * application that contains the following annotation-based resources:
     *
     * <pre>  &#64;Path("a")
     *  public class ResourceA {
     *
     *      &#64;GET
     *      public Response get(Request request) { ... }
     *
     *      &#64;OPTIONS &#64;HEAD
     *      public Response optionsAndHead(Request request) { ... }
     *  }
     *
     *  &#64;Path("b")
     *  public class ResourceB {
     *
     *      &#64;GET
     *      public Response getB(Request request) { ... }
     *
     *      &#64;Path("c")
     *      &#64;GET
     *      public Response getBC(Request request) { ... }
     *  }
     * </pre>
     *
     */
    public interface Builder {

        /**
         * Bind a new resource to a path within the application.
         *
         * The method is an entry point to the Jersey application builder
         * fluent programmatic resource binding API. It is equivalent to placing
         * {@link javax.ws.rs.Path &#64;Path} annotation on an annotation-based
         * resource class. See the {@link Builder application builder example} for
         * more information.
         * <p />
         * When invoked, the application builder creates a new {@link BoundBuilder
         * bound resource builder} that is bound to the supplied {@code path},
         * relative to the base application URI.
         *
         * @param path resource path relative to the base application URI.
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
             * Bind previously specified method(s) to provided request-to-response
             * {@link Inflector inflector} instance.
             * <p />
             * Invoking is method is equivalent to defining a resource method
             * in an annotation-based resource class. See the {@link Builder
             * application builder example} for more information.
             *
             * @param inflector request to response transformation implemented
             *     as an {@link Inflector Inflector&lt;Request, Response&gt;}.
             * @return {@link BoundBuilder parent bound builder} that can be used to
             *     add more resource methods or sub-resource locators.
             */
            public Builder.BoundBuilder to(Inflector<Request, Response> inflector);

            /**
             * Bind previously specified method(s) to provided request-to-response
             * {@link Inflector inflector} class.
             * <p />
             * Invoking is method is equivalent to defining a resource method
             * in an annotation-based resource class. See the {@link Builder
             * application builder example} for more information.
             *
             * @param inflectorClass request to response transformation implemented
             *     as an {@link Inflector Inflector&lt;Request, Response&gt;}.
             * @return {@link BoundBuilder parent bound builder} that can be used to
             *     add more resource methods or sub-resource locators.
             */
            public Builder.BoundBuilder to(Class<? extends Inflector<Request, Response>> inflectorClass);

            /**
             * Set supported response media types on a resource method.
             * Overrides any previously set values.
             * <p />
             * Invoking is method is equivalent to placing {@link javax.ws.rs.Produces
             * &#64;Produces} annotation on a resource method in an annotation-based
             * resource class. See the {@link Builder application builder example}
             * for more information.
             *
             * @param mediaTypes supported response media types.
             * @return {@link ResourceMethodBuilder} updated builder instance}.
             */
            public Builder.ResourceMethodBuilder produces(MediaType... mediaTypes);

            /**
             * Set accepted request media types on a resource method.
             * Overrides any previously set values.
             * <p />
             * Invoking is method is equivalent to placing {@link javax.ws.rs.Consumes
             * &#64;Consumes} annotation on a resource method in an annotation-based
             * resource class. See the {@link Builder application builder example}
             * for more information.
             *
             * @param mediaTypes accepted request media types.
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
             * If any of the specified methods has already been bound earlier, the
             * previous method binding will be overridden.
             * <p />
             * Invoking is method is equivalent to placing a {@link javax.ws.rs.HttpMethod
             * http method meta-annotated} annotation on a resource method in an
             * annotation-based resource class. See the {@link Builder application
             * builder example} for more information.
             *
             * @param methods set of HTTP methods to be bound. Any duplicate values
             *     will be automatically discarded.
             * @return configured {@link ResourceMethodBuilder resource method builder}
             *     instance.
             */
            public Builder.ResourceMethodBuilder method(String... methods);

            /**
             * Set supported response media types (equivalent of {@link javax.ws.rs.Produces})
             * for the current path. Overrides any previously set values.
             * <p />
             * Invoking is method is equivalent to placing {@link javax.ws.rs.Produces
             * &#64;Produces} annotation on a resource class in an annotation-based
             * resource class. See the {@link Builder application builder example}
             * for more information.
             *
             * @param mediaTypes supported response media types.
             * @return {@link ResourceMethodBuilder} updated builder instance}.
             */
            public Builder.BoundBuilder produces(MediaType... mediaTypes);

            /**
             * Set supported request media types (equivalent of {@link javax.ws.rs.Consumes})
             * for the current path. Overrides any previously set values.
             * <p />
             * Invoking is method is equivalent to placing {@link javax.ws.rs.Consumes
             * &#64;Consumes} annotation on a resource class in an annotation-based
             * resource class. See the {@link Builder application builder example}
             * for more information.
             *
             * @param mediaTypes supported request media types.
             * @return {@link BoundBuilder} updated builder instance}.
             */
            public Builder.BoundBuilder consumes(MediaType... mediaTypes);

            /**
             * Append sub-path to the current path which can be used to bind new
             * sub-resource methods and locators.
             * <p />
             * Invoking is method is equivalent to putting {@link javax.ws.rs.Path
             * &#64;Path} annotation on a sub-resource method or sub-resource locator
             * in an annotation-based resource class. See the {@link Builder application
             * builder example} for more information.
             *
             * @param subPath path to be appended to the current path value.
             * @return {@link BoundBuilder updated builder instance} bound the the
             *     new path.
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
     * Invokes a request and returns the {@link Future response future}.
     *
     * @param request request data.
     * @return response future.
     */
    @Override
    public Future<Response> apply(Request request) {
        final TimingOutInvocationCallback callback = new TimingOutInvocationCallback() {

            @Override
            protected Response handleFailure(Throwable exception) {
                return Application.handleFailure(exception);
            }

            @Override
            protected Response handleTimeout(InvocationContext context) {
                return Application.prepareTimeoutResponse(context);
            }
        };

        apply(request, callback);

        return callback;
    }

    /**
     * Invokes a request. Supplied callback is notified about the invocation result.
     *
     * @param request request data.
     * @param callback request invocation callback called when the request
     *     transformation is done, suspended, resumed etc. Must not be {@code null}.
     */
    private void apply(Request request, InvocationCallback callback) {
        try {
            requestScope.enter();
            configureProviders();
            // FIXME: This must be moved into the acceptor chain otherwise exception mapping & possibly
            //        other stuff may not work!
            final Pair<Request, Optional<LinearAcceptor>> pair = preMatchFilterAcceptor.apply(request);
            invoker.apply(pair.left(), callback);
        } finally {
            requestScope.exit();
        }
    }

    /**
     * The main request/response processing entry point for Jersey container
     * implementations.
     *
     * The method invokes the request processing and uses the provided
     * {@link ContainerResponseWriter container context} to suspend & resume the
     * processing as well as write the response back to the container.
     *
     * @param request request data.
     * @param responseWriter request-scoped container context.
     */
    public void apply(final Request request, final ContainerResponseWriter responseWriter) {
        final ContainerResponseWriterCallback callback = new ContainerResponseWriterCallback(request, responseWriter) {

            @Override
            protected void writeResponse(Response response) {
                Application.this.writeResponse(responseWriter, request, response);
            }

            @Override
            protected void writeResponse(Throwable exception) {
                Application.this.writeResponse(
                        responseWriter, request, Application.handleFailure(exception));
            }

            @Override
            protected void writeTimeoutResponse(InvocationContext context) {
                Application.this.writeResponse(
                        responseWriter, request, Application.prepareTimeoutResponse(context));
            }
        };
        apply(request, callback);
        callback.suspendWriterIfRunning();
    }

    private static Response prepareTimeoutResponse(final InvocationContext context) {
        Response response = context.getResponse();
        if (response == null) {
            response = Response.serverError().entity("Request processing has timed out.").type(MediaType.TEXT_PLAIN).build();
        }
        return response;
    }

    private static Response handleFailure(Throwable failure) {
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

        return Response.status(statusCode).entity(message).type(MediaType.TEXT_PLAIN).build();
    }

    @SuppressWarnings("unchecked")
    private void writeResponse(ContainerResponseWriter writer, Request request, Response response) {
        try {
            final boolean entityExists = response.hasEntity();

            if (!HttpMethod.HEAD.equals(request.getMethod()) && response.getStatus() == 200 && !entityExists) {
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

                final OutputStream os = writer.writeResponseStatusAndHeaders(-1, response);

                final MessageBodyWriter bWriter = workers.getMessageBodyWriter(
                        entity.getClass(), entityType, outputAnnotations, outputType);
                bWriter.writeTo(
                        entity,
                        entity.getClass(),
                        entityType, outputAnnotations, outputType, response.getMetadata(), os);
            } else {
                writer.writeResponseStatusAndHeaders(0, response);
            }
        } catch (IOException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
            throw new MappableException(ex);
        } finally {
            writer.commit();
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
     * @see Module
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
