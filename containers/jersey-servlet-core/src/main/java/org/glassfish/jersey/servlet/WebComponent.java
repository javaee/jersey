/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.security.Principal;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.internal.ServiceFinderModule;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.inject.HttpContext;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.ContainerResponseWriter.TimeoutHandler;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;
import org.glassfish.jersey.servlet.internal.LocalizationMessages;
import org.glassfish.jersey.servlet.internal.ResponseWriter;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegate;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegateProvider;

import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;

/**
 * An common Jersey web component that may be extended by a Servlet and/or
 * Filter implementation, or encapsulated by a Servlet or Filter implementation.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class WebComponent {

    private static final Logger LOGGER = Logger.getLogger(WebComponent.class.getName());

    private static final AsyncContextDelegate DefaultAsyncDELEGATE = new AsyncContextDelegate() {

        @Override
        public void suspend(final ContainerResponseWriter writer, final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) throws IllegalStateException {
            throw new UnsupportedOperationException("Asynchronous processing not supported on Servlet 2.x container.");
        }

        @Override
        public void setSuspendTimeout(final long timeOut, final TimeUnit timeUnit) throws IllegalStateException {
            throw new UnsupportedOperationException("Asynchronous processing not supported on Servlet 2.x container.");
        }

        @Override
        public void complete() {
        }

    };

    private AsyncContextDelegateProvider getAsyncExtensionDelegate() {

        final List<AsyncContextDelegateProvider> providers = Providers.getAllProviders(appHandler.getServices(),
                AsyncContextDelegateProvider.class);
        if (!providers.isEmpty()) {
            return providers.iterator().next();
        }

        return new AsyncContextDelegateProvider() {

            @Override
            public AsyncContextDelegate createDelegate(final HttpServletRequest request, final HttpServletResponse response) {
                return DefaultAsyncDELEGATE;
            }
        };
    }

    private static class HttpServletRequestReferencingFactory extends ReferencingFactory<HttpServletRequest> {
        public HttpServletRequestReferencingFactory(@Inject Factory<Ref<HttpServletRequest>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class HttpServletResponseReferencingFactory extends ReferencingFactory<HttpServletResponse> {

        public HttpServletResponseReferencingFactory(@Inject Factory<Ref<HttpServletResponse>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private class WebComponentModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(HttpServletRequest.class).toFactory(HttpServletRequestReferencingFactory.class).in(PerLookup.class);
            bind(new TypeLiteral<Ref<HttpServletRequest>>() {
            }).
                    toFactory(ReferencingFactory.<HttpServletRequest>referenceFactory()).in(RequestScope.class);

            bind(HttpServletResponse.class).toFactory(HttpServletResponseReferencingFactory.class).in(PerLookup.class);
            bind(new TypeLiteral<Ref<HttpServletResponse>>() {
            }).
                    toFactory(ReferencingFactory.<HttpServletResponse>referenceFactory()).in(RequestScope.class);

            bind(ServletContext.class).toFactory(new Factory<ServletContext>() {
                @Override
                public ServletContext get() throws ComponentException {
                    return webConfig.getServletContext();
                }
            }).in(Singleton.class);

            if (webConfig.getConfigType() == WebConfig.ConfigType.ServletConfig) {
                bind(ServletConfig.class).toFactory(new Factory<ServletConfig>() {
                    @Override
                    public ServletConfig get() throws ComponentException {
                        return webConfig.getServletConfig();
                    }
                }).in(Singleton.class);
            } else {
                bind(FilterConfig.class).toFactory(new Factory<FilterConfig>() {

                    @Override
                    public FilterConfig get() throws ComponentException {
                        return webConfig.getFilterConfig();
                    }
                }).in(Singleton.class);
            }

            bind(WebConfig.class).toFactory(new Factory<WebConfig>() {
                @Override
                public WebConfig get() throws ComponentException {
                    return webConfig;
                }
            }).in(Singleton.class);
            install(new ServiceFinderModule<AsyncContextDelegateProvider>(AsyncContextDelegateProvider.class));
        }
    }

    /**
     * Jersey application handler.
     */
    final ApplicationHandler appHandler;
    /**
     * Web component configuration.
     */
    final WebConfig webConfig;
    /**
     * If {@code true} and deployed as filter, the unmatched requests will be forwarded.
     */
    final boolean forwardOn404;
    /**
     * Asynchronous context delegate provider.
     */
    private final AsyncContextDelegateProvider asyncExtensionDelegate;

    /**
     * Create and initialize new web component instance.
     *
     * @param webConfig      we component configuration.
     * @param resourceConfig Jersey application configuration.
     * @throws ServletException in case the Jersey application cannot be created from the supplied
     *                          resource configuration.
     */
    public WebComponent(final WebConfig webConfig, ResourceConfig resourceConfig) throws ServletException {
        this.webConfig = webConfig;
        if (resourceConfig == null) {
            resourceConfig = createResourceConfig(webConfig);
        }
        resourceConfig.addModules(new WebComponentModule());
        this.appHandler = new ApplicationHandler(resourceConfig);
        this.asyncExtensionDelegate = getAsyncExtensionDelegate();
        this.forwardOn404 = webConfig.getConfigType().equals(WebConfig.ConfigType.FilterConfig) &&
                resourceConfig.isProperty(ServletProperties.FILTER_FORWARD_ON_404);
    }

    /**
     * Dispatch client requests to a resource class.
     *
     * @param baseUri         the base URI of the request.
     * @param requestUri      the URI of the request.
     * @param servletRequest  the {@link javax.servlet.http.HttpServletRequest} object that
     *                        contains the request the client made to
     *                        the Web component.
     * @param servletResponse the {@link javax.servlet.http.HttpServletResponse} object that
     *                        contains the response the Web component returns
     *                        to the client.
     * @return the status code of the response.
     * @throws java.io.IOException            if an input or output error occurs
     *                                        while the Web component is handling the
     *                                        HTTP request.
     * @throws javax.servlet.ServletException if the HTTP request cannot
     *                                        be handled.
     */
    public int service(
            final URI baseUri,
            final URI requestUri,
            final HttpServletRequest servletRequest,
            final HttpServletResponse servletResponse) throws ServletException, IOException {

        ContainerRequest requestContext = new ContainerRequest(baseUri, requestUri,
                servletRequest.getMethod(), getSecurityContext(servletRequest), new ServletPropertiesDelegate(servletRequest));
        requestContext.setEntityStream(servletRequest.getInputStream());
        addRequestHeaders(servletRequest, requestContext);

        // Check if any servlet filters have consumed a request entity
        // of the media type application/x-www-form-urlencoded
        // This can happen if a filter calls request.getParameter(...)
        filterFormParameters(servletRequest, requestContext);

        try {
            final ResponseWriter responseWriter = new ResponseWriter(forwardOn404, servletResponse,
                    asyncExtensionDelegate.createDelegate(servletRequest, servletResponse));

            requestContext.setRequestScopedInitializer(new RequestScopedInitializer() {
                @Override
                public void initialize(Services services) {
                    services.forContract(new TypeLiteral<Ref<HttpServletRequest>>() {
                    }).get().set(servletRequest);
                    services.forContract(new TypeLiteral<Ref<HttpServletResponse>>() {
                    }).get().set(servletResponse);
                }
            });
            requestContext.setWriter(responseWriter);

            appHandler.handle(requestContext);

            return responseWriter.getResponseStatus();
        } catch (Exception e) {
            // TODO: proper error handling.
            throw new ServletException(e);
        }

    }

    private SecurityContext getSecurityContext(final HttpServletRequest request) {
        return new SecurityContext() {

            @Override
            public Principal getUserPrincipal() {
                return request.getUserPrincipal();

            }

            @Override
            public boolean isUserInRole(String role) {
                return request.isUserInRole(role);
            }

            @Override
            public boolean isSecure() {
                return request.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return request.getAuthType();
            }
        };
    }

    private static ResourceConfig createResourceConfig(WebConfig config) throws ServletException {
        final Map<String, Object> initParams = getInitParams(config);

        // check if the JAX-RS application config class property is present
        String jaxrsApplicationClassName = config.getInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS);

        if (jaxrsApplicationClassName == null) {
            // If no resource config class property is present, create default config
            final ResourceConfig rc = new ResourceConfig().addProperties(initParams);

            final String webApp = config.getInitParameter(ServletProperties.PROVIDER_WEB_APP);
            if (webApp != null && !"false".equals(webApp)) {
                rc.addFinder(new WebAppResourcesScanner(config.getServletContext()));
            }
            return rc;
        }

        try {
            Class<? extends javax.ws.rs.core.Application> jaxrsApplicationClass = ReflectionHelper.classForNameWithException(jaxrsApplicationClassName);
            if (javax.ws.rs.core.Application.class.isAssignableFrom(jaxrsApplicationClass)) {
                return ResourceConfig.forApplicationClass(jaxrsApplicationClass)
                        .addProperties(initParams);
            } else {
                throw new ServletException(LocalizationMessages.RESOURCE_CONFIG_PARENT_CLASS_INVALID(
                        jaxrsApplicationClassName, javax.ws.rs.core.Application.class));
            }
        } catch (ClassNotFoundException e) {
            throw new ServletException(LocalizationMessages.RESOURCE_CONFIG_UNABLE_TO_LOAD(jaxrsApplicationClassName), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void addRequestHeaders(HttpServletRequest request, ContainerRequest requestContext) {
        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); ) {
            String name = names.nextElement();
            for (Enumeration<String> values = request.getHeaders(name); values.hasMoreElements(); ) {
                requestContext.header(name, values.nextElement());
            }
        }
    }

    private static Map<String, Object> getInitParams(WebConfig webConfig) {
        Map<String, Object> props = new HashMap<String, Object>();
        Enumeration names = webConfig.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            props.put(name, webConfig.getInitParameter(name));
        }
        return props;
    }

