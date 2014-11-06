/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Type;
import java.net.URI;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.internal.ServiceFinderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.InternalServerProperties;
import org.glassfish.jersey.server.internal.RuntimeExecutorsBinder;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;
import org.glassfish.jersey.servlet.internal.LocalizationMessages;
import org.glassfish.jersey.servlet.internal.PersistenceUnitBinder;
import org.glassfish.jersey.servlet.internal.ResponseWriter;
import org.glassfish.jersey.servlet.internal.ServletContainerProviderFactory;
import org.glassfish.jersey.servlet.internal.spi.ServletContainerProvider;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegate;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegateProvider;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * An common Jersey web component that may be extended by a Servlet and/or
 * Filter implementation, or encapsulated by a Servlet or Filter implementation.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class WebComponent {

    private static final Logger LOGGER = Logger.getLogger(WebComponent.class.getName());

    private static final Type RequestTYPE = (new TypeLiteral<Ref<HttpServletRequest>>() {}).getType();
    private static final Type ResponseTYPE = (new TypeLiteral<Ref<HttpServletResponse>>() {}).getType();

    public static final String SERVICE_LOCATOR_ATTRIBUTE = "jersey.servlet.WebComponent.ServiceLocator";

    private static final AsyncContextDelegate DefaultAsyncDELEGATE = new AsyncContextDelegate() {

        @Override
        public void suspend() throws IllegalStateException {
            throw new UnsupportedOperationException(LocalizationMessages.ASYNC_PROCESSING_NOT_SUPPORTED());
        }

        @Override
        public void complete() {
        }
    };

    /**
     * Return the first found {@link AsyncContextDelegateProvider}
     * (via {@link Providers#getAllProviders(org.glassfish.hk2.api.ServiceLocator, Class)}) or {@code #DefaultAsyncDELEGATE} if
     * other delegate cannot be found.
     *
     * @return a non-null AsyncContextDelegateProvider.
     */
    private AsyncContextDelegateProvider getAsyncExtensionDelegate() {
        final Iterator<AsyncContextDelegateProvider> providers = Providers.getAllProviders(appHandler.getServiceLocator(),
                AsyncContextDelegateProvider.class).iterator();
        if (providers.hasNext()) {
            return providers.next();
        }

        return new AsyncContextDelegateProvider() {

            @Override
            public AsyncContextDelegate createDelegate(final HttpServletRequest request, final HttpServletResponse response) {
                return DefaultAsyncDELEGATE;
            }
        };
    }

    @SuppressWarnings("JavaDoc")
    private static class HttpServletRequestReferencingFactory extends ReferencingFactory<HttpServletRequest> {
        @Inject
        public HttpServletRequestReferencingFactory(Provider<Ref<HttpServletRequest>> referenceFactory) {
            super(referenceFactory);
        }
    }

    @SuppressWarnings("JavaDoc")
    private static class HttpServletResponseReferencingFactory extends ReferencingFactory<HttpServletResponse> {
        @Inject
        public HttpServletResponseReferencingFactory(Provider<Ref<HttpServletResponse>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private class WebComponentBinder extends AbstractBinder {

        private final Map<String, Object> applicationProperties;

        /**
         * Create binder for {@link WebComponent} passing a map of properties to determine whether certain features are allowed or
         * not.
         *
         * @param applicationProperties map of properties to determine whether certain features are allowed or not.
         */
        private WebComponentBinder(Map<String, Object> applicationProperties) {
            this.applicationProperties = applicationProperties;
        }

        @Override
        protected void configure() {
            bindFactory(HttpServletRequestReferencingFactory.class).to(HttpServletRequest.class)
                    .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
            bindFactory(ReferencingFactory.<HttpServletRequest>referenceFactory())
                    .to(new TypeLiteral<Ref<HttpServletRequest>>() {}).in(RequestScoped.class);

            bindFactory(HttpServletResponseReferencingFactory.class).to(HttpServletResponse.class)
                    .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
            bindFactory(ReferencingFactory.<HttpServletResponse>referenceFactory())
                    .to(new TypeLiteral<Ref<HttpServletResponse>>() {}).in(RequestScoped.class);

            bindFactory(new Factory<ServletContext>() {
                @Override
                public ServletContext provide() {
                    return webConfig.getServletContext();
                }

                @Override
                public void dispose(ServletContext instance) {
                    //not used
                }
            }).to(ServletContext.class).in(Singleton.class);

            final ServletConfig servletConfig = webConfig.getServletConfig();
            if (webConfig.getConfigType() == WebConfig.ConfigType.ServletConfig) {
                bindFactory(new Factory<ServletConfig>() {
                    @Override
                    public ServletConfig provide() {
                        return servletConfig;
                    }

                    @Override
                    public void dispose(ServletConfig instance) {
                        //not used
                    }
                }).to(ServletConfig.class).in(Singleton.class);

                // @PersistenceUnit
                for (final Enumeration initParams = servletConfig.getInitParameterNames(); initParams.hasMoreElements(); ) {
                    final String initParamName = (String) initParams.nextElement();

                    if (initParamName.startsWith(PersistenceUnitBinder.PERSISTENCE_UNIT_PREFIX)) {
                        install(new PersistenceUnitBinder());
                        break;
                    }
                }
            } else {
                bindFactory(new Factory<FilterConfig>() {
                    @Override
                    public FilterConfig provide() {
                        return webConfig.getFilterConfig();
                    }

                    @Override
                    public void dispose(FilterConfig instance) {
                        //not used
                    }
                }).to(FilterConfig.class).in(Singleton.class);
            }

            bindFactory(new Factory<WebConfig>() {
                @Override
                public WebConfig provide() {
                    return webConfig;
                }

                @Override
                public void dispose(WebConfig instance) {
                    //not used
                }
            }).to(WebConfig.class).in(Singleton.class);

            install(new ServiceFinderBinder<AsyncContextDelegateProvider>(AsyncContextDelegateProvider.class,
                    applicationProperties, RuntimeType.SERVER));
        }
    }

    /**
     * Jersey application handler.
     */
    final ApplicationHandler appHandler;
    /**
     * Jersey background task scheduler - used for scheduling request timeout event handling tasks.
     */
    final ScheduledExecutorService backgroundTaskScheduler;
    /**
     * Web component configuration.
     */
    final WebConfig webConfig;
    /**
     * If {@code true} and deployed as filter, the unmatched requests will be forwarded.
     */
    final boolean forwardOn404;
    /**
     * Cached value of configuration property
     * {@link org.glassfish.jersey.server.ServerProperties#RESPONSE_SET_STATUS_OVER_SEND_ERROR}.
     * If {@code true} method {@link HttpServletResponse#setStatus} is used over {@link HttpServletResponse#sendError}.
     */
    final boolean configSetStatusOverSendError;
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

        // SPI/extension hook to configure ResourceConfig
        configure(resourceConfig);

        AbstractBinder webComponentBinder = new WebComponentBinder(resourceConfig.getProperties());
        resourceConfig.register(webComponentBinder);

        ServiceLocator locator = (ServiceLocator) webConfig.getServletContext().getAttribute(SERVICE_LOCATOR_ATTRIBUTE);

        this.appHandler = new ApplicationHandler(resourceConfig, webComponentBinder, locator);

        this.asyncExtensionDelegate = getAsyncExtensionDelegate();
        this.forwardOn404 = webConfig.getConfigType().equals(WebConfig.ConfigType.FilterConfig) &&
                resourceConfig.isProperty(ServletProperties.FILTER_FORWARD_ON_404);
        this.configSetStatusOverSendError = ServerProperties.getValue(resourceConfig.getProperties(),
                ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, false, Boolean.class);
        this.backgroundTaskScheduler = appHandler.getServiceLocator()
                .getService(ScheduledExecutorService.class, new RuntimeExecutorsBinder.BackgroundSchedulerLiteral());
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
     * @return lazily initialized response status code {@link Value value provider}.
     * @throws java.io.IOException            if an input or output error occurs
     *                                        while the Web component is handling the
     *                                        HTTP request.
     * @throws javax.servlet.ServletException if the HTTP request cannot
     *                                        be handled.
     */
    public Value<Integer> service(
            final URI baseUri,
            final URI requestUri,
            final HttpServletRequest servletRequest,
            final HttpServletResponse servletResponse) throws ServletException, IOException {

        ContainerRequest requestContext = new ContainerRequest(baseUri, requestUri,
                servletRequest.getMethod(), getSecurityContext(servletRequest), new ServletPropertiesDelegate(servletRequest));
        requestContext.setEntityStream(servletRequest.getInputStream());
        addRequestHeaders(servletRequest, requestContext);

        try {
            // Check if any servlet filters have consumed a request entity
            // of the media type application/x-www-form-urlencoded
            // This can happen if a filter calls request.getParameter(...)
            filterFormParameters(servletRequest, requestContext);

            final ResponseWriter responseWriter = new ResponseWriter(
                    forwardOn404,
                    configSetStatusOverSendError,
                    servletResponse,
                    asyncExtensionDelegate.createDelegate(servletRequest, servletResponse),
                    backgroundTaskScheduler);

            requestContext.setRequestScopedInitializer(new RequestScopedInitializer() {
                @Override
                public void initialize(ServiceLocator locator) {
                    locator.<Ref<HttpServletRequest>>getService(RequestTYPE).set(servletRequest);
                    locator.<Ref<HttpServletResponse>>getService(ResponseTYPE).set(servletResponse);
                }
            });
            requestContext.setWriter(responseWriter);

            appHandler.handle(requestContext);

            return Values.lazy(new Value<Integer>() {
                @Override
                public Integer get() {
                    return responseWriter.getResponseStatus();
                }
            });
        } catch (final HeaderValueException hve) {
            final Response.Status status = Response.Status.BAD_REQUEST;
            if (configSetStatusOverSendError) {
                servletResponse.reset();
                servletResponse.setStatus(status.getStatusCode(), status.getReasonPhrase());
            } else {
                servletResponse.sendError(status.getStatusCode(), status.getReasonPhrase());
            }

            return Values.of(status.getStatusCode());
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

    /**
     * Get default {@link SecurityContext} for given {@code request}.
     *
     * @param request http servlet request to create a security context for.
     * @return a non-null security context instance.
     */
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

    /**
     * Create a {@link ResourceConfig} instance from given {@link WebConfig}.
     *
     * @param config web config to create resource config from.
     * @return resource config instance.
     * @throws ServletException if an error has occurred.
     */
    private static ResourceConfig createResourceConfig(WebConfig config) throws ServletException {
        final Map<String, Object> initParams = getInitParams(config);
        final Map<String, Object> contextParams = getContextParams(config.getServletContext());

        // check if the JAX-RS application config class property is present
        String jaxrsApplicationClassName = config.getInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS);

        if (jaxrsApplicationClassName == null) {
            // If no resource config class property is present, create default config
            final ResourceConfig rc = new ResourceConfig().addProperties(initParams).addProperties(contextParams);

            final String webApp = config.getInitParameter(ServletProperties.PROVIDER_WEB_APP);
            if (webApp != null && !"false".equals(webApp)) {
                rc.registerFinder(new WebAppResourcesScanner(config.getServletContext()));
            }
            return rc;
        }

        try {
            Class<? extends javax.ws.rs.core.Application> jaxrsApplicationClass = AccessController.doPrivileged(
                    ReflectionHelper.<javax.ws.rs.core.Application>classForNameWithExceptionPEA(jaxrsApplicationClassName)
            );

            if (javax.ws.rs.core.Application.class.isAssignableFrom(jaxrsApplicationClass)) {
                return ResourceConfig.forApplicationClass(jaxrsApplicationClass)
                        .addProperties(initParams).addProperties(contextParams);
            } else {
                throw new ServletException(LocalizationMessages.RESOURCE_CONFIG_PARENT_CLASS_INVALID(
                        jaxrsApplicationClassName, javax.ws.rs.core.Application.class));
            }
        } catch (PrivilegedActionException e) {
            throw new ServletException(
                    LocalizationMessages.RESOURCE_CONFIG_UNABLE_TO_LOAD(jaxrsApplicationClassName), e.getCause());
        } catch (ClassNotFoundException e) {
            throw new ServletException(LocalizationMessages.RESOURCE_CONFIG_UNABLE_TO_LOAD(jaxrsApplicationClassName), e);
        }
    }

    /**
     * SPI/extension hook to configure ResourceConfig.
     *
     * @param resourceConfig Jersey application configuration.
     * @throws ServletException if an error has occurred.
     */
    private static void configure(ResourceConfig resourceConfig) throws ServletException {
        final ServletContainerProvider[] allServletContainerProviders = //TODO check if META-INF/services lookup is enabled
                ServletContainerProviderFactory.getAllServletContainerProviders();
        for (ServletContainerProvider servletContainerProvider : allServletContainerProviders) {
            servletContainerProvider.configure(resourceConfig);
        }
    }

    /**
     * Copy request headers present in {@code request} into {@code requestContext} ignoring {@code null} values.
     *
     * @param request http servlet request to copy headers from.
     * @param requestContext container request to copy headers to.
     */
    @SuppressWarnings("unchecked")
    private void addRequestHeaders(HttpServletRequest request, ContainerRequest requestContext) {
        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); ) {
            String name = names.nextElement();
            for (Enumeration<String> values = request.getHeaders(name); values.hasMoreElements(); ) {
                final String value = values.nextElement();
                if (value != null) { // filter out null values
                    requestContext.header(name, value);
                }
            }
        }
    }

    /**
     * Extract init params from {@link WebConfig}.
     *
     * @param webConfig actual servlet context.
     * @return map representing current init parameters.
     */
    private static Map<String, Object> getInitParams(WebConfig webConfig) {
        Map<String, Object> props = new HashMap<String, Object>();
        Enumeration names = webConfig.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            props.put(name, webConfig.getInitParameter(name));
        }
        return props;
    }

    /**
     * Extract context params from {@link ServletContext}.
     *
     * @param servletContext actual servlet context.
     * @return map representing current context parameters.
     */
    public static Map<String, Object> getContextParams(ServletContext servletContext) {
        Map<String, Object> props = new HashMap<String, Object>();
        Enumeration names = servletContext.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            props.put(name, servletContext.getAttribute(name));
        }
        return props;
    }

    /**
     * Extract parameters contained in {@link HttpServletRequest servlet request} and put them into
     * {@link ContainerRequest container request} under {@value org.glassfish.jersey.server.internal.InternalServerProperties#FORM_DECODED_PROPERTY} property (as {@link Form}
     * instance).
     *
     * @param servletRequest http servlet request to extract params from.
     * @param containerRequest container request to put {@link Form} property to.
     */
    private void filterFormParameters(HttpServletRequest servletRequest, ContainerRequest containerRequest) {
        if (MediaTypes.typeEqual(MediaType.APPLICATION_FORM_URLENCODED_TYPE, containerRequest.getMediaType())
                && !containerRequest.hasEntity()) {
            final Form form = new Form();
            final Enumeration parameterNames = servletRequest.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                final String name = (String) parameterNames.nextElement();
                final String[] values = servletRequest.getParameterValues(name);

                form.asMap().put(name, Arrays.asList(values));
            }

            if (!form.asMap().isEmpty()) {
                containerRequest.setProperty(InternalServerProperties.FORM_DECODED_PROPERTY, form);

                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, LocalizationMessages.FORM_PARAM_CONSUMED(containerRequest.getRequestUri()));
                }
            }
        }
    }

    /**
     * Get {@link ApplicationHandler} used by this web component.
     * 
     * @return The application handler
     */
    public ApplicationHandler getAppHandler() {
        return appHandler;
    }
}
