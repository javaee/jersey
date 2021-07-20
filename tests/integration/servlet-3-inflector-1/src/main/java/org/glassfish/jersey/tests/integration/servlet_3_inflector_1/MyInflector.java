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

package org.glassfish.jersey.tests.integration.servlet_3_inflector_1;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.process.Inflector;

/**
 * @author Michal Gajdos
 */
public class MyInflector implements Inflector<ContainerRequestContext, Response> {

    @Inject
    private Provider<HttpServletRequest> requestProvider;
    @Inject
    private Provider<HttpServletResponse> responseProvider;

    @Override
    public Response apply(final ContainerRequestContext requestContext) {
        final StringBuilder stringBuilder = new StringBuilder();

        // Request provider & request.
        if (requestProvider != null) {
            stringBuilder.append("requestProvider_");
            stringBuilder.append(requestProvider.get() != null ? "request" : null);
        } else {
            stringBuilder.append("null_null");
        }

        stringBuilder.append('_');

        // Response provider & response.
        if (responseProvider != null) {
            stringBuilder.append("responseProvider_");
            stringBuilder.append(responseProvider.get() != null ? "response" : null);
        } else {
            stringBuilder.append("null_null");
        }

        return Response.ok(stringBuilder.toString()).build();
    }
}
