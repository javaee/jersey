/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.InvocationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientRequestContext;
import org.glassfish.jersey.client.JerseyClientResponseContext;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.JaxrsRequestBuilderView;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.server.ApplicationHandler;

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
        Future<Response> responseListenableFuture;

        JaxrsRequestBuilderView requestBuilder = Requests.from(
                baseUri,
                clientRequestContext.getUri(),
                clientRequestContext.getMethod());

        for (Map.Entry<String, List<Object>> entry : clientRequestContext.getHeaders().entrySet()) {
            for (Object value : entry.getValue()) {
                requestBuilder = requestBuilder.header(entry.getKey(), value);
            }
        }

        final Request request = requestBuilder.entity(clientRequestContext.getEntity()).build();

        boolean followRedirects = PropertiesHelper.getValue(clientRequestContext.getConfiguration().getProperties(), ClientProperties.FOLLOW_REDIRECTS,
                true);

        responseListenableFuture = appHandler.apply(request);

        try {
            if (responseListenableFuture != null) {
                return tryFollowRedirects(followRedirects, createClientResponseContext(clientRequestContext, responseListenableFuture.get()), clientRequestContext);
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

    private JerseyClientResponseContext createClientResponseContext(JerseyClientRequestContext clientRequestContext, Response response) {

        final JerseyClientResponseContext clientResponseContext = new JerseyClientResponseContext(response.getStatusInfo(), clientRequestContext);

        for (Map.Entry<String, List<Object>> entry : response.getMetadata().entrySet()) {
            for (Object value : entry.getValue()) {
                // TODO value.toString?
                clientResponseContext.getHeaders().add(entry.getKey(), value.toString());
            }
        }

        response.bufferEntity();
        clientResponseContext.setEntityStream(response.readEntity(InputStream.class));

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
