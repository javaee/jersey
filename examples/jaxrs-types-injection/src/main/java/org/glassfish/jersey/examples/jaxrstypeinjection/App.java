/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.jaxrstypeinjection;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

import org.glassfish.grizzly.http.server.HttpServer;

/**
 * Jersey application that demonstrates injection of JAX-RS components into resources.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class App {

    private static final URI BASE_URI = URI.create("http://localhost:8080/jaxrs-type-injection/");
    /**
     * Programmatic root resource path template.
     */
    public static final String ROOT_PATH_PROGRAMMATIC = "programmatic/{p1}/{p2}";
    /**
     * Annotated class-based root resource path template demonstrating instance field injection.
     */
    public static final String ROOT_PATH_ANNOTATED_INSTANCE = "annotated/instance/{p1}/{p2}";
    /**
     * Annotated class-based root resource path template demonstrating method injection.
     */
    public static final String ROOT_PATH_ANNOTATED_METHOD = "annotated/method/{p1}/{p2}";

    /**
     * Main application entry point.
     *
     * @param args application arguments.
     */
    public static void main(String[] args) {
        try {
            System.out.println("JAX-RS Type Injection Jersey Example App");

            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, create(), false);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    server.shutdownNow();
                }
            }));
            server.start();

            System.out.println(String.format(
                    "Application started.%n"
                            + "To test injection into a programmatic resource, try out:%n  %s%s%s%n"
                            + "To test instance injection into an annotated resource, try out:%n  %s%s%s%n"
                            + "To test method injection into an annotated resource, try out:%n  %s%s%s%n"
                            + "Stop the application using CTRL+C",
                    BASE_URI, ROOT_PATH_PROGRAMMATIC, "?q1=<value_1>&q2=<value_2>&q2=<value_3>",
                    BASE_URI, ROOT_PATH_ANNOTATED_INSTANCE, "?q1=<value_1>&q2=<value_2>&q2=<value_3>",
                    BASE_URI, ROOT_PATH_ANNOTATED_METHOD, "?q1=<value_1>&q2=<value_2>&q2=<value_3>"));

            Thread.currentThread().join();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Create example application resource configuration.
     *
     * @return initialized resource configuration of the example application.
     */
    public static ResourceConfig create() {
        final ResourceConfig resourceConfig = new ResourceConfig(JaxrsInjectionReportingResource.class);
        final Resource.Builder resourceBuilder = Resource.builder(ROOT_PATH_PROGRAMMATIC);
        resourceBuilder.addMethod("GET").handledBy(JaxrsInjectionReportingInflector.class);

        return resourceConfig.registerResources(resourceBuilder.build());
    }
}
