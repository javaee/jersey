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
package org.glassfish.jersey.examples.clipboard;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.JerseyApplication;

import org.glassfish.grizzly.http.server.HttpServer;

/**
 * Hello world!
 *
 */
public class App {

    private static final URI BASE_URI = URI.create("http://localhost:8080/base/");
    public static final String ROOT_PATH = "clipboard";

    public static void main(String[] args) {
        try {
            System.out.println("Clipboard Jersey Example App");

            final JerseyApplication app = createProgrammaticClipboardApp();
            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, app);

            System.out.println(
                    String.format("Application started.%n"
                    + "Try out %s%s%n"
                    + "Hit enter to stop it...",
                    BASE_URI, ROOT_PATH));
            System.in.read();
            server.stop();
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static JerseyApplication createProgrammaticClipboardApp() {
        final JerseyApplication.Builder appFactory = JerseyApplication.builder();
        final Clipboard clipboard = new Clipboard();

        appFactory.bind(ROOT_PATH)

                .method("GET").to(new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        final String content = clipboard.content();
                        if (content.isEmpty()) {
                            return Response.noContent().build();
                        }
                        return Response.ok(content).build();
                    }
                })

                .method("PUT").to(new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        if (data != null) {
                            clipboard.setContent(data.readEntity(String.class));
                        }
                        return Response.noContent().build();
                    }
                })

                .method("POST").to(new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        String newContent = (data != null) ? clipboard.append(data.readEntity(String.class)) : "";
                        return Response.ok(newContent).build();
                    }
                })

                .method("DELETE").to(new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        clipboard.clear();
                        return Response.noContent().build();
                    }
                });


        return appFactory.build();
    }
}
