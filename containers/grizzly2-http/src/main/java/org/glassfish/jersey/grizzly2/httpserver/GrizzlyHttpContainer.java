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
package org.glassfish.jersey.grizzly2.httpserver;

import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.HeadersFactory;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.JerseyContainerRequestContext;
import org.glassfish.jersey.server.JerseyContainerResponseContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.ConfigHelper;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.scopes.PerLookup;

import org.jvnet.hk2.annotations.Inject;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.utils.Charsets;

/**
 * Grizzly 2 Jersey HTTP Container.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public final class GrizzlyHttpContainer extends HttpHandler implements Container {

    private static final ExtendedLogger logger =
            new ExtendedLogger(Logger.getLogger(GrizzlyHttpContainer.class.getName()), Level.FINEST);

    /**
     * Referencing factory for Grizzly request.
     */
    private static class GrizzlyRequestReferencingFactory extends ReferencingFactory<Request> {

        public GrizzlyRequestReferencingFactory(@Inject Factory<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * Referencing factory for Grizzly response.
     */
    private static class GrizzlyResponseReferencingFactory extends ReferencingFactory<Response> {

        public GrizzlyResponseReferencingFactory(@Inject Factory<Ref<Response>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * An internal module to enable Grizzly HTTP container specific types injection.
     * This module allows to inject underlying Grizzly HTTP request and response instances.
     */
    private static class GrizzlyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(Request.class).toFactory(GrizzlyRequestReferencingFactory.class).in(PerLookup.class);
            bind(new TypeLiteral<Ref<Request>>() {
            }).
                    toFactory(ReferencingFactory.<Request>referenceFactory()).in(RequestScope.class);

            bind(Response.class).toFactory(GrizzlyResponseReferencingFactory.class).in(PerLookup.class);
            bind(new TypeLiteral<Ref<Response>>() {
            }).
                    toFactory(ReferencingFactory.<Response>referenceFactory()).in(RequestScope.class);
        }
    }

    private static final CompletionHandler<Response> EMPTY_COMPLETION_HANDLER = new CompletionHandler<Response>() {

        @Override
        public void cancelled() {
            // no-op
        }

        @Override
        public void failed(Throwable throwable) {
            // no-op
        }

        @Override
        public void completed(Response result) {
            // no-op
        }

        @Override
        public void updated(Response result) {
            // no-op
        }
    };

    private final static class ResponseWriter implements ContainerResponseWriter {

        private final String name;
        private final Response grizzlyResponse;

        ResponseWriter(final Response response) {
            this.grizzlyResponse = response;

            if (logger.isDebugLoggable()) {
                this.name = "ResponseWriter {" + "id=" + UUID.randomUUID().toString() + ", grizzlyResponse=" + grizzlyResponse.hashCode() + '}';
                logger.debugLog("{0} - init", name);
            } else {
                this.name = "ResponseWriter";
            }
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public void commit() {
            try {
                if (grizzlyResponse.isSuspended()) {
                    grizzlyResponse.resume();
                }
            } finally {
                logger.debugLog("{0} - commit() called", name);
            }
        }

        @Override
        public void cancel() {
            try {
                grizzlyResponse.cancel();
            } finally {
                logger.debugLog("{0} - cancel() called", name);
            }
        }

        @Override
        public void suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
            try {
                grizzlyResponse.suspend(timeOut, timeUnit, EMPTY_COMPLETION_HANDLER,
                        new org.glassfish.grizzly.http.server.TimeoutHandler() {

                            @Override
                            public boolean onTimeout(Response response) {
                                if (timeoutHandler != null) {
                                    timeoutHandler.onTimeout(ResponseWriter.this);
                                }

                                // TODO should we return true ins some cases instead?
                                // Returning false relies on the fact that the timeoutHandler
                                // will resume the response.
                                return false;
                            }
                        });
            } finally {
                logger.debugLog("{0} - suspend(...) called", name);
            }
        }

        @Override
        public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
            try {
                grizzlyResponse.getSuspendContext().setTimeout(timeOut, timeUnit);
            } finally {
                logger.debugLog("{0} - setSuspendTimeout(...) called", name);
            }
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(final long contentLength,
                                                          final JerseyContainerResponseContext context)
                throws ContainerException {
            try {
                grizzlyResponse.setStatus(context.getStatus());
                grizzlyResponse.setContentLengthLong(contentLength);

                for (final Map.Entry<String, List<String>> e : HeadersFactory.getStringHeaders(
                        context.getHeaders()).entrySet()) {
                    for (final String value : e.getValue()) {
                        grizzlyResponse.addHeader(e.getKey(), value);
                    }
                }

                return grizzlyResponse.getOutputStream();
            } finally {
                logger.debugLog("{0} - writeResponseStatusAndHeaders() called", name);
            }
        }
    }

    private volatile ApplicationHandler appHandler;
    private final ContainerLifecycleListener containerListener;

    /**
     * Creates a new Grizzly container.
     *
     * @param application Jersey application to be deployed on Grizzly container.
     */
    GrizzlyHttpContainer(final ApplicationHandler application) {
        this.appHandler = application;
        this.containerListener = ConfigHelper.getContainerLifecycleListener(application);

        this.appHandler.registerAdditionalModules(new HashSet<Module>() {{
            add(new GrizzlyModule());
        }});
    }

    @Override
    public void start() {
        super.start();
        containerListener.onStartup(this);
    }

    @Override
    public void service(final Request request, final Response response) {
        final ResponseWriter responseWriter = new ResponseWriter(response);
        try {
            logger.debugLog("GrizzlyHttpContaner.service(...) started");
            URI baseUri = getBaseUri(request);
            JerseyContainerRequestContext requestContext = new JerseyContainerRequestContext(baseUri,
                    getRequestUri(baseUri, request), request.getMethod().getMethodString(),
                    getSecurityContext(request), new GrizzlyRequestPropertiesDelegate(request));
            requestContext.setEntityStream(request.getInputStream());
            for (String headerName : request.getHeaderNames()) {
                requestContext.headers(headerName, request.getHeaders(headerName));
            }
            requestContext.setWriter(responseWriter);
            requestContext.setRequestScopedInitializer(new RequestScopedInitializer() {
                @Override
                public void initialize(Services services) {
                    services.forContract(new TypeLiteral<Ref<Request>>() {}).get().set(request);
                    services.forContract(new TypeLiteral<Ref<Response>>() {}).get().set(response);
                }
            });
            appHandler.handle(requestContext);
        } finally {
            // TODO if writer not closed or suspended yet, suspend.
            logger.debugLog("GrizzlyHttpContaner.service(...) finished");
        }
    }

    @Override
    public ResourceConfig getConfiguration() {
        return appHandler.getConfiguration();
    }

    @Override
    public void reload() {
        reload(appHandler.getConfiguration());
    }

    @Override
    public void reload(ResourceConfig configuration) {
        appHandler = new ApplicationHandler(configuration.addModules(new GrizzlyModule()));
        containerListener.onReload(this);
    }

    @Override
    public void destroy() {
        super.destroy();
        containerListener.onShutdown(this);
    }

    private SecurityContext getSecurityContext(final Request request) {
        return new SecurityContext() {

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return request.isSecure();
            }

            @Override
            public Principal getUserPrincipal() {
                return request.getUserPrincipal();
            }

            @Override
            public String getAuthenticationScheme() {
                return request.getAuthType();
            }
        };
    }

    private URI getBaseUri(final Request request) {
        try {
            return new URI(request.getScheme(), null, request.getServerName(),
                    request.getServerPort(), getBasePath(request), null, null);
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private String getBasePath(final Request request) {
        final String contextPath = request.getContextPath();

        if (contextPath == null || contextPath.length() == 0) {
            return "/";
        } else if (contextPath.charAt(contextPath.length() - 1) != '/') {
            return contextPath + "/";
        } else {
            return contextPath;
        }
    }

    private URI getRequestUri(URI baseUri, Request grizzlyRequest) {
        // TODO: this is terrible, there must be a way to obtain the original request URI!
        String originalUri = UriBuilder
                .fromPath(
                        grizzlyRequest.getRequest().getRequestURIRef().getOriginalRequestURIBC()
                                .toString(Charsets.DEFAULT_CHARSET)).build().toString();

        String queryString = grizzlyRequest.getQueryString();
        if (queryString != null) {
            originalUri = originalUri + "?" + queryString;
        }

        final URI requestUri = baseUri.resolve(originalUri);

        return requestUri;
    }
}
