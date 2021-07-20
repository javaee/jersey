/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.bookmark_em.resource;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

import org.glassfish.jersey.examples.bookmark_em.entity.BookmarkEntity;
import org.glassfish.jersey.examples.bookmark_em.util.tx.TransactionManager;
import org.glassfish.jersey.examples.bookmark_em.util.tx.Transactional;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class BookmarksResource {

    UriInfo uriInfo; // actual uri info
    EntityManager em; // entity manager provided by parent resource
    UserTransaction utx; // user transaction provided by parent resource

    UserResource userResource; // parent user resource

    /**
     * Creates a new instance of BookmarksResource
     */
    public BookmarksResource(UriInfo uriInfo, EntityManager em, UserTransaction utx, UserResource userResource) {
        this.uriInfo = uriInfo;
        this.em = em;
        this.utx = utx;
        this.userResource = userResource;
    }

    public Collection<BookmarkEntity> getBookmarks() {
        return userResource.getUserEntity().getBookmarkEntityCollection();
    }

    @Path("{bmid: .+}")
    public BookmarkResource getBookmark(@PathParam("bmid") String bmid) {
        return new BookmarkResource(uriInfo, em, utx, userResource.getUserEntity(), bmid);
    }

    @GET
    @Produces("application/json")
    public JSONArray getBookmarksAsJsonArray() {
        JSONArray uriArray = new JSONArray();
        for (BookmarkEntity bookmarkEntity : getBookmarks()) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI bookmarkUri = ub
                    .path(bookmarkEntity.getBookmarkEntityPK().getBmid())
                    .build();
            uriArray.put(bookmarkUri.toASCIIString());
        }
        return uriArray;
    }

    @POST
    @Consumes("application/json")
    public Response postForm(JSONObject bookmark) throws JSONException {
        final BookmarkEntity bookmarkEntity = new BookmarkEntity(getBookmarkId(bookmark.getString("uri")),
                userResource.getUserEntity().getUserid());

        bookmarkEntity.setUri(bookmark.getString("uri"));
        bookmarkEntity.setUpdated(new Date());
        bookmarkEntity.setSdesc(bookmark.getString("sdesc"));
        bookmarkEntity.setLdesc(bookmark.getString("ldesc"));
        userResource.getUserEntity().getBookmarkEntityCollection().add(bookmarkEntity);

        TransactionManager.manage(utx, new Transactional(em) {
            public void transact() {
                em.merge(userResource.getUserEntity());
            }
        });

        URI bookmarkUri = uriInfo.getAbsolutePathBuilder()
                .path(bookmarkEntity.getBookmarkEntityPK().getBmid())
                .build();
        return Response.created(bookmarkUri).build();
    }

    private String getBookmarkId(String uri) {
        return uri;
    }
}
