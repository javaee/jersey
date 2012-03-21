/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.api;

import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ResponseIntegrationTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig rc = ResourceConfig.builder().addClasses(ResponseIntegrationTest.ResponseTest.class).build();
        return Application.builder(rc).build();
    }

    @Path(value = "/ResponseTest")
    public static class ResponseTest {

        @GET
        @Produces(value = "text/plain")
        public Response statusTest(@QueryParam("status") int status) {
            Response res;
            Response.ResponseBuilder resp;

            StringBuilder sb = new StringBuilder();
            sb.append("status code in request = ").append(status);

            switch (status) {
                case 200:
                    resp = Response.ok();
                    break;
                case 204:
                case 201:
                case 202:
                case 303:
                case 304:
                case 307:
                case 400:
                case 401:
                case 403:
                case 404:
                case 406:
                case 409:
                case 410:
                case 415:
                case 500:
                case 503:
                case 411:
                case 412:
                    resp =
                            Response.status(status);
                    break;
                default:
                    resp =
                            Response.ok().entity("Unexpected parameter in request: " +
                                    status);
                    break;
            }

            res = resp.header("TESTHEADER", sb.toString()).build();
            return res;
        }
    }


    private void testStatus(int status) {
        final Response response = target()
                .path("ResponseTest")
                .queryParam("status", status)
                .request(MediaType.TEXT_PLAIN)
                .get(Response.class);

        assertEquals(status, response.getStatus());
    }

    /*
     * Client send request to a resource,
     * verify that correct status code returned
     */
    @Test
    public void testStatuses() {
        final List<Integer> statuses = new ArrayList<Integer>(){{
            add(200);
            add(201);
            add(202);
            add(204);
            add(303);
            add(304);
            add(307);
            add(401);
            add(403);
            add(404);
            add(406);
            add(409);
            add(410);
            add(411);
            add(412);
            add(415);
            add(500);
            add(503);
        }};

        for(Integer i : statuses) {
            System.out.println("### Testing status: " + i);
            testStatus(i);
        }

        Response.status(123);
    }
}
