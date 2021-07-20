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

package org.glassfish.jersey.examples.bookmark.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import javax.persistence.EntityManager;

import org.glassfish.jersey.examples.bookmark.entity.UserEntity;
import org.glassfish.jersey.examples.bookmark.exception.ExtendedNotFoundException;
import org.glassfish.jersey.examples.bookmark.util.tx.TransactionManager;
import org.glassfish.jersey.examples.bookmark.util.tx.Transactional;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class UserResource {

    String userid; // userid from url
    UserEntity userEntity; // appropriate jpa user entity

    UriInfo uriInfo; // actual uri info provided by parent resource
    EntityManager em; // entity manager provided by parent resource

    /**
     * Creates a new instance of UserResource
     */
    public UserResource(UriInfo uriInfo, EntityManager em, String userid) {
        this.uriInfo = uriInfo;
        this.userid = userid;
        this.em = em;
        userEntity = em.find(UserEntity.class, userid);
    }

    @Path("bookmarks/")
    public BookmarksResource getBookmarksResource() {
        if (null == userEntity) {
            throw new ExtendedNotFoundException("userid " + userid + " does not exist!");
        }
        return new BookmarksResource(uriInfo, em, this);
    }

    @GET
    @Produces("application/json")
    public JSONObject getUser() throws JSONException {
        if (null == userEntity) {
            throw new ExtendedNotFoundException("userid " + userid + "does not exist!");
        }
        return new JSONObject().put("userid", userEntity.getUserid())
                .put("username", userEntity.getUsername())
                .put("email", userEntity.getEmail())
                .put("password", userEntity.getPassword())
                .put("bookmarks", uriInfo.getAbsolutePathBuilder().path("bookmarks").build());
    }

    @PUT
    @Consumes("application/json")
    public Response putUser(JSONObject jsonEntity) throws JSONException {

        String jsonUserid = jsonEntity.getString("userid");

        if ((null != jsonUserid) && !jsonUserid.equals(userid)) {
            return Response.status(409).entity("userids differ!\n").build();
        }

        final boolean newRecord = (null == userEntity); // insert or update ?

        if (newRecord) { // new user record to be inserted
            userEntity = new UserEntity();
            userEntity.setUserid(userid);
        }
        userEntity.setUsername(jsonEntity.getString("username"));
        userEntity.setEmail(jsonEntity.getString("email"));
        userEntity.setPassword(jsonEntity.getString("password"));

        if (newRecord) {
            TransactionManager.manage(new Transactional(em) {
                public void transact() {
                    em.persist(userEntity);
                }
            });
            return Response.created(uriInfo.getAbsolutePath()).build();
        } else {
            TransactionManager.manage(new Transactional(em) {
                public void transact() {
                    em.merge(userEntity);
                }
            });
            return Response.noContent().build();
        }
    }

    @DELETE
    public void deleteUser() {
        if (null == userEntity) {
            throw new ExtendedNotFoundException("userid " + userid + "does not exist!");
        }
        TransactionManager.manage(new Transactional(em) {
            public void transact() {
                em.remove(userEntity);
            }
        });
    }

    public String toString() {
        return userEntity.getUserid();
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }
}
