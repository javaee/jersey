/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jdkhttp;

import java.net.URI;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;

/**
 * Abstract JDK HTTP Server unit tester.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractJdkHttpServerTester {

    public static final String CONTEXT = "";
    private final int DEFAULT_PORT = 9998;

    private static final Logger LOGGER = Logger.getLogger(AbstractJdkHttpServerTester.class.getName());

    /**
     * Get the port to be used for test application deployments.
     *
     * @return The HTTP port of the URI
     */
    protected final int getPort() {
        final String value =
                AccessController.doPrivileged(PropertiesHelper.getSystemProperty("jersey.config.test.container.port"));
        if (value != null) {

            try {
                final int i = Integer.parseInt(value);
                if (i <= 0) {
                    throw new NumberFormatException("Value not positive.");
                }
                return i;
            } catch (NumberFormatException e) {
                LOGGER.log(Level.CONFIG,
                        "Value of 'jersey.config.test.container.port'"
                                + " property is not a valid positive integer [" + value + "]."
                                + " Reverting to default [" + DEFAULT_PORT + "].",
                        e);
            }
        }
        return DEFAULT_PORT;
    }

    private volatile HttpServer server;

    public UriBuilder getUri() {
        return UriBuilder.fromUri("http://localhost").port(getPort()).path(CONTEXT);
    }

    public void startServer(Class... resources) {
        ResourceConfig config = new ResourceConfig(resources);
        config.register(LoggingFeature.class);
        final URI baseUri = getBaseUri();
        server = JdkHttpServerFactory.createHttpServer(baseUri, config);
        LOGGER.log(Level.INFO, "jdk-http server started on base uri: " + baseUri);
    }

    public void startServer(ResourceConfig config) {
        final URI baseUri = getBaseUri();
        config.register(LoggingFeature.class);
        server = JdkHttpServerFactory.createHttpServer(baseUri, config);
        LOGGER.log(Level.INFO, "jdk-http server started on base uri: " + baseUri);
    }

    public URI getBaseUri() {
        return UriBuilder.fromUri("http://localhost/").port(getPort()).build();
    }

    public void stopServer() {
        try {
            server.stop(3);
            server = null;
            LOGGER.log(Level.INFO, "Simple-http server stopped.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void tearDown() {
        if (server != null) {
            stopServer();
        }
    }
}
