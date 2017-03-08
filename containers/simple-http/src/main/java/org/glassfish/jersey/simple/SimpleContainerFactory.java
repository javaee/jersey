/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.simple;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import javax.ws.rs.ProcessingException;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.internal.util.collection.UnsafeValue;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.simple.internal.LocalizationMessages;

import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.SocketProcessor;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * Factory for creating and starting Simple server containers. This returns a handle to the started
 * server as {@link Closeable} instances, which allows the server to be stopped by invoking the
 * {@link Closeable#close} method.
 * <p/>
 * To start the server in HTTPS mode an {@link SSLContext} can be provided. This will be used to
 * decrypt and encrypt information sent over the connected TCP socket channel.
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Paul Sandoz
 */
public final class SimpleContainerFactory {

    private SimpleContainerFactory() {
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that in turn manages all root
     * resource and provider classes declared by the resource configuration.
     *
     * @param address the URI to create the http server. The URI scheme must be equal to "http". The
     *                URI user information and host are ignored If the URI port is not present then port 80
     *                will be used. The URI path, query and fragment components are ignored.
     * @param config  the resource configuration.
     * @return the closeable connection, with the endpoint started.
     * @throws ProcessingException      thrown when problems during server creation.
     * @throws IllegalArgumentException if {@code address} is {@code null}.
     */
    public static SimpleServer create(final URI address, final ResourceConfig config) {
        return create(address, null, new SimpleContainer(config));
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that in turn manages all root
     * resource and provider classes declared by the resource configuration.
     *
     * @param address the URI to create the http server. The URI scheme must be equal to "http". The
     *                URI user information and host are ignored If the URI port is not present then port 80
     *                will be used. The URI path, query and fragment components are ignored.
     * @param config  the resource configuration.
     * @param count   this is the number of threads to be used.
     * @param select  this is the number of selector threads to use.
     * @return the closeable connection, with the endpoint started.
     * @throws ProcessingException      thrown when problems during server creation.
     * @throws IllegalArgumentException if {@code address} is {@code null}.
     */
    public static SimpleServer create(final URI address, final ResourceConfig config, final int count,
                                      final int select) {
        return create(address, null, new SimpleContainer(config), count, select);
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that in turn manages all root
     * resource and provider classes declared by the resource configuration.
     *
     * @param address the URI to create the http server. The URI scheme must be equal to {@code https}
     *                . The URI user information and host are ignored. If the URI port is not present then
     *                port {@value org.glassfish.jersey.server.spi.Container#DEFAULT_HTTPS_PORT} will be used.
     *                The URI path, query and fragment components are ignored.
     * @param context this is the SSL context used for SSL connections.
     * @param config  the resource configuration.
     * @return the closeable connection, with the endpoint started.
     * @throws ProcessingException      thrown when problems during server creation.
     * @throws IllegalArgumentException if {@code address} is {@code null}.
     */
    public static SimpleServer create(final URI address, final SSLContext context,
                                      final ResourceConfig config) {
        return create(address, context, new SimpleContainer(config));
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that in turn manages all root
     * resource and provider classes declared by the resource configuration.
     *
     * @param address the URI to create the http server. The URI scheme must be equal to {@code https}
     *                . The URI user information and host are ignored. If the URI port is not present then
     *                port {@value org.glassfish.jersey.server.spi.Container#DEFAULT_HTTPS_PORT} will be used.
     *                The URI path, query and fragment components are ignored.
     * @param context this is the SSL context used for SSL connections.
     * @param config  the resource configuration.
     * @param count   this is the number of threads to be used.
     * @param select  this is the number of selector threads to use.
     * @return the closeable connection, with the endpoint started.
     * @throws ProcessingException      thrown when problems during server creation.
     * @throws IllegalArgumentException if {@code address} is {@code null}.
     */
    public static SimpleServer create(final URI address, final SSLContext context,
                                      final ResourceConfig config, final int count, final int select) {
        return create(address, context, new SimpleContainer(config), count, select);
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that in turn manages all root
     * resource and provider classes found by searching the classes referenced in the java classpath.
     *
     * @param address   the URI to create the http server. The URI scheme must be equal to {@code https}
     *                  . The URI user information and host are ignored. If the URI port is not present then
     *                  port {@value org.glassfish.jersey.server.spi.Container#DEFAULT_HTTPS_PORT} will be used.
     *                  The URI path, query and fragment components are ignored.
     * @param context   this is the SSL context used for SSL connections.
     * @param container the container that handles all HTTP requests.
     * @return the closeable connection, with the endpoint started.
     * @throws ProcessingException      thrown when problems during server creation.
     * @throws IllegalArgumentException if {@code address} is {@code null}.
     */
    public static SimpleServer create(final URI address, final SSLContext context,
                                      final SimpleContainer container) {
        return _create(address, context, container, new UnsafeValue<SocketProcessor, IOException>() {
            @Override
            public SocketProcessor get() throws IOException {
                return new ContainerSocketProcessor(container);
            }
        });
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that in turn manages all root
     * resource and provider classes declared by the resource configuration.
     *
     * @param address       the URI to create the http server. The URI scheme must be equal to {@code https}
     *                      . The URI user information and host are ignored. If the URI port is not present then
     *                      port {@value org.glassfish.jersey.server.spi.Container#DEFAULT_HTTPS_PORT} will be used.
     *                      The URI path, query and fragment components are ignored.
     * @param context       this is the SSL context used for SSL connections.
     * @param config        the resource configuration.
     * @param parentContext DI provider specific context with application's registered bindings.
     * @param count         this is the number of threads to be used.
     * @param select        this is the number of selector threads to use.
     * @return the closeable connection, with the endpoint started.
     * @throws ProcessingException      thrown when problems during server creation.
     * @throws IllegalArgumentException if {@code address} is {@code null}.
     * @since 2.12
     */
    public static SimpleServer create(final URI address, final SSLContext context,
                                      final ResourceConfig config, final Object parentContext, final int count,
                                      final int select) {
        return create(address, context, new SimpleContainer(config, parentContext), count, select);
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that in turn manages all root
     * resource and provider classes found by searching the classes referenced in the java classpath.
     *
     * @param address   the URI to create the http server. The URI scheme must be equal to {@code https}
     *                  . The URI user information and host are ignored. If the URI port is not present then
     *                  port {@value org.glassfish.jersey.server.spi.Container#DEFAULT_HTTPS_PORT} will be used.
     *                  The URI path, query and fragment components are ignored.
     * @param context   this is the SSL context used for SSL connections.
     * @param container the container that handles all HTTP requests.
     * @param count     this is the number of threads to be used.
     * @param select    this is the number of selector threads to use.
     * @return the closeable connection, with the endpoint started.
     * @throws ProcessingException      thrown when problems during server creation.
     * @throws IllegalArgumentException if {@code address} is {@code null}.
     */
    public static SimpleServer create(final URI address, final SSLContext context,
                                      final SimpleContainer container, final int count, final int select)
            throws ProcessingException {

        return _create(address, context, container, new UnsafeValue<SocketProcessor, IOException>() {
            @Override
            public SocketProcessor get() throws IOException {
                return new ContainerSocketProcessor(container, count, select);
            }
        });
    }

    private static SimpleServer _create(final URI address, final SSLContext context,
                                        final SimpleContainer container,
                                        final UnsafeValue<SocketProcessor, IOException> serverProvider)
            throws ProcessingException {
        if (address == null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_CANNOT_BE_NULL());
        }
        final String scheme = address.getScheme();
        int defaultPort = org.glassfish.jersey.server.spi.Container.DEFAULT_HTTP_PORT;

        if (context == null) {
            if (!scheme.equalsIgnoreCase("http")) {
                throw new IllegalArgumentException(LocalizationMessages.WRONG_SCHEME_WHEN_USING_HTTP());
            }
        } else {
            if (!scheme.equalsIgnoreCase("https")) {
                throw new IllegalArgumentException(LocalizationMessages.WRONG_SCHEME_WHEN_USING_HTTPS());
            }
            defaultPort = org.glassfish.jersey.server.spi.Container.DEFAULT_HTTPS_PORT;
        }
        int port = address.getPort();

        if (port == -1) {
            port = defaultPort;
        }
        final InetSocketAddress listen = new InetSocketAddress(port);
        final Connection connection;
        try {
            final SimpleTraceAnalyzer analyzer = new SimpleTraceAnalyzer();
            final SocketProcessor server = serverProvider.get();
            connection = new SocketConnection(server, analyzer);


            final SocketAddress socketAddr = connection.connect(listen, context);
            container.onServerStart();

            return new SimpleServer() {

                @Override
                public void close() throws IOException {
                    container.onServerStop();
                    analyzer.stop();
                    connection.close();
                }

                @Override
                public int getPort() {
                    return ((InetSocketAddress) socketAddr).getPort();
                }

                @Override
                public boolean isDebug() {
                    return analyzer.isActive();
                }

                @Override
                public void setDebug(boolean enable) {
                    if (enable) {
                        analyzer.start();
                    } else {
                        analyzer.stop();
                    }
                }
            };
        } catch (final IOException ex) {
            throw new ProcessingException(LocalizationMessages.ERROR_WHEN_CREATING_SERVER(), ex);
        }
    }
}
