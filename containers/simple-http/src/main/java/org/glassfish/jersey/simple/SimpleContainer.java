/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.simple;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;

import javax.ws.rs.core.SecurityContext;

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

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.simpleframework.http.Address;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

/**
 * Simple Jersey HTTP Container.
 *
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public final class SimpleContainer implements org.simpleframework.http.core.Container, Container {
    private static final ExtendedLogger logger =
            new ExtendedLogger(Logger.getLogger(SimpleContainer.class.getName()), Level.FINEST);

    /**
     * Referencing factory for Simple request.
     */
    private static class SimpleRequestReferencingFactory extends ReferencingFactory<Request> {
        @Inject
        public SimpleRequestReferencingFactory(Provider<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * Referencing factory for Simple response.
     */
    private static class SimpleResponseReferencingFactory extends ReferencingFactory<Response> {
        @Inject
        public SimpleResponseReferencingFactory(Provider<Ref<Response>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * An internal binder to enable Simple HTTP container specific types injection.
     * This binder allows to inject underlying Grizzly HTTP request and response instances.
     */
    private static class SimpleBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(SimpleRequestReferencingFactory.class).to(Request.class).in(PerLookup.class);
            bindFactory(ReferencingFactory.<Request>referenceFactory()).to(new TypeLiteral<Ref<Request>>() {
            }).in(RequestScoped.class);

            bindFactory(SimpleResponseReferencingFactory.class).to(Response.class).in(PerLookup.class);
            bindFactory(ReferencingFactory.<Response>referenceFactory()).to(new TypeLiteral<Ref<Response>>() {
            }).in(RequestScoped.class);
        }
    }

    private volatile ApplicationHandler appHandler;
    private volatile ContainerLifecycleListener containerListener;

    private final static class Writer implements ContainerResponseWriter {
        private final Response response;
        private final Request request;

        Writer(Request request, Response response) {
            this.response = response;
            this.request = request;
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse context) throws ContainerException {
            int code = context.getStatus();
            String text = Status.getDescription(code);
            String method = request.getMethod();

            response.setCode(code);
            response.setDescription(text);

            if (!method.equalsIgnoreCase("HEAD") && contentLength != -1 && contentLength < Integer.MAX_VALUE) {
                response.setContentLength((int) contentLength);
            }
            for (final Map.Entry<String, List<String>> e : context.getStringHeaders().entrySet()) {
                for (final String value : e.getValue()) {
                    response.addValue(e.getKey(), value);
                }
            }

            try {
                return response.getOutputStream();
            } catch (IOException ioe) {
                throw new ContainerException("Error during writing out the response headers.", ioe);
            }
        }

        @Override
        public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
            throw new UnsupportedOperationException("Method suspend is not supported by the container.");
        }

        @Override
        public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
            throw new UnsupportedOperationException("Method suspend is not supported by the container.");
        }

        @Override
        public void commit() {
            try {
                response.close();
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
                    response.setCode(500);
                    response.setDescription(error.getMessage());
                }
            } finally {
                logger.debugLog("failure(...) called");
                commit();
                rethrow(error);
            }

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

    public void handle(final Request request, final Response response) {
        final Writer responseWriter = new Writer(request, response);
        final URI baseUri = getBaseUri(request);
        final URI requestUri = baseUri.resolve(request.getTarget());

        try {
            final ContainerRequest requestContext = new ContainerRequest(
                    baseUri,
                    requestUri,
                    request.getMethod(),
                    getSecurityContext(request),
                    new MapPropertiesDelegate());
            requestContext.setEntityStream(request.getInputStream());
            for (String headerName : request.getNames()) {
                requestContext.headers(headerName, request.getValue(headerName));
            }
            requestContext.setWriter(responseWriter);
            requestContext.setRequestScopedInitializer(new RequestScopedInitializer() {
                @Override
                public void initialize(ServiceLocator locator) {
                    locator.<Ref<Request>>getService((new TypeLiteral<Ref<Request>>() {
                    }).getType()).set(request);
                    locator.<Ref<Response>>getService((new TypeLiteral<Ref<Response>>() {
                    }).getType()).set(response);
                }
            });

            appHandler.handle(requestContext);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            close(response);
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
                return request.getSecuritySession().getLocalPrincipal();
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };
    }


    private void close(Response response) {
        try {
            response.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private URI getBaseUri(Request request) {
        try {
            final Address address = request.getAddress();

            return new URI(
                    address.getScheme(),
                    null,
                    address.getDomain(),
                    address.getPort(),
                    "/",
                    null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
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
        appHandler = new ApplicationHandler(configuration.register(new SimpleBinder()));
        containerListener.onReload(this);
        containerListener = ConfigHelper.getContainerLifecycleListener(appHandler);
    }

    /**
     * Inform this container that the server was started. This method must be implicitly called after
     * the server containing this container is started.
     */
    void onServerStart() {
        this.containerListener.onStartup(this);
    }


    /**
     * Creates a new Grizzly container.
     *
     * @param application Jersey application to be deployed on Grizzly container.
     */
    SimpleContainer(final ApplicationHandler application) {
        this.appHandler = application;
        this.containerListener = ConfigHelper.getContainerLifecycleListener(application);

        this.appHandler.registerAdditionalBinders(new HashSet<Binder>() {{
            add(new SimpleBinder());
        }});
    }

}
