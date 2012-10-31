/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.model.internal.FeatureBag;
import org.glassfish.jersey.model.internal.ProviderBag;
import org.glassfish.jersey.model.internal.DefaultConfig;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.scanning.AnnotationAcceptingListener;
import org.glassfish.jersey.server.internal.scanning.FilesScanner;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.model.Resource;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * The resource configuration for configuring a web application.
 *
 * @author Paul Sandoz
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class ResourceConfig extends Application {

    private static final Logger LOGGER = Logger.getLogger(ResourceConfig.class.getName());
    //
    private transient Set<Class<?>> cachedClasses = null;
    private transient Set<Class<?>> cachedClassesView = null;
    private transient Set<Object> cachedSingletons = null;
    private transient Set<Object> cachedSingletonsView = null;
    //
    private final Set<Class<?>> classes;
    private final Set<Object> resourceSingletons;
    private final Set<ResourceFinder> resourceFinders;
    //
    private final Set<Resource> resources;
    private final Set<Resource> resourcesView;
    //
    private final Set<Binder> customBinders;
    //
    private ClassLoader classLoader = null;
    //
    private InternalState internalState = new Mutable();

    /**
     * Create a new resource configuration without any custom properties or
     * resource and provider classes.
     */
    public ResourceConfig() {
        this.classLoader = ReflectionHelper.getContextClassLoader();

        this.classes = Sets.newHashSet();
        this.resourceSingletons = Sets.newHashSet();
        this.resources = Sets.newHashSet();
        this.resourcesView = Collections.unmodifiableSet(this.resources);

        this.resourceFinders = Sets.newHashSet();
        this.customBinders = Sets.newHashSet();
    }

    /**
     * Create a new resource configuration initialized with a given set of
     * resource/provider classes.
     *
     * @param classes application-specific resource and/or provider classes.
     */
    public ResourceConfig(Set<Class<?>> classes) {
        this();
        this.addClasses(classes);
    }

    /**
     * Create a new resource configuration initialized with a given set of
     * resource/provider classes.
     *
     * @param classes application-specific resource and/or provider classes.
     */
    public ResourceConfig(Class<?>... classes) {
        this(Sets.newHashSet(classes));
    }

    /**
     * Create a defensive resource configuration copy initialized with a given {@code ResourceConfig}.
     *
     * @param original resource configuration to create a defensive copy from.
     */
    public ResourceConfig(final ResourceConfig original) {
        this.classLoader = original.classLoader;

        this.classes = Sets.newHashSet(original.classes);
        this.resourceSingletons = Sets.newHashSet(original.resourceSingletons);
        this.resources = Sets.newHashSet(original.resources);
        this.resourcesView = Collections.unmodifiableSet(this.resources);

        this.resourceFinders = Sets.newHashSet(original.resourceFinders);
        this.customBinders = Sets.newHashSet(original.customBinders);

        this.internalState = new Mutable((DefaultConfig) original.internalState);
    }

    /**
     * Returns a {@link ResourceConfig} instance for the supplied application.
     *
     * If the application is an instance of {@link ResourceConfig} the method returns defensive copy of the resource config.
     * Otherwise it creates a new {@link ResourceConfig} from the application.
     *
     * @param application Application to provide the {@link ResourceConfig} instance for.
     * @return ResourceConfig instance for the supplied application.
     */
    public static ResourceConfig forApplication(Application application) {
        return (application instanceof ResourceConfig) ? ((ResourceConfig) application) : new WrappingResourceConfig
                (application, null, null);
    }

    static RuntimeResourceConfig newRuntimeResourceConfig(Application application) {
        ResourceConfig runtimeResourceConfig;

        if (application instanceof ResourceConfig) {
            final ResourceConfig resourceConfig = (ResourceConfig) application;
            runtimeResourceConfig = new RuntimeResourceConfig(resourceConfig);
            application = resourceConfig.getApplication();
        } else {
            runtimeResourceConfig = new RuntimeResourceConfig();
        }

        if (application != null) {
            final Set<Class<?>> classes = application.getClasses();
            if (classes != null) {
                runtimeResourceConfig.classes.addAll(classes);
            }

            final Set<Object> singletons = application.getSingletons();
            if (singletons != null) {
                runtimeResourceConfig.resourceSingletons.addAll(singletons);
            }
        }

        return (RuntimeResourceConfig) runtimeResourceConfig;
    }

    /**
     * Returns a {@link ResourceConfig} instance wrapping the application of the supplied class.
     *
     * @param applicationClass Class representing a JAX-RS application.
     * @return ResourceConfig wrapping the JAX-RS application defined by the supplied class.
     */
    public static ResourceConfig forApplicationClass(Class<? extends Application> applicationClass) {
        return new WrappingResourceConfig(null, applicationClass, null);
    }

    /**
     * Returns a {@link ResourceConfig} instance wrapping the application of the supplied class.
     *
     * This method provides an option of supplying the set of classes that should be returned from {@link #getClasses()}
     * method if the application defined by the supplied application class returns empty sets from {@link javax.ws.rs.core
     * .Application#getClasses()}
     * and {@link javax.ws.rs.core.Application#getSingletons()} methods.
     *
     * @param applicationClass Class representing a JAX-RS application.
     * @param defaultClasses   Default set of classes that should be returned from {@link #getClasses()} if the underlying
     *                         application does not provide any classes and singletons.
     * @return ResourceConfig wrapping the JAX-RS application defined by the supplied class.
     */
    public static ResourceConfig forApplicationClass(Class<? extends Application> applicationClass,
                                                     Set<Class<?>> defaultClasses) {
        return new WrappingResourceConfig(null, applicationClass, defaultClasses);
    }

    /**
     * Add classes to {@code ResourceConfig}.
     *
     * @param classes list of classes to add.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addClasses(Set<Class<?>> classes) {
        return internalState.addClasses(classes);
    }

    /**
     * Add classes to {@code ResourceConfig}.
     *
     * @param classes {@link Set} of classes to add.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addClasses(Class<?>... classes) {
        return addClasses(Sets.newHashSet(classes));
    }

    /**
     * Add singletons to {@code ResourceConfig}.
     *
     * @param singletons {@link Set} of instances to add.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addSingletons(Set<Object> singletons) {
        return internalState.addSingletons(singletons);
    }

    /**
     * Add singletons to {@code ResourceConfig}.
     *
     * @param singletons list of instances to add.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addSingletons(Object... singletons) {
        return addSingletons(Sets.newHashSet(singletons));
    }

    /**
     * Add new resource models to the configuration.
     *
     * @param resources resource models.
     * @return updated resource configuration.
     */
    public final ResourceConfig addResources(Resource... resources) {
        return addResources(Sets.newHashSet(resources));
    }

    /**
     * Add new resource models to the configuration.
     *
     * @param resources resource models.
     * @return updated resource configuration.
     */
    public final ResourceConfig addResources(Set<Resource> resources) {
        return internalState.addResources(resources);
    }

    /**
     * Set a {@code ResourceConfig} property.
     *
     * @param name  property name.
     * @param value property value.
     * @return updated resource configuration instance.
     */
    public ResourceConfig setProperty(String name, Object value) {
        internalState.setProperty(name, value);
        return this;
    }

    public ResourceConfig setProperties(final Map<String, ?> properties) {
        internalState.setProperties(properties);
        return this;
    }

    public Collection<Feature> getFeatures() {
        return internalState.getFeatures();
    }

    public Set<Class<?>> getProviderClasses() {
        return internalState.getProviderClasses();
    }

    public Set<Object> getProviderInstances() {
        return internalState.getProviderInstances();
    }

    public ResourceConfig register(final Class<?> providerClass) {
        internalState.register(providerClass);
        return this;
    }

    public ResourceConfig register(final Object provider) {
        internalState.register(provider);
        return this;
    }

    public ResourceConfig register(final Class<?> providerClass, final int bindingPriority) {
        internalState.register(providerClass, bindingPriority);
        return this;
    }

    public <T> ResourceConfig register(final Class<T> providerClass, final Class<? super T>... contracts) {
        internalState.register(providerClass, contracts);
        return this;
    }

    public <T> ResourceConfig register(final Class<T> providerClass, final int bindingPriority, Class<? super T>... contracts) {
        internalState.register(providerClass, bindingPriority, contracts);
        return this;
    }

    public ResourceConfig register(final Object provider, final int bindingPriority) {
        internalState.register(provider, bindingPriority);
        return this;
    }

    public <T> ResourceConfig register(final Object provider, final Class<? super T>... contracts) {
        internalState.register(provider, contracts);
        return this;
    }

    public <T> ResourceConfig register(final Object provider, final int bindingPriority, final Class<? super T>... contracts) {
        internalState.register(provider, bindingPriority, contracts);
        return this;
    }

    ProviderBag getProviderBag() {
        return ((DefaultConfig)internalState).getProviderBag();
    }

    FeatureBag getFeatureBag() {
        return ((DefaultConfig)internalState).getFeatureBag();
    }

    /**
     * Add properties to {@code ResourceConfig}.
     *
     * If any of the added properties exists already, he values of the existing
     * properties will be replaced with new values.
     *
     * @param properties properties to add.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addProperties(Map<String, Object> properties) {
        return internalState.addProperties(properties);
    }

    /**
     * Add a {@link ResourceFinder} to {@code ResourceConfig}.
     *
     * @param resourceFinder {@link ResourceFinder}
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addFinder(ResourceFinder resourceFinder) {
        return internalState.addFinder(resourceFinder);
    }

    /**
     * Add {@link Binder HK2 binders} to {@code ResourceConfig}.
     *
     * These binders will be added when creating {@link ServiceLocator} instance.
     *
     * @param binders custom binders.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addBinders(Set<Binder> binders) {
        return internalState.addBinders(binders);
    }

    /**
     * Add {@link Binder HK2 binders} to {@code ResourceConfig}.
     *
     * These binders will be added when creating {@link ServiceLocator} instance.
     *
     * @param binders custom binders.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig addBinders(Binder... binders) {
        return addBinders(Sets.newHashSet(binders));
    }

    /**
     * Set {@link ClassLoader} which will be used for resource discovery.
     *
     * @param classLoader provided {@link ClassLoader}.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig setClassLoader(ClassLoader classLoader) {
        return internalState.setClassLoader(classLoader);
    }

    /**
     * Adds array of package names which will be used to scan for
     * providers.
     *
     * @param packages array of package names
     * @return updated resource configuration instance.
     */
    public final ResourceConfig packages(String... packages) {
        return addFinder(new PackageNamesScanner(packages));
    }

    /**
     * Adds array of file names to scan for providers.
     *
     * @param files array of file names.
     * @return updated resource configuration instance.
     */
    public final ResourceConfig files(String... files) {
        return addFinder(new FilesScanner(files));
    }


    private void invalidateCache() {
        this.cachedClasses = null;
        this.cachedClassesView = null;
        this.cachedSingletons = null;
        this.cachedSingletonsView = null;
    }

    /**
     * Returns binders declared during {@code ResourceConfig} creation.
     *
     * @return set of custom HK2 binders.
     */
    final Set<Binder> getCustomBinders() {
        return customBinders;
    }

    /**
     * Switches the ResourceConfig to read-only state.
     *
     * Called by the WrappingResourceConfig if this ResourceConfig is set as the application.
     * Also called by ApplicationHandler on WrappingResourceConfig at the point when it is going to build the resource model.
     */
    void lock() {
        if (!(internalState instanceof Immutable)) {
            internalState = new Immutable((DefaultConfig) internalState);
        }
    }

    /**
     * Unmodifiable {@link Set} of current resource and provider classes.
     *
     * @return Unmodifiable {@link Set} of resource and provider classes.
     */
    @Override
    public final Set<Class<?>> getClasses() {
        if (cachedClassesView == null) {
            cachedClasses = _getClasses();
            cachedClassesView = Collections.unmodifiableSet(cachedClasses);
        }
        return cachedClassesView;
    }


    /**
     * Get configured resource and/or provider classes. The method is overridden
     * in a {@link WrappingResourceConfig private sub-type}.
     *
     * @return set of configured resource and/or provider classes.
     */
    Set<Class<?>> _getClasses() {
        Set<Class<?>> result = Sets.newHashSet();

        Set<ResourceFinder> rfs = Sets.newHashSet(resourceFinders);

        // classes registered via configuration property
        String[] classNames = parsePropertyValue(ServerProperties.PROVIDER_CLASSNAMES);
        if (classNames != null) {
            for (String className : classNames) {
                try {
                    result.add(classLoader.loadClass(className));
                } catch (ClassNotFoundException e) {
                    LOGGER.log(Level.CONFIG, LocalizationMessages.UNABLE_TO_LOAD_CLASS(className));
                }
            }
        }

        String[] packageNames = parsePropertyValue(ServerProperties.PROVIDER_PACKAGES);
        if (packageNames != null) {
            rfs.add(new PackageNamesScanner(packageNames));
        }

        String[] classPathElements = parsePropertyValue(ServerProperties.PROVIDER_CLASSPATH);
        if (classPathElements != null) {
            rfs.add(new FilesScanner(classPathElements));
        }

        AnnotationAcceptingListener afl = AnnotationAcceptingListener.newJaxrsResourceAndProviderListener(classLoader);
        for (ResourceFinder resourceFinder : rfs) {
            while (resourceFinder.hasNext()) {
                final String next = resourceFinder.next();
                if (afl.accept(next)) {
                    try {
                        afl.process(next, resourceFinder.open());
                    } catch (IOException e) {
                        // TODO L10N
                        LOGGER.log(Level.WARNING, "Unable to process {0}", next);
                    }
                }
            }
        }

        result.addAll(afl.getAnnotatedClasses());
        result.addAll(internalState.getProviderClasses());
        result.addAll(classes);
        return result;
    }

    private String[] parsePropertyValue(String propertyName) {
        String[] classNames = null;
        final Object o = internalState.getProperties().get(propertyName);
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
     * @return Set of classes registered by the user.
     */
    public Set<Class<?>> getRegisteredClasses() {
        return classes;
    }

    /**
     * Unmodifiable {@link Set} of singletons.
     *
     * @return Unmodifiable {@link Set} of singletons.
     */
    @Override
    public final Set<Object> getSingletons() {
        if (cachedSingletonsView == null) {
            cachedSingletons = _getSingletons();
            cachedSingletonsView = Collections.unmodifiableSet(cachedSingletons == null ? new HashSet<Object>() : cachedSingletons);
        }

        return cachedSingletonsView;
    }

    /**
     * Get configured resource and/or provider instances. The method is overridden
     * in a {@link WrappingResourceConfig private sub-type}.
     *
     * @return set of configured resource and/or provider instances.
     */
    Set<Object> _getSingletons() {
        Set<Object> result = Sets.newHashSet();
        result.addAll(internalState.getProviderInstances());
        result.addAll(resourceSingletons);
        return result;
    }

    /**
     * Get programmatically modeled resources.
     *
     * @return programmatically modeled resources.
     */
    public final Set<Resource> getResources() {
        return resourcesView;
    }

    /**
     * Get resource and provider class loader.
     *
     * @return class loader to be used when looking up the resource classes and
     *         providers.
     */
    public final ClassLoader getClassLoader() {
        return classLoader;
    }

    public final Map<String, Object> getProperties() {
        return internalState.getProperties();
    }

    public final Object getProperty(String name) {
        return internalState.getProperty(name);
    }

    public final boolean isProperty(final String name) {
        return PropertiesHelper.isProperty(internalState.getProperties(), name);
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
     * Method used by ApplicationHandler to retrieve application class (this method is overriden by WrappingResourceConfig.
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
    final ResourceConfig setApplication(Application app) {
        internalState.setApplication(app);
        return this;
    }

    /**
     * Allows overriding the setApplication() method functionality in WrappingResourceConfig.
     *
     * @param app application to be set for this ResourceConfig
     * @return this resource config instance
     */
    ResourceConfig _setApplication(Application app) {
        throw new UnsupportedOperationException();
    }

    private interface InternalState extends Configurable {

        ResourceConfig addClasses(Set<Class<?>> classes);

        ResourceConfig addResources(Set<Resource> resources);

        ResourceConfig addFinder(ResourceFinder resourceFinder);

        ResourceConfig addBinders(Set<Binder> binders);

        ResourceConfig addProperties(Map<String, Object> properties);

        ResourceConfig addSingletons(Set<Object> singletons);

        ResourceConfig setClassLoader(ClassLoader classLoader);

        ResourceConfig setApplication(Application application);

        @Override
        InternalState setProperty(String name, Object value);

        @Override
        InternalState setProperties(Map<String,?> properties);

        @Override
        InternalState register(Class<?> providerClass);

        @Override
        InternalState register(Class<?> providerClass, int bindingPriority);

        @Override
        <T> InternalState register(Class<T> providerClass, Class<? super T>... contracts);

        @Override
        <T> InternalState register(Class<T> providerClass, int bindingPriority, Class<? super T>... contracts);

        @Override
        InternalState register(Object provider);

        @Override
        InternalState register(Object provider, int bindingPriority);

        @Override
        <T> InternalState register(Object provider, Class<? super T>... contracts);

        @Override
        <T> InternalState register(Object provider, int bindingPriority, Class<? super T>... contracts);
    }

    private class Immutable extends DefaultConfig implements InternalState {

        private Immutable(final DefaultConfig configurable) {
            super(configurable);
        }

        @Override
        public ResourceConfig addClasses(Set<Class<?>> classes) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public ResourceConfig addResources(Set<Resource> resources) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public ResourceConfig addFinder(ResourceFinder resourceFinder) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public ResourceConfig addBinders(Set<Binder> binders) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public ResourceConfig addProperties(Map<String, Object> properties) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public ResourceConfig addSingletons(Set<Object> singletons) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public ResourceConfig setClassLoader(ClassLoader classLoader) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public Immutable setProperty(String name, Object value) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public Immutable setProperties(final Map<String, ?> properties) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public Immutable register(final Class<?> providerClass) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public Immutable register(final Class<?> providerClass, final int bindingPriority) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public <T> Immutable register(final Class<T> providerClass, final Class<? super T>... contracts) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public <T> Immutable register(final Class<T> providerClass, final int bindingPriority, final Class<? super T>...
                contracts) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public Immutable register(final Object provider) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public Immutable register(final Object provider, final int bindingPriority) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public <T> Immutable register(final Object provider, final Class<? super T>... contracts) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public <T> Immutable register(final Object provider, final int bindingPriority, final Class<? super T>... contracts) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }

        @Override
        public ResourceConfig setApplication(Application application) {
            throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
        }
    }

    private class Mutable extends DefaultConfig implements InternalState {

        private Mutable() {
        }

        private Mutable(final DefaultConfig config) {
            super(config);
        }

        @Override
        public ResourceConfig addClasses(Set<Class<?>> classes) {
            invalidateCache();
            ResourceConfig.this.classes.addAll(classes);
            return ResourceConfig.this;
        }

        @Override
        public ResourceConfig addResources(Set<Resource> resources) {
            ResourceConfig.this.resources.addAll(resources);
            return ResourceConfig.this;
        }

        @Override
        public ResourceConfig addFinder(ResourceFinder resourceFinder) {
            invalidateCache();
            ResourceConfig.this.resourceFinders.add(resourceFinder);
            return ResourceConfig.this;
        }

        @Override
        public ResourceConfig addBinders(Set<Binder> binders) {
            ResourceConfig.this.customBinders.addAll(binders);
            return ResourceConfig.this;
        }

        @Override
        public ResourceConfig addProperties(Map<String, Object> properties) {
            invalidateCache();

            final Map<String,Object> props = Maps.newHashMap(getProperties());
            props.putAll(properties);

            setProperties(properties);

            return ResourceConfig.this;
        }

        @Override
        public ResourceConfig addSingletons(Set<Object> singletons) {
            invalidateCache();
            ResourceConfig.this.resourceSingletons.addAll(singletons);
            return ResourceConfig.this;
        }

        @Override
        public ResourceConfig setClassLoader(ClassLoader classLoader) {
            invalidateCache();
            ResourceConfig.this.classLoader = classLoader;
            return ResourceConfig.this;
        }

        @Override
        public Mutable setProperty(String name, Object value) {
            invalidateCache();
            super.setProperty(name, value);
            return this;
        }

        @Override
        public Mutable setProperties(final Map<String, ?> properties) {
            invalidateCache();
            super.setProperties(properties);
            return this;
        }

        @Override
        public Mutable register(final Class<?> providerClass) {
            invalidateCache();
            super.register(providerClass);
            return this;
        }

        @Override
        public Mutable register(final Class<?> providerClass, final int bindingPriority) {
            invalidateCache();
            super.register(providerClass, bindingPriority);
            return this;
        }

        @Override
        public <T> Mutable register(final Class<T> providerClass, final Class<? super T>... contracts) {
            invalidateCache();
            super.register(providerClass, contracts);
            return this;
        }

        @Override
        public <T> Mutable register(final Class<T> providerClass,
                                    final int bindingPriority,
                                    final Class<? super T>... contracts) {
            invalidateCache();
            super.register(providerClass, bindingPriority, contracts);
            return this;
        }

        @Override
        public Mutable register(final Object provider) {
            invalidateCache();
            super.register(provider);
            return this;
        }

        @Override
        public Mutable register(final Object provider, final int bindingPriority) {
            invalidateCache();
            super.register(provider, bindingPriority);
            return this;
        }

        @Override
        public <T> Mutable register(final Object provider, final Class<? super T>... contracts) {
            invalidateCache();
            super.register(provider, contracts);
            return this;
        }

        @Override
        public <T> Mutable register(final Object provider, final int bindingPriority, final Class<? super T>... contracts) {
            invalidateCache();
            super.register(provider, bindingPriority, contracts);
            return this;
        }

        @Override
        public ResourceConfig setApplication(Application application) {
            invalidateCache();
            return ResourceConfig.this._setApplication(application);
        }
    }

    static class RuntimeResourceConfig extends ResourceConfig implements Configurable {

        public RuntimeResourceConfig() {
        }

        public RuntimeResourceConfig(final ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        public RuntimeResourceConfig setProperties(final Map<String, ?> properties) {
            super.setProperties(properties);
            return this;
        }

        @Override
        public RuntimeResourceConfig setProperty(final String name, final Object value) {
            super.setProperty(name, value);
            return this;
        }

        @Override
        public RuntimeResourceConfig register(final Class<?> providerClass) {
            super.register(providerClass);
            return this;
        }

        @Override
        public RuntimeResourceConfig register(final Class<?> providerClass, final int bindingPriority) {
            super.register(providerClass, bindingPriority);
            return this;
        }

        @Override
        public <T> RuntimeResourceConfig register(final Class<T> providerClass, final Class<? super T>... contracts) {
            super.register(providerClass, contracts);
            return this;
        }

        @Override
        public <T> RuntimeResourceConfig register(final Class<T> providerClass,
                                                  final int bindingPriority, final Class<? super T>... contracts) {
            super.register(providerClass, bindingPriority, contracts);
            return this;
        }

        @Override
        public RuntimeResourceConfig register(final Object provider) {
            super.register(provider);
            return this;
        }

        @Override
        public RuntimeResourceConfig register(final Object provider, final int bindingPriority) {
            super.register(provider, bindingPriority);
            return this;
        }

        @Override
        public <T> RuntimeResourceConfig register(final Object provider, final Class<? super T>... contracts) {
            super.register(provider, contracts);
            return this;
        }

        @Override
        public <T> RuntimeResourceConfig register(final Object provider,
                                                  final int bindingPriority, final Class<? super T>... contracts) {
            super.register(provider, bindingPriority, contracts);
            return this;
        }
    }

    private static class WrappingResourceConfig extends ResourceConfig {
        private Application application;
        private Class<? extends Application> applicationClass;
        private final Set<Class<?>> defaultClasses = Sets.newHashSet();

        public WrappingResourceConfig(Application application, Class<? extends Application> applicationClass,
                                      Set<Class<?>> defaultClasses) {
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
         *
         * This method is used by the {@link org.glassfish.jersey.server.ApplicationHandler} in case this resource
         * configuration instance was created with application class rather than application instance.
         *
         * @param application JAX-RS Application instance.
         * @return updated resource configuration instance.
         */
        @Override
        ResourceConfig _setApplication(Application application) {
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
         * @return original JAX-RS application class or {@code null} if there is no
         *         such class configured or if the class has been already instantiated.
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
         * @param application the application which fields should be merged with this application.
         * @see org.glassfish.jersey.server.spi.Container#reload()
         * @see org.glassfish.jersey.server.spi.Container#reload(ResourceConfig)
         */
        private void mergeApplications(final Application application) {
            if (application instanceof ResourceConfig) {
                // Merge custom binders.
                ResourceConfig rc = (ResourceConfig) application;
                super.customBinders.addAll(rc.customBinders);

                // Merge resources
                super.resources.addAll(rc.resources);

                // properties set on the wrapping resource config take precedence (as those are retrieved from the web.xml for
                // example)
                rc.invalidateCache();
                rc.internalState.addProperties(super.getProperties());
                super.addProperties(rc.getProperties());

                rc.lock();
            }
        }

        @Override
        Set<Class<?>> _getClasses() {
            Set<Class<?>> result = Sets.newHashSet();
            Set<Class<?>> applicationClasses = application.getClasses();
            result.addAll(applicationClasses == null ? new HashSet<Class<?>>() : applicationClasses);
            if (result.isEmpty() &&  getSingletons().isEmpty()) {
                result.addAll(defaultClasses);
            }

            // if the application is not an instance of ResourceConfig, handle scanning triggered by the way of properties
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
}
