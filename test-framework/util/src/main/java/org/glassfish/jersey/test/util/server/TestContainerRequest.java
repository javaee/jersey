/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.util.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.ContainerRequest;

/**
 * {@link org.glassfish.jersey.server.ContainerRequest Container request context} used for testing/benchmarking purposes.
 *
 * @author Michal Gajdos
 * @since 2.17
 */
final class TestContainerRequest extends ContainerRequest {

    private static final Logger LOGGER = Logger.getLogger(TestContainerRequest.class.getName());

    TestContainerRequest(final URI baseUri,
                         final URI requestUri,
                         final String method,
                         final SecurityContext securityContext,
                         final PropertiesDelegate propertiesDelegate) {
        super(baseUri, requestUri, method, securityContext, propertiesDelegate);
    }

    void setEntity(final InputStream stream) {
        setEntityStream(stream);
    }

    void setEntity(final Object requestEntity, final MessageBodyWorkers workers) {
        final Object entity;
        final GenericType entityType;

        if (requestEntity instanceof GenericEntity) {
            entity = ((GenericEntity) requestEntity).getEntity();
            entityType = new GenericType(((GenericEntity) requestEntity).getType());
        } else {
            entity = requestEntity;
            entityType = new GenericType(requestEntity.getClass());
        }

        final byte[] entityBytes;

        if (entity != null) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            OutputStream stream = null;

            try {
                stream = workers.writeTo(entity, entity.getClass(),
                        entityType.getType(),
                        new Annotation[0],
                        getMediaType(),
                        new MultivaluedHashMap<String, Object>(getHeaders()),
                        getPropertiesDelegate(),
                        output,
                        Collections.<WriterInterceptor>emptyList());
            } catch (final IOException | WebApplicationException ex) {
                LOGGER.log(Level.SEVERE, "Transforming entity to input stream failed.", ex);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }

            entityBytes = output.toByteArray();
        } else {
            entityBytes = new byte[0];
        }

        setEntity(new ByteArrayInputStream(entityBytes));
    }
}
