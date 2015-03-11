/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Array;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * A simple service-provider lookup mechanism.  A <i>service</i> is a
 * well-known set of interfaces and (usually abstract) classes.  A <i>service
 * provider</i> is a specific implementation of a service.  The classes in a
 * provider typically implement the interfaces and subclass the classes defined
 * in the service itself.  Service providers may be installed in an
 * implementation of the Java platform in the form of extensions, that is, jar
 * files placed into any of the usual extension directories.  Providers may
 * also be made available by adding them to the applet or application class
 * path or by some other platform-specific means.
 * <p/>
 * <p> In this lookup mechanism a service is represented by an interface or an
 * abstract class.  (A concrete class may be used, but this is not
 * recommended.)  A provider of a given service contains one or more concrete
 * classes that extend this <i>service class</i> with data and code specific to
 * the provider.  This <i>provider class</i> will typically not be the entire
 * provider itself but rather a proxy that contains enough information to
 * decide whether the provider is able to satisfy a particular request together
 * with code that can create the actual provider on demand.  The details of
 * provider classes tend to be highly service-specific; no single class or
 * interface could possibly unify them, so no such class has been defined.  The
 * only requirement enforced here is that provider classes must have a
 * zero-argument constructor so that they may be instantiated during lookup.
 * <p/>
 * <p>The default service provider registration/lookup mechanism based
 * on <tt>META-INF/services</tt> files is described below.
 * For environments, where the basic mechanism is not suitable, clients
 * can enforce a different approach by setting their custom <tt>ServiceIteratorProvider</tt>
 * by calling <tt>setIteratorProvider</tt>. The call must be made prior to any lookup attempts.
 * </p>
 * <p> A service provider identifies itself by placing a provider-configuration
 * file in the resource directory <tt>META-INF/services</tt>.  The file's name
 * should consist of the fully-qualified name of the abstract service class.
 * The file should contain a list of fully-qualified concrete provider-class
 * names, one per line.  Space and tab characters surrounding each name, as
 * well as blank lines, are ignored.  The comment character is <tt>'#'</tt>
 * (<tt>0x23</tt>); on each line all characters following the first comment
 * character are ignored.  The file must be encoded in UTF-8.
 * <p/>
 * <p> If a particular concrete provider class is named in more than one
 * configuration file, or is named in the same configuration file more than
 * once, then the duplicates will be ignored.  The configuration file naming a
 * particular provider need not be in the same jar file or other distribution
 * unit as the provider itself.  The provider must be accessible from the same
 * class loader that was initially queried to locate the configuration file;
 * note that this is not necessarily the class loader that found the file.
 * <p/>
 * <p> <b>Example:</b> Suppose we have a service class named
 * <tt>java.io.spi.CharCodec</tt>.  It has two abstract methods:
 * <p/>
 * <pre>
 *   public abstract CharEncoder getEncoder(String encodingName);
 *   public abstract CharDecoder getDecoder(String encodingName);
 * </pre>
 * <p/>
 * Each method returns an appropriate object or <tt>null</tt> if it cannot
 * translate the given encoding.  Typical <tt>CharCodec</tt> providers will
 * support more than one encoding.
 * <p/>
 * <p> If <tt>sun.io.StandardCodec</tt> is a provider of the <tt>CharCodec</tt>
 * service then its jar file would contain the file
 * <tt>META-INF/services/java.io.spi.CharCodec</tt>.  This file would contain
 * the single line:
 * <p/>
 * <pre>
 *   sun.io.StandardCodec    # Standard codecs for the platform
 * </pre>
 * <p/>
 * To locate an codec for a given encoding name, the internal I/O code would
 * do something like this:
 * <p/>
 * <pre>
 *   CharEncoder getEncoder(String encodingName) {
 *       for( CharCodec cc : ServiceFinder.find(CharCodec.class) ) {
 *           CharEncoder ce = cc.getEncoder(encodingName);
 *           if (ce != null)
 *               return ce;
 *       }
 *       return null;
 *   }
 * </pre>
 * <p/>
 * The provider-lookup mechanism always executes in the security context of the
 * caller.  Trusted system code should typically invoke the methods in this
 * class from within a privileged security context.
 *
 * @param <T> the type of the service instance.
 * @author Mark Reinhold
 * @author Jakub Podlesak
 * @author Marek Potociar
 */
