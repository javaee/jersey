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
package org.glassfish.jersey.apache.connector;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.logging.Logger;

/**
 * @author Paul.Sandoz@Sun.Com
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public abstract class AbstractGrizzlyServerTester {

    protected static final Logger LOGGER = Logger.getLogger(AbstractGrizzlyServerTester.class.getName());

    public static final String CONTEXT = "";

    private HttpServer httpServer;

    private int port = getEnvVariable("JERSEY_HTTP_PORT", 9997);

    private static int getEnvVariable(final String varName, int defaultValue) {
        if (null == varName) {
            return defaultValue;
        }
        String varValue = System.getenv(varName);
        if (null != varValue) {
            try {
                return Integer.parseInt(varValue);
            } catch (NumberFormatException e) {
                // will return default value bellow
            }
        }
        return defaultValue;
    }

    public UriBuilder getUri() {
        return UriBuilder.fromUri("http://localhost").port(port).path(CONTEXT);
    }

    public void startServer(Class... resources) {
        ResourceConfig config = new ResourceConfig(resources);
        config.register(new LoggingFilter(LOGGER, true));

        httpServer = GrizzlyHttpServerFactory.createHttpServer(getBaseUri(), config);
    }

    public void startServer(ResourceConfig config) {
        config.register(new LoggingFilter(LOGGER, true));
        httpServer = GrizzlyHttpServerFactory.createHttpServer(getBaseUri(), config);
    }

    public URI getBaseUri() {
        return UriBuilder.fromUri("http://localhost/").port(port).build();
    }

    private void start(ResourceConfig config) {
        if (httpServer != null && httpServer.isStarted()) {
            stopServer();
        }

        System.out.println("Starting GrizzlyServer port number = " + port);

        URI u = UriBuilder.fromUri("http://localhost").port(port).build();
        try {
            httpServer = GrizzlyHttpServerFactory.createHttpServer(u, config);
        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Started GrizzlyServer");

        int timeToSleep = getEnvVariable("JERSEY_HTTP_SLEEP", 0);
        if (timeToSleep > 0) {
            System.out.println("Sleeping for " + timeToSleep + " ms");
            try {
                // Wait for the server to start
                Thread.sleep(timeToSleep);
            } catch (InterruptedException ex) {
                System.out.println("Sleeping interrupted: " + ex.getLocalizedMessage());
            }
        }
    }

    public void stopServer() {
        if (httpServer.isStarted()) {
            httpServer.stop();
        }
    }

    @After
    public void tearDown() {
        stopServer();
    }
}
