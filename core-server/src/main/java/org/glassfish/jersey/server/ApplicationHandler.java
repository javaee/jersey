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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.util.CommittingOutputStream;
import org.glassfish.jersey.internal.util.collection.Pair;
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
import org.glassfish.jersey.process.internal.PreMatchRequestFilterAcceptor;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.routing.RouterModule;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.server.internal.routing.RuntimeModelProviderFromRootResource;
import org.glassfish.jersey.server.model.BasicValidator;
import org.glassfish.jersey.server.model.IntrospectionModeller;
import org.glassfish.jersey.server.model.ResourceClass;
import org.glassfish.jersey.server.model.ResourceModelIssue;
import org.glassfish.jersey.server.model.ResourceModelValidator;
import org.glassfish.jersey.server.spi.ContainerRequestContext;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;
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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Jersey server-side application handler.
 *
 * Container implementations use the {@code ApplicationHandler} API to process requests
 * by invoking the {@link #apply(Request) apply(request)} method on a configured application
 * handler instance.
 *
 * @see ResourceConfig
 * @see org.glassfish.jersey.server.spi.ContainerProvider
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
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

        private class RootAcceptorProvider implements Factory<TreeAcceptor> {

            @Override
            public TreeAcceptor get() {
                return ApplicationHandler.this.rootAcceptor;
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
                return ApplicationHandler.this.immutableConfigurationView;
            }
        }

        @Override
        protected void configure() {
            ResourceConfigProvider rcp = new ResourceConfigProvider();
            bind(ResourceConfig.class).toFactory(rcp).in(Singleton.class);
            bind(FeaturesAndProperties.class).toFactory(rcp).in(Singleton.class);

            bind(Application.class).toFactory(new JaxrsApplicationProvider()).in(Singleton.class);

            bind(ApplicationHandler.class).toInstance(ApplicationHandler.this);

            bind(TreeAcceptor.class).annotatedWith(Stage.Root.class).toFactory(new RootAcceptorProvider()).in(Singleton.class);

            bind().to(PreMatchRequestFilterAcceptor.class).in(Singleton.class);

            for (Module m : ApplicationHandler.this.configuration.getCustomModules()) {
                install(m);
            }
        }
    }
    // FIXME move filter acceptor away from here! It must be part of the root acceptor chain!
    @Inject
    private PreMatchRequestFilterAcceptor preMatchFilterAcceptor;
    @Inject
    private RequestScope requestScope;
    @Inject
    private RequestInvoker invoker;
    @Inject
    private Factory<RouterModule.RoutingContext> routingContextFactory;
    @Inject
    Factory<Ref<SecurityContext>> securityContextRefFactory;
    //
    private Services services;
    private TreeAcceptor rootAcceptor;
    private final ResourceConfig configuration;
    private ResourceConfig immutableConfigurationView;
    private References refs;

    public ApplicationHandler() {
        this.configuration = new ResourceConfig();
        initialize();
    }

    public ApplicationHandler(Class<? extends Application> jaxrsApplicationClass) {
        if (ResourceConfig.class.isAssignableFrom(jaxrsApplicationClass)) {
            // TODO should we do some special processing in this case?
        }

        this.configuration = new ResourceConfig(jaxrsApplicationClass);
        initialize();
    }

    public ApplicationHandler(Application application) {
        if (application instanceof ResourceConfig) {
            this.configuration = (ResourceConfig) application;
        } else {
            this.configuration = new ResourceConfig(application);
        }

        initialize();
    }

    /**
     * Assumes the configuration field is initialized with a valid ResourceConfig.
     */
    private void initialize() {
        // TODO parent/child services - when HK2 bec ready:
        //  this.jerseyServices = HK2.get().build(null, jerseyModules);
        //  this.services = HK2.get().build(jerseyServices, customModules);
        this.services = HK2.get().create(null, new ServerModule(), new ApplicationModule());

        final Class<? extends Application> applicationClass = configuration.getApplicationClass();
        if (applicationClass != null) {
            final Application application = services.forContract(applicationClass).get();

            // (JERSEY-1094) If the application is an instance of ResourceConfig then register it's
            // custom modules into the HK2 service registry so they can be used right away.
            if (application instanceof ResourceConfig) {
                registerCustomResourceConfigModules((ResourceConfig) application);
            }

            configuration.setApplication(application);
        }
        immutableConfigurationView = ResourceConfig.unmodifiableCopy(configuration);

        final Map<String, ResourceClass> pathToResourceMap = Maps.newHashMap();
        final Set<ResourceClass> resources = Sets.newHashSet();
        for (Class<?> c : configuration.getClasses()) {
            if (IntrospectionModeller.isRootResource(c)) {
                try {
                    final ResourceClass resource = IntrospectionModeller.createResource(c);
                    resources.add(resource);
                    pathToResourceMap.put(resource.getPath().getValue(), resource);
                } catch (IllegalArgumentException ex) {
                    LOGGER.warning(ex.getMessage());
                }
            }
        }

        for (ResourceClass programmaticResource : configuration.getResources()) {
            ResourceClass r = pathToResourceMap.get(programmaticResource.getPath().getValue());
            if (r != null) {
                // Merge programmatic resource with an existing resource
                r.getResourceMethods().addAll(programmaticResource.getResourceMethods());
                r.getSubResourceMethods().addAll(programmaticResource.getSubResourceMethods());
                r.getSubResourceLocators().addAll(programmaticResource.getSubResourceLocators());
            } else {
                resources.add(programmaticResource);
            }
        }

        final Injector injector = services.forContract(Injector.class).get();
        this.refs = injector.inject(References.class);

        final ServiceProviders providers = services.forContract(ServiceProviders.Builder.class).get()
                .setProviderClasses(Sets.filter(configuration.getClasses(), new Predicate<Class<?>>() {

                    @Override
                    public boolean apply(Class<?> input) {
                        final boolean acceptable = IntrospectionModeller.isAcceptable(input);
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

        validateResources(workers, resources);

        // FIXME: why do we need to inject this? We seem to have all the information
        // available.
        final RuntimeModelProviderFromRootResource runtimeModelCreator =
                services.byType(RuntimeModelProviderFromRootResource.class).get();
        runtimeModelCreator.setWorkers(workers);
        for (ResourceClass r : resources) {
            runtimeModelCreator.process(r);
        }

        this.rootAcceptor = runtimeModelCreator.getRuntimeModel();

        injector.inject(this);
    }

    /**
     * Registers custom modules of a given {@code ResourceConfig} into the HK2 service register.
     *
     * @param resourceConfig the resource config whose custom modules should be registered into the HK2 service
     *                       register.
     */
    private void registerCustomResourceConfigModules(final ResourceConfig resourceConfig) {
        final Set<Module> modules = resourceConfig.getCustomModules();
        final DynamicBinderFactory dynamicBinderFactory = services.bindDynamically();

        for (Module module : modules) {
            module.configure(dynamicBinderFactory);
        }
        dynamicBinderFactory.commit();
    }

    private void validateResources(MessageBodyWorkers workers, Set<ResourceClass> resources) {

        ResourceModelValidator validator = new BasicValidator(workers);

        for (ResourceClass r : resources) {
            validator.validate(r);
        }
        processIssues(validator);
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
            throw new ResourceModelValidator.ModelException(issueList);
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
    public Future<Response> apply(Request request) {
        final TimingOutInvocationCallback callback = new TimingOutInvocationCallback() {

            @Override
            protected Response handleFailure(Throwable exception) {
                return ApplicationHandler.handleFailure(exception);
            }

            @Override
            protected Response handleTimeout(InvocationContext context) {
                return ApplicationHandler.prepareTimeoutResponse(context);
            }
        };

        apply(request, callback, DEFAULT_SECURITY_CONTEXT, null);

        return callback;
    }

    /**
     * The main request/response processing entry point for Jersey container implementations.
     *
     * The method invokes the request processing of the provided {@link Request jax-rs request} from the
     * {@link ContainerRequestContext container request context} and uses the {@link ContainerResponseWriter container response
     * writer} from the provided {@link ContainerRequestContext container request context} to suspend & resume the processing as
     * well as write the response back to the container. If the {@link ContainerRequestContext container request context} contains
     * {@link SecurityContext security context} it will be registered for further request processing.
     * {@link RequestScopedInitializer Custom scope injections} will be initialized into the request scope.
     *
     * @param containerContext
     *            container request context of the current request.
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
     * @param request request data.
     * @param callback request invocation callback called when the request
     *     transformation is done, suspended, resumed etc. Must not be {@code null}.
     * @param securityContext custom security context.
     * @param requestScopeInitializer custom request-scoped initializer.
     */
    private void apply(Request request, InvocationCallback callback, SecurityContext securityContext, RequestScopedInitializer requestScopeInitializer) {
        try {
            requestScope.enter();
            initRequestScopeInjections(securityContext, requestScopeInitializer);
            // FIXME: This must be moved into the acceptor chain otherwise exception mapping & possibly
            //        other stuff may not work!
            final Pair<Request, Optional<LinearAcceptor>> pair = preMatchFilterAcceptor.apply(request);
            invoker.apply(pair.left(), callback);
        } finally {
            requestScope.exit();
        }
    }

    private void initRequestScopeInjections(SecurityContext securityContext, RequestScopedInitializer requestScopeInitializer) {
        Ref<SecurityContext> secReference = securityContextRefFactory.get();
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

    @SuppressWarnings("unchecked")
    private void writeResponse(final ContainerResponseWriter writer, Request request, Response response) {
        CommittingOutputStream committingOutput = null;

        try {
            final boolean entityExists = response.hasEntity();

            final MessageBodyWorkers workers = Requests.getMessageWorkers(request);
            if (entityExists) {
                final Object entity = response.getEntity();
                final RoutingContext routingContext = routingContextFactory.get();

                final MediaType outputType = routingContext.getEffectiveAcceptableType();
                final Annotation[] outputAnnotations = routingContext.getResponseMethodAnnotations();
                Type entityType = routingContext.getResponseMethodType();

                // TODO this is just a quick workaround for issue #JERSEY-1089
                //      which needs to be fixed by a common solution
                if (entityType == null || entityType == Void.class) {
                    final Type genericSuperclass = entity.getClass().getGenericSuperclass();
                    entityType = (genericSuperclass instanceof ParameterizedType) ? genericSuperclass : entity.getClass();
                }

                // TODO this is just a quick workaround for issue #JERSEY-1088
                //      which needs to be fixed by a common solution
                if (response.getHeaders().getMediaType() == null) {
                    response = Responses.toBuilder(response).type(outputType).build();
                }

                final Response outResponse = response;
                committingOutput = new CommittingOutputStream() {

                    private OutputStream output;


                    @Override
                    protected void commit() throws IOException {
                        output = writer.writeResponseStatusAndHeaders(-1, outResponse);
                    }

                    @Override
                    protected OutputStream getOutputStream() throws IOException {
                        return output;
                    }
                };

                final MessageBodyWriter bWriter = workers.getMessageBodyWriter(
                        entity.getClass(), entityType, outputAnnotations, outputType);
                bWriter.writeTo(
                        entity,
                        entity.getClass(),
                        entityType, outputAnnotations, outputType, response.getMetadata(), committingOutput);
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

    public ServiceProviders getServiceProviders() {
        return refs.providers.get();
    }

    public ResourceConfig getConfiguration() {
        return immutableConfigurationView;
    }
}
