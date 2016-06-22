/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.server.async;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import static org.glassfish.jersey.logging.LoggingFeature.Verbosity.PAYLOAD_ANY;

/**
 * Jersey example application for custom executors managed async resources.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class App {

    private static final URI BASE_URI = URI.create("http://localhost:8080/base/");
    public static final String ASYNC_MESSAGING_FIRE_N_FORGET_PATH = "async/messaging/fireAndForget";
    public static final String ASYNC_MESSAGING_BLOCKING_PATH = "async/messaging/blocking";
    public static final String ASYNC_LONG_RUNNING_OP_PATH = "async/longrunning";

    public static void main(String[] args) {
        try {
            System.out.println("\"Async resources\" Jersey Example App");

            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, create(), false);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    server.shutdownNow();
                }
            }));
            server.start();

            System.out.println(String.format(
                    "Application started.\n"
                            + "To test simple, non-blocking asynchronous messaging resource, try %s%s\n"
                            + "To test blocking version of asynchronous messaging resource, try %s%s\n"
                            + "To test long-running asynchronous operation resource, try %s%s\n"
                            + "Stop the application using CTRL+C",
                    BASE_URI, ASYNC_MESSAGING_FIRE_N_FORGET_PATH,
                    BASE_URI, ASYNC_MESSAGING_BLOCKING_PATH,
                    BASE_URI, ASYNC_LONG_RUNNING_OP_PATH));

            Thread.currentThread().join();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static ResourceConfig create() {
        final ResourceConfig resourceConfig = new ResourceConfig()
                .registerClasses(BlockingPostChatResource.class, FireAndForgetChatResource.class, SimpleLongRunningResource.class)
                .registerInstances(new LoggingFeature(Logger.getLogger(App.class.getName()), PAYLOAD_ANY));

        return resourceConfig;
    }
}
