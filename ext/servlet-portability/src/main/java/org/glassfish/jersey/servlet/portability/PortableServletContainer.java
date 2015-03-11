/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.servlet.portability;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Jersey Servlet/Filter class that can be referenced in web.xml instead of Jersey 1.x specific
 * {@code com.sun.jersey.spi.container.servlet.ServletContainer} and Jersey 2.x specific
 * {@link org.glassfish.jersey.servlet.ServletContainer} to enable web application portability between
 * Jersey 1.x and Jersey 2.x servlet containers.
 * <p>
 *     Since for some of the {@link org.glassfish.jersey.servlet.ServletProperties servlet init parameters} that can be
 *     specified in web.xml you may want different values depending on which version of Jersey container is present,
 *     You can prefix the init parameter name either with {@code jersey1#} or {@code jersey2#} to
 *     make it specific to a given version. For example, to specify different values for
 *     {@code javax.ws.rs.Application} init parameter depending on the version of Jersey used, you can include
 *     the following in your web.xml:
 *     <pre>
 *     &lt;servlet&gt;
 *         &lt;servlet-name&gt;Jersey Web Application&lt;/servlet-name&gt;
 *         &lt;servlet-class&gt;org.glassfish.jersey.servlet.portability.PortableServletContainer&lt;/servlet-class&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;jersey1#javax.ws.rs.Application&lt;/param-name&gt;
 *             &lt;param-value&gt;myapp.jersey1specific.Jersey1Application&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;jersey2#javax.ws.rs.Application&lt;/param-name&gt;
 *             &lt;param-value&gt;myapp.jersey2specific.Jersey2Application&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *     &lt;/servlet&gt;
 *     </pre>
 * </p>
 *
 * @author Martin Matula
 */
public class PortableServletContainer implements Filter, Servlet {

    private static final String JERSEY_1_PREFIX = "jersey1#";
    private static final String JERSEY_2_PREFIX = "jersey2#";

    private final Servlet wrappedServlet;
    private final Filter wrappedFilter;
    private final String includePrefix;
    private final String excludePrefix;

    /**
     * Create a new servlet container.
     */
    @SuppressWarnings("unchecked")
    public PortableServletContainer() {
        Class<Servlet> servletClass;
        boolean isJersey1 = false;
        try {
            servletClass = (Class<Servlet>) Class.forName("com.sun.jersey.spi.container.servlet.ServletContainer");
            isJersey1 = true;
        } catch (ClassNotFoundException e) {
            // Jersey 1.x not present, try Jersey 2.x
            try {
                servletClass = (Class<Servlet>) Class.forName("org.glassfish.jersey.servlet.ServletContainer");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException(LocalizationMessages.JERSEY_NOT_AVAILABLE());
            }
        }

        try {
            wrappedServlet = servletClass.newInstance();
            wrappedFilter = (Filter) wrappedServlet;
        } catch (Exception e) {
            throw new RuntimeException(LocalizationMessages.JERSEY_CONTAINER_CANT_LOAD(), e);
        }
        includePrefix = isJersey1 ? JERSEY_1_PREFIX : JERSEY_2_PREFIX;
        excludePrefix = isJersey1 ? JERSEY_2_PREFIX : JERSEY_1_PREFIX;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        wrappedFilter.init(new FilterConfigWrapper(filterConfig));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        wrappedFilter.doFilter(request, response, chain);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        wrappedServlet.init(new ServletConfigWrapper(config));
    }

    @Override
    public ServletConfig getServletConfig() {
        return wrappedServlet.getServletConfig();
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        wrappedServlet.service(req, res);
    }

    @Override
    public String getServletInfo() {
        return wrappedServlet.getServletInfo();
    }

    @Override
    public void destroy() {
        wrappedServlet.destroy();
    }

    private abstract class InitParamsWrapper {

        private final HashMap<String, String> filteredInitParams = new HashMap<String, String>();

        void init() {
            for (Enumeration e = getInitParamNames(); e.hasMoreElements(); ) {
                String name = (String) e.nextElement();
                String value = getInitParamValue(name);
                if (name.startsWith(includePrefix)) {
                    name = name.substring(includePrefix.length());
                } else if (name.startsWith(excludePrefix)) {
                    continue;
                }
                filteredInitParams.put(name, value);
            }
        }

        abstract String getInitParamValue(String name);

        abstract Enumeration getInitParamNames();

        public String getInitParameter(String name) {
            return filteredInitParams.get(name);
        }

        public Enumeration getInitParameterNames() {
            return Collections.enumeration(filteredInitParams.keySet());
        }
    }

    private class FilterConfigWrapper extends InitParamsWrapper implements FilterConfig {

        private final FilterConfig wrapped;

        FilterConfigWrapper(FilterConfig wrapped) {
            this.wrapped = wrapped;
            init();
        }

        @Override
        public String getFilterName() {
            return wrapped.getFilterName();
        }

        @Override
        public ServletContext getServletContext() {
            return wrapped.getServletContext();
        }

        @Override
        String getInitParamValue(String name) {
            return wrapped.getInitParameter(name);
        }

        @Override
        Enumeration getInitParamNames() {
            return wrapped.getInitParameterNames();
        }
    }

    private class ServletConfigWrapper extends InitParamsWrapper implements ServletConfig {

        private final ServletConfig wrapped;

        ServletConfigWrapper(ServletConfig wrapped) {
            this.wrapped = wrapped;
            init();
        }

        @Override
        String getInitParamValue(String name) {
            return wrapped.getInitParameter(name);
        }

        @Override
        Enumeration getInitParamNames() {
            return wrapped.getInitParameterNames();
        }

        @Override
        public String getServletName() {
            return wrapped.getServletName();
        }

        @Override
        public ServletContext getServletContext() {
            return wrapped.getServletContext();
        }
    }
}
