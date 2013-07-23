/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.Parameter;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Value factory provider supporting the {@link FormParam} injection annotation.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
final class FormParamValueFactoryProvider extends AbstractValueFactoryProvider {

    /**
     * {@link FormParam} injection resolver.
     */
    @Singleton
    static final class InjectionResolver extends ParamInjectionResolver<FormParam> {

        /**
         * Create new FormParam injection resolver.
         */
        public InjectionResolver() {
            super(FormParamValueFactoryProvider.class);
        }
    }


    private static final class FormParamValueFactory extends AbstractHttpContextValueFactory<Object> {

        private final MultivaluedParameterExtractor<?> extractor;
        private final boolean decode;

        FormParamValueFactory(MultivaluedParameterExtractor<?> extractor, boolean decode) {
            this.extractor = extractor;
            this.decode = decode;
        }

        @Override
        public Object get(HttpContext context) {

            Form form = getCachedForm(context, decode);

            if (form == null) {
                Form otherForm = getCachedForm(context, !decode);
                if (otherForm != null) {
                    form = switchUrlEncoding(context, otherForm, decode);
                    cacheForm(context, form);
                } else {
                    form = getForm(context);
                    cacheForm(context, form);
                }
            }

            try {
                return extractor.extract(form.asMap());
            } catch (ExtractorException e) {
                throw new ParamException.FormParamException(e.getCause(),
                        extractor.getName(), extractor.getDefaultValueString());
            }
        }

        private Form switchUrlEncoding(final HttpContext context, final Form otherForm, final boolean decode) {
            final Set<Map.Entry<String, List<String>>> entries = otherForm.asMap().entrySet();
            Form newForm = new Form();
            for (Map.Entry<String, List<String>> entry : entries) {
                final String charsetName = ReaderWriter.getCharset(MediaType.valueOf(context.getRequestContext()
                        .getHeaderString(HttpHeaders.CONTENT_TYPE))).name();

                String key;
                try {
                    key = decode ? URLDecoder.decode(entry.getKey(), charsetName) : URLEncoder.encode(entry.getKey(),
                            charsetName);

                    for (String value : entry.getValue()) {
                        newForm.asMap().add(key, decode ? URLDecoder.decode(value, charsetName) : URLEncoder.encode(value,
                                charsetName));
                    }

                } catch (UnsupportedEncodingException uee) {
                    throw new ProcessingException(LocalizationMessages.ERROR_UNSUPPORTED_ENCODING(charsetName,
                            extractor.getName()), uee);
                }
            }
            return newForm;
        }


        private void cacheForm(final HttpContext context, final Form form) {
            context.getRequestContext().setProperty(decode ? HttpContext
                    .FORM_DECODED_PROPERTY : HttpContext.FORM_PROPERTY, form);
        }

        private Form getForm(HttpContext context) {
            return getFormParameters(ensureValidRequest(context.getRequestContext()));
        }

        private Form getCachedForm(final HttpContext context, boolean decode) {
            return (Form) context.getRequestContext().getProperty(decode ? HttpContext
                    .FORM_DECODED_PROPERTY : HttpContext.FORM_PROPERTY);
        }

        private ContainerRequest ensureValidRequest(
                final ContainerRequest requestContext) throws IllegalStateException {
            if (requestContext.getMethod().equals("GET")) {
                throw new IllegalStateException(
                        LocalizationMessages.FORM_PARAM_METHOD_ERROR());
            }

            if (!MediaTypes.typeEqual(MediaType.APPLICATION_FORM_URLENCODED_TYPE, requestContext.getMediaType())) {
                throw new IllegalStateException(
                        LocalizationMessages.FORM_PARAM_CONTENT_TYPE_ERROR());
            }
            return requestContext;
        }

        private final static Annotation encodedAnnotation = getEncodedAnnotation();

        private static Annotation getEncodedAnnotation() {
            /**
             * Encoded-annotated class.
             */
            @Encoded
            final class EncodedAnnotationTemp {
            }
            return EncodedAnnotationTemp.class.getAnnotation(Encoded.class);
        }

        private Form getFormParameters(ContainerRequest requestContext) {
            if (MediaTypes.typeEqual(MediaType.APPLICATION_FORM_URLENCODED_TYPE, requestContext.getMediaType())) {
                requestContext.bufferEntity();
                Form form;
                if (decode) {
                    form = requestContext.readEntity(Form.class);
                } else {
                    Annotation[] annotations = new Annotation[1];
                    annotations[0] = encodedAnnotation;
                    form = requestContext.readEntity(Form.class, annotations);
                }

                return (form == null ? new Form() : form);
            } else {
                return new Form();
            }
        }
    }

    /**
     * Injection constructor.
     *
     * @param mpep     extractor provider.
     * @param injector injector.
     */
    @Inject
    public FormParamValueFactoryProvider(MultivaluedParameterExtractorProvider mpep, ServiceLocator injector) {
        super(mpep, injector, Parameter.Source.FORM);
    }

    @Override
    public AbstractHttpContextValueFactory<?> createValueFactory(Parameter parameter) {
        String parameterName = parameter.getSourceName();

        if (parameterName == null || parameterName.isEmpty()) {
            // Invalid query parameter name
            return null;
        }

        MultivaluedParameterExtractor e = get(parameter);
        if (e == null) {
            return null;
        }
        return new FormParamValueFactory(e, !parameter.isEncoded());
    }
}
