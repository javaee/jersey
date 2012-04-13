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
package org.glassfish.jersey.grizzly.connector;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Configuration;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestWriter;
import org.glassfish.jersey.process.Inflector;

/**
 * The transport using the AsyncHttpClient.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class GrizzlyConnector extends RequestWriter implements Inflector<Request, Response> {

    private AsyncHttpClient client;
    private AsyncHttpClientConfig config;
    private final ExecutorService executorService;
    private static final int DEFAULT_TIMEOUT = 10000;

    /*
     * Constructs the new transport.
     */
    public GrizzlyConnector(Configuration configuration) {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

        if (configuration != null) {
            final Object threadpoolSize = configuration.getProperties().get(ClientProperties.ASYNC_THREADPOOL_SIZE);

            if (threadpoolSize != null && threadpoolSize instanceof Integer && (Integer) threadpoolSize > 0) {
                this.executorService = Executors.newFixedThreadPool((Integer) threadpoolSize);
            } else {
                this.executorService = Executors.newCachedThreadPool();
            }

            builder = builder.setExecutorService(this.executorService);

            Integer timeout = (Integer) configuration.getProperties().get(ClientProperties.CONNECT_TIMEOUT);
            if (timeout != null) {
                builder = builder.setConnectionTimeoutInMs(timeout);
            } else {
                builder = builder.setConnectionTimeoutInMs(DEFAULT_TIMEOUT);
            }
        } else {
            this.executorService = Executors.newCachedThreadPool();
            builder.setExecutorService(this.executorService);
        }

        this.config = builder.setAllowPoolingConnection(true).build();
        this.client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);
    }

    /*
     * Sends the {@link javax.ws.rs.core.Request} via Grizzly transport and returns the {@link javax.ws.rs.core.Response}.
     */
    @Override
    public Response apply(Request jerseyRequest) {
        com.ning.http.client.Response ningResponse = null;

        try {
            com.ning.http.client.Request grizzlyRequest = this.getRequest(jerseyRequest);
            Future<com.ning.http.client.Response> respFuture = client.executeRequest(grizzlyRequest);
            ningResponse = respFuture.get();

        } catch (Exception ex) {
            Logger.getLogger(GrizzlyConnector.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            client.close();
        }
        Response resp = getClientResponse(ningResponse);

        return resp;
    }

    private Response getClientResponse(com.ning.http.client.Response original) {
        Response r = null;
        try {
            ResponseBuilder builder = Response.status(original.getStatusCode()).entity(original.getResponseBody());
            builder = insertHeaders(builder, original);
            r = builder.build();
        } catch (IOException ex) {
            Logger.getLogger(GrizzlyConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!r.hasEntity()) {
            r.bufferEntity();
            r.close();
        }

        return r;
    }

    private ResponseBuilder insertHeaders(ResponseBuilder builder, com.ning.http.client.Response response) {
        for (final Map.Entry entry : response.getHeaders().entrySet()) {
            builder.header(entry.toString(), entry.getValue());
        }

        return builder;
    }

    private com.ning.http.client.Request getRequest(final Request request) {
        final String strMethod = request.getMethod();
        final URI uri = request.getUri();

        RequestBuilder builder = new RequestBuilder(strMethod).setUrl(uri.toString());

        final com.ning.http.client.Request.EntityWriter entity = this.getHttpEntity(request);

        if (entity != null) {
            builder = builder.setBody(entity);
        }
        com.ning.http.client.Request result = builder.build();
        writeOutBoundHeaders( request.getHeaders().asMap(), result);

        return result;
    }

    protected static void writeOutBoundHeaders(final MultivaluedMap<String, String> headers, final com.ning.http.client.Request request) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            List<String> vs = e.getValue();
            if (vs.size() == 1) {
                request.getHeaders().add(e.getKey(),vs.get(0));
            } else {
                StringBuilder b = new StringBuilder();
                for (Object v : e.getValue()) {
                    if (b.length() > 0) {
                        b.append(',');
                    }
                    b.append(v);
                }
                request.getHeaders().add(e.getKey(), b.toString());
            }
        }
    }

    private com.ning.http.client.Request.EntityWriter getHttpEntity(final Request jerseyRequest) {
        final Object entity = jerseyRequest.getEntity();

        if (entity == null) {
            return null;
        }
        final RequestEntityWriter rew = this.getRequestEntityWriter(jerseyRequest);

        return new com.ning.http.client.Request.EntityWriter() {
            @Override
            public void writeEntity(OutputStream out) throws IOException {
                rew.writeRequestEntity(out);
            }
        };
    }
}
