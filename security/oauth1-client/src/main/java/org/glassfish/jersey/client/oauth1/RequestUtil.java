/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client.oauth1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.uri.UriComponent;


/**
 * Utility class for processing client requests. This class somehow wants to be
 * more than just a utility class for this one filter.
 *
 * @author Paul C. Bryan <pbryan@sun.com>
 * @since 2.3
 */
final class RequestUtil {

    private RequestUtil() {
        throw new AssertionError("Instantiation not allowed.");
    }

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    /**
     * Returns the query parameters of a request as a multi-valued map.
     *
     * @param request the client request to retrieve query parameters from.
     * @return a {@link javax.ws.rs.core.MultivaluedMap} containing the entity query parameters.
     */
    public static MultivaluedMap<String, String> getQueryParameters(ClientRequestContext request) {
        URI uri = request.getUri();
        if (uri == null) {
            return null;
        }

        return UriComponent.decodeQuery(uri, true);
    }

    /**
     * Returns the form parameters from a request entity as a multi-valued map.
     * If the request does not have a POST method, or the media type is not
     * x-www-form-urlencoded, then null is returned.
     *
     * @param request the client request containing the entity to extract parameters from.
     * @return a {@link javax.ws.rs.core.MultivaluedMap} containing the entity form parameters.
     */
    @SuppressWarnings("unchecked")
    public static MultivaluedMap<String, String> getEntityParameters(ClientRequestContext request,
                                                                     MessageBodyWorkers messageBodyWorkers) {

        Object entity = request.getEntity();
        String method = request.getMethod();
        MediaType mediaType = request.getMediaType();

        // no entity, not a post or not x-www-form-urlencoded: return empty map
        if (entity == null || method == null || !HttpMethod.POST.equalsIgnoreCase(method)
                || mediaType == null || !mediaType.equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
            return new MultivaluedHashMap<String, String>();
        }

        // it's ready to go if already expressed as a multi-valued map
        if (entity instanceof MultivaluedMap) {
            return (MultivaluedMap<String, String>) entity;
        }

        Type entityType = entity.getClass();

        // if the entity is generic, get specific type and class
        if (entity instanceof GenericEntity) {
            final GenericEntity generic = (GenericEntity) entity;
            entityType = generic.getType(); // overwrite
            entity = generic.getEntity();
        }

        final Class entityClass = entity.getClass();

        ByteArrayOutputStream out = new ByteArrayOutputStream();


        MessageBodyWriter writer = messageBodyWorkers.getMessageBodyWriter(entityClass,
                entityType, EMPTY_ANNOTATIONS, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        try {
            writer.writeTo(entity, entityClass, entityType,
                    EMPTY_ANNOTATIONS, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, out);
        } catch (WebApplicationException wae) {
            throw new IllegalStateException(wae);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        MessageBodyReader reader = messageBodyWorkers.getMessageBodyReader(MultivaluedMap.class,
                MultivaluedMap.class, EMPTY_ANNOTATIONS, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        try {
            return (MultivaluedMap<String, String>) reader.readFrom(MultivaluedMap.class,
                    MultivaluedMap.class, EMPTY_ANNOTATIONS, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, in);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
}

