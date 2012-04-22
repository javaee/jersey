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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.scanning.AnnotationAcceptingListener;
import org.glassfish.jersey.server.internal.scanning.FilesScanner;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.model.ResourceBuilder;
import org.glassfish.jersey.server.model.ResourceClass;
import org.glassfish.jersey.server.spi.PropertiesProvider;
import static org.glassfish.jersey.server.ServerProperties.COMMON_DELIMITERS;

import org.glassfish.hk2.Module;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * The resource configuration for configuring a web application.
 *
 * @author Paul Sandoz
 */
public class ResourceConfig extends Application implements FeaturesAndProperties {

    private static final Logger LOGGER = Logger.getLogger(ResourceConfig.class.getName());

    /**
     * Create an immutable copy of the given resource configuration.
     *
     * @param config original resource configuration.
     * @return immutable copy of the original resource configuration.
     */
    static ResourceConfig unmodifiableCopy(final ResourceConfig config) {
        return new ResourceConfig(config) {

            {
                super.application = config.getApplication();
            }

            @Override
            public ResourceConfig addClasses(Set<Class<?>> classes) {
                throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
            }

            @Override
            public ResourceConfig addResources(Set<ResourceClass> resources) {
                throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
            }

            @Override
            public ResourceConfig addFinder(ResourceFinder resourceFinder) {
                throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
            }

            @Override
            public ResourceConfig addModules(Set<Module> modules) {
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
            ResourceConfig setApplication(Application application) {
                throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
            }

            @Override
            public ResourceConfig setClassLoader(ClassLoader classLoader) {
                throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
            }

            @Override
            public ResourceConfig setProperty(String name, Object value) {
                throw new IllegalStateException(LocalizationMessages.RC_NOT_MODIFIABLE());
            }
        };
    }

    public static ResourceBuilder resourceBuilder() {
        return new DefaultResourceBuilder();
    }
    //
    private transient Set<Class<?>> cachedClasses = null;
    private transient Set<Class<?>> cachedClassesView = null;
    private transient Set<Object> cachedSingletons = null;
    private transient Set<Object> cachedSingletonsView = null;
    //
    private ClassLoader classLoader = null;
    //
    private volatile Application application;
    private Class<? extends Application> applicationClass;
    //
    private final Set<Class<?>> classes;
    private final Set<Object> singletons;
    private final Set<ResourceClass> resources;
    private final Set<ResourceClass> resourcesView;
    //
    private final Map<String, Object> properties;
    private final Map<String, Object> propertiesView;
    //
    private final Set<ResourceFinder> resourceFinders;
    private final Set<org.glassfish.hk2.Module> customModules;

    public ResourceConfig() {
        this.classLoader = ReflectionHelper.getContextClassLoader();

        this.application = null;
        this.applicationClass = null;

        this.classes = Sets.newHashSet();
        this.singletons = Sets.newHashSet();
        this.resources = Sets.newHashSet();
        this.resourcesView = Collections.unmodifiableSet(resources);

        this.properties = Maps.newHashMap();
        this.propertiesView = Collections.unmodifiableMap(properties);

        this.resourceFinders = Sets.newHashSet();
        this.customModules = Sets.newHashSet();
    }

    public ResourceConfig(final Application application) {
        this();

        this.application = application;
        mergeApplications(application);
    }

    public ResourceConfig(Class<? extends Application> applicationClass) {
        this();

        this.applicationClass = applicationClass;
    }

    public ResourceConfig(Set<Class<?>> classes) {
        this();

        this.classes.addAll(classes);
    }

    public ResourceConfig(Class<?>... classes) {
        this(Sets.newHashSet(classes));
    }

    private ResourceConfig(
            final ClassLoader classLoader,
            final Application application,
            final Class<? extends Application> applicationClass,
            final Set<Class<?>> providerClasses,
            final Set<Object> providerInstances,
            final Set<ResourceClass> resources,
            final Map<String, Object> properties,
            final Set<ResourceFinder> resourceFinders,
            final Set<org.glassfish.hk2.Module> customModules) {
        this.classLoader = classLoader;

        this.application = application;
        this.applicationClass = applicationClass;

        this.classes = providerClasses;
        this.singletons = providerInstances;
        this.resources = resources;
        this.resourcesView = Collections.unmodifiableSet(resources);

        this.properties = properties;
        this.propertiesView = Collections.unmodifiableMap(properties);

        this.resourceFinders = resourceFinders;
        this.customModules = customModules;
    }

    public ResourceConfig(ResourceConfig that) {
        this.classLoader = that.classLoader;

        this.application = that.application;
        this.applicationClass = that.applicationClass;

        this.classes = Sets.newHashSet(that.classes);
        this.singletons = Sets.newHashSet(that.singletons);
        this.resources = Sets.newHashSet(that.resources);
        this.resourcesView = Collections.unmodifiableSet(resources);

        this.properties = Maps.newHashMap(that.properties);
        this.propertiesView = Collections.unmodifiableMap(this.properties);

        this.resourceFinders = Sets.newHashSet(that.resourceFinders);
        this.customModules = Sets.newHashSet(that.customModules);
    }

    /**
     * Add classes to {@code ResourceConfig}.
     *
     * @param classes list of classes to add.
     * @return updated resource configuration instance.
     */
    public ResourceConfig addClasses(Set<Class<?>> classes) {
        invalidateProviderCache();
        this.classes.addAll(classes);
        return this;
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
    public ResourceConfig addSingletons(Set<Object> singletons) {
        invalidateProviderCache();
        this.singletons.addAll(singletons);
        return this;
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

    public final ResourceConfig addResources(ResourceClass... resources) {
        return addResources(Sets.newHashSet(resources));
    }

    public ResourceConfig addResources(Set<ResourceClass> resources) {
        this.resources.addAll(resources);
        return this;
    }

    /**
     * Set a {@code ResourceConfig} property.
     *
     * @param name property name.
     * @param value property value.
     * @return updated resource configuration instance.
     */
    public ResourceConfig setProperty(String name, Object value) {
        if (ServerProperties.PROVIDER_CLASSNAMES.equals(name)) {
            invalidateProviderCache();
        }
        this.properties.put(name, value);
        return this;
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
    public ResourceConfig addProperties(Map<String, Object> properties) {
        if (properties.containsKey(ServerProperties.PROVIDER_CLASSNAMES)) {
            invalidateProviderCache();
        }
        this.properties.putAll(properties);
        return this;
    }

    /**
     * Add a {@link ResourceFinder} to {@code ResourceConfig}.
     *
     * @param resourceFinder {@link ResourceFinder}
     * @return updated resource configuration instance.
     */
    public ResourceConfig addFinder(ResourceFinder resourceFinder) {
        invalidateProviderCache();
        this.resourceFinders.add(resourceFinder);
        return this;
    }

    /**
     * Add {@link org.glassfish.hk2.Module HK2 modules} to {@code ResourceConfig}.
     *
     * These modules will be added when creating {@link org.glassfish.hk2.Services} instance.
     *
     * @param modules custom modules.
     * @return updated resource configuration instance.
     */
    public ResourceConfig addModules(Set<org.glassfish.hk2.Module> modules) {
        this.customModules.addAll(modules);
        return this;
    }

    /**
     * Add {@link org.glassfish.hk2.Module HK2 modules} to {@code ResourceConfig}.
     *
     * These modules will be added when creating {@link org.glassfish.hk2.Services} instance.
     *
     * @param modules custom modules.
     * @return updated resource configuration instance..
     */
    public final ResourceConfig addModules(org.glassfish.hk2.Module... modules) {
        return addModules(Sets.newHashSet(modules));
    }

    /**
     * Set {@link ClassLoader} which will be used for resource discovery.
     *
     * @param classLoader provided {@link ClassLoader}.
     * @return updated resource configuration instance.
     */
    public ResourceConfig setClassLoader(ClassLoader classLoader) {
        invalidateProviderCache();
        this.classLoader = classLoader;
        return this;
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

    /**
     * Set the {@link javax.ws.rs.core.Application JAX-RS Application instance}
     * in the {@code ResourceConfig}.
     *
     * This method is used by the {@link ApplicationHandler} in case this resource
     * configuration instance was created using the {@link #ResourceConfig(java.lang.Class)
     * JAX-RS Application class constructor}.
     *
     * @param application JAX-RS Application instance.
     * @return updated resource configuration instance.
     */
    /*package*/ ResourceConfig setApplication(Application application) {
        invalidateProviderCache();
        this.application = application;
        this.applicationClass = null;
        mergeApplications(application);
        return this;
    }

    /**
     * Merges fields (e.g. custom modules, properties) of the given application with this application.
     * <p/>
     * The merging should be done because of the possibility of reloading this {@code ResourceConfig} in a container
     * so this resource config should know about custom modules and properties of the underlying application to ensure
     * the reload process will complete successfully.
     *
     * @param application the application which fields should be merged with this application.
     *
     * @see org.glassfish.jersey.server.spi.Container#reload()
     * @see org.glassfish.jersey.server.spi.Container#reload(ResourceConfig)
     */
    private void mergeApplications(final Application application) {
        // Merge custom modules.
        if (application instanceof ResourceConfig) {
            customModules.addAll(((ResourceConfig)application).getCustomModules());
        }

        mergeProperties(properties, application);
    }

    private void invalidateProviderCache() {
        this.cachedClasses = null;
        this.cachedClassesView = null;
        this.cachedSingletons = null;
        this.cachedSingletonsView = null;
    }

    /**
     * Get the original underlying JAX-RS {@link Application} instance used to
     * initialize the resource configuration instance, or {@code this}, if there
     * is no underlying application instance.
     *
     * @return JAX-RS application instance.
     */
    final javax.ws.rs.core.Application getApplication() {
        return (application != null) ? application : this;
    }

    /**
     * Get the original uninstantiated JAX-RS {@link Application} class.
     *
     * This class will be used to initialize the resource configuration instance.
     * If there is no JAX-RS application class set, or if the class has been
     * instantiated already, the method will return {@code null}.
     *
     * @return original uninstantiated JAX-RS application class or {@code null}
     *     if there is no such class or if the class has been already instantiated.
     */
    final Class<? extends javax.ws.rs.core.Application> getApplicationClass() {
        return applicationClass;
    }

    /**
     * Returns modules declared during {@code ResourceConfig} creation.
     *
     * @return set of custom modules.
     */
    final Set<org.glassfish.hk2.Module> getCustomModules() {
        return customModules;
    }

    /**
     * {@link Set} of current resource and provider classes.
     *
     * Any modification to this list won't be reflected anywhere.
     * set.
     *
     * @return {@link Set} of resource and provider classes.
     */
    @Override
    public final Set<Class<?>> getClasses() {
        if (cachedClassesView == null) {
            AnnotationAcceptingListener afl = AnnotationAcceptingListener.newJaxrsResourceAndProviderListener(classLoader);
            cachedClasses = new HashSet<Class<?>>();
            cachedClassesView = Collections.unmodifiableSet(cachedClasses);

            for (ResourceFinder resourceFinder : resourceFinders) {
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

            if (application != null) {
                cachedClasses.addAll(application.getClasses());
            }

            cachedClasses.addAll(afl.getAnnotatedClasses());
            cachedClasses.addAll(classes);

            // classes registered via configuration property
            final Object o = properties.get(ServerProperties.PROVIDER_CLASSNAMES);
            if (o != null) {
                String[] classNames = null;

                if (o instanceof String) {
                    classNames = ResourceConfig.getElements((String) o, COMMON_DELIMITERS);
                } else if (o instanceof String[]) {
                    classNames = ResourceConfig.getElements((String[]) o, COMMON_DELIMITERS);
                }

                if (classNames != null) {
                    for (String className : classNames) {
                        try {
                            cachedClasses.add(classLoader.loadClass(className));
                        } catch (ClassNotFoundException e) {
                            LOGGER.log(Level.CONFIG, LocalizationMessages.UNABLE_TO_LOAD_CLASS(className));
                        }
                    }
                }
            }
        }

        return cachedClassesView;
    }

    /**
     * {@link Set} of singletons.
     *
     * Any modification to this list won't be reflected anywhere.
     * set.
     *
     * @return {@link Set} of singletons.
     */
    @Override
    public final Set<Object> getSingletons() {
        if (cachedSingletonsView == null) {
            cachedSingletons = new HashSet<Object>();
            cachedSingletonsView = Collections.unmodifiableSet(cachedSingletons);

            if (application != null) {
                cachedSingletons.addAll(application.getSingletons());
            }

            cachedSingletons.addAll(singletons);
        }

        return cachedSingletonsView;
    }

    /**
     * Get programmatically modeled resources.
     *
     * @return programmatically modeled resources.
     */
    public final Set<ResourceClass> getResources() {
        return resourcesView;
    }

    /**
     * Get resource and provider class loader.
     *
     * @return class loader to be used when looking up the resource classes and
     *     providers.
     */
    public final ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public final Map<String, Object> getProperties() {
        return propertiesView;
    }

    @Override
    public final Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public final boolean isProperty(String name) {
        if (properties.containsKey(name)) {
            Object value = properties.get(name);
            if (value instanceof Boolean) {
                return Boolean.class.cast(value);
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        }

        return false;
    }

    /**
     * Get a canonical array of String elements from a String array
     * where each entry may contain zero or more elements separated by ';'.
     *
     * @param elements an array where each String entry may contain zero or more
     *        {@link ServerProperties#COMMON_DELIMITERS} separated elements.
     * @return the array of elements, each element is trimmed, the array will
     *         not contain any empty or null entries.
     */
    public static String[] getElements(String[] elements) {
        return getElements(elements, COMMON_DELIMITERS);
    }

    /**
     * Get a canonical array of String elements from a String array
     * where each entry may contain zero or more elements separated by characters
     * in delimiters string.
     *
     * @param elements an array where each String entry may contain zero or more
     *        delimiters separated elements.
     * @param delimiters string with delimiters, every character represents one
     *        delimiter.
     * @return the array of elements, each element is trimmed, the array will
     *         not contain any empty or null entries.
     */
    public static String[] getElements(String[] elements, String delimiters) {
        List<String> es = new LinkedList<String>();
        for (String element : elements) {
            if (element == null) {
                continue;
            }
            element = element.trim();
            if (element.length() == 0) {
                continue;
            }
            for (String subElement : getElements(element, delimiters)) {
                if (subElement == null || subElement.length() == 0) {
                    continue;
                }
                es.add(subElement);
            }
        }
        return es.toArray(new String[es.size()]);
    }

    /**
     * Get a canonical array of String elements from a String
     * that may contain zero or more elements separated by characters in
     * delimiters string.
     *
     * @param elements a String that may contain zero or more
     *        delimiters separated elements.
     * @param delimiters string with delimiters, every character represents one
     *        delimiter.
     * @return the array of elements, each element is trimmed.
     */
    public static String[] getElements(String elements, String delimiters) {
        String regex = "[";
        for (char c : delimiters.toCharArray()) {
            regex += Pattern.quote(String.valueOf(c));
        }
        regex += "]";

        String[] es = elements.split(regex);
        for (int i = 0; i < es.length; i++) {
            es[i] = es[i].trim();
        }
        return es;
    }

    private static void mergeProperties(Map<String, Object> properties, final Application application) {
        if (application instanceof PropertiesProvider) {
            properties.putAll(((PropertiesProvider) application).getProperties());
        }
    }
}
