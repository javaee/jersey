/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.media.multipart.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import javax.inject.Inject;

import org.glassfish.jersey.internal.inject.ExtractorException;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageUtils;
import org.glassfish.jersey.message.internal.Utils;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractor;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

import org.jvnet.mimepull.MIMEParsingException;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Value factory provider supporting the {@link FormDataParam} injection annotation and entity ({@link FormDataMultiPart})
 * injection.
 *
 * @author Craig McClanahan
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
final class FormDataParamValueFactoryProvider extends AbstractValueFactoryProvider {

    private static final Logger LOGGER = Logger.getLogger(FormDataParamValueFactoryProvider.class.getName());

    private static final class FormDataParamException extends ParamException {

        protected FormDataParamException(final Throwable cause, final String name, final String defaultStringValue) {
            super(cause, Response.Status.BAD_REQUEST, FormDataParam.class, name, defaultStringValue);
        }
    }

    /**
     * {@link FormDataParam} injection resolver.
     */
    static final class InjectionResolver extends ParamInjectionResolver<FormDataParam> {

        /**
         * Create new {@link FormDataParam} injection resolver.
         */
        public InjectionResolver() {
            super(FormDataParamValueFactoryProvider.class);
        }
    }

    private abstract class ValueFactory<T> extends AbstractContainerRequestValueFactory<T> {

        /**
         * Returns a {@code FormDataMultiPart} entity from the request and stores it in the request context properties.
         *
         * @return a form data multi part entity.
         */
        FormDataMultiPart getEntity() {
            final ContainerRequest request = getContainerRequest();
            final String requestPropertyName = FormDataMultiPart.class.getName();

            Object entity = request.getProperty(requestPropertyName);
            if (entity == null) {
                entity = request.readEntity(FormDataMultiPart.class);
                if (entity == null) {
                    throw new BadRequestException(LocalizationMessages.ENTITY_IS_EMPTY());
                }
                request.setProperty(requestPropertyName, entity);
            }

            return (FormDataMultiPart) entity;
        }
    }

    /**
     * Provider factory for entity of {@code FormDataMultiPart} type.
     */
    private final class FormDataMultiPartFactory extends ValueFactory<FormDataMultiPart> {

        @Override
        public FormDataMultiPart provide() {
            return getEntity();
        }
    }

    /**
     * Provider factory for list of {@link org.glassfish.jersey.media.multipart.FormDataBodyPart} types injected via
     * {@link FormDataParam} annotation.
     */
    private final class ListFormDataBodyPartValueFactory extends ValueFactory<List<FormDataBodyPart>> {

        private final String name;

        public ListFormDataBodyPartValueFactory(final String name) {
            this.name = name;
        }

        @Override
        public List<FormDataBodyPart> provide() {
            return getEntity().getFields(name);
        }
    }

    /**
     * Provider factory for list of {@link org.glassfish.jersey.media.multipart.FormDataContentDisposition} types injected via
     * {@link FormDataParam} annotation.
     */
    private final class ListFormDataContentDispositionFactory extends ValueFactory<List<FormDataContentDisposition>> {

        private final String name;

        public ListFormDataContentDispositionFactory(final String name) {
            this.name = name;
        }

        @Override
        public List<FormDataContentDisposition> provide() {
            final List<FormDataBodyPart> parts = getEntity().getFields(name);

            return parts == null ? null : Lists.transform(parts, new Function<FormDataBodyPart, FormDataContentDisposition>() {
                @Override
                public FormDataContentDisposition apply(final FormDataBodyPart part) {
                    return part.getFormDataContentDisposition();
                }
            });
        }
    }

    /**
     * Provider factory for {@link org.glassfish.jersey.media.multipart.FormDataBodyPart} types injected via
     * {@link FormDataParam} annotation.
     */
    private final class FormDataBodyPartFactory extends ValueFactory<FormDataBodyPart> {

        private final String name;

