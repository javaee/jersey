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
package org.glassfish.jersey.server;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;

/**
 * Jersey container response context.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ContainerResponse implements ContainerResponseContext {

    private Response.StatusType status;
    private final ContainerRequest requestContext;
    private final OutboundMessageContext messageContext;

    /**
     * Create a new Jersey container response context.
     *
     * @param requestContext associated container request context.
     * @param response response instance initializing the response context.
     */
    public ContainerResponse(ContainerRequest requestContext, Response response) {
        this(requestContext, OutboundJaxrsResponse.unwrap(response));
    }

    /**
     * Create a new Jersey container response context.
     *
     * @param requestContext associated container request context.
     * @param response response instance initializing the response context.
     */
    ContainerResponse(ContainerRequest requestContext, OutboundJaxrsResponse response) {
        this.requestContext = requestContext;
        this.status = response.getStatusInfo();
        this.messageContext = response.getContext();

        final String varyValue = requestContext.getVaryValue();
        if (varyValue != null && !this.messageContext.getHeaders().containsKey(HttpHeaders.VARY)) {
            /**
             * Add a Vary header using the value computed in the request if present
             * and if the Vary header was not explicitly set in the response already.
             */
            this.messageContext.getHeaders().add(HttpHeaders.VARY, varyValue);
        }
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
     * Get the associated container request context paired with this response context.
     *
     * @return associated container request context.
     */
    public ContainerRequest getRequestContext() {
        return requestContext;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return messageContext.getResponseCookies();
    }

    /**
     * Get the wrapped response message context.
     *
     * @return wrapped response message context.
     */
    public OutboundMessageContext getWrappedMessageContext() {
        return messageContext;
    }

    /**
     * Get a message header as a single string value.
     *
     * Each single header value is converted to String using a
     * {@link javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate} if one is available
     * via {@link javax.ws.rs.ext.RuntimeDelegate#createHeaderDelegate(java.lang.Class)}
     * for the header value class or using its {@code toString} method  if a header
     * delegate is not available.
     *
     * @param name the message header.
     * @return the message header value. If the message header is not present then
     *         {@code null} is returned. If the message header is present but has no
     *         value then the empty string is returned. If the message header is present
     *         more than once then the values of joined together and separated by a ','
     *         character.
     */
    public String getHeaderString(String name) {
        return messageContext.getHeaderString(name);
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return messageContext.getHeaders();
    }

    //@Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return messageContext.getStringHeaders();
    }

    @Override
    public Date getDate() {
        return messageContext.getDate();
    }

    @Override
    public Locale getLanguage() {
        return messageContext.getLanguage();
    }

    @Override
    public MediaType getMediaType() {
        return messageContext.getMediaType();
    }

    @Override
    public Set<String> getAllowedMethods() {
        return messageContext.getAllowedMethods();
    }

    @Override
    public int getLength() {
        return messageContext.getLength();
    }

    @Override
    public EntityTag getEntityTag() {
        return messageContext.getEntityTag();
    }

    @Override
    public Date getLastModified() {
        return messageContext.getLastModified();
    }

    @Override
    public URI getLocation() {
        return messageContext.getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return messageContext.getLinks();
    }

    @Override
    public boolean hasLink(String relation) {
        return messageContext.hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return messageContext.getLink(relation);
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return messageContext.getLinkBuilder(relation);
    }

    @Override
    public boolean hasEntity() {
        return messageContext.hasEntity();
    }

    @Override
    public Object getEntity() {
        return messageContext.getEntity();
    }

    /**
     * Set a new message message entity.
     *
     * @param entity entity object.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity) {
        messageContext.setEntity(entity);
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param annotations annotations attached to the entity.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity, Annotation[] annotations) {
        messageContext.setEntity(entity, annotations);
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param type        declared entity class.
     * @param annotations annotations attached to the entity.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(Object entity, Type type, Annotation[] annotations) {
        messageContext.setEntity(entity, type, annotations);
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        messageContext.setEntity(entity, annotations, mediaType);
    }

    @Override
    public void setEntity(Object entity, Type type, Annotation[] annotations, MediaType mediaType) {
        messageContext.setEntity(entity, type, annotations, mediaType);
    }

    /**
     * Set the message content media type.
     *
     * @param mediaType message content media type.
     */
    public void setMediaType(MediaType mediaType) {
        messageContext.setMediaType(mediaType);
    }

    @Override
    public Class<?> getEntityClass() {
        return messageContext.getEntityClass();
    }

    @Override
    public Type getEntityType() {
        return messageContext.getEntityType();
    }

    /**
     * Set the message entity type information.
     *
     * This method overrides any computed or previously set entity type information.
     *
     * @param type overriding message entity type.
     */
    public void setEntityType(Type type) {
        messageContext.setEntityType(type);
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return messageContext.getEntityAnnotations();
    }

    /**
     * Set the annotations attached to the entity.
     *
     * @param annotations entity annotations.
     */
    public void setEntityAnnotations(Annotation[] annotations) {
        messageContext.setEntityAnnotations(annotations);
    }

    @Override
    public OutputStream getEntityStream() {
        return messageContext.getEntityStream();
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {
        messageContext.setEntityStream(outputStream);
    }

    /**
     * Set the output stream provider.
     *
     * @param streamProvider output stream provider.
     */
    public void setStreamProvider(OutboundMessageContext.StreamProvider streamProvider) {
        messageContext.setStreamProvider(streamProvider);
    }

    /**
     * Commits the {@link #getEntityStream() entity stream} if it wasn't already committed.
     */
    public void commitStream() {
        messageContext.commitStream();
    }

    /**
     * Returns {@code true} if the entity stream has been committed.
     * @return {@code true} if the entity stream has been committed. Otherwise returns {@code false}.
     */
    public boolean isCommitted() {
        return messageContext.isCommitted();
    }
}
