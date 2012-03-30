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
import java.util.Map;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MessageProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.ResponseHeaders;

import org.glassfish.jersey.internal.util.collection.ListMultimapAdapter;

/**
 * Adapter for {@link Response Jersey Response} to {@link javax.ws.rs.core.Response
 * JAX-RS Response}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 */
// TODO Methods in this class should cache results to improve performance
final class JaxrsResponseView extends javax.ws.rs.core.Response {

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

    @Override
    public Map<String, Object> getProperties() {
        return wrapped.properties();
    }

    @Override
    public int getStatus() {
        return wrapped.status().getStatusCode();
    }

    @Override
    public Status getStatusEnum() {
        return Status.fromStatusCode(wrapped.status().getStatusCode());
    }

    @Override
    public ResponseHeaders getHeaders() {
        return wrapped.getJaxrsHeaders();
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
    public boolean isEntityRetrievable() {
        return wrapped.isEntityRetrievable();
    }

    @Override
    public void bufferEntity() throws MessageProcessingException {
        wrapped.bufferEntity();
    }

    @Override
    public void close() throws MessageProcessingException {
        wrapped.close();
    }

    @Override
    @SuppressWarnings("unchecked")
    public MultivaluedMap<String, Object> getMetadata() {
        return new ListMultimapAdapter(wrapped.headers());
    }
}
