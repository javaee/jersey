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
package org.glassfish.jersey.message.internal;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

/**
 * Adapter for {@link Response Jersey Response} to {@link javax.ws.rs.core.Response
 * JAX-RS Response}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 */
// TODO Methods in this class should cache results to improve performance
public final class JaxrsResponseView extends javax.ws.rs.core.Response {

    private final Response wrapped;

    public JaxrsResponseView(Response wrapped) {
        this.wrapped = wrapped;
    }

    static Response unwrap(javax.ws.rs.core.Response response) {
        if (response instanceof JaxrsResponseView) {
            return ((JaxrsResponseView) response).wrapped;
        }

        throw new IllegalArgumentException(String.format("Response class type '%s' not supported.", response.getClass().getName()));
    }

    public Map<String, Object> getProperties() {
        return wrapped.properties();
    }

    @Override
    public int getStatus() {
        return wrapped.status().getStatusCode();
    }

    @Override
    public StatusType getStatusInfo() {
        return wrapped.status();
    }

    public MultivaluedMap<String, String> getHeaders() {
        return wrapped.headers();
    }

    @Override
    public Object getEntity() {
        return wrapped.content();
    }

    @Override
    public <T> T readEntity(Class<T> type) throws MessageProcessingException {
        return wrapped.content(type);
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) throws MessageProcessingException {
        return wrapped.content(entityType);
    }

    @Override
    public <T> T readEntity(Class<T> type, Annotation[] annotations) throws MessageProcessingException {
        return wrapped.content(type, annotations);
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) throws MessageProcessingException {
        return wrapped.content(entityType, annotations);
    }

    @Override
    public boolean hasEntity() {
        return !wrapped.isEmpty();
    }

    @Override
    public boolean bufferEntity() throws MessageProcessingException {
        wrapped.bufferEntity();
        // TODO
        return false;
    }

    @Override
    public void close() throws MessageProcessingException {
        wrapped.close();
    }

    @Override
    public String getHeader(String name) {
        return wrapped.getJaxrsHeaders().getHeader(name);
    }

    @Override
    public MediaType getMediaType() {
        return wrapped.getJaxrsHeaders().getMediaType();
    }

    @Override
    public Locale getLanguage() {
        return wrapped.getJaxrsHeaders().getLanguage();
    }

    @Override
    public int getLength() {
        return wrapped.getJaxrsHeaders().getLength();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return wrapped.getJaxrsHeaders().getCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return wrapped.getJaxrsHeaders().getEntityTag();
    }

    @Override
    public Date getDate() {
        return wrapped.getJaxrsHeaders().getDate();
    }

    @Override
    public Date getLastModified() {
        return wrapped.getJaxrsHeaders().getLastModified();
    }

    public Set<String> getAllowedMethods() {
        return wrapped.getJaxrsHeaders().getAllowedMethods();
    }

    @Override
    public URI getLocation() {
        return wrapped.getJaxrsHeaders().getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return wrapped.getJaxrsHeaders().getLinks();
    }

    @Override
    public boolean hasLink(String relation) {
        return wrapped.getJaxrsHeaders().hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return wrapped.getJaxrsHeaders().getLink(relation);
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return wrapped.getJaxrsHeaders().getLinkBuilder(relation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MultivaluedMap<String, Object> getMetadata() {
        final MultivaluedMap<String, ?> headers = wrapped.headers();
        return (MultivaluedMap<String, Object>) headers;
    }
}
