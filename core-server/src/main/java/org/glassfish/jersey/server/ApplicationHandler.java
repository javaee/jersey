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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NameBinding;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.Version;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.ProviderBag;
import org.glassfish.jersey.model.internal.RankedProvider;
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
import org.glassfish.jersey.server.model.ResourceModelValidator;
import org.glassfish.jersey.server.model.internal.ModelErrors;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.wadl.WadlApplicationContext;
import org.glassfish.jersey.server.wadl.internal.WadlApplicationContextImpl;
import org.glassfish.jersey.server.wadl.internal.WadlResource;
import org.glassfish.jersey.spi.Errors;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;

import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.AbstractFuture;

/**
 * Jersey server-side application handler.
 * <p>
 * Container implementations use the {@code ApplicationHandler} API to process requests
 * by invoking the {@link #handle(ContainerRequest) handle(requestContext)}
 * method on a configured application  handler instance.
 * </p>
 * <p>
 * {@code ApplicationHandler} provides two implementations of {@link Configurable config} that can be injected
 * into the application classes. The first is {@link ResourceConfig resource config} which implements {@code Configurable} itself
 * and is configured by the user. The resource config is not modified by this application handler so the future reloads of the
 * application is not disrupted by providers found on a classpath. This config can
 * be injected only as {@code ResourceConfig} or {@code Application}. The second one can be injected into the
 * {@code Configurable} parameters / fields and contains info about all the properties / provider classes / provider instances
 * from the resource config and also about all the providers found during processing classes registered under
 * {@link ServerProperties server properties}. After the application handler is initialized both configurations are marked as
 * read-only.
 * </p>
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see ResourceConfig
 * @see Configurable
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
                return ApplicationHandler.this.application;
            }

            @Override
            public void dispose(Application instance) {
                //not used
            }
        }

        private class ResourceConfigProvider implements Factory<ResourceConfig.RuntimeResourceConfig> {

            @Override
            public ResourceConfig.RuntimeResourceConfig provide() {
                return ApplicationHandler.this.resourceConfig;
            }

            @Override
            public void dispose(ResourceConfig.RuntimeResourceConfig instance) {
                //not used
            }
        }

        @Override
        protected void configure() {
            bindFactory(new ResourceConfigProvider()).to(ResourceConfig.class).to(Configurable.class).in(Singleton.class);
            bindFactory(new JaxrsApplicationProvider()).to(Application.class).in(Singleton.class);
            bind(ApplicationHandler.this).to(ApplicationHandler.class);
        }
    }

    private final Application application;
    private final ResourceConfig.RuntimeResourceConfig resourceConfig;
    private final ServiceLocator locator;
    private ServerRuntime runtime;

    /**
     * Create a new Jersey application handler using a default configuration.
     */
    public ApplicationHandler() {
        this.application = new Application();
        this.resourceConfig = new ResourceConfig.RuntimeResourceConfig();
        this.locator = Injections.createLocator(new ServerBinder(), new ApplicationBinder());

        Errors.processWithException(new Errors.Closure<Void>() {
            @Override
            public Void invoke() {
                initialize();
                return null;
            }
        });
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
        this.application = createApplication(jaxrsApplicationClass);
        this.resourceConfig = createResourceConfig(application);

        Errors.processWithException(new Errors.Closure<Void>() {
            @Override
            public Void invoke() {
                initialize();
                return null;
            }
        });
    }

    /**
     * Create a new Jersey server-side application handler configured by an instance
     * of a {@link Application JAX-RS Application sub-class}.
     *
     * @param application an instance of a JAX-RS {@code Application} (sub-)class that
     *                    will be used to configure the new Jersey application handler.
     */
    public ApplicationHandler(Application application) {
        this.locator = Injections.createLocator(new ServerBinder(), new ApplicationBinder());
        this.application = application;
        this.resourceConfig = createResourceConfig(application);

        Errors.processWithException(new Errors.Closure<Void>() {
            @Override
            public Void invoke() {
                initialize();
                return null;
            }
        });
    }

    private ResourceConfig.RuntimeResourceConfig createResourceConfig(Application application) {
        if (application instanceof ResourceConfig) {
            final ResourceConfig resourceConfig = (ResourceConfig) application;
            final Class<? extends Application> applicationClass = resourceConfig.getApplicationClass();
            if (applicationClass != null) {
                final Application app = createApplication(applicationClass);

                // (JERSEY-1094) If the application is an instance of ResourceConfig then register it's
                // custom binders into the HK2 service locator so they can be used right away.
                registerAdditionalBinders(resourceConfig.getCustomBinders());

                resourceConfig.setApplication(app);
            }
        }
        return ResourceConfig.newRuntimeResourceConfig(application);
    }

    /**
     * Assumes the configuration field is initialized with a valid ResourceConfig.
     */
    private void initialize() {
        LOGGER.info(LocalizationMessages.INIT_MSG(Version.getBuildId()));

        // Lock original ResourceConfig.
        if (application instanceof ResourceConfig) {
            ((ResourceConfig)application).lock();
        }

        registerAdditionalBinders(resourceConfig.getCustomBinders());

        // Introspecting classes & instances
        final ResourceBag.Builder resourceBagBuilder = new ResourceBag.Builder();
        final Set<Class<?>> classes = new HashSet<Class<?>>(resourceConfig.getClasses());

        boolean wadlDisabled = resourceConfig.isProperty(ServerProperties.FEATURE_DISABLE_WADL);
        if (!wadlDisabled) {
            classes.add(WadlResource.class);
        }

        for (Class<?> c : classes) {
            if (c == null) {
                LOGGER.warning(LocalizationMessages.NON_INSTANTIABLE_CLASS(c));
                break;
            }

            try {
                Resource resource = Resource.from(c);
                if (resource != null) {
                    resourceBagBuilder.registerResource(c, resource, resourceConfig.getProviderBag().getContractProvider(c));
                } else {
                    resourceConfig.register(c);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.warning(ex.getMessage());
            }
        }

        for (Object o : resourceConfig.getSingletons()) {
            if(o == null) {
                LOGGER.warning(LocalizationMessages.NON_INSTANTIABLE_CLASS(o));
                break;
            }

            try {
                Resource resource = Resource.from(o);
                if (resource != null) {
                    resourceBagBuilder.registerResource(o, resource, resourceConfig.getProviderBag().getContractProvider(o.getClass()));
                } else {
                    resourceConfig.register(o);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.warning(ex.getMessage());
            }
        }

        // Configure features.
        ProviderBinder.configureFeatures(resourceConfig.getFeatureBag(), resourceConfig, locator);

        resourceConfig.lock();

        // Adding programmatic resource models
        for (Resource programmaticResource : resourceConfig.getResources()) {
            resourceBagBuilder.registerProgrammaticResource(programmaticResource);
        }

        final ResourceBag resourceBag = resourceBagBuilder.build();
        final ProviderBag providerBag = resourceConfig.getProviderBag();

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
                ReflectionHelper.getAnnotationTypes(application.getClass(), NameBinding.class);

        // find all filters, interceptors and dynamic features
        final Iterable<RankedProvider<ContainerResponseFilter>> responseFilters = Providers.getAllRankedProviders(locator,
                ContainerResponseFilter.class);
        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<ContainerResponseFilter>> nameBoundResponseFilters
                = filterNameBound(responseFilters, null, providerBag, applicationNameBindings);

        final Iterable<RankedProvider<ContainerRequestFilter>> requestFilters = Providers.getAllRankedProviders(locator,
                ContainerRequestFilter.class);
        final List<RankedProvider<ContainerRequestFilter>> preMatchFilters = Lists.newArrayList();
        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<ContainerRequestFilter>> nameBoundRequestFilters
                = filterNameBound(requestFilters, preMatchFilters, providerBag, applicationNameBindings);

        final Iterable<RankedProvider<ReaderInterceptor>> readerInterceptors = Providers.getAllRankedProviders(locator,
                ReaderInterceptor.class);
        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<ReaderInterceptor>> nameBoundReaderInterceptors
                = filterNameBound(readerInterceptors, null, providerBag, applicationNameBindings);

        final Iterable<RankedProvider<WriterInterceptor>> writerInterceptors = Providers.getAllRankedProviders(locator,
                WriterInterceptor.class);
        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<WriterInterceptor>> nameBoundWriterInterceptors
                = filterNameBound(writerInterceptors, null, providerBag, applicationNameBindings);
        final Iterable<DynamicFeature> dynamicFeatures = Providers.getAllProviders(locator, DynamicFeature.class);

        // validate the models
        validate(resourceBag.models);

        // create a router
        DynamicConfiguration dynamicConfiguration = Injections.getConfiguration(locator);
        Injections.addBinding(Injections.newBinder(new WadlApplicationContextImpl(resourceBag.getRootResources(), resourceConfig))
                .to(WadlApplicationContext.class), dynamicConfiguration);
        dynamicConfiguration.commit();

        final RuntimeModelBuilder runtimeModelBuilder = locator.getService(RuntimeModelBuilder.class);
        runtimeModelBuilder.setGlobalInterceptors(readerInterceptors, writerInterceptors);
        runtimeModelBuilder.setBoundProviders(nameBoundRequestFilters, nameBoundResponseFilters, nameBoundReaderInterceptors,
                nameBoundWriterInterceptors, dynamicFeatures);
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
        for (Object instance : providerBag.getInstances()) {
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
    private static <T> MultivaluedMap<Class<? extends Annotation>, RankedProvider<T>> filterNameBound(
            final Iterable<RankedProvider<T>> all,
            final Collection<RankedProvider<ContainerRequestFilter>> preMatching,
            final ProviderBag providerBag,
            final Collection<Class<? extends Annotation>> applicationNameBindings) {

        final MultivaluedMap<Class<? extends Annotation>, RankedProvider<T>> result
                = new MultivaluedHashMap<Class<? extends Annotation>, RankedProvider<T>>();

        for (Iterator<RankedProvider<T>> it = all.iterator(); it.hasNext(); ) {
            RankedProvider<T> provider = it.next();
            final Class<?> providerClass = provider.getProvider().getClass();

            ContractProvider model = providerBag.getModels().get(providerClass);
            if (model == null) {
                // the provider was (most likely) bound in HK2 externally
                model = ContractProvider.from(providerClass);
            }

            if (preMatching != null && providerClass.getAnnotation(PreMatching.class) != null) {
                it.remove();
                preMatching.add(new RankedProvider<ContainerRequestFilter>((ContainerRequestFilter) provider.getProvider(),
                                model.getPriority(ContainerRequestFilter.class)));
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
            ContractProvider providerModel = resourceBag.contractProviders.get(resourceClass);
            if (bindWithComponentProvider(resourceClass, providerModel, componentProviders)) {
                continue;
            }

            if (!Resource.isAcceptable(resourceClass)) {
                LOGGER.warning(LocalizationMessages.NON_INSTANTIABLE_CLASS(resourceClass));
                continue;
            }

            if (providerModel != null &&
                    !providerModel.getContracts().isEmpty() &&
                    !Providers.checkProviderRuntime(
                            resourceClass,
                            ConstrainedTo.Type.SERVER,
                            !resourceConfig.getRegisteredClasses().contains(resourceClass),
                            true)) {
                providerModel = null;
            }
            resourceContext.unsafeBindResource(resourceClass, providerModel, dc);
        }
        // Bind resource instances
        for (Object resourceInstance : resourceBag.instances) {
            ContractProvider providerModel = resourceBag.contractProviders.get(resourceInstance.getClass());
            // TODO Try to bind resource instances using component providers?

            if (providerModel != null &&
                    !providerModel.getContracts().isEmpty() &&
                    !Providers.checkProviderRuntime(
                            resourceInstance.getClass(),
                            ConstrainedTo.Type.SERVER,
                            !resourceConfig.getRegisteredClasses().contains(resourceInstance.getClass()),
                            true)) {
                providerModel = null;
            }
            resourceContext.unsafeBindResource(resourceInstance, providerModel, dc);
        }
        // Bind providers.
        ProviderBinder.bindProviders(providerBag, ConstrainedTo.Type.SERVER, resourceConfig.getRegisteredClasses(), dc);

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

    private void validate(List<Resource> resources) {
        final ResourceModelValidator validator = new BasicValidator(locator);

        for (Resource r : resources) {
            validator.validate(r);
        }
        if (Errors.fatalIssuesFound()) {
            throw new ModelValidationException(ModelErrors.getErrorsAsResourceModelIssues());
        }
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
                response.getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
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
                        LOGGER.log(Level.WARNING, LocalizationMessages.SUSPEND_HANDLER_EXECUTION_FAILED(), throwable);
                    }
                }
            };
            synchronized (runtimeLock) {
                if (!suspended) {
                    throw new IllegalStateException(LocalizationMessages.SUSPEND_NOT_SUSPENDED());
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
                    LOGGER.log(Level.WARNING, LocalizationMessages.SUSPEND_SHEDULING_ERROR(), ex);
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
        return resourceConfig;
    }
}
