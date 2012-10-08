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

import javax.ws.rs.client.ClientException;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.InboundMessageContext;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * In-memory client connector.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InMemoryConnector implements Connector {

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
     * Transforms client-side request to server-side and invokes it on provided application ({@link ApplicationHandler}
     * instance).
     *
     * @param requestContext client side request to be invoked.
     */
    @Override
    public ClientResponse apply(final ClientRequest requestContext) {
        // TODO replace request building with a common request cloning functionality
        Future<ContainerResponse> responseListenableFuture;
        PropertiesDelegate propertiesDelegate = new MapPropertiesDelegate();

        final ContainerRequest containerRequestContext = new ContainerRequest(baseUri,
                requestContext.getUri(), requestContext.getMethod(),
                null, propertiesDelegate);
        outboundToInbound(requestContext, containerRequestContext, propertiesDelegate, requestContext.getWorkers(), null);

        boolean followRedirects = PropertiesHelper.getValue(requestContext.getConfiguration().getProperties(),
                ClientProperties.FOLLOW_REDIRECTS, true);

        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();

        responseListenableFuture = appHandler.apply(containerRequestContext, entityStream);

        try {
            if (responseListenableFuture != null) {
                return tryFollowRedirects(followRedirects,
                        createClientResponseContext(requestContext,
                                responseListenableFuture.get(),
                                propertiesDelegate,
                                containerRequestContext.getWorkers(),
                                entityStream),
                        new ClientRequest(requestContext));
            }
        } catch (InterruptedException e) {
            Logger.getLogger(InMemoryConnector.class.getName()).log(Level.SEVERE, null, e);
            throw new ClientException("In-memory transport can't process incoming request", e);
        } catch (ExecutionException e) {
            Logger.getLogger(InMemoryConnector.class.getName()).log(Level.SEVERE, null, e);
            throw new ClientException("In-memory transport can't process incoming request", e);
        }

        throw new ClientException("In-memory transport can't process incoming request");
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        return MoreExecutors.sameThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.response(apply(request));
                } catch (ClientException ex) {
                    throw ex;
                } catch (Throwable t) {
                    callback.failure(t);
                }
            }
        });
    }

    @Override
    public void close() {
        // do nothing
    }

    private void outboundToInbound(final OutboundMessageContext outboundContext,
                                   final InboundMessageContext inboundContext,
                                   final PropertiesDelegate propertiesDelegate,
                                   final MessageBodyWorkers workers,
                                   final ByteArrayOutputStream entityBaos) {
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
                    entityStream = workers.writeTo(
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
                    outboundContext.setEntityStream(entityStream);
                    outboundContext.commitStream();
                } catch (IOException e) {
                    throw new ClientException(e.getMessage(), e);
                } finally {
                    if (entityStream != null) {
                        try {
                            entityStream.close();
                        } catch (IOException ex) {
                            Logger.getLogger(InMemoryConnector.class.getName()).log(Level.FINE, "Error closing output stream", ex);
                        }
                    }
                }

                inboundContext.setEntityStream(new ByteArrayInputStream(baos.toByteArray()));
            }
        } else {
            inboundContext.setEntityStream(new ByteArrayInputStream(entityBaos.toByteArray()));
        }

        inboundContext.getHeaders().putAll(outboundContext.getStringHeaders());
    }

    private ClientResponse createClientResponseContext(final ClientRequest requestContext,
                                                                    final ContainerResponse containerResponseContext,
                                                                    final PropertiesDelegate propertiesDelegate,
                                                                    final MessageBodyWorkers workers,
                                                                    final ByteArrayOutputStream entityStream) {

        final ClientResponse responseContext =
                new ClientResponse(containerResponseContext.getStatusInfo(), requestContext);

        outboundToInbound(
                containerResponseContext.getWrappedMessageContext(), responseContext, propertiesDelegate, workers, entityStream);
        responseContext.setStatus(containerResponseContext.getStatus());

        return responseContext;
    }

    private ClientResponse tryFollowRedirects(boolean followRedirects, ClientResponse response, ClientRequest request) {
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

    @Override
    public String getName() {
        return "Jersey InMemory Container Client";
    }
}
