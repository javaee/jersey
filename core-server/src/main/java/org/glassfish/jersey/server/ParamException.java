/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.lang.annotation.Annotation;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An abstract extension of {@link WebApplicationException} for the class of
 * parameter-based exceptions.
 * <p>
 * Exceptions that are instances of this class will be thrown if the runtime
 * encounters an error obtaining a parameter value, from a request, for a
 * Java type that is annotated with a parameter-based annotation, such as
 * {@link QueryParam}. For more details see
 * <a href="http://jsr311.java.net/nonav/releases/1.0/spec/index.html">section 3.2</a>
 * of the JAX-RS specification.
 * <p>
 * An {@link ExceptionMapper} may be configured to map this class or a sub-class
 * of to customize responses for parameter-based errors.
 * <p>
 * Unless otherwise stated all such exceptions of this type will contain a
 * response with a 400 (Client error) status code.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class ParamException extends WebApplicationException {

    private static final long serialVersionUID = -2359567574412607846L;

    /**
     * An abstract parameter exception for the class of URI-parameter-based
     * exceptions.
     * <p>
     * All such exceptions of this type will contain a response with a 404
     * (Not Found) status code.
     */
    public abstract static class UriParamException extends ParamException {

        private static final long serialVersionUID = 44233528459885541L;

        protected UriParamException(Throwable cause,
                                    Class<? extends Annotation> parameterType, String name, String defaultStringValue) {
            super(cause, Response.Status.NOT_FOUND, parameterType, name, defaultStringValue);
        }
    }

    /**
     * A URI-parameter-based exception for errors with {@link PathParam}.
     */
    public static class PathParamException extends UriParamException {

        private static final long serialVersionUID = -2708538214692835633L;

        public PathParamException(Throwable cause, String name, String defaultStringValue) {
            super(cause, PathParam.class, name, defaultStringValue);
        }
    }

    /**
     * A URI-parameter-based exception for errors with {@link MatrixParam}.
     */
    public static class MatrixParamException extends UriParamException {

        private static final long serialVersionUID = -5849392883623736362L;

        public MatrixParamException(Throwable cause, String name, String defaultStringValue) {
            super(cause, MatrixParam.class, name, defaultStringValue);
        }
    }

    /**
     * A URI-parameter-based exception for errors with {@link QueryParam}.
     */
    public static class QueryParamException extends UriParamException {

        private static final long serialVersionUID = -4822407467792322910L;

        public QueryParamException(Throwable cause, String name, String defaultStringValue) {
            super(cause, QueryParam.class, name, defaultStringValue);
        }
    }

    /**
     * A parameter exception for errors with {@link HeaderParam}.
     */
    public static class HeaderParamException extends ParamException {

        private static final long serialVersionUID = 6508174603506313274L;

        public HeaderParamException(Throwable cause, String name, String defaultStringValue) {
            super(cause, Response.Status.BAD_REQUEST, HeaderParam.class, name, defaultStringValue);
        }
    }

    /**
     * A parameter exception for errors with {@link CookieParam}.
     */
    public static class CookieParamException extends ParamException {

        private static final long serialVersionUID = -5288504201234567266L;

        public CookieParamException(Throwable cause, String name, String defaultStringValue) {
            super(cause, Response.Status.BAD_REQUEST, CookieParam.class, name, defaultStringValue);
        }
    }

    /**
     * A parameter exception for errors with {@link FormParam}.
     */
    public static class FormParamException extends ParamException {

        private static final long serialVersionUID = -1704379792199980689L;

        public FormParamException(Throwable cause, String name, String defaultStringValue) {
            super(cause, Response.Status.BAD_REQUEST, FormParam.class, name, defaultStringValue);
        }
    }

    private final Class<? extends Annotation> parameterType;
    private final String name;
    private final String defaultStringValue;

    protected ParamException(Throwable cause, Response.StatusType status,
                             Class<? extends Annotation> parameterType, String name, String defaultStringValue) {
        super(cause, status.getStatusCode());
        this.parameterType = parameterType;
        this.name = name;
        this.defaultStringValue = defaultStringValue;
    }

    /**
     * Get the type of the parameter annotation.
     *
     * @return the type of the parameter annotation.
     */
    public Class<? extends Annotation> getParameterType() {
        return parameterType;
    }

    /**
     * Get the parameter name.
     *
     * @return the parameter name.
     */
    public String getParameterName() {
        return name;
    }

    /**
     * Get the default String value.
     *
     * @return the default String value.
     */
    public String getDefaultStringValue() {
        return defaultStringValue;
    }
}
