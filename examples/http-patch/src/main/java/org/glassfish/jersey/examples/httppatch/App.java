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

package org.glassfish.jersey.examples.httppatch;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ext.ContextResolver;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.glassfish.grizzly.http.server.HttpServer;

/**
 * HTTP PATCH Demo Application.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class App {
    /**
     * Prevents instantiation.
     */
    private App() {
    }

    private static final URI BASE_URI = URI.create("http://localhost:8080/http-patch");
    /**
     * Root resource path.
     */
    static final String ROOT_PATH = "patchable-state";

    public static void main(String[] args) {
        try {
            System.out.println("Jersey HTTP PATCH Support Example App");

            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, create(), false);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    server.shutdownNow();
                }
            }));
            server.start();

            System.out.println(String.format("Application started.\nTry out %s/%s\nStop the application using CTRL+C",
                    BASE_URI,
                    ROOT_PATH));

            Thread.currentThread().join();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Create JAX-RS application for the example.
     *
     * @return created application instance.
     */
    public static ResourceConfig create() {
        final ResourceConfig app = new ResourceConfig(
                PatchableResource.class,
                OptionsAcceptPatchHeaderFilter.class,
                PatchingInterceptor.class);

        app.register(createMoxyJsonResolver());

        // Enable on-demand tracing
        app.property(ServerProperties.TRACING, "ON_DEMAND");

        return app;
    }

    /**
     * Create {@link javax.ws.rs.ext.ContextResolver} for {@link org.glassfish.jersey.moxy.json.MoxyJsonConfig}
     * for this application.
     *
     * @return {@code MoxyJsonConfig} context resolver.
     */
    public static ContextResolver<MoxyJsonConfig> createMoxyJsonResolver() {
        final MoxyJsonConfig moxyJsonConfig = new MoxyJsonConfig()
                .setFormattedOutput(true)
                .setNamespaceSeparator(':');
        return moxyJsonConfig.resolver();
    }
}
