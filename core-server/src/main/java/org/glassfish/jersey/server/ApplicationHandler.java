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
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.process.internal.InflectorNotFoundException;
import org.glassfish.jersey.process.internal.InvocationCallback;
import org.glassfish.jersey.process.internal.InvocationContext;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.routing.RoutedInflectorExtractorStage;
import org.glassfish.jersey.server.internal.routing.Router;
import org.glassfish.jersey.server.internal.routing.RoutingStage;
import org.glassfish.jersey.server.internal.routing.RuntimeModelBuilder;
import org.glassfish.jersey.server.model.BasicValidator;
import org.glassfish.jersey.server.model.ModelValidationException;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModelIssue;
import org.glassfish.jersey.server.model.ResourceModelValidator;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;
import org.glassfish.jersey.spi.CloseableService;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.DynamicBinderFactory;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Jersey server-side application handler.
 * <p/>
 * Container implementations use the {@code ApplicationHandler} API to process requests
 * by invoking the {@link #handle(JerseyContainerRequestContext) handle(requestContext)}
 * method on a configured application  handler instance.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see ResourceConfig
 * @see org.glassfish.jersey.server.spi.ContainerProvider
 */
public final class ApplicationHandler {

    private static final Logger LOGGER = Logger.getLogger(ApplicationHandler.class.getName());
    /**
     * Default dummy security context.
     */
    private static final SecurityContext DEFAULT_SECURITY_CONTEXT = new SecurityContext() {

        @Override
        public boolean isUserInRole(String role) {
            return false;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getAuthenticationScheme() {
            return null;
        }
    };

    private static class References {

        @Inject
        private Ref<ServiceProviders> providers;
        @Inject
        private Ref<ExceptionMappers> mappers;
        @Inject
        private Ref<MessageBodyWorkers> workers;
        @Inject
        private Ref<ContextResolvers> resolvers;
    }

    private class ApplicationModule extends AbstractModule {

        private class JaxrsApplicationProvider implements Factory<Application> {

            @Override
            public Application get() throws ComponentException {
                return ApplicationHandler.this.configuration.getApplication();
            }
        }

        private class ResourceConfigProvider implements Factory<ResourceConfig> {

            @Override
            public ResourceConfig get() throws ComponentException {
                return ApplicationHandler.this.configuration;
            }
        }

        @Override
        protected void configure() {
            ResourceConfigProvider rcp = new ResourceConfigProvider();
            bind(ResourceConfig.class).toFactory(rcp).in(Singleton.class);
            bind(FeaturesAndProperties.class).toFactory(rcp).in(Singleton.class);

            bind(Application.class).toFactory(new JaxrsApplicationProvider()).in(Singleton.class);

            bind(ApplicationHandler.class).toInstance(ApplicationHandler.this);
        }
    }

    @Inject
    private RequestScope requestScope;
    @Inject
    private Factory<CloseableService> closeableServiceFactory;
    //
    private Services services;
    /**
     * Request invoker.
     */
    private RequestInvoker<JerseyContainerRequestContext, JerseyContainerResponseContext> invoker;
    private final ResourceConfig configuration;
    private References refs;

    /**
     * Create a new Jersey application handler using a default configuration.
     */
    public ApplicationHandler() {
        initServices();
        this.configuration = new ResourceConfig();
        initialize();
    }

    /**
     * Create a new Jersey server-side application handler configured by a
     * {@link Application JAX-RS Application (sub-)class}.
     *
     * @param jaxrsApplicationClass JAX-RS {@code Application} (sub-)class that will be
     *                              instantiated and used to configure the new Jersey
     *                              application handler.
     */
    public ApplicationHandler(Class<? extends Application> jaxrsApplicationClass) {
        initServices();
        if (ResourceConfig.class.isAssignableFrom(jaxrsApplicationClass)) {
            this.configuration = (ResourceConfig) createApplication(jaxrsApplicationClass);
        } else {
            this.configuration = ResourceConfig.forApplicationClass(jaxrsApplicationClass);
        }
        initialize();
    }

    /**
     * Create a new Jersey server-side application handler configured by an instance
     * of a {@link Application JAX-RS Application sub-class}.
     *
     * @param application an instance of a JAX-RS {@code Application} (sub-)class that
     *                    will be used to configure the new Jersey application handler.
     */
    public ApplicationHandler(Application application) {
        initServices();
        this.configuration = ResourceConfig.forApplication(application);
        initialize();
    }

    private void initServices() {
        // TODO parent/child services - when HK2 bec ready:
        //  this.jerseyServices = HK2.get().build(null, jerseyModules);
        //  this.services = HK2.get().build(jerseyServices, customModules);
        services = HK2.get().create(null, new ServerModule(), new ApplicationModule());
    }

    /**
     * Assumes the configuration field is initialized with a valid ResourceConfig.
     */
    private void initialize() {
        registerAdditionalModules(configuration.getCustomModules());

        final Class<? extends Application> applicationClass = configuration.getApplicationClass();
        if (applicationClass != null) {
            final Application application = createApplication(applicationClass);
            if (application instanceof ResourceConfig) {
                // (JERSEY-1094) If the application is an instance of ResourceConfig then register it's
                // custom modules into the HK2 service registry so they can be used right away.
                registerAdditionalModules(((ResourceConfig) application).getCustomModules());
            }
            configuration.setApplication(application);
        }

        configuration.lock();

        final List<ResourceModelIssue> resourceModelIssues = Lists.newLinkedList();

        final Map<String, Resource.Builder> pathToResourceBuilderMap = Maps.newHashMap();
        final List<Resource.Builder> resourcesBuilders = new LinkedList<Resource.Builder>();
        for (Class<?> c : configuration.getClasses()) {
            Path path = Resource.getPath(c);
            if (path != null) { // root resource
                try {
                    final String pathValue = path.value();
                    final Resource.Builder builder = Resource.builder(c, resourceModelIssues);

                    Resource.Builder existing = pathToResourceBuilderMap.get(pathValue);
                    if (existing != null) {
                        existing.mergeWith(builder);
                    } else {
                        resourcesBuilders.add(builder);
                        pathToResourceBuilderMap.put(pathValue, builder);
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.warning(ex.getMessage());
                }
            }
        }

        for (Resource programmaticResource : configuration.getResources()) {
            Resource.Builder builder = pathToResourceBuilderMap.get(programmaticResource.getPath());
            if (builder != null) {
                builder.mergeWith(programmaticResource);
            } else {
                resourcesBuilders.add(Resource.builder(programmaticResource));
            }
        }

        final Injector injector = services.forContract(Injector.class).get();
        this.refs = injector.inject(References.class);

        final ServiceProviders providers = services.forContract(ServiceProviders.Builder.class).get()
                .setProviderClasses(Sets.filter(configuration.getClasses(), new Predicate<Class<?>>() {

                    @Override
                    public boolean apply(Class<?> input) {
                        final boolean acceptable = Resource.isAcceptable(input);
                        if (!acceptable) {
                            LOGGER.warning(LocalizationMessages.NON_INSTANTIATABLE_CLASS(input));
                        }
                        return acceptable;
                    }
                }))
                .setProviderInstances(configuration.getSingletons()).build();
        this.refs.providers.set(providers);

        final MessageBodyFactory workers = new MessageBodyFactory(providers);
        this.refs.workers.set(workers);
        this.refs.mappers.set(new ExceptionMapperFactory(providers));
        this.refs.resolvers.set(new ContextResolverFactory(providers));

        List<Resource> resources = buildAndValidate(resourcesBuilders, resourceModelIssues, workers);

        final RuntimeModelBuilder runtimeModelBuilder =
                services.byType(RuntimeModelBuilder.class).get();
        runtimeModelBuilder.setWorkers(workers);
        for (Resource r : resources) {
            runtimeModelBuilder.process(r);
        }

        // assembly request processing chain
        /**
         * Root hierarchical request matching acceptor.
         * Invoked in a single linear stage as part of the main linear accepting chain.
         */
        final Router resourceRoutingRoot = runtimeModelBuilder.buildModel();

        final ContainerFilteringStage preMatchRequestFilteringStage =
                injector.inject(ContainerFilteringStage.Builder.class).build(true);
        final RoutingStage routingStage =
                injector.inject(RoutingStage.Builder.class).build(resourceRoutingRoot);
        final ContainerFilteringStage resourceFilteringStage =
                injector.inject(ContainerFilteringStage.Builder.class).build(false);
        final RoutedInflectorExtractorStage routedInflectorExtractorStage = injector.inject(RoutedInflectorExtractorStage.class);
        /**
         *  Root linear request acceptor. This is the main entry point for the whole request processing.
         */
        final Stage<JerseyContainerRequestContext> rootStage = Stages
                .chain(injector.inject(ReferencesInitializer.class))
                .to(injector.inject(ContainerMessageBodyWorkersInitializer.class))
                .to(preMatchRequestFilteringStage)
                .to(routingStage)
                .to(resourceFilteringStage)
                .build(routedInflectorExtractorStage);

        this.invoker = injector.inject(ServerModule.RequestInvokerBuilder.class)
                .build(rootStage);

        // inject self
        injector.inject(this);
    }

    private Application createApplication(Class<? extends Application> applicationClass) {
        // need to handle ResourceConfig and Application separately as invoking forContract() on these
        // will trigger the factories which we don't want at this point
        if (applicationClass == ResourceConfig.class) {
            return new ResourceConfig();
        } else if (applicationClass == Application.class) {
            return new Application();
        } else {
            return services.forContract(applicationClass).get();
        }
    }

    /**
     * Registers modules into the HK2 service register.
     *
     * @param modules Modules to be registered.
     */
    public void registerAdditionalModules(final Set<Module> modules) {
        final DynamicBinderFactory dynamicBinderFactory = services.bindDynamically();

        for (Module module : modules) {
            module.configure(dynamicBinderFactory);
        }
        dynamicBinderFactory.commit();
    }

    private List<Resource> buildAndValidate(List<Resource.Builder> resources, List<ResourceModelIssue> modelIssues, MessageBodyWorkers workers) {
        final List<Resource> result = new ArrayList<Resource>(resources.size());

        ResourceModelValidator validator = new BasicValidator(modelIssues, workers);

        for (Resource.Builder rb : resources) {
            final Resource r = rb.build();
            result.add(r);
            validator.validate(r);
        }
        processIssues(validator);

        return result;
    }

    private void processIssues(ResourceModelValidator validator) {

        final List<ResourceModelIssue> issueList = validator.getIssueList();
        if (!issueList.isEmpty()) {
            final String allIssueMessages = allIssueLogMessages(validator.getIssueList());
            if (validator.fatalIssuesFound()) {
                LOGGER.severe(
                        LocalizationMessages.ERRORS_AND_WARNINGS_DETECTED_WITH_RESOURCE_CLASSES(allIssueMessages));
            } else {
                LOGGER.warning(
                        LocalizationMessages.WARNINGS_DETECTED_WITH_RESOURCE_CLASSES(allIssueMessages));
            }
        }

        if (validator.fatalIssuesFound()) {
            throw new ModelValidationException(issueList);
        }
    }

    private String allIssueLogMessages(final List<ResourceModelIssue> issueList) {
        StringBuilder errors = new StringBuilder("\n");
        StringBuilder warnings = new StringBuilder();

        for (ResourceModelIssue issue : issueList) {
            if (issue.isFatal()) {
                errors.append(LocalizationMessages.ERROR_MSG(issue.getMessage())).append('\n');
            } else {
                warnings.append(LocalizationMessages.WARNING_MSG(issue.getMessage())).append('\n');
            }
        }

        return errors.append(warnings).toString();
    }

    /**
     * Invokes a request and returns the {@link Future response future}.
     *
     * @param requestContext request data.
     * @return response future.
     */
    public Future<JerseyContainerResponseContext> apply(final JerseyContainerRequestContext requestContext) {
        return apply(requestContext, new OutputStream() {
            @Override
            public void write(int i) throws IOException {
                // dummy
            }
        });
    }

    /**
     * Invokes a request and returns the {@link Future response future}.
     *
     * @param requestContext request data.
     * @param outputStream response output stream.
     * @return response future.
     */
    public Future<JerseyContainerResponseContext> apply(final JerseyContainerRequestContext requestContext,
                                                        final OutputStream outputStream) {
        requestContext.setSecurityContext(DEFAULT_SECURITY_CONTEXT);
        final ContainerResponseWriter containerResponseWriter = new ContainerResponseWriter() {
            @Override
            public OutputStream writeResponseStatusAndHeaders(long contentLength, JerseyContainerResponseContext responseContext)
                    throws ContainerException {

                if (contentLength >= 0) {
                    responseContext.getHeaders().putSingle("Content-Length", Long.toString(contentLength));
                }

                return outputStream;
            }

            @Override
            public void suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) throws IllegalStateException {
            }

            @Override
            public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
            }

            @Override
            public void cancel() {
            }

            @Override
            public void commit() {
            }
        };

        final TimingOutInvocationCallback callback = new TimingOutInvocationCallback() {

            @Override
            protected JerseyContainerResponseContext handleResponse(JerseyContainerResponseContext responseContext) {
                ApplicationHandler.this.writeResponse(containerResponseWriter, requestContext, responseContext);
                if (HttpMethod.HEAD.equals(requestContext.getMethod())) {
                    // for testing purposes:
                    // need to also strip the object entity as it was stripped writeResponse(...)
                    stripEntity(responseContext);
                }
                return responseContext;
            }

            @Override
            protected JerseyContainerResponseContext handleFailure(Throwable exception) {
                final JerseyContainerResponseContext response = ApplicationHandler.handleFailure(exception, requestContext);
                ApplicationHandler.this.writeResponse(containerResponseWriter, requestContext, response);
                return response;
            }

            @Override
            protected JerseyContainerResponseContext handleTimeout(InvocationContext context) {
                final JerseyContainerResponseContext response =
                        ApplicationHandler.prepareTimeoutResponse(context, requestContext);
                ApplicationHandler.this.writeResponse(containerResponseWriter, requestContext, response);
                return response;
            }
        };

        apply(requestContext, callback);

        return callback;
    }

    /**
     * Strips entity if present.
     *
     * @param responseContext processed response context.
     * @return original response without entity.
     */
    private JerseyContainerResponseContext stripEntity(final JerseyContainerResponseContext responseContext) {
        if (responseContext.hasEntity()) {
            responseContext.setEntity(null);
        }

        return responseContext;
    }

    /**
     * The main request/response processing entry point for Jersey container implementations.
     * <p>
     * The method invokes the request processing of the provided
     * {@link JerseyContainerRequestContext container request context} and uses the
     * {@link ContainerResponseWriter container response writer} to suspend & resume the processing
     * as well as write the response back to the container.
     * </p>
     * <p>
     * The the {@link SecurityContext security context} stored in the container request context
     * is bound as an injectable instance in the scope of the processed request context.
     * Also, any {@link RequestScopedInitializer cCustom scope injections} are initialized in the
     * current request scope.
     * </p>
     *
     * @param requestContext container request context of the current request.
     */
    public void handle(final JerseyContainerRequestContext requestContext) {
        checkContainerRequestContext(requestContext);

        final ContainerResponseWriterCallback callback = new ContainerResponseWriterCallback(requestContext) {

            @Override
            protected void writeResponse(JerseyContainerResponseContext response) {
                ApplicationHandler.this.writeResponse(requestContext.getResponseWriter(), requestContext, response);
            }

            @Override
            protected void writeResponse(Throwable exception) {
                ApplicationHandler.this.writeResponse(requestContext.getResponseWriter(), requestContext,
                        ApplicationHandler.handleFailure(exception, requestContext));
            }

            @Override
            protected void writeTimeoutResponse(InvocationContext context) {
                ApplicationHandler.this.writeResponse(requestContext.getResponseWriter(), requestContext,
                        ApplicationHandler.prepareTimeoutResponse(context, requestContext));
            }
        };

        apply(requestContext, callback);

        callback.suspendWriterIfRunning();
    }

    private void checkContainerRequestContext(final JerseyContainerRequestContext requestContext) {
        if (requestContext.getSecurityContext() == null) {
            throw new IllegalArgumentException("SecurityContext from ContainerRequestContext must not be null.");
        } else if (requestContext.getRequest() == null) {
            throw new IllegalArgumentException("Request from ContainerRequestContext must not be null.");
        } else if (requestContext.getResponseWriter() == null) {
            throw new IllegalArgumentException("ResponseWriter from ContainerRequestContext must not be null.");
        }
    }

    /**
     * Invokes a request. Supplied callback is notified about the invocation result.
     *
     * @param requestContext request data.
     * @param callback       request invocation callback called when the request
     *                       transformation is done, suspended, resumed etc. Must not be {@code null}.
     */
    private void apply(
            final JerseyContainerRequestContext requestContext,
            final InvocationCallback<JerseyContainerResponseContext> callback) {

        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {
                try {
                    invoker.apply(requestContext, callback);
                } finally {
                    closeableServiceFactory.get().close();
                }
            }
        });
    }

    private static JerseyContainerResponseContext prepareTimeoutResponse(
            final InvocationContext context, JerseyContainerRequestContext requestContext) {

        Response response = context.getResponse();
        if (response == null) {
            response = Response.serverError().entity("Request processing has timed out.").type(MediaType.TEXT_PLAIN).build();
        }
        return new JerseyContainerResponseContext(requestContext, response);
    }

    private static JerseyContainerResponseContext handleFailure(Throwable failure, JerseyContainerRequestContext requestContext) {
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
            LOGGER.log(Level.SEVERE, message, failure);
        } else {
            LOGGER.log(Level.FINE, message, failure);
        }

        return new JerseyContainerResponseContext(
                requestContext,
                Response.status(statusCode).entity(message).type(MediaType.TEXT_PLAIN).build());
    }

    /**
     * Used to set proper Content-Length header to outgoing {@link Response}s.
     */
    private class MessageBodySizeCallback implements MessageBodyWorkers.MessageBodySizeCallback {
        private long size = -1;

        @Override
        public void onRequestEntitySize(long size) throws IOException {
            this.size = size;
        }

        public long getSize() {
            return size;
        }
    }

    private void writeResponse(
            final ContainerResponseWriter writer,
            final JerseyContainerRequestContext requestContext,
            final JerseyContainerResponseContext responseContext) {
        final MessageBodySizeCallback messageBodySizeCallback = new MessageBodySizeCallback();

        if (!responseContext.hasEntity()) {
            writer.writeResponseStatusAndHeaders(0, responseContext);
            writer.commit();
            return;
        }

        responseContext.setStreamProvider(new OutboundMessageContext.StreamProvider() {
            private OutputStream output;

            @Override
            public void commit() throws IOException {
                output = writer.writeResponseStatusAndHeaders(messageBodySizeCallback.getSize(), responseContext);
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return output;
            }
        });

        final Object entity = responseContext.getEntity();
        try {
            requestContext.getWorkers().writeTo(
                    entity,
                    entity.getClass(),
                    responseContext.getEntityType(),
                    responseContext.getEntityAnnotations(),
                    responseContext.getMediaType(),
                    responseContext.getHeaders(),
                    requestContext.getPropertiesDelegate(),
                    responseContext.getEntityStream(),
                    messageBodySizeCallback,
                    true,
                    !requestContext.getMethod().equals(HttpMethod.HEAD));
        } catch (IOException ex) {
            Logger.getLogger(ApplicationHandler.class.getName()).log(Level.SEVERE, null, ex);
            throw new MappableException(ex);
        } finally {
            responseContext.commitStream();

            if (ChunkedResponse.class.isAssignableFrom(entity.getClass())) {
                try {
                    ((ChunkedResponse) entity).setContext(requestContext, responseContext);
                } catch (IOException ex) {
                    Logger.getLogger(ApplicationHandler.class.getName()).log(Level.SEVERE, null, ex);
                    //noinspection ThrowFromFinallyBlock
                    throw new MappableException(ex);
                }
                writer.suspend(0, TimeUnit.SECONDS, null);
            } else {
                writer.commit();
            }
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

    /**
     * Get the service providers configured for the application.
     *
     * @return application service providers.
     */
    public ServiceProviders getServiceProviders() {
        return refs.providers.get();
    }

    /**
     * Get the application configuration.
     *
     * @return application configuration.
     */
    public ResourceConfig getConfiguration() {
        return configuration;
    }
}
