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
package org.glassfish.jersey.jetty.connector.ssl;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class Server {

    private static org.eclipse.jetty.server.Server webServer;

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    public static final URI BASE_URI = getBaseURI();
    public static final String CONTENT = "JERSEY HTTPS EXAMPLE\n";

    private static URI getBaseURI() {
        return UriBuilder.fromUri("https://localhost/").port(getPort(4463)).build();
    }

    private static int getPort(int defaultPort) {
        final String port = System.getProperty("jersey.config.test.container.port");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                System.out.println("Value of jersey.config.test.container.port property" +
                        " is not a valid positive integer [" + port + "]." +
                        " Reverting to default [" + defaultPort + "].");
            }
        }
        return defaultPort;
    }

    protected static void startServer() throws IOException {
        final URL trustStore = Server.class.getResource("/truststore_server");
        final URL keyStore = Server.class.getResource("/keystore_server");

        // Jetty ssl context factory
        SslContextFactory sslContextFactory = new SslContextFactory();

        // set up security context
        sslContextFactory.setKeyStoreResource(Resource.newResource(keyStore));  // contains server keypair
        sslContextFactory.setKeyStorePassword("asdfgh");
        sslContextFactory.setTrustStoreResource(Resource.newResource(trustStore)); // contains client certificate
        sslContextFactory.setTrustStorePassword("asdfgh");

        ResourceConfig rc = new ResourceConfig();
        rc.register(new LoggingFilter(LOGGER, true));
        rc.registerClasses(RootResource.class, SecurityFilter.class, AuthenticationExceptionMapper.class);

        try {
            webServer = JettyHttpContainerFactory.create(
                    getBaseURI(),
                    sslContextFactory,
                    rc
            );

            // Jetty embedded server started //
            System.out.println("Jersey app started. Try out " + BASE_URI + "\nHit CTRL + C to stop it...");

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    protected static void stopServer() throws Exception {
        if (webServer != null)
            webServer.stop();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws InterruptedException, IOException {
        startServer();

        System.in.read();
    }
}