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
package org.glassfish.jersey.media.multipart.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.FormDataContentDisposition;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.internal.inject.AbstractHttpContextValueFactory;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.HttpContext;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractor;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;

/**
 * Value factory provider supporting the {@link FormDataParam} injection annotation.
 *
 * @author Craig McClanahan
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public final class FormDataParamValueFactoryProvider extends AbstractValueFactoryProvider<FormDataParam> {

    private static final class FormDataParamException extends ParamException {

        protected FormDataParamException(Throwable cause, String name, String defaultStringValue) {
            super(cause, Response.Status.BAD_REQUEST, FormDataParam.class, name, defaultStringValue);
        }
    }

    @Singleton
    public static final class InjectionResolver extends ParamInjectionResolver<FormDataParam> {

        public InjectionResolver() {
            super(FormDataParamValueFactoryProvider.class);
        }

    }

    private final class ListFormDataBodyPartValueFactory extends AbstractHttpContextValueFactory<List<FormDataBodyPart>> {

        private final String name;

        public ListFormDataBodyPartValueFactory(String name) {
            this.name = name;
        }

        @Override
        protected List<FormDataBodyPart> get(HttpContext context) {
            return getEntity(context).getFields(name);
        }

    }

    private final class ListFormDataContentDispositionValueFactory
            extends AbstractHttpContextValueFactory<List<FormDataContentDisposition>> {

        private final String name;

        public ListFormDataContentDispositionValueFactory(String name) {
            this.name = name;
        }

        @Override
        protected List<FormDataContentDisposition> get(HttpContext context) {
            FormDataMultiPart formDataMultiPart = getEntity(context);

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
            extends AbstractHttpContextValueFactory<FormDataBodyPart> {

        private final String name;

        public FormDataBodyPartValueFactory(String name) {
            this.name = name;
        }

        @Override
        protected FormDataBodyPart get(HttpContext context) {
            return getEntity(context).getField(name);
        }
    }

    private final class FormDataContentDispositionMultiPartInjectable
            extends AbstractHttpContextValueFactory<FormDataContentDisposition> {

        private final String name;

        public FormDataContentDispositionMultiPartInjectable(String name) {
            this.name = name;
        }

        @Override
        protected FormDataContentDisposition get(HttpContext context) {
            FormDataMultiPart formDataMultiPart = getEntity(context);

            FormDataBodyPart formDataBodyPart = formDataMultiPart.getField(name);
            if (formDataBodyPart == null) {
                return null;
            }

            return formDataMultiPart.getField(name).getFormDataContentDisposition();
        }
    }

    private final class FormDataParamValueFactory extends AbstractHttpContextValueFactory<Object> {

        private final MultivaluedParameterExtractor<?> extractor;
        private final Parameter parameter;

        public FormDataParamValueFactory(final Parameter parameter, final MultivaluedParameterExtractor<?> extractor) {
            this.parameter = parameter;
            this.extractor = extractor;
        }

        @Override
        protected Object get(HttpContext context) {
            // Return the field value for the field specified by the sourceName property.
            final FormDataMultiPart formDataMultiPart = getEntity(context);

            List<FormDataBodyPart> formDataBodyParts = formDataMultiPart.getFields(parameter.getSourceName());
            FormDataBodyPart formDataBodyPart = (formDataBodyParts != null) ? formDataBodyParts.get(0) : null;

            MediaType mediaType = (formDataBodyPart != null) ? formDataBodyPart.getMediaType() : MediaType.TEXT_PLAIN_TYPE;

            MessageBodyWorkers messageBodyWorkers = context.getRequestContext().getWorkers();

            MessageBodyReader reader = messageBodyWorkers.getMessageBodyReader(
                    parameter.getRawType(),
                    parameter.getType(),
                    parameter.getAnnotations(),
                    mediaType);

            if (reader != null) {
                InputStream in = null;
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
                    return reader.readFrom(
                            parameter.getRawType(),
                            parameter.getType(),
                            parameter.getAnnotations(),
                            mediaType,
                            context.getRequestContext().getHeaders(),
                            in);
                } catch (IOException e) {
                    throw new FormDataParamException(e, extractor.getName(), extractor.getDefaultValueString());
                }
            } else if (extractor != null) {
                MultivaluedMap<String, String> map = new MultivaluedStringMap();
                if (formDataBodyPart != null) {
                    try {
                        for (FormDataBodyPart p : formDataBodyParts) {
                            mediaType = p.getMediaType();

                            reader = messageBodyWorkers.getMessageBodyReader(
                                    String.class,
                                    String.class,
                                    parameter.getAnnotations(),
                                    mediaType);

                            String value = (String) reader.readFrom(
                                    String.class,
                                    String.class,
                                    parameter.getAnnotations(),
                                    mediaType,
                                    context.getRequestContext().getHeaders(),
                                    ((BodyPartEntity) p.getEntity()).getInputStream());

                            map.add(parameter.getSourceName(), value);
                        }
                    } catch (IOException e) {
                        throw new FormDataParamException(e, extractor.getName(), extractor.getDefaultValueString());
                    }
                }
                return extractor.extract(map);
            } else {
                return null;
            }
        }

    }

    private final class FormDataMultiPartValueFactory extends AbstractHttpContextValueFactory<Object> {

        @Override
        protected Object get(HttpContext context) {
            return getEntity(context);
        }

    }

    @Inject
    public FormDataParamValueFactoryProvider(final MultivaluedParameterExtractorProvider mpep,
                                             final ServiceLocator injector) {
        super(mpep, injector, Parameter.Source.ENTITY, Parameter.Source.UNKNOWN);
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
        } else if (parameter.getAnnotation().annotationType() == FormDataParam.class) {
            String parameterName = parameter.getSourceName();
            if (parameterName == null || parameterName.length() == 0) {
                // Invalid query parameter name
                return null;
            }

            final MultivaluedParameterExtractor<?> extractor = get(parameter);

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
                return new FormDataParamValueFactory(parameter, extractor);
            }
        }

        return null;
    }

    /**
     * Returns a {@code FormDataMultiPart} entity from the request and stores it in the context properties.
     *
     * @param context http context to retrieve an entity from.
     * @return a form data multi part entity.
     */
    private FormDataMultiPart getEntity(final HttpContext context) {
        final ContainerRequest requestContext = context.getRequestContext();
        if (requestContext.getProperty(FormDataMultiPart.class.getName()) == null) {
            FormDataMultiPart formDataMultiPart = requestContext.readEntity(FormDataMultiPart.class);
            requestContext.setProperty(FormDataMultiPart.class.getName(), formDataMultiPart);
        }

        return (FormDataMultiPart) requestContext.getProperty(FormDataMultiPart.class.getName());
    }

    @Override
    public PriorityType getPriority() {
        return Priority.HIGH;
    }

}
