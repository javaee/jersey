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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import com.google.common.base.Objects;

/**
 * Implementation of an inbound JAX-RS response message.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class InboundJaxrsResponse extends Response {

    private final ClientResponse context;

    /**
     * Create new inbound JAX-RS response message.
     *
     * @param context jersey client response context.
     */
    public InboundJaxrsResponse(ClientResponse context) {
        this.context = context;
    }

    public ClientRequest getRequestContext() {
        return context.getRequestContext();
    }

    @Override
    public int getStatus() {
        return context.getStatus();
    }

    @Override
    public StatusType getStatusInfo() {
        return context.getStatusInfo();
    }

    @Override
    public Object getEntity() throws IllegalStateException {
        // TODO implement some advanced caching support?
        return context.getEntityStream();
    }

    @Override
    public <T> T readEntity(Class<T> entityType) throws ProcessingException, IllegalStateException {
        return context.readEntity(entityType, context.getRequestContext().getPropertiesDelegate());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readEntity(GenericType<T> entityType) throws ProcessingException, IllegalStateException {
        return (T) context.readEntity(
                entityType.getRawType(),
                entityType.getType(),
                context.getRequestContext().getPropertiesDelegate());
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) throws ProcessingException, IllegalStateException {
        return context.readEntity(entityType, annotations, context.getRequestContext().getPropertiesDelegate());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) throws ProcessingException, IllegalStateException {
        return (T) context.readEntity(
                entityType.getRawType(),
                entityType.getType(),
                annotations,
                context.getRequestContext().getPropertiesDelegate());
    }

    @Override
    public boolean hasEntity() {
        return context.hasEntity();
    }

    @Override
    public boolean bufferEntity() throws ProcessingException {
        return context.bufferEntity();
    }

    @Override
    public void close() throws ProcessingException {
        context.close();
    }

    @Override
    public String getHeaderString(String name) {
        return context.getHeaderString(name);
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return context.getHeaders();
    }

    @Override
    public MediaType getMediaType() {
        return context.getMediaType();
    }

    @Override
    public Locale getLanguage() {
        return context.getLanguage();
    }

    @Override
    public int getLength() {
        return context.getLength();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return context.getResponseCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return context.getEntityTag();
    }

    @Override
    public Date getDate() {
        return context.getDate();
    }

    @Override
    public Date getLastModified() {
        return context.getLastModified();
    }

    @Override
    public Set<String> getAllowedMethods() {
        return context.getAllowedMethods();
    }

    @Override
    public URI getLocation() {
        return context.getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return context.getLinks();
    }

    @Override
    public boolean hasLink(String relation) {
        return context.hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return context.getLink(relation);
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return context.getLinkBuilder(relation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MultivaluedMap<String, Object> getMetadata() {
        final MultivaluedMap<String, ?> headers = context.getHeaders();
        return (MultivaluedMap<String, Object>) headers;
    }

    @Override
    public String toString() {
        return Objects
                .toStringHelper(this)
                .addValue(context)
                .toString();
    }
}
