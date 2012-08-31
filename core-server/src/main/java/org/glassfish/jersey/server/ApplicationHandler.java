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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NameBinding;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.Config;
import org.glassfish.jersey.internal.ExtractorException;
import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.process.internal.InflectorNotFoundException;
import org.glassfish.jersey.process.internal.PriorityComparator;
import org.glassfish.jersey.process.internal.ProcessingContext;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
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
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;

import deprecated.javax.ws.rs.DynamicBinder;
import deprecated.javax.ws.rs.PostMatching;

/**
 * Jersey server-side application handler.
 * <p>
 * Container implementations use the {@code ApplicationHandler} API to process requests
 * by invoking the {@link #handle(ContainerRequest) handle(requestContext)}
 * method on a configured application  handler instance.
 * </p>
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

    private class ApplicationBinder extends AbstractBinder {

        private class JaxrsApplicationProvider implements Factory<Application> {

            @Override
            public Application provide() {
                return ApplicationHandler.this.configuration.getApplication();
            }

            @Override
            public void dispose(Application instance) {
                //not used
            }
        }

        private class ResourceConfigProvider implements Factory<ResourceConfig> {

            @Override
            public ResourceConfig provide() {
                return ApplicationHandler.this.configuration;
            }

            @Override
            public void dispose(ResourceConfig instance) {
                //not used
            }
        }

        @Override
        protected void configure() {
            bindFactory(new ResourceConfigProvider())
                    .to(ResourceConfig.class).to(Config.class).in(Singleton.class);
            bindFactory(new JaxrsApplicationProvider()).to(Application.class).in(Singleton.class);
            bind(ApplicationHandler.this).to(ApplicationHandler.class);
        }
    }

    @Inject
    private Factory<CloseableService> closeableServiceFactory;
    private ServiceLocator locator;
    /**
     * Request invoker.
     */
    private RequestInvoker<ContainerRequest, ContainerResponse> invoker;
    private final ResourceConfig configuration;

    /**
     * Create a new Jersey application handler using a default configuration.
     */
    public ApplicationHandler() {
        initServiceLocator();
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
        initServiceLocator();
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
        initServiceLocator();
        this.configuration = ResourceConfig.forApplication(application);
        initialize();
    }

    private void initServiceLocator() {
        locator = Injections.createLocator(new ServerBinder(), new ApplicationBinder());
    }

    /**
     * Assumes the configuration field is initialized with a valid ResourceConfig.
     */
    private void initialize() {
        registerAdditionalBinders(configuration.getCustomBinders());

        final Class<? extends Application> applicationClass = configuration.getApplicationClass();
        if (applicationClass != null) {
            final Application application = createApplication(applicationClass);
            if (application instanceof ResourceConfig) {
                // (JERSEY-1094) If the application is an instance of ResourceConfig then register it's
                // custom binders into the HK2 service locator so they can be used right away.
                registerAdditionalBinders(((ResourceConfig) application).getCustomBinders());
            }
            configuration.setApplication(application);
        }

        configuration.lock();

        // Introspecting classes & instances
        final Map<Class<?>, ContractProvider> providerClasses = new IdentityHashMap<Class<?>, ContractProvider>();
        final Map<Object, ContractProvider> providerInstances = new IdentityHashMap<Object, org.glassfish.jersey.model.ContractProvider>();

        final ResourceBag.Builder resourceBagBuilder = new ResourceBag.Builder();

        final List<ResourceModelIssue> resourceModelIssues = new LinkedList<ResourceModelIssue>();

        for (Class<?> c : configuration.getClasses()) {
            org.glassfish.jersey.model.ContractProvider contractProvider = ContractProvider.from(c);
            if (contractProvider != null) {
                providerClasses.put(c, contractProvider);
            }

            try {
                Resource resource = Resource.from(c, resourceModelIssues);
                if (resource != null) {
                    resourceBagBuilder.registerResource(c, resource);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.warning(ex.getMessage());
            }

        }

        for (Object o : configuration.getSingletons()) {
            ContractProvider contractProvider = ContractProvider.from(o);
            if (contractProvider != null) {
                providerInstances.put(o, contractProvider);
            }

            try {
                Resource resource = Resource.from(o, resourceModelIssues);
                if (resource != null) {
                    resourceBagBuilder.registerResource(o, resource);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.warning(ex.getMessage());
            }
        }

        // Adding programmatic resource models
        for (Resource programmaticResource : configuration.getResources()) {
            resourceBagBuilder.registerProgrammaticResource(programmaticResource);
        }

        ResourceBag resourceBag = resourceBagBuilder.build();

        // Registering Injection Bindings
        final Set<ComponentProvider> componentProviders = new HashSet<ComponentProvider>();

        for (ComponentProvider provider : ServiceFinder.find(ComponentProvider.class)) {
            provider.initialize(locator);
            componentProviders.add(provider);
        }
        registerProvidersAndResources(componentProviders, providerClasses, providerInstances, resourceBag);
        for (ComponentProvider componentProvider : componentProviders) {
            componentProvider.done();
        }

        // scan for NameBinding annotations attached to the application class
        Collection<Class<? extends Annotation>> applicationNameBindings =
                ReflectionHelper.getAnnotationTypes(configuration.getApplication().getClass(), NameBinding.class);

        // find all filters, interceptors and dynamic binders
        final List<ContainerResponseFilter> responseFilters = Providers.getAllProviders(locator,
                ContainerResponseFilter.class);
        final MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters
                = filterNameBound(responseFilters, null, applicationNameBindings);
        final List<ContainerRequestFilter> preMatchFilters = Providers.getAllProviders(
                locator,
                ContainerRequestFilter.class,
                new PriorityComparator<ContainerRequestFilter>(PriorityComparator.Order.ASCENDING)
        );
        final List<ContainerRequestFilter> requestFilters = new ArrayList<ContainerRequestFilter>();
        final MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters
                = filterNameBound(preMatchFilters, requestFilters, applicationNameBindings);

        final List<ReaderInterceptor> readerInterceptors = locator.getAllServices(ReaderInterceptor.class);
        final MultivaluedMap<Class<? extends Annotation>, ReaderInterceptor> nameBoundReaderInterceptors
                = filterNameBound(readerInterceptors, null, applicationNameBindings);

        final List<WriterInterceptor> writerInterceptors = locator.getAllServices(WriterInterceptor.class);
        final MultivaluedMap<Class<? extends Annotation>, WriterInterceptor> nameBoundWriterInterceptors
                = filterNameBound(writerInterceptors, null, applicationNameBindings);
        final List<DynamicBinder> dynamicBinders = Providers.getAllProviders(locator, DynamicBinder.class);

        // validate the models
        validate(resourceBag.models, resourceModelIssues, locator.<MessageBodyWorkers>getService(MessageBodyWorkers.class));

        // create a router
        final RuntimeModelBuilder runtimeModelBuilder = locator.getService(RuntimeModelBuilder.class);
        runtimeModelBuilder.setGlobalInterceptors(readerInterceptors, writerInterceptors);
        runtimeModelBuilder.setBoundProviders(nameBoundRequestFilters, nameBoundResponseFilters, nameBoundReaderInterceptors,
                nameBoundWriterInterceptors, dynamicBinders);
        for (Resource resource : resourceBag.models) {
            runtimeModelBuilder.process(resource, false);
        }

        /**
         * Root hierarchical request matching acceptor.
         * Invoked in a single linear stage as part of the main linear accepting chain.
         */
        final Router resourceRoutingRoot = runtimeModelBuilder.buildModel(false);

        final ContainerFilteringStage preMatchRequestFilteringStage =
                locator.createAndInitialize(ContainerFilteringStage.Builder.class).build(preMatchFilters, responseFilters);
        final RoutingStage routingStage =
                locator.createAndInitialize(RoutingStage.Builder.class).build(resourceRoutingRoot);
        final ContainerFilteringStage resourceFilteringStage =
                locator.createAndInitialize(ContainerFilteringStage.Builder.class).build(requestFilters, null);
        final RoutedInflectorExtractorStage routedInflectorExtractorStage =
                locator.createAndInitialize(RoutedInflectorExtractorStage.class);
        /**
         *  Root linear request acceptor. This is the main entry point for the whole request processing.
         */
        final Stage<ContainerRequest> rootStage = Stages
                .chain(locator.createAndInitialize(ReferencesInitializer.class))
                .to(locator.createAndInitialize(ContainerMessageBodyWorkersInitializer.class))
                .to(preMatchRequestFilteringStage)
                .to(routingStage)
                .to(resourceFilteringStage)
                .build(routedInflectorExtractorStage);

        // Inject instances.
        for (Object instance : providerInstances.keySet()) {
            locator.inject(instance);
        }
        for (Object instance : resourceBag.instances) {
            locator.inject(instance);
        }

        this.invoker = locator.createAndInitialize(ServerBinder.RequestInvokerBuilder.class)
                .build(rootStage);

        // inject self
        locator.inject(this);
    }

    /**
     * Takes collection of all filters/interceptors (either request/reader or response/writer)
     * and separates out all name-bound filters/interceptors, returns them as a separate MultivaluedMap,
     * mapping the name-bound annotation to the list of name-bound filters/interceptors.
     *
     * Note, the name-bound filters/interceptors are removed from the original filters/interceptors collection.
     * If non-null collection is passed in the postMatching parameter (applicable for filters only),
     * this method also removes all the global
     * postMatching filters from the original collection and adds them to the collection passed in the postMatching
     * parameter.
     *
     * @param all                     Collection of all filters to be processed.
     * @param postMatching            Collection where this method should move all global post-matching filters,
     *                                or {@code null} if separating out global post-matching filters is not desirable.
     * @param applicationNameBindings collection of name binding annotations attached to the JAX-RS application.
     * @param <T>                     Filter type (either {@link ContainerRequestFilter} or {@link ContainerResponseFilter}).
     * @return {@link MultivaluedMap} of all name-bound filters.
     */
    private static <T> MultivaluedMap<Class<? extends Annotation>, T> filterNameBound(
            final Collection<T> all,
            final Collection<ContainerRequestFilter> postMatching,
            final Collection<Class<? extends Annotation>> applicationNameBindings) {

        final MultivaluedMap<Class<? extends Annotation>, T> result
                = new MultivaluedHashMap<Class<? extends Annotation>, T>();

        outer:
        for (Iterator<T> it = all.iterator(); it.hasNext(); ) {
            T filter = it.next();
            boolean post = false;
            HashSet<Class<? extends Annotation>> nameBindings = new HashSet<Class<? extends Annotation>>();
            for (Annotation annotation : filter.getClass().getAnnotations()) {
                if (postMatching != null && (annotation instanceof PostMatching ||
                        // treat NameBindings attached to application as global post-matching filters
                        applicationNameBindings.contains(annotation.annotationType()))) {
                    post = true;
                } else {
                    if (postMatching == null && applicationNameBindings.contains(annotation.annotationType())) {
                        // treat NameBindings attached to annotation as global filters
                        // (if no need to distinguish as post-matching - i.e. postMatching == null)
                        continue outer;
                    }
                    for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
                        if (metaAnnotation instanceof NameBinding) {
                            nameBindings.add(annotation.annotationType());
                        }
                    }
                }
            }

            if (post) {
                it.remove();
                postMatching.add((ContainerRequestFilter) filter);
            } else if (!nameBindings.isEmpty()) {
                it.remove();
                for (Class<? extends Annotation> nameBinding : nameBindings) {
                    result.add(nameBinding, filter);
                }
            }
        }

        return result;
    }

    private void registerProvidersAndResources(
            final Set<ComponentProvider> componentProviders,
            final Map<Class<?>, org.glassfish.jersey.model.ContractProvider> providerClasses,
            final Map<Object, org.glassfish.jersey.model.ContractProvider> providerInstances,
            final ResourceBag resourceBag) {

        final JerseyResourceContext resourceContext = locator.getService(JerseyResourceContext.class);
        DynamicConfiguration dc = Injections.getConfiguration(locator);

        // Bind resource classes
        for (Class<?> resourceClass : resourceBag.classes) {
            final ContractProvider providerModel = providerClasses.remove(resourceClass);

            if (bindWithComponentProvider(resourceClass, providerModel, componentProviders)) {
                continue;
            }
            if (!Resource.isAcceptable(resourceClass)) {
                LOGGER.warning(LocalizationMessages.NON_INSTANTIABLE_CLASS(resourceClass));
                continue;
            }

            resourceContext.unsafeBindResource(resourceClass, providerModel, dc);
        }
        // Bind resource instances
        for (Object resourceInstance : resourceBag.instances) {
            final ContractProvider providerModel = providerInstances.remove(resourceInstance);
            // TODO Try to bind resource instances using component providers?
            resourceContext.unsafeBindResource(resourceInstance, providerModel, dc);
        }
        // Bind pure provider classes
        for (ContractProvider provider : providerClasses.values()) {
            ProviderBinder.bindProvider(provider, dc);
        }
        // Bind pure provider instances
        for (ContractProvider provider : providerInstances.values()) {
            ProviderBinder.bindProvider(provider, dc);
        }

        dc.commit();
    }

    private boolean bindWithComponentProvider(
            final Class<?> component,
            final ContractProvider providerModel,
            final Collection<ComponentProvider> componentProviders) {

        final Set<Class<?>> contracts = providerModel == null ? Collections.<Class<?>>emptySet() : providerModel.getContracts();
        for (ComponentProvider provider : componentProviders) {
            if (provider.bind(component, contracts)) {
                return true;
            }
        }
        return false;
    }

    private Application createApplication(Class<? extends Application> applicationClass) {
        // need to handle ResourceConfig and Application separately as invoking forContract() on these
        // will trigger the factories which we don't want at this point
        if (applicationClass == ResourceConfig.class) {
            return new ResourceConfig();
        } else if (applicationClass == Application.class) {
            return new Application();
        } else {
            return locator.createAndInitialize(applicationClass);
        }
    }

    /**
     * Registers HK2 binders into the HK2 service register.
     *
     * @param binders binders to be registered.
     */
    public void registerAdditionalBinders(final Set<Binder> binders) {
        final DynamicConfiguration dc = Injections.getConfiguration(locator);

        for (Binder binder : binders) {
            binder.bind(dc);
        }
        dc.commit();
    }

    private void validate(List<Resource> resources, List<ResourceModelIssue> modelIssues, MessageBodyWorkers workers) {
        ResourceModelValidator validator = new BasicValidator(modelIssues, workers);

        for (Resource r : resources) {
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
    public Future<ContainerResponse> apply(final ContainerRequest requestContext) {
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
     * @param outputStream   response output stream.
     * @return response future.
     */
    public Future<ContainerResponse> apply(final ContainerRequest requestContext,
                                           final OutputStream outputStream) {
        requestContext.setSecurityContext(DEFAULT_SECURITY_CONTEXT);
        requestContext.setWriter(new ContainerResponseWriter() {
            @Override
            public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext)
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
        });

        final TimingOutProcessingCallback callback = new TimingOutProcessingCallback() {
            private ContainerResponse responseContext;

            @Override
            protected ContainerResponse handleResponse(ContainerResponse responseContext) {
                this.responseContext = ApplicationHandler.this.writeResponse(requestContext, responseContext);
                if (HttpMethod.HEAD.equals(requestContext.getMethod())) {
                    // for testing purposes:
                    // need to also strip the object entity as it was stripped writeResponse(...)
                    stripEntity(responseContext);
                }
                return responseContext;
            }

            @Override
            protected ContainerResponse handleFailure(Throwable exception) {
                return responseContext = ApplicationHandler.this.writeResponse(requestContext,
                        ApplicationHandler.handleFailure(exception, requestContext, responseContext, locator));
            }

            @Override
            protected ContainerResponse handleTimeout(ProcessingContext context) {
                return responseContext = ApplicationHandler.this.writeResponse(
                        requestContext, ApplicationHandler.prepareTimeoutResponse(context, requestContext));
            }

            @Override
            protected void release() {
                releaseRequestProcessing(responseContext);
            }
        };

        invoker.apply(requestContext, callback);

        return callback;
    }

    /**
     * Strips entity if present.
     *
     * @param responseContext processed response context.
     * @return original response without entity.
     */
    private ContainerResponse stripEntity(final ContainerResponse responseContext) {
        if (responseContext.hasEntity()) {
            responseContext.setEntity(null);
        }

        return responseContext;
    }

    /**
     * The main request/response processing entry point for Jersey container implementations.
     * <p>
     * The method invokes the request processing of the provided
     * {@link ContainerRequest container request context} and uses the
     * {@link ContainerResponseWriter container response writer} to suspend & resume the processing
     * as well as write the response back to the container.
     * </p>
     * <p>
     * The the {@link SecurityContext security context} stored in the container request context
     * is bound as an injectable instance in the scope of the processed request context.
     * Also, any {@link org.glassfish.jersey.server.spi.RequestScopedInitializer custom scope injections}
     * are initialized in the current request scope.
     * </p>
     *
     * @param requestContext container request context of the current request.
     */
    public void handle(final ContainerRequest requestContext) {
        checkContainerRequestContext(requestContext);

        final ContainerResponseWriterCallback callback = new ContainerResponseWriterCallback(requestContext) {
            private ContainerResponse responseContext;

            @Override
            protected void writeResponse(ContainerResponse response) {
                responseContext = ApplicationHandler.this.writeResponse(requestContext, response);
            }

            @Override
            protected void writeResponse(Throwable exception) {
                responseContext = ApplicationHandler.this.writeResponse(
                        requestContext, ApplicationHandler.handleFailure(exception, requestContext, responseContext, locator));
            }

            @Override
            protected void writeTimeoutResponse(ProcessingContext context) {
                responseContext = ApplicationHandler.this.writeResponse(
                        requestContext, ApplicationHandler.prepareTimeoutResponse(context, requestContext));
            }

            @Override
            protected void release() {
                releaseRequestProcessing(responseContext);
            }
        };

        invoker.apply(requestContext, callback);

        callback.suspendWriterIfRunning();
    }

    private void releaseRequestProcessing(ContainerResponse responseContext) {
        closeableServiceFactory.provide().close();
        // Commit the container response writer if not in chunked mode
        // responseContext may be null in case the request processing was cancelled.
        if (responseContext != null && !responseContext.isChunked()) {
            responseContext.close();
        }
    }

    private void checkContainerRequestContext(final ContainerRequest requestContext) {
        if (requestContext.getSecurityContext() == null) {
            throw new IllegalArgumentException("SecurityContext from ContainerRequestContext must not be null.");
        } else if (requestContext.getRequest() == null) {
            throw new IllegalArgumentException("Request from ContainerRequestContext must not be null.");
        } else if (requestContext.getResponseWriter() == null) {
            throw new IllegalArgumentException("ResponseWriter from ContainerRequestContext must not be null.");
        }
    }

    private static ContainerResponse prepareTimeoutResponse(
            final ProcessingContext context, ContainerRequest requestContext) {

        Response response = context.getResponse();
        if (response == null) {
            response = Response.serverError().entity("Request processing has timed out.").type(MediaType.TEXT_PLAIN).build();
        }
        return new ContainerResponse(requestContext, response);
    }

    private static ContainerResponse handleFailure(Throwable failure, ContainerRequest requestContext,
                                                   ContainerResponse containerResponse, ServiceLocator locator) {
        if (failure instanceof MappableException) {
            failure = failure.getCause();
        }
        Response.StatusType statusCode = Response.Status.INTERNAL_SERVER_ERROR;
        String message = failure.getMessage();

        if (containerResponse != null && containerResponse.isMappedFromException()) {
            // an exception was already thrown and mapped to the response. We don't not map it again.
            return new ContainerResponse(
                    requestContext,
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        } else {
            ExceptionMappers mappers = locator.getService(ExceptionMappers.class);
            ExceptionMapper mapper = mappers.find(failure.getClass());
            if (mapper != null) {
                try {
                    //noinspection unchecked
                    return new ContainerResponse(requestContext, mapper.toResponse(failure));
                } catch (Exception e) {
                    return new ContainerResponse(
                            requestContext,
                            Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
                }
            }
        }

        if (failure instanceof ProcessingException) {
            if (failure instanceof HeaderValueException) {
                statusCode = Response.Status.BAD_REQUEST;
            } else if (failure instanceof InflectorNotFoundException) {
                statusCode = Response.Status.NOT_FOUND;
                message = "Requested resource not found.";
            } else if (failure instanceof ExtractorException) {
                statusCode = Response.Status.BAD_REQUEST;
            }
        } else if (failure instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) failure;
            return new ContainerResponse(requestContext, wae.getResponse());
        }

        if (statusCode == Response.Status.INTERNAL_SERVER_ERROR) {
            LOGGER.log(Level.SEVERE, message, failure);
        } else {
            LOGGER.log(Level.FINE, message, failure);
        }

        return new ContainerResponse(
                requestContext,
                Response.status(statusCode).build());
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


    private ContainerResponse writeResponse(
            final ContainerRequest requestContext,
            final ContainerResponse responseContext) {

        final ContainerResponseWriter writer = requestContext.getResponseWriter();
        final MessageBodySizeCallback messageBodySizeCallback = new MessageBodySizeCallback();


        if (!responseContext.hasEntity()) {
            writer.writeResponseStatusAndHeaders(0, responseContext);
            return responseContext;
        }

        final Object entity = responseContext.getEntity();
        boolean skipFinally = false;
        try {
            responseContext.setStreamProvider(new OutboundMessageContext.StreamProvider() {
                private OutputStream output;

                @Override
                public void commit() throws IOException {
                    final long size;
                    if (responseContext.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING) != null) {
                        size = -1;
                    } else {
                        size = messageBodySizeCallback.getSize();
                    }
                    output = writer.writeResponseStatusAndHeaders(size, responseContext);
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    return output;
                }
            });
            responseContext.setEntityStream(requestContext.getWorkers().writeTo(
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
                    !requestContext.getMethod().equals(HttpMethod.HEAD)));
        } catch (Exception ex) {
            if (responseContext.isCommitted()) {
                /**
                 * We're done with processing here. There's nothing we can do about the exception so
                 * let's just log it.
                 */
                LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_WRITING_RESPONSE_ENTITY(), ex);
            } else {
                skipFinally = true;
                return writeResponse(requestContext, ApplicationHandler.handleFailure(ex, requestContext, responseContext,
                        locator));
            }
        } finally {
            if (!skipFinally) {
                responseContext.commitStream();

                if (responseContext.isChunked()) {
                    try {
                        ((ChunkedResponse) entity).setContext(requestContext, responseContext);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_WRITING_RESPONSE_ENTITY_CHUNK(), ex);
                    }
                    // suspend the writer
                    writer.suspend(0, TimeUnit.SECONDS, null);
                }
            }
        }

        return responseContext;
    }

    /**
     * Returns {@link ServiceLocator} relevant to current application.
     *
     * @return {@link ServiceLocator} instance.
     */
    public ServiceLocator getServiceLocator() {
        return locator;
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
