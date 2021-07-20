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

package org.glassfish.jersey.examples.managedclientsimple.resources;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.Uri;

/**
 * A resource which use managed client injected by {@link org.glassfish.jersey.server.Uri @Uri annotation} to query
 * external resources and resource from {@link StandardResource}.
 *
 * @author Miroslav Fuksa
 *
 */
@Path("client")
public class ClientResource {

    /**
     * Make request to external web site using injected client. The response from the injected client is then
     * returned as a response from this resource method.
     *
     * @param webTarget Injected web target.
     * @return Response.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("glassfish")
    public Response glassfish(@Uri("http://glassfish.java.net/") WebTarget webTarget) {
        final Response response = webTarget.request().get();
        return Response.fromResponse(response).build();
    }

    /**
     * Query {@link StandardResource} and return result based on the results from methods of the {@link StandardResource}.
     *
     * @param dogWebTarget Injected client.
     * @param catWebTarget Injected client.
     * @param elefantWebTarget Injected client.
     * @return String entity.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("animals")
    public String animals(@Uri("resource/dog") WebTarget dogWebTarget,
                          @Uri("resource/cat") WebTarget catWebTarget,
                          @Uri("resource/elefant") WebTarget elefantWebTarget) {

        final String dog = dogWebTarget.request().get(String.class);
        final String cat = catWebTarget.request().get(String.class);
        final String elefant = elefantWebTarget.request().get(String.class);
        return "Queried animals: " + dog + " and " + cat + " and " + elefant;
    }

    /**
     * Query {@link StandardResource} using a injected client. The client injection is using a template parameter {@code id}
     * which is filled by JAX-RS implementation using a path parameter of this resource method.
     *
     * @param webTarget Injected client.
     * @param id Path parameter.
     * @return String entity.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("car/{id}")
    public String car(@Uri("resource/car/{id}") WebTarget webTarget, @PathParam("id") String id) {
        final Response response = webTarget.request().get();
        return "Response from resource/car/" + id + ": " + response.readEntity(String.class);
    }
}
