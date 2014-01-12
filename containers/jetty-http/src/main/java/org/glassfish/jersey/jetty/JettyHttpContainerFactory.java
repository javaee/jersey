/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ProcessingException;
import java.net.URI;

/**
 * Factory for creating and starting Jetty server handlers. This returns
 * a handle to the started server as {@link Server} instances, which allows
 * the server to be stopped by invoking the {@link org.eclipse.jetty.server.Server#stop()} method.
 * <p/>
 * To start the server in HTTPS mode an {@link SslContextFactory} can be provided.
 * This will be used to decrypt and encrypt information sent over the
 * connected TCP socket channel.
 *
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public final class JettyHttpContainerFactory {

    private JettyHttpContainerFactory() {
    }

    /**
     * Creates a {@link Server} instance that registers an {@link org.eclipse.jetty.server.Handler}.
     *
     * @param uri uri on which the {@link ApplicationHandler} will be deployed. Only first path segment will be used as
     *            context path, the rest will be ignored.
     * @return newly created {@link Server}.
     * @throws ProcessingException
     * @throws IllegalArgumentException if <code>uri</code> is null
     */
    public static Server createServer(final URI uri) throws ProcessingException {
        return createServer(uri, null, null, true);
    }

    /**
     * Creates a {@link Server} instance that registers an {@link org.eclipse.jetty.server.Handler}.
     *
     * @param uri   uri on which the {@link ApplicationHandler} will be deployed. Only first path segment will be used
     *              as context path, the rest will be ignored.
     * @param start if set to false, server will not get started, which allows to configure the underlying transport
     *              layer, see above for details.
     * @return newly created {@link Server}.
     * @throws ProcessingException
     * @throws IllegalArgumentException if <code>uri</code> is null
     */
    public static Server createServer(final URI uri, final boolean start) throws ProcessingException {
        return createServer(uri, null, null, start);
    }

    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     * <p/>
     * This implementation defers to the
     * {@link org.glassfish.jersey.server.ContainerFactory#createContainer(Class, javax.ws.rs.core.Application)} method
     * for creating an Container that manages the root resources.
     *
     * @param uri the URI to create the http server. The URI scheme must be
     *                equal to "http". The URI user information and host
     *                are ignored If the URI port is not present then port 80 will be
     *                used. The URI path, query and fragment components are ignored.
     * @param config  the resource configuration.
     * @return newly created {@link Server}.
     * @throws ProcessingException      Thrown when problems during server creation
     * @throws IllegalArgumentException if <code>uri</code> is null
     */
    public static Server createServer(final URI uri, final ResourceConfig config)
            throws ProcessingException {

        final JettyHttpContainer container = ContainerFactory.createContainer(JettyHttpContainer.class, config);
        return createServer(uri, null, container, true);
    }

    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     * <p/>
     * This implementation defers to the
     * {@link org.glassfish.jersey.server.ContainerFactory#createContainer(Class, javax.ws.rs.core.Application)} method
     * for creating an Container that manages the root resources.
     *
     * @param uri           URI on which the Jersey web application will be deployed. Only first path segment will be
     *                      used as context path, the rest will be ignored.
     * @param configuration web application configuration.
     * @param start         if set to false, server will not get started, which allows to configure the underlying
     *                      transport layer, see above for details.
     * @return newly created {@link Server}.
     * @throws ProcessingException
     * @throws IllegalArgumentException if <code>uri</code> is null
     */
    public static Server createServer(final URI uri, final ResourceConfig configuration, final boolean start) throws ProcessingException {
        return createServer(uri, null, ContainerFactory.createContainer(JettyHttpContainer.class, configuration), start);
    }


    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     * <p/>
     * This implementation defers to the
     * {@link ContainerFactory#createContainer(Class, javax.ws.rs.core.Application)} method
     * for creating an Container that manages the root resources.
     *
     * @param uri the URI to create the http server. The URI scheme must be
     *                equal to "https". The URI user information and host
     *                are ignored If the URI port is not present then port 143 will be
     *                used. The URI path, query and fragment components are ignored.
     * @param sslContextFactory this is the SSL context factory used to configure SSL connector
     * @param config  the resource configuration.
     * @return newly created {@link Server}.
     * @throws ProcessingException      Thrown when problems during server creation
     * @throws IllegalArgumentException if <code>uri</code> is null
     */
    public static Server createServer(final URI uri, final SslContextFactory sslContextFactory, final ResourceConfig config)
            throws ProcessingException {
        final JettyHttpContainer container = ContainerFactory.createContainer(JettyHttpContainer.class, config);
        return createServer(uri, sslContextFactory, container, true);
    }

    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     *
     * @param uri        URI on which the Jersey web application will be deployed.
     * @param appHandler web application handler.
     * @return newly created {@link Server}.
     * @throws ProcessingException Thrown when problems during server creation
     * @throws IllegalArgumentException if <code>uri</code> is null
     */
    public static Server createServer(final URI uri, final ApplicationHandler appHandler) throws ProcessingException {
        return createServer(uri, null, new JettyHttpContainer(appHandler), true);
    }

    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     *
     * @param uri        URI on which the Jersey web application will be deployed. Only first path segment will be used
     *                   as context path, the rest will be ignored.
     * @param appHandler web application handler.
     * @param start      if set to false, server will not get started, which allows to configure the underlying
     *                   transport, see above for details.
     * @return newly created {@link Server}.
     * @throws ProcessingException
     * @throws IllegalArgumentException if <code>uri</code> is null
     */
    public static Server createServer(final URI uri, final ApplicationHandler appHandler, final boolean start) throws ProcessingException {
        return createServer(uri, null, new JettyHttpContainer(appHandler), start);
    }

    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     *
     * @param uri        URI on which the Jersey web application will be deployed.
     * @param sslContextFactory    this is the SSL context factory used to configure SSL connector
     * @param appHandler web application handler.
     * @return newly created {@link Server}.
     * @throws ProcessingException Thrown when problems during server creation
     * @throws IllegalArgumentException if <code>uri</code> is null
     */
    public static Server createServer(final URI uri, final SslContextFactory sslContextFactory, final ApplicationHandler appHandler) throws ProcessingException {
        return createServer(uri, sslContextFactory, new JettyHttpContainer(appHandler), true);
    }

    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes found by searching the
     * classes referenced in the java classpath.
     *
     * @param uri   the URI to create the http server. The URI scheme must be
     *                  equal to "https". The URI user information and host
     *                  are ignored If the URI port is not present then port 143 will be
     *                  used. The URI path, query and fragment components are ignored.
     * @param sslContextFactory   this is the SSL context factory used to configure SSL connector
     * @param handler the container that handles all HTTP requests
     * @param start                 if set to false, server will not get started, this allows end users to set
     *                              additional properties on the underlying listener.
     * @return newly created {@link Server}.
     * @throws ProcessingException Thrown when problems during server creation
     * @throws IllegalArgumentException if <code>uri</code> is null
     * @see JettyHttpContainer
     */
    public static Server createServer(final URI uri,
                                final SslContextFactory sslContextFactory,
                                final JettyHttpContainer handler,
                                final boolean start)
            throws ProcessingException {
        if (uri == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }
        String scheme = uri.getScheme();
        int defaultPort = 80;

        if (sslContextFactory == null) {
            if (!scheme.equalsIgnoreCase("http")) {
                throw new IllegalArgumentException("The URI scheme should be 'http' when not using SSL");
            }
        } else {
            if (!scheme.equalsIgnoreCase("https")) {
                throw new IllegalArgumentException("The URI scheme should be 'https' when using SSL");
            }
            defaultPort = 143; // default HTTPS port
        }
        final int port = (uri.getPort() == -1) ? defaultPort : uri.getPort();

        Server server = new Server();
        HttpConfiguration config = new HttpConfiguration();
        if (sslContextFactory != null) {
            config.setSecureScheme("https");
            config.setSecurePort(port);
            config.addCustomizer(new SecureRequestCustomizer());

            ServerConnector https = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(config));
            https.setPort(port);
            server.setConnectors(new Connector[]{https});

        } else {
            ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(config));
            http.setPort(port);
            server.setConnectors(new Connector[]{http});
        }
        if (handler != null) {
            server.setHandler(handler);
        }

        if (start) {
            try {
                // Start the server.
                server.start();
            } catch (Exception e) {
                throw new ProcessingException("Exception thrown when trying to create jetty server", e);
            }
        }
        return server;
    }
}