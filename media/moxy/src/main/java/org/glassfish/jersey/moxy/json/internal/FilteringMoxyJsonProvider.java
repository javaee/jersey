/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.moxy.json.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.glassfish.jersey.message.filtering.spi.ObjectProvider;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.ObjectGraph;

/**
 * Entity Data Filtering provider based on MOXy JSON provider.
 *
 * @author Michal Gajdos
 */
@Singleton
public class FilteringMoxyJsonProvider extends ConfigurableMoxyJsonProvider {

    @Inject
    private Provider<ObjectProvider<ObjectGraph>> provider;

    @Override
    protected void preWriteTo(final Object object, final Class<?> type, final Type genericType, final Annotation[] annotations,
                              final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                              final Marshaller marshaller) throws JAXBException {
        super.preWriteTo(object, type, genericType, annotations, mediaType, httpHeaders, marshaller);

        // Entity Filtering.
        if (marshaller.getProperty(MarshallerProperties.OBJECT_GRAPH) == null) {
            final Object objectGraph = provider.get().getFilteringObject(genericType, true, annotations);

            if (objectGraph != null) {
                marshaller.setProperty(MarshallerProperties.OBJECT_GRAPH, objectGraph);
            }
        }
    }

    @Override
    protected void preReadFrom(final Class<Object> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                               final Unmarshaller unmarshaller) throws JAXBException {
        super.preReadFrom(type, genericType, annotations, mediaType, httpHeaders, unmarshaller);

        // Entity Filtering.
        if (unmarshaller.getProperty(MarshallerProperties.OBJECT_GRAPH) == null) {
            final Object objectGraph = provider.get().getFilteringObject(genericType, false, annotations);

            if (objectGraph != null) {
                unmarshaller.setProperty(MarshallerProperties.OBJECT_GRAPH, objectGraph);
            }
        }
    }
}
