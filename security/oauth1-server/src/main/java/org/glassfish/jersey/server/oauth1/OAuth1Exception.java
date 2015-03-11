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

package org.glassfish.jersey.server.oauth1;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * {@link WebApplicationException Web application exception} that is mapped either
 * to {@link javax.ws.rs.core.Response.Status#BAD_REQUEST} (e.g. if problem with OAuth
 * parameters occurs) or
 * {@link javax.ws.rs.core.Response.Status#UNAUTHORIZED} (e.g. if signature is incorrect).
 *
 * @author Martin Matula
 * @author Miroslav Fuksa
 */
public class OAuth1Exception extends WebApplicationException {

    /**
     * Create a new exception.
     * @param status Response status.
     * @param wwwAuthHeader {@code Authorization} header value of the request that cause the exception.
     */
    public OAuth1Exception(final Response.Status status, final String wwwAuthHeader) {
        super(createResponse(status, wwwAuthHeader));
    }

    /**
     * Get the status of the error response.
     *
     * @return Response status code.
     */
    public Response.Status getStatus() {
        return Response.Status.fromStatusCode(super.getResponse().getStatus());
    }

    /**
     * Get the {@code WWW-Authenticate} header of the request that cause the exception.
     *
     * @return {@code WWW-Authenticate} header value.
     */
    public String getWwwAuthHeader() {
        return super.getResponse().getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
    }

    private static Response createResponse(Response.Status status, String wwwAuthHeader) {
        ResponseBuilder rb = Response.status(status);
        if (wwwAuthHeader != null) {
            rb.header(HttpHeaders.WWW_AUTHENTICATE, wwwAuthHeader);
        }
        return rb.build();
    }
}

