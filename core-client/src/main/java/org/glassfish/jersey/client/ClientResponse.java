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
package org.glassfish.jersey.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.ServiceLocatorSupplier;
import org.glassfish.jersey.message.internal.InboundMessageContext;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.Statuses;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.base.MoreObjects;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Jersey client response context.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ClientResponse extends InboundMessageContext implements ClientResponseContext, ServiceLocatorSupplier {

    private Response.StatusType status;
    private final ClientRequest requestContext;
    private URI resolvedUri;

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
                                        response.getMetadata(), requestContext.getPropertiesDelegate(), baos,
                                        Collections.<WriterInterceptor>emptyList());
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
        this(status, requestContext, requestContext.getUri());
    }

    /**
     * Create a new Jersey client response context.
     *
     * @param status             response status.
     * @param requestContext     associated client request context.
     * @param resolvedRequestUri resolved request URI (see {@link #getResolvedRequestUri()}).
     */
    public ClientResponse(Response.StatusType status, ClientRequest requestContext, URI resolvedRequestUri) {
        this.status = status;
        this.resolvedUri = resolvedRequestUri;
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
            throw new NullPointerException(LocalizationMessages.CLIENT_RESPONSE_STATUS_NULL());
        }
        this.status = status;
    }

    @Override
    public Response.StatusType getStatusInfo() {
        return status;
    }

    /**
     * Get the absolute URI of the ultimate request made to receive this response.
     * <p>
     * The returned URI points to the ultimate location of the requested resource that
     * provided the data represented by this response instance. Because Jersey client connectors
     * may be configured to {@link ClientProperties#FOLLOW_REDIRECTS
     * automatically follow redirect responses}, the value of the URI returned by this method may
     * be different from the value of the {@link javax.ws.rs.client.ClientRequestContext#getUri()
     * original request URI} that can be retrieved using {@code response.getRequestContext().getUri()}
     * chain of method calls.
     * </p>
     *
     * @return absolute URI of the ultimate request made to receive this response.
     *
     * @see ClientProperties#FOLLOW_REDIRECTS
     * @see #setResolvedRequestUri(java.net.URI)
     * @since 2.6
     */
    public URI getResolvedRequestUri() {
        return resolvedUri;
    }

    /**
     * Set the absolute URI of the ultimate request that was made to receive this response.
     * <p>
     * If the original request URI has been modified (e.g. due to redirections), the absolute URI of
     * the ultimate request being made to receive the response should be set by the caller
     * on the response instance using this method.
     * </p>
     *
     * @param uri absolute URI of the ultimate request made to receive this response. Must not be {@code null}.
     * @throws java.lang.NullPointerException     in case the passed {@code uri} parameter is null.
     * @throws java.lang.IllegalArgumentException in case the passed {@code uri} parameter does
     *                                            not represent an absolute URI.
     * @see ClientProperties#FOLLOW_REDIRECTS
     * @see #getResolvedRequestUri()
     * @since 2.6
     */
    public void setResolvedRequestUri(final URI uri) {
        if (uri == null) {
            throw new NullPointerException(LocalizationMessages.CLIENT_RESPONSE_RESOLVED_URI_NULL());
        }
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException(LocalizationMessages.CLIENT_RESPONSE_RESOLVED_URI_NOT_ABSOLUTE());
        }
        this.resolvedUri = uri;
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

                return Link.fromLink(link).baseUri(getResolvedRequestUri()).build();
            }
        }));
    }

    @Override
    public String toString() {
        return MoreObjects
                .toStringHelper(this)
                .add("method", requestContext.getMethod())
                .add("uri", requestContext.getUri())
                .add("status", status.getStatusCode())
                .add("reason", status.getReasonPhrase())
                .toString();
    }

    /**
     * Get the message entity Java instance. Returns {@code null} if the message
     * does not contain an entity body.
     * <p>
     * If the entity is represented by an un-consumed {@link InputStream input stream}
     * the method will return the input stream.
     * </p>
     *
     * @return the message entity or {@code null} if message does not contain an
     *         entity body (i.e. when {@link #hasEntity()} returns {@code false}).
     * @throws IllegalStateException if the entity was previously fully consumed
     *                               as an {@link InputStream input stream}, or
     *                               if the response has been {@link #close() closed}.
     * @see javax.ws.rs.core.Response#getEntity()
     * @since 2.5
     */
    public Object getEntity() throws IllegalStateException {
        // TODO implement some advanced caching support?
        return getEntityStream();
    }

    /**
     * Read the message entity input stream as an instance of specified Java type
     * using a {@link javax.ws.rs.ext.MessageBodyReader} that supports mapping the
     * message entity stream onto the requested type.
     * <p>
     * Method throws an {@link ProcessingException} if the content of the
     * message cannot be mapped to an entity of the requested type and
     * {@link IllegalStateException} in case the entity is not backed by an input
     * stream or if the original entity input stream has already been consumed
     * without {@link #bufferEntity() buffering} the entity data prior consuming.
     * </p>
     * <p>
     * A message instance returned from this method will be cached for
     * subsequent retrievals via {@link #getEntity()}. Unless the supplied entity
     * type is an {@link java.io.InputStream input stream}, this method automatically
     * {@link #close() closes} the an unconsumed original response entity data stream
     * if open. In case the entity data has been buffered, the buffer will be reset
     * prior consuming the buffered data to enable subsequent invocations of
     * {@code readEntity(...)} methods on this response.
     * </p>
     *
     * @param <T>        entity instance Java type.
     * @param entityType the type of entity.
     * @return the message entity; for a zero-length response entities returns a corresponding
     *         Java object that represents zero-length data. In case no zero-length representation
     *         is defined for the Java type, a {@link ProcessingException} wrapping the
     *         underlying {@link javax.ws.rs.core.NoContentException} is thrown.
     * @throws ProcessingException   if the content of the message cannot be
     *                               mapped to an entity of the requested type.
     * @throws IllegalStateException if the entity is not backed by an input stream,
     *                               the response has been {@link #close() closed} already,
     *                               or if the entity input stream has been fully consumed already and has
     *                               not been buffered prior consuming.
     * @see javax.ws.rs.ext.MessageBodyReader
     * @see javax.ws.rs.core.Response#readEntity(Class)
     * @since 2.5
     */
    public <T> T readEntity(Class<T> entityType) throws ProcessingException, IllegalStateException {
        return readEntity(entityType, requestContext.getPropertiesDelegate());
    }

    /**
     * Read the message entity input stream as an instance of specified Java type
     * using a {@link javax.ws.rs.ext.MessageBodyReader} that supports mapping the
     * message entity stream onto the requested type.
     * <p>
     * Method throws an {@link ProcessingException} if the content of the
     * message cannot be mapped to an entity of the requested type and
     * {@link IllegalStateException} in case the entity is not backed by an input
     * stream or if the original entity input stream has already been consumed
     * without {@link #bufferEntity() buffering} the entity data prior consuming.
     * </p>
     * <p>
     * A message instance returned from this method will be cached for
     * subsequent retrievals via {@link #getEntity()}. Unless the supplied entity
     * type is an {@link java.io.InputStream input stream}, this method automatically
     * {@link #close() closes} the an unconsumed original response entity data stream
     * if open. In case the entity data has been buffered, the buffer will be reset
     * prior consuming the buffered data to enable subsequent invocations of
     * {@code readEntity(...)} methods on this response.
     * </p>
     *
     * @param <T>        entity instance Java type.
     * @param entityType the type of entity; may be generic.
     * @return the message entity; for a zero-length response entities returns a corresponding
     *         Java object that represents zero-length data. In case no zero-length representation
     *         is defined for the Java type, a {@link ProcessingException} wrapping the
     *         underlying {@link javax.ws.rs.core.NoContentException} is thrown.
     * @throws ProcessingException   if the content of the message cannot be
     *                               mapped to an entity of the requested type.
     * @throws IllegalStateException if the entity is not backed by an input stream,
     *                               the response has been {@link #close() closed} already,
     *                               or if the entity input stream has been fully consumed already and has
     *                               not been buffered prior consuming.
     * @see javax.ws.rs.ext.MessageBodyReader
     * @see javax.ws.rs.core.Response#readEntity(javax.ws.rs.core.GenericType)
     * @since 2.5
     */
    @SuppressWarnings("unchecked")
    public <T> T readEntity(GenericType<T> entityType) throws ProcessingException, IllegalStateException {
        return (T) readEntity(entityType.getRawType(), entityType.getType(), requestContext.getPropertiesDelegate());
    }

    /**
     * Read the message entity input stream as an instance of specified Java type
     * using a {@link javax.ws.rs.ext.MessageBodyReader} that supports mapping the
     * message entity stream onto the requested type.
     * <p>
     * Method throws an {@link ProcessingException} if the content of the
     * message cannot be mapped to an entity of the requested type and
     * {@link IllegalStateException} in case the entity is not backed by an input
     * stream or if the original entity input stream has already been consumed
     * without {@link #bufferEntity() buffering} the entity data prior consuming.
     * </p>
     * <p>
     * A message instance returned from this method will be cached for
     * subsequent retrievals via {@link #getEntity()}. Unless the supplied entity
     * type is an {@link java.io.InputStream input stream}, this method automatically
     * {@link #close() closes} the an unconsumed original response entity data stream
     * if open. In case the entity data has been buffered, the buffer will be reset
     * prior consuming the buffered data to enable subsequent invocations of
     * {@code readEntity(...)} methods on this response.
     * </p>
     *
     * @param <T>         entity instance Java type.
     * @param entityType  the type of entity.
     * @param annotations annotations that will be passed to the {@link javax.ws.rs.ext.MessageBodyReader}.
     * @return the message entity; for a zero-length response entities returns a corresponding
     *         Java object that represents zero-length data. In case no zero-length representation
     *         is defined for the Java type, a {@link ProcessingException} wrapping the
     *         underlying {@link javax.ws.rs.core.NoContentException} is thrown.
     * @throws ProcessingException   if the content of the message cannot be
     *                               mapped to an entity of the requested type.
     * @throws IllegalStateException if the entity is not backed by an input stream,
     *                               the response has been {@link #close() closed} already,
     *                               or if the entity input stream has been fully consumed already and has
     *                               not been buffered prior consuming.
     * @see javax.ws.rs.ext.MessageBodyReader
     * @see javax.ws.rs.core.Response#readEntity(Class, java.lang.annotation.Annotation[])
     * @since 2.5
     */
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) throws ProcessingException, IllegalStateException {
        return readEntity(entityType, annotations, requestContext.getPropertiesDelegate());
    }

    /**
     * Read the message entity input stream as an instance of specified Java type
     * using a {@link javax.ws.rs.ext.MessageBodyReader} that supports mapping the
     * message entity stream onto the requested type.
     * <p>
     * Method throws an {@link ProcessingException} if the content of the
     * message cannot be mapped to an entity of the requested type and
     * {@link IllegalStateException} in case the entity is not backed by an input
     * stream or if the original entity input stream has already been consumed
     * without {@link #bufferEntity() buffering} the entity data prior consuming.
     * </p>
     * <p>
     * A message instance returned from this method will be cached for
     * subsequent retrievals via {@link #getEntity()}. Unless the supplied entity
     * type is an {@link java.io.InputStream input stream}, this method automatically
     * {@link #close() closes} the an unconsumed original response entity data stream
     * if open. In case the entity data has been buffered, the buffer will be reset
     * prior consuming the buffered data to enable subsequent invocations of
     * {@code readEntity(...)} methods on this response.
     * </p>
     *
     * @param <T>         entity instance Java type.
     * @param entityType  the type of entity; may be generic.
     * @param annotations annotations that will be passed to the {@link javax.ws.rs.ext.MessageBodyReader}.
     * @return the message entity; for a zero-length response entities returns a corresponding
     *         Java object that represents zero-length data. In case no zero-length representation
     *         is defined for the Java type, a {@link ProcessingException} wrapping the
     *         underlying {@link javax.ws.rs.core.NoContentException} is thrown.
     * @throws ProcessingException   if the content of the message cannot be
     *                               mapped to an entity of the requested type.
     * @throws IllegalStateException if the entity is not backed by an input stream,
     *                               the response has been {@link #close() closed} already,
     *                               or if the entity input stream has been fully consumed already and has
     *                               not been buffered prior consuming.
     * @see javax.ws.rs.ext.MessageBodyReader
     * @see javax.ws.rs.core.Response#readEntity(javax.ws.rs.core.GenericType, java.lang.annotation.Annotation[])
     * @since 2.5
     */
    @SuppressWarnings("unchecked")
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations)
            throws ProcessingException, IllegalStateException {
        return (T) readEntity(entityType.getRawType(), entityType.getType(), annotations, requestContext.getPropertiesDelegate());
    }

    @Override
    public ServiceLocator getServiceLocator() {
        return getRequestContext().getServiceLocator();
    }

    @Override
    protected Iterable<ReaderInterceptor> getReaderInterceptors() {
        return requestContext.getReaderInterceptors();
    }
}