public final class ServiceFinder<T> implements Iterable<T> {

    private static final Logger LOGGER = Logger.getLogger(ServiceFinder.class.getName());
    private static final String PREFIX = "META-INF/services/";
    private final Class<T> serviceClass;
    private final String serviceName;
    private final ClassLoader classLoader;
    private final boolean ignoreOnClassNotFound;

    static {
        final OsgiRegistry osgiRegistry = ReflectionHelper.getOsgiRegistryInstance();

        if (osgiRegistry != null) {
            LOGGER.log(Level.CONFIG, "Running in an OSGi environment");

            osgiRegistry.hookUp();
        } else {
            LOGGER.log(Level.CONFIG, "Running in a non-OSGi environment");
        }
    }

    private static Enumeration<URL> getResources(final ClassLoader loader, final String name) throws IOException {
        if (loader == null) {
            return getResources(name);
        } else {
            final Enumeration<URL> resources = loader.getResources(name);
            if ((resources != null) && resources.hasMoreElements()) {
                return resources;
            } else {
                return getResources(name);
            }
        }
    }

    private static Enumeration<URL> getResources(final String name) throws IOException {
        if (ServiceFinder.class.getClassLoader() != null) {
            return ServiceFinder.class.getClassLoader().getResources(name);
        } else {
            return ClassLoader.getSystemResources(name);
        }
    }

    private static ClassLoader _getContextClassLoader() {
        return AccessController.doPrivileged(ReflectionHelper.getContextClassLoaderPA());
    }

    /**
     * Locates and incrementally instantiates the available providers of a
     * given service using the given class loader.
     * <p/>
     * <p> This method transforms the name of the given service class into a
     * provider-configuration filename as described above and then uses the
     * <tt>getResources</tt> method of the given class loader to find all
     * available files with that name.  These files are then read and parsed to
     * produce a list of provider-class names.  The iterator that is returned
     * uses the given class loader to lookup and then instantiate each element
     * of the list.
     * <p/>
     * <p> Because it is possible for extensions to be installed into a running
     * Java virtual machine, this method may return different results each time
     * it is invoked. <p>
     * @param service The service's abstract service class
     * @param loader The class loader to be used to load provider-configuration files
     *                and instantiate provider classes, or <tt>null</tt> if the system
     *                class loader (or, failing that the bootstrap class loader) is to
     *                be used
     * @throws ServiceConfigurationError If a provider-configuration file violates the specified format
     *                                   or names a provider class that cannot be found and instantiated
     * @see #find(Class)
     * @param <T> the type of the service instance.
     * @return the service finder
     */
    public static <T> ServiceFinder<T> find(final Class<T> service, final ClassLoader loader)
            throws ServiceConfigurationError {
        return find(service,
                loader,
                false);
    }

    /**
     * Locates and incrementally instantiates the available providers of a
     * given service using the given class loader.
     * <p/>
     * <p> This method transforms the name of the given service class into a
     * provider-configuration filename as described above and then uses the
     * <tt>getResources</tt> method of the given class loader to find all
     * available files with that name.  These files are then read and parsed to
     * produce a list of provider-class names.  The iterator that is returned
     * uses the given class loader to lookup and then instantiate each element
     * of the list.
     * <p/>
     * <p> Because it is possible for extensions to be installed into a running
     * Java virtual machine, this method may return different results each time
     * it is invoked. <p>
     * @param service The service's abstract service class
     * @param loader The class loader to be used to load provider-configuration files
     *                and instantiate provider classes, or <tt>null</tt> if the system
     *                class loader (or, failing that the bootstrap class loader) is to
     *                be used
     * @param ignoreOnClassNotFound If a provider cannot be loaded by the class loader
     *                              then move on to the next available provider.
     * @throws ServiceConfigurationError If a provider-configuration file violates the specified format
     *                                   or names a provider class that cannot be found and instantiated
     * @see #find(Class)
     * @param <T> the type of the service instance.
     * @return the service finder
     */
    public static <T> ServiceFinder<T> find(final Class<T> service,
                                            final ClassLoader loader,
                                            final boolean ignoreOnClassNotFound) throws ServiceConfigurationError {
        return new ServiceFinder<T>(service,
                loader,
                ignoreOnClassNotFound);
    }

