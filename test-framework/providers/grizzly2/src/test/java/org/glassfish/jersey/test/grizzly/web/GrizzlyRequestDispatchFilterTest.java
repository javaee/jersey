/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.grizzly.web;

import java.io.IOException;
import java.util.EnumSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.glassfish.grizzly.servlet.HttpServletRequestImpl;

import org.junit.Assert;
import org.junit.Test;

/**
 * Reproducer for JERSEY-1893.
 *
 * This is to make sure filters could be utilized even for filtering
 * requests that are being forwarded/included within the server side
 * using {@link javax.servlet.RequestDispatcher} mechanism.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class GrizzlyRequestDispatchFilterTest extends JerseyTest {

    /**
     * We can only register a single Servlet instance. This one then serves as a request dispatcher
     * as well as Jersey Servlet container.
     */
    public static class RequestDispatcherServlet extends ServletContainer {

        @Override
        public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (isInbound(req)) {
                final String action = req.getParameter("action");
                if ("forward".equals(action)) {
                    getServletContext().getRequestDispatcher("/forward").forward(req, resp);
                } else if ("include".equals(action)) {
                    getServletContext().getRequestDispatcher("/included").include(req, resp);
                } else {
                    super.service(req, resp);
                }
            } else {
                super.service(req, resp);
            }
        }

        private boolean isInbound(HttpServletRequest req) {
            // this is a workaround for broken getDispatchType in grizzly
            return req instanceof HttpServletRequestImpl;
        }

    }

    /**
     * Filter that should be applied for regular requests coming directly from client.
     */
    public static class RegularFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
                throws IOException, ServletException {
            servletResponse.getOutputStream().print("[");
            filterChain.doFilter(servletRequest, servletResponse);
            servletResponse.getOutputStream().print("]");
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * Filter that will only be applied for internally forwarded requests.
     */
    public static class ForwardFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
                throws IOException, ServletException {
            servletResponse.getOutputStream().print(">>");
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * Filter for internal include calls.
     */
    public static class IncludeFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
                throws IOException, ServletException {
            servletResponse.getOutputStream().print("SOMETHING ");
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * Resource to be accessed directly from the client only.
     */
    @Path("direct")
    public static class DirectResource {

        @GET
        public String get() {
            return "DIRECT";
        }
    }

    /**
     * Resource that will also be called from the server side using the Servlet forward mechanism.
     */
    @Path("forward")
    public static class ForwardResource {

        @GET
        public String get() {
            return "FORWARD";
        }
    }

    /**
     * Resource that will also be called from the server side using the Servlet include mechanism.
     */
    @Path("included")
    public static class IncludeResource {

        @GET
        public String get() {
            return "INCLUDED";
        }
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.forServlet(RequestDispatcherServlet.class)
                .addFilter(ForwardFilter.class, "forwardFilter",
                        EnumSet.of(javax.servlet.DispatcherType.FORWARD))
                .addFilter(IncludeFilter.class, "includeFilter",
                        EnumSet.of(javax.servlet.DispatcherType.INCLUDE))
                .addFilter(RegularFilter.class, "regularFilter")
                .initParam(ServerProperties.PROVIDER_PACKAGES, this.getClass().getPackage().getName())
                .build();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    /**
     * Reproducer test for JERSEY-1893.
     */
    @Test
    public void testGet() {
        WebTarget target = target();

        String s;

        // check that the regular filter gets involved
        s = target.path("direct").request().get(String.class);
        Assert.assertEquals("[DIRECT]", s);

        // the regular filter should work for directly requested forward resource as well.
        s = target.path("forward").request().get(String.class);
        Assert.assertEquals("[FORWARD]", s);

        // forward action should enforce forward filter to be invoked
        s = target.queryParam("action", "forward").request().get(String.class);
        Assert.assertEquals(">>FORWARD", s);

        // direct call to the include resource
        s = target.path("included").request().get(String.class);
        Assert.assertEquals("[INCLUDED]", s);

        // include call should involve both regular and include filter
        s = target.path("included").queryParam("action", "include").request().get(String.class);
        Assert.assertEquals("[SOMETHING INCLUDED]", s);
    }
}
