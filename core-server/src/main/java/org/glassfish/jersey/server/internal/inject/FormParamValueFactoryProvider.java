/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.FormParam;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.internal.ExtractorException;

import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

/**
 * Value factory provider supporting the {@link FormParam} injection annotation.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class FormParamValueFactoryProvider extends AbstractValueFactoryProvider<FormParam> {

    static final class InjectionResolver extends ParamInjectionResolver<FormParam> {

        public InjectionResolver() {
            super(FormParam.class, FormParamValueFactoryProvider.class);
        }
    }


    private static final class FormParamValueFactory extends AbstractHttpContextValueFactory<Object> {

        private final MultivaluedParameterExtractor<?> extractor;

        FormParamValueFactory(MultivaluedParameterExtractor<?> extractor, boolean decode) {
            this.extractor = extractor;
        }

        @Override
        public Object get(HttpContext context) {

            Form form = getCachedForm(context);

            if (form == null) {
                form = getForm(context);
                cacheForm(context, form);
            }

            try {
                return extractor.extract(form.asMap());
            } catch (ExtractorException e) {
                throw new ParamException.FormParamException(e.getCause(),
                        extractor.getName(), extractor.getDefaultValueString());
            }
        }

        private void cacheForm(final HttpContext context, final Form form) {
            context.getProperties().put(HttpContext.FORM_PROPERTY, form);
        }

        private Form getForm(HttpContext context) {
            final Request request = ensureValidRequest(context.getRequest());
            return getFormParameters(request);
        }

        private Form getCachedForm(final HttpContext context) {
            return (Form) context.getProperties().get(HttpContext.FORM_PROPERTY);
        }

        private Request ensureValidRequest(final Request request) throws IllegalStateException {
            if (request.getMethod().equals("GET")) {
                throw new IllegalStateException(
                        LocalizationMessages.FORM_PARAM_METHOD_ERROR());
            }

            if (!MediaTypes.typeEqual(MediaType.APPLICATION_FORM_URLENCODED_TYPE, request.getHeaders().getMediaType())) {
                throw new IllegalStateException(
                        LocalizationMessages.FORM_PARAM_CONTENT_TYPE_ERROR());
            }
            return request;
        }

        private Form getFormParameters(Request request) {
            if (request.getHeaders().getMediaType().equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
                request.bufferEntity();
                Form f = request.readEntity(Form.class);
                return (f == null ? new Form() : f);
            } else {
                return new Form();
            }
        }
    }

    public FormParamValueFactoryProvider(@Inject MultivaluedParameterExtractorProvider mpep, @Inject Injector injector) {
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