    /**
     * Locates and incrementally instantiates the available providers of a
     * given service using the context class loader.  This convenience method
     * is equivalent to
     * <p/>
     * <pre>
     *   ClassLoader cl = Thread.currentThread().getContextClassLoader();
     *   return Service.providers(service, cl, false);
     * </pre>
     * @param service The service's abstract service class
     * @throws ServiceConfigurationError If a provider-configuration file violates the specified format
     *                                   or names a provider class that cannot be found and instantiated
     * @see #find(Class, ClassLoader)
     * @param <T> the type of the service instance.
     * @return the service finder
     */
    public static <T> ServiceFinder<T> find(final Class<T> service)
            throws ServiceConfigurationError {
        return find(service,
                _getContextClassLoader(),
                false);
    }

    /**
     * Locates and incrementally instantiates the available providers of a
     * given service using the context class loader.  This convenience method
     * is equivalent to
     * <p/>
     * <pre>
     *   ClassLoader cl = Thread.currentThread().getContextClassLoader();
     *   boolean ingore = ...
     *   return Service.providers(service, cl, ignore);
     * </pre>
     * @param service The service's abstract service class
     * @param ignoreOnClassNotFound If a provider cannot be loaded by the class loader
     *                              then move on to the next available provider.
     * @throws ServiceConfigurationError If a provider-configuration file violates the specified format
     *                                   or names a provider class that cannot be found and instantiated
     * @see #find(Class, ClassLoader)
     * @param <T> the type of the service instance.
     * @return the service finder
     */
    public static <T> ServiceFinder<T> find(final Class<T> service,
                                            final boolean ignoreOnClassNotFound) throws ServiceConfigurationError {
        return find(service,
                _getContextClassLoader(),
                ignoreOnClassNotFound);
    }

    /**
     * Locates and incrementally instantiates the available classes of a given
     * service file using the context class loader.
     *
     * @param serviceName the service name correspond to a file in
     *        META-INF/services that contains a list of fully qualified class
     *        names
     * @throws ServiceConfigurationError If a service file violates the specified format
     *                                   or names a provider class that cannot be found and instantiated
     * @return the service finder
     */
    public static ServiceFinder<?> find(final String serviceName) throws ServiceConfigurationError {
        return new ServiceFinder<Object>(Object.class, serviceName, _getContextClassLoader(), false);
    }

    /**
     * Register the service iterator provider to iterate on provider instances
     * or classes.
     * <p>
     * The default implementation registered, {@link DefaultServiceIteratorProvider},
     * looks up provider classes in META-INF/service files.
     * <p>
     * This method must be called prior to any attempts to obtain provider
     * instances or classes.
     *
     * @param sip the service iterator provider.
     * @throws SecurityException if the provider cannot be registered.
     */
    public static void setIteratorProvider(final ServiceIteratorProvider sip) throws SecurityException {
        ServiceIteratorProvider.setInstance(sip);
    }

    private ServiceFinder(
            final Class<T> service,
            final ClassLoader loader,
            final boolean ignoreOnClassNotFound) {
        this(service, service.getName(), loader, ignoreOnClassNotFound);
    }

    private ServiceFinder(
            final Class<T> service,
            final String serviceName,
            final ClassLoader loader,
            final boolean ignoreOnClassNotFound) {
        this.serviceClass = service;
        this.serviceName = serviceName;
        this.classLoader = loader;
        this.ignoreOnClassNotFound = ignoreOnClassNotFound;
    }

    /**
     * Returns discovered objects incrementally.
     *
     * @return An <tt>Iterator</tt> that yields provider objects for the given
     *         service, in some arbitrary order.  The iterator will throw a
     *         <tt>ServiceConfigurationError</tt> if a provider-configuration
     *         file violates the specified format or if a provider class cannot
     *         be found and instantiated.
     */
    @Override
    public Iterator<T> iterator() {
        return ServiceIteratorProvider.getInstance()
                .createIterator(serviceClass, serviceName, classLoader, ignoreOnClassNotFound);
    }

    /**
     * Returns discovered objects all at once.
     *
     * @return
     *      can be empty but never null.
     *
     * @throws ServiceConfigurationError If a provider-configuration file violates the specified format
     *                                   or names a provider class that cannot be found and instantiated
     */
    @SuppressWarnings("unchecked")
    public T[] toArray() throws ServiceConfigurationError {
        final List<T> result = new ArrayList<T>();
        for (final T t : this) {
            result.add(t);
        }
        return result.toArray((T[]) Array.newInstance(serviceClass, result.size()));
    }

