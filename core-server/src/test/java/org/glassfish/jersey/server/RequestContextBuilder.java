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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MediaTypes;

/**
 * Used by tests to create mock JerseyContainerRequestContext instances.
 *
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class RequestContextBuilder {

    public RequestContextBuilder type(String contentType) {
        result.getHeaders().putSingle("content-type", contentType);
        return this;

    }

    public class JerseyTestContainerRequestContext extends JerseyContainerRequestContext {

        private Object entity;
        private GenericType entityType;
        private final PropertiesDelegate propertiesDelegate;

        public JerseyTestContainerRequestContext(URI baseUri, URI requestUri, String method, SecurityContext securityContext, PropertiesDelegate propertiesDelegate) {
            super(baseUri, requestUri, method, securityContext, propertiesDelegate);
            this.propertiesDelegate = propertiesDelegate;
        }

        public void setEntity(Object entity) {
            if (entity instanceof GenericEntity) {
                this.entity = ((GenericEntity) entity).getEntity();
                this.entityType = new GenericType(((GenericEntity) entity).getType());
            } else {
                this.entity = entity;
                this.entityType = new GenericType(entity.getClass());
            }
        }

        @Override
        public void setWorkers(MessageBodyWorkers workers) {
            super.setWorkers(workers);
            MultivaluedMap<String, Object> myMap = new MultivaluedHashMap<String, Object>(getHeaders());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                workers.writeTo(entity, entityType.getRawType(), entityType.getType(), new Annotation[0], getMediaType(),
                        myMap,
                        propertiesDelegate, baos, null, true);
            } catch (IOException ex) {
                Logger.getLogger(RequestContextBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } catch (WebApplicationException ex) {
                Logger.getLogger(RequestContextBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
            setEntityStream(new ByteArrayInputStream(baos.toByteArray()));
        }
    }
    private final JerseyTestContainerRequestContext result;

    public static RequestContextBuilder from(String requestUri, String method) {
        return from(null, requestUri, method);
    }

    public static RequestContextBuilder from(String baseUri, String requestUri, String method) {
        return new RequestContextBuilder(baseUri, requestUri, method);
    }

    private RequestContextBuilder(String baseUri, String requestUri, String method) {
        result = new JerseyTestContainerRequestContext(URI.create(baseUri), URI.create(requestUri), method, null,
                new MapPropertiesDelegate());
    }

    public JerseyContainerRequestContext build() {
        return result;
    }

    public RequestContextBuilder header(String name, Object value) {
        result.header(name, value);
        return this;
    }

    public RequestContextBuilder accept(String... acceptHeader) {
        result.getAcceptableMediaTypes().addAll(MediaTypes.createFrom(acceptHeader));
        return this;
    }

    public RequestContextBuilder entity(Object entity) {
        result.setEntity(entity);
        return this;
    }
}
