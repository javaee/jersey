/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.CommonConfig;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.scanning.AnnotationAcceptingListener;
import org.glassfish.jersey.server.internal.scanning.FilesScanner;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.model.Resource;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;

import jersey.repackaged.com.google.common.base.Predicate;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * The resource configuration for configuring a web application.
 *
 * @author Paul Sandoz
 * @author Martin Matula
 * @author Michal Gajdos
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ResourceConfig extends Application implements Configurable<ResourceConfig>, ServerConfig {

    private static final Logger LOGGER = Logger.getLogger(ResourceConfig.class.getName());

    private transient Set<Class<?>> cachedClasses = null;
    private transient Set<Class<?>> cachedClassesView = null;
    private transient Set<Object> cachedSingletons = null;
    private transient Set<Object> cachedSingletonsView = null;

    private transient boolean resetFinders = false;

    private volatile State state;

    private static class State extends CommonConfig implements ServerConfig {

        private final Set<ResourceFinder> resourceFinders;

        private final Set<Resource> resources;
        private final Set<Resource> resourcesView;
        private volatile String applicationName;

        private volatile ClassLoader classLoader = null;

        public State() {
            super(RuntimeType.SERVER, ComponentBag.INCLUDE_ALL);
            this.classLoader = AccessController.doPrivileged(ReflectionHelper.getContextClassLoaderPA());

            this.resourceFinders = Sets.newHashSet();

            this.resources = Sets.newHashSet();
            this.resourcesView = Collections.unmodifiableSet(this.resources);
        }

        public State(final State original) {
            super(original);
            this.classLoader = original.classLoader;
            this.applicationName = original.applicationName;

            this.resources = Sets.newHashSet(original.resources);
            this.resourcesView = Collections.unmodifiableSet(this.resources);

            this.resourceFinders = Sets.newHashSet(original.resourceFinders);
        }

        public void setClassLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public void setApplicationName(final String applicationName) {
            this.applicationName = applicationName;
        }

        public void registerResources(final Set<Resource> resources) {
            this.resources.addAll(resources);
        }

        public void registerFinder(final ResourceFinder resourceFinder) {
            this.resourceFinders.add(resourceFinder);
        }

        @Override
        protected Inflector<ContractProvider.Builder, ContractProvider> getModelEnhancer(final Class<?> componentClass) {
            return new Inflector<ContractProvider.Builder, ContractProvider>() {
                @Override
                public ContractProvider apply(final ContractProvider.Builder builder) {
                    if (builder.getScope() == null && builder.getContracts().isEmpty()
                            && Resource.getPath(componentClass) != null) {
                        builder.scope(RequestScoped.class);
                    }

                    return builder.build();
                }
            };

        }

        @Override
        public State loadFrom(final Configuration config) {
            super.loadFrom(config);
            this.resourceFinders.clear();
            this.resources.clear();

            State other = null;
            if (config instanceof ResourceConfig) {
                other = ((ResourceConfig) config).state;
            }
            if (config instanceof State) {
                other = (State) config;
            }

            if (other != null) {
                this.resourceFinders.addAll(other.resourceFinders);
                this.resources.addAll(other.resources);
            }

            return this;
        }

        @Override
        public final Set<Resource> getResources() {
            return resourcesView;
        }

        @Override
        public ServerConfig getConfiguration() {
            return this;
        }

        /**
         * Get the registered resource finders.
         *
         * @return registered resource finders.
         */
        public Set<ResourceFinder> getResourceFinders() {
            return resourceFinders;
        }

        /**
         * Get resource and provider class loader.
         *
         * @return class loader to be used when looking up the resource classes and providers.
         */
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        private String getApplicationName() {
            return applicationName;
        }
    }

    private static final class ImmutableState extends State {

        private ImmutableState(final State original) {
            super(original);
        }

        @Override
        public void setClassLoader(final ClassLoader classLoader) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public void registerResources(final Set<Resource> resources) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public void registerFinder(final ResourceFinder resourceFinder) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State addProperties(final Map<String, ?> properties) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State property(final String name, final Object value) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State register(final Class<?> componentClass) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State register(final Class<?> componentClass, final int bindingPriority) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State register(final Class<?> componentClass, final Class<?>... contracts) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State register(final Object component) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State register(final Object component, final int bindingPriority) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State register(final Object component, final Class<?>... contracts) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State register(final Object component, final Map<Class<?>, Integer> contracts) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public State setProperties(final Map<String, ?> properties) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public void configureAutoDiscoverableProviders(final ServiceLocator locator, final boolean forcedOnly) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public void configureMetaProviders(final ServiceLocator locator) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }
    }

    /**
     * Returns a {@code ResourceConfig} instance for the supplied application.
     * <p/>
     * If the application is an instance of {@code ResourceConfig} the method returns defensive copy of the resource config.
     * Otherwise it creates a new {@code ResourceConfig} from the application.
     *
     * @param application Application to provide the {@code ResourceConfig} instance for.
     * @return ResourceConfig instance for the supplied application.
     */
    public static ResourceConfig forApplication(final Application application) {
        return application instanceof ResourceConfig
                ? ((ResourceConfig) application)
                : new WrappingResourceConfig(application, null, null);
    }

    /**
     * Returns a {@code ResourceConfig} instance wrapping the application of the supplied class.
     *
     * @param applicationClass Class representing a JAX-RS application.
     * @return ResourceConfig wrapping the JAX-RS application defined by the supplied class.
     */
    public static ResourceConfig forApplicationClass(final Class<? extends Application> applicationClass) {
        return new WrappingResourceConfig(null, applicationClass, null);
    }

    /**
     * Returns a {@code ResourceConfig} instance wrapping the application of the supplied class.
     * <p/>
     * This method provides an option of supplying the set of classes that should be returned from {@link #getClasses()}
     * method if the application defined by the supplied application class returns empty sets from
     * {@link javax.ws.rs.core.Application#getClasses()}
     * and {@link javax.ws.rs.core.Application#getSingletons()} methods.
     *
     * @param applicationClass Class representing a JAX-RS application.
     * @param defaultClasses   Default set of classes that should be returned from {@link #getClasses()} if the underlying
     *                         application does not provide any classes and singletons.
     * @return ResourceConfig wrapping the JAX-RS application defined by the supplied class.
     */
    public static ResourceConfig forApplicationClass(final Class<? extends Application> applicationClass,
                                                     final Set<Class<?>> defaultClasses) {
        return new WrappingResourceConfig(null, applicationClass, defaultClasses);
    }

    /**
     * Create a new resource configuration without any custom properties or
     * resource and provider classes.
     */
    public ResourceConfig() {
        this.state = new State();
    }

    /**
     * Create a new resource configuration initialized with a given set of
     * resource/provider classes.
     *
     * @param classes application-specific resource and/or provider classes.
     */
    public ResourceConfig(final Set<Class<?>> classes) {
        this();
        this.registerClasses(classes);
    }

    /**
     * Create a new resource configuration initialized with a given set of
     * resource/provider classes.
     *
     * @param classes application-specific resource and/or provider classes.
     */
    public ResourceConfig(final Class<?>... classes) {
        this(Sets.newHashSet(classes));
    }

    /**
     * Create a defensive resource configuration copy initialized with a given {@code ResourceConfig}.
     *
     * @param original resource configuration to create a defensive copy from.
     */
    public ResourceConfig(final ResourceConfig original) {
        this.state = new State(original.state);
    }

    /**
     * Add properties to {@code ResourceConfig}.
     * <p/>
     * If any of the added properties exists already, he values of the existing
     * properties will be replaced with new values.
     *
     * @param properties properties to add.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addProperties(final Map<String, Object> properties) {
        state.addProperties(properties);
        return this;
    }

    /**
     * Set new configuration properties replacing all previously set properties.
     *
     * @param properties new set of configuration properties. The content of
     *                   the map will replace any existing properties set on the configuration
     *                   instance.
     * @return the updated configuration instance.
     */
    public ResourceConfig setProperties(final Map<String, ?> properties) {
        state.setProperties(properties);
        return this;
    }

    @Override
    public ResourceConfig property(final String name, final Object value) {
        state.property(name, value);
        return this;
    }

    @Override
    public ResourceConfig register(final Class<?> componentClass) {
        invalidateCache();
        state.register(componentClass);
        return this;
    }

    @Override
    public ResourceConfig register(final Class<?> componentClass, final int bindingPriority) {
        invalidateCache();
        state.register(componentClass, bindingPriority);
        return this;
    }

    @Override
    public ResourceConfig register(final Class<?> componentClass, final Class<?>... contracts) {
        invalidateCache();
        state.register(componentClass, contracts);
        return this;
    }

    @Override
    public ResourceConfig register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
        invalidateCache();
        state.register(componentClass, contracts);
        return this;
    }

    @Override
    public ResourceConfig register(final Object component) {
        invalidateCache();
        state.register(component);
        return this;
    }

    @Override
    public ResourceConfig register(final Object component, final int bindingPriority) {
        invalidateCache();
        state.register(component, bindingPriority);
        return this;
    }

    @Override
    public ResourceConfig register(final Object component, final Class<?>... contracts) {
        invalidateCache();
        state.register(component, contracts);
        return this;
    }

    @Override
    public ResourceConfig register(final Object component, final Map<Class<?>, Integer> contracts) {
        invalidateCache();
        state.register(component, contracts);
        return this;
    }

    /**
     * Register annotated JAX-RS resource, JAX-RS or Jersey contract provider or JAX-RS feature
     * in the {@code ResourceConfig}.
     * <p>
     * Note that registered JAX-RS features are used to initialize and configure
     * the Jersey runtime {@link ServiceLocator} instance during application deployment, but are
     * otherwise ignored by server-side runtime, unless they implement also another contract
     * recognized by Jersey runtime.
     * </p>
     * <p>
     * Also note that registration of {@link Binder HK2 binder} classes is note supported. HK2 binders
     * must be {@link #registerInstances(Object...) registered as instances}.
     * </p>
     *
     * @param classes classes to register.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig registerClasses(final Set<Class<?>> classes) {
        if (classes == null) {
            return this;
        }

        for (final Class<?> cls : classes) {
            register(cls);
        }
        return this;
    }

    /**
     * Register annotated JAX-RS resource, JAX-RS or Jersey contract provider or JAX-RS feature
     * in the {@code ResourceConfig}.
     * <p>
     * Note that registered JAX-RS features are used to initialize and configure
     * the Jersey runtime {@link ServiceLocator} instance during application deployment, but are
     * otherwise ignored by server-side runtime, unless they implement also another contract
     * recognized by Jersey runtime.
     * </p>
     * <p>
     * Also note that registration of {@link Binder HK2 binder} classes is note supported. HK2 binders
     * must be {@link #registerInstances(Object...) registered as instances}.
     * </p>
     *
     * @param classes classes to register.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig registerClasses(final Class<?>... classes) {
        if (classes == null) {
            return this;
        }

        return registerClasses(Sets.newHashSet(classes));
    }

    /**
     * Register annotated JAX-RS resource, JAX-RS or Jersey contract provider, JAX-RS feature
     * or {@link Binder HK2 binder} instances (singletons) in the {@code ResourceConfig}.
     * <p>
     * Note that registered HK2 binders and JAX-RS features are used to initialize and configure
     * the Jersey runtime {@link ServiceLocator} instance during application deployment, but are
     * otherwise ignored by server-side runtime, unless they implement also another contract
     * recognized by Jersey runtime.
     * </p>
     *
     * @param instances instances to register.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig registerInstances(final Set<Object> instances) {
        if (instances == null) {
            return this;
        }

        for (final Object instance : instances) {
            register(instance);
        }
        return this;
    }

    /**
     * Register annotated JAX-RS resource, JAX-RS or Jersey contract provider, JAX-RS feature
     * or {@link Binder HK2 binder} instances (singletons) in the {@code ResourceConfig}.
     * <p>
     * Note that registered HK2 binders and JAX-RS features are used to initialize and configure
     * the Jersey runtime {@link ServiceLocator} instance during application deployment, but are
     * otherwise ignored by server-side runtime, unless they implement also another contract
     * recognized by Jersey runtime.
     * </p>
     *
     * @param instances instances to register.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig registerInstances(final Object... instances) {
        if (instances == null) {
            return this;
        }

        return registerInstances(Sets.newHashSet(instances));
    }

    /**
     * Register new programmatic resource models in the {@code ResourceConfig}.
     *
     * @param resources resource models to register.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig registerResources(final Resource... resources) {
        if (resources == null) {
            return this;
        }

        return registerResources(Sets.newHashSet(resources));
    }

    /**
     * Register new resource models in the {@code ResourceConfig}.
     *
     * @param resources resource models to register.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig registerResources(final Set<Resource> resources) {
        if (resources == null) {
            return this;
        }

        this.state.registerResources(resources);
        return this;
    }

    /**
     * Add a {@link ResourceFinder} to {@code ResourceConfig}.
     *
     * @param resourceFinder {@link ResourceFinder}
     * @return updated resource configuration instance.
     */
    public final ResourceConfig registerFinder(final ResourceFinder resourceFinder) {
        if (resourceFinder == null) {
            return this;
        }
        invalidateCache();

        this.state.registerFinder(resourceFinder);
        return this;
    }

    /**
     * Set the name of the application. The name is an arbitrary user defined name
     * which is used to distinguish between Jersey applications in the case that more applications
     * are deployed on the same runtime (container). The name can be used for example for purposes
     * of monitoring by JMX when name identifies to which application deployed MBeans belong to.
     * The name should be unique in the runtime.
     *
     * @param applicationName Unique application name.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig setApplicationName(final String applicationName) {
        state.setApplicationName(applicationName);
        return this;
    }

    /**
     * Set {@link ClassLoader} which will be used for resource discovery.
     *
     * @param classLoader provided {@link ClassLoader}.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig setClassLoader(final ClassLoader classLoader) {
        this.state.setClassLoader(classLoader);
        return this;
    }

    /**
     * Adds array of package names which will be used to scan for components.
     * <p/>
     * Package scanning ignores inheritance and therefore {@link Path} annotation
     * on parent classes and interfaces will be ignored.
     * <p/>
     * Packages will be scanned recursively, including all nested packages.
     *
     * @param packages array of package names.
     * @return updated resource configuration instance.
     * @see #packages(boolean, String...)
     */
    public final ResourceConfig packages(final String... packages) {
        return packages(true, packages);
    }

    /**
     * Adds array of package names which will be used to scan for components.
     * <p/>
     * Package scanning ignores an inheritance and therefore {@link Path} annotation
     * on parent classes and interfaces will be ignored.
     * <p/>
     * @param recursive defines whether any nested packages in the collection of specified
     *                  package names should be recursively scanned (value of {@code true})
     *                  as part of the package scanning or not (value of {@code false}).
     * @param packages  array of package names.
     * @return updated resource configuration instance.
     * @see #packages(String...)
     */
    public final ResourceConfig packages(final boolean recursive, final String... packages) {
        if (packages == null || packages.length == 0) {
            return this;
        }
        return registerFinder(new PackageNamesScanner(packages, recursive));
    }

    /**
     * Adds array of file and directory names to scan for components.
     * <p/>
     * Any directories in the list will be scanned recursively, including their sub-directories.
     *
     * @param files array of file and directory names.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig files(final String... files) {
        return files(true, files);
    }

    /**
     * Adds array of file and directory names to scan for components.
     *
     * @param recursive defines whether any sub-directories of the directories specified
     *                  in the collection of file names should be recursively scanned (value of {@code true})
     *                  as part of the file scanning or not (value of {@code false}).
     * @param files     array of file and directory names.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig files(final boolean recursive, final String... files) {
        if (files == null || files.length == 0) {
            return this;
        }
        return registerFinder(new FilesScanner(files, recursive));
    }

    /**
     * Invalidate cached component instances and classes.
     */
    final void invalidateCache() {
        this.cachedClasses = null;
        this.cachedClassesView = null;
        this.cachedSingletons = null;
        this.cachedSingletonsView = null;

        // Reset ResourceFinders to make sure the next package scanning is successful.
        if (resetFinders) {
            for (final ResourceFinder finder : this.state.resourceFinders) {
                finder.reset();
            }
            resetFinders = false;
        }
    }

    /**
     * Switches the ResourceConfig to read-only state.
     * <p/>
     * Called by the WrappingResourceConfig if this ResourceConfig is set as the application.
     * Also called by ApplicationHandler on WrappingResourceConfig at the point when it is going
     * to build the resource model.
     * <p/>
     * The method also sets the application name from properties if the name is not defined yer
     * and the property {@link ServerProperties#APPLICATION_NAME} is defined.
     */
    final void lock() {
        final State current = state;
        if (!(current instanceof ImmutableState)) {
            setupApplicationName();
            state = new ImmutableState(current);
        }
    }

    @Override
    public final ServerConfig getConfiguration() {
        return this;
    }

    @Override
    public final Map<String, Object> getProperties() {
        return state.getProperties();
    }

    @Override
    public final Object getProperty(final String name) {
        return state.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return state.getPropertyNames();
    }

    @Override
    public final boolean isProperty(final String name) {
        return state.isProperty(name);
    }

    @Override
    public final Set<Class<?>> getClasses() {
        if (cachedClassesView == null) {
            cachedClasses = _getClasses();
            cachedClassesView = Collections.unmodifiableSet(cachedClasses);
        }
        return cachedClassesView;
    }

    @Override
    public final Set<Object> getInstances() {
        return getSingletons();
    }

    @Override
    public final Set<Object> getSingletons() {
        if (cachedSingletonsView == null) {
            cachedSingletons = _getSingletons();
            cachedSingletonsView = Collections.unmodifiableSet(cachedSingletons == null ? new HashSet<>() : cachedSingletons);
        }

        return cachedSingletonsView;
    }

    /**
     * Get the internal component bag.
     *
     * @return internal component bag.
     */
    final ComponentBag getComponentBag() {
        return state.getComponentBag();
    }

    /**
     * Configure auto-discoverables.
     *
     * @param locator service locator to obtain auto-discoverables from.
     */
    final void configureAutoDiscoverableProviders(final ServiceLocator locator) {
        state.configureAutoDiscoverableProviders(locator, false);
    }

    /**
     * Configure forced auto-discoverables.
     *
     * @param locator service locator to obtain auto-discoverables from.
     */
    final void configureForcedAutoDiscoverableProviders(final ServiceLocator locator) {
        state.configureAutoDiscoverableProviders(locator, true);
    }

    /**
     * Configure custom binders registered in the resource config.
     *
     * @param locator service locator to update with the custom binders.
     */
    final void configureMetaProviders(final ServiceLocator locator) {
        state.configureMetaProviders(locator);
    }

    @Override
    public RuntimeType getRuntimeType() {
        return state.getRuntimeType();
    }

    @Override
    public boolean isEnabled(final Feature feature) {
        return state.isEnabled(feature);
    }

    @Override
    public boolean isEnabled(final Class<? extends Feature> featureClass) {
        return state.isEnabled(featureClass);
    }

    @Override
    public boolean isRegistered(final Object component) {
        return state.isRegistered(component);
    }

    @Override
    public boolean isRegistered(final Class<?> componentClass) {
        return state.isRegistered(componentClass);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(final Class<?> componentClass) {
        return state.getContracts(componentClass);
    }

    /**
     * Get configured resource and/or provider classes. The method is overridden
     * in a {@link WrappingResourceConfig private sub-type}.
     *
     * @return set of configured resource and/or provider classes.
     */
    Set<Class<?>> _getClasses() {
        final Set<Class<?>> result = scanClasses();
        result.addAll(state.getClasses());
        return result;
    }

    private Set<Class<?>> scanClasses() {
        final Set<Class<?>> result = Sets.newHashSet();

        final ResourceConfig.State _state = state;
        final Set<ResourceFinder> rfs = Sets.newHashSet(_state.getResourceFinders());

        // In case new entity is registered the available finders should be reset.
        resetFinders = true;

        // classes registered via configuration property
        final String[] classNames = parsePropertyValue(ServerProperties.PROVIDER_CLASSNAMES);
        if (classNames != null) {
            for (final String className : classNames) {
                try {
                    result.add(_state.getClassLoader().loadClass(className));
                } catch (final ClassNotFoundException e) {
                    LOGGER.log(Level.CONFIG, LocalizationMessages.UNABLE_TO_LOAD_CLASS(className));
                }
            }
        }

        final String[] packageNames = parsePropertyValue(ServerProperties.PROVIDER_PACKAGES);
        if (packageNames != null) {
            final Object p = getProperty(ServerProperties.PROVIDER_SCANNING_RECURSIVE);
            final boolean recursive = p == null || PropertiesHelper.isProperty(p);
            rfs.add(new PackageNamesScanner(packageNames, recursive));
        }

        final String[] classPathElements = parsePropertyValue(ServerProperties.PROVIDER_CLASSPATH);
        if (classPathElements != null) {
            rfs.add(new FilesScanner(classPathElements, true));
        }

        final AnnotationAcceptingListener afl =
                AnnotationAcceptingListener.newJaxrsResourceAndProviderListener(_state.getClassLoader());
        for (final ResourceFinder resourceFinder : rfs) {
            while (resourceFinder.hasNext()) {
                final String next = resourceFinder.next();
                if (afl.accept(next)) {
                    final InputStream in = resourceFinder.open();
                    try {
                        afl.process(next, in);
                    } catch (final IOException e) {
                        LOGGER.log(Level.WARNING, LocalizationMessages.RESOURCE_CONFIG_UNABLE_TO_PROCESS(next));
                    } finally {
                        try {
                            in.close();
                        } catch (final IOException ex) {
                            LOGGER.log(Level.FINER, "Error closing resource stream.", ex);
                        }
                    }
                }
            }
        }

        result.addAll(afl.getAnnotatedClasses());
        return result;
    }

    private String[] parsePropertyValue(final String propertyName) {
        String[] classNames = null;
        final Object o = state.getProperties().get(propertyName);
        if (o != null) {
            if (o instanceof String) {
                classNames = Tokenizer.tokenize((String) o);
            } else if (o instanceof String[]) {
                classNames = Tokenizer.tokenize((String[]) o);
            }
        }
        return classNames;
    }

    /**
     * Return classes which were registered by the user and not found by class path scanning (or any other scanning).
     *
     * @return Set of classes registered by the user.
     */
    Set<Class<?>> getRegisteredClasses() {
        return state.getComponentBag().getRegistrations();
    }

    /**
     * Get configured resource and/or provider instances. The method is overridden
     * in a {@link WrappingResourceConfig private sub-type}.
     *
     * @return set of configured resource and/or provider instances.
     */
    Set<Object> _getSingletons() {
        final Set<Object> result = Sets.newHashSet();
        result.addAll(state.getInstances());
        return result;
    }

    @Override
    public final Set<Resource> getResources() {
        return state.getResources();
    }

    /**
     * Get resource and provider class loader.
     *
     * @return class loader to be used when looking up the resource classes and
     * providers.
     */
    public final ClassLoader getClassLoader() {
        return state.getClassLoader();
    }

    /**
     * Returns JAX-RS application corresponding with this ResourceConfig.
     *
     * @return JAX-RS application corresponding with this ResourceConfig.
     */
    public final Application getApplication() {
        return _getApplication();
    }

    /**
     * Allows overriding the {@link #getApplication()} method functionality in {@link WrappingResourceConfig}.
     *
     * @return JAX-RS application corresponding with this ResourceConfig.
     */
    Application _getApplication() {
        return this;
    }

    /**
     * Get the name of the Jersey application.
     *
     * @return Name of the application.
     * @see #setApplicationName(String)
     */
    public String getApplicationName() {
        return state.getApplicationName();
    }

    /**
     * Method used by ApplicationHandler to retrieve application class
     * (this method is overridden by {@link WrappingResourceConfig}).
     *
     * @return application class
     */
    Class<? extends Application> getApplicationClass() {
        return null;
    }

    /**
     * This method is used by ApplicationHandler to set application instance to the resource config (should
     * always be called on WrappingResourceConfig instance, never on plain instances of ResourceConfig
     * unless we have a bug in the code).
     *
     * @param app JAX-RS application
     * @return this ResourceConfig instance (for convenience)
     */
    final ResourceConfig setApplication(final Application app) {
        return _setApplication(app);
    }

    /**
     * Allows overriding the setApplication() method functionality in WrappingResourceConfig.
     *
     * @param app application to be set for this ResourceConfig
     * @return this resource config instance
     */
    ResourceConfig _setApplication(final Application app) {
        throw new UnsupportedOperationException();
    }

    private static class WrappingResourceConfig extends ResourceConfig {

        private Application application;
        private Class<? extends Application> applicationClass;
        private final Set<Class<?>> defaultClasses = Sets.newHashSet();

        public WrappingResourceConfig(
                final Application application, final Class<? extends Application> applicationClass,
                final Set<Class<?>> defaultClasses) {

            if (application == null && applicationClass == null) {
                throw new IllegalArgumentException(LocalizationMessages.RESOURCE_CONFIG_ERROR_NULL_APPLICATIONCLASS());
            }
            this.application = application;
            this.applicationClass = applicationClass;
            if (defaultClasses != null) {
                this.defaultClasses.addAll(defaultClasses);
            }
            mergeApplications(application);
        }

        /**
         * Set the {@link javax.ws.rs.core.Application JAX-RS Application instance}
         * in the {@code ResourceConfig}.
         * <p/>
         * This method is used by the {@link org.glassfish.jersey.server.ApplicationHandler} in case this resource
         * configuration instance was created with application class rather than application instance.
         *
         * @param application JAX-RS Application instance.
         * @return updated resource configuration instance.
         */
        @Override
        ResourceConfig _setApplication(final Application application) {
            this.application = application;
            this.applicationClass = null;
            mergeApplications(application);
            return this;
        }

        /**
         * Get the original underlying JAX-RS {@link Application} instance used to
         * initialize the resource configuration instance.
         *
         * @return JAX-RS application instance.
         */
        @Override
        Application _getApplication() {
            return application;
        }

        /**
         * Get the original JAX-RS {@link Application} class provided it was not
         * instantiated yet. A {@code null} is returned in case the class has been
         * instantiated already or was not configured at all.
         * <p>
         * This class will be used to initialize the resource configuration instance.
         * If there is no JAX-RS application class set, or if the class has been
         * instantiated already, the method will return {@code null}.
         * </p>
         *
         * @return original JAX-RS application class or {@code null} if there is no
         * such class configured or if the class has been already instantiated.
         */
        @Override
        Class<? extends Application> getApplicationClass() {
            return applicationClass;
        }

        /**
         * Merges fields (e.g. custom binders, properties) of the given application with this application.
         * <p>
         * The merging should be done because of the possibility of reloading this {@code ResourceConfig} in a container
         * so this resource config should know about custom binders and properties of the underlying application to ensure
         * the reload process will complete successfully.
         * </p>
         *
         * @param application the application which fields should be merged with this application.
         * @see org.glassfish.jersey.server.spi.Container#reload()
         * @see org.glassfish.jersey.server.spi.Container#reload(ResourceConfig)
         */
        private void mergeApplications(final Application application) {
            if (application instanceof ResourceConfig) {
                // Merge custom binders.
                final ResourceConfig rc = (ResourceConfig) application;

                // Merge resources
                super.registerResources(rc.getResources());

                // properties set on the wrapping resource config take precedence
                // (as those are retrieved from the web.xml, for example)
                rc.invalidateCache();
                rc.addProperties(super.getProperties());
                super.addProperties(rc.getProperties());
                super.setApplicationName(rc.getApplicationName());
                super.setClassLoader(rc.getClassLoader());

                rc.lock();
            } else if (application != null) {
                super.addProperties(application.getProperties());
            }
        }

        @Override
        Set<Class<?>> _getClasses() {
            final Set<Class<?>> result = Sets.newHashSet();
            final Set<Class<?>> applicationClasses = application.getClasses();
            result.addAll(applicationClasses == null ? new HashSet<Class<?>>() : applicationClasses);
            if (result.isEmpty() && getSingletons().isEmpty()) {
                result.addAll(defaultClasses);
            }

            // if the application is not an instance of ResourceConfig, handle scanning triggered via properties
            if (!(application instanceof ResourceConfig)) {
                result.addAll(super._getClasses());
            }
            return result;
        }

        @Override
        Set<Object> _getSingletons() {
            return application.getSingletons();
        }
    }

    /**
     * Create runtime configuration initialized from a given deploy-time JAX-RS/Jersey
     * application configuration.
     *
     * @param application deploy-time JAX-RS/Jersey application configuration.
     * @return initialized run-time resource config.
     */
    static ResourceConfig createRuntimeConfig(final Application application) {
        return (application instanceof ResourceConfig)
                ? new RuntimeConfig((ResourceConfig) application) : new RuntimeConfig(application);
    }

    private static class RuntimeConfig extends ResourceConfig {

        private final Set<Class<?>> originalRegistrations;
        private final Application application;

        private RuntimeConfig(final ResourceConfig original) {
            super(original);

            this.application = original;

            final Application customRootApp = ResourceConfig.unwrapCustomRootApplication(original);
            if (customRootApp != null) {
                registerComponentsOf(customRootApp);
            }

            originalRegistrations = Sets.newIdentityHashSet();
            originalRegistrations.addAll(super.getRegisteredClasses());

            // Register externally provided instances.
            final Set<Object> externalInstances = Sets.filter(original.getSingletons(), new Predicate<Object>() {
                @Override
                public boolean apply(final Object external) {
                    return !originalRegistrations.contains(external.getClass());
                }
            });
            registerInstances(externalInstances);

            // Register externally provided classes.
            final Set<Class<?>> externalClasses = Sets.filter(original.getClasses(), new Predicate<Class<?>>() {
                @Override
                public boolean apply(final Class<?> external) {
                    return !originalRegistrations.contains(external);
                }
            });
            registerClasses(externalClasses);
        }

        private void registerComponentsOf(final Application application) {
            Errors.processWithException(new Runnable() {
                @Override
                public void run() {
                    // First register instances that should take precedence over classes
                    // in case of duplicate registrations
                    final Set<Object> singletons = application.getSingletons();
                    if (singletons != null) {
                        registerInstances(Sets.filter(singletons, new Predicate<Object>() {
                            @Override
                            public boolean apply(final Object input) {
                                if (input == null) {
                                    Errors.warning(application, LocalizationMessages.NON_INSTANTIABLE_COMPONENT(null));
                                }
                                return input != null;
                            }
                        }));
                    }

                    final Set<Class<?>> classes = application.getClasses();
                    if (classes != null) {
                        registerClasses(Sets.filter(classes, new Predicate<Class<?>>() {
                            @Override
                            public boolean apply(final Class<?> input) {
                                if (input == null) {
                                    Errors.warning(application, LocalizationMessages.NON_INSTANTIABLE_COMPONENT(null));
                                }
                                return input != null;
                            }
                        }));
                    }
                }
            });
        }

        private RuntimeConfig(final Application application) {
            super();

            this.application = application;

            if (application != null) {
                registerComponentsOf(application);

                // Copy all available properties.
                addProperties(application.getProperties());
            }

            originalRegistrations = super.getRegisteredClasses();
        }

        @Override
        Set<Class<?>> _getClasses() {
            // Get only a read-only classes cached in internal state.
            return super.state.getClasses();
        }

        @Override
        Set<Object> _getSingletons() {
            // Get only a read-only classes cached in internal state.
            return super.state.getInstances();
        }

        @Override
        Set<Class<?>> getRegisteredClasses() {
            return originalRegistrations;
        }

        @Override
        Application _getApplication() {
            return application;
        }
    }

    private static Application unwrapCustomRootApplication(ResourceConfig resourceConfig) {
        Application app = null;
        while (resourceConfig != null) {
            app = resourceConfig.getApplication();
            if (app == resourceConfig) {
                // resource config is the root application - return null
                return null;
            } else if (app instanceof ResourceConfig) {
                resourceConfig = (ResourceConfig) app;
            } else {
                break;
            }
        }
        return app;
    }

    /**
     * Get the most internal wrapped {@link Application application} class.
     * <p>
     * This method is similar to {@link ResourceConfig#getApplication()} except if provided application was
     * created by wrapping multiple {@code ResourceConfig} instances, this method will return the original (inner-most)
     * JAX-RS {@code Application} sub-class rather than a potentially intermediate {@code ResourceConfig} wrapper.
     * </p>
     *
     * @param application application that is potentially wrapped.
     * @return the original, inner-most {@link Application} subclass. May return the same instance directly,
     * in case the supplied {@code application} instance is not a wrapper {@code ResourceConfig} instance.
     */
    static Application unwrapApplication(Application application) {
        while (application instanceof ResourceConfig) {
            final Application wrappedApplication = ((ResourceConfig) application).getApplication();
            if (wrappedApplication == application) {
                break;
            }
            application = wrappedApplication;
        }
        return application;
    }

    private void setupApplicationName() {
        final String appName = ServerProperties.getValue(getProperties(), ServerProperties.APPLICATION_NAME, null, String.class);
        if (appName != null && getApplicationName() == null) {
            setApplicationName(appName);
        }
    }
}