    /**
     * Returns discovered classes all at once.
     *
     * @return
     *      can be empty but never null.
     *
     * @throws ServiceConfigurationError If a provider-configuration file violates the specified format
     *                                   or names a provider class that cannot be found
     */
    @SuppressWarnings("unchecked")
    public Class<T>[] toClassArray() throws ServiceConfigurationError {
        final List<Class<T>> result = new ArrayList<Class<T>>();

        final ServiceIteratorProvider iteratorProvider = ServiceIteratorProvider.getInstance();
        final Iterator<Class<T>> i = iteratorProvider
                .createClassIterator(serviceClass, serviceName, classLoader, ignoreOnClassNotFound);
        while (i.hasNext()) {
            result.add(i.next());
        }
        return result.toArray((Class<T>[]) Array.newInstance(Class.class, result.size()));
    }

    private static void fail(final String serviceName, final String msg, final Throwable cause)
            throws ServiceConfigurationError {
        final ServiceConfigurationError sce = new ServiceConfigurationError(serviceName + ": " + msg);
        sce.initCause(cause);
        throw sce;
    }

    private static void fail(final String serviceName, final String msg)
            throws ServiceConfigurationError {
        throw new ServiceConfigurationError(serviceName + ": " + msg);
    }

    private static void fail(final String serviceName, final URL u, final int line, final String msg)
            throws ServiceConfigurationError {
        fail(serviceName, u + ":" + line + ": " + msg);
    }

