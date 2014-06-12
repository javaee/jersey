/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.performance.monitoring;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.*;
import org.glassfish.jersey.server.model.Resource;

/**
 * Application class to start performance test web service at http://localhost:8080/ if the base URI
 * is not passed via the first command line argument.
 */
public class JerseyApp {

    private static final URI BASE_URI = URI.create("http://localhost:8080/");
    public static final String ROOT_PATH = "text";

    public static void main(final String[] args) throws Exception {
            System.out.println("Jersey performance test web service application");

            final URI baseUri = args.length > 0 ? URI.create(args[0]) : BASE_URI;
            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, createResourceConfig());

            System.out.println(String.format("Application started.\nTry out %s%s\nHit Ctrl-C to stop it...",
                    baseUri, ROOT_PATH));

            while (server.isStarted()) {
                Thread.sleep(600000);
            }
    }

    private static ResourceConfig createResourceConfig() {
        final Set<Resource> resources = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            resources.add(Resource.builder(org.glassfish.jersey.tests.performance.monitoring.Resource.class)
                    .path("" + i)
                    .build());
        }

        return new ResourceConfig()
                .property(ServerProperties.MONITORING_STATISTICS_ENABLED, true)
                .property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true)
                .registerResources(resources);
    }
}