// TODO remove the getPaths() method if really not needed.
//    private String[] getPaths(String classpath, ServletContext context) throws ServletException {
//        if (classpath == null) {
//            String[] paths = {
//                    context.getRealPath("/WEB-INF/lib"),
//                    context.getRealPath("/WEB-INF/classes")
//            };
//            if (paths[0] == null && paths[1] == null) {
////                String message = "The default deployment configuration that scans for " +
////                        "classes in /WEB-INF/lib and /WEB-INF/classes is not supported " +
////                        "for the application server." +
////                        "Try using the package scanning configuration, see the JavaDoc for " +
////                        PackagesResourceConfig.class.getName() + " and the property " +
////                        PackagesResourceConfig.PROVIDER_PACKAGES + ".";
////                throw new ServletException(message);
//            }
//            return paths;
//        } else {
//            String[] virtualPaths = classpath.split(";");
//            List<String> resourcePaths = new ArrayList<String>();
//            for (String virtualPath : virtualPaths) {
//                virtualPath = virtualPath.trim();
//                if (virtualPath.length() == 0) {
//                    continue;
//                }
//                String path = context.getRealPath(virtualPath);
//                if (path != null) {
//                    resourcePaths.add(path);
//                }
//            }
//            if (resourcePaths.isEmpty()) {
////                String message = "None of the declared classpath locations, " +
////                        classpath +
////                        ", could be resolved. " +
////                        "This could be because the default deployment configuration that scans for " +
////                        "classes in classpath locations is not supported. " +
////                        "Try using the package scanning configuration, see the JavaDoc for " +
////                        PackagesResourceConfig.class.getName() + " and the property " +
////                        PackagesResourceConfig.PROVIDER_PACKAGES + ".";
////                throw new ServletException(message);
//            }
//            return resourcePaths.toArray(new String[resourcePaths.size()]);
//        }
//    }

    private void filterFormParameters(HttpServletRequest hsr, ContainerRequest request) throws IOException {
        if (MediaTypes.typeEqual(MediaType.APPLICATION_FORM_URLENCODED_TYPE, request.getMediaType())
                && !request.hasEntity()) {

            Form f = new Form();

            Enumeration e = hsr.getParameterNames();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                String[] values = hsr.getParameterValues(name);

                f.asMap().put(name, Arrays.asList(values));
            }

            if (!f.asMap().isEmpty()) {
                request.setProperty(HttpContext.FORM_PROPERTY, f);
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, LocalizationMessages.FORM_PARAM_CONSUMED(request.getRequestUri()));
                }
            }
        }
    }
}
