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
package org.glassfish.jersey.examples.jaxrstypeinjection;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application that demonstrates injection of JAX-RS components into resources.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class App {

    public static final URI BASE_URI = URI.create("http://localhost:8080/jaxrs-type-injection/");
    public static final String ROOT_PATH_PROGRAMMATIC = "programmatic/{p1}/{p2}";
    public static final String ROOT_PATH_ANNOTATED_INSTANCE = "annotated/instance/{p1}/{p2}";
    public static final String ROOT_PATH_ANNOTATED_METHOD = "annotated/method/{p1}/{p2}";

    public static void main(String[] args) {
        try {
            System.out.println("JAX-RS Type Injection Jersey Example App");

            final Application app = create();
            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, app);

            System.out.println(String.format(
                    "Application started.%n"
                    + "To test injection into a programmatic resource, try out:%n  %s%s%s%n"
                    + "To test instance injection into an annotated resource, try out:%n  %s%s%s%n"
                    + "To test method injection into an annotated resource, try out:%n  %s%s%s%n"
                    + "Hit enter to stop it...",
                    BASE_URI, ROOT_PATH_PROGRAMMATIC, "?q1=<value_1>&q2=<value_2>&q2=<value_3>",
                    BASE_URI, ROOT_PATH_ANNOTATED_INSTANCE, "?q1=<value_1>&q2=<value_2>&q2=<value_3>",
                    BASE_URI, ROOT_PATH_ANNOTATED_METHOD, "?q1=<value_1>&q2=<value_2>&q2=<value_3>"));
            System.in.read();
            server.stop();
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Application create() {
        final ResourceConfig resourceConfig = ResourceConfig.builder().addClasses(JaxrsInjectionReportingResource.class).build();
        final Application.Builder appBuilder = Application.builder(resourceConfig);

        appBuilder.bind(ROOT_PATH_PROGRAMMATIC).method("GET").to(JaxrsInjectionReportingInflector.class);

        return appBuilder.build();
    }
}
