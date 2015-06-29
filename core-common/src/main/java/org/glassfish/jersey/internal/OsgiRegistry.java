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
package org.glassfish.jersey.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.SynchronousBundleListener;

/**
 * Utility class to deal with OSGi runtime specific behavior.
 * This is mainly to handle META-INF/services lookup
 * and generic/application class lookup issue in OSGi.
 *
 * When OSGi runtime is detected by the {@link ServiceFinder} class,
 * an instance of OsgiRegistry is created and associated with given
 * OSGi BundleContext. META-INF/services entries are then being accessed
 * via the OSGi Bundle API as direct ClassLoader#getResource() method invocation
 * does not work in this case within OSGi.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public final class OsgiRegistry implements SynchronousBundleListener {

    private static final String WEB_INF_CLASSES = "WEB-INF/classes/";
    private static final String CoreBundleSymbolicNAME = "org.glassfish.jersey.core.jersey-common";
    private static final Logger LOGGER = Logger.getLogger(OsgiRegistry.class.getName());

    private final BundleContext bundleContext;
    private final Map<Long, Map<String, Callable<List<Class<?>>>>> factories =
            new HashMap<Long, Map<String, Callable<List<Class<?>>>>>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static OsgiRegistry instance;

    private final Map<String, Bundle> classToBundleMapping = new HashMap<String, Bundle>();

    /**
     * Returns an {@code OsgiRegistry} instance. Call this method only if sure that the application is running in OSGi
     * environment, otherwise a call to this method can lead to an {@link ClassNotFoundException}.
     *
     * @return an {@code OsgiRegistry} instance.
     */
    public static synchronized OsgiRegistry getInstance() {
        if (instance == null) {
            final ClassLoader classLoader = AccessController
                    .doPrivileged(ReflectionHelper.getClassLoaderPA(ReflectionHelper.class));
            if (classLoader instanceof BundleReference) {
                final BundleContext context = FrameworkUtil.getBundle(OsgiRegistry.class).getBundleContext();
                if (context != null) { // context could be still null if the current bundle has not been started
                    instance = new OsgiRegistry(context);
                }
            }
        }
        return instance;
    }

    private final class OsgiServiceFinder extends ServiceFinder.ServiceIteratorProvider {

        final ServiceFinder.ServiceIteratorProvider defaultIterator = new ServiceFinder.DefaultServiceIteratorProvider();

        @Override
        public <T> Iterator<T> createIterator(
                final Class<T> serviceClass,
                final String serviceName,
                final ClassLoader loader,
                final boolean ignoreOnClassNotFound) {

            final List<Class<?>> providerClasses = locateAllProviders(serviceName);
            if (!providerClasses.isEmpty()) {
                return new Iterator<T>() {

                    Iterator<Class<?>> it = providerClasses.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public T next() {
                        final Class<T> nextClass = (Class<T>) it.next();
                        try {
                            return nextClass.newInstance();
                        } catch (final Exception ex) {
                            final ServiceConfigurationError sce = new ServiceConfigurationError(serviceName + ": "
                                    + LocalizationMessages.PROVIDER_COULD_NOT_BE_CREATED(
                                    nextClass.getName(), serviceClass, ex.getLocalizedMessage()));
                            sce.initCause(ex);
                            throw sce;
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
            return defaultIterator.createIterator(serviceClass, serviceName, loader, ignoreOnClassNotFound);
        }

        @Override
        public <T> Iterator<Class<T>> createClassIterator(
                final Class<T> service, final String serviceName, final ClassLoader loader, final boolean ignoreOnClassNotFound) {
            final List<Class<?>> providerClasses = locateAllProviders(serviceName);
            if (!providerClasses.isEmpty()) {
                return new Iterator<Class<T>>() {

                    Iterator<Class<?>> it = providerClasses.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public Class<T> next() {
                        return (Class<T>) it.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
            return defaultIterator.createClassIterator(service, serviceName, loader, ignoreOnClassNotFound);
        }
    }

    private static class BundleSpiProvidersLoader implements Callable<List<Class<?>>> {

        private final String spi;
        private final URL spiRegistryUrl;
        private final String spiRegistryUrlString;
        private final Bundle bundle;

        BundleSpiProvidersLoader(final String spi, final URL spiRegistryUrl, final Bundle bundle) {
            this.spi = spi;
            this.spiRegistryUrl = spiRegistryUrl;
            this.spiRegistryUrlString = spiRegistryUrl.toExternalForm();
            this.bundle = bundle;
        }

        @Override
        public List<Class<?>> call() throws Exception {
            BufferedReader reader = null;

            try {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Loading providers for SPI: {0}", spi);
                }
                reader = new BufferedReader(new InputStreamReader(spiRegistryUrl.openStream(), "UTF-8"));
                String providerClassName;

                final List<Class<?>> providerClasses = new ArrayList<Class<?>>();
                while ((providerClassName = reader.readLine()) != null) {
                    if (providerClassName.trim().length() == 0) {
                        continue;
                    }
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "SPI provider: {0}", providerClassName);
                    }
                    providerClasses.add(loadClass(bundle, providerClassName));
                }

                return providerClasses;
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, LocalizationMessages.EXCEPTION_CAUGHT_WHILE_LOADING_SPI_PROVIDERS(), e);
                throw e;
            } catch (final Error e) {
                LOGGER.log(Level.WARNING, LocalizationMessages.ERROR_CAUGHT_WHILE_LOADING_SPI_PROVIDERS(), e);
                throw e;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException ioe) {
                        LOGGER.log(Level.FINE, "Error closing SPI registry stream:" + spiRegistryUrl, ioe);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return spiRegistryUrlString;
        }

        @Override
        public int hashCode() {
            return spiRegistryUrlString.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof BundleSpiProvidersLoader) {
                return spiRegistryUrlString.equals(((BundleSpiProvidersLoader) obj).spiRegistryUrlString);
            } else {
                return false;
            }
        }
    }

    @Override
    public void bundleChanged(final BundleEvent event) {

        if (event.getType() == BundleEvent.RESOLVED) {
            register(event.getBundle());
        } else if (event.getType() == BundleEvent.UNRESOLVED || event.getType() == BundleEvent.UNINSTALLED) {

            final Bundle unregisteredBundle = event.getBundle();

            lock.writeLock().lock();
            try {
                factories.remove(unregisteredBundle.getBundleId());

                if (unregisteredBundle.getSymbolicName().equals(CoreBundleSymbolicNAME)) {
                    bundleContext.removeBundleListener(this);
                    factories.clear();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Translates bundle entry path as returned from {@link org.osgi.framework.Bundle#findEntries(String, String, boolean)} to
     * fully qualified class name that resides in given package path (directly or indirectly in its subpackages).
     *
     * @param packagePath     The package path where the class is located (even recursively)
     * @param bundleEntryPath The bundle path to translate.
     * @return Fully qualified class name.
     */
    public static String bundleEntryPathToClassName(String packagePath, String bundleEntryPath) {
        // normalize packagePath
        packagePath = normalizedPackagePath(packagePath);

        // remove WEB-INF/classes from bundle entry path
        if (bundleEntryPath.contains(WEB_INF_CLASSES)) {
            bundleEntryPath = bundleEntryPath.substring(bundleEntryPath.indexOf(WEB_INF_CLASSES) + WEB_INF_CLASSES.length());
        }

        final int packageIndex = bundleEntryPath.indexOf(packagePath);

        String normalizedClassNamePath = packageIndex > -1
                // the package path was found in the bundle path
                ? bundleEntryPath.substring(packageIndex)
                // the package path is not included in the bundle entry path
                // fall back to the original implementation of the translation which does not consider recursion
                : packagePath + bundleEntryPath.substring(bundleEntryPath.lastIndexOf('/') + 1);

        return (normalizedClassNamePath.startsWith("/") ? normalizedClassNamePath.substring(1) : normalizedClassNamePath)
                .replace('/', '.').replace(".class", "");
    }

    /**
     * Returns whether the given entry path is located directly in the provided package path. That is,
     * if the entry is located in a sub-package, then {@code false} is returned.
     *
     * @param packagePath Package path which the entry is compared to
     * @param entryPath Entry path
     * @return Whether the given entry path is located directly in the provided package path.
     */
    public static boolean isPackageLevelEntry(String packagePath, final String entryPath) {
        // normalize packagePath
        packagePath = normalizedPackagePath(packagePath);

        // if the package path is contained in the jar entry name, subtract it
        String entryWithoutPackagePath = entryPath.contains(packagePath)
                ? entryPath.substring(entryPath.indexOf(packagePath) + packagePath.length())
                : entryPath;

        return !(entryWithoutPackagePath.startsWith("/") ? entryWithoutPackagePath.substring(1)
                         : entryWithoutPackagePath)
                .contains("/");
    }

    /**
     * Normalized package returns path that does not start with '/' character and ends with '/' character.
     * If the argument is '/' then returned value is empty string "".
     *
     * @param packagePath package path to normalize.
     * @return Normalized package path.
     */
    public static String normalizedPackagePath(String packagePath) {
        packagePath = packagePath.startsWith("/") ? packagePath.substring(1) : packagePath;
        packagePath = packagePath.endsWith("/") ? packagePath : packagePath + "/";
        packagePath = "/".equals(packagePath) ? "" : packagePath;
        return packagePath;
    }

    /**
     * Get URLs of resources from a given package.
     *
     * @param packagePath package.
     * @param classLoader resource class loader.
     * @param recursive   whether the given package path should be scanned recursively by OSGi
     * @return URLs of the located resources.
     */
    @SuppressWarnings("unchecked")
    public Enumeration<URL> getPackageResources(final String packagePath,
                                                final ClassLoader classLoader,
                                                final boolean recursive) {
        final List<URL> result = new LinkedList<URL>();

        for (final Bundle bundle : bundleContext.getBundles()) {
            // Look for resources at the given <packagePath> and at WEB-INF/classes/<packagePath> in case a WAR is being examined.
            for (final String bundlePackagePath : new String[] {packagePath, WEB_INF_CLASSES + packagePath}) {
                final Enumeration<URL> enumeration = findEntries(bundle, bundlePackagePath, "*.class", recursive);

                if (enumeration != null) {
                    while (enumeration.hasMoreElements()) {
                        final URL url = enumeration.nextElement();
                        final String path = url.getPath();

                        classToBundleMapping.put(bundleEntryPathToClassName(packagePath, path), bundle);
                        result.add(url);
                    }
                }
            }

            // Now interested only in .jar provided by current bundle.
            final Enumeration<URL> jars = findEntries(bundle, "/", "*.jar", true);
            if (jars != null) {
                while (jars.hasMoreElements()) {
                    final URL jar = jars.nextElement();
                    final InputStream inputStream = classLoader.getResourceAsStream(jar.getPath());
                    if (inputStream == null) {
                        LOGGER.config(LocalizationMessages.OSGI_REGISTRY_ERROR_OPENING_RESOURCE_STREAM(jar));
                        continue;
                    }
                    final JarInputStream jarInputStream;
                    try {
                        jarInputStream = new JarInputStream(inputStream);
                    } catch (final IOException ex) {
                        LOGGER.log(Level.CONFIG, LocalizationMessages.OSGI_REGISTRY_ERROR_PROCESSING_RESOURCE_STREAM(jar), ex);
                        try {
                            inputStream.close();
                        } catch (final IOException e) {
                            // ignored
                        }
                        continue;
                    }

                    try {
                        JarEntry jarEntry;
                        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                            final String jarEntryName = jarEntry.getName();
                            final String jarEntryNameLeadingSlash = jarEntryName.startsWith("/")
                                    ? jarEntryName : "/" + jarEntryName;

                            if (jarEntryName.endsWith(".class")
                                    // Added leading and trailing slashes '/' to package path (e.g. '/com/') helps us to not
                                    // accidentally match sub-strings of the package path (e.g., if package path 'com' was used
                                    // for scanning, package 'whatever.foo.telecom' would be matched because of word 'tele[com]').
                                    // Note that we cannot avoid all corner cases with accidental matches since jar
                                    // entry name might be almost anything (e.g., if package path 'telecom' was used, package
                                    // 'whatever.foo.telecom' will be matched and there is no way to avoid it unless user
                                    // explicitly instructs us to do so somehow (not implemented)
                                    && jarEntryNameLeadingSlash.contains("/" + normalizedPackagePath(packagePath))) {
                                if (!recursive && !isPackageLevelEntry(packagePath, jarEntryName)) {
                                    continue;
                                }
                                classToBundleMapping.put(jarEntryName.replace(".class", "").replace('/', '.'), bundle);
                                result.add(bundle.getResource(jarEntryName));
                            }
                        }
                    } catch (final Exception ex) {
                        LOGGER.log(Level.CONFIG, LocalizationMessages.OSGI_REGISTRY_ERROR_PROCESSING_RESOURCE_STREAM(jar), ex);
                    } finally {
                        try {
                            jarInputStream.close();
                        } catch (final IOException e) {
                            // ignored
                        }
                    }
                }
            }
        }

        return Collections.enumeration(result);
    }

    /**
     * Get the Class from the class name.
     * <p>
     * The context class loader will be utilized if accessible and non-null.
     * Otherwise the defining class loader of this class will
     * be utilized.
     *
     * @param className the class name.
     * @return the Class, otherwise null if the class cannot be found.
     * @throws ClassNotFoundException if the class cannot be found.
     */
    public Class<?> classForNameWithException(final String className) throws ClassNotFoundException {
        final Bundle bundle = classToBundleMapping.get(className);

        if (bundle == null) {
            throw new ClassNotFoundException(className);
        }
        return loadClass(bundle, className);
    }

    /**
     * Tries to load resource bundle via OSGi means. No caching involved here,
     * as localization properties are being cached in Localizer class already.
     *
     * @param bundleName name of the resource bundle to load
     * @return resource bundle instance if found, null otherwise
     */
    public ResourceBundle getResourceBundle(final String bundleName) {
        final int lastDotIndex = bundleName.lastIndexOf('.');
        final String path = bundleName.substring(0, lastDotIndex).replace('.', '/');
        final String propertiesName = bundleName.substring(lastDotIndex + 1, bundleName.length()) + ".properties";
        for (final Bundle bundle : bundleContext.getBundles()) {
            final Enumeration<URL> entries = findEntries(bundle, path, propertiesName, false);
            if (entries != null && entries.hasMoreElements()) {
                final URL entryUrl = entries.nextElement();
                try {
                    return new PropertyResourceBundle(entryUrl.openStream());
                } catch (final IOException ex) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        // does not make sense to localize this
                        LOGGER.fine("Exception caught when tried to load resource bundle in OSGi");
                    }
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Creates a new OsgiRegistry instance bound to a particular OSGi runtime.
     * The only parameter must be an instance of a {@link BundleContext}.
     *
     * @param bundleContext must be a non-null instance of a BundleContext
     */
    private OsgiRegistry(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Will hook up this instance with the OSGi runtime.
     * This is to actually update SPI provider lookup and class loading mechanisms in Jersey
     * to utilize OSGi features.
     */
    void hookUp() {
        setOSGiServiceFinderIteratorProvider();
        bundleContext.addBundleListener(this);
        registerExistingBundles();
    }

    private void registerExistingBundles() {
        for (final Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.STARTING
                    || bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STOPPING) {
                register(bundle);
            }
        }
    }

    private void setOSGiServiceFinderIteratorProvider() {
        ServiceFinder.setIteratorProvider(new OsgiServiceFinder());
    }

    private void register(final Bundle bundle) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "checking bundle {0}", bundle.getBundleId());
        }

        Map<String, Callable<List<Class<?>>>> map;
        lock.writeLock().lock();
        try {
            map = factories.get(bundle.getBundleId());
            if (map == null) {
                map = new ConcurrentHashMap<String, Callable<List<Class<?>>>>();
                factories.put(bundle.getBundleId(), map);
            }
        } finally {
            lock.writeLock().unlock();
        }

        final Enumeration<URL> e = findEntries(bundle, "META-INF/services/", "*", false);
        if (e != null) {
            while (e.hasMoreElements()) {
                final URL u = e.nextElement();
                final String url = u.toString();
                if (url.endsWith("/")) {
                    continue;
                }
                final String factoryId = url.substring(url.lastIndexOf("/") + 1);
                map.put(factoryId, new BundleSpiProvidersLoader(factoryId, u, bundle));
            }
        }
    }

    private List<Class<?>> locateAllProviders(final String serviceName) {
        lock.readLock().lock();
        try {
            final List<Class<?>> result = new LinkedList<Class<?>>();
            for (final Map<String, Callable<List<Class<?>>>> value : factories.values()) {
                if (value.containsKey(serviceName)) {
                    try {
                        result.addAll(value.get(serviceName).call());
                    } catch (final Exception ex) {
                        // ignore
                    }
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    private static Class<?> loadClass(final Bundle bundle, final String className) throws ClassNotFoundException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                @Override
                public Class<?> run() throws ClassNotFoundException {
                    return bundle.loadClass(className);
                }
            });
        } catch (final PrivilegedActionException ex) {
            final Exception originalException = ex.getException();
            if (originalException instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) originalException;
            } else if (originalException instanceof RuntimeException) {
                throw (RuntimeException) originalException;
            } else {
                throw new ProcessingException(originalException);
            }
        }
    }

    private static Enumeration<URL> findEntries(final Bundle bundle,
                                                final String path,
                                                final String fileNamePattern,
                                                final boolean recursive) {
        return AccessController.doPrivileged(new PrivilegedAction<Enumeration<URL>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Enumeration<URL> run() {
                return bundle.findEntries(path, fileNamePattern, recursive);
            }
        });
    }
}
