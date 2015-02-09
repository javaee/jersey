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
package org.glassfish.jersey.tests.e2e.client.connector.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

import com.google.common.io.ByteStreams;

/**
 * A simple SSL-secured HTTP server for testing purposes.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
final class Server {

    private static final String SERVER_TRUST_STORE = "truststore_server";
    private static final String SERVER_KEY_STORE = "keystore_server";
    private static HttpServer webServer;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    /**
     * Base server URI.
     */
    public static final URI BASE_URI = getBaseURI();

    private Server() {
        throw new AssertionError("Instantiation not allowed.");
    }

    private static URI getBaseURI() {
        return UriBuilder.fromUri("https://localhost/").port(getPort(4463)).build();
    }

    private static int getPort(int defaultPort) {
        final String port = System.getProperty("jersey.config.test.container.port");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                LOGGER.warning("Value of jersey.config.test.container.port property" +
                        " is not a valid positive integer [" + port + "]." +
                        " Reverting to default [" + defaultPort + "].");
            }
        }
        return defaultPort;
    }

    /**
     * Start SSL-secured HTTP test server.
     *
     * @throws IOException in case there is an error while reading server key store or trust store.
     */
    static void startServer() throws IOException {
        final InputStream trustStore = Server.class.getResourceAsStream(SERVER_TRUST_STORE);
        final InputStream keyStore = Server.class.getResourceAsStream(SERVER_KEY_STORE);

        // Grizzly ssl configuration
        SSLContextConfigurator sslContext = new SSLContextConfigurator();

        // set up security context
        sslContext.setKeyStoreBytes(ByteStreams.toByteArray(keyStore));  // contains server key pair
        sslContext.setKeyStorePass("asdfgh");
        sslContext.setTrustStoreBytes(ByteStreams.toByteArray(trustStore)); // contains client certificate
        sslContext.setTrustStorePass("asdfgh");

        ResourceConfig rc = new ResourceConfig();
        rc.register(new LoggingFilter(LOGGER, true));
        rc.registerClasses(RootResource.class, SecurityFilter.class, AuthenticationExceptionMapper.class);

        try {
            webServer = GrizzlyHttpServerFactory.createHttpServer(
                    getBaseURI(),
                    rc,
                    true,
                    new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(true)
            );

            // start Grizzly embedded server //
            LOGGER.info("Jersey app started. Try out " + BASE_URI + "\nHit CTRL + C to stop it...");
            webServer.start();

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while starting the server.", ex);
        }
    }

    /**
     * Stop SSL-secured HTTP test server.
     */
    static void stopServer() {
        webServer.shutdownNow();
    }
}
