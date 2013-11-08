/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty.servlet;

import java.net.URI;
import java.util.Map;

import javax.servlet.Servlet;

import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.uri.UriComponent;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

/**
 * Factory for creating and starting Jetty {@link Server} instances
 * for deploying a Servlet.
 * <p/>
 * The default deployed server is an instance of {@link ServletContainer}.
 * <p/>
 * If no initialization parameters are declared (or is null) then root
 * resource and provider classes will be found by searching the classes
 * referenced in the java classpath.
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public final class JettyWebContainerFactory {

    private JettyWebContainerFactory() {
    }

    /**
     * Create a {@link Server} that registers the {@link ServletContainer}.
     *
     * @param u the URI to create the http server. The URI scheme must be
     *          equal to "http". The URI user information and host
     *          are ignored If the URI port is not present then port 80 will be
     *          used. The URI query and fragment components are ignored. Only first path segment will be used
     *          as context path, the rest will be ignored.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(String u)
            throws Exception {
        if (u == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }

        return create(URI.create(u));
    }

    /**
     * Create a {@link Server} that registers the {@link ServletContainer}.
     *
     * @param u          the URI to create the http server. The URI scheme must be
     *                   equal to "http". The URI user information and host
     *                   are ignored If the URI port is not present then port 80 will be
     *                   used. The URI query and fragment components are ignored. Only first path segment will be used
     *                   as context path, the rest will be ignored.
     * @param initParams the servlet initialization parameters.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(String u, Map<String, String> initParams)
            throws Exception {
        if (u == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }

        return create(URI.create(u), initParams);
    }

    /**
     * Create a {@link Server} that registers the {@link ServletContainer}.
     *
     * @param u the URI to create the http server. The URI scheme must be
     *          equal to "http". The URI user information and host
     *          are ignored If the URI port is not present then port 80 will be
     *          used. The URI query and fragment components are ignored. Only first path segment will be used
     *          as context path, the rest will be ignored.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(URI u)
            throws Exception {
        return create(u, ServletContainer.class);
    }

    /**
     * Create a {@link Server} that registers the {@link ServletContainer}.
     *
     * @param u          the URI to create the http server. The URI scheme must be
     *                   equal to "http". The URI user information and host
     *                   are ignored If the URI port is not present then port 80 will be
     *                   used. The URI query and fragment components are ignored. Only first path segment will be used
     *                   as context path, the rest will be ignored.
     * @param initParams the servlet initialization parameters.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(URI u, Map<String, String> initParams)
            throws Exception {
        return create(u, ServletContainer.class, initParams);
    }

    /**
     * Create a {@link Server} that registers the declared
     * servlet class.
     *
     * @param u the URI to create the http server. The URI scheme must be
     *          equal to "http". The URI user information and host
     *          are ignored If the URI port is not present then port 80 will be
     *          used. The URI query and fragment components are ignored. Only first path segment will be used
     *          as context path, the rest will be ignored.
     * @param c the servlet class.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(String u, Class<? extends Servlet> c)
            throws Exception {
        if (u == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }

        return create(URI.create(u), c);
    }

    /**
     * Create a {@link Server} that registers the declared
     * servlet class.
     *
     * @param u          the URI to create the http server. The URI scheme must be
     *                   equal to "http". The URI user information and host
     *                   are ignored If the URI port is not present then port 80 will be
     *                   used. The URI query and fragment components are ignored. Only first path segment will be used
     *                   as context path, the rest will be ignored.
     * @param c          the servlet class.
     * @param initParams the servlet initialization parameters.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(String u, Class<? extends Servlet> c,
                                Map<String, String> initParams)
            throws Exception {
        if (u == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }

        return create(URI.create(u), c, initParams);
    }

    /**
     * Create a {@link Server} that registers the declared
     * servlet class.
     *
     * @param u the URI to create the http server. The URI scheme must be
     *          equal to "http". The URI user information and host
     *          are ignored If the URI port is not present then port 80 will be
     *          used. The URI query and fragment components are ignored. Only first path segment will be used
     *          as context path, the rest will be ignored.
     * @param c the servlet class.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(URI u, Class<? extends Servlet> c)
            throws Exception {
        return create(u, c, null);
    }

    /**
     * Create a {@link Server} that registers the declared
     * servlet class.
     *
     * @param u          the URI to create the http server. The URI scheme must be
     *                   equal to "http". The URI user information and host
     *                   are ignored If the URI port is not present then port 80 will be
     *                   used. The URI query and fragment components are ignored. Only first path segment will be used
     *                   as context path, the rest will be ignored.
     * @param c          the servlet class.
     * @param initParams the servlet initialization parameters.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(URI u, Class<? extends Servlet> c, Map<String, String> initParams)
            throws Exception {
        return create(u, c, null, initParams, null);
    }

    private static Server create(URI u, Class<? extends Servlet> c, Servlet servlet,
                                 Map<String, String> initParams, Map<String, String> contextInitParams)
            throws Exception {
        if (u == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }

        String path = u.getPath();
        if (path == null) {
            throw new IllegalArgumentException("The URI path, of the URI " + u + ", must be non-null");
        } else if (path.isEmpty()) {
            throw new IllegalArgumentException("The URI path, of the URI " + u + ", must be present");
        } else if (path.charAt(0) != '/') {
            throw new IllegalArgumentException("The URI path, of the URI " + u + ". must start with a '/'");
        }

        path = String.format("/%s", UriComponent.decodePath(u.getPath(), true).get(1).toString());
        WebAppContext context = new WebAppContext();
        context.setDisplayName("JettyContext");
        context.setContextPath(path);
        context.setConfigurations(new Configuration[]{new WebXmlConfiguration()});
        ServletHolder holder;
        if (c != null) {
            holder = context.addServlet(c, "/*");
        } else {
            holder = new ServletHolder(servlet);
            context.addServlet(holder, "/*");
        }

        if (contextInitParams != null) {
            for (Map.Entry<String, String> e : contextInitParams.entrySet()) {
                context.setInitParameter(e.getKey(), e.getValue());
            }
        }

        if (initParams != null) {
            holder.setInitParameters(initParams);
        }

        Server server = JettyHttpContainerFactory.createServer(u, false);
        server.setHandler(context);
        server.start();
        return server;
    }

    /**
     * Create a {@link Server} that registers the declared
     * servlet instance.
     *
     * @param u                 the URI to create the HTTP server. The URI scheme must be
     *                          equal to "http". The URI user information and host
     *                          are ignored If the URI port is not present then port 80 will be
     *                          used. The URI query and fragment components are ignored. Only first path segment will be used
     *                          as context path, the rest will be ignored.
     * @param servlet           the servlet instance.
     * @param initParams        the servlet initialization parameters.
     * @param contextInitParams the servlet context initialization parameters.
     * @return the http server, with the endpoint started.
     * @throws Exception                if an error occurs creating the container.
     * @throws IllegalArgumentException if HTTP server URI is {@code null}.
     */
    public static Server create(URI u, Servlet servlet, Map<String, String> initParams, Map<String, String> contextInitParams)
            throws Exception {
        if (servlet == null) {
            throw new IllegalArgumentException("The servlet must not be null");
        }
        return create(u, null, servlet, initParams, contextInitParams);
    }
}
