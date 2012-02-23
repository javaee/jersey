/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.httptrace;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the example entry point, where Jersey application gets populated and published
 * using the Grizzly 2 HTTP container.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class App {

    public static final URI BASE_URI = URI.create("http://localhost:9998/base/");
    public static final String ROOT_PATH_PROGRAMMATIC = "tracing/programmatic";
    public static final String ROOT_PATH_ANNOTATED = "tracing/annotated";

    public static void main(String[] args) {
        try {
            System.out.println("HTTP TRACE Support Jersey Example App");

            final Application app = create();
            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, app);

            System.out.println(String.format(
                    "Application started.\n"
                    + "To test TRACE with a programmatic resource, send HTTP TRACE request to:%n  %s%s%n"
                    + "To test TRACE with an annotated resource, send HTTP TRACE request to:%n  %s%s%n"
                    + "Hit enter to stop it...",
                    BASE_URI, ROOT_PATH_PROGRAMMATIC,
                    BASE_URI, ROOT_PATH_ANNOTATED));
            System.in.read();
            server.stop();
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Application create() {
        final ResourceConfig resourceConfig = ResourceConfig.builder().addClasses(TracingResource.class).build();

        final Application.Builder appBuilder = Application.builder(resourceConfig);

        appBuilder.bind(ROOT_PATH_PROGRAMMATIC).method(TRACE.NAME).to(new Inflector<Request, Response>() {

            @Override
            public Response apply(Request request) {
                if (request == null) {
                    return Response.noContent().build();
                } else {
                    return Response.ok(Stringifier.stringify(request), MediaType.TEXT_PLAIN).build();
                }
            }
        });

        return appBuilder.build();
    }
}
