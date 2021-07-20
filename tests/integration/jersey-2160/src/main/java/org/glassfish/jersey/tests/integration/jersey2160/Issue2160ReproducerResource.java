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

package org.glassfish.jersey.tests.integration.jersey2160;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.hk2.api.ProxyCtl;

/**
 * Test resource.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Path("servletInjectees")
public class Issue2160ReproducerResource {

    @Context
    HttpServletRequest requestField;
    @Context
    HttpServletResponse responseField;

    @GET
    @Produces("text/plain")
    public String ensureNoProxyInjected(@Context HttpServletRequest requestParam, @Context HttpServletResponse responseParam) {

        // make sure the injectees are same no matter how they got injected
        if (requestParam != requestField || responseParam != responseField) {
            throw new IllegalArgumentException("injected field and parameter should refer to the same instance");
        }

        // make sure we have not got proxies
        if (requestParam instanceof ProxyCtl || responseParam instanceof ProxyCtl) {
            throw new IllegalArgumentException("no proxy expected!");
        }

        return (String) requestParam.getAttribute(RequestFilter.REQUEST_NUMBER_PROPERTY);
    }
}
