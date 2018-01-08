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

package org.glassfish.jersey.examples.managedclient;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A filter for appending and validating custom headers.
 * <p>
 * On the client side, appends a new custom request header with a configured name and value to each outgoing request.
 * </p>
 * <p>
 * On the server side, validates that each request has a custom header with a configured name and value.
 * If the validation fails a HTTP 403 response is returned.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class CustomHeaderFilter implements ContainerRequestFilter, ClientRequestFilter {
    private final String headerName;
    private final String headerValue;

    public CustomHeaderFilter(String headerName, String headerValue) {
        if (headerName == null || headerValue == null) {
            throw new IllegalArgumentException("Header name and value must not be null.");
        }
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException { // validate
        if (!headerValue.equals(ctx.getHeaderString(headerName))) {
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(String.format("Expected header '%s' not present or value not equal to '%s'", headerName, headerValue))
                    .build());
        }
    }

    @Override
    public void filter(ClientRequestContext ctx) throws IOException { // append
        ctx.getHeaders().putSingle(headerName, headerValue);
    }
}