        public FormDataBodyPartFactory(final String name) {
            this.name = name;
        }

        @Override
        public FormDataBodyPart provide() {
            return getEntity().getField(name);
        }
    }

    /**
     * Provider factory for {@link org.glassfish.jersey.media.multipart.FormDataContentDisposition} types injected via
     * {@link FormDataParam} annotation.
     */
    private final class FormDataContentDispositionFactory extends ValueFactory<FormDataContentDisposition> {

        private final String name;

        public FormDataContentDispositionFactory(final String name) {
            this.name = name;
        }

        @Override
        public FormDataContentDisposition provide() {
            final FormDataBodyPart part = getEntity().getField(name);

            return part == null ? null : part.getFormDataContentDisposition();
        }
    }

    /**
     * Provider factory for {@link java.io.File} types injected via {@link FormDataParam} annotation.
     */
    private final class FileFactory extends ValueFactory<File> {

        private final String name;

        public FileFactory(final String name) {
            this.name = name;
        }

        @Override
        public File provide() {
            final FormDataBodyPart part = getEntity().getField(name);
            final BodyPartEntity entity = part != null ? part.getEntityAs(BodyPartEntity.class) : null;

            if (entity != null) {
                try {
                    // Create a temporary file.
                    final File file = Utils.createTempFile();

                    // Move the part (represented either via stream or file) to the specific temporary file.
                    entity.moveTo(file);

                    return file;
                } catch (final IOException | MIMEParsingException cannotMove) {
                    // Unable to create a temporary file or move the file.
                    LOGGER.log(Level.WARNING, LocalizationMessages.CANNOT_INJECT_FILE(), cannotMove);
                }
            }

            return null;
        }
    }

    /**
     * Provider factory for generic types injected via {@link FormDataParam} annotation.
     */
    private final class FormDataParamValueFactory extends ValueFactory<Object> {

        private final MultivaluedParameterExtractor<?> extractor;
        private final Parameter parameter;

        public FormDataParamValueFactory(final Parameter parameter, final MultivaluedParameterExtractor<?> extractor) {
            this.parameter = parameter;
            this.extractor = extractor;
        }

        @Override
        public Object provide() {
            // Return the field value for the field specified by the sourceName property.
            final List<FormDataBodyPart> parts = getEntity().getFields(parameter.getSourceName());

            final FormDataBodyPart part = parts != null ? parts.get(0) : null;
            final MediaType mediaType = part != null ? part.getMediaType() : MediaType.TEXT_PLAIN_TYPE;

            final ContainerRequest request = getContainerRequest();
            final MessageBodyWorkers messageBodyWorkers = request.getWorkers();

            MessageBodyReader reader = messageBodyWorkers.getMessageBodyReader(
                    parameter.getRawType(),
                    parameter.getType(),
                    parameter.getAnnotations(),
                    mediaType);

            // Transform non-primitive part entity into an instance.
            if (reader != null
                    && !isPrimitiveType(parameter.getRawType())) {

                // Get input stream of the body part.
                final InputStream stream;
                if (part == null) {
                    if (parameter.getDefaultValue() != null) {
                        // Convert default value to bytes.
                        stream = new ByteArrayInputStream(parameter.getDefaultValue()
                                .getBytes(MessageUtils.getCharset(mediaType)));
                    } else {
                        return null;
                    }
                } else {
                    stream = part.getEntityAs(BodyPartEntity.class).getInputStream();
                }

                // Transform input stream into instance of desired Java type.
                try {
                    //noinspection unchecked
                    return reader.readFrom(
                            parameter.getRawType(),
                            parameter.getType(),
                            parameter.getAnnotations(),
                            mediaType,
                            request.getHeaders(),
                            stream);
                } catch (final IOException e) {
                    throw new FormDataParamException(e, parameter.getSourceName(), parameter.getDefaultValue());
                }
            }

            // If no reader was found or a primitive type is being transformed use extractor instead.
            if (extractor != null) {
                final MultivaluedMap<String, String> map = new MultivaluedStringMap();
                try {
                    if (part != null) {
                        for (final FormDataBodyPart p : parts) {
                            reader = messageBodyWorkers.getMessageBodyReader(
                                    String.class,
                                    String.class,
                                    parameter.getAnnotations(),
                                    p.getMediaType());

                            @SuppressWarnings("unchecked") final String value = (String) reader.readFrom(
                                    String.class,
                                    String.class,
                                    parameter.getAnnotations(),
                                    mediaType,
                                    request.getHeaders(),
                                    ((BodyPartEntity) p.getEntity()).getInputStream());

                            map.add(parameter.getSourceName(), value);
                        }
                    }
                    return extractor.extract(map);
                } catch (final IOException | ExtractorException ex) {
                    throw new FormDataParamException(ex, extractor.getName(), extractor.getDefaultValueString());
                }
            }

            return null;
        }
    }

