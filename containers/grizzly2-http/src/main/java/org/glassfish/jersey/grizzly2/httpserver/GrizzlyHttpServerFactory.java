/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;

/**
 * Factory for creating Grizzly Http Server.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 *
 * @see HttpServer
 * @see GrizzlyHttpContainer
 */
public class GrizzlyHttpServerFactory {

    /**
     * Creates HttpServer instance.
     *
     * @param uri URI on which the Jersey web application will be deployed.
     * @param configuration web application configuration.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri, final ResourceConfig configuration) throws ProcessingException {
        return createHttpServer(uri, ContainerFactory.createContainer(GrizzlyHttpContainer.class, configuration), false, null);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri URI on which the Jersey web application will be deployed.
     * @param appHandler web application handler.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri, final ApplicationHandler appHandler) throws ProcessingException {
        return createHttpServer(uri, new GrizzlyHttpContainer(appHandler), false, null);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri uri on which the {@link ApplicationHandler} will be deployed.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri) throws ProcessingException {
        return createHttpServer(uri, (GrizzlyHttpContainer) null, false, null);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri URI on which the Jersey web application will be deployed.
     * @param configuration web application configuration.
     * @param secure used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig(org.glassfish.grizzly.ssl.SSLEngineConfigurator)}.
     * @return newly created {@link HttpServer}.
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ResourceConfig configuration,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator) {
        return createHttpServer(uri, ContainerFactory.createContainer(GrizzlyHttpContainer.class, configuration), secure, sslEngineConfigurator);
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri URI on which the Jersey web application will be deployed.
     * @param appHandler web application handler.
     * @param secure used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig(org.glassfish.grizzly.ssl.SSLEngineConfigurator)}.
     * @return newly created {@link HttpServer}.
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ApplicationHandler appHandler,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator) {
        return createHttpServer(uri, new GrizzlyHttpContainer(appHandler), secure, sslEngineConfigurator);
    }



    /**
     * Creates HttpServer instance.
     *
     * @param uri uri on which the {@link ApplicationHandler} will be deployed.
     * @param handler {@link HttpHandler} instance.
     * @param secure used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig(org.glassfish.grizzly.ssl.SSLEngineConfigurator)}.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     *
     * @see GrizzlyHttpContainer
     */
    private static HttpServer createHttpServer(final URI uri,
                                               final HttpHandler handler,
                                               final boolean secure,
                                               final SSLEngineConfigurator sslEngineConfigurator)
            throws ProcessingException {
        final String host = (uri.getHost() == null) ? NetworkListener.DEFAULT_NETWORK_HOST
                : uri.getHost();
        final int port = (uri.getPort() == -1) ? 80 : uri.getPort();
        final HttpServer server = new HttpServer();
        final NetworkListener listener = new NetworkListener("grizzly", host, port);
        listener.setSecure(secure);
        if(sslEngineConfigurator != null) {
            listener.setSSLEngineConfig(sslEngineConfigurator);
        }

        server.addListener(listener);

        // Map the path to the processor.
        final ServerConfiguration config = server.getServerConfiguration();
        if (handler != null) {
            config.addHttpHandler(handler, uri.getPath());
        }
        try {
            // Start the server.
            server.start();
        } catch (IOException ex) {
            throw new ProcessingException("IOException thrown when trying to start grizzly server", ex);
        }

        return server;
    }

    /**
     * Prevents instantiation.
     */
    private GrizzlyHttpServerFactory() {
    }
}
