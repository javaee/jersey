/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey1960;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.message.MessageUtils;

/**
 * Filter testing injection support for of servlet artifacts.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@PreMatching
public class RequestFilter implements ContainerRequestFilter {
    public static final String REQUEST_NUMBER = "request-number";
    @Context
    private HttpServletRequest hsReq;
    @Context
    private HttpServletResponse hsResp;
    @Context
    private ServletContext sCtx;
    @Context
    private ServletConfig sCfg;

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final StringBuilder sb = new StringBuilder();

        // First, make sure there are no null injections.
        if (hsReq == null) {
            sb.append("HttpServletRequest is null.\n");
        }
        if (hsResp == null) {
            sb.append("HttpServletResponse is null.\n");
        }
        if (sCtx == null) {
            sb.append("ServletContext is null.\n");
        }
        if (sCfg == null) {
            sb.append("ServletConfig is null.\n");
        }

        if (sb.length() > 0) {
            ctx.abortWith(Response.serverError().entity(sb.toString()).build());
        }

        // let's also test some method calls
        int flags = 0;

        if ("/jersey-1960".equals(hsReq.getServletPath())) {
            flags += 1;
        }
        if (!hsResp.isCommitted()) {
            flags += 10;
        }
        if (!sCtx.getServerInfo().isEmpty()) {
            flags += 100;
        }
        if (sCfg.getServletContext() == sCtx) {
            flags += 1000;
        }
        final String header = hsReq.getHeader(REQUEST_NUMBER);

        ctx.setEntityStream(new ByteArrayInputStream(("filtered-" + flags + "-" + header).getBytes(
                MessageUtils.getCharset(ctx.getMediaType()))));
    }
}
