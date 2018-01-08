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

package org.glassfish.jersey.examples.sparklines;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;

/**
 * @author Paul Sandoz
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class Main {

    public static final String WEB_ROOT = "/webroot/";
    public static final String APP_PATH = "/sparklines/";
    public static final int PORT = 9998;

    public static HttpServer startServer(String webRootPath) {
        final HttpServer server = new HttpServer();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.shutdownNow();
            }
        }));

        final NetworkListener listener = new NetworkListener("grizzly", "localhost", PORT);

        server.addListener(listener);

        final ServerConfiguration config = server.getServerConfiguration();
        // add handler for serving static content
        config.addHttpHandler(new CLStaticHttpHandler(Main.class.getClassLoader(), WEB_ROOT), APP_PATH);

        // add handler for serving JAX-RS resources
        config.addHttpHandler(RuntimeDelegate.getInstance().createEndpoint(createResourceConfig(), GrizzlyHttpContainer.class),
                APP_PATH);

        try {
            // Start the server.
            server.start();
        } catch (Exception ex) {
            throw new ProcessingException("Exception thrown when trying to start grizzly server", ex);
        }

        return server;
    }

    public static void main(String[] args) {

        try {
            final HttpServer server = startServer(args.length >= 1 ? args[0] : null);
            System.out.println(String.format("Application started.\n"
                            + "Access it at %s\n"
                            + "Stop the application using CTRL+C",
                    getAppUri()));

            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String getAppUri() {
        return String.format("http://localhost:%s%s", PORT, APP_PATH);
    }

    /**
     * Create Jersey server-side application resource configuration.
     *
     * @return Jersey server-side application configuration.
     */
    public static ResourceConfig createResourceConfig() {
        return new ResourceConfig().registerClasses(SparklinesResource.class);
    }
}
