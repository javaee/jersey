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
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.server.ApplicationHandler;

/**
 * In-memory client transport.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InMemoryTransport implements Inflector<Request, Response> {

    private final ApplicationHandler appHandler;
    private final URI baseUri;

    /**
     * Constructor.
     *
     * @param baseUri application base URI.
     * @param application RequestInvoker instance which represents application.
     */
    public InMemoryTransport(final URI baseUri, final ApplicationHandler application) {
        this.baseUri = baseUri;
        this.appHandler = application;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Transforms client-side request to server-side and invokes it on provided application ({@link RequestInvoker}
     * instance).
     *
     * @param request client side request to be invoked.
     */
    @Override
    public Response apply(final Request request) {
        // TODO replace request building with a common request cloning functionality
        Future<Response> responseListenableFuture;

        Request.RequestBuilder requestBuilder = Requests.from(
                baseUri,
                request.getUri(),
                request.getMethod());

        for (Map.Entry<String, List<String>> entry : request.getHeaders().asMap().entrySet()) {
            for (String value : entry.getValue()) {
                requestBuilder = requestBuilder.header(entry.getKey(), value);
            }
        }

        final Request request1 = requestBuilder.entity(request.getEntity()).build();

        boolean followRedirects = PropertiesHelper.getValue(request.getProperties(), ClientProperties.FOLLOW_REDIRECTS,
                true);

        responseListenableFuture = appHandler.apply(request1);

        try {
            if (responseListenableFuture != null) {
                return tryFollowRedirects(followRedirects, responseListenableFuture.get(), request1);
            }
        } catch (InterruptedException e) {
            Logger.getLogger(InMemoryTransport.class.getName()).log(Level.SEVERE, null, e);
            throw new InvocationException("In-memory transport can't process incoming request", e);
        } catch (ExecutionException e) {
            Logger.getLogger(InMemoryTransport.class.getName()).log(Level.SEVERE, null, e);
            throw new InvocationException("In-memory transport can't process incoming request", e);
        }

        throw new InvocationException("In-memory transport can't process incoming request");
    }

    private Response tryFollowRedirects(boolean followRedirects, Response response, Request request) {
        if (!followRedirects || response.getStatus() < 302 || response.getStatus() > 307) {
            return response;
        }

        Request.RequestBuilder rb = Requests.from(request);

        switch (response.getStatus()) {
            case 303:
                rb.method("GET");
                // intentionally no break
            case 302:
            case 307:
                rb.redirect(response.getHeaders().getLocation());
                return apply(rb.build());
            default:
                return response;
        }
    }
}
