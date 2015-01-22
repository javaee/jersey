/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.resources;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * JERSEY-2526 reproducer. CDI managed JAX-RS root resource
 * that is constructor injected with JAX-RS parameters provided by Jersey
 * and a single String parameter coming from application provided CDI producer,
 * {@link CustomCdiProducer}.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Path("ctor-injected/{p}")
@RequestScoped
public class ConstructorInjectedResource {

    String pathParam;
    String queryParam;
    String matrixParam;
    String headerParam;
    String cdiParam;

    /**
     * WLS requires this.
     */
    public ConstructorInjectedResource() {
    }

    /**
     * This will get CDI injected with JAX-RS provided parameters.
     *
     * @param pathParam path parameter from the actual request.
     * @param queryParam query parameter q from the actual request.
     * @param matrixParam matrix parameter m from the actual request.
     * @param headerParam Accept header parameter from the actual request.
     * @param cdiParam custom CDI produced string.
     */
    @Inject
    public ConstructorInjectedResource(
            @PathParam("p") String pathParam,
            @QueryParam("q") String queryParam,
            @MatrixParam("m") String matrixParam,
            @HeaderParam("Custom-Header") String headerParam,
            @CustomCdiProducer.Qualifier String cdiParam) {

        this.pathParam = pathParam;
        this.queryParam = queryParam;
        this.matrixParam = matrixParam;
        this.headerParam = headerParam;
        this.cdiParam = cdiParam;
    }

    /**
     * Provide string representation of a single injected parameter
     * given by the actual path parameter (that is also injected).
     *
     * @return a single parameter value.
     */
    @GET
    public String getParameter() {

        switch (pathParam) {

            case "pathParam": return pathParam;
            case "queryParam": return queryParam;
            case "matrixParam": return matrixParam;
            case "headerParam": return headerParam;
            case "cdiParam": return cdiParam;

            default: throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
}