    private static final Set<Class<?>> TYPES = initializeTypes();

    private static Set<Class<?>> initializeTypes() {
        final Set<Class<?>> newSet = new HashSet<>();
        newSet.add(Byte.class);
        newSet.add(byte.class);
        newSet.add(Short.class);
        newSet.add(short.class);
        newSet.add(Integer.class);
        newSet.add(int.class);
        newSet.add(Long.class);
        newSet.add(long.class);
        newSet.add(Float.class);
        newSet.add(float.class);
        newSet.add(Double.class);
        newSet.add(double.class);
        newSet.add(Boolean.class);
        newSet.add(boolean.class);
        newSet.add(Character.class);
        newSet.add(char.class);
        return newSet;
    }

    private static boolean isPrimitiveType(final Class<?> type) {
        return TYPES.contains(type);
    }

    /**
     * Injection constructor.
     *
     * @param extractorProvider multi-valued map parameter extractor provider.
     * @param locator           HK2 service locator.
     */
    @Inject
    public FormDataParamValueFactoryProvider(final MultivaluedParameterExtractorProvider extractorProvider,
                                             final ServiceLocator locator) {
        super(extractorProvider, locator, Parameter.Source.ENTITY, Parameter.Source.UNKNOWN);
    }

    @Override
    protected Factory<?> createValueFactory(final Parameter parameter) {
        final Class<?> rawType = parameter.getRawType();

        if (Parameter.Source.ENTITY == parameter.getSource()) {
            if (FormDataMultiPart.class.isAssignableFrom(rawType)) {
                return new FormDataMultiPartFactory();
            } else {
                return null;
            }
        } else if (parameter.getSourceAnnotation().annotationType() == FormDataParam.class) {
            final String paramName = parameter.getSourceName();
            if (paramName == null || paramName.isEmpty()) {
                // Invalid query parameter name
                return null;
            }

            if (Collection.class == rawType || List.class == rawType) {
                final Class clazz = ReflectionHelper.getGenericTypeArgumentClasses(parameter.getType()).get(0);

                if (FormDataBodyPart.class == clazz) {
                    // Return a collection of form data body part.
                    return new ListFormDataBodyPartValueFactory(paramName);
                } else if (FormDataContentDisposition.class == clazz) {
                    // Return a collection of form data content disposition.
                    return new ListFormDataContentDispositionFactory(paramName);
                } else {
                    // Return a collection of specific type.
                    return new FormDataParamValueFactory(parameter, get(parameter));
                }
            } else if (FormDataBodyPart.class == rawType) {
                return new FormDataBodyPartFactory(paramName);
            } else if (FormDataContentDisposition.class == rawType) {
                return new FormDataContentDispositionFactory(paramName);
            } else if (File.class == rawType) {
                return new FileFactory(paramName);
            } else {
                return new FormDataParamValueFactory(parameter, get(parameter));
            }
        }

        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.HIGH;
    }

}
