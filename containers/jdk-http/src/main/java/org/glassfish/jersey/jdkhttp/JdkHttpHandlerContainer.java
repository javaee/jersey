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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.jdkhttp.internal.LocalizationMessages;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.spi.ContainerRequestContext;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.JerseyContainerRequestContext;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsExchange;

/**
 * Container adapter between {@link HttpServer JDK HttpServer} and {@link ApplicationHandler}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class JdkHttpHandlerContainer implements HttpHandler {

    private final ApplicationHandler appHandler;

    /**
     * Creates a new Container connected to given {@link ApplicationHandler}.
     *
     * @param appHandler Jersey application handler for which the container should be
     * initialized.
     */
    JdkHttpHandlerContainer(ApplicationHandler appHandler) {
        this.appHandler = appHandler;
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
                 * Currently the request path is modified. TODO support
                 * redirection in accordance with resource configuration
                 * feature.
                 */
                exchangeUri = UriBuilder.fromUri(exchangeUri).
                        path("/").build();
            }
            decodedBasePath += "/";
        }

        /*
         * The following is madness, there is no easy way to get the complete
         * URI of the HTTP request!!
         *
         * TODO this is missing the user information component, how can this be
         * obtained?
         */
        final boolean isSecure = exchange instanceof HttpsExchange;
        String scheme = isSecure ? "https" : "http";

        URI baseUri = null;
        try {
            List<String> hostHeader = exchange.getRequestHeaders().get("Host");
            if (hostHeader != null) {
                StringBuilder sb = new StringBuilder(scheme);
                sb.append("://").append(hostHeader.get(0)).append(decodedBasePath);
                baseUri = new URI(sb.toString());
            } else {
                InetSocketAddress addr = exchange.getLocalAddress();
                baseUri = new URI(scheme, null, addr.getHostName(), addr.getPort(),
                        decodedBasePath, null, null);
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }

        final URI requestUri = baseUri.resolve(exchangeUri);

        Request.RequestBuilder requestBuilder = Requests.from(baseUri, requestUri, exchange.getRequestMethod(),
                exchange.getRequestBody());

        /**
         * Define http headers
         */
        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                requestBuilder.header(entry.getKey(), value);
            }
        }



        Request jaxRsRequest = requestBuilder.build();
        final ResponseWriter responseWriter = new ResponseWriter(exchange);
        ContainerRequestContext containerRequestCtx = new JerseyContainerRequestContext(jaxRsRequest, responseWriter,
                getSecurityContext(exchange.getPrincipal(), isSecure), null);
        try {
            appHandler.apply(containerRequestCtx);
        } finally {
            // if the response was not commited yet by the JerseyApplication
            // then commit it and log warning
            responseWriter.closeAndLogWarning();
        }
    }

    private SecurityContext getSecurityContext(final Principal principal, final boolean isSecure) {
        return new SecurityContext() {

            @Override
            public boolean isUserInRole(String role) {
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

    private final static class ResponseWriter implements ContainerResponseWriter {

        HttpExchange exchange;
        private final AtomicBoolean closed;

        /**
         * Creates a new ResponseWriter for given {@link HttpExchange HTTP Exchange}.
         *
         * @param exchange Exchange of the {@link HttpServer JDK Http Server}
         */
        ResponseWriter(HttpExchange exchange) {
            this.exchange = exchange;
            this.closed = new AtomicBoolean(false);
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(long contentLength, Response jaxRsResponse)
                throws ContainerException {
            final MultivaluedMap<String, String> jaxRsHeaders = jaxRsResponse.getHeaders().asMap();
            final Headers serverHeaders = exchange.getResponseHeaders();
            for (final Map.Entry<String, List<String>> e : jaxRsHeaders.entrySet()) {
                for (String value : e.getValue()) {
                    serverHeaders.add(e.getKey(), value);
                }
            }

            try {
                if (jaxRsResponse.getStatus() == 204) {
                    // Work around bug in LW HTTP server
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6886436
                    exchange.sendResponseHeaders(jaxRsResponse.getStatus(), -1);
                } else {
                    exchange.sendResponseHeaders(jaxRsResponse.getStatus(),
                            getResponseLength(contentLength));
                }
            } catch (IOException ioe) {
                throw new ContainerException("Error during writing out the response headers.", ioe);
            }

            return exchange.getResponseBody();
        }

        private long getResponseLength(long contentLength) {
            if (contentLength == 0) {
                return -1;
            }
            if (contentLength < 0) {
                return 0;
            }
            return contentLength;
        }

        @Override
        public void suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) throws IllegalStateException {
            throw new UnsupportedOperationException("Method suspend is not support by the container.");
        }

        @Override
        public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
            throw new UnsupportedOperationException("Method suspend is not support by the container.");
        }

        @Override
        public void cancel() {
            commit();
        }

        @Override
        public void commit() {
            if (closed.compareAndSet(false, true)) {
                exchange.close();
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
                Logger.getLogger(JdkHttpHandlerContainer.class.getName()).log(Level.WARNING, LocalizationMessages.ERROR_RESPONSEWRITER_RESPONSE_UNCOMMITED());
                exchange.close();
            }
        }
    }
}