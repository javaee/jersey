/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Type;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NameBinding;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.ServiceConfigurationError;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.Version;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.JerseyClassAnalyzer;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.NullOutputStream;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedComparator.Order;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.process.internal.ChainableStage;
import org.glassfish.jersey.process.internal.ExecutorProviders;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.JerseyRequestTimeoutHandler;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.monitoring.ApplicationEventImpl;
import org.glassfish.jersey.server.internal.monitoring.CompositeApplicationEventListener;
import org.glassfish.jersey.server.internal.monitoring.MonitoringContainerListener;
import org.glassfish.jersey.server.internal.process.ReferencesInitializer;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.internal.routing.Routing;
import org.glassfish.jersey.server.model.ComponentModelValidator;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.ModelValidationException;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.internal.ModelErrors;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.ExternalRequestScope;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.binding.ScopedBindingBuilder;

import org.jvnet.hk2.external.runtime.ServiceLocatorRuntimeBean;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.base.Predicate;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.util.concurrent.AbstractFuture;

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
 * application-specific {@link org.glassfish.hk2.api.ServiceLocator HK2 ServiceLocator}.
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

    private class ApplicationBinder extends AbstractBinder {

        private class JaxrsApplicationProvider implements Factory<Application> {

            @Override
            public Application provide() {
                return ApplicationHandler.this.application;
            }

            @Override
            public void dispose(final Application instance) {
                //not used
            }
        }

        private class RuntimeConfigProvider implements Factory<ServerConfig> {

            @Override
            public ServerConfig provide() {
                return ApplicationHandler.this.runtimeConfig;
            }

            @Override
            public void dispose(final ServerConfig instance) {
                //not used
            }
        }

        @Override
        protected void configure() {
            bindFactory(new RuntimeConfigProvider()).to(ServerConfig.class).to(Configuration.class).in(Singleton.class);
            bindFactory(new JaxrsApplicationProvider()).to(Application.class).in(Singleton.class);
            bind(ApplicationHandler.this).to(ApplicationHandler.class);
        }
    }

    private final Application application;
    private final ResourceConfig runtimeConfig;
    private final ServiceLocator locator;
    private final ServerRuntime runtime;
    private final Iterable<ContainerLifecycleListener> containerLifecycleListeners;

    private MessageBodyWorkers msgBodyWorkers;

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
        this.locator = Injections.createLocator(new ServerBinder(null), new ApplicationBinder());
        locator.setDefaultClassAnalyzerName(JerseyClassAnalyzer.NAME);

        final LazyValue<Iterable<ComponentProvider>> componentProviders = getLazyInitializedComponentProviders(locator);
        this.application = createApplication(jaxrsApplicationClass, componentProviders);
        this.runtimeConfig = ResourceConfig.createRuntimeConfig(application);

        this.runtime = Errors.processWithException(new Producer<ServerRuntime>() {
            @Override
            public ServerRuntime call() {
                return initialize(componentProviders.get());
            }
        });

        this.containerLifecycleListeners = Providers.getAllProviders(locator, ContainerLifecycleListener.class);
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
     * @param customBinder additional custom bindings used to configure the application's {@link ServiceLocator}.
     */
    public ApplicationHandler(final Application application, final Binder customBinder) {
        this(application, customBinder, null);
    }

    /**
     * Create a new Jersey server-side application handler configured by an instance
     * of a {@link ResourceConfig}, custom {@link Binder} and a parent {@link org.glassfish.hk2.api.ServiceLocator}.
     *
     * @param application  an instance of a JAX-RS {@code Application} (sub-)class that
     *                     will be used to configure the new Jersey application handler.
     * @param customBinder additional custom bindings used during {@link ServiceLocator} creation.
     * @param parent       parent {@link ServiceLocator} instance.
     */
    public ApplicationHandler(final Application application, final Binder customBinder, final ServiceLocator parent) {
        if (customBinder == null) {
            this.locator = Injections.createLocator(
                    parent,
                    new ServerBinder(application.getProperties()),
                    new ApplicationBinder());
        } else {
            this.locator = Injections.createLocator(parent,
                    new ServerBinder(application.getProperties()),
                    new ApplicationBinder(),
                    customBinder);
        }
        locator.setDefaultClassAnalyzerName(JerseyClassAnalyzer.NAME);
        final LazyValue<Iterable<ComponentProvider>> componentProviders = getLazyInitializedComponentProviders(locator);

        this.application = application;
        if (application instanceof ResourceConfig) {
            final ResourceConfig rc = (ResourceConfig) application;
            if (rc.getApplicationClass() != null) {
                rc.setApplication(createApplication(rc.getApplicationClass(), componentProviders));
            }
        }
        this.runtimeConfig = ResourceConfig.createRuntimeConfig(application);

        this.runtime = Errors.processWithException(new Producer<ServerRuntime>() {
            @Override
            public ServerRuntime call() {
                return initialize(componentProviders.get());
            }
        });

        this.containerLifecycleListeners = Providers.getAllProviders(locator, ContainerLifecycleListener.class);
    }

    private Application createApplication(final Class<? extends Application> applicationClass,
                                          final Value<Iterable<ComponentProvider>> componentProvidersValue) {
        // need to handle ResourceConfig and Application separately as invoking forContract() on these
        // will trigger the factories which we don't want at this point
        if (applicationClass == ResourceConfig.class) {
            return new ResourceConfig();
        } else if (applicationClass == Application.class) {
            return new Application();
        } else {
            Iterable<ComponentProvider> componentProviders = componentProvidersValue.get();
            boolean appClassBound = false;
            for (ComponentProvider cp : componentProviders) {
                if (cp.bind(applicationClass, Collections.<Class<?>>emptySet())) {
                    appClassBound = true;
                    break;
                }
            }
            if (!appClassBound) {
                if (applicationClass.isAnnotationPresent(Singleton.class)) {
                    final DynamicConfiguration dc = Injections.getConfiguration(locator);
                    final ScopedBindingBuilder<? extends Application> binder =
                            Injections.newBinder(applicationClass).to(applicationClass).in(Singleton.class);
                    Injections.addBinding(binder, dc);
                    dc.commit();
                    appClassBound = true;
                }
            }
            final Application app = appClassBound
                    ? locator.getService(applicationClass) : locator.createAndInitialize(applicationClass);
            if (app instanceof ResourceConfig) {
                final ResourceConfig _rc = (ResourceConfig) app;
                final Class<? extends Application> innerAppClass = _rc.getApplicationClass();
                if (innerAppClass != null) {
                    final Application innerApp = createApplication(innerAppClass, componentProvidersValue);
                    _rc.setApplication(innerApp);
                }
            }
            return app;
        }
    }

    private static LazyValue<Iterable<ComponentProvider>> getLazyInitializedComponentProviders(final ServiceLocator locator) {
        return Values.lazy(new Value<Iterable<ComponentProvider>>() {
            @Override
            public Iterable<ComponentProvider> get() {
                // Registering Injection Bindings
                List<ComponentProvider> result = new LinkedList<>();

                // Registering Injection Bindings
                for (final RankedProvider<ComponentProvider> rankedProvider : getRankedComponentProviders()) {
                    final ComponentProvider provider = rankedProvider.getProvider();
                    provider.initialize(locator);
                    result.add(provider);
                }
                return result;
            }
        });
    }

    /**
     * Assumes the configuration field is initialized with a valid ResourceConfig.
     */
    private ServerRuntime initialize(Iterable<ComponentProvider> componentProviders) {
        LOGGER.config(LocalizationMessages.INIT_MSG(Version.getBuildId()));

        // Lock original ResourceConfig.
        if (application instanceof ResourceConfig) {
            ((ResourceConfig) application).lock();
        }

        final boolean ignoreValidationErrors = ServerProperties.getValue(runtimeConfig.getProperties(),
                ServerProperties.RESOURCE_VALIDATION_IGNORE_ERRORS,
                Boolean.FALSE,
                Boolean.class);
        final boolean disableValidation = ServerProperties.getValue(runtimeConfig.getProperties(),
                ServerProperties.RESOURCE_VALIDATION_DISABLE,
                Boolean.FALSE,
                Boolean.class);

        final ResourceBag resourceBag;
        final ProcessingProviders processingProviders;
        final ComponentBag componentBag;
        ResourceModel resourceModel;
        CompositeApplicationEventListener compositeListener = null;

        Errors.mark(); // mark begin of validation phase
        try {
            // AutoDiscoverable.
            if (!CommonProperties.getValue(runtimeConfig.getProperties(), RuntimeType.SERVER,
                    CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.FALSE, Boolean.class)) {
                runtimeConfig.configureAutoDiscoverableProviders(locator);
            } else {
                runtimeConfig.configureForcedAutoDiscoverableProviders(locator);
            }

            // Configure binders and features.
            runtimeConfig.configureMetaProviders(locator);

            final ResourceBag.Builder resourceBagBuilder = new ResourceBag.Builder();

            // Adding programmatic resource models
            for (final Resource programmaticResource : runtimeConfig.getResources()) {
                resourceBagBuilder.registerProgrammaticResource(programmaticResource);
            }

            // Introspecting classes & instances
            for (final Class<?> c : runtimeConfig.getClasses()) {
                try {
                    final Resource resource = Resource.from(c, disableValidation);
                    if (resource != null) {
                        resourceBagBuilder.registerResource(c, resource);
                    }
                } catch (final IllegalArgumentException ex) {
                    LOGGER.warning(ex.getMessage());
                }
            }

            for (final Object o : runtimeConfig.getSingletons()) {
                try {
                    final Resource resource = Resource.from(o.getClass(), disableValidation);
                    if (resource != null) {
                        resourceBagBuilder.registerResource(o, resource);
                    }
                } catch (final IllegalArgumentException ex) {
                    LOGGER.warning(ex.getMessage());
                }
            }

            resourceBag = resourceBagBuilder.build();

            runtimeConfig.lock();

            componentBag = runtimeConfig.getComponentBag();
            final Class<ExternalRequestScope>[] extScopes = ServiceFinder.find(ExternalRequestScope.class, true).toClassArray();

            boolean extScopeBound = false;

            if (extScopes.length == 1) {
                for (final ComponentProvider p : componentProviders) {
                    if (p.bind(extScopes[0], new HashSet<Class<?>>() {{
                        add(ExternalRequestScope.class);
                    }})) {
                        extScopeBound = true;
                        break;
                    }
                }
            } else if (extScopes.length > 1) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    final StringBuilder scopeList = new StringBuilder("\n");
                    for (final Class<ExternalRequestScope> ers : extScopes) {
                        scopeList.append("   ").append(ers.getTypeParameters()[0]).append('\n');
                    }
                    LOGGER.warning(LocalizationMessages.WARNING_TOO_MANY_EXTERNAL_REQ_SCOPES(scopeList.toString()));
                }
            }

            if (!extScopeBound) {
                final DynamicConfiguration configuration = Injections.getConfiguration(locator);
                final ScopedBindingBuilder<ExternalRequestScope> binder = Injections
                        .newBinder((ExternalRequestScope) ServerRuntime.NOOP_EXTERNAL_REQ_SCOPE)
                        .to(ExternalRequestScope.class);
                Injections.addBinding(binder, configuration);
                configuration.commit();
            }

            bindProvidersAndResources(componentProviders, componentBag, resourceBag.classes, resourceBag.instances);
            for (final ComponentProvider componentProvider : componentProviders) {
                componentProvider.done();
            }

            final Iterable<ApplicationEventListener> appEventListeners = Providers.getAllProviders(locator,
                    ApplicationEventListener.class, new RankedComparator<ApplicationEventListener>());

            if (appEventListeners.iterator().hasNext()) {
                compositeListener = new CompositeApplicationEventListener(appEventListeners);
                compositeListener.onEvent(new ApplicationEventImpl(ApplicationEvent.Type.INITIALIZATION_START,
                        this.runtimeConfig, componentBag.getRegistrations(), resourceBag.classes, resourceBag.instances,
                        null));
            }

            processingProviders = getProcessingProviders(componentBag);

            // initialize processing provider reference
            final GenericType<Ref<ProcessingProviders>> refGenericType = new GenericType<Ref<ProcessingProviders>>() {
            };
            final Ref<ProcessingProviders> refProcessingProvider = locator.getService(refGenericType.getType());
            refProcessingProvider.set(processingProviders);

            resourceModel = new ResourceModel.Builder(resourceBag.getRootResources(), false).build();
            resourceModel = processResourceModel(resourceModel);

            if (!disableValidation) {
                final ComponentModelValidator validator = new ComponentModelValidator(locator);
                validator.validate(resourceModel);
            }

            if (Errors.fatalIssuesFound() && !ignoreValidationErrors) {
                throw new ModelValidationException(LocalizationMessages.RESOURCE_MODEL_VALIDATION_FAILED_AT_INIT(),
                        ModelErrors.getErrorsAsResourceModelIssues(true));
            }
        } finally {
            if (ignoreValidationErrors) {
                Errors.logErrors(true);
                Errors.reset(); // reset errors to the state before validation phase
            } else {
                Errors.unmark();
            }
        }

        bindEnhancingResourceClasses(resourceModel, resourceBag, componentProviders);

        ExecutorProviders.createInjectionBindings(locator);

        // initiate resource model into JerseyResourceContext
        final JerseyResourceContext jerseyResourceContext = locator.getService(JerseyResourceContext.class);
        jerseyResourceContext.setResourceModel(resourceModel);

        msgBodyWorkers = locator.getService(MessageBodyWorkers.class);

        // assembly request processing chain
        final ReferencesInitializer referencesInitializer = locator.createAndInitialize(ReferencesInitializer.class);
        final ContainerFilteringStage preMatchRequestFilteringStage = new ContainerFilteringStage(
                processingProviders.getPreMatchFilters(),
                processingProviders.getGlobalResponseFilters());
        final ChainableStage<RequestProcessingContext> routingStage = Routing.forModel(resourceModel.getRuntimeResourceModel())
                .locator(locator)
                .resourceContext(jerseyResourceContext)
                .configuration(runtimeConfig)
                .entityProviders(msgBodyWorkers)
                .processingProviders(processingProviders)
                .buildStage();
        final ContainerFilteringStage resourceFilteringStage =
                new ContainerFilteringStage(processingProviders.getGlobalRequestFilters(), null);
        /**
         *  Root linear request acceptor. This is the main entry point for the whole request processing.
         */
        final Stage<RequestProcessingContext> rootStage = Stages
                .chain(referencesInitializer)
                .to(preMatchRequestFilteringStage)
                .to(routingStage)
                .to(resourceFilteringStage)
                .build(Routing.matchedEndpointExtractor());

        final ServerRuntime serverRuntime = locator.createAndInitialize(ServerRuntime.Builder.class)
                .build(rootStage, compositeListener, processingProviders);

        // clear HK2 caches
        final ServiceLocatorRuntimeBean serviceLocatorRuntimeBean = locator.getService(ServiceLocatorRuntimeBean.class);
        if (serviceLocatorRuntimeBean != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(LocalizationMessages.CLEARING_HK_2_CACHE(
                        serviceLocatorRuntimeBean.getServiceCacheSize(),
                        serviceLocatorRuntimeBean.getReflectionCacheSize()));
            }
            serviceLocatorRuntimeBean.clearReflectionCache();
            serviceLocatorRuntimeBean.clearServiceCache();
        }

        // Inject instances.
        for (final Object instance : componentBag.getInstances(ComponentBag.EXCLUDE_META_PROVIDERS)) {
            locator.inject(instance);
        }
        for (final Object instance : resourceBag.instances) {
            locator.inject(instance);
        }

        logApplicationInitConfiguration(locator, resourceBag, processingProviders);

        if (compositeListener != null) {
            final ApplicationEvent initFinishedEvent = new ApplicationEventImpl(
                    ApplicationEvent.Type.INITIALIZATION_APP_FINISHED, runtimeConfig,
                    componentBag.getRegistrations(), resourceBag.classes, resourceBag.instances, resourceModel);
            compositeListener.onEvent(initFinishedEvent);

            final MonitoringContainerListener containerListener
                    = locator.getService(MonitoringContainerListener.class);
            containerListener.init(compositeListener, initFinishedEvent);
        }

        return serverRuntime;
    }

    private static void logApplicationInitConfiguration(final ServiceLocator locator,
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
            messageBodyReaders = Sets.newHashSet(Providers.getAllProviders(locator, MessageBodyReader.class));
            messageBodyWriters = Sets.newHashSet(Providers.getAllProviders(locator, MessageBodyWriter.class));
        } else {
            messageBodyReaders = Providers.getCustomProviders(locator, MessageBodyReader.class);
            messageBodyWriters = Providers.getCustomProviders(locator, MessageBodyWriter.class);
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
                Collections2.transform(messageBodyReaders, new WorkersToStringTransform<MessageBodyReader>()), sb);
        printProviders(LocalizationMessages.LOGGING_MESSAGE_BODY_WRITERS(),
                Collections2.transform(messageBodyWriters, new WorkersToStringTransform<MessageBodyWriter>()), sb);

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

    private static Iterable<RankedProvider<ComponentProvider>> getRankedComponentProviders() throws ServiceConfigurationError {
        final List<RankedProvider<ComponentProvider>> result = new LinkedList<>();

        for (final ComponentProvider provider : ServiceFinder.find(ComponentProvider.class)) {
            result.add(new RankedProvider<>(provider));
        }
        Collections.sort(result, new RankedComparator<ComponentProvider>(Order.DESCENDING));
        return result;
    }

    private ProcessingProviders getProcessingProviders(final ComponentBag componentBag) {

        // scan for NameBinding annotations attached to the application class
        final Collection<Class<? extends Annotation>> applicationNameBindings = ReflectionHelper.getAnnotationTypes(
                ResourceConfig.unwrapApplication(runtimeConfig).getClass(), NameBinding.class);

        // find all filters, interceptors and dynamic features
        final Iterable<RankedProvider<ContainerResponseFilter>> responseFilters = Providers.getAllRankedProviders(locator,
                ContainerResponseFilter.class);

        final MultivaluedMap<RankedProvider<ContainerResponseFilter>, Class<? extends Annotation>> nameBoundRespFiltersInverse =
                new MultivaluedHashMap<>();
        final MultivaluedMap<RankedProvider<ContainerRequestFilter>, Class<? extends Annotation>> nameBoundReqFiltersInverse =
                new MultivaluedHashMap<>();
        final MultivaluedMap<RankedProvider<ReaderInterceptor>, Class<? extends Annotation>> nameBoundReaderInterceptorsInverse =
                new MultivaluedHashMap<>();
        final MultivaluedMap<RankedProvider<WriterInterceptor>, Class<? extends Annotation>> nameBoundWriterInterceptorsInverse =
                new MultivaluedHashMap<>();

        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<ContainerResponseFilter>> nameBoundResponseFilters
                = filterNameBound(responseFilters, null, componentBag, applicationNameBindings, nameBoundRespFiltersInverse);

        final Iterable<RankedProvider<ContainerRequestFilter>> requestFilters = Providers.getAllRankedProviders(locator,
                ContainerRequestFilter.class);

        final List<RankedProvider<ContainerRequestFilter>> preMatchFilters = Lists.newArrayList();

        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<ContainerRequestFilter>> nameBoundReqFilters =
                filterNameBound(requestFilters, preMatchFilters, componentBag, applicationNameBindings,
                        nameBoundReqFiltersInverse);

        final Iterable<RankedProvider<ReaderInterceptor>> readerInterceptors = Providers.getAllRankedProviders(locator,
                ReaderInterceptor.class);

        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<ReaderInterceptor>> nameBoundReaderInterceptors =
                filterNameBound(readerInterceptors, null, componentBag, applicationNameBindings,
                        nameBoundReaderInterceptorsInverse);

        final Iterable<RankedProvider<WriterInterceptor>> writerInterceptors = Providers.getAllRankedProviders(locator,
                WriterInterceptor.class);

        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<WriterInterceptor>> nameBoundWriterInterceptors =
                filterNameBound(writerInterceptors, null, componentBag, applicationNameBindings,
                        nameBoundWriterInterceptorsInverse);

        final Iterable<DynamicFeature> dynamicFeatures = Providers.getAllProviders(locator, DynamicFeature.class);

        return new ProcessingProviders(nameBoundReqFilters,
                nameBoundReqFiltersInverse,
                nameBoundResponseFilters,
                nameBoundRespFiltersInverse,
                nameBoundReaderInterceptors,
                nameBoundReaderInterceptorsInverse,
                nameBoundWriterInterceptors,
                nameBoundWriterInterceptorsInverse,
                requestFilters,
                preMatchFilters,
                responseFilters,
                readerInterceptors,
                writerInterceptors,
                dynamicFeatures);
    }

    private ResourceModel processResourceModel(ResourceModel resourceModel) {
        final Iterable<RankedProvider<ModelProcessor>> allRankedProviders = Providers.getAllRankedProviders(locator,
                ModelProcessor.class);
        final Iterable<ModelProcessor> modelProcessors = Providers.sortRankedProviders(new RankedComparator<ModelProcessor>(),
                allRankedProviders);

        for (final ModelProcessor modelProcessor : modelProcessors) {
            resourceModel = modelProcessor.processResourceModel(resourceModel, getConfiguration());
        }
        return resourceModel;
    }

    private void bindEnhancingResourceClasses(
            final ResourceModel resourceModel,
            final ResourceBag resourceBag,
            final Iterable<ComponentProvider> componentProviders) {

        final Set<Class<?>> newClasses = Sets.newHashSet();
        final Set<Object> newInstances = Sets.newHashSet();
        for (final Resource res : resourceModel.getRootResources()) {
            newClasses.addAll(res.getHandlerClasses());
            newInstances.addAll(res.getHandlerInstances());
        }
        newClasses.removeAll(resourceBag.classes);
        newInstances.removeAll(resourceBag.instances);

        final ComponentBag emptyComponentBag = ComponentBag.newInstance(new Predicate<ContractProvider>() {
            @Override
            public boolean apply(final ContractProvider input) {
                return false;
            }
        });
        bindProvidersAndResources(componentProviders, emptyComponentBag, newClasses, newInstances);
    }

    /**
     * Takes collection of all filters/interceptors (either request/reader or response/writer)
     * and separates out all name-bound filters/interceptors, returns them as a separate MultivaluedMap,
     * mapping the name-bound annotation to the list of name-bound filters/interceptors. The same key values
     * are also added into the inverse map passed in {@code inverseNameBoundMap}.
     * <p/>
     * Note, the name-bound filters/interceptors are removed from the original filters/interceptors collection.
     * If non-null collection is passed in the postMatching parameter (applicable for filters only),
     * this method also removes all the global
     * postMatching filters from the original collection and adds them to the collection passed in the postMatching
     * parameter.
     *
     * @param all                     Collection of all filters to be processed.
     * @param preMatchingFilters      Collection into which pre-matching filters should be added.
     * @param componentBag            Component bag
     * @param applicationNameBindings Collection of name binding annotations attached to the JAX-RS application.
     * @param inverseNameBoundMap     Inverse name bound map into which the name bound providers should be inserted. The keys
     *                                are providers (filters, interceptor)
     * @return {@link MultivaluedMap} of all name-bound filters.
     */
    private static <T> MultivaluedMap<Class<? extends Annotation>, RankedProvider<T>> filterNameBound(
            final Iterable<RankedProvider<T>> all,
            final Collection<RankedProvider<ContainerRequestFilter>> preMatchingFilters,
            final ComponentBag componentBag,
            final Collection<Class<? extends Annotation>> applicationNameBindings,
            final MultivaluedMap<RankedProvider<T>, Class<? extends Annotation>> inverseNameBoundMap) {

        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<T>> result
                = new MultivaluedHashMap<>();

        for (final Iterator<RankedProvider<T>> it = all.iterator(); it.hasNext(); ) {
            final RankedProvider<T> provider = it.next();
            Class<?> providerClass = provider.getProvider().getClass();
            final Set<Type> contractTypes = provider.getContractTypes();
            if (contractTypes != null && !contractTypes.contains(providerClass)) {
                providerClass = ReflectionHelper.theMostSpecificTypeOf(contractTypes);
            }

            ContractProvider model = componentBag.getModel(providerClass);
            if (model == null) {
                // the provider was (most likely) bound in HK2 externally
                model = ComponentBag.modelFor(providerClass);
            }

            final boolean preMatching = providerClass.getAnnotation(PreMatching.class) != null;
            if (preMatching && preMatchingFilters != null) {
                it.remove();
                preMatchingFilters.add(new RankedProvider<>((ContainerRequestFilter) provider.getProvider(),
                        model.getPriority(ContainerRequestFilter.class)));
            }

            boolean nameBound = model.isNameBound();
            if (nameBound
                    && !applicationNameBindings.isEmpty()
                    && applicationNameBindings.containsAll(model.getNameBindings())) {
                // override the name-bound flag
                nameBound = false;
            }

            if (nameBound) { // not application-bound
                if (!preMatching) {
                    it.remove();
                    for (final Class<? extends Annotation> binding : model.getNameBindings()) {
                        result.add(binding, provider);
                        inverseNameBoundMap.add(provider, binding);
                    }
                } else {
                    LOGGER.warning(LocalizationMessages.PREMATCHING_ALSO_NAME_BOUND(providerClass));
                }
            }
        }

        return result;
    }

    private void bindProvidersAndResources(
            final Iterable<ComponentProvider> componentProviders,
            final ComponentBag componentBag,
            final Collection<Class<?>> resourceClasses,
            final Collection<Object> resourceInstances) {

        final JerseyResourceContext resourceContext = locator.getService(JerseyResourceContext.class);
        final DynamicConfiguration dc = Injections.getConfiguration(locator);
        final Set<Class<?>> registeredClasses = runtimeConfig.getRegisteredClasses();

        // Merge programmatic resource classes with component classes.
        final Set<Class<?>> classes = Sets.newIdentityHashSet();
        classes.addAll(Sets.filter(componentBag.getClasses(ComponentBag.EXCLUDE_META_PROVIDERS),
                new Predicate<Class<?>>() {
                    @Override
                    public boolean apply(final Class<?> componentClass) {
                        return Providers.checkProviderRuntime(
                                componentClass,
                                componentBag.getModel(componentClass),
                                RuntimeType.SERVER,
                                !registeredClasses.contains(componentClass),
                                resourceClasses.contains(componentClass));
                    }
                }
        ));
        classes.addAll(resourceClasses);

        // Bind classes.
        for (final Class<?> componentClass : classes) {

            ContractProvider model = componentBag.getModel(componentClass);

            if (bindWithComponentProvider(componentClass, model, componentProviders)) {
                continue;
            }

            if (resourceClasses.contains(componentClass)) {

                if (!Resource.isAcceptable(componentClass)) {
                    LOGGER.warning(LocalizationMessages.NON_INSTANTIABLE_COMPONENT(componentClass));
                    continue;
                }

                if (model != null && !Providers.checkProviderRuntime(
                        componentClass,
                        model,
                        RuntimeType.SERVER,
                        !registeredClasses.contains(componentClass),
                        true)) {
                    model = null;
                }
                resourceContext.unsafeBindResource(componentClass, model, dc);
            } else {
                ProviderBinder.bindProvider(componentClass, model, dc);
            }
        }

        // Merge programmatic resource instances with other component instances.
        final Set<Object> instances = Sets.newHashSet();
        instances.addAll(Sets.filter(componentBag.getInstances(ComponentBag.EXCLUDE_META_PROVIDERS),
                new Predicate<Object>() {
                    @Override
                    public boolean apply(final Object component) {
                        final Class<?> componentClass = component.getClass();
                        return Providers.checkProviderRuntime(
                                componentClass,
                                componentBag.getModel(componentClass),
                                RuntimeType.SERVER,
                                !registeredClasses.contains(componentClass),
                                resourceInstances.contains(component));
                    }
                }
        ));
        instances.addAll(resourceInstances);

        // Bind instances.
        for (final Object component : instances) {
            ContractProvider model = componentBag.getModel(component.getClass());
            if (resourceInstances.contains(component)) {
                if (model != null && !Providers.checkProviderRuntime(
                        component.getClass(),
                        model,
                        RuntimeType.SERVER,
                        !registeredClasses.contains(component.getClass()),
                        true)) {
                    model = null;
                }
                resourceContext.unsafeBindResource(component, model, dc);
            } else {
                ProviderBinder.bindProvider(component, model, dc);
            }
        }

        dc.commit();
    }

    private boolean bindWithComponentProvider(
            final Class<?> component,
            final ContractProvider providerModel,
            final Iterable<ComponentProvider> componentProviders) {

        final Set<Class<?>> contracts = providerModel == null ? Collections.<Class<?>>emptySet() : providerModel.getContracts();
        for (final ComponentProvider provider : componentProviders) {
            if (provider.bind(component, contracts)) {
                return true;
            }
        }
        return false;
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

    private static class FutureResponseWriter extends AbstractFuture<ContainerResponse> implements ContainerResponseWriter {

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
                super.set(current);
            }
        }

        @Override
        public void failure(final Throwable error) {
            requestTimeoutHandler.close();
            super.setException(error);
        }

        @Override
        public boolean enableResponseBuffering() {
            return true;
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
     * @param request container request context of the current request.
     */
    public void handle(final ContainerRequest request) {
        request.setWorkers(msgBodyWorkers);
        runtime.process(request);
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
                locator.preDestroy(ResourceConfig.unwrapApplication(application));
            } finally {
                // Shutdown ServiceLocator.
                // Takes care of the injected executors & schedulers shut-down too.
                Injections.shutdownLocator(locator);
            }
        }
    }
}
