/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.util.concurrent.ThreadFactory;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.jetty.internal.LocalizationMessages;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

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
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class JettyHttpContainerFactory {

    private JettyHttpContainerFactory() {
    }

    /**
     * Creates a {@link Server} instance that registers an {@link org.eclipse.jetty.server.Handler}.
     *
     * @param uri uri on which the {@link org.glassfish.jersey.server.ApplicationHandler} will be deployed. Only first path
     *            segment will be used as context path, the rest will be ignored.
     * @return newly created {@link Server}.
     *
     * @throws ProcessingException      in case of any failure when creating a new Jetty {@code Server} instance.
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
     */
    public static Server createServer(final URI uri) throws ProcessingException {
        return createServer(uri, null, null, true);
    }

    /**
     * Creates a {@link Server} instance that registers an {@link org.eclipse.jetty.server.Handler}.
     *
     * @param uri   uri on which the {@link org.glassfish.jersey.server.ApplicationHandler} will be deployed. Only first path
     *              segment will be used as context path, the rest will be ignored.
     * @param start if set to false, server will not get started, which allows to configure the underlying transport
     *              layer, see above for details.
     * @return newly created {@link Server}.
     *
     * @throws ProcessingException      in case of any failure when creating a new Jetty {@code Server} instance.
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
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
     * @param uri    the URI to create the http server. The URI scheme must be
     *               equal to "http". The URI user information and host
     *               are ignored If the URI port is not present then port 80 will be
     *               used. The URI path, query and fragment components are ignored.
     * @param config the resource configuration.
     * @return newly created {@link Server}.
     *
     * @throws ProcessingException      in case of any failure when creating a new Jetty {@code Server} instance.
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
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
     *
     * @throws ProcessingException      in case of any failure when creating a new Jetty {@code Server} instance.
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
     */
    public static Server createServer(final URI uri, final ResourceConfig configuration, final boolean start)
            throws ProcessingException {
        return createServer(uri, null, ContainerFactory.createContainer(JettyHttpContainer.class, configuration), start);
    }


    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     *
     * @param uri           the URI to create the http server. The URI scheme must be
     *                      equal to "https". The URI user information and host
     *                      are ignored If the URI port is not present then port 143 will be
     *                      used. The URI path, query and fragment components are ignored.
     * @param config        the resource configuration.
     * @param parentContext DI provider specific context with application's registered bindings.
     * @param start         if set to false, server will not get started, this allows end users to set
     *                      additional properties on the underlying listener.
     * @return newly created {@link Server}.
     *
     * @throws ProcessingException      in case of any failure when creating a new Jetty {@code Server} instance.
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
     * @see JettyHttpContainer
     * @since 2.12
     */
    public static Server createServer(final URI uri, final ResourceConfig config, final boolean start,
                                      final Object parentContext) {
        return createServer(uri, null, new JettyHttpContainer(config, parentContext), start);
    }


    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     *
     * @param uri           the URI to create the http server. The URI scheme must be
     *                      equal to "https". The URI user information and host
     *                      are ignored If the URI port is not present then port 143 will be
     *                      used. The URI path, query and fragment components are ignored.
     * @param config        the resource configuration.
     * @param parentContext DI provider specific context with application's registered bindings.
     * @return newly created {@link Server}.
     *
     * @throws ProcessingException      in case of any failure when creating a new Jetty {@code Server} instance.
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
     * @see JettyHttpContainer
     * @since 2.12
     */
    public static Server createServer(final URI uri, final ResourceConfig config, final Object parentContext) {
        return createServer(uri, null, new JettyHttpContainer(config, parentContext), true);
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
     * @param uri               the URI to create the http server. The URI scheme must be
     *                          equal to {@code https}. The URI user information and host
     *                          are ignored. If the URI port is not present then port
     *                          {@value org.glassfish.jersey.server.spi.Container#DEFAULT_HTTPS_PORT} will be
     *                          used. The URI path, query and fragment components are ignored.
     * @param sslContextFactory this is the SSL context factory used to configure SSL connector
     * @param config            the resource configuration.
     * @return newly created {@link Server}.
     *
     * @throws ProcessingException      in case of any failure when creating a new Jetty {@code Server} instance.
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
     */
    public static Server createServer(final URI uri, final SslContextFactory sslContextFactory, final ResourceConfig config)
            throws ProcessingException {
        final JettyHttpContainer container = ContainerFactory.createContainer(JettyHttpContainer.class, config);
        return createServer(uri, sslContextFactory, container, true);
    }

    /**
     * Create a {@link Server} that registers an {@link org.eclipse.jetty.server.Handler} that
     * in turn manages all root resource and provider classes found by searching the
     * classes referenced in the java classpath.
     *
     * @param uri               the URI to create the http server. The URI scheme must be
     *                          equal to {@code https}. The URI user information and host
     *                          are ignored. If the URI port is not present then port
     *                          {@value org.glassfish.jersey.server.spi.Container#DEFAULT_HTTPS_PORT} will be
     *                          used. The URI path, query and fragment components are ignored.
     * @param sslContextFactory this is the SSL context factory used to configure SSL connector
     * @param handler           the container that handles all HTTP requests
     * @param start             if set to false, server will not get started, this allows end users to set
     *                          additional properties on the underlying listener.
     * @return newly created {@link Server}.
     *
     * @throws ProcessingException      in case of any failure when creating a new Jetty {@code Server} instance.
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
     * @see JettyHttpContainer
     */
    public static Server createServer(final URI uri,
                                      final SslContextFactory sslContextFactory,
                                      final JettyHttpContainer handler,
                                      final boolean start) {
        if (uri == null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_CANNOT_BE_NULL());
        }
        final String scheme = uri.getScheme();
        int defaultPort = Container.DEFAULT_HTTP_PORT;

        if (sslContextFactory == null) {
            if (!"http".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException(LocalizationMessages.WRONG_SCHEME_WHEN_USING_HTTP());
            }
        } else {
            if (!"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException(LocalizationMessages.WRONG_SCHEME_WHEN_USING_HTTPS());
            }
            defaultPort = Container.DEFAULT_HTTPS_PORT;
        }
        final int port = (uri.getPort() == -1) ? defaultPort : uri.getPort();

        final Server server = new Server(new JettyConnectorThreadPool());
        final HttpConfiguration config = new HttpConfiguration();
        if (sslContextFactory != null) {
            config.setSecureScheme("https");
            config.setSecurePort(port);
            config.addCustomizer(new SecureRequestCustomizer());

            final ServerConnector https = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(config));
            https.setPort(port);
            server.setConnectors(new Connector[]{https});

        } else {
            final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(config));
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
            } catch (final Exception e) {
                throw new ProcessingException(LocalizationMessages.ERROR_WHEN_CREATING_SERVER(), e);
            }
        }
        return server;
    }

    private static final class JettyConnectorThreadPool extends QueuedThreadPool {
        private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("jetty-http-server-%d")
                .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                .build();

        @Override
        protected Thread newThread(Runnable runnable) {
            return threadFactory.newThread(runnable);
        }
    }
}
