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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.httpsclientservergrizzly;

import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

/**
 * A simple SSL-secured HTTP server.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class Server {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private static final String KEYSTORE_SERVER_FILE = "./keystore_server";
    private static final String KEYSTORE_SERVER_PWD = "asdfgh";
    private static final String TRUSTORE_SERVER_FILE = "./truststore_server";
    private static final String TRUSTORE_SERVER_PWD = "asdfgh";

    public static final URI BASE_URI = getBaseURI();
    public static final String CONTENT = "JERSEY HTTPS EXAMPLE\n";

    private final HttpServer webServer;

    private Server(final HttpServer webServer) {
        this.webServer = webServer;
    }

    private static URI getBaseURI() {
        return UriBuilder.fromUri("https://localhost/").port(getPort(8463)).build();
    }

    private static int getPort(int defaultPort) {
        final String port =
                AccessController.doPrivileged(PropertiesHelper.getSystemProperty("jersey.config.test.container.port"));
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                LOGGER.warning("Value of jersey.config.test.container.port property"
                        + " is not a valid positive integer [" + port + "]."
                        + " Reverting to default [" + defaultPort + "].");
            }
        }
        return defaultPort;
    }

    /**
     * Start SSL-secured HTTP test server.
     *
     * @throws IOException in case there is an error while reading server key store or trust store.
     * @return an instance of the started SSL-secured HTTP test server.
     */
    public static Server start() throws IOException {
        // Grizzly ssl configuration
        SSLContextConfigurator sslContext = new SSLContextConfigurator();

        // set up security context
        sslContext.setKeyStoreFile(KEYSTORE_SERVER_FILE); // contains server keypair
        sslContext.setKeyStorePass(KEYSTORE_SERVER_PWD);
        sslContext.setTrustStoreFile(TRUSTORE_SERVER_FILE); // contains client certificate
        sslContext.setTrustStorePass(TRUSTORE_SERVER_PWD);

        ResourceConfig rc = new ResourceConfig();
        rc.registerClasses(RootResource.class, SecurityFilter.class, AuthenticationExceptionMapper.class);

        final HttpServer grizzlyServer = GrizzlyHttpServerFactory.createHttpServer(
                getBaseURI(),
                rc,
                true,
                new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(true)
        );

        // start Grizzly embedded server //
        LOGGER.info("Jersey app started. Try out " + BASE_URI + "\nHit CTRL + C to stop it...");
        grizzlyServer.start();

        return new Server(grizzlyServer);
    }

    /**
     * Stop SSL-secured HTTP test server.
     */
    public void stop() {
        webServer.shutdownNow();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws InterruptedException, IOException {
        start();

        System.in.read();
    }
}
