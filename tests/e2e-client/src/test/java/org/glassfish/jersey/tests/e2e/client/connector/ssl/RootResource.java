/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.client.connector.ssl;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.glassfish.jersey.internal.util.Base64;

/**
 * Simple resource demonstrating low level approach of getting user credentials.
 *
 * A better way would be injecting {@link javax.ws.rs.core.SecurityContext}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Path("/")
public class RootResource {
    private static final Logger LOGGER = Logger.getLogger(RootResource.class.getName());
    /**
     * Served content.
     */
    public static final String CONTENT = "JERSEY HTTPS EXAMPLE\n";

    /**
     * Serve content.
     *
     * @param headers request headers.
     * @return content (see {@link #CONTENT}).
     */
    @GET
    public String getContent(@Context HttpHeaders headers) {
        // you can get username form HttpHeaders
        LOGGER.info("Service: GET / User: " + getUser(headers));

        return CONTENT;
    }

    private String getUser(HttpHeaders headers) {
        // this is a very minimalistic and "naive" code;
        // if you plan to use it, add the necessary checks
        String auth = headers.getRequestHeader("authorization").get(0);

        auth = auth.substring("Basic ".length());
        String[] values = Base64.decodeAsString(auth).split(":");

        // String username = values[0];
        // String password = values[1];
        return values[0];
    }
}
