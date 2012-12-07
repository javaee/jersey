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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.RequestWriter;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;

import org.glassfish.grizzly.http.client.Version;

import com.google.common.util.concurrent.SettableFuture;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;

/**
 * The transport using the AsyncHttpClient.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class GrizzlyConnector extends RequestWriter implements Connector {

    private AsyncHttpClient client;

    /**
     * Create the new Grizzly async client connector.
     *
     * @param config client configuration.
     */
    public GrizzlyConnector(Configuration config) {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

        ExecutorService executorService;
        if (config != null) {
            final Object threadPoolSize = config.getProperties().get(ClientProperties.ASYNC_THREADPOOL_SIZE);

            if (threadPoolSize != null && threadPoolSize instanceof Integer && (Integer) threadPoolSize > 0) {
                executorService = Executors.newFixedThreadPool((Integer) threadPoolSize);
            } else {
                executorService = Executors.newCachedThreadPool();
            }

            builder = builder.setExecutorService(executorService);

            builder.setConnectionTimeoutInMs(PropertiesHelper.getValue(config.getProperties(),
                    ClientProperties.CONNECT_TIMEOUT, 0));

            builder.setRequestTimeoutInMs(PropertiesHelper.getValue(config.getProperties(),
                    ClientProperties.READ_TIMEOUT, 0));
        } else {
            executorService = Executors.newCachedThreadPool();
            builder.setExecutorService(executorService);
        }

        AsyncHttpClientConfig asyncClientConfig = builder.setAllowPoolingConnection(true).build();
        this.client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(asyncClientConfig), asyncClientConfig);
    }

    /*
     * Sends the {@link javax.ws.rs.core.Request} via Grizzly transport and returns the {@link javax.ws.rs.core.Response}.
     */
    @Override
    public ClientResponse apply(ClientRequest requestContext) {
        com.ning.http.client.Response connectorResponse;

        try {
            com.ning.http.client.Request connectorRequest = translate(requestContext);
            Future<com.ning.http.client.Response> respFuture = client.executeRequest(connectorRequest);
            connectorResponse = respFuture.get();
        } catch (ExecutionException ex) {
            Throwable e = ex.getCause() == null ? ex : ex.getCause();
            throw new ClientException(e.getMessage(), e);
        } catch (InterruptedException ex) {
            throw new ClientException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ClientException(ex.getMessage(), ex);
        }

        return translate(requestContext, connectorResponse);
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        final Request connectorRequest = translate(request);

        Throwable failure;
        try {
            return client.executeRequest(connectorRequest, new AsyncCompletionHandler<ClientResponse>() {
                @Override
                public ClientResponse onCompleted(com.ning.http.client.Response connectorResponse) throws Exception {
                    final ClientResponse response = translate(request, connectorResponse);
                    try {
                        return response;
                    } finally {
                        callback.response(response);
                    }
                }

                @Override
                public void onThrowable(Throwable t) {
                    t = t instanceof IOException ? new ClientException(t.getMessage(), t) : t;
                    callback.failure(t);
                }
            });
        } catch (IOException ex) {
            failure = ex;
            callback.failure(new ClientException(ex.getMessage(), ex.getCause()));
        } catch (Throwable t) {
            failure = t;
            callback.failure(t);
        }

        final SettableFuture<Object> errorFuture = SettableFuture.create();
        errorFuture.setException(failure);
        return errorFuture;
    }

    @Override
    public void close() {
        client.close();
    }

    private ClientResponse translate(ClientRequest requestContext, final com.ning.http.client.Response original) {

        final ClientResponse responseContext = new ClientResponse(new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return original.getStatusCode();
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.familyOf(original.getStatusCode());
            }

            @Override
            public String getReasonPhrase() {
                return original.getStatusText();
            }
        }, requestContext);

        for (Map.Entry<String, List<String>> entry : original.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                // TODO value.toString?
                responseContext.getHeaders().add(entry.getKey(), value);
            }
        }

        try {
            responseContext.setEntityStream(original.getResponseBodyAsStream());
        } catch (IOException e) {
            Logger.getLogger(GrizzlyConnector.class.getName()).log(Level.SEVERE, null, e);
        }

        return responseContext;
    }

    private com.ning.http.client.Request translate(final ClientRequest requestContext) {
        final String strMethod = requestContext.getMethod();
        final URI uri = requestContext.getUri();

        RequestBuilder builder = new RequestBuilder(strMethod).setUrl(uri.toString());

        builder.setFollowRedirects(PropertiesHelper.getValue(requestContext.getConfiguration().getProperties(), ClientProperties.FOLLOW_REDIRECTS,
                true));

        final com.ning.http.client.Request.EntityWriter entity = this.getHttpEntity(requestContext);

        if (entity != null) {
            builder = builder.setBody(entity);
        }

        com.ning.http.client.Request result = builder.build();
        writeOutBoundHeaders(requestContext.getHeaders(), result);

        return result;
    }

    private static void writeOutBoundHeaders(final MultivaluedMap<String, Object> headers, final com.ning.http.client.Request request) {
        for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
            List<Object> vs = e.getValue();
            if (vs.size() == 1) {
                request.getHeaders().add(e.getKey(), vs.get(0).toString());
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

    private com.ning.http.client.Request.EntityWriter getHttpEntity(final ClientRequest requestContext) {
        final Object entity = requestContext.getEntity();

        if (entity == null) {
            return null;
        }

        final RequestEntityWriter rew = this.getRequestEntityWriter(requestContext);

        return new com.ning.http.client.Request.EntityWriter() {
            @Override
            public void writeEntity(OutputStream out) throws IOException {
                rew.writeRequestEntity(out);
            }
        };
    }

    @Override
    public String getName() {
        return String.format("Grizzly Http Client %d.%d", Version.MAJOR_VERSION, Version.MINOR_VERSION);
    }
}
