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

package org.glassfish.jersey.examples.cdi.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;

/**
 * Shows injection of path and query parameter into a managed bean.
 *
 * @author Roberto Chinnici
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@ManagedBean
@Path("echofield/{b}")
public class EchoParamFieldResource {

    @PathParam("b") String bInjected;

    String b;

    /**
     * Ensure we got path parameter value injected.
     */
    @PostConstruct
    @SuppressWarnings("unused")
    private void postConstruct() {
        if (bInjected == null) {
            throw new IllegalStateException("Field b has not been injected!");
        }
        b = bInjected;
    }

    /**
     * Return a string containing injected values.
     *
     * @param a value of a query parameter a.
     * @return message containing injected values.
     */
    @GET
    @Produces("text/plain")
    public String get(@QueryParam("a") String a) {
        return String.format("ECHO %s %s", a, b);
    }
}
