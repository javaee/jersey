/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity.filtering;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import javax.inject.Inject;

import org.glassfish.jersey.message.filtering.spi.FilteringHelper;
import org.glassfish.jersey.message.filtering.spi.ObjectGraph;
import org.glassfish.jersey.message.filtering.spi.ObjectProvider;

/**
 * @author Michal Gajdos
 */
@Provider
@Consumes("entity/filtering")
@Produces("entity/filtering")
public class FilteringMessageBodyProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    private static final Logger LOGGER = Logger.getLogger(FilteringMessageBodyProvider.class.getName());

    @Inject
    private javax.inject.Provider<ObjectProvider<ObjectGraph>> provider;

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                              final MediaType mediaType) {
        return String.class != type;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return String.class != type;
    }

    @Override
    public long getSize(final Object o, final Class<?> type, final Type genericType, final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    public Object readFrom(final Class<Object> type, final Type genericType, final Annotation[] annotations,
                           final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                           final InputStream entityStream) throws IOException, WebApplicationException {
        try {
            final ObjectGraph objectGraph = provider.get()
                    .getFilteringObject(FilteringHelper.getEntityClass(genericType), false, annotations);

            return objectGraphToString(objectGraph);
        } catch (final Throwable t) {
            LOGGER.log(Level.WARNING, "Error during reading an object graph.", t);
            return "ERROR: " + t.getMessage();
        }
    }

    @Override
    public void writeTo(final Object o, final Class<?> type, final Type genericType, final Annotation[] annotations,
                        final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {
        final ObjectGraph objectGraph = provider.get()
                .getFilteringObject(FilteringHelper.getEntityClass(genericType), true, annotations);

        try {
            entityStream.write(objectGraphToString(objectGraph).getBytes());
        } catch (final Throwable t) {
            LOGGER.log(Level.WARNING, "Error during writing an object graph.", t);
        }
    }

    private static String objectGraphToString(final ObjectGraph objectGraph) {
        final StringBuilder sb = new StringBuilder();
        for (final String field : objectGraphToFields("", objectGraph)) {
            if (!field.contains("Transient")) {
                sb.append(field).append(',');
            }
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    private static List<String> objectGraphToFields(final String prefix, final ObjectGraph objectGraph) {
        final List<String> fields = new ArrayList<>();

        // Fields.
        for (final String field : objectGraph.getFields()) {
            fields.add(prefix + field);
        }

        for (final Map.Entry<String, ObjectGraph> entry : objectGraph.getSubgraphs().entrySet()) {
            fields.addAll(objectGraphToFields(prefix + entry.getKey() + ".", entry.getValue()));
        }

        return fields;
    }
}
