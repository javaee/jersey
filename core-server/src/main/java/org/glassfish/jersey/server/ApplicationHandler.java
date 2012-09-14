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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NameBinding;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Singleton;

import org.glassfish.jersey.Config;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.process.internal.PriorityComparator;
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
import org.glassfish.jersey.server.wadl.WadlApplicationContext;
import org.glassfish.jersey.server.wadl.internal.WadlApplicationContextImpl;
import org.glassfish.jersey.server.wadl.internal.WadlResource;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;

import com.google.common.util.concurrent.AbstractFuture;

import deprecated.javax.ws.rs.DynamicBinder;

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

    private final ResourceConfig configuration;
    private final ServiceLocator locator;
    private ServerRuntime runtime;

    /**
     * Create a new Jersey application handler using a default configuration.
     */
    public ApplicationHandler() {
        this.configuration = new ResourceConfig();
        this.locator = Injections.createLocator(new ServerBinder(), new ApplicationBinder());
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
        this.locator = Injections.createLocator(new ServerBinder(), new ApplicationBinder());
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
        this.configuration = ResourceConfig.forApplication(application);
        this.locator = Injections.createLocator(new ServerBinder(), new ApplicationBinder());
        initialize();
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
        final ProviderBag.Builder providerBagBuilder = new ProviderBag.Builder();
        final ResourceBag.Builder resourceBagBuilder = new ResourceBag.Builder();
        final List<ResourceModelIssue> resourceModelIssues = new LinkedList<ResourceModelIssue>();
        final Set<Class<?>> classes = new HashSet<Class<?>>(configuration.getClasses());

        boolean wadlDisabled = configuration.isProperty(ServerProperties.FEATURE_DISABLE_WADL);
        if (!wadlDisabled) {
            classes.add(WadlResource.class);
        }

        for (Class<?> c : classes) {
            boolean isResource = false;

            try {
                Resource resource = Resource.from(c, resourceModelIssues);
                isResource = resource != null;
                if (isResource) {
                    resourceBagBuilder.registerResource(c, resource);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.warning(ex.getMessage());
            }
            providerBagBuilder.register(c, isResource);
        }

        for (Object o : configuration.getSingletons()) {
            boolean isResource = false;
            try {
                Resource resource = Resource.from(o, resourceModelIssues);
                isResource = resource != null;
                if (isResource) {
                    resourceBagBuilder.registerResource(o, resource);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.warning(ex.getMessage());
            }
            providerBagBuilder.register(o, isResource);
        }

        final ProviderBag providerBag = providerBagBuilder.build();

        // Adding programmatic resource models
        for (Resource programmaticResource : configuration.getResources()) {
            resourceBagBuilder.registerProgrammaticResource(programmaticResource);
        }

        final ResourceBag resourceBag = resourceBagBuilder.build();

        // Registering Injection Bindings
        final Set<ComponentProvider> componentProviders = new HashSet<ComponentProvider>();

        for (ComponentProvider provider : ServiceFinder.find(ComponentProvider.class)) {
            provider.initialize(locator);
            componentProviders.add(provider);
        }
        registerProvidersAndResources(componentProviders, providerBag, resourceBag);
        for (ComponentProvider componentProvider : componentProviders) {
            componentProvider.done();
        }

        // scan for NameBinding annotations attached to the application class
        Collection<Class<? extends Annotation>> applicationNameBindings =
                ReflectionHelper.getAnnotationTypes(configuration.getApplication().getClass(), NameBinding.class);

        // find all filters, interceptors and dynamic binders
        final List<ContainerResponseFilter> responseFilters = Providers.getAllProviders(
                locator,
                ContainerResponseFilter.class);
        final MultivaluedMap<Class<? extends Annotation>, ContainerResponseFilter> nameBoundResponseFilters
                = filterNameBound(responseFilters, null, providerBag, applicationNameBindings);

        final List<ContainerRequestFilter> requestFilters = Providers.getAllProviders(
                locator,
                ContainerRequestFilter.class,
                new PriorityComparator<ContainerRequestFilter>(PriorityComparator.Order.ASCENDING));
        final List<ContainerRequestFilter> preMatchFilters = new ArrayList<ContainerRequestFilter>();
        final MultivaluedMap<Class<? extends Annotation>, ContainerRequestFilter> nameBoundRequestFilters
                = filterNameBound(requestFilters, preMatchFilters, providerBag, applicationNameBindings);

        final List<ReaderInterceptor> readerInterceptors = locator.getAllServices(ReaderInterceptor.class);
        final MultivaluedMap<Class<? extends Annotation>, ReaderInterceptor> nameBoundReaderInterceptors
                = filterNameBound(readerInterceptors, null, providerBag, applicationNameBindings);

        final List<WriterInterceptor> writerInterceptors = locator.getAllServices(WriterInterceptor.class);
        final MultivaluedMap<Class<? extends Annotation>, WriterInterceptor> nameBoundWriterInterceptors
                = filterNameBound(writerInterceptors, null, providerBag, applicationNameBindings);
        final List<DynamicBinder> dynamicBinders = Providers.getAllProviders(locator, DynamicBinder.class);

        // validate the models
        validate(resourceBag.models, resourceModelIssues, locator.<MessageBodyWorkers>getService(MessageBodyWorkers.class));

        // create a router
        DynamicConfiguration dynamicConfiguration = Injections.getConfiguration(locator);
        Injections.addBinding(Injections.newBinder(new WadlApplicationContextImpl(resourceBag.models, configuration)).to(WadlApplicationContext.class), dynamicConfiguration);
        dynamicConfiguration.commit();

        final RuntimeModelBuilder runtimeModelBuilder = locator.getService(RuntimeModelBuilder.class);
        runtimeModelBuilder.setGlobalInterceptors(readerInterceptors, writerInterceptors);
        runtimeModelBuilder.setBoundProviders(nameBoundRequestFilters, nameBoundResponseFilters, nameBoundReaderInterceptors,
                nameBoundWriterInterceptors, dynamicBinders);
        for (Resource resource : resourceBag.models) {
            runtimeModelBuilder.process(resource, false);
        }


        // assembly request processing chain
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
        for (Object instance : providerBag.instances) {
            locator.inject(instance);
        }
        for (Object instance : resourceBag.instances) {
            locator.inject(instance);
        }

        this.runtime = locator.createAndInitialize(ServerRuntime.Builder.class).build(rootStage);

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
     * @param applicationNameBindings collection of name binding annotations attached to the JAX-RS application.
     * @return {@link MultivaluedMap} of all name-bound filters.
     */
    private static <T> MultivaluedMap<Class<? extends Annotation>, T> filterNameBound(
            final Collection<T> all,
            final Collection<ContainerRequestFilter> preMatching,
            final ProviderBag providerBag,
            final Collection<Class<? extends Annotation>> applicationNameBindings) {

        final MultivaluedMap<Class<? extends Annotation>, T> result
                = new MultivaluedHashMap<Class<? extends Annotation>, T>();

        for (Iterator<T> it = all.iterator(); it.hasNext(); ) {
            T provider = it.next();
            final Class<?> providerClass = provider.getClass();

            if (preMatching != null && providerClass.getAnnotation(PreMatching.class) != null) {
                it.remove();
                preMatching.add((ContainerRequestFilter) provider);
            }

            ContractProvider model = providerBag.models.get(providerClass);
            if (model == null) {
                // the provider was (most likely) bound in HK2 externally
                model = ContractProvider.from(providerClass);
            }
            boolean nameBound = model.isNameBound();
            if (nameBound && !applicationNameBindings.isEmpty()) {
                for (Class<? extends Annotation> binding : model.getNameBindings()) {
                    if (applicationNameBindings.contains(binding)) {
                        // override the name-bound flag
                        nameBound = false;
                        break;
                    }
                }
            }
            if (nameBound) { // not application-bound
                it.remove();
                for (Class<? extends Annotation> binding : model.getNameBindings()) {
                    result.add(binding, provider);
                }
            }
        }

        return result;
    }

    private void registerProvidersAndResources(
            final Set<ComponentProvider> componentProviders,
            final ProviderBag providerBag,
            final ResourceBag resourceBag) {

        final JerseyResourceContext resourceContext = locator.getService(JerseyResourceContext.class);
        DynamicConfiguration dc = Injections.getConfiguration(locator);

        // Bind resource classes
        for (Class<?> resourceClass : resourceBag.classes) {
            final ContractProvider providerModel = providerBag.models.get(resourceClass);
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
            final ContractProvider providerModel = providerBag.models.get(resourceInstance.getClass());
            // TODO Try to bind resource instances using component providers?
            resourceContext.unsafeBindResource(resourceInstance, providerModel, dc);
        }
        // Bind pure provider classes
        for (Class<?> providerClass : providerBag.classes) {
            final ContractProvider model = providerBag.models.get(providerClass);
            ProviderBinder.bindProvider(providerClass, model, dc);
        }
        // Bind pure provider instances
        for (Object provider : providerBag.instances) {
            final ContractProvider model = providerBag.models.get(provider.getClass());
            ProviderBinder.bindProvider(provider, model, dc);
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
     * @param request      request data.
     * @param outputStream response output stream.
     * @return response future.
     */
    public Future<ContainerResponse> apply(final ContainerRequest request,
                                           final OutputStream outputStream) {
        final FutureResponseWriter responseFuture = new FutureResponseWriter(request.getMethod(), outputStream);

        request.setSecurityContext(DEFAULT_SECURITY_CONTEXT);
        request.setWriter(responseFuture);

        handle(request);

        return responseFuture;
    }

    private static class FutureResponseWriter extends AbstractFuture<ContainerResponse> implements ContainerResponseWriter {
        private static final Logger LOGGER = Logger.getLogger(FutureResponseWriter.class.getName());
        private static final Timer TIMER = new Timer("Jersey application request timer");

        private ContainerResponse response = null;
        private TimerTask timeoutTask = null;
        private TimeoutHandler timeoutHandler = null;
        private boolean suspended = false;
        private final Object runtimeLock = new Object();

        private final String requestMethodName;
        private final OutputStream outputStream;

        private FutureResponseWriter(String requestMethodName, OutputStream outputStream) {
            this.requestMethodName = requestMethodName;
            this.outputStream = outputStream;
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse response)
                throws ContainerException {
            this.response = response;

            if (contentLength >= 0) {
                response.getHeaders().putSingle("Content-Length", Long.toString(contentLength));
            }

            return outputStream;
        }

        @Override
        public boolean suspend(long time, TimeUnit unit, final TimeoutHandler handler) throws IllegalStateException {
            synchronized (runtimeLock) {
                if (suspended) {
                    return false;
                }

                suspended = true;
                timeoutHandler = handler;

                setSuspendTimeout(time, unit);
                return true;
            }
        }

        @Override
        public void setSuspendTimeout(long time, TimeUnit unit) throws IllegalStateException {
            final TimerTask task = new TimerTask() {

                @Override
                public void run() {
                    try {
                        synchronized (runtimeLock) {
                            timeoutHandler.onTimeout(FutureResponseWriter.this);
                        }
                    } catch (Throwable throwable) {
                        LOGGER.log(Level.WARNING, "Time-out handler execution failed.", throwable);
                    }
                }
            };
            synchronized (runtimeLock) {
                if (!suspended) {
                    throw new IllegalStateException("Not suspended.");
                }

                if (timeoutTask != null) {
                    timeoutTask.cancel();
                    timeoutTask = null;
                }

                if (time <= AsyncResponse.NO_TIMEOUT) {
                    return;
                }

                timeoutTask = task;
                try {
                    TIMER.schedule(task, unit.toMillis(time));
                } catch (IllegalStateException ex) {
                    LOGGER.log(Level.WARNING, "Error while scheduling a timeout task.", ex);
                }
            }
        }

        @Override
        public void commit() {
            final ContainerResponse current = response;
            if (current != null) {
                if (HttpMethod.HEAD.equals(requestMethodName) && current.hasEntity()) {
                    // for testing purposes:
                    // need to also strip the object entity as it was stripped when writing to output
                    current.setEntity(null);
                }

                super.set(current);
            }
        }

        @Override
        public void failure(Throwable error) {
            super.setException(error);
        }

        @Override
        protected void interruptTask() {
            // TODO implement cancellation logic.
        }
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
        runtime.process(requestContext);
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
