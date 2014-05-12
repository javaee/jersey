/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.grizzly;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestHelper;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;

/**
 * A Servlet-based test container factory for creating test container instances using Grizzly.
 *
 * @author Srinivas Bhimisetty
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class GrizzlyWebTestContainerFactory implements TestContainerFactory {

    @Override
    public TestContainer create(final URI baseUri, final DeploymentContext context) {
        if (!(context instanceof ServletDeploymentContext)) {
            throw new IllegalArgumentException("The deployment context must be an instance of ServletDeploymentContext.");
        }

        return new GrizzlyWebTestContainer(baseUri, (ServletDeploymentContext) context);
    }

    /**
     * This class has methods for instantiating, starting and stopping the Grizzly 2 Web
     * Server.
     */
    private static class GrizzlyWebTestContainer implements TestContainer {

        private static final Logger LOGGER = Logger.getLogger(GrizzlyWebTestContainer.class.getName());

        private URI baseUri;

        private final ServletDeploymentContext deploymentContext;

        private HttpServer server;

        private GrizzlyWebTestContainer(final URI baseUri, final ServletDeploymentContext context) {
            this.baseUri = UriBuilder.fromUri(baseUri)
                    .path(context.getContextPath())
                    .path(context.getServletPath())
                    .build();

            LOGGER.info("Creating GrizzlyWebTestContainer configured at the base URI "
                    + TestHelper.zeroPortToAvailablePort(baseUri));

            this.deploymentContext = context;
            instantiateGrizzlyWebServer();
        }

        @Override
        public ClientConfig getClientConfig() {
            return null;
        }

        @Override
        public URI getBaseUri() {
            return baseUri;
        }

        @Override
        public void start() {
            if (server.isStarted()) {
                LOGGER.log(Level.WARNING, "Ignoring start request - GrizzlyWebTestContainer is already started.");

            } else {
                LOGGER.log(Level.FINE, "Starting GrizzlyWebTestContainer...");
                try {
                    server.start();

                    if (baseUri.getPort() == 0) {
                        baseUri = UriBuilder.fromUri(baseUri)
                                .port(server.getListener("grizzly").getPort())
                                .build();
                        LOGGER.log(Level.INFO, "Started GrizzlyWebTestContainer at the base URI " + baseUri);
                    }
                } catch (final IOException ioe) {
                    throw new TestContainerException(ioe);
                }
            }
        }

        @Override
        public void stop() {
            if (server.isStarted()) {
                LOGGER.log(Level.FINE, "Stopping GrizzlyWebTestContainer...");
                this.server.shutdownNow();
            } else {
                LOGGER.log(Level.WARNING, "Ignoring stop request - GrizzlyWebTestContainer is already stopped.");
            }
        }

        private void instantiateGrizzlyWebServer() {

            String contextPathLocal = deploymentContext.getContextPath();
            if (!contextPathLocal.isEmpty() && !contextPathLocal.startsWith("/")) {
                contextPathLocal = "/" + contextPathLocal;
            }

            String servletPathLocal = deploymentContext.getServletPath();
            if (!servletPathLocal.startsWith("/")) {
                servletPathLocal = "/" + servletPathLocal;
            }
            if (servletPathLocal.endsWith("/")) {
                servletPathLocal += "*";
            } else {
                servletPathLocal += "/*";
            }

            final WebappContext context = new WebappContext("TestContext", contextPathLocal);

            // servlet class and servlet instance can be both null or one of them is specified exclusively.
            final HttpServlet servletInstance = deploymentContext.getServletInstance();
            final Class<? extends HttpServlet> servletClass = deploymentContext.getServletClass();
            if (servletInstance != null || servletClass != null) {
                final ServletRegistration registration;
                if (servletInstance != null) {
                    registration = context.addServlet(servletInstance.getClass().getName(), servletInstance);
                } else {
                    registration = context.addServlet(servletClass.getName(), servletClass);
                }
                registration.setInitParameters(deploymentContext.getInitParams());
                registration.addMapping(servletPathLocal);
            }

            for (final Class<? extends EventListener> eventListener : deploymentContext.getListeners()) {
                context.addListener(eventListener);
            }

            final Map<String, String> contextParams = deploymentContext.getContextParams();
            for (final String contextParamName : contextParams.keySet()) {
                context.addContextInitParameter(contextParamName, contextParams.get(contextParamName));
            }

            // Filter support
            if (deploymentContext.getFilters() != null) {
                for (final ServletDeploymentContext.FilterDescriptor filterDescriptor : deploymentContext.getFilters()) {

                    final FilterRegistration filterRegistration =
                            context.addFilter(filterDescriptor.getFilterName(), filterDescriptor.getFilterClass());

                    filterRegistration.setInitParameters(filterDescriptor.getInitParams());
                    filterRegistration.addMappingForUrlPatterns(
                            grizzlyDispatcherTypes(filterDescriptor.getDispatcherTypes()),
                            true,
                            servletPathLocal);
                }
            }

            try {
                server = GrizzlyHttpServerFactory.createHttpServer(baseUri, (GrizzlyHttpContainer) null, false, null, false);
                context.deploy(server);
            } catch (final ProcessingException ex) {
                throw new TestContainerException(ex);
            }
        }

        private EnumSet<DispatcherType> grizzlyDispatcherTypes(final Set<DispatcherType> dispatcherTypes) {
            final Set<DispatcherType> grizzlyDispatcherTypes = new HashSet<>();
            for (final javax.servlet.DispatcherType servletDispatchType : dispatcherTypes) {
                grizzlyDispatcherTypes.add(DispatcherType.valueOf(servletDispatchType.name()));
            }
            return EnumSet.copyOf(grizzlyDispatcherTypes);
        }
    }
}
