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

package org.glassfish.jersey.examples.linking.resources;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.examples.linking.model.ItemsModel;
import org.glassfish.jersey.examples.linking.representation.ItemsRepresentation;

/**
 * Resource that provides access to the entire list of items
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
@Path("items")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class ItemsResource {

    private ItemsModel itemsModel;

    public ItemsResource() {
        itemsModel = ItemsModel.getInstance();
    }

    @GET
    public ItemsRepresentation query(
            @Context javax.ws.rs.core.UriInfo info,
            @QueryParam("offset") @DefaultValue("-1") int offset, @DefaultValue("-1") @QueryParam("limit") int limit) {

        if (offset == -1 || limit == -1) {
            offset = offset == -1 ? 0 : offset;
            limit = limit == -1 ? 10 : limit;

            throw new WebApplicationException(
                    Response.seeOther(info.getRequestUriBuilder().queryParam("offset", offset)
                            .queryParam("limit", limit).build())
                            .build()
            );
        }

        return new ItemsRepresentation(itemsModel, offset, limit);
    }

    @Path("{id}")
    public ItemResource get(@PathParam("id") String id) {
        return new ItemResource(itemsModel, id);
    }

}
