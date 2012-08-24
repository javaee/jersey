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
package org.glassfish.jersey.server.internal.scanning;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ReflectPermission;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.jersey.internal.OsgiRegistry;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.server.ResourceFinder;
import org.glassfish.jersey.uri.UriComponent;

/**
 * A scanner that recursively scans URI-based resources present in a set of
 * package names, and sub-package names of that set.
 * <p>
 * The URIs for a package name are obtained, by default, by invoking
 * {@link ClassLoader#getResources(java.lang.String) } with the parameter that
 * is the package name with "." replaced by "/".
 * <p>
 * Each URI is then scanned using a registered {@link UriSchemeResourceFinderFactory} that
 * supports the URI scheme.
 * <p>
 * The following are registered by default.
 * The {@link FileSchemeResourceFinderFactory} for "file" URI schemes.
 * The {@link JarZipSchemeResourceFinderFactory} for "jar" or "zip" URI schemes to jar
 * resources.
 * The {@link VfsSchemeResourceFinderFactory} for the JBoss-based "vfsfile" and "vfszip"
 * URI schemes.
 * <p>
 * Further schemes may be registered by registering an implementation of
 * {@link UriSchemeResourceFinderFactory} in the META-INF/services file whose name is the
 * the fully qualified class name of {@link UriSchemeResourceFinderFactory}.
 * <p>
 * If a URI scheme is not supported a {@link ResourceFinderException} will be thrown
 * and package scanning deployment will fail.
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class PackageNamesScanner implements ResourceFinder {

    private final String[] packages;
    private final ClassLoader classloader;
    private final Map<String, UriSchemeResourceFinderFactory> finderFactories;

    private ResourceFinderStack resourceFinderStack;

    /**
     * Scan from a set of packages using the context {@link ClassLoader}.
     *
     * @param packages an array of package names.
     */
    public PackageNamesScanner(final String[] packages) {
        this(ReflectionHelper.getContextClassLoader(), Tokenizer.tokenize(packages, Tokenizer.COMMON_DELIMITERS));
    }

    /**
     * Scan from a set of packages using provided {@link ClassLoader}.
     *
     * @param classloader the {@link ClassLoader} to load classes from.
     * @param packages an array of package names.
     */
    public PackageNamesScanner(final ClassLoader classloader, final String[] packages) {
        this.packages = packages;
        this.classloader = classloader;

        this.finderFactories = new HashMap<String, UriSchemeResourceFinderFactory>();
        add(new JarZipSchemeResourceFinderFactory());
        add(new FileSchemeResourceFinderFactory());
        add(new VfsSchemeResourceFinderFactory());
        add(new BundleSchemeResourceFinderFactory());

//        TODO - Services?
//        for (UriSchemeResourceFinderFactory s : ServiceFinder.find(UriSchemeResourceFinderFactory.class)) {
//            add(s);
//        }

        final OsgiRegistry osgiRegistry = ReflectionHelper.getOsgiRegistryInstance();
        if (osgiRegistry != null) {
            setResourcesProvider(new PackageNamesScanner.ResourcesProvider() {

                @Override
                public Enumeration<URL> getResources(String name, ClassLoader cl) throws IOException {
                    return osgiRegistry.getPackageResources(name);
                }
            });
        }

        init();
    }

    private void add(final UriSchemeResourceFinderFactory uriSchemeResourceFinderFactory) {
        for (final String s : uriSchemeResourceFinderFactory.getSchemes()) {
            finderFactories.put(s.toLowerCase(), uriSchemeResourceFinderFactory);
        }
    }


    @Override
    public boolean hasNext() {
        return resourceFinderStack.hasNext();
    }

    @Override
    public String next() {
        return resourceFinderStack.next();
    }

    @Override
    public void remove() {
        resourceFinderStack.remove();
    }

    @Override
    public InputStream open() {
        return resourceFinderStack.open();
    }

    @Override
    public void reset() {
        init();
    }

    private void init() {
        resourceFinderStack = new ResourceFinderStack();

        for (final String p : packages) {
            try {
                final Enumeration<URL> urls = ResourcesProvider.getInstance().
                        getResources(p.replace('.', '/'), classloader);
                while (urls.hasMoreElements()) {
                    try {
                        addResourceFinder(toURI(urls.nextElement()));
                    } catch (URISyntaxException e) {
                        throw new ResourceFinderException("Error when converting a URL to a URI", e);
                    }
                }
            } catch (IOException e) {
                throw new ResourceFinderException("IO error when package scanning jar", e);
            }
        }

    }

    /**
     * Find resources with a given name and class loader.
     */
    public static abstract class ResourcesProvider {

        private static volatile ResourcesProvider provider;

        private static ResourcesProvider getInstance() {
            // Double-check idiom for lazy initialization
            ResourcesProvider result = provider;

            if (result == null) { // first check without locking
                synchronized (ResourcesProvider.class) {
                    result = provider;
                    if (result == null) { // second check with locking
                        provider = result = new ResourcesProvider() {

                            @Override
                            public Enumeration<URL> getResources(String name, ClassLoader cl)
                                    throws IOException {
                                return cl.getResources(name);
                            }
                        };

                    }
                }

            }
            return result;
        }

        private static void setInstance(ResourcesProvider provider) throws SecurityException {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                ReflectPermission rp = new ReflectPermission("suppressAccessChecks");
                security.checkPermission(rp);
            }
            synchronized (ResourcesProvider.class) {
                ResourcesProvider.provider = provider;
            }
        }

        /**
         * Find all resources with the given name using a class loader.
         *
         * @param cl the class loader use to find the resources
         * @param name the resource name
         * @return An enumeration of URL objects for the resource.
         *         If no resources could be found, the enumeration will be empty.
         *         Resources that the class loader doesn't have access to will
         *         not be in the enumeration.
         * @throws IOException if I/O errors occur
         */
        public abstract Enumeration<URL> getResources(String name, ClassLoader cl) throws IOException;
    }

    /**
     * Set the {@link ResourcesProvider} implementation to find resources.
     * <p>
     * This method should be invoked before any package scanning is performed
     * otherwise the functionality method will be utilized.
     *
     * @param provider the resources provider.
     * @throws SecurityException if the resources provider cannot be set.
     */
    public static void setResourcesProvider(ResourcesProvider provider) throws SecurityException {
        ResourcesProvider.setInstance(provider);
    }

    private void addResourceFinder(final URI u) {
        final UriSchemeResourceFinderFactory finderFactory = finderFactories.get(u.getScheme().toLowerCase());
        if (finderFactory != null) {
            resourceFinderStack.push(finderFactory.create(u));
        } else {
            throw new ResourceFinderException("The URI scheme " + u.getScheme()
                    + " of the URI " + u
                    + " is not supported. Package scanning deployment is not"
                    + " supported for such URIs."
                    + "\nTry using a different deployment mechanism such as"
                    + " explicitly declaring root resource and provider classes"
                    + " using an extension of javax.ws.rs.core.Application");
        }
    }

    private URI toURI(URL url) throws URISyntaxException {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            // Work around bug where some URLs are incorrectly encoded.
            // This can occur when certain class loaders are utilized
            // to obtain URLs for resources.
            return URI.create(toExternalForm(url));
        }
    }

    private String toExternalForm(URL u) {

        // pre-compute length of StringBuffer
        int len = u.getProtocol().length() + 1;
        if (u.getAuthority() != null && u.getAuthority().length() > 0) {
            len += 2 + u.getAuthority().length();
        }
        if (u.getPath() != null) {
            len += u.getPath().length();
        }
        if (u.getQuery() != null) {
            len += 1 + u.getQuery().length();
        }
        if (u.getRef() != null) {
            len += 1 + u.getRef().length();
        }

        StringBuilder result = new StringBuilder(len);
        result.append(u.getProtocol());
        result.append(":");
        if (u.getAuthority() != null && u.getAuthority().length() > 0) {
            result.append("//");
            result.append(u.getAuthority());
        }
        if (u.getPath() != null) {
            result.append(UriComponent.contextualEncode(u.getPath(), UriComponent.Type.PATH));
        }
        if (u.getQuery() != null) {
            result.append('?');
            result.append(UriComponent.contextualEncode(u.getQuery(), UriComponent.Type.QUERY));
        }
        if (u.getRef() != null) {
            result.append("#");
            result.append(u.getRef());
        }
        return result.toString();
    }
}
