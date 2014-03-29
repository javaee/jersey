/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.grizzly2.httpserver;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

/**
 * Factory for creating Grizzly Http Server.
 * Should you need to fine tune the underlying
 * Grizzly transport layer, you could obtain direct access to the corresponding Grizzly
 * structures with {@code server.getListener("grizzly").getTransport()}. To make certain
 * options take effect, you need to work with a non-started-yet HttpServer instance.
 * To obtain such an instance, use one of the bellow factory methods with {@code start}
 * parameter set to false. When the {@code start} parameter is not present,
 * the factory method returns an already started instance.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @see HttpServer
 * @see GrizzlyHttpContainer
 */
public class GrizzlyHttpServerFactory {

    /**
     * Creates HttpServer instance.
     *
     * @param uri uri on which the {@link ApplicationHandler} will be deployed. Only first path segment will be used as
     *            context path, the rest will be ignored.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri) throws ProcessingException {
        return createHttpServer(uri, (GrizzlyHttpContainer) null, false, null, true);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri   uri on which the {@link ApplicationHandler} will be deployed. Only first path segment will be used
     *              as context path, the rest will be ignored.
     * @param start if set to false, server will not get started, which allows to configure the underlying transport
     *              layer, see above for details.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri, final boolean start) throws ProcessingException {
        return createHttpServer(uri, (GrizzlyHttpContainer) null, false, null, start);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri           URI on which the Jersey web application will be deployed. Only first path segment will be
     *                      used as context path, the rest will be ignored.
     * @param configuration web application configuration.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri, final ResourceConfig configuration) throws ProcessingException {
        return createHttpServer(uri, createContainer(configuration), false, null, true);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri           URI on which the Jersey web application will be deployed. Only first path segment will be
     *                      used as context path, the rest will be ignored.
     * @param configuration web application configuration.
     * @param start         if set to false, server will not get started, which allows to configure the underlying
     *                      transport layer, see above for details.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri, final ResourceConfig configuration, final boolean start) throws ProcessingException {
        return createHttpServer(uri, createContainer(configuration), false, null, start);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri        URI on which the Jersey web application will be deployed. Only first path segment will be used
     *                   as context path, the rest will be ignored.
     * @param appHandler web application handler.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri, final ApplicationHandler appHandler) throws ProcessingException {
        return createHttpServer(uri, new GrizzlyHttpContainer(appHandler), false, null, true);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri        URI on which the Jersey web application will be deployed. Only first path segment will be used
     *                   as context path, the rest will be ignored.
     * @param appHandler web application handler.
     * @param start      if set to false, server will not get started, which allows to configure the underlying
     *                   transport, see above for details.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri, final ApplicationHandler appHandler, final boolean start) throws ProcessingException {
        return createHttpServer(uri, new GrizzlyHttpContainer(appHandler), false, null, start);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri                   URI on which the Jersey web application will be deployed. Only first path segment
     *                              will be used as context path, the rest will be ignored.
     * @param configuration         web application configuration.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig(org.glassfish.grizzly.ssl.SSLEngineConfigurator)}.
     * @return newly created {@link HttpServer}.
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ResourceConfig configuration,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator) {
        return createHttpServer(uri, createContainer(configuration), secure, sslEngineConfigurator, true);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri                   URI on which the Jersey web application will be deployed. Only first path segment
     *                              will be used as context path, the rest will be ignored.
     * @param configuration         web application configuration.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig(org.glassfish.grizzly.ssl.SSLEngineConfigurator)}.
     * @param start                 if set to false, server will not get started, which allows to configure the
     *                              underlying transport, see above for details.
     * @return newly created {@link HttpServer}.
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ResourceConfig configuration,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator,
                                              final boolean start) {
        return createHttpServer(uri, createContainer(configuration), secure, sslEngineConfigurator, start);
    }

    private static GrizzlyHttpContainer createContainer(ResourceConfig configuration) {
        return new GrizzlyHttpContainer(new ApplicationHandler(configuration));
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri                   URI on which the Jersey web application will be deployed. Only first path segment
     *                              will be used as context path, the rest will be ignored.
     * @param appHandler            web application handler.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig(org.glassfish.grizzly.ssl.SSLEngineConfigurator)}.
     * @return newly created {@link HttpServer}.
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ApplicationHandler appHandler,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator) {
        return createHttpServer(uri, new GrizzlyHttpContainer(appHandler), secure, sslEngineConfigurator, true);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri                   URI on which the Jersey web application will be deployed. Only first path segment
     *                              will be used as context path, the rest will be ignored.
     * @param appHandler            web application handler.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig(org.glassfish.grizzly.ssl.SSLEngineConfigurator)}.
     * @param start                 if set to false, server will not get started, which allows to configure the
     *                              underlying transport, see above for details.
     * @return newly created {@link HttpServer}.
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ApplicationHandler appHandler,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator,
                                              final boolean start) {
        return createHttpServer(uri, new GrizzlyHttpContainer(appHandler), secure, sslEngineConfigurator, start);
    }


    /**
     * Creates HttpServer instance.
     *
     * @param uri                   uri on which the {@link ApplicationHandler} will be deployed. Only first path
     *                              segment will be used as context path, the rest will be ignored.
     * @param handler               {@link HttpHandler} instance.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig(org.glassfish.grizzly.ssl.SSLEngineConfigurator)}.
     * @param start                 if set to false, server will not get started, this allows end users to set
     *                              additional properties on the underlying listener.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     * @see GrizzlyHttpContainer
     */
    public static HttpServer createHttpServer(final URI uri,
                                               final GrizzlyHttpContainer handler,
                                               final boolean secure,
                                               final SSLEngineConfigurator sslEngineConfigurator,
                                               final boolean start)
            throws ProcessingException {
        final String host = (uri.getHost() == null) ? NetworkListener.DEFAULT_NETWORK_HOST
                : uri.getHost();
        final int port = (uri.getPort() == -1) ? 80 : uri.getPort();
        final HttpServer server = new HttpServer();
        final NetworkListener listener = new NetworkListener("grizzly", host, port);
        listener.setSecure(secure);
        if (sslEngineConfigurator != null) {
            listener.setSSLEngineConfig(sslEngineConfigurator);
        }

        server.addListener(listener);

        // Map the path to the processor.
        final ServerConfiguration config = server.getServerConfiguration();
        if (handler != null) {
            config.addHttpHandler(handler, uri.getPath());
        }

        config.setPassTraceRequest(true);

        if (start) {
            try {
                // Start the server.
                server.start();
            } catch (IOException ex) {
                throw new ProcessingException("IOException thrown when trying to start grizzly server", ex);
            }
        }

        return server;
    }

    /**
     * Prevents instantiation.
     */
    private GrizzlyHttpServerFactory() {
    }
}
