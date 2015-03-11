/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey2176;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class TraceResponseFilter implements Filter {

    public static final String X_SERVER_DURATION_HEADER = "X-SERVER-DURATION";
    public static final String X_STATUS_HEADER = "X-STATUS";
    public static final String X_NO_FILTER_HEADER = "X-NO-FILTER";

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest request, ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        TraceResponseWrapper wrappedResponse = null;
        if (((HttpServletRequest) request).getHeader(X_NO_FILTER_HEADER) == null) {
            response = wrappedResponse = new TraceResponseWrapper((HttpServletResponse) response);
        }
        String status = "n/a";
        final long startTime = System.nanoTime();
        try {
            chain.doFilter(request, response);
            status = "OK";
        } catch (final Throwable th) {
            status = "FAIL";
        } finally {
            final long duration = System.nanoTime() - startTime;
            ((HttpServletResponse) response).addHeader(X_SERVER_DURATION_HEADER, String.valueOf(duration));
            ((HttpServletResponse) response).addHeader(X_STATUS_HEADER, status);
            if (wrappedResponse != null) {
                ((HttpServletResponse) response).setHeader(HttpHeaders.CONTENT_LENGTH, wrappedResponse.getContentLength());
                wrappedResponse.writeBodyAndClose(response.getCharacterEncoding());
            }
        }
    }

}
