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

package org.glassfish.jersey.jackson.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.message.filtering.spi.ObjectProvider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.BeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.jaxrs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterModifier;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JsonEndpointConfig;

/**
 * Entity Data Filtering provider based on Jackson JSON provider.
 *
 * @author Michal Gajdos
 */
@Singleton
public final class FilteringJacksonJaxbJsonProvider extends JacksonJaxbJsonProvider {

    @Inject
    private Provider<ObjectProvider<FilterProvider>> provider;

    @Override
    protected JsonEndpointConfig _configForWriting(final ObjectMapper mapper, final Annotation[] annotations,
                                                   final Class<?> defaultView) {
        final AnnotationIntrospector customIntrospector = mapper.getSerializationConfig().getAnnotationIntrospector();
        // Set the custom (user) introspector to be the primary one.
        final ObjectMapper filteringMapper = mapper.setAnnotationIntrospector(AnnotationIntrospector.pair(customIntrospector,
                new JacksonAnnotationIntrospector() {
                    @Override
                    public Object findFilterId(final Annotated a) {
                        final Object filterId = super.findFilterId(a);

                        if (filterId != null) {
                            return filterId;
                        }

                        if (a instanceof AnnotatedMethod) {
                            final Method method = ((AnnotatedMethod) a).getAnnotated();

                            // Interested only in getters - trying to obtain "field" name from them.
                            if (ReflectionHelper.isGetter(method)) {
                                return ReflectionHelper.getPropertyName(method);
                            }
                        }
                        if (a instanceof AnnotatedField || a instanceof AnnotatedClass) {
                            return a.getName();
                        }

                        return null;
                    }
                }));

        return super._configForWriting(filteringMapper, annotations, defaultView);
    }

    @Override
    public void writeTo(final Object value,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException {
        final FilterProvider filterProvider = provider.get().getFilteringObject(genericType, true, annotations);
        if (filterProvider != null) {
            ObjectWriterInjector.set(new FilteringObjectWriterModifier(filterProvider, ObjectWriterInjector.getAndClear()));
        }

        super.writeTo(value, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    private static final class FilteringObjectWriterModifier extends ObjectWriterModifier {

        private final ObjectWriterModifier original;
        private final FilterProvider filterProvider;

        private FilteringObjectWriterModifier(final FilterProvider filterProvider, final ObjectWriterModifier original) {
            this.original = original;
            this.filterProvider = filterProvider;
        }

        @Override
        public ObjectWriter modify(final EndpointConfigBase<?> endpoint,
                                   final MultivaluedMap<String, Object> responseHeaders,
                                   final Object valueToWrite,
                                   final ObjectWriter w,
                                   final JsonGenerator g) throws IOException {
            final ObjectWriter writer = original == null ? w : original.modify(endpoint, responseHeaders, valueToWrite, w, g);
            final FilterProvider customFilterProvider = writer.getConfig().getFilterProvider();

            // Try the custom (user) filter provider first.
            return customFilterProvider == null
                    ? writer.with(filterProvider)
                    : writer.with(new FilterProvider() {
                        @Override
                        public BeanPropertyFilter findFilter(final Object filterId) {
                            return customFilterProvider.findFilter(filterId);
                        }

                        @Override
                        public PropertyFilter findPropertyFilter(final Object filterId, final Object valueToFilter) {
                            final PropertyFilter filter = customFilterProvider.findPropertyFilter(filterId, valueToFilter);
                            if (filter != null) {
                                return filter;
                            }

                            return filterProvider.findPropertyFilter(filterId, valueToFilter);
                        }
                    });
        }
    }
}
