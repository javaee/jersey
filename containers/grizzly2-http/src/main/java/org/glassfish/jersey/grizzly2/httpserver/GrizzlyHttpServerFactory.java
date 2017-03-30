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
package org.glassfish.jersey.grizzly2.httpserver;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.grizzly2.httpserver.internal.LocalizationMessages;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.utils.Charsets;

/**
 * Factory for creating Grizzly Http Server.
 * <p>
 * Should you need to fine tune the underlying Grizzly transport layer, you can obtain direct access to the corresponding
 * Grizzly structures with <tt>server.getListener("grizzly").getTransport()</tt>. To make certain options take effect,
 * you need to work with an inactive HttpServer instance (that is the one that has not been started yet).
 * To obtain such an instance, use one of the below factory methods with {@code start} parameter set to {@code false}.
 * When the {@code start} parameter is not present, the factory method returns an already started instance.
 * </p>
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see HttpServer
 * @see GrizzlyHttpContainer
 */
public final class GrizzlyHttpServerFactory {

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri uri on which the {@link ApplicationHandler} will be deployed. Only first path segment will be used as
     *            context path, the rest will be ignored.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     */
    public static HttpServer createHttpServer(final URI uri) {
        return createHttpServer(uri, (GrizzlyHttpContainer) null, false, null, true);
    }

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri   uri on which the {@link ApplicationHandler} will be deployed. Only first path segment will be used
     *              as context path, the rest will be ignored.
     * @param start if set to false, server will not get started, which allows to configure the underlying transport
     *              layer, see above for details.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     */
    public static HttpServer createHttpServer(final URI uri, final boolean start) {
        return createHttpServer(uri, (GrizzlyHttpContainer) null, false, null, start);
    }

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri           URI on which the Jersey web application will be deployed. Only first path segment will be
     *                      used as context path, the rest will be ignored.
     * @param configuration web application configuration.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     */
    public static HttpServer createHttpServer(final URI uri, final ResourceConfig configuration) {
        return createHttpServer(
                uri,
                new GrizzlyHttpContainer(configuration),
                false,
                null,
                true
        );
    }

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri           URI on which the Jersey web application will be deployed. Only first path segment will be
     *                      used as context path, the rest will be ignored.
     * @param configuration web application configuration.
     * @param start         if set to false, server will not get started, which allows to configure the underlying
     *                      transport layer, see above for details.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     */
    public static HttpServer createHttpServer(final URI uri, final ResourceConfig configuration, final boolean start) {
        return createHttpServer(
                uri,
                new GrizzlyHttpContainer(configuration),
                false,
                null,
                start);
    }

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri                   URI on which the Jersey web application will be deployed. Only first path segment
     *                              will be used as context path, the rest will be ignored.
     * @param configuration         web application configuration.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig}.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ResourceConfig configuration,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator) {
        return createHttpServer(
                uri,
                new GrizzlyHttpContainer(configuration),
                secure,
                sslEngineConfigurator,
                true);
    }

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri                   URI on which the Jersey web application will be deployed. Only first path segment
     *                              will be used as context path, the rest will be ignored.
     * @param configuration         web application configuration.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig}.
     * @param start                 if set to false, server will not get started, which allows to configure the
     *                              underlying transport, see above for details.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ResourceConfig configuration,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator,
                                              final boolean start) {
        return createHttpServer(
                uri,
                new GrizzlyHttpContainer(configuration),
                secure,
                sslEngineConfigurator,
                start);
    }

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri                   uri on which the {@link ApplicationHandler} will be deployed. Only first path
     *                              segment will be used as context path, the rest will be ignored.
     * @param config                web application configuration.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig}.
     * @param parentContext         DI provider specific context with application's registered bindings.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     * @see GrizzlyHttpContainer
     * @since 2.12
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ResourceConfig config,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator,
                                              final Object parentContext) {
        return createHttpServer(uri, new GrizzlyHttpContainer(config, parentContext), secure, sslEngineConfigurator,
                true);
    }

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri           uri on which the {@link ApplicationHandler} will be deployed. Only first path
     *                      segment will be used as context path, the rest will be ignored.
     * @param config        web application configuration.
     * @param parentContext DI provider specific context with application's registered bindings.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     * @see GrizzlyHttpContainer
     * @since 2.12
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final ResourceConfig config,
                                              final Object parentContext) {
        return createHttpServer(uri, new GrizzlyHttpContainer(config, parentContext), false, null, true);
    }

    /**
     * Create new {@link HttpServer} instance.
     *
     * @param uri                   uri on which the {@link ApplicationHandler} will be deployed. Only first path
     *                              segment will be used as context path, the rest will be ignored.
     * @param handler               {@link HttpHandler} instance.
     * @param secure                used for call {@link NetworkListener#setSecure(boolean)}.
     * @param sslEngineConfigurator Ssl settings to be passed to {@link NetworkListener#setSSLEngineConfig}.
     * @param start                 if set to false, server will not get started, this allows end users to set
     *                              additional properties on the underlying listener.
     * @return newly created {@code HttpServer}.
     * @throws ProcessingException in case of any failure when creating a new {@code HttpServer} instance.
     * @see GrizzlyHttpContainer
     */
    public static HttpServer createHttpServer(final URI uri,
                                              final GrizzlyHttpContainer handler,
                                              final boolean secure,
                                              final SSLEngineConfigurator sslEngineConfigurator,
                                              final boolean start) {

        final String host = (uri.getHost() == null) ? NetworkListener.DEFAULT_NETWORK_HOST : uri.getHost();
        final int port = (uri.getPort() == -1)
                ? (secure ? Container.DEFAULT_HTTPS_PORT : Container.DEFAULT_HTTP_PORT)
                : uri.getPort();

        final NetworkListener listener = new NetworkListener("grizzly", host, port);

        listener.getTransport().getWorkerThreadPoolConfig().setThreadFactory(new ThreadFactoryBuilder()
                .setNameFormat("grizzly-http-server-%d")
                .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                .build());

        listener.setSecure(secure);
        if (sslEngineConfigurator != null) {
            listener.setSSLEngineConfig(sslEngineConfigurator);
        }

        final HttpServer server = new HttpServer();
        server.addListener(listener);

        // Map the path to the processor.
        final ServerConfiguration config = server.getServerConfiguration();
        if (handler != null) {
            final String path = uri.getPath().replaceAll("/{2,}", "/");

            final String contextPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            config.addHttpHandler(handler, HttpHandlerRegistration.bulder().contextPath(contextPath).build());
        }

        config.setPassTraceRequest(true);
        config.setDefaultQueryEncoding(Charsets.UTF8_CHARSET);

        if (start) {
            try {
                // Start the server.
                server.start();
            } catch (final IOException ex) {
                server.shutdownNow();
                throw new ProcessingException(LocalizationMessages.FAILED_TO_START_SERVER(ex.getMessage()), ex);
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
