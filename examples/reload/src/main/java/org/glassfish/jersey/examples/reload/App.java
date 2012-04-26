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
package org.glassfish.jersey.examples.reload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
 *
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
            File configFile = new File(CONFIG_FILENAME);
            final long actualLastModified = configFile.lastModified();
            if (lastModified < actualLastModified) {
                lastModified = actualLastModified;
                reloadApp(configFile);
            }
        }

        private void reloadApp(File configFile) {
            System.out.println("Reloading resource classes:");
            ResourceConfig rc = new ResourceConfig();

            // @todo Java SE 7 - use try-with-resources
            try {
                BufferedReader r = new BufferedReader(new FileReader(configFile));
                try {
                    while (r.ready()) {
                        final String className = r.readLine();
                        if (!className.startsWith("#")) {
                            try {
                                rc.addClasses(Class.forName(className));
                                System.out.printf(" - added class %s.\n", className);
                            } catch (ClassNotFoundException ex) {
                                System.out.printf(" - class %s not found.\n", className);
                            }
                        } else {
                            System.out.printf(" - ignored class %s\n", className.substring(1));
                        }
                    }
                } finally {
                    r.close();
                }
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
            App.container.reload(rc);
        }

    }

    public static void main(String[] args) {
        try {
            System.out.println("Resource Config Reload Jersey Example App");

            final ResourceConfig resourceConfig = new ResourceConfig(ArrivalsResource.class);
            resourceConfig.addSingletons(new ContainerLifecycleListener(){
                @Override
                public void onStartup(Container container) {
                    App.container = container;
                    Timer t = new Timer(true);
                    t.scheduleAtFixedRate(new FileCheckTask(0), 0, REFRESH_PERIOD_MS);
                }

                @Override
                public void onReload(Container container) {
                    System.out.println("Application has been reloaded!");
                }

                @Override
                public void onShutdown(Container container) {
                    // ignore
                }
            });

            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, resourceConfig);

            System.out.println(String.format("Application started.\nTry out %s%s\nHit enter to stop it...",
                    BASE_URI, ROOT_PATH));
            System.in.read();
            server.stop();
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
