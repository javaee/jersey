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

import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.InvocationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientRequestContext;
import org.glassfish.jersey.client.JerseyClientResponseContext;
import org.glassfish.jersey.client.RequestWriter;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.process.Inflector;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;

/**
 * The transport using the AsyncHttpClient.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class GrizzlyConnector extends RequestWriter implements Inflector<JerseyClientRequestContext, JerseyClientResponseContext> {

    private AsyncHttpClient client;
    private AsyncHttpClientConfig config;
    private final ExecutorService executorService;

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

            builder.setConnectionTimeoutInMs(PropertiesHelper.getValue(configuration.getProperties(),
                    ClientProperties.CONNECT_TIMEOUT, 0));

            builder.setRequestTimeoutInMs(PropertiesHelper.getValue(configuration.getProperties(),
                    ClientProperties.READ_TIMEOUT, 0));
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
    public JerseyClientResponseContext apply(JerseyClientRequestContext clientRequestContext) {
        com.ning.http.client.Response ningResponse = null;

        try {
            com.ning.http.client.Request grizzlyRequest = this.getRequest(clientRequestContext);
            Future<com.ning.http.client.Response> respFuture = client.executeRequest(grizzlyRequest);
            ningResponse = respFuture.get();
        } catch (ExecutionException ex) {
            Throwable e = ex.getCause() == null ? ex : ex.getCause();
            throw new InvocationException(e.getMessage(), e);
        } catch (Exception ex) {
            throw new InvocationException(ex.getMessage(), ex);
        } finally {
            client.close();
        }
        JerseyClientResponseContext clientResponseContext = getClientResponse(clientRequestContext, ningResponse);

        return clientResponseContext;
    }

    private JerseyClientResponseContext getClientResponse(JerseyClientRequestContext clientRequestContext, final com.ning.http.client.Response original) {

        final JerseyClientResponseContext clientResponseContext = new JerseyClientResponseContext(new Response.StatusType() {
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
        }, clientRequestContext);

        for (Map.Entry<String, List<String>> entry : original.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                // TODO value.toString?
                clientResponseContext.getHeaders().add(entry.getKey(), value);
            }
        }

        try {
            clientResponseContext.setEntityStream(original.getResponseBodyAsStream());
        } catch (IOException e) {
            Logger.getLogger(GrizzlyConnector.class.getName()).log(Level.SEVERE, null, e);
        }

        return clientResponseContext;
    }

    private com.ning.http.client.Request getRequest(final JerseyClientRequestContext clientRequestContext) {
        final String strMethod = clientRequestContext.getMethod();
        final URI uri = clientRequestContext.getUri();

        RequestBuilder builder = new RequestBuilder(strMethod).setUrl(uri.toString());

        builder.setFollowRedirects(PropertiesHelper.getValue(clientRequestContext.getConfiguration().getProperties(), ClientProperties.FOLLOW_REDIRECTS,
                true));

        final com.ning.http.client.Request.EntityWriter entity = this.getHttpEntity(clientRequestContext);

        if (entity != null) {
            builder = builder.setBody(entity);
        }

        com.ning.http.client.Request result = builder.build();
        writeOutBoundHeaders(clientRequestContext.getHeaders(), result);

        return result;
    }

    protected static void writeOutBoundHeaders(final MultivaluedMap<String, Object> headers, final com.ning.http.client.Request request) {
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

    private com.ning.http.client.Request.EntityWriter getHttpEntity(final JerseyClientRequestContext clientRequestContext) {
        final Object entity = clientRequestContext.getEntity();

        if (entity == null) {
            return null;
        }

        final RequestEntityWriter rew = this.getRequestEntityWriter(clientRequestContext);

        return new com.ning.http.client.Request.EntityWriter() {
            @Override
            public void writeEntity(OutputStream out) throws IOException {
                rew.writeRequestEntity(out);
            }
        };
    }
}
