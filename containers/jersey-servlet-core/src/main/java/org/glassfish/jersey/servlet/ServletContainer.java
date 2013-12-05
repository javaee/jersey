/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.ConfigHelper;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.internal.LocalizationMessages;
import org.glassfish.jersey.uri.UriComponent;

/**
 * A {@link javax.servlet.Servlet} or {@link Filter} for deploying root resource classes.
 * <p />
 * The following sections make reference to initialization parameters. Unless
 * otherwise specified the initialization parameters apply to both server
 * and filter initialization parameters.
 * <p />
 * The servlet or filter may be configured to have an initialization
 * parameter {@value ServletProperties#JAXRS_APPLICATION_CLASS}
 * (see {@link org.glassfish.jersey.servlet.ServletProperties#JAXRS_APPLICATION_CLASS}) and whose value is a
 * fully qualified name of a class that implements {@link javax.ws.rs.core.Application}.
 * The class is instantiated as a singleton component
 * managed by the runtime, and injection may be performed (the artifacts that
 * may be injected are limited to injectable providers registered when
 * the servlet or filter is configured).
 * <p />
 * If the initialization parameter {@value ServletProperties#JAXRS_APPLICATION_CLASS}
 * is not present and a initialization parameter {@value org.glassfish.jersey.server.ServerProperties#PROVIDER_PACKAGES}
 * is present (see {@link ServerProperties#PROVIDER_PACKAGES}) a new instance of
 * {@link ResourceConfig} with this configuration is created. The initialization parameter
 * {@value org.glassfish.jersey.server.ServerProperties#PROVIDER_PACKAGES} MUST be set to provide one or
 * more package names. Each package name MUST be separated by ';'.
 * <p />
 * If none of the above resource configuration related initialization parameters
 * are present a new instance of {@link ResourceConfig} with {@link WebAppResourcesScanner}
 * is created. The initialization parameter {@value org.glassfish.jersey.server.ServerProperties#PROVIDER_CLASSPATH}
 * is present (see {@link ServerProperties#PROVIDER_CLASSPATH}) MAY be
 * set to provide one or more resource paths. Each path MUST be separated by ';'.
 * If the initialization parameter is not present then the following resource
 * paths are utilized: {@code "/WEB-INF/lib"} and {@code "/WEB-INF/classes"}.
 * <p />
 * All initialization parameters are added as properties of the created
 * {@link ResourceConfig}.
 * <p />
 * A new {@link org.glassfish.jersey.server.ApplicationHandler} instance will be created and configured such
 * that the following classes may be injected onto a root resource, provider
 * and {@link javax.ws.rs.core.Application} classes using {@link javax.ws.rs.core.Context
 * &#64;Context} annotation:
 * {@link HttpServletRequest}, {@link HttpServletResponse},
 * {@link ServletContext}, {@link javax.servlet.ServletConfig} and {@link WebConfig}.
 * If this class is used as a Servlet then the {@link javax.servlet.ServletConfig} class may
 * be injected. If this class is used as a servlet filter then the {@link FilterConfig}
 * class may be injected. {@link WebConfig} may be injected to abstract
 * servlet or filter deployment.
 * <p />
 * Persistence units that may be injected must be configured in web.xml
 * in the normal way plus an additional servlet parameter to enable the
 * Jersey servlet to locate them in JNDI. E.g. with the following
 * persistence unit configuration:
 * <pre>{@code
 * <persistence-unit-ref>
 *     <persistence-unit-ref-name>persistence/widget</persistence-unit-ref-name>
 *     <persistence-unit-name>WidgetPU</persistence-unit-name>
 * </persistence-unit-ref>
 * }</pre>
 * the Jersey servlet requires an additional servlet parameter as
 * follows:
 * <pre>{@code
 * <init-param>
 *     <param-name>unit:WidgetPU</param-name>
 *     <param-value>persistence/widget</param-value>
 * </init-param>
 * }</pre>
 * Given the above, Jersey will inject the {@link javax.persistence.EntityManagerFactory EntityManagerFactory} found
 * at {@code java:comp/env/persistence/widget} in JNDI when encountering a
 * field or parameter annotated with {@code @PersistenceUnit(unitName="WidgetPU")}.
 *
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class ServletContainer extends HttpServlet implements Filter, Container {

    private static final long serialVersionUID = 3932047066686065219L;
    private transient FilterConfig filterConfig;
    private transient WebComponent webComponent;
    private transient ResourceConfig resourceConfig;
    private transient Pattern staticContentPattern;
    private transient String filterContextPath;
    private volatile ContainerLifecycleListener containerListener;

    private static final ExtendedLogger logger =
            new ExtendedLogger(Logger.getLogger(ServletContainer.class.getName()), Level.FINEST);

    /**
     * Initiate the Web component.
     *
     * @param webConfig the Web configuration.
     * @throws javax.servlet.ServletException in case of an initialization failure
     */
    protected void init(WebConfig webConfig) throws ServletException {
        webComponent = new WebComponent(webConfig, resourceConfig);
        containerListener = ConfigHelper.getContainerLifecycleListener(webComponent.appHandler);
        containerListener.onStartup(this);
    }

    /**
     * Create Jersey Servlet container.
     */
    public ServletContainer() {
    }

    /**
     * Create Jersey Servlet container.
     *
     * @param resourceConfig container configuration.
     */
    public ServletContainer(ResourceConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
    }

    /**
     * Dispatches client requests to the protected
     * {@code service} method. There's no need to
     * override this method.
     *
     * @param req the {@link HttpServletRequest} object that
     *            contains the request the client made of
     *            the servlet
     * @param res the {@link HttpServletResponse} object that
     *            contains the response the servlet returns
     *            to the client
     * @throws IOException      if an input or output error occurs
     *                          while the servlet is handling the
     *                          HTTP request
     * @throws ServletException if the HTTP request cannot
     *                          be handled
     * @see javax.servlet.Servlet#service
     */
    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
        HttpServletRequest request;
        HttpServletResponse response;

        if (!(req instanceof HttpServletRequest && res instanceof HttpServletResponse)) {
            throw new ServletException("non-HTTP request or response");
        }

        request = (HttpServletRequest) req;
        response = (HttpServletResponse) res;

        service(request, response);
    }

    /**
     * Receives standard HTTP requests from the public {@code service} method and dispatches
     * them to the {@code do}<i>XXX</i> methods defined in
     * this class. This method is an HTTP-specific version of the
     * {@link javax.servlet.Servlet#service} method. There's no
     * need to override this method.
     *
     * @param request  the {@link HttpServletRequest} object that
     *                 contains the request the client made of
     *                 the servlet
     * @param response the {@link HttpServletResponse} object that
     *                 contains the response the servlet returns
     *                 to the client
     * @throws IOException      if an input or output error occurs
     *                          while the servlet is handling the
     *                          HTTP request
     * @throws ServletException if the HTTP request
     *                          cannot be handled
     * @see javax.servlet.Servlet#service
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /**
         * There is an annoying edge case where the service method is
         * invoked for the case when the URI is equal to the deployment URL
         * minus the '/', for example http://locahost:8080/HelloWorldWebApp
         */
        final String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        StringBuffer requestURL = request.getRequestURL();
        String requestURI = request.getRequestURI();
        final boolean checkPathInfo = pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/");
        if (checkPathInfo && !request.getRequestURI().endsWith("/")) {
            // Only do this if the last segment of the servlet path does not contain '.'
            // This handles the case when the extension mapping is used with the servlet
            // see issue 506
            // This solution does not require parsing the deployment descriptor,
            // however still leaves it broken for the very rare case if a standard path
            // servlet mapping would include dot in the last segment (e.g. /.webresources/*)
            // and somebody would want to hit the root resource without the trailing slash
            int i = servletPath.lastIndexOf('/');
            if (servletPath.substring(i + 1).indexOf('.') < 0) {
                // TODO (+ handle request URL with invalid characters - see the creation of absoluteUriBuilder bellow)
//                if (webComponent.getResourceConfig().getFeature(ResourceConfig.FEATURE_REDIRECT)) {
//                    URI l = UriBuilder.fromUri(request.getRequestURL().toString()).
//                            path("/").
//                            replaceQuery(request.getQueryString()).build();
//
//                    response.setStatus(307);
//                    response.setHeader("Location", l.toASCIIString());
//                    return;
//                } else {
//                pathInfo = "/";
//                requestURL.append("/");
//                requestURI += "/";
//                }
            }
        }

        /**
         * The HttpServletRequest.getRequestURL() contains the complete URI
         * minus the query and fragment components.
         */
        UriBuilder absoluteUriBuilder;
        try {
            absoluteUriBuilder = UriBuilder.fromUri(requestURL.toString());
        } catch (IllegalArgumentException iae) {
            final Response.Status badRequest = Response.Status.BAD_REQUEST;
            response.sendError(badRequest.getStatusCode(), badRequest.getReasonPhrase());
            return;
        }

        /**
         * The HttpServletRequest.getPathInfo() and
         * HttpServletRequest.getServletPath() are in decoded form.
         *
         * On some servlet implementations the getPathInfo() removed
         * contiguous '/' characters. This is problematic if URIs
         * are embedded, for example as the last path segment.
         * We need to work around this and not use getPathInfo
         * for the decodedPath.
         */
        final String decodedBasePath = request.getContextPath() + servletPath + "/";

        final String encodedBasePath = UriComponent.encode(decodedBasePath,
                UriComponent.Type.PATH);

        if (!decodedBasePath.equals(encodedBasePath)) {
            throw new ProcessingException("The servlet context path and/or the "
                    + "servlet path contain characters that are percent encoded");
        }

        final URI baseUri;
        final URI requestUri;
        try {
            baseUri = absoluteUriBuilder.replacePath(encodedBasePath).
                    build();

            String queryParameters = request.getQueryString();
            if (queryParameters == null) {
                queryParameters = "";
            }

            requestUri = absoluteUriBuilder.replacePath(requestURI).
                    replaceQuery(queryParameters).
                    build();
        } catch (UriBuilderException ex) {
            final Response.Status badRequest = Response.Status.BAD_REQUEST;
            response.sendError(badRequest.getStatusCode(), badRequest.getReasonPhrase());
            return;
        }

        service(baseUri, requestUri, request, response);
    }

    @Override
    public void destroy() {
        super.destroy();
        ContainerLifecycleListener listener = containerListener;
        if (listener != null) {
            listener.onShutdown(this);
        }
    }

    @Override
    public void init() throws ServletException {
        init(new WebServletConfig(this));
    }

    /**
     * Dispatch client requests to a resource class.
     *
     * @param baseUri    the base URI of the request.
     * @param requestUri the URI of the request.
     * @param request    the {@link javax.servlet.http.HttpServletRequest} object that
     *                   contains the request the client made to
     *                   the Web component.
     * @param response   the {@link javax.servlet.http.HttpServletResponse} object that
     *                   contains the response the Web component returns
     *                   to the client.
     * @return lazily initialized response status code {@link Value value provider}.
     * @throws IOException      if an input or output error occurs
     *                          while the Web component is handling the
     *                          HTTP request.
     * @throws ServletException if the HTTP request cannot
     *                          be handled.
     */
    public Value<Integer> service(URI baseUri, URI requestUri, final HttpServletRequest request,
                                  HttpServletResponse response) throws ServletException, IOException {
        return webComponent.service(baseUri, requestUri, request, response);
    }

    // Filter
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        init(new WebFilterConfig(filterConfig));

        String regex = (String) getConfiguration().getProperty(ServletProperties.FILTER_STATIC_CONTENT_REGEX);
        if (regex != null && regex.length() > 0) {
            try {
                staticContentPattern = Pattern.compile(regex);
            } catch (PatternSyntaxException ex) {
                throw new ContainerException(LocalizationMessages.INIT_PARAM_REGEX_SYNTAX_INVALID(
                        regex, ServletProperties.FILTER_STATIC_CONTENT_REGEX), ex);
            }
        }

        this.filterContextPath = filterConfig.getInitParameter(ServletProperties.FILTER_CONTEXT_PATH);
        if (filterContextPath != null) {
            if (filterContextPath.isEmpty()) {
                filterContextPath = null;
            } else {
                if (!filterContextPath.startsWith("/")) {
                    filterContextPath = '/' + filterContextPath;
                }
                if (filterContextPath.endsWith("/")) {
                    filterContextPath = filterContextPath.substring(0, filterContextPath.length() - 1);
                }
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
        } catch (ClassCastException e) {
            throw new ServletException("non-HTTP request or response", e);
        }
    }

    /**
     * Get the servlet context for the servlet or filter, depending on
     * how this class is registered.
     *
     * @return the servlet context for the servlet or filter.
     */
    @Override
    public ServletContext getServletContext() {
        if (filterConfig != null) {
            return filterConfig.getServletContext();
        }

        return super.getServletContext();
    }

    /**
     * Dispatches client requests to the
     * {@link #service(URI, URI, HttpServletRequest, HttpServletResponse)} method.
     * <p />
     * If the servlet path matches the regular expression declared by the
     * property {@link ServletProperties#FILTER_STATIC_CONTENT_REGEX} then the
     * request is forwarded to the next filter in the filter chain so that the
     * underlying servlet engine can process the request otherwise Jersey
     * will process the request.
     *
     * @param request  the {@link HttpServletRequest} object that
     *                 contains the request the client made to
     *                 the servlet.
     * @param response the {@link HttpServletResponse} object that
     *                 contains the response the servlet returns
     *                 to the client.
     * @param chain    the chain of filters from which the next filter can be invoked.
     * @throws java.io.IOException            in case of an I/O error.
     * @throws javax.servlet.ServletException in case of an error while executing the
     *                                        filter chain.
     */
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            final String includeRequestURI = (String) request.getAttribute("javax.servlet.include.request_uri");

            if (!includeRequestURI.equals(request.getRequestURI())) {
                doFilter(request, response, chain,
                        includeRequestURI,
                        (String) request.getAttribute("javax.servlet.include.servlet_path"),
                        (String) request.getAttribute("javax.servlet.include.query_string"));
                return;
            }
        }

        /**
         * JERSEY-880 - WAS interprets HttpServletRequest#getServletPath() and HttpServletRequest#getPathInfo()
         * differently when accessing a static resource.
         */
        final String servletPath = request.getServletPath()
                + (request.getPathInfo() == null ? "" : request.getPathInfo());

        doFilter(request, response, chain,
                request.getRequestURI(),
                servletPath,
                request.getQueryString());
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                          String requestURI, String servletPath, String queryString) throws IOException, ServletException {
        // if we match the static content regular expression lets delegate to
        // the filter chain to use the default container servlets & handlers
        final Pattern p = getStaticContentPattern();
        if (p != null && p.matcher(servletPath).matches()) {
            chain.doFilter(request, response);
            return;
        }

        if (filterContextPath != null) {
            if (!servletPath.startsWith(filterContextPath)) {
                throw new ContainerException(LocalizationMessages.SERVLET_PATH_MISMATCH(servletPath, filterContextPath));
                //TODO:
//            } else if (servletPath.length() == filterContextPath.length()) {
//                // Path does not end in a slash, may need to redirect
//                if (webComponent.getResourceConfig().getFeature(ResourceConfig.FEATURE_REDIRECT)) {
//                    URI l = UriBuilder.fromUri(request.getRequestURL().toString()).
//                            path("/").
//                            replaceQuery(queryString).build();
//
//                    response.setStatus(307);
//                    response.setHeader("Location", l.toASCIIString());
//                    return;
//                } else {
//                    requestURI += "/";
//                }
            }
        }

        final UriBuilder absoluteUriBuilder = UriBuilder.fromUri(
                request.getRequestURL().toString());

        final URI baseUri = (filterContextPath == null)
                ? absoluteUriBuilder.replacePath(request.getContextPath()).
                path("/").
                build()
                : absoluteUriBuilder.replacePath(request.getContextPath()).
                path(filterContextPath).
                path("/").
                build();

        final URI requestUri = absoluteUriBuilder.replacePath(requestURI).
                replaceQuery(queryString).
                build();

        final int status = service(baseUri, requestUri, request, response).get();

        // If forwarding is configured and response is a 404 with no entity
        // body then call the next filter in the chain
        if (webComponent.forwardOn404 && status == 404 && !response.isCommitted()) {
            // lets clear the response to OK before we forward to the next in the chain
            // as OK is the default set by servlet containers before filters/servlets do any wor
            // so lets hide our footsteps and pretend we were never in the chain at all and let the
            // next filter or servlet return the 404 if they can't find anything to return
            //
            // We could add an optional flag to disable this step if anyone can ever find a case where
            // this causes a problem, though I suspect any problems will really be with downstream
            // servlets not correctly setting an error status if they cannot find something to return
            response.setStatus(HttpServletResponse.SC_OK);
            chain.doFilter(request, response);
        }
    }

    /**
     * Get the static content path pattern.
     *
     * @return the {@link Pattern} compiled from a regular expression that is
     *         the property value of {@link ServletProperties#FILTER_STATIC_CONTENT_REGEX}.
     *         A {@code null} value will be returned if the property is not set or is
     *         an empty String.
     */
    protected Pattern getStaticContentPattern() {
        return staticContentPattern;
    }

    @Override
    public ResourceConfig getConfiguration() {
        return webComponent.appHandler.getConfiguration();
    }

    @Override
    public void reload() {
        reload(getConfiguration());
    }

    @Override
    public void reload(ResourceConfig configuration) {
        try {
            containerListener.onShutdown(this);
            webComponent = new WebComponent(webComponent.webConfig, configuration);
            containerListener = ConfigHelper.getContainerLifecycleListener(webComponent.appHandler);
            containerListener.onReload(this);
            containerListener.onStartup(this);
        } catch (ServletException ex) {
            logger.log(Level.SEVERE, "Reload failed", ex);
        }
    }
}
