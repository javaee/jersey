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

package org.glassfish.jersey.examples.httptrace;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

import org.glassfish.grizzly.http.server.HttpServer;

/**
 * This is the example entry point, where Jersey application gets populated and published
 * using the Grizzly 2 HTTP container.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class App {

    private static final URI BASE_URI = URI.create("http://localhost:9998/base/");
    /**
     * Programmatic tracing root resource path.
     */
    public static final String ROOT_PATH_PROGRAMMATIC = "tracing/programmatic";
    /**
     * Annotated class-based tracing root resource path.
     */
    public static final String ROOT_PATH_ANNOTATED = "tracing/annotated";

    /**
     * Main application entry point.
     *
     * @param args application arguments.
     */
    public static void main(String[] args) {
        try {
            System.out.println("HTTP TRACE Support Jersey Example App");

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
                            + "To test TRACE with a programmatic resource, send HTTP TRACE request to:%n  %s%s%n"
                            + "To test TRACE with an annotated resource, send HTTP TRACE request to:%n  %s%s%n"
                            + "Stop the application using CTRL+C",
                    BASE_URI, ROOT_PATH_PROGRAMMATIC,
                    BASE_URI, ROOT_PATH_ANNOTATED));

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
        final ResourceConfig resourceConfig = new ResourceConfig(TracingResource.class);

        final Resource.Builder resourceBuilder = Resource.builder(ROOT_PATH_PROGRAMMATIC);
        resourceBuilder.addMethod(TRACE.NAME).handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                if (request == null) {
                    return Response.noContent().build();
                } else {
                    return Response.ok(Stringifier.stringify((ContainerRequest) request), MediaType.TEXT_PLAIN).build();
                }
            }
        });

        return resourceConfig.registerResources(resourceBuilder.build());
    }
}
