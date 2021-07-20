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

package org.glassfish.jersey.examples.console.resources;

import java.io.InputStream;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

/**
 * A Web form resource, produces the form and processes the results of
 * submitting it.
 *
 */
@Path("/form")
@Produces("text/html")
public class FormResource {

    private static final Colours coloursResource = new Colours();

    @Context
    HttpHeaders headers;

    @Path("colours")
    public Colours getColours() {
        return coloursResource;
    }

    /**
     * Produce a form from a static HTML file packaged with the compiled class
     * @return a stream from which the HTML form can be read.
     */
    @GET
    public Response getForm() {
        Date now = new Date();

        InputStream entity = this.getClass().getClassLoader().getResourceAsStream("form.html");
        return Response.ok(entity)
                .cookie(new NewCookie("date", now.toString())).build();
    }

    /**
     * Process the form submission. Produces a table showing the form field
     * values submitted.
     * @return a dynamically generated HTML table.
     * @param formData the data from the form submission
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public String processForm(MultivaluedMap<String, String> formData) {
        StringBuilder buf = new StringBuilder();
        buf.append("<html><head><title>Form results</title></head><body>");
        buf.append("<p>Hello, you entered the following information: </p><table border='1'>");
        for (String key : formData.keySet()) {
            if (key.equals("submit")) {
                continue;
            }
            buf.append("<tr><td>");
            buf.append(key);
            buf.append("</td><td>");
            buf.append(formData.getFirst(key));
            buf.append("</td></tr>");
        }
        for (Cookie c : headers.getCookies().values()) {
            buf.append("<tr><td>Cookie: ");
            buf.append(c.getName());
            buf.append("</td><td>");
            buf.append(c.getValue());
            buf.append("</td></tr>");
        }

        buf.append("</table></body></html>");
        return buf.toString();
    }

}
