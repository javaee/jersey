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
package org.glassfish.jersey.grizzly2;

import java.io.IOException;
import java.net.URI;

import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;

/**
 * TODO javadoc.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
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
        return createHttpServer(uri, ContainerFactory.createContainer(GrizzlyHttpContainer.class, configuration));
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
        return createHttpServer(uri, new GrizzlyHttpContainer(appHandler));
    }

    /**
     * Creates HttpServer instance.
     *
     * @param uri uri on which the {@link JerseyApplication} will be deployed.
     * @return newly created {@link HttpServer}.
     * @throws ProcessingException
     */
    public static HttpServer createHttpServer(final URI uri) throws ProcessingException {
        return createHttpServer(uri, (GrizzlyHttpContainer) null);
    }

    private static HttpServer createHttpServer(final URI u, final GrizzlyHttpContainer handler)
            throws ProcessingException {
        final String host = (u.getHost() == null) ? NetworkListener.DEFAULT_NETWORK_HOST
                : u.getHost();
        final int port = (u.getPort() == -1) ? 80 : u.getPort();
        final HttpServer server = new HttpServer();
        final NetworkListener listener = new NetworkListener("grizzly", host, port);

        server.addListener(listener);

        // Map the path to the processor.
        final ServerConfiguration config = server.getServerConfiguration();
        if (handler != null) {
            config.addHttpHandler(handler, u.getPath());
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
