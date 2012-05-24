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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.util.CommittingOutputStream;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.InflectorNotFoundException;
import org.glassfish.jersey.process.internal.InvocationCallback;
import org.glassfish.jersey.process.internal.InvocationContext;
import org.glassfish.jersey.process.internal.LinearAcceptor;
import org.glassfish.jersey.process.internal.MessageBodyWorkersInitializer;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.routing.RouterModule;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.server.internal.routing.RuntimeModelBuilder;
import org.glassfish.jersey.server.model.BasicValidator;
import org.glassfish.jersey.server.model.ModelValidationException;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModelIssue;
import org.glassfish.jersey.server.model.ResourceModelValidator;
import org.glassfish.jersey.server.spi.ContainerRequestContext;
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
 * by invoking the {@link #apply(Request) apply(request)} method on a configured application
 * handler instance.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see ResourceConfig
 * @see org.glassfish.jersey.server.spi.ContainerProvider
 */
public final class ApplicationHandler implements Inflector<Request, Future<Response>> {

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

        private class RootResourceMatchingAcceptorProvider implements Factory<TreeAcceptor> {

            @Override
            public TreeAcceptor get() {
                return ApplicationHandler.this.rootResourceMatchingAcceptor;
            }
        }

        private class RootStageAcceptorProvider implements Factory<LinearAcceptor> {

            @Override
            public LinearAcceptor get() {
                return ApplicationHandler.this.rootStageAcceptor;
            }
        }

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

            bind(TreeAcceptor.class).annotatedWith(Stage.Root.class).toFactory(new RootResourceMatchingAcceptorProvider())
                    .in(Singleton.class);
            bind(LinearAcceptor.class).annotatedWith(Stage.Root.class).toFactory(new RootStageAcceptorProvider())
                    .in(Singleton.class);
        }
    }

    @Inject
    private RequestScope requestScope;
    @Inject
    private RequestInvoker invoker;
    @Inject
    private Factory<RouterModule.RoutingContext> routingContextFactory;
    @Inject
    private Factory<Ref<SecurityContext>> securityContextRefFactory;
    @Inject
    private Factory<CloseableService> closeableServiceFactory;
    //
    private Services services;
    /**
     * Root linear request acceptor.
     * This is the main entry point for the whole request processing.
     */
    private LinearAcceptor rootStageAcceptor;
    /**
     * Root hierarchical request matching acceptor.
     * Invoked in a single linear stage as part of the main linear accepting chain.
     */
    private TreeAcceptor rootResourceMatchingAcceptor;
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
                    final Resource.Builder builder = Resource.builder(c, resourceModelIssues);
                    resourcesBuilders.add(builder);
                    pathToResourceBuilderMap.put(path.value(), builder);
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

        this.rootResourceMatchingAcceptor = runtimeModelBuilder.buildModel();

        // Create a linear accepting chain
        final PreMatchRequestFilteringStage preMatchRequestFilteringStage = injector.inject(PreMatchRequestFilteringStage.class);
        final ResourceMatchingStage resourceMatchingStage = injector.inject(ResourceMatchingStage.class);
        final InflectorExtractingStage inflectorExtractingStage = injector.inject(InflectorExtractingStage.class);
        this.rootStageAcceptor = Stages
                .acceptingChain(injector.inject(MessageBodyWorkersInitializer.class))
                .to(preMatchRequestFilteringStage)
                .to(resourceMatchingStage)
                .build(inflectorExtractingStage);

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
    private void registerAdditionalModules(final Set<Module> modules) {
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
     * @param request request data.
     * @return response future.
     */
    @Override
    public Future<Response> apply(final Request request) {

        final ContainerResponseWriter containerResponseWriter = new ContainerResponseWriter() {
            @Override
            public OutputStream writeResponseStatusAndHeaders(long contentLength, Response response) throws ContainerException {
                if (contentLength >= 0) {
                    response.getHeaders().asMap().putSingle("Content-Length", Long.toString(contentLength));
                }

                // fake output stream - Response is not written (serialized) in this case.
                return new OutputStream() {
                    @Override
                    public void write(int i) throws IOException {
                    }
                };
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
            protected Response handleResponse(Response response) {
                ApplicationHandler.this.writeResponse(containerResponseWriter, request, response);
                return (request.getMethod().equals(HttpMethod.HEAD) ? stripEntity(response) : response);
            }

            @Override
            protected Response handleFailure(Throwable exception) {
                final Response response = ApplicationHandler.handleFailure(exception);
                ApplicationHandler.this.writeResponse(containerResponseWriter, request,
                        response);
                return response;
            }

            @Override
            protected Response handleTimeout(InvocationContext context) {
                final Response response = ApplicationHandler.prepareTimeoutResponse(context);
                ApplicationHandler.this.writeResponse(containerResponseWriter, request,
                        response);
                return response;
            }
        };

        apply(request, callback, DEFAULT_SECURITY_CONTEXT, null);

        return callback;
    }

    /**
     * Strips entity if present.
     *
     * @param originalResponse processed response.
     * @return original response without entity.
     */
    private Response stripEntity(final Response originalResponse) {
        if (originalResponse.hasEntity()) {
            Response.ResponseBuilder result = Response.status(originalResponse.getStatus());
            Responses.fillHeaders(result, originalResponse.getHeaders().asMap());
            return result.build();
        } else {
            return originalResponse;
        }
    }

    /**
     * The main request/response processing entry point for Jersey container implementations.
     * <p/>
     * The method invokes the request processing of the provided {@link Request jax-rs request} from the
     * {@link ContainerRequestContext container request context} and uses the {@link ContainerResponseWriter container response
     * writer} from the provided {@link ContainerRequestContext container request context} to suspend & resume the processing as
     * well as write the response back to the container. If the {@link ContainerRequestContext container request context} contains
     * {@link SecurityContext security context} it will be registered for further request processing.
     * {@link RequestScopedInitializer Custom scope injections} will be initialized into the request scope.
     *
     * @param containerContext container request context of the current request.
     */
    public void apply(final ContainerRequestContext containerContext) {
        checkContainerRequestContext(containerContext);

        final ContainerResponseWriterCallback callback = new ContainerResponseWriterCallback(containerContext.getRequest(),
                containerContext.getResponseWriter()) {

            @Override
            protected void writeResponse(Response response) {
                ApplicationHandler.this.writeResponse(containerContext.getResponseWriter(), request, response);
            }

            @Override
            protected void writeResponse(Throwable exception) {
                ApplicationHandler.this.writeResponse(containerContext.getResponseWriter(), request,
                        ApplicationHandler.handleFailure(exception));
            }

            @Override
            protected void writeTimeoutResponse(InvocationContext context) {
                ApplicationHandler.this.writeResponse(containerContext.getResponseWriter(), request,
                        ApplicationHandler.prepareTimeoutResponse(context));
            }
        };
        apply(containerContext.getRequest(), callback, containerContext.getSecurityContext(),
                containerContext.getRequestScopedInitializer());

        callback.suspendWriterIfRunning();
    }

    private void checkContainerRequestContext(final ContainerRequestContext containerContext) {
        if (containerContext.getSecurityContext() == null) {
            throw new IllegalArgumentException("SecurityContext from ContainerRequestContext must not be null.");
        } else if (containerContext.getRequest() == null) {
            throw new IllegalArgumentException("Request from ContainerRequestContext must not be null.");
        } else if (containerContext.getResponseWriter() == null) {
            throw new IllegalArgumentException("ResponseWriter from ContainerRequestContext must not be null.");
        }
    }

    /**
     * Invokes a request. Supplied callback is notified about the invocation result.
     *
     * @param request                 request data.
     * @param callback                request invocation callback called when the request
     *                                transformation is done, suspended, resumed etc. Must not be {@code null}.
     * @param securityContext         custom security context.
     * @param requestScopeInitializer custom request-scoped initializer.
     */
    private void apply(final Request request, final InvocationCallback callback, final SecurityContext securityContext,
                       final RequestScopedInitializer requestScopeInitializer) {
        requestScope.runInScope(new Runnable() {

            @Override
            public void run() {
                try {
                    // TODO move to initialization stage
                    initRequestScopeInjections(securityContext, requestScopeInitializer);

                    invoker.apply(request, callback);
                } finally {
                    closeableServiceFactory.get().close();
                }
            }
        });
    }

    private void initRequestScopeInjections(SecurityContext securityContext, RequestScopedInitializer requestScopeInitializer) {
        final Ref<SecurityContext> secReference = securityContextRefFactory.get();
        secReference.set(securityContext);

        if (requestScopeInitializer != null) {
            requestScopeInitializer.initialize(services);
        }
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
            LOGGER.log(Level.SEVERE, message, failure);
        } else {
            LOGGER.log(Level.FINE, message, failure);
        }

        return Response.status(statusCode).entity(message).type(MediaType.TEXT_PLAIN).build();
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

    private void writeResponse(final ContainerResponseWriter writer, final Request request, Response response) {
        CommittingOutputStream committingOutput = null;
        final MessageBodySizeCallback messageBodySizeCallback = new MessageBodySizeCallback();

        try {
            final boolean entityExists = response.hasEntity();

            final MessageBodyWorkers workers = Requests.getMessageWorkers(request);
            if (entityExists) {
                final Object entity = response.getEntity();
                final RoutingContext routingContext = routingContextFactory.get();

                final MediaType outputMediaType = routingContext.getEffectiveAcceptableType();
                final Annotation[] outputAnnotations = routingContext.getResponseMethodAnnotations();
                Type entityType = routingContext.getResponseMethodType();

                // TODO this is just a quick workaround for issue #JERSEY-1089
                //      which needs to be fixed by a common solution
                if (entityType == null || Void.TYPE == entityType || Void.class == entityType || entityType == Response.class) {
                    final Type genericSuperclass = entity.getClass().getGenericSuperclass();
                    entityType = (genericSuperclass instanceof ParameterizedType) ? genericSuperclass : entity.getClass();
                }

                if (entityType instanceof ParameterizedType) {
                    ParameterizedType paramEntityType = (ParameterizedType) entityType;
                    Type rawEntityType = paramEntityType.getRawType();
                    if (rawEntityType == GenericEntity.class) {
                        entityType = paramEntityType.getActualTypeArguments()[0];
                    }
                }

                // TODO this is just a quick workaround for issue #JERSEY-1088
                //      which needs to be fixed by a common solution
                if (response.getHeaders().getMediaType() == null) {
                    response = Responses.toBuilder(response).type(outputMediaType).build();
                }

                final Response outResponse = response;
                committingOutput = new CommittingOutputStream() {

                    private OutputStream output;

                    @Override
                    protected void commit() throws IOException {
                        output = writer.writeResponseStatusAndHeaders(messageBodySizeCallback.getSize(), outResponse);
                    }

                    @Override
                    protected OutputStream getOutputStream() throws IOException {
                        return output;
                    }
                };

                workers.writeTo(entity, GenericType.of(entity.getClass(), entityType), outputAnnotations, outputMediaType,
                        response.getMetadata(), response.getProperties(), committingOutput, messageBodySizeCallback,
                        true, !request.getMethod().equals(HttpMethod.HEAD));
            } else {
                writer.writeResponseStatusAndHeaders(0, response);
            }
        } catch (IOException ex) {
            Logger.getLogger(ApplicationHandler.class.getName()).log(Level.SEVERE, null, ex);
            throw new MappableException(ex);
        } finally {
            commitOutputStream(committingOutput);
            writer.commit();
        }
    }

    /**
     * Commits the {@link CommittingOutputStream} if it wasn't already committed.
     *
     * @param committingOutput the {@code CommittingOutputStream} to commit.
     */
    private void commitOutputStream(final CommittingOutputStream committingOutput) {
        if (committingOutput == null) {
            return;
        }

        if (!committingOutput.isCommitted()) {
            try {
                // Commit the OutputStream.
                committingOutput.flush();
            } catch (Exception ioe) {
                // Do nothing - we are already handling an exception.
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
