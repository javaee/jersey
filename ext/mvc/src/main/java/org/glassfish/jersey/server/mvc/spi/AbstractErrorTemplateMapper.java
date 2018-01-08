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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.server.mvc.spi;

import javax.ws.rs.core.Response;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.mvc.ErrorTemplate;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.internal.TemplateInflector;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

/**
 * Default implementation of {@link ExtendedExceptionMapper} used to declare special handling for exception types that should be
 * processed by MVC.
 * <p/>
 * Extensions should override {@link #getErrorStatus(Throwable)} and {@link #getErrorModel(Throwable)} to provide a response
 * status and model derived from a raised throwable.
 * <p/>
 * By default every {@link Exception exception} is mapped and used as a model in a viewable and passed to the MVC runtime for
 * further processing.
 *
 * @param <T> A type of the exception processed by the exception mapper.
 * @author Michal Gajdos
 * @since 2.3
 */
@Singleton
public abstract class AbstractErrorTemplateMapper<T extends Throwable> implements ExtendedExceptionMapper<T> {

    @Inject
    private javax.inject.Provider<ExtendedUriInfo> uriInfoProvider;

    @Override
    public final boolean isMappable(final T throwable) {
        return getErrorTemplate() != null;
    }

    /**
     * Get an {@link ErrorTemplate} annotation from resource method / class the throwable was raised from.
     *
     * @return an error template annotation or {@code null} if the method is not annotated.
     */
    private ErrorTemplate getErrorTemplate() {
        final ExtendedUriInfo uriInfo = uriInfoProvider.get();
        final ResourceMethod matchedResourceMethod = uriInfo.getMatchedResourceMethod();

        if (matchedResourceMethod != null) {
            final Invocable invocable = matchedResourceMethod.getInvocable();

            ErrorTemplate errorTemplate = invocable.getHandlingMethod().getAnnotation(ErrorTemplate.class);
            if (errorTemplate == null) {
                Class<?> handlerClass = invocable.getHandler().getHandlerClass();

                if (invocable.isInflector() && TemplateInflector.class
                        .isAssignableFrom(invocable.getHandler().getHandlerClass())) {

                    handlerClass = ((TemplateInflector) invocable.getHandler().getInstance(null)).getModelClass();
                }

                errorTemplate = handlerClass.getAnnotation(ErrorTemplate.class);
            }

            return errorTemplate;
        }

        return null;
    }

    @Override
    public final Response toResponse(final T throwable) {
        final ErrorTemplate error = getErrorTemplate();
        final String templateName = "".equals(error.name()) ? "index" : error.name();

        return Response
                .status(getErrorStatus(throwable))
                .entity(new Viewable(templateName, getErrorModel(throwable)))
                .build();
    }

    /**
     * Get a model for error template. Default value is the {@code throwable} itself.
     *
     * @param throwable throwable raised during processing a resource method.
     * @return a model for error template.
     */
    protected Object getErrorModel(final T throwable) {
        return throwable;
    }

    /**
     * Get a response status of to-be-processed error template. Default value is {@link Response.Status#OK}.
     *
     * @param throwable throwable raised during processing a resource method.
     * @return response status of error response.
     */
    protected Response.Status getErrorStatus(final T throwable) {
        return Response.Status.OK;
    }
}
