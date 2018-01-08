/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.SecurityContext;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.grizzly2.httpserver.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.ContainerUtils;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * Jersey {@code Container} implementation based on Grizzly {@link org.glassfish.grizzly.http.server.HttpHandler}.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class GrizzlyHttpContainer extends HttpHandler implements Container {

    private static final ExtendedLogger logger =
            new ExtendedLogger(Logger.getLogger(GrizzlyHttpContainer.class.getName()), Level.FINEST);

    private final Type RequestTYPE = (new GenericType<Ref<Request>>() { }).getType();
    private final Type ResponseTYPE = (new GenericType<Ref<Response>>() { }).getType();

    /**
     * Cached value of configuration property
     * {@link org.glassfish.jersey.server.ServerProperties#RESPONSE_SET_STATUS_OVER_SEND_ERROR}.
     * If {@code true} method {@link org.glassfish.grizzly.http.server.Response#setStatus} is used over
     * {@link org.glassfish.grizzly.http.server.Response#sendError}.
     */
    private boolean configSetStatusOverSendError;

    /**
     * Cached value of configuration property
     * {@link org.glassfish.jersey.server.ServerProperties#REDUCE_CONTEXT_PATH_SLASHES_ENABLED}.
     * If {@code true} method {@link org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer#getRequestUri(Request)}
     * will reduce the of leading context-path slashes to only one.
     */
    private boolean configReduceContextPathSlashesEnabled;

    /**
     * Referencing factory for Grizzly request.
     */
    private static class GrizzlyRequestReferencingFactory extends ReferencingFactory<Request> {

        @Inject
        public GrizzlyRequestReferencingFactory(final Provider<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * Referencing factory for Grizzly response.
     */
    private static class GrizzlyResponseReferencingFactory extends ReferencingFactory<Response> {

        @Inject
        public GrizzlyResponseReferencingFactory(final Provider<Ref<Response>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * An internal binder to enable Grizzly HTTP container specific types injection.
     * <p/>
     * This binder allows to inject underlying Grizzly HTTP request and response instances.
     * Note that since Grizzly {@code Request} class is not proxiable as it does not expose an empty constructor,
     * the injection of Grizzly request instance into singleton JAX-RS and Jersey providers is only supported via
     * {@link javax.inject.Provider injection provider}.
     */
    static class GrizzlyBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(GrizzlyRequestReferencingFactory.class).to(Request.class)
                    .proxy(false).in(RequestScoped.class);
            bindFactory(ReferencingFactory.<Request>referenceFactory()).to(new GenericType<Ref<Request>>() {})
                    .in(RequestScoped.class);

            bindFactory(GrizzlyResponseReferencingFactory.class).to(Response.class)
                    .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
            bindFactory(ReferencingFactory.<Response>referenceFactory()).to(new GenericType<Ref<Response>>() {})
                    .in(RequestScoped.class);
        }
    }

    private static final CompletionHandler<Response> EMPTY_COMPLETION_HANDLER = new CompletionHandler<Response>() {

        @Override
        public void cancelled() {
            // no-op
        }

        @Override
        public void failed(final Throwable throwable) {
            // no-op
        }

        @Override
        public void completed(final Response result) {
            // no-op
        }

        @Override
        public void updated(final Response result) {
            // no-op
        }
    };

    private static final class ResponseWriter implements ContainerResponseWriter {

        private final String name;
        private final Response grizzlyResponse;
        private final boolean configSetStatusOverSendError;

        ResponseWriter(final Response response, final boolean configSetStatusOverSendError) {
            this.grizzlyResponse = response;
            this.configSetStatusOverSendError = configSetStatusOverSendError;

            if (logger.isDebugLoggable()) {
                this.name = "ResponseWriter {" + "id=" + UUID.randomUUID().toString() + ", grizzlyResponse="
                        + grizzlyResponse.hashCode() + '}';
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
        public boolean suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
            try {
                grizzlyResponse.suspend(timeOut, timeUnit, EMPTY_COMPLETION_HANDLER,
                        new org.glassfish.grizzly.http.server.TimeoutHandler() {

                            @Override
                            public boolean onTimeout(final Response response) {
                                if (timeoutHandler != null) {
                                    timeoutHandler.onTimeout(ResponseWriter.this);
                                }

                                // TODO should we return true in some cases instead?
                                // Returning false relies on the fact that the timeoutHandler will resume the response.
                                return false;
                            }
                        }
                );
                return true;
            } catch (final IllegalStateException ex) {
                return false;
            } finally {
                logger.debugLog("{0} - suspend(...) called", name);
            }
        }

        @Override
        public void setSuspendTimeout(final long timeOut, final TimeUnit timeUnit) throws IllegalStateException {
            try {
                grizzlyResponse.getSuspendContext().setTimeout(timeOut, timeUnit);
            } finally {
                logger.debugLog("{0} - setTimeout(...) called", name);
            }
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(final long contentLength,
                                                          final ContainerResponse context)
                throws ContainerException {
            try {
                final javax.ws.rs.core.Response.StatusType statusInfo = context.getStatusInfo();
                if (statusInfo.getReasonPhrase() == null) {
                    grizzlyResponse.setStatus(statusInfo.getStatusCode());
                } else {
                    grizzlyResponse.setStatus(statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                }

                grizzlyResponse.setContentLengthLong(contentLength);

                for (final Map.Entry<String, List<String>> e : context.getStringHeaders().entrySet()) {
                    for (final String value : e.getValue()) {
                        grizzlyResponse.addHeader(e.getKey(), value);
                    }
                }

                return grizzlyResponse.getOutputStream();
            } finally {
                logger.debugLog("{0} - writeResponseStatusAndHeaders() called", name);
            }
        }

        @Override
        @SuppressWarnings("MagicNumber")
        public void failure(final Throwable error) {
            try {
                if (!grizzlyResponse.isCommitted()) {
                    try {
                        if (configSetStatusOverSendError) {
                            grizzlyResponse.reset();
                            grizzlyResponse.setStatus(500, "Request failed.");
                        } else {
                            grizzlyResponse.sendError(500, "Request failed.");
                        }
                    } catch (final IllegalStateException ex) {
                        // a race condition externally committing the response can still occur...
                        logger.log(Level.FINER, "Unable to reset failed response.", ex);
                    } catch (final IOException ex) {
                        throw new ContainerException(
                                LocalizationMessages.EXCEPTION_SENDING_ERROR_RESPONSE(500, "Request failed."),
                                ex);
                    }
                }
            } finally {
                logger.debugLog("{0} - failure(...) called", name);
                rethrow(error);
            }
        }

        @Override
        public boolean enableResponseBuffering() {
            return true;
        }

        /**
         * Rethrow the original exception as required by JAX-RS, 3.3.4
         *
         * @param error throwable to be re-thrown
         */
        private void rethrow(final Throwable error) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else {
                throw new ContainerException(error);
            }
        }
    }

    private volatile ApplicationHandler appHandler;

    /**
     * Create a new Grizzly HTTP container.
     *
     * @param application JAX-RS / Jersey application to be deployed on Grizzly HTTP container.
     */
    /* package */ GrizzlyHttpContainer(final Application application) {
        this.appHandler = new ApplicationHandler(application, new GrizzlyBinder());
        cacheConfigSetStatusOverSendError();
        cacheConfigEnableLeadingContextPathSlashes();
    }

    /**
     * Create a new Grizzly HTTP container.
     *
     * @param application   JAX-RS / Jersey application to be deployed on Grizzly HTTP container.
     * @param parentContext DI provider specific context with application's registered bindings.
     */
    /* package */ GrizzlyHttpContainer(final Application application, final Object parentContext) {
        this.appHandler = new ApplicationHandler(application, new GrizzlyBinder(), parentContext);
        cacheConfigSetStatusOverSendError();
        cacheConfigEnableLeadingContextPathSlashes();
    }

    @Override
    public void start() {
        super.start();
        appHandler.onStartup(this);
    }

    @Override
    public void service(final Request request, final Response response) {
        final ResponseWriter responseWriter = new ResponseWriter(response, configSetStatusOverSendError);
        try {
            logger.debugLog("GrizzlyHttpContainer.service(...) started");
            URI baseUri = getBaseUri(request);
            URI requestUri = getRequestUri(request);
            final ContainerRequest requestContext = new ContainerRequest(baseUri,
                    requestUri, request.getMethod().getMethodString(),
                    getSecurityContext(request), new GrizzlyRequestPropertiesDelegate(request));
            requestContext.setEntityStream(request.getInputStream());
            for (final String headerName : request.getHeaderNames()) {
                requestContext.headers(headerName, request.getHeaders(headerName));
            }
            requestContext.setWriter(responseWriter);

            requestContext.setRequestScopedInitializer(injectionManager -> {
                injectionManager.<Ref<Request>>getInstance(RequestTYPE).set(request);
                injectionManager.<Ref<Response>>getInstance(ResponseTYPE).set(response);
            });
            appHandler.handle(requestContext);
        } finally {
            logger.debugLog("GrizzlyHttpContainer.service(...) finished");
        }
    }

    private boolean containsContextPath(Request request) {
        return request.getContextPath() != null && request.getContextPath().length() > 0;
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
    public void reload(final ResourceConfig configuration) {
        appHandler.onShutdown(this);

        appHandler = new ApplicationHandler(configuration, new GrizzlyBinder());
        appHandler.onReload(this);
        appHandler.onStartup(this);
        cacheConfigSetStatusOverSendError();
        cacheConfigEnableLeadingContextPathSlashes();
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return appHandler;
    }

    @Override
    public void destroy() {
        super.destroy();
        appHandler.onShutdown(this);
        appHandler = null;
    }

    private SecurityContext getSecurityContext(final Request request) {
        return new SecurityContext() {

            @Override
            public boolean isUserInRole(final String role) {
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

        if (contextPath == null || contextPath.isEmpty()) {
            return "/";
        } else if (contextPath.charAt(contextPath.length() - 1) != '/') {
            return contextPath + "/";
        } else {
            return contextPath;
        }
    }

    private URI getRequestUri(final Request request) {
        try {
            final String serverAddress = getServerAddress(request);

            String uri;
            if (configReduceContextPathSlashesEnabled && containsContextPath(request)) {
                uri = ContainerUtils.reduceLeadingSlashes(request.getRequestURI());
            } else {
                uri = request.getRequestURI();
            }

            final String queryString = request.getQueryString();
            if (queryString != null) {
                uri = uri + "?" + ContainerUtils.encodeUnsafeCharacters(queryString);
            }

            return new URI(serverAddress + uri);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private String getServerAddress(final Request request) throws URISyntaxException {
        return new URI(request.getScheme(), null,  request.getServerName(), request.getServerPort(), null, null, null).toString();
    }

    /**
     * The method reads and caches value of configuration property
     * {@link org.glassfish.jersey.server.ServerProperties#RESPONSE_SET_STATUS_OVER_SEND_ERROR} for future purposes.
     */
    private void cacheConfigSetStatusOverSendError() {
        this.configSetStatusOverSendError = ServerProperties.getValue(getConfiguration().getProperties(),
                ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, false, Boolean.class);
    }

    /**
     * The method reads and caches value of configuration property
     * {@link org.glassfish.jersey.server.ServerProperties#REDUCE_CONTEXT_PATH_SLASHES_ENABLED} for future purposes.
     */
    private void cacheConfigEnableLeadingContextPathSlashes() {
        this.configReduceContextPathSlashesEnabled = ServerProperties.getValue(getConfiguration().getProperties(),
                ServerProperties.REDUCE_CONTEXT_PATH_SLASHES_ENABLED, false, Boolean.class);
    }
}
