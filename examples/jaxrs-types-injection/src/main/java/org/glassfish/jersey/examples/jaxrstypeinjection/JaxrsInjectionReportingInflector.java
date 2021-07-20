/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.jaxrstypeinjection;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.process.Inflector;

/**
 * Programmatic resource.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class JaxrsInjectionReportingInflector implements Inflector<Request, Response> {

    @Inject
    HttpHeaders httpHeaders;
    @Inject
    UriInfo uriInfo;
    @PathParam(value = "p1")
    String p1;
    private PathSegment p2;

    @PathParam(value = "p2")
    public void setP2(PathSegment p2) {
        this.p2 = p2;
    }
    @QueryParam(value = "q1")
    private int q1;
    @QueryParam(value = "q2")
    private List<String> q2;

    @Override
    public Response apply(Request data) {
        StringBuilder sb = ReportBuilder.append(
                new StringBuilder("Injected information:\n"), uriInfo, httpHeaders);
        sb.append("\n URI component injection:");
        sb.append("\n   String path param p1=").append(p1);
        sb.append("\n   PathSegment path param p2=").append(p2);
        sb.append("\n   int query param q1=").append(q1);
        sb.append("\n   List<String> query param q2=").append(q2);
        return Response.ok(sb.toString(), MediaType.TEXT_PLAIN).build();
    }
}
