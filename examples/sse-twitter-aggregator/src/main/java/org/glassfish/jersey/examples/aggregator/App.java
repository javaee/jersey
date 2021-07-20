/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.aggregator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.util.HttpStatus;

/**
 * Jersey Twitter Aggregator example application.
 */
public class App {

    private static final Properties TWITTER_PROPERTIES = loadSettings();
    private static final String TWITTER_USER_NAME = "twitter.user.name";
    private static final String TWITTER_USER_PASSWORD = "twitter.user.password";
    private static final String TWITTER_PROPERTIES_FILE_NAME = "twitter-api.properties";

    private static final String APP_PATH = "/aggregator/";
    private static final String API_PATH = "/aggregator-api/";
    static final String WEB_ROOT = "/webroot";
    private static final int PORT = 8080;

    /**
     * Starts Grizzly HTTP server exposing static content, JAX-RS resources
     * and web sockets defined in this application.
     *
     * @param webRootPath static content root path.
     */
    private static void startServer(String webRootPath) {
        final HttpServer server = new HttpServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        final NetworkListener listener = new NetworkListener("grizzly", "localhost", PORT);

        server.addListener(listener);

        final ServerConfiguration config = server.getServerConfiguration();
        // add handler for serving static content
        config.addHttpHandler(new StaticContentHandler(webRootPath),
                APP_PATH);

        // add handler for serving JAX-RS resources
        config.addHttpHandler(RuntimeDelegate.getInstance().createEndpoint(createResourceConfig(), GrizzlyHttpContainer.class),
                API_PATH);

        try {
            // Start the server.
            server.start();
        } catch (Exception ex) {
            throw new ProcessingException("Exception thrown when trying to start grizzly server", ex);
        }
    }

    public static void main(String[] args) {
        MainWindow.main(args);

        try {
            System.out.println("\"SSE Twitter Message Aggregator\" Jersey Example App");
            startServer(args.length >= 1 ? args[0] : null);
            System.out.println(String.format("Application started.\n"
                            + "Access it at %s\n"
                            + "Stop the application using CTRL+C",
                    getAppUri()));

            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static String getApiUri() {
        return String.format("http://localhost:%s%s", PORT, API_PATH);
    }

    private static String getAppUri() {
        return String.format("http://localhost:%s%s", PORT, APP_PATH);
    }

    /**
     * Create Jersey server-side application resource configuration.
     *
     * @return Jersey server-side application configuration.
     */
    private static ResourceConfig createResourceConfig() {
        return new ResourceConfig()
                .registerClasses(MessageStreamResourceJersey.class,
                        MessageStreamResourceJaxRs.class,
                        SseFeature.class,
                        MoxyJsonFeature.class);
    }

    /**
     * Get configured twitter user name.
     *
     * @return configured twitter user name.
     */
    static String getTwitterUserName() {
        return (String) TWITTER_PROPERTIES.get(TWITTER_USER_NAME);
    }

    /**
     * Get configured twitter user password.
     *
     * @return configured twitter user password.
     */
    static String getTwitterUserPassword() {
        return (String) TWITTER_PROPERTIES.get(TWITTER_USER_PASSWORD);
    }

    private static Properties loadSettings() {
        final Properties properties = new Properties();

        FileInputStream st = null;
        try {
            String homeDir = System.getProperty("user.home");
            st = new FileInputStream(homeDir + File.separator + TWITTER_PROPERTIES_FILE_NAME);
            properties.load(st);
        } catch (IOException e) {
            // ignore
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }

        for (String name : new String[] {TWITTER_USER_NAME, TWITTER_USER_PASSWORD}) {
            String value = System.getProperty(name);
            if (value != null) {
                properties.setProperty(name, value);
            }
        }

        if (properties.getProperty(TWITTER_USER_NAME) == null
                || properties.getProperty(TWITTER_USER_PASSWORD) == null) {
            System.out.println(String.format(
                    "'%s' and '%s' properties not set. "
                            + "You need to provide them either via '$HOME/%s' file or as system properties.",
                    TWITTER_USER_NAME, TWITTER_USER_PASSWORD, TWITTER_PROPERTIES_FILE_NAME));
            System.exit(1);
        }
        return properties;
    }

    /**
     * Simple HttpHandler for serving static content included in web root
     * directory of this application.
     */
    private static class StaticContentHandler extends HttpHandler {

        private static final HashMap<String, String> EXTENSION_TO_MEDIA_TYPE;

        static {
            EXTENSION_TO_MEDIA_TYPE = new HashMap<>();

            EXTENSION_TO_MEDIA_TYPE.put("html", "text/html");
            EXTENSION_TO_MEDIA_TYPE.put("js", "application/javascript");
            EXTENSION_TO_MEDIA_TYPE.put("css", "text/css");
            EXTENSION_TO_MEDIA_TYPE.put("png", "image/png");
            EXTENSION_TO_MEDIA_TYPE.put("ico", "image/png");
        }

        private final String webRootPath;

        StaticContentHandler(String webRootPath) {
            this.webRootPath = webRootPath;
        }

        @Override
        public void service(Request request, Response response) throws Exception {
            String uri = request.getRequestURI();

            int pos = uri.lastIndexOf('.');
            String extension = uri.substring(pos + 1);
            String mediaType = EXTENSION_TO_MEDIA_TYPE.get(extension);

            if (uri.contains("..") || mediaType == null) {
                response.sendError(HttpStatus.NOT_FOUND_404.getStatusCode());
                return;
            }

            final String resourcesContextPath = request.getContextPath();
            System.out.println("context: " + resourcesContextPath);
            if (resourcesContextPath != null && !resourcesContextPath.isEmpty()) {
                if (!uri.startsWith(resourcesContextPath)) {
                    response.sendError(HttpStatus.NOT_FOUND_404.getStatusCode());
                    return;
                }

                uri = uri.substring(resourcesContextPath.length());
                System.out.println("URI: " + uri);
            }

            InputStream fileStream;

            try {
                fileStream = webRootPath == null
                        ? App.class.getResourceAsStream(WEB_ROOT + uri)
                        : new FileInputStream(webRootPath + uri);
            } catch (IOException e) {
                fileStream = null;
            }

            if (fileStream == null) {
                response.sendError(HttpStatus.NOT_FOUND_404.getStatusCode());
            } else {
                response.setStatus(HttpStatus.OK_200);
                response.setContentType(mediaType);
                ReaderWriter.writeTo(fileStream, response.getOutputStream());
            }
        }
    }
}
