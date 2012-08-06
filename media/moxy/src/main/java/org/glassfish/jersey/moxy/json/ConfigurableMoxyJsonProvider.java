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
package org.glassfish.jersey.moxy.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;

/**
 * Jersey specific {@link MOXyJsonProvider} that can be configured via {@code ContextResolver<JsonMoxyConfiguration>} instance.
 * <p/>
 * Note: Pre-configured default values
 * <ul>
 *     <li>Attribute prefix - {@code @}</li>
 *     <li>Value wrapper - {@code $}</li>
 *     <li>Namespace separator - {@code :}</li>
 * </ul>
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
@Produces("*/*")
@Consumes("*/*")
class ConfigurableMoxyJsonProvider extends MOXyJsonProvider {

    @Context
    private Providers providers;

    /**
     * Create new configurable moxy JSON provider instance.
     */
    ConfigurableMoxyJsonProvider() {
        setAttributePrefix("@");
        setValueWrapper("$");
        setNamespaceSeparator(':');
    }

    private void initializeProperties() {
        final ContextResolver<MoxyJsonConfiguration> contextResolver =
                providers.getContextResolver(MoxyJsonConfiguration.class, MediaType.APPLICATION_JSON_TYPE);

        if (contextResolver != null) {
            final MoxyJsonConfiguration jsonConfiguration = contextResolver.getContext(MoxyJsonConfiguration.class);

            if (jsonConfiguration.getAttributePrefix() != null) {
                setAttributePrefix(jsonConfiguration.getAttributePrefix());
            }
            if (jsonConfiguration.getValueWrapper() != null) {
                setValueWrapper(jsonConfiguration.getValueWrapper());
            }

            setFormattedOutput(jsonConfiguration.isFormattedOutput());
            setIncludeRoot(jsonConfiguration.isIncludeRoot());
            setMarshalEmptyCollections(jsonConfiguration.isMarshalEmptyCollections());
            setNamespaceSeparator(jsonConfiguration.getNamespaceSeparator());

            setNamespacePrefixMapper(jsonConfiguration.getNamespacePrefixMapper());
        }
    }

    private void initializeUnmarshaller(final Unmarshaller unmarshaller) throws PropertyException {
        unmarshaller.setProperty(UnmarshallerProperties.JSON_ATTRIBUTE_PREFIX, getAttributePrefix());
        unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, isIncludeRoot());
        unmarshaller.setProperty(UnmarshallerProperties.JSON_NAMESPACE_SEPARATOR, getNamespaceSeparator());
        unmarshaller.setProperty(UnmarshallerProperties.JSON_VALUE_WRAPPER, getValueWrapper());
        unmarshaller.setProperty(UnmarshallerProperties.JSON_NAMESPACE_PREFIX_MAPPER, getNamespacePrefixMapper());
    }

    private void initializeMarshaller(final Marshaller marshaller) throws PropertyException {
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, isFormattedOutput());
        marshaller.setProperty(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, getAttributePrefix());
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, isIncludeRoot());
        marshaller.setProperty(MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS, isMarshalEmptyCollections());
        marshaller.setProperty(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, getNamespaceSeparator());
        marshaller.setProperty(MarshallerProperties.JSON_VALUE_WRAPPER, getValueWrapper());
        marshaller.setProperty(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, getNamespacePrefixMapper());
    }

    @Override
    // TODO remove this when moved to MOXy 2.4.1
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                              final MediaType mediaType) {
        // MOXy returns empty string when Response#readEntity(String.class) is called.
        return !String.class.equals(type)
                && super.isReadable(type, genericType, annotations, mediaType)
                && (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType) || mediaType.getSubtype().endsWith("+json"));
    }

    @Override
    // TODO remove this when moved to MOXy 2.4.1
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return super.isReadable(type, genericType, annotations, mediaType)
                && (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType) || mediaType.getSubtype().endsWith("+json"));
    }

    @Override
    protected void preReadFrom(final Class<Object> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                               final Unmarshaller unmarshaller) throws JAXBException {
        super.preReadFrom(type, genericType, annotations, mediaType, httpHeaders, unmarshaller);

        initializeProperties();
        initializeUnmarshaller(unmarshaller);
    }

    @Override
    protected void preWriteTo(final Object object, final Class<?> type, final Type genericType, final Annotation[] annotations,
                              final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                              final Marshaller marshaller) throws JAXBException {
        super.preWriteTo(object, type, genericType, annotations, mediaType, httpHeaders, marshaller);

        initializeProperties();
        initializeMarshaller(marshaller);
    }

}
