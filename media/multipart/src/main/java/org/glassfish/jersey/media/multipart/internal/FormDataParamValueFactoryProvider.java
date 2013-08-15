/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import javax.inject.Inject;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.ExtractorException;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractor;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Value factory provider supporting the {@link FormDataParam} injection annotation.
 *
 * @author Craig McClanahan
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public final class FormDataParamValueFactoryProvider extends AbstractValueFactoryProvider {

    private static final class FormDataParamException extends ParamException {

        protected FormDataParamException(Throwable cause, String name, String defaultStringValue) {
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

    private final class ListFormDataBodyPartValueFactory extends AbstractContainerRequestValueFactory<List<FormDataBodyPart>> {

        private final String name;

        public ListFormDataBodyPartValueFactory(String name) {
            this.name = name;
        }

        @Override
        public List<FormDataBodyPart> provide() {
            return getEntity(getContainerRequest()).getFields(name);
        }

    }

    private final class ListFormDataContentDispositionValueFactory
            extends AbstractContainerRequestValueFactory<List<FormDataContentDisposition>> {

        private final String name;

        public ListFormDataContentDispositionValueFactory(String name) {
            this.name = name;
        }

        @Override
        public List<FormDataContentDisposition> provide() {
            FormDataMultiPart formDataMultiPart = getEntity(getContainerRequest());

            List<FormDataBodyPart> formDataBodyParts = formDataMultiPart.getFields(name);
            if (formDataBodyParts == null)
                return null;

            List<FormDataContentDisposition> list = new ArrayList<FormDataContentDisposition>(formDataBodyParts.size());
            for (FormDataBodyPart formDataBodyPart : formDataBodyParts) {
                list.add(formDataBodyPart.getFormDataContentDisposition());
            }

            return list;
        }
    }

    private final class FormDataBodyPartValueFactory
            extends AbstractContainerRequestValueFactory<FormDataBodyPart> {

        private final String name;

        public FormDataBodyPartValueFactory(String name) {
            this.name = name;
        }

        @Override
        public FormDataBodyPart provide() {
            return getEntity(getContainerRequest()).getField(name);
        }
    }

    private final class FormDataContentDispositionMultiPartInjectable
            extends AbstractContainerRequestValueFactory<FormDataContentDisposition> {

        private final String name;

        public FormDataContentDispositionMultiPartInjectable(String name) {
            this.name = name;
        }

        @Override
        public FormDataContentDisposition provide() {
            FormDataMultiPart formDataMultiPart = getEntity(getContainerRequest());

            FormDataBodyPart formDataBodyPart = formDataMultiPart.getField(name);
            if (formDataBodyPart == null) {
                return null;
            }

            return formDataMultiPart.getField(name).getFormDataContentDisposition();
        }
    }

    private final class FormDataParamValueFactory extends AbstractContainerRequestValueFactory<Object> {

        private final MultivaluedParameterExtractor<?> extractor;
        private final Parameter parameter;

        public FormDataParamValueFactory(final Parameter parameter, final MultivaluedParameterExtractor<?> extractor) {
            this.parameter = parameter;
            this.extractor = extractor;
        }

        @Override
        public Object provide() {
            // Return the field value for the field specified by the sourceName property.
            final ContainerRequest request = getContainerRequest();
            final FormDataMultiPart formDataMultiPart = getEntity(request);

            List<FormDataBodyPart> formDataBodyParts = formDataMultiPart.getFields(parameter.getSourceName());
            FormDataBodyPart formDataBodyPart = (formDataBodyParts != null) ? formDataBodyParts.get(0) : null;

            MediaType mediaType = (formDataBodyPart != null) ? formDataBodyPart.getMediaType() : MediaType.TEXT_PLAIN_TYPE;

            MessageBodyWorkers messageBodyWorkers = request.getWorkers();

            MessageBodyReader reader = messageBodyWorkers.getMessageBodyReader(
                    parameter.getRawType(),
                    parameter.getType(),
                    parameter.getAnnotations(),
                    mediaType);

            if (reader != null && !isPrimitiveType(parameter.getRawType())) {
                InputStream in;
                if (formDataBodyPart == null) {
                    if (parameter.getDefaultValue() != null) {
                        // Convert default value to bytes.
                        in = new ByteArrayInputStream(parameter.getDefaultValue().getBytes());
                    } else {
                        return null;
                    }
                } else {
                    in = ((BodyPartEntity) formDataBodyPart.getEntity()).getInputStream();
                }


                try {
                    //noinspection unchecked
                    return reader.readFrom(
                            parameter.getRawType(),
                            parameter.getType(),
                            parameter.getAnnotations(),
                            mediaType,
                            request.getHeaders(),
                            in);
                } catch (IOException e) {
                    throw new FormDataParamException(e, extractor.getName(), extractor.getDefaultValueString());
                }
            } else if (extractor != null) {
                MultivaluedMap<String, String> map = new MultivaluedStringMap();
                try {
                    if (formDataBodyPart != null) {
                        for (FormDataBodyPart p : formDataBodyParts) {
                            mediaType = p.getMediaType();

                            reader = messageBodyWorkers.getMessageBodyReader(
                                    String.class,
                                    String.class,
                                    parameter.getAnnotations(),
                                    mediaType);

                            @SuppressWarnings("unchecked") String value = (String) reader.readFrom(
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
                } catch (IOException ex) {
                    throw new FormDataParamException(ex, extractor.getName(), extractor.getDefaultValueString());
                } catch (ExtractorException ex) {
                    throw new FormDataParamException(ex, extractor.getName(), extractor.getDefaultValueString());
                }
            }
            return null;
        }

    }

    private static final Set<Class<?>> types = initializeTypes();

    private static Set<Class<?>> initializeTypes() {
        Set<Class<?>> newSet = new HashSet<Class<?>>();
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

    private static boolean isPrimitiveType(Class<?> type) {
        return types.contains(type);
    }

    private final class FormDataMultiPartValueFactory extends AbstractContainerRequestValueFactory<Object> {

        @Override
        public Object provide() {
            return getEntity(getContainerRequest());
        }

    }

    /**
     * Injection constructor.
     *
     * @param mpep    multi-valued map parameter extractor provider.
     * @param locator HK2 service locator.
     */
    @Inject
    public FormDataParamValueFactoryProvider(final MultivaluedParameterExtractorProvider mpep,
                                             final ServiceLocator locator) {
        super(mpep, locator, Parameter.Source.ENTITY, Parameter.Source.UNKNOWN);
    }

    @Override
    protected Factory<?> createValueFactory(Parameter parameter) {
        Class<?> parameterRawType = parameter.getRawType();
        if (Parameter.Source.ENTITY == parameter.getSource()) {
            if (FormDataMultiPart.class.isAssignableFrom(parameterRawType)) {
                return new FormDataMultiPartValueFactory();
            } else {
                return null;
            }
        } else if (parameter.getSourceAnnotation().annotationType() == FormDataParam.class) {
            String parameterName = parameter.getSourceName();
            if (parameterName == null || parameterName.length() == 0) {
                // Invalid query parameter name
                return null;
            }

            if (Collection.class == parameterRawType || List.class == parameterRawType) {
                Class c = ReflectionHelper.getGenericTypeArgumentClasses(parameter.getType()).get(0);
                if (FormDataBodyPart.class == c) {
                    return new ListFormDataBodyPartValueFactory(parameter.getSourceName());
                } else if (FormDataContentDisposition.class == c) {
                    return new ListFormDataContentDispositionValueFactory(parameter.getSourceName());
                }
            } else if (FormDataBodyPart.class == parameterRawType) {
                return new FormDataBodyPartValueFactory(parameter.getSourceName());
            } else if (FormDataContentDisposition.class == parameterRawType) {
                return new FormDataContentDispositionMultiPartInjectable(parameter.getSourceName());
            } else {
                return new FormDataParamValueFactory(parameter, get(parameter));
            }
        }

        return null;
    }

    /**
     * Returns a {@code FormDataMultiPart} entity from the request and stores it in the context properties.
     *
     * @param request container request.
     * @return a form data multi part entity.
     */
    private FormDataMultiPart getEntity(final ContainerRequest request) {
        if (request.getProperty(FormDataMultiPart.class.getName()) == null) {
            FormDataMultiPart formDataMultiPart = request.readEntity(FormDataMultiPart.class);
            request.setProperty(FormDataMultiPart.class.getName(), formDataMultiPart);
        }

        return (FormDataMultiPart) request.getProperty(FormDataMultiPart.class.getName());
    }

    @Override
    public PriorityType getPriority() {
        return Priority.HIGH;
    }

}
