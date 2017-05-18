/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.AutoDiscoverableConfigurator;
import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.Version;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.CompositeBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.message.internal.NullOutputStream;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.model.internal.ManagedObjectsFinalizer;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.process.internal.ChainableStage;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.JerseyRequestTimeoutHandler;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.inject.ParamConverterConfigurator;
import org.glassfish.jersey.server.internal.inject.ParamExtractorConfigurator;
import org.glassfish.jersey.server.internal.inject.ValueParamProviderConfigurator;
import org.glassfish.jersey.server.internal.monitoring.ApplicationEventImpl;
import org.glassfish.jersey.server.internal.monitoring.CompositeApplicationEventListener;
import org.glassfish.jersey.server.internal.monitoring.MonitoringContainerListener;
import org.glassfish.jersey.server.internal.process.ReferencesInitializer;
import org.glassfish.jersey.server.internal.process.RequestProcessingConfigurator;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.internal.process.RequestProcessingContextReference;
import org.glassfish.jersey.server.internal.routing.Routing;
import org.glassfish.jersey.server.model.ComponentModelValidator;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.ModelValidationException;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.internal.ModelErrors;
import org.glassfish.jersey.server.model.internal.ResourceMethodInvokerConfigurator;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

/**
 * Jersey server-side application handler.
 * <p>
 * Container implementations use the {@code ApplicationHandler} API to process requests
 * by invoking the {@link #handle(ContainerRequest) handle(request)}
 * method on a configured application  handler instance.
 * </p>
 * <p>
 * {@code ApplicationHandler} provides two implementations of {@link javax.ws.rs.core.Configuration config} that can be injected
 * into the application classes. The first is {@link ResourceConfig resource config} which implements {@code Configuration}
 * itself and is configured by the user. The resource config is not modified by this application handler so the future reloads of
 * the application is not disrupted by providers found on a classpath. This config can
 * be injected only as {@code ResourceConfig} or {@code Application}. The second one can be injected into the
 * {@code Configuration} parameters / fields and contains info about all the properties / provider classes / provider instances
 * from the resource config and also about all the providers found during processing classes registered under
 * {@link ServerProperties server properties}. After the application handler is initialized both configurations are marked as
 * read-only.
 * </p>
 * <p>
 * Application handler instance also acts as an aggregate {@link ContainerLifecycleListener} instance
 * for the associated application. It aggregates all the registered container lifecycle listeners
 * under a single, umbrella listener, represented by this application handler instance, that delegates all container lifecycle
 * listener method calls to all the registered listeners. Jersey {@link Container containers} are expected to invoke
 * the container lifecycle methods directly on the active {@code ApplicationHandler} instance. The application handler will then
 * make sure to delegate the lifecycle listener calls further to all the container lifecycle listeners registered within the
 * application. Additionally, invoking the {@link ContainerLifecycleListener#onShutdown(Container)} method on this application
 * handler instance will release all the resources associated with the underlying application instance as well as close the
 * application-specific {@link InjectionManager injection manager}.
 * </p>
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @see ResourceConfig
 * @see javax.ws.rs.core.Configuration
 * @see org.glassfish.jersey.server.spi.ContainerProvider
 */
public final class ApplicationHandler implements ContainerLifecycleListener {

    private static final Logger LOGGER = Logger.getLogger(ApplicationHandler.class.getName());

