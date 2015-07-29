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

package org.glassfish.jersey.server.validation.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Provider;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.validation.ValidationError;

/**
 * {@link ExceptionMapper} for {@link ValidationException}.
 * <p/>
 * If {@value ServerProperties#BV_SEND_ERROR_IN_RESPONSE} property is enabled then a list of {@link ValidationError}
 * instances is sent in {@link Response} as well (in addition to HTTP 400/500 status code). Supported media types are:
 * {@code application/json}/{@code application/xml} (in appropriate provider is registered on server) or
 * {@code text/html}/{@code text/plain} (via custom {@link ValidationErrorMessageBodyWriter}).
 *
 * @author Michal Gajdos
 */
public final class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    private static final Logger LOGGER = Logger.getLogger(ValidationExceptionMapper.class.getName());

    @Context
    private Configuration config;
    @Context
    private Provider<Request> request;

    @Override
    public Response toResponse(final ValidationException exception) {
        if (exception instanceof ConstraintViolationException) {
            LOGGER.log(Level.FINER, LocalizationMessages.CONSTRAINT_VIOLATIONS_ENCOUNTERED(), exception);

            final ConstraintViolationException cve = (ConstraintViolationException) exception;
            final Response.ResponseBuilder response = Response.status(ValidationHelper.getResponseStatus(cve));

            // Entity.
            final Object property = config.getProperty(ServerProperties.BV_SEND_ERROR_IN_RESPONSE);
            if (property != null && Boolean.valueOf(property.toString())) {
                final List<Variant> variants = Variant.mediaTypes(
                        MediaType.TEXT_PLAIN_TYPE,
                        MediaType.TEXT_HTML_TYPE,
                        MediaType.APPLICATION_XML_TYPE,
                        MediaType.APPLICATION_JSON_TYPE).build();
                final Variant variant = request.get().selectVariant(variants);
                if (variant != null) {
                    response.type(variant.getMediaType());
                } else {

                    // default media type which will be used only when none media type from {@value variants} is in accept
                    // header of original request.
                    // could be settable by configuration property.
                    response.type(MediaType.TEXT_PLAIN_TYPE);
                }
                response.entity(
                        new GenericEntity<>(
                                ValidationHelper.constraintViolationToValidationErrors(cve),
                                new GenericType<List<ValidationError>>() {}.getType()
                        )
                );
            }

            return response.build();
        } else {
            LOGGER.log(Level.WARNING, LocalizationMessages.VALIDATION_EXCEPTION_RAISED(), exception);

            return Response.serverError().entity(exception.getMessage()).build();
        }
    }
}
