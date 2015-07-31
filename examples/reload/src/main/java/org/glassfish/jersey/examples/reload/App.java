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
package org.glassfish.jersey.examples.reload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import org.glassfish.grizzly.http.server.HttpServer;

/**
 * Reload example application.
 * <p/>
 * A {@link ContainerLifecycleListener container listener} gets registered
 * with the application. Upon application startup notification, the listener schedules
 * a new {@link TimerTask timer task} to check a text file called {@code resources}
 * every 2 seconds. When the text file change is detected, the application gets reloaded with
 * a new {@link ResourceConfig resource configuration} including all
 * resource classes listed in that file.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class App {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final URI BASE_URI = URI.create("http://localhost:8080/flights/");
    public static final String ROOT_PATH = "arrivals";
    public static final String CONFIG_FILENAME = "resources";
    public static final long REFRESH_PERIOD_MS = 2000;

    static Container container;

    // @todo Java SE 7 - use java.nio.file.WatchService
    static class FileCheckTask extends TimerTask {

        long lastModified;

        FileCheckTask(final long lastModified) {
            this.lastModified = lastModified;
        }

        @Override
        public void run() {
            final File configFile = new File(CONFIG_FILENAME);
            final long actualLastModified = configFile.lastModified();
            if (lastModified < actualLastModified) {
                lastModified = actualLastModified;
                reloadApp(configFile);
            }
        }

        private void reloadApp(final File configFile) {
            LOGGER.info("Reloading resource classes:");
            final ResourceConfig rc = new ResourceConfig();

            try {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), "UTF-8"))) {
                    while (r.ready()) {
                        final String className = r.readLine();
                        if (!className.startsWith("#")) {
                            try {
                                rc.registerClasses(Class.forName(className));
                                LOGGER.info(String.format(" + loaded class %s.\n", className));
                            } catch (final ClassNotFoundException ex) {
                                LOGGER.info(String.format(" ! class %s not found.\n", className));
                            }
                        } else {
                            LOGGER.info(String.format(" - ignored class %s\n", className.substring(1)));
                        }
                    }
                }
            } catch (final Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
            App.container.reload(rc);
        }

    }

    public static void main(final String[] args) {
        try {
            LOGGER.info("Resource Config Reload Jersey Example App");

            final ResourceConfig resourceConfig = new ResourceConfig(ArrivalsResource.class);
            resourceConfig.registerInstances(new ContainerLifecycleListener() {
                @Override
                public void onStartup(final Container container) {
                    App.container = container;
                    final Timer t = new Timer(true);
                    t.scheduleAtFixedRate(new FileCheckTask(0), 0, REFRESH_PERIOD_MS);
                }

                @Override
                public void onReload(final Container container) {
                    System.out.println("Application has been reloaded!");
                }

                @Override
                public void onShutdown(final Container container) {
                    // ignore
                }
            });

            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, resourceConfig, true);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    server.shutdownNow();
                }
            }));
            server.start();

            System.out.println(
                    String.format("Application started.\nTry out %s%s\nStop the application using CTRL+C", BASE_URI, ROOT_PATH));

            Thread.currentThread().join();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
