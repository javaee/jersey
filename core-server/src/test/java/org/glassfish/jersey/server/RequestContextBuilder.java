/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeaderUtils;

/**
 * Used by tests to create mock JerseyContainerRequestContext instances.
 *
 * @author Martin Matula
 */
public class RequestContextBuilder {

    public class TestContainerRequest extends ContainerRequest {

        private Object entity;
        private GenericType entityType;
        private final PropertiesDelegate propertiesDelegate;

        public TestContainerRequest(final URI baseUri,
                                    final URI requestUri,
                                    final String method,
                                    final SecurityContext securityContext,
                                    final PropertiesDelegate propertiesDelegate) {
            super(baseUri, requestUri, method, securityContext, propertiesDelegate);
            this.propertiesDelegate = propertiesDelegate;
        }

        public void setEntity(final Object entity) {
            if (entity instanceof GenericEntity) {
                this.entity = ((GenericEntity) entity).getEntity();
                this.entityType = new GenericType(((GenericEntity) entity).getType());
            } else {
                this.entity = entity;
                this.entityType = new GenericType(entity.getClass());
            }
        }

        @Override
        public void setWorkers(final MessageBodyWorkers workers) {
            super.setWorkers(workers);
            final byte[] entityBytes;
            if (entity != null) {
                final MultivaluedMap<String, Object> myMap = new MultivaluedHashMap<String, Object>(getHeaders());
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStream stream = null;
                try {
                    stream = workers.writeTo(entity, entity.getClass(), entityType.getType(),
                            new Annotation[0], getMediaType(),
                            myMap,
                            propertiesDelegate, baos, Collections.<WriterInterceptor>emptyList());
                } catch (final IOException | WebApplicationException ex) {
                    Logger.getLogger(TestContainerRequest.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (final IOException e) {
                            // ignore
                        }
                    }
                }
                entityBytes = baos.toByteArray();
            } else {
                entityBytes = new byte[0];
            }
            setEntityStream(new ByteArrayInputStream(entityBytes));
        }
    }

    private final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
    private final TestContainerRequest request;

    public static RequestContextBuilder from(final String requestUri, final String method) {
        return from(null, requestUri, method);
    }

    public static RequestContextBuilder from(final String baseUri, final String requestUri, final String method) {
        return new RequestContextBuilder(baseUri, requestUri, method);
    }

    public static RequestContextBuilder from(final URI requestUri, final String method) {
        return from(null, requestUri, method);
    }

    public static RequestContextBuilder from(final URI baseUri, final URI requestUri, final String method) {
        return new RequestContextBuilder(baseUri, requestUri, method);
    }

    private RequestContextBuilder(final String baseUri, final String requestUri, final String method) {
        this(baseUri == null || baseUri.isEmpty() ? null : URI.create(baseUri), URI.create(requestUri), method);
    }

    private RequestContextBuilder(final URI baseUri, final URI requestUri, final String method) {
        request = new TestContainerRequest(baseUri, requestUri, method, null,
                new MapPropertiesDelegate());
    }

    public ContainerRequest build() {
        return request;
    }

    public RequestContextBuilder accept(final String... acceptHeader) {
        putHeaders(HttpHeaders.ACCEPT, acceptHeader);
        return this;
    }

    public RequestContextBuilder accept(final MediaType... acceptHeader) {
        putHeaders(HttpHeaders.ACCEPT, (Object[]) acceptHeader);
        return this;
    }

    public RequestContextBuilder entity(final Object entity) {
        request.setEntity(entity);
        return this;
    }

    public RequestContextBuilder type(final String contentType) {
        request.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, contentType);
        return this;

    }

    public RequestContextBuilder type(final MediaType contentType) {
        request.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, HeaderUtils.asString(contentType, delegate));
        return this;
    }

    public RequestContextBuilder header(final String name, final Object value) {
        putHeader(name, value);
        return this;
    }

    public RequestContextBuilder cookie(final Cookie cookie) {
        putHeader(HttpHeaders.COOKIE, cookie);
        return this;
    }

    public RequestContextBuilder cookies(final Cookie... cookies) {
        putHeaders(HttpHeaders.COOKIE, (Object[]) cookies);
        return this;
    }

    private void putHeader(final String name, final Object value) {
        if (value == null) {
            request.getHeaders().remove(name);
            return;
        }
        request.header(name, HeaderUtils.asString(value, delegate));
    }

    private void putHeaders(final String name, final Object... values) {
        if (values == null) {
            request.getHeaders().remove(name);
            return;
        }
        request.getHeaders().addAll(name, HeaderUtils.asStringList(Arrays.asList(values), delegate));
    }

    private void putHeaders(final String name, final String... values) {
        if (values == null) {
            request.getHeaders().remove(name);
            return;
        }
        request.getHeaders().addAll(name, values);
    }
}
