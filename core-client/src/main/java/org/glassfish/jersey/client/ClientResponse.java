/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.InboundMessageContext;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.Statuses;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

/**
 * Jersey client response context.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ClientResponse extends InboundMessageContext implements ClientResponseContext {
    private Response.StatusType status;
    private final ClientRequest requestContext;

    /**
     * Create new Jersey client response context initialized from a JAX-RS {@link Response response}.
     *
     * @param requestContext associated request context.
     * @param response       JAX-RS response to be used to initialize the response context.
     */
    public ClientResponse(final ClientRequest requestContext, final Response response) {
        this(response.getStatusInfo(), requestContext);
        this.headers(OutboundJaxrsResponse.from(response).getContext().getStringHeaders());

        final Object entity = response.getEntity();
        if (entity != null) {
            InputStream entityStream = new InputStream() {

                private ByteArrayInputStream byteArrayInputStream = null;

                @Override
                public int read() throws IOException {
                    if (byteArrayInputStream == null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        OutputStream stream = null;
                        try {
                            try {
                                stream = requestContext.getWorkers().writeTo(
                                        entity, entity.getClass(), null, null, response.getMediaType(),
                                        response.getMetadata(), requestContext.getPropertiesDelegate(), baos, false);
                            } finally {
                                if (stream != null) {
                                    stream.close();
                                }
                            }
                        } catch (IOException e) {
                            // ignore
                        }

                        byteArrayInputStream = new ByteArrayInputStream(baos.toByteArray());
                    }

                    return byteArrayInputStream.read();
                }
            };
            setEntityStream(entityStream);
        }
    }

    /**
     * Create a new Jersey client response context.
     *
     * @param status         response status.
     * @param requestContext associated client request context.
     */
    public ClientResponse(Response.StatusType status, ClientRequest requestContext) {
        this.status = status;
        this.requestContext = requestContext;
        setWorkers(requestContext.getWorkers());
    }

    @Override
    public int getStatus() {
        return status.getStatusCode();
    }

    @Override
    public void setStatus(int code) {
        this.status = Statuses.from(code);
    }

    @Override
    public void setStatusInfo(Response.StatusType status) {
        if (status == null) {
            throw new NullPointerException("Response status must not be 'null'");
        }
        this.status = status;
    }

    @Override
    public Response.StatusType getStatusInfo() {
        return status;
    }

    /**
     * Get the associated client request context paired with this response context.
     *
     * @return associated client request context.
     */
    public ClientRequest getRequestContext() {
        return requestContext;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return super.getResponseCookies();
    }

    @Override
    public Set<Link> getLinks() {
        return Sets.newHashSet(Collections2.transform(super.getLinks(), new Function<Link, Link>() {
            @Override
            public Link apply(Link link) {
                if (link.getUri().isAbsolute()) {
                    return link;
                }

                return Link.fromLink(link).baseUri(requestContext.getUri()).build();
            }
        }));
    }

    @Override
    public String toString() {
        return Objects
                .toStringHelper(this)
                .add("method", requestContext.getMethod())
                .add("uri", requestContext.getUri())
                .add("status", status.getStatusCode())
                .add("reason", status.getReasonPhrase())
                .toString();
    }
}
