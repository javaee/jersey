/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client.proxy;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@Path("myresource")
public interface MyResourceIfc {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getIt();

    @POST
    @Consumes({MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_XML})
    List<MyBean> postIt(List<MyBean> entity);

    @POST
    @Path("valid")
    @Consumes({MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_XML})
    MyBean postValid(@Valid MyBean entity);

    @Path("{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getId(@PathParam("id") String id);

    @Path("query")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByName(@QueryParam("name") String name);

    @Path("cookie")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameCookie(@CookieParam("cookie-name") String name);

    @Path("header")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameHeader(@HeaderParam("header-name") String name);

    @Path("matrix")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameMatrix(@MatrixParam("matrix-name") String name);

    @Path("form")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    String postByNameFormParam(@FormParam("form-name") String name);


    @Path("query-list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameList(@QueryParam("name-list") List<String> name);

    @Path("query-set")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameSet(@QueryParam("name-set") Set<String> name);

    @Path("query-sortedset")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameSortedSet(@QueryParam("name-sorted") SortedSet<String> name);

    @Path("cookie-list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameCookieList(@CookieParam("cookie-name-list") List<String> name);

    @Path("cookie-set")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameCookieSet(@CookieParam("cookie-name-set") Set<String> name);

    @Path("cookie-sortedset")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameCookieSortedSet(@CookieParam("cookie-name-sorted") SortedSet<String> name);

    @Path("header-list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameHeaderList(@HeaderParam("header-name-list") List<String> name);

    @Path("header-set")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameHeaderSet(@HeaderParam("header-name-set") Set<String> name);

    @Path("header-sortedset")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameHeaderSortedSet(@HeaderParam("header-name-sorted") SortedSet<String> name);

    @Path("matrix-list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameMatrixList(@MatrixParam("matrix-name-list") List<String> name);

    @Path("matrix-set")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameMatrixSet(@MatrixParam("matrix-name-set") Set<String> name);

    @Path("matrix-sortedset")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getByNameMatrixSortedSet(@MatrixParam("matrix-name-sorted") SortedSet<String> name);

    @Path("form-list")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    String postByNameFormList(@FormParam("form-name-list") List<String> name);

    @Path("form-set")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    String postByNameFormSet(@FormParam("form-name-set") Set<String> name);

    @Path("form-sortedset")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    String postByNameFormSortedSet(@FormParam("form-name-sorted") SortedSet<String> name);

    @Path("subresource")
    MySubResourceIfc getSubResource();

    @Path("isAcceptHeaderValid")
    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.TEXT_XML})
    boolean isAcceptHeaderValid(@Context HttpHeaders headers);

    @Path("putIt")
    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    String putIt(MyBean dummyBean);
}
