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

package org.glassfish.jersey.jdkhttp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.jdkhttp.internal.LocalizationMessages;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsExchange;

/**
 * Jersey {@code Container} implementation based on Java SE {@link HttpServer}.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JdkHttpHandlerContainer implements HttpHandler, Container {

    private static final Logger LOGGER = Logger.getLogger(JdkHttpHandlerContainer.class.getName());

    private volatile ApplicationHandler appHandler;

    /**
     * Create new lightweight Java SE HTTP server container.
     *
     * @param application JAX-RS / Jersey application to be deployed on the container.
     */
    JdkHttpHandlerContainer(final Application application) {
        this.appHandler = new ApplicationHandler(application);
    }

    /**
     * Create new lightweight Java SE HTTP server container.
     *
     * @param application   JAX-RS / Jersey application to be deployed on the container.
     * @param parentContext DI provider specific context with application's registered bindings.
     */
    JdkHttpHandlerContainer(final Application application, final Object parentContext) {
        this.appHandler = new ApplicationHandler(application, null, parentContext);
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        /**
         * This is a URI that contains the path, query and fragment components.
         */
        URI exchangeUri = exchange.getRequestURI();

        /**
         * The base path specified by the HTTP context of the HTTP handler. It
         * is in decoded form.
         */
        String decodedBasePath = exchange.getHttpContext().getPath();

        // Ensure that the base path ends with a '/'
        if (!decodedBasePath.endsWith("/")) {
            if (decodedBasePath.equals(exchangeUri.getPath())) {
                /**
                 * This is an edge case where the request path does not end in a
                 * '/' and is equal to the context path of the HTTP handler.
                 * Both the request path and base path need to end in a '/'
                 * Currently the request path is modified.
                 *
                 * TODO support redirection in accordance with resource configuration feature.
                 */
                exchangeUri = UriBuilder.fromUri(exchangeUri)
                        .path("/").build();
            }
            decodedBasePath += "/";
        }

        /*
         * The following is madness, there is no easy way to get the complete
         * URI of the HTTP request!!
         *
         * TODO this is missing the user information component, how can this be obtained?
         */
        final boolean isSecure = exchange instanceof HttpsExchange;
        final String scheme = isSecure ? "https" : "http";

        final URI baseUri = getBaseUri(exchange, decodedBasePath, scheme);
        final URI requestUri = getRequestUri(exchange, baseUri);

        final ResponseWriter responseWriter = new ResponseWriter(exchange);
        final ContainerRequest requestContext = new ContainerRequest(baseUri, requestUri,
                exchange.getRequestMethod(), getSecurityContext(exchange.getPrincipal(), isSecure),
                new MapPropertiesDelegate());
        requestContext.setEntityStream(exchange.getRequestBody());
        requestContext.getHeaders().putAll(exchange.getRequestHeaders());
        requestContext.setWriter(responseWriter);
        try {
            appHandler.handle(requestContext);
        } finally {
            // if the response was not committed yet by the JerseyApplication
            // then commit it and log warning
            responseWriter.closeAndLogWarning();
        }
    }

    private URI getBaseUri(final HttpExchange exchange, final String decodedBasePath, final String scheme) {
        final URI baseUri;
        try {
            final List<String> hostHeader = exchange.getRequestHeaders().get("Host");
            if (hostHeader != null) {
                baseUri = new URI(scheme + "://" + hostHeader.get(0) + decodedBasePath);
            } else {
                final InetSocketAddress addr = exchange.getLocalAddress();
                baseUri = new URI(scheme, null, addr.getHostName(), addr.getPort(),
                        decodedBasePath, null, null);
            }
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        return baseUri;
    }

    private URI getRequestUri(final HttpExchange exchange, final URI baseUri) {
        try {
            return new URI(getServerAddress(baseUri) + exchange.getRequestURI());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private String getServerAddress(final URI baseUri) throws URISyntaxException {
        return new URI(baseUri.getScheme(), null,  baseUri.getHost(), baseUri.getPort(), null, null, null).toString();
    }

    private SecurityContext getSecurityContext(final Principal principal, final boolean isSecure) {
        return new SecurityContext() {

            @Override
            public boolean isUserInRole(final String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return isSecure;
            }

            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };
    }

    @Override
    public ResourceConfig getConfiguration() {
        return appHandler.getConfiguration();
    }

    @Override
    public void reload() {
        reload(getConfiguration());
    }

    @Override
    public void reload(final ResourceConfig configuration) {
        appHandler.onShutdown(this);

        appHandler = new ApplicationHandler(configuration);
        appHandler.onReload(this);
        appHandler.onStartup(this);
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return appHandler;
    }

    /**
     * Inform this container that the server has been started.
     *
     * This method must be implicitly called after the server containing this container is started.
     */
    void onServerStart() {
        this.appHandler.onStartup(this);
    }

    /**
     * Inform this container that the server is being stopped.
     *
     * This method must be implicitly called before the server containing this container is stopped.
     */
    void onServerStop() {
        this.appHandler.onShutdown(this);
    }

    private static final class ResponseWriter implements ContainerResponseWriter {

        private final HttpExchange exchange;
        private final AtomicBoolean closed;

        /**
         * Creates a new ResponseWriter for given {@link HttpExchange HTTP Exchange}.
         *
         * @param exchange Exchange of the {@link HttpServer JDK Http Server}
         */
        ResponseWriter(final HttpExchange exchange) {
            this.exchange = exchange;
            this.closed = new AtomicBoolean(false);
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(final long contentLength, final ContainerResponse context)
                throws ContainerException {
            final MultivaluedMap<String, String> responseHeaders = context.getStringHeaders();
            final Headers serverHeaders = exchange.getResponseHeaders();
            for (final Map.Entry<String, List<String>> e : responseHeaders.entrySet()) {
                for (final String value : e.getValue()) {
                    serverHeaders.add(e.getKey(), value);
                }
            }

            try {
                if (context.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                    // Work around bug in LW HTTP server
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6886436
                    exchange.sendResponseHeaders(context.getStatus(), -1);
                } else {
                    exchange.sendResponseHeaders(context.getStatus(),
                            getResponseLength(contentLength));
                }
            } catch (final IOException ioe) {
                throw new ContainerException(LocalizationMessages.ERROR_RESPONSEWRITER_WRITING_HEADERS(), ioe);
            }

            return exchange.getResponseBody();
        }

        private long getResponseLength(final long contentLength) {
            if (contentLength == 0) {
                return -1;
            }
            if (contentLength < 0) {
                return 0;
            }
            return contentLength;
        }

        @Override
        public boolean suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
            throw new UnsupportedOperationException("Method suspend is not supported by the container.");
        }

        @Override
        public void setSuspendTimeout(final long timeOut, final TimeUnit timeUnit) throws IllegalStateException {
            throw new UnsupportedOperationException("Method setSuspendTimeout is not supported by the container.");
        }

        @Override
        public void failure(final Throwable error) {
            try {
                exchange.sendResponseHeaders(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), getResponseLength(0));
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, LocalizationMessages.ERROR_RESPONSEWRITER_SENDING_FAILURE_RESPONSE(), e);
            } finally {
                commit();
                rethrow(error);
            }
        }

        @Override
        public boolean enableResponseBuffering() {
            return true;
        }

        @Override
        public void commit() {
            if (closed.compareAndSet(false, true)) {
                exchange.close();
            }
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

        /**
         * Commits the response and logs a warning message.
         *
         * This method should be called by the container at the end of the
         * handle method to make sure that the ResponseWriter was committed.
         */
        private void closeAndLogWarning() {
            if (closed.compareAndSet(false, true)) {
                exchange.close();
                LOGGER.log(Level.WARNING, LocalizationMessages.ERROR_RESPONSEWRITER_RESPONSE_UNCOMMITED());
            }
        }
    }
}