    /**
     * Parse a single line from the given configuration file, adding the name
     * on the line to both the names list and the returned set iff the name is
     * not already a member of the returned set.
     */
    private static int parseLine(final String serviceName, final URL u, final BufferedReader r, final int lc,
                                 final List<String> names, final Set<String> returned)
            throws IOException, ServiceConfigurationError {
        String ln = r.readLine();
        if (ln == null) {
            return -1;
        }
        final int ci = ln.indexOf('#');
        if (ci >= 0) {
            ln = ln.substring(0, ci);
        }
        ln = ln.trim();
        final int n = ln.length();
        if (n != 0) {
            if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0)) {
                fail(serviceName, u, lc, LocalizationMessages.ILLEGAL_CONFIG_SYNTAX());
            }
            int cp = ln.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp)) {
                fail(serviceName, u, lc, LocalizationMessages.ILLEGAL_PROVIDER_CLASS_NAME(ln));
            }
            for (int i = Character.charCount(cp); i < n; i += Character.charCount(cp)) {
                cp = ln.codePointAt(i);
                if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                    fail(serviceName, u, lc, LocalizationMessages.ILLEGAL_PROVIDER_CLASS_NAME(ln));
                }
            }
            if (!returned.contains(ln)) {
                names.add(ln);
                returned.add(ln);
            }
        }
        return lc + 1;
    }

    /**
     * Parse the content of the given URL as a provider-configuration file.
     *
     * @param serviceName  The service class for which providers are being sought;
     *                     used to construct error detail strings
     * @param u        The URL naming the configuration file to be parsed
     * @param returned A Set containing the names of provider classes that have already
     *                 been returned.  This set will be updated to contain the names
     *                 that will be yielded from the returned <tt>Iterator</tt>.
     * @return A (possibly empty) <tt>Iterator</tt> that will yield the
     *         provider-class names in the given configuration file that are
     *         not yet members of the returned set
     * @throws ServiceConfigurationError If an I/O error occurs while reading from the given URL, or
     *                                   if a configuration-file format error is detected
     */
    @SuppressWarnings({"StatementWithEmptyBody"})
    private static Iterator<String> parse(final String serviceName, final URL u, final Set<String> returned)
            throws ServiceConfigurationError {
        InputStream in = null;
        BufferedReader r = null;
        final ArrayList<String> names = new ArrayList<String>();
        try {
            final URLConnection uConn = u.openConnection();
            uConn.setUseCaches(false);
            in = uConn.getInputStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while ((lc = parseLine(serviceName, u, r, lc, names, returned)) >= 0) {
                // continue
            }
        } catch (final IOException x) {
            fail(serviceName, ": " + x);
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException y) {
                fail(serviceName, ": " + y);
            }
        }
        return names.iterator();
    }

    private static class AbstractLazyIterator<T> {

        final Class<T> service;
        final String serviceName;
        final ClassLoader loader;
        final boolean ignoreOnClassNotFound;
        Enumeration<URL> configs = null;
        Iterator<String> pending = null;
        Set<String> returned = new TreeSet<String>();
        String nextName = null;

        private AbstractLazyIterator(
                final Class<T> service,
                final String serviceName,
                final ClassLoader loader,
                final boolean ignoreOnClassNotFound) {
            this.service = service;
            this.serviceName = serviceName;
            this.loader = loader;
            this.ignoreOnClassNotFound = ignoreOnClassNotFound;
        }

        protected final void setConfigs() {
            if (configs == null) {
                try {
                    final String fullName = PREFIX + serviceName;
                    configs = getResources(loader, fullName);
                } catch (final IOException x) {
                    fail(serviceName, ": " + x);
                }
            }
        }

        public boolean hasNext() throws ServiceConfigurationError {
            if (nextName != null) {
                return true;
            }
            setConfigs();

            while (nextName == null) {
                while ((pending == null) || !pending.hasNext()) {
                    if (!configs.hasMoreElements()) {
                        return false;
                    }
                    pending = parse(serviceName, configs.nextElement(), returned);
                }
                nextName = pending.next();
                if (ignoreOnClassNotFound) {
                    try {
                        AccessController.doPrivileged(ReflectionHelper.classForNameWithExceptionPEA(nextName, loader));
                    } catch (final ClassNotFoundException ex) {
                        handleClassNotFoundException();
                    } catch (final PrivilegedActionException pae) {
                        final Throwable thrown = pae.getException();
                        if (thrown instanceof ClassNotFoundException) {
                            handleClassNotFoundException();
                        } else if (thrown instanceof NoClassDefFoundError) {
                            // Dependent class of provider not found
                            if (LOGGER.isLoggable(Level.CONFIG)) {
                                // This assumes that ex.getLocalizedMessage() returns
                                // the name of a dependent class that is not found
                                LOGGER.log(Level.CONFIG,
                                        LocalizationMessages.DEPENDENT_CLASS_OF_PROVIDER_NOT_FOUND(
                                                thrown.getLocalizedMessage(), nextName, service));
                            }
                            nextName = null;
                        } else if (thrown instanceof ClassFormatError) {
                            // Dependent class of provider not found
                            if (LOGGER.isLoggable(Level.CONFIG)) {
                                LOGGER.log(Level.CONFIG,
                                        LocalizationMessages.DEPENDENT_CLASS_OF_PROVIDER_FORMAT_ERROR(
                                                thrown.getLocalizedMessage(), nextName, service));
                            }
                            nextName = null;
                        } else if (thrown instanceof RuntimeException) {
                            throw (RuntimeException) thrown;
                        } else {
                            throw new IllegalStateException(thrown);
                        }
                    }
                }
            }
            return true;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void handleClassNotFoundException() {
            // Provider implementation not found
            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.log(Level.CONFIG,
                        LocalizationMessages.PROVIDER_NOT_FOUND(nextName, service));
            }
            nextName = null;
        }
    }

    private static final class LazyClassIterator<T> extends AbstractLazyIterator<T>
            implements Iterator<Class<T>> {

        private LazyClassIterator(
                final Class<T> service,
                final String serviceName,
                final ClassLoader loader,
                final boolean ignoreOnClassNotFound) {
            super(service, serviceName, loader, ignoreOnClassNotFound);
        }

        @Override
        public Class<T> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final String cn = nextName;
            nextName = null;
            try {

                final Class<T> tClass = AccessController.doPrivileged(
                        ReflectionHelper.<T>classForNameWithExceptionPEA(cn, loader));

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Loading next class: " + tClass.getName());
                }

                return tClass;

            } catch (final ClassNotFoundException ex) {
                fail(serviceName,
                        LocalizationMessages.PROVIDER_NOT_FOUND(cn, service));
            } catch (final PrivilegedActionException pae) {

                final Throwable thrown = pae.getCause();

                if (thrown instanceof ClassNotFoundException) {
                    fail(serviceName,
                            LocalizationMessages.PROVIDER_NOT_FOUND(cn, service));
                } else if (thrown instanceof NoClassDefFoundError) {
                    fail(serviceName,
                            LocalizationMessages.DEPENDENT_CLASS_OF_PROVIDER_NOT_FOUND(
                                    thrown.getLocalizedMessage(), cn, service));
                } else if (thrown instanceof ClassFormatError) {
                    fail(serviceName,
                            LocalizationMessages.DEPENDENT_CLASS_OF_PROVIDER_FORMAT_ERROR(
                                    thrown.getLocalizedMessage(), cn, service));
                } else {
                    fail(serviceName,
                            LocalizationMessages.PROVIDER_CLASS_COULD_NOT_BE_LOADED(cn, service, thrown.getLocalizedMessage()),
                            thrown);
                }
            }

            return null;    /* This cannot happen */
        }
    }

    private static final class LazyObjectIterator<T> extends AbstractLazyIterator<T> implements Iterator<T> {

        private T t;

        private LazyObjectIterator(
                final Class<T> service,
                final String serviceName,
                final ClassLoader loader,
                final boolean ignoreOnClassNotFound) {
            super(service, serviceName, loader, ignoreOnClassNotFound);
        }

        @Override
        public boolean hasNext() throws ServiceConfigurationError {
            if (nextName != null) {
                return true;
            }
            setConfigs();

            while (nextName == null) {
                while ((pending == null) || !pending.hasNext()) {
                    if (!configs.hasMoreElements()) {
                        return false;
                    }
                    pending = parse(serviceName, configs.nextElement(), returned);
                }
                nextName = pending.next();
                try {
                    t = service.cast(AccessController.doPrivileged(
                            ReflectionHelper.classForNameWithExceptionPEA(nextName, loader)).newInstance());

                } catch (final InstantiationException ex) {
                    if (ignoreOnClassNotFound) {
                        if (LOGGER.isLoggable(Level.CONFIG)) {
                            LOGGER.log(Level.CONFIG,
                                    LocalizationMessages.PROVIDER_COULD_NOT_BE_CREATED(nextName, service,
                                            ex.getLocalizedMessage()));
                        }
                        nextName = null;
                    } else {
                        fail(serviceName,
                                LocalizationMessages.PROVIDER_COULD_NOT_BE_CREATED(nextName, service, ex.getLocalizedMessage()),
                                ex);
                    }
                } catch (final IllegalAccessException ex) {
                    fail(serviceName,
                            LocalizationMessages.PROVIDER_COULD_NOT_BE_CREATED(nextName, service, ex.getLocalizedMessage()),
                            ex);

                } catch (final ClassNotFoundException ex) {
                    handleClassNotFoundException();
                } catch (final NoClassDefFoundError ex) {
                    // Dependent class of provider not found
                    if (ignoreOnClassNotFound) {
                        if (LOGGER.isLoggable(Level.CONFIG)) {
                            // This assumes that ex.getLocalizedMessage() returns
                            // the name of a dependent class that is not found
                            LOGGER.log(Level.CONFIG,
                                    LocalizationMessages.DEPENDENT_CLASS_OF_PROVIDER_NOT_FOUND(
                                            ex.getLocalizedMessage(), nextName, service));
                        }
                        nextName = null;
                    } else {
                        fail(serviceName,
                                LocalizationMessages
                                        .DEPENDENT_CLASS_OF_PROVIDER_NOT_FOUND(ex.getLocalizedMessage(), nextName, service),
                                ex);
                    }

                } catch (final PrivilegedActionException pae) {
                    final Throwable cause = pae.getCause();
                    if (cause instanceof ClassNotFoundException) {
                        handleClassNotFoundException();
                    } else if (cause instanceof ClassFormatError) {
                        // Dependent class of provider not found
                        if (ignoreOnClassNotFound) {
                            if (LOGGER.isLoggable(Level.CONFIG)) {
                                LOGGER.log(Level.CONFIG,
                                        LocalizationMessages.DEPENDENT_CLASS_OF_PROVIDER_FORMAT_ERROR(
                                                cause.getLocalizedMessage(), nextName, service));
                            }
                            nextName = null;
                        } else {
                            fail(serviceName,
                                    LocalizationMessages
                                            .DEPENDENT_CLASS_OF_PROVIDER_FORMAT_ERROR(cause.getLocalizedMessage(), nextName,
                                                    service),
                                    cause);
                        }
                    } else {
                        fail(serviceName,
                                LocalizationMessages
                                        .PROVIDER_COULD_NOT_BE_CREATED(nextName, service, cause.getLocalizedMessage()),
                                cause);
                    }
                }
            }
            return true;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            nextName = null;
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Loading next object: " + t.getClass().getName());
            }
            return t;
        }

        private void handleClassNotFoundException() throws ServiceConfigurationError {
            if (ignoreOnClassNotFound) {
                // Provider implementation not found
                if (LOGGER.isLoggable(Level.CONFIG)) {
                    LOGGER.log(Level.CONFIG,
                            LocalizationMessages.PROVIDER_NOT_FOUND(nextName, service));
                }
                nextName = null;
            } else {
                fail(serviceName,
                        LocalizationMessages.PROVIDER_NOT_FOUND(nextName, service));
            }
        }
    }

    /**
     * Supports iteration of provider instances or classes.
     * <p>
     * The default implementation looks up provider classes from META-INF/services
     * files, see {@link DefaultServiceIteratorProvider}.
     * This implementation may be overridden by invoking
     * {@link ServiceFinder#setIteratorProvider(org.glassfish.jersey.internal.ServiceFinder.ServiceIteratorProvider)}.
     */
    public abstract static class ServiceIteratorProvider {

        private static volatile ServiceIteratorProvider sip;
        private static final Object sipLock = new Object();

        private static ServiceIteratorProvider getInstance() {
            // TODO: check the following is a good practice: Double-check idiom for lazy initialization of fields.
            ServiceIteratorProvider result = sip;
            if (result == null) { // First check (no locking)
                synchronized (sipLock) {
                    result = sip;
                    if (result == null) { // Second check (with locking)
                        sip = result = new DefaultServiceIteratorProvider();
                    }
                }
            }
            return result;
        }

        private static void setInstance(final ServiceIteratorProvider sip) throws SecurityException {
            final SecurityManager security = System.getSecurityManager();
            if (security != null) {
                final ReflectPermission rp = new ReflectPermission("suppressAccessChecks");
                security.checkPermission(rp);
            }
            synchronized (sipLock) {
                ServiceIteratorProvider.sip = sip;
            }
        }

        /**
         * Iterate over provider instances of a service.
         *
         * @param <T> the type of the service.
         * @param service the service class.
         * @param serviceName the service name.
         * @param loader the class loader to utilize when loading provider
         *        classes.
         * @param ignoreOnClassNotFound if true ignore an instance if the
         *        corresponding provider class if cannot be found,
         *        otherwise throw a {@link ClassNotFoundException}.
         * @return the provider instance iterator.
         */
        public abstract <T> Iterator<T> createIterator(Class<T> service,
                                                       String serviceName, ClassLoader loader, boolean ignoreOnClassNotFound);

        /**
         * Iterate over provider classes of a service.
         *
         * @param <T> the type of the service.
         * @param service the service class.
         * @param serviceName the service name.
         * @param loader the class loader to utilize when loading provider
         *        classes.
         * @param ignoreOnClassNotFound if true ignore the provider class if
         *        cannot be found,
         *        otherwise throw a {@link ClassNotFoundException}.
         * @return the provider class iterator.
         */
        public abstract <T> Iterator<Class<T>> createClassIterator(Class<T> service,
                                                                   String serviceName,
                                                                   ClassLoader loader,
                                                                   boolean ignoreOnClassNotFound);
    }

    /**
     * The default service iterator provider that looks up provider classes in
     * META-INF/services files.
     * <p>
     * This class may utilized if a {@link ServiceIteratorProvider} needs to
     * reuse the default implementation.
     */
    public static final class DefaultServiceIteratorProvider extends ServiceIteratorProvider {

        @Override
        public <T> Iterator<T> createIterator(final Class<T> service, final String serviceName,
                                              final ClassLoader loader, final boolean ignoreOnClassNotFound) {
            return new LazyObjectIterator<T>(service, serviceName, loader, ignoreOnClassNotFound);
        }

        @Override
        public <T> Iterator<Class<T>> createClassIterator(final Class<T> service, final String serviceName,
                                                          final ClassLoader loader, final boolean ignoreOnClassNotFound) {
            return new LazyClassIterator<T>(service, serviceName, loader, ignoreOnClassNotFound);
        }
    }
}
