/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.feedcombiner.resources;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import javax.inject.Inject;
import javax.validation.Valid;

import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.model.FeedEntry;
import org.glassfish.jersey.examples.feedcombiner.service.CrudService;
import org.glassfish.jersey.server.validation.ValidationError;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * Expose REST API for manipulating with Feeds
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
@Path("feeds")
public class CombinedFeedResource {

    @Context
    private UriInfo uriInfo;

    @Inject
    private CrudService<CombinedFeed> feedService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Valid CombinedFeed insertedFeed) {
        CombinedFeed feed = feedService.save(insertedFeed);
        return Response.created(getEntityLocation(feed.getId())).entity(feed).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String feedId) {
        Serializable deleted = feedService.delete(feedId);

        if (deleted != null) {
            return Response.noContent().build();
        } else {
            throw notFoundException(feedId);
        }
    }

    @GET
    @Path("/{id}/entries")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response getEntries(@PathParam("id") String feedId) {
        CombinedFeed combinedFeed = feedService.get(feedId);

        if (combinedFeed != null) {
            return Response.ok(new GenericEntity<List<FeedEntry>>(combinedFeed.getFeedEntries()) {}).build();
        } else {
            throw notFoundException(feedId);
        }
    }

    private NotFoundException notFoundException(String feedId) {
        String message = "No Combined Feed was found with ID: " + feedId;
        Response response = Response.status(NOT_FOUND)
                .entity(new ValidationError(message, null, null, feedId))
                .build();

        return new NotFoundException(message, response);
    }

    private URI getEntityLocation(String entityId) {
        URI absolutePath = uriInfo.getAbsolutePath();
        return UriBuilder.fromUri(absolutePath).path("/" + entityId).build();
    }

}
