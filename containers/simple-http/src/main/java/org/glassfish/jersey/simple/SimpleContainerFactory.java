/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.ConfigHelper;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

/**
 * Factory for creating and starting Simple server containers. This returns
 * a handle to the started server as {@link Closeable} instances, which allows
 * the server to be stopped by invoking the {@link Closeable#close} method.
 * <p/>
 * To start the server in HTTPS mode an {@link SSLContext} can be provided.
 * This will be used to decrypt and encrypt information sent over the
 * connected TCP socket channel.
 *
 * @author Paul.Sandoz@Sun.Com
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public final class SimpleContainerFactory {

    private SimpleContainerFactory() {
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     * <p/>
     * This implementation defers to the
     * {@link ContainerFactory#createContainer(Class, javax.ws.rs.core.Application)} method
     * for creating an Container that manages the root resources.
     *
     * @param address the URI to create the http server. The URI scheme must be
     *                equal to "http". The URI user information and host
     *                are ignored If the URI port is not present then port 80 will be
     *                used. The URI path, query and fragment components are ignored.
     * @param config  the resource configuration.
     * @return the closeable connection, with the endpoint started
     * @throws ProcessingException      Thrown when problems during server creation
     * @throws IllegalArgumentException if <code>address</code> is null
     */
    public static Closeable create(URI address, ResourceConfig config)
            throws ProcessingException {

        final SimpleContainer container = ContainerFactory.createContainer(SimpleContainer.class, config);
        return create(address, null, container, ConfigHelper.getContainerLifecycleListener(new ApplicationHandler(config)));
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that
     * in turn manages all root resource and provider classes declared by the
     * resource configuration.
     * <p/>
     * This implementation defers to the
     * {@link ContainerFactory#createContainer(Class, javax.ws.rs.core.Application)} method
     * for creating an Container that manages the root resources.
     *
     * @param address the URI to create the http server. The URI scheme must be
     *                equal to "https". The URI user information and host
     *                are ignored If the URI port is not present then port 143 will be
     *                used. The URI path, query and fragment components are ignored.
     * @param context this is the SSL context used for SSL connections
     * @param config  the resource configuration.
     * @return the closeable connection, with the endpoint started
     * @throws ProcessingException      Thrown when problems during server creation
     * @throws IllegalArgumentException if <code>address</code> is null
     */
    public static Closeable create(URI address, SSLContext context, ResourceConfig config)
            throws ProcessingException {
        final SimpleContainer container = ContainerFactory.createContainer(SimpleContainer.class, config);
        return create(address, context, container, ConfigHelper.getContainerLifecycleListener(new ApplicationHandler(config)));
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri        URI on which the Jersey web application will be deployed.
     * @param appHandler web application handler.
     * @return the closeable connection, with the endpoint started
     * @throws ProcessingException Thrown when problems during server creation
     */
    public static Closeable create(final URI uri, final ApplicationHandler appHandler) throws ProcessingException {
        return create(uri, null, new SimpleContainer(appHandler), ConfigHelper.getContainerLifecycleListener(appHandler));
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri        URI on which the Jersey web application will be deployed.
     * @param appHandler web application handler.
     * @return the closeable connection, with the endpoint started
     * @throws ProcessingException Thrown when problems during server creation
     */
    public static Closeable create(final URI uri, SSLContext context, final ApplicationHandler appHandler) throws ProcessingException {
        return create(uri, context, new SimpleContainer(appHandler), ConfigHelper.getContainerLifecycleListener(appHandler));
    }

    /**
     * Create a {@link Closeable} that registers an {@link Container} that
     * in turn manages all root resource and provder classes found by searching the
     * classes referenced in the java classpath.
     *
     * @param address   the URI to create the http server. The URI scheme must be
     *                  equal to "https". The URI user information and host
     *                  are ignored If the URI port is not present then port 143 will be
     *                  used. The URI path, query and fragment components are ignored.
     * @param context   this is the SSL context used for SSL connections
     * @param container the container that handles all HTTP requests
     * @return the closeable connection, with the endpoint started
     * @throws ProcessingException Thrown when problems during server creation
     */
    public static Closeable create(final URI address, final SSLContext context, final SimpleContainer container, final ContainerLifecycleListener containerListener)
            throws ProcessingException {
        if (address == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }
        String scheme = address.getScheme();
        int defaultPort = 80;

        if (context == null) {
            if (!scheme.equalsIgnoreCase("http")) {
                throw new IllegalArgumentException("The URI scheme should be 'http' when not using SSL");
            }
        } else {
            if (!scheme.equalsIgnoreCase("https")) {
                throw new IllegalArgumentException("The URI scheme should be 'https' when using SSL");
            }
            defaultPort = 143; // default HTTPS port
        }
        int port = address.getPort();

        if (port == -1) {
            port = defaultPort;
        }
        SocketAddress listen = new InetSocketAddress(port);
        Connection connection;
        try {
            Server server = new ContainerServer(container);
            connection = new SocketConnection(server);

            connection.connect(listen, context);
        } catch (IOException ex) {
            throw new ProcessingException("IOException thrown when trying to create simple server", ex);
        }

        containerListener.onStartup(container);

        return connection;
    }
}
