/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.*;
import org.glassfish.jersey.server.internal.ConfigHelper;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jetty Jersey HTTP Container.
 *
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public final class JettyHttpContainer extends AbstractHandler implements Container {

    private static final ExtendedLogger logger =
            new ExtendedLogger(Logger.getLogger(JettyHttpContainer.class.getName()), Level.FINEST);

    private static final Type RequestTYPE = (new TypeLiteral<Ref<Request>>(){}).getType();
    private static final Type ResponseTYPE = (new TypeLiteral<Ref<Response>>(){}).getType();

    /**
     * Referencing factory for Jetty request.
     */
    private static class JettyRequestReferencingFactory extends ReferencingFactory<Request> {
        @Inject
        public JettyRequestReferencingFactory(Provider<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * Referencing factory for Jetty response.
     */
    private static class JettyResponseReferencingFactory extends ReferencingFactory<Response> {
        @Inject
        public JettyResponseReferencingFactory(Provider<Ref<Response>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * An internal binder to enable Jetty HTTP container specific types injection.
     * This binder allows to inject underlying Jetty HTTP request and response instances.
     */
    private static class JettyBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(JettyRequestReferencingFactory.class).to(Request.class).in(PerLookup.class);
            bindFactory(ReferencingFactory.<Request>referenceFactory()).to(new TypeLiteral<Ref<Request>>() {
            }).in(RequestScoped.class);

            bindFactory(JettyResponseReferencingFactory.class).to(Response.class).in(PerLookup.class);
            bindFactory(ReferencingFactory.<Response>referenceFactory()).to(new TypeLiteral<Ref<Response>>() {
            }).in(RequestScoped.class);
        }
    }

    private volatile ApplicationHandler appHandler;
    private volatile ContainerLifecycleListener containerListener;

    @Override
    public void handle(String target, final Request request, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException, ServletException {
        final Response response = Response.getResponse(httpServletResponse);
        final ResponseWriter responseWriter = new ResponseWriter(request, response);
        final URI baseUri = getBaseUri(request);
        final URI requestUri = baseUri.resolve(request.getUri().toString());

        try {
            final ContainerRequest requestContext = new ContainerRequest(
                    baseUri,
                    requestUri,
                    request.getMethod(),
                    getSecurityContext(request),
                    new MapPropertiesDelegate());
            requestContext.setEntityStream(request.getInputStream());
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                requestContext.headers(headerName, request.getHeader(headerName));
            }
            requestContext.setWriter(responseWriter);
            requestContext.setRequestScopedInitializer(new RequestScopedInitializer() {
                @Override
                public void initialize(ServiceLocator locator) {
                    locator.<Ref<Request>>getService(RequestTYPE).set(request);
                    locator.<Ref<Response>>getService(ResponseTYPE).set(response);
                }
            });

            // Mark the request as handled before generating the body of the response
            request.setHandled(true);
            appHandler.handle(requestContext);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

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

    private final static class ResponseWriter implements ContainerResponseWriter {
        private final Response response;
        private final String method;
        private final Continuation continuation;

        ResponseWriter(Request request, Response response) {
            this.response = response;
            this.method = request.getMethod();
            this.continuation = ContinuationSupport.getContinuation(request);
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse context) throws ContainerException {
            final javax.ws.rs.core.Response.StatusType statusInfo = context.getStatusInfo();

            final int code = statusInfo.getStatusCode();
            final String reason = statusInfo.getReasonPhrase() == null ? HttpStatus.getMessage(code) : statusInfo.getReasonPhrase();

            response.setStatusWithReason(code, reason);

            if (contentLength != -1 && contentLength < Integer.MAX_VALUE) {
                response.setContentLength((int) contentLength);
            }
            for (final Map.Entry<String, List<String>> e : context.getStringHeaders().entrySet()) {
                for (final String value : e.getValue()) {
                    response.addHeader(e.getKey(), value);
                }
            }

            try {
                return response.getOutputStream();
            } catch (IOException ioe) {
                throw new ContainerException("Error during writing out the response headers.", ioe);
            }
        }

        @Override
        public boolean suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
            try {
                if (timeOut > 0) {
                    final long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeOut, timeUnit);
                    continuation.setTimeout(timeoutMillis);
                }
                continuation.addContinuationListener(new ContinuationListener() {
                    @Override
                    public void onComplete(Continuation continuation) {
                    }

                    @Override
                    public void onTimeout(Continuation continuation) {
                        if (timeoutHandler != null) {
                            timeoutHandler.onTimeout(ResponseWriter.this);
                        }
                    }
                });
                continuation.suspend();
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
            if (timeOut > 0) {
                final long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeOut, timeUnit);
                continuation.setTimeout(timeoutMillis);
            }
        }

        @Override
        public void commit() {
            try {
                if (continuation.isSuspended()) {
                    continuation.resume();
                }
                response.closeOutput();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to send 500 error response.", e);
            } finally {
                logger.debugLog("commit() called");
            }
        }

        @Override
        public void failure(Throwable error) {
            try {
                if (!response.isCommitted()) {
                    response.sendError(500, error.getMessage());
                }
            } catch (IOException e) {
                logger.debugLog("failure(...) called");
            } finally {
                logger.debugLog("failure(...) called");
                commit();
                rethrow(error);
            }

        }

        @Override
        public boolean enableResponseBuffering() {
          return false;
        }


        /**
         * Rethrow the original exception as required by JAX-RS, 3.3.4
         *
         * @param error throwable to be re-thrown
         */
        private void rethrow(Throwable error) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else {
                throw new ContainerException(error);
            }
        }

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
    public void reload(ResourceConfig configuration) {
        appHandler = new ApplicationHandler(configuration.register(new JettyBinder()));
        containerListener.onReload(this);
        containerListener = ConfigHelper.getContainerLifecycleListener(appHandler);
    }

    /**
     * Inform this container that the server has been started.
     *
     * This method must be implicitly called after the server containing this container is started.
     */
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        containerListener.onStartup(this);
    }

    /**
     * Inform this container that the server is being stopped.
     *
     * This method must be implicitly called before the server containing this container is stopped.
     */
    @Override
    public void doStop() throws Exception {
        super.doStop();
        containerListener.onShutdown(this);
        appHandler = null;
    }

    /**
     * Creates a new Jetty container.
     *
     * @param application Jersey application to be deployed on Jetty container.
     */
    JettyHttpContainer(final ApplicationHandler application) {
        this.appHandler = application;
        this.containerListener = ConfigHelper.getContainerLifecycleListener(application);

        this.appHandler.registerAdditionalBinders(new HashSet<Binder>() {{
            add(new JettyBinder());
        }});
    }
}