    /**
     * Default dummy security context.
     */
    private static final SecurityContext DEFAULT_SECURITY_CONTEXT = new SecurityContext() {

        @Override
        public boolean isUserInRole(final String role) {
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

    /**
     * Configurator which initializes and register {@link ApplicationHandler} and {@link Configuration} instances into
     * {@link InjectionManager} and {@link BootstrapBag}.
     *
     * @author Petr Bouda (petr.bouda at oracle.com)
     */
    private class RuntimeConfigConfigurator implements BootstrapConfigurator {

        @Override
        public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
            ServerBootstrapBag serverBag = (ServerBootstrapBag) bootstrapBag;
            serverBag.setApplicationHandler(ApplicationHandler.this);
            serverBag.setConfiguration(ResourceConfig.createRuntimeConfig(serverBag.getApplication()));

            // TODO: Do we really need these three bindings in DI provider? What JAX-RS specification says?
            InstanceBinding<ApplicationHandler> handlerBinding =
                    Bindings.service(ApplicationHandler.this)
                            .to(ApplicationHandler.class);

            InstanceBinding<ResourceConfig> configBinding =
                    Bindings.service(serverBag.getRuntimeConfig())
                            .to(Configuration.class)
                            .to(ServerConfig.class);

            injectionManager.register(handlerBinding);
            injectionManager.register(configBinding);
        }
    }

    private Application application;
    private ResourceConfig runtimeConfig;
    private ServerRuntime runtime;
    private Iterable<ContainerLifecycleListener> containerLifecycleListeners;
    private InjectionManager injectionManager;
    private MessageBodyWorkers msgBodyWorkers;
    private ManagedObjectsFinalizer managedObjectsFinalizer;

    /**
     * Create a new Jersey application handler using a default configuration.
     */
    public ApplicationHandler() {
        this(new Application());
    }

    /**
     * Create a new Jersey server-side application handler configured by a
     * {@link Application JAX-RS Application (sub-)class}.
     *
     * @param jaxrsApplicationClass JAX-RS {@code Application} (sub-)class that will be
     *                              instantiated and used to configure the new Jersey
     *                              application handler.
     */
    public ApplicationHandler(final Class<? extends Application> jaxrsApplicationClass) {
        initialize(new ApplicationConfigurator(jaxrsApplicationClass), Injections.createInjectionManager(), null);
    }

    /**
     * Create a new Jersey server-side application handler configured by an instance
     * of a {@link Application JAX-RS Application sub-class}.
     *
     * @param application an instance of a JAX-RS {@code Application} (sub-)class that
     *                    will be used to configure the new Jersey application handler.
     */
    public ApplicationHandler(final Application application) {
        this(application, null, null);
    }

    /**
     * Create a new Jersey server-side application handler configured by an instance
     * of a {@link ResourceConfig} and a custom {@link Binder}.
     *
     * @param application  an instance of a JAX-RS {@code Application} (sub-)class that
     *                     will be used to configure the new Jersey application handler.
     * @param customBinder additional custom bindings used to configure the application's.
     */
    public ApplicationHandler(final Application application, final Binder customBinder) {
        this(application, customBinder, null);
    }

    /**
     * Create a new Jersey server-side application handler configured by an instance
     * of a {@link ResourceConfig}, custom {@link Binder} and a parent used by {@link InjectionManager}.
     *
     * @param application   an instance of a JAX-RS {@code Application} (sub-)class that
     *                      will be used to configure the new Jersey application handler.
     * @param customBinder  additional custom bindings used during {@link InjectionManager} creation.
     * @param parentManager parent used in {@link InjectionManager} for a specific DI provider.
     */
    public ApplicationHandler(final Application application, final Binder customBinder, final Object parentManager) {
        initialize(new ApplicationConfigurator(application), Injections.createInjectionManager(parentManager), customBinder);
    }

    private void initialize(ApplicationConfigurator applicationConfigurator, InjectionManager injectionManager,
            Binder customBinder) {
        LOGGER.config(LocalizationMessages.INIT_MSG(Version.getBuildId()));
        this.injectionManager = injectionManager;
        this.injectionManager.register(CompositeBinder.wrap(new ServerBinder(), customBinder));
        this.managedObjectsFinalizer = new ManagedObjectsFinalizer(injectionManager);

        ServerBootstrapBag bootstrapBag = new ServerBootstrapBag();
        bootstrapBag.setManagedObjectsFinalizer(managedObjectsFinalizer);
        List<BootstrapConfigurator> bootstrapConfigurators = Arrays.asList(
                new RequestProcessingConfigurator(),
                new RequestScope.RequestScopeConfigurator(),
                new ParamConverterConfigurator(),
                new ParamExtractorConfigurator(),
                new ValueParamProviderConfigurator(),
                new JerseyResourceContextConfigurator(),
                new ComponentProviderConfigurator(),
                new JaxrsProviders.ProvidersConfigurator(),
                applicationConfigurator,
                new RuntimeConfigConfigurator(),
                new ContextResolverFactory.ContextResolversConfigurator(),
                new MessageBodyFactory.MessageBodyWorkersConfigurator(),
                new ExceptionMapperFactory.ExceptionMappersConfigurator(),
                new ResourceMethodInvokerConfigurator(),
                new ProcessingProvidersConfigurator(),
                new ContainerProviderConfigurator(RuntimeType.SERVER),
                new AutoDiscoverableConfigurator(RuntimeType.SERVER));

        bootstrapConfigurators.forEach(configurator -> configurator.init(injectionManager, bootstrapBag));

        this.runtime = Errors.processWithException(
                () -> initialize(injectionManager, bootstrapConfigurators, bootstrapBag));
        this.containerLifecycleListeners = Providers.getAllProviders(injectionManager, ContainerLifecycleListener.class);
    }

    /**
     * Assumes the configuration field is initialized with a valid ResourceConfig.
     */
    private ServerRuntime initialize(InjectionManager injectionManager, List<BootstrapConfigurator> bootstrapConfigurators,
            ServerBootstrapBag bootstrapBag) {

        this.application = bootstrapBag.getApplication();
        this.runtimeConfig = bootstrapBag.getRuntimeConfig();

        // Register the binders which are dependent on "Application.properties()"
        injectionManager.register(new MessagingBinders.MessageBodyProviders(application.getProperties(), RuntimeType.SERVER));

        // Lock original ResourceConfig.
        if (application instanceof ResourceConfig) {
            ((ResourceConfig) application).lock();
        }

        CompositeApplicationEventListener compositeListener = null;

        Errors.mark(); // mark begin of validation phase
        try {
            // TODO: Create as a configurator? / The same code in ClientConfig.
            // AutoDiscoverable.
            if (!CommonProperties.getValue(runtimeConfig.getProperties(), RuntimeType.SERVER,
                    CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.FALSE, Boolean.class)) {
                runtimeConfig.configureAutoDiscoverableProviders(injectionManager, bootstrapBag.getAutoDiscoverables());
            } else {
                runtimeConfig.configureForcedAutoDiscoverableProviders(injectionManager);
            }

            // Configure binders and features.
            runtimeConfig.configureMetaProviders(injectionManager, bootstrapBag.getManagedObjectsFinalizer());

            ResourceBagConfigurator resourceBagConfigurator = new ResourceBagConfigurator();
            resourceBagConfigurator.init(injectionManager, bootstrapBag);

            runtimeConfig.lock();

            ExternalRequestScopeConfigurator externalRequestScopeConfigurator = new ExternalRequestScopeConfigurator();
            externalRequestScopeConfigurator.init(injectionManager, bootstrapBag);

            ModelProcessorConfigurator modelProcessorConfigurator = new ModelProcessorConfigurator();
            modelProcessorConfigurator.init(injectionManager, bootstrapBag);

            ResourceModelConfigurator resourceModelConfigurator = new ResourceModelConfigurator();
            resourceModelConfigurator.init(injectionManager, bootstrapBag);

            ServerExecutorProvidersConfigurator executorProvidersConfigurator = new ServerExecutorProvidersConfigurator();
            executorProvidersConfigurator.init(injectionManager, bootstrapBag);

            injectionManager.completeRegistration();

            bootstrapConfigurators.forEach(configurator -> configurator.postInit(injectionManager, bootstrapBag));
            resourceModelConfigurator.postInit(injectionManager, bootstrapBag);

            Iterable<ApplicationEventListener> appEventListeners =
                    Providers.getAllProviders(injectionManager, ApplicationEventListener.class, new RankedComparator<>());

            if (appEventListeners.iterator().hasNext()) {
                ResourceBag resourceBag = bootstrapBag.getResourceBag();
                compositeListener = new CompositeApplicationEventListener(appEventListeners);
                compositeListener.onEvent(new ApplicationEventImpl(ApplicationEvent.Type.INITIALIZATION_START,
                        this.runtimeConfig, runtimeConfig.getComponentBag().getRegistrations(),
                        resourceBag.classes, resourceBag.instances, null));
            }

            if (!disableValidation()) {
                ComponentModelValidator validator = new ComponentModelValidator(
                        bootstrapBag.getValueParamProviders(), bootstrapBag.getMessageBodyWorkers());
                    validator.validate(bootstrapBag.getResourceModel());
            }

            if (Errors.fatalIssuesFound() && !ignoreValidationError()) {
                throw new ModelValidationException(LocalizationMessages.RESOURCE_MODEL_VALIDATION_FAILED_AT_INIT(),
                        ModelErrors.getErrorsAsResourceModelIssues(true));
            }
        } finally {
            if (ignoreValidationError()) {
                Errors.logErrors(true);
                Errors.reset(); // reset errors to the state before validation phase
            } else {
                Errors.unmark();
            }
        }

        this.msgBodyWorkers = bootstrapBag.getMessageBodyWorkers();

        // assembly request processing chain
        ProcessingProviders processingProviders = bootstrapBag.getProcessingProviders();
        final ContainerFilteringStage preMatchRequestFilteringStage = new ContainerFilteringStage(
                processingProviders.getPreMatchFilters(),
                processingProviders.getGlobalResponseFilters());
        final ChainableStage<RequestProcessingContext> routingStage =
                Routing.forModel(bootstrapBag.getResourceModel().getRuntimeResourceModel())
                    .resourceContext(bootstrapBag.getResourceContext())
                    .configuration(runtimeConfig)
                    .entityProviders(msgBodyWorkers)
                    .valueSupplierProviders(bootstrapBag.getValueParamProviders())
                    .modelProcessors(Providers.getAllRankedSortedProviders(injectionManager, ModelProcessor.class))
                    .createService(serviceType -> Injections.getOrCreate(injectionManager, serviceType))
                    .processingProviders(processingProviders)
                    .resourceMethodInvokerBuilder(bootstrapBag.getResourceMethodInvokerBuilder())
                    .buildStage();
        /*
         *  Root linear request acceptor. This is the main entry point for the whole request processing.
         */
        final ContainerFilteringStage resourceFilteringStage =
                new ContainerFilteringStage(processingProviders.getGlobalRequestFilters(), null);

        final ReferencesInitializer referencesInitializer = new ReferencesInitializer(injectionManager,
                () -> injectionManager.getInstance(RequestProcessingContextReference.class));

        final Stage<RequestProcessingContext> rootStage = Stages
                .chain(referencesInitializer)
                .to(preMatchRequestFilteringStage)
                .to(routingStage)
                .to(resourceFilteringStage)
                .build(Routing.matchedEndpointExtractor());

        ServerRuntime serverRuntime = ServerRuntime.createServerRuntime(
                injectionManager, bootstrapBag, rootStage, compositeListener, processingProviders);

        // Inject instances.
        ComponentBag componentBag = runtimeConfig.getComponentBag();
        ResourceBag resourceBag = bootstrapBag.getResourceBag();
        for (final Object instance : componentBag.getInstances(ComponentBag.excludeMetaProviders(injectionManager))) {
            injectionManager.inject(instance);
        }
        for (final Object instance : resourceBag.instances) {
            injectionManager.inject(instance);
        }

        logApplicationInitConfiguration(injectionManager, resourceBag, processingProviders);

        if (compositeListener != null) {
            ApplicationEvent initFinishedEvent = new ApplicationEventImpl(
                    ApplicationEvent.Type.INITIALIZATION_APP_FINISHED, runtimeConfig,
                    componentBag.getRegistrations(), resourceBag.classes, resourceBag.instances,
                    bootstrapBag.getResourceModel());
            compositeListener.onEvent(initFinishedEvent);

            MonitoringContainerListener containerListener = injectionManager.getInstance(MonitoringContainerListener.class);
            containerListener.init(compositeListener, initFinishedEvent);
        }

        return serverRuntime;
    }

    private boolean ignoreValidationError() {
        return ServerProperties.getValue(runtimeConfig.getProperties(),
                ServerProperties.RESOURCE_VALIDATION_IGNORE_ERRORS,
                Boolean.FALSE,
                Boolean.class);
    }

    private boolean disableValidation() {
        return ServerProperties.getValue(runtimeConfig.getProperties(),
                ServerProperties.RESOURCE_VALIDATION_DISABLE,
                Boolean.FALSE,
                Boolean.class);
    }

    private static void logApplicationInitConfiguration(final InjectionManager injectionManager,
                                                        final ResourceBag resourceBag,
                                                        final ProcessingProviders processingProviders) {
        if (!LOGGER.isLoggable(Level.CONFIG)) {
            return;
        }

        final StringBuilder sb = new StringBuilder(LocalizationMessages.LOGGING_APPLICATION_INITIALIZED()).append('\n');

        final List<Resource> rootResourceClasses = resourceBag.getRootResources();

        if (!rootResourceClasses.isEmpty()) {
            sb.append(LocalizationMessages.LOGGING_ROOT_RESOURCE_CLASSES()).append(":");
            for (final Resource r : rootResourceClasses) {
                for (final Class clazz : r.getHandlerClasses()) {
                    sb.append('\n').append("  ").append(clazz.getName());
                }
            }
        }

        sb.append('\n');

        final Set<MessageBodyReader> messageBodyReaders;
        final Set<MessageBodyWriter> messageBodyWriters;

        if (LOGGER.isLoggable(Level.FINE)) {
            Spliterator<MessageBodyReader> mbrSpliterator =
                    Providers.getAllProviders(injectionManager, MessageBodyReader.class).spliterator();
            messageBodyReaders = StreamSupport.stream(mbrSpliterator, false).collect(Collectors.toSet());

            Spliterator<MessageBodyWriter> mbwSpliterator =
                    Providers.getAllProviders(injectionManager, MessageBodyWriter.class).spliterator();
            messageBodyWriters = StreamSupport.stream(mbwSpliterator, false).collect(Collectors.toSet());
        } else {
            messageBodyReaders = Providers.getCustomProviders(injectionManager, MessageBodyReader.class);
            messageBodyWriters = Providers.getCustomProviders(injectionManager, MessageBodyWriter.class);
        }

        printProviders(LocalizationMessages.LOGGING_PRE_MATCH_FILTERS(),
                processingProviders.getPreMatchFilters(), sb);
        printProviders(LocalizationMessages.LOGGING_GLOBAL_REQUEST_FILTERS(),
                processingProviders.getGlobalRequestFilters(), sb);
        printProviders(LocalizationMessages.LOGGING_GLOBAL_RESPONSE_FILTERS(),
                processingProviders.getGlobalResponseFilters(), sb);
        printProviders(LocalizationMessages.LOGGING_GLOBAL_READER_INTERCEPTORS(),
                processingProviders.getGlobalReaderInterceptors(), sb);
        printProviders(LocalizationMessages.LOGGING_GLOBAL_WRITER_INTERCEPTORS(),
                processingProviders.getGlobalWriterInterceptors(), sb);
        printNameBoundProviders(LocalizationMessages.LOGGING_NAME_BOUND_REQUEST_FILTERS(),
                processingProviders.getNameBoundRequestFilters(), sb);
        printNameBoundProviders(LocalizationMessages.LOGGING_NAME_BOUND_RESPONSE_FILTERS(),
                processingProviders.getNameBoundResponseFilters(), sb);
        printNameBoundProviders(LocalizationMessages.LOGGING_NAME_BOUND_READER_INTERCEPTORS(),
                processingProviders.getNameBoundReaderInterceptors(), sb);
        printNameBoundProviders(LocalizationMessages.LOGGING_NAME_BOUND_WRITER_INTERCEPTORS(),
                processingProviders.getNameBoundWriterInterceptors(), sb);
        printProviders(LocalizationMessages.LOGGING_DYNAMIC_FEATURES(),
                processingProviders.getDynamicFeatures(), sb);
        printProviders(LocalizationMessages.LOGGING_MESSAGE_BODY_READERS(),
                       messageBodyReaders.stream().map(new WorkersToStringTransform<>()).collect(Collectors.toList()), sb);
        printProviders(LocalizationMessages.LOGGING_MESSAGE_BODY_WRITERS(),
                       messageBodyWriters.stream().map(new WorkersToStringTransform<>()).collect(Collectors.toList()), sb);

        LOGGER.log(Level.CONFIG, sb.toString());
    }

    private static class WorkersToStringTransform<T> implements Function<T, String> {

        @Override
        public String apply(final T t) {
            if (t != null) {
                return t.getClass().getName();
            }
            return null;
        }
    }

    private static <T> void printNameBoundProviders(final String title,
                                                    final Map<Class<? extends Annotation>, List<RankedProvider<T>>> providers,
                                                    final StringBuilder sb) {
        if (!providers.isEmpty()) {
            sb.append(title).append(":").append('\n');

            for (final Map.Entry<Class<? extends Annotation>, List<RankedProvider<T>>> entry : providers.entrySet()) {
                for (final RankedProvider rankedProvider : entry.getValue()) {
                    sb.append("   ")
                            .append(LocalizationMessages.LOGGING_PROVIDER_BOUND(rankedProvider, entry.getKey()))
                            .append('\n');
                }
            }
        }
    }

    private static <T> void printProviders(final String title, final Iterable<T> providers, final StringBuilder sb) {
        final Iterator<T> iterator = providers.iterator();
        boolean first = true;
        while (iterator.hasNext()) {
            if (first) {
                sb.append(title).append(":").append('\n');
                first = false;
            }
            final T provider = iterator.next();
            sb.append("   ").append(provider).append('\n');
        }
    }

    /**
     * Invokes a request and returns the {@link Future response future}.
     *
     * @param requestContext request data.
     * @return response future.
     */
    public Future<ContainerResponse> apply(final ContainerRequest requestContext) {
        return apply(requestContext, new NullOutputStream());
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
        final FutureResponseWriter responseFuture =
                new FutureResponseWriter(request.getMethod(), outputStream, runtime.getBackgroundScheduler());

        if (request.getSecurityContext() == null) {
            request.setSecurityContext(DEFAULT_SECURITY_CONTEXT);
        }
        request.setWriter(responseFuture);

        handle(request);

        return responseFuture;
    }

    private static class FutureResponseWriter extends CompletableFuture<ContainerResponse> implements ContainerResponseWriter {

        private ContainerResponse response = null;

        private final String requestMethodName;
        private final OutputStream outputStream;

        private final JerseyRequestTimeoutHandler requestTimeoutHandler;

        private FutureResponseWriter(final String requestMethodName,
                                     final OutputStream outputStream,
                                     final ScheduledExecutorService backgroundScheduler) {
            this.requestMethodName = requestMethodName;
            this.outputStream = outputStream;
            this.requestTimeoutHandler = new JerseyRequestTimeoutHandler(this, backgroundScheduler);
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(final long contentLength, final ContainerResponse response) {
            this.response = response;

            if (contentLength >= 0) {
                response.getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
            }

            return outputStream;
        }

        @Override
        public boolean suspend(final long time, final TimeUnit unit, final TimeoutHandler handler) {
            return requestTimeoutHandler.suspend(time, unit, handler);
        }

        @Override
        public void setSuspendTimeout(final long time, final TimeUnit unit) {
            requestTimeoutHandler.setSuspendTimeout(time, unit);
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
                requestTimeoutHandler.close();
                super.complete(current);
            }
        }

        @Override
        public void failure(final Throwable error) {
            requestTimeoutHandler.close();
            super.completeExceptionally(error);
        }

        @Override
        public boolean enableResponseBuffering() {
            return true;
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
     * @param request container request context of the current request.
     */
    public void handle(final ContainerRequest request) {
        request.setWorkers(msgBodyWorkers);
        runtime.process(request);
    }

    /**
     * Returns {@link InjectionManager} relevant to current application.
     *
     * @return {@link InjectionManager} instance.
     * @since 2.26
     */
    public InjectionManager getInjectionManager() {
        return injectionManager;
    }

    /**
     * Get the application configuration.
     *
     * @return application configuration.
     */
    public ResourceConfig getConfiguration() {
        return runtimeConfig;
    }

    // Aggregate container lifecycle listener implementation

    @Override
    public void onStartup(final Container container) {
        for (final ContainerLifecycleListener listener : containerLifecycleListeners) {
            listener.onStartup(container);
        }
    }

    @Override
    public void onReload(final Container container) {
        for (final ContainerLifecycleListener listener : containerLifecycleListeners) {
            listener.onReload(container);
        }
    }

    @Override
    public void onShutdown(final Container container) {
        try {
            for (final ContainerLifecycleListener listener : containerLifecycleListeners) {
                listener.onShutdown(container);
            }
        } finally {
            try {
                // Call @PreDestroy method on Application.
                injectionManager.preDestroy(ResourceConfig.unwrapApplication(application));
            } finally {
                // Shutdown ServiceLocator.
                // Takes care of the injected executors & schedulers shut-down too.
                managedObjectsFinalizer.preDestroy();
                injectionManager.shutdown();
            }
        }
    }
}
