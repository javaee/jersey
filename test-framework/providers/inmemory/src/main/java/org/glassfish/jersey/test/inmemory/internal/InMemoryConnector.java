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
package org.glassfish.jersey.test.inmemory.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.InvocationException;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientRequestContext;
import org.glassfish.jersey.client.JerseyClientResponseContext;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeadersFactory;
import org.glassfish.jersey.message.internal.InboundMessageContext;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.JerseyContainerRequestContext;
import org.glassfish.jersey.server.JerseyContainerResponseContext;

/**
 * In-memory client connector.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InMemoryConnector implements Inflector<JerseyClientRequestContext, JerseyClientResponseContext> {

    private final ApplicationHandler appHandler;
    private final URI baseUri;

    /**
     * Constructor.
     *
     * @param baseUri application base URI.
     * @param application RequestInvoker instance which represents application.
     */
    public InMemoryConnector(final URI baseUri, final ApplicationHandler application) {
        this.baseUri = baseUri;
        this.appHandler = application;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Transforms client-side request to server-side and invokes it on provided application ({@link RequestInvoker}
     * instance).
     *
     * @param clientRequestContext client side request to be invoked.
     */
    @Override
    public JerseyClientResponseContext apply(final JerseyClientRequestContext clientRequestContext) {
        // TODO replace request building with a common request cloning functionality
        Future<JerseyContainerResponseContext> responseListenableFuture;
        PropertiesDelegate propertiesDelegate = new MapPropertiesDelegate();

        final JerseyContainerRequestContext containerRequestContext = new JerseyContainerRequestContext(baseUri,
                clientRequestContext.getUri(), clientRequestContext.getMethod(),
                null, propertiesDelegate);
        outboundToInbound(clientRequestContext, containerRequestContext, propertiesDelegate, clientRequestContext.getWorkers(), null);

        boolean followRedirects = PropertiesHelper.getValue(clientRequestContext.getConfiguration().getProperties(),
                ClientProperties.FOLLOW_REDIRECTS, true);

        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();

        responseListenableFuture = appHandler.apply(containerRequestContext, entityStream);

        try {
            if (responseListenableFuture != null) {
                return tryFollowRedirects(followRedirects,
                        createClientResponseContext(clientRequestContext,
                                responseListenableFuture.get(),
                                propertiesDelegate,
                                containerRequestContext.getWorkers(),
                                entityStream),
                        new JerseyClientRequestContext(clientRequestContext));
            }
        } catch (InterruptedException e) {
            Logger.getLogger(InMemoryConnector.class.getName()).log(Level.SEVERE, null, e);
            throw new InvocationException("In-memory transport can't process incoming request", e);
        } catch (ExecutionException e) {
            Logger.getLogger(InMemoryConnector.class.getName()).log(Level.SEVERE, null, e);
            throw new InvocationException("In-memory transport can't process incoming request", e);
        }

        throw new InvocationException("In-memory transport can't process incoming request");
    }

    private void outboundToInbound(final OutboundMessageContext outboundContext,
                                   final InboundMessageContext inboundContext,
                                   final PropertiesDelegate propertiesDelegate,
                                   final MessageBodyWorkers workers,
                                   final ByteArrayOutputStream entityBaos
    ) {
        if (entityBaos == null) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            outboundContext.setStreamProvider(new OutboundMessageContext.StreamProvider() {
                @Override
                public OutputStream getOutputStream() throws IOException {
                    return baos;
                }

                @Override
                public void commit() throws IOException {
                }
            });

            if (outboundContext.hasEntity()) {
                OutputStream entityStream = outboundContext.getEntityStream();

                try {
                    workers.writeTo(
                            outboundContext.getEntity(),
                            outboundContext.getEntity().getClass(),
                            outboundContext.getEntityType(),
                            outboundContext.getEntityAnnotations(),
                            outboundContext.getMediaType(),
                            outboundContext.getHeaders(),
                            propertiesDelegate,
                            entityStream,
                            null,
                            true);
                } catch (IOException e) {
                    throw new InvocationException(e.getMessage(), e);
                } finally {
                    if (entityStream != null) {
                        try {
                            entityStream.close();
                        } catch (IOException ex) {
                            Logger.getLogger(InMemoryConnector.class.getName()).log(Level.FINE, "Error closing output stream", ex);
                        }
                    }
                }

                outboundContext.commitStream();

                inboundContext.setEntityStream(new ByteArrayInputStream(baos.toByteArray()));
            }
        } else {
            inboundContext.setEntityStream(new ByteArrayInputStream(entityBaos.toByteArray()));
        }

        inboundContext.getHeaders().putAll(HeadersFactory.getStringHeaders(outboundContext.getHeaders()));
    }

    private JerseyClientResponseContext createClientResponseContext(final JerseyClientRequestContext clientRequestContext,
                                                                    final JerseyContainerResponseContext containerResponseContext,
                                                                    final PropertiesDelegate propertiesDelegate,
                                                                    final MessageBodyWorkers workers,
                                                                    final ByteArrayOutputStream entityStream) {

        final JerseyClientResponseContext clientResponseContext =
                new JerseyClientResponseContext(containerResponseContext.getStatusInfo(), clientRequestContext);

        outboundToInbound(containerResponseContext, clientResponseContext, propertiesDelegate, workers, entityStream);
        clientResponseContext.setStatus(containerResponseContext.getStatus());

        return clientResponseContext;
    }

    private JerseyClientResponseContext tryFollowRedirects(boolean followRedirects, JerseyClientResponseContext response, JerseyClientRequestContext request) {
        final int statusCode = response.getStatus();
        if (!followRedirects || statusCode < 302 || statusCode > 307) {
            return response;
        }

        switch (statusCode) {
            case 303:
                request.setMethod("GET");
                // intentionally no break
            case 302:
            case 307:
                request.setUri(response.getLocation());

                return apply(request);
            default:
                return response;
        }
    }
}
