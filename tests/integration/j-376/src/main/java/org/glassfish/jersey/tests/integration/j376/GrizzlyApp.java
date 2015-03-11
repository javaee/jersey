/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.j376;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.jboss.weld.environment.se.Weld;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CDI Test App launcher. Starts the Grizzly server and initializes weld.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class GrizzlyApp {

    private static Weld weld;
    private static HttpServer server;

    private static final URI BASE_URI = URI.create("http://localhost:8080/j376/");

    public static void main(String[] args) {
        try {
            System.out.println("Jersey CDI Test App");

            start();

            System.out.println(String.format("Application started.\nTry out %s%s\nHit enter to stop it...",
                    BASE_URI, "application.wadl"));
            System.in.read();
            stop();
        } catch (IOException ex) {
            Logger.getLogger(GrizzlyApp.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    protected static void stop() {
        server.shutdownNow();
        weld.shutdown();
    }

    protected static void start() {
        weld = new Weld();
        weld.initialize();

        server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, createJaxRsApp(), true);
    }

    public static URI getBaseUri() {
        return BASE_URI;
    }

    public static ResourceConfig createJaxRsApp() {
        return new ResourceConfig(new MyApplication().getClasses());
    }
}