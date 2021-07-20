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

package org.glassfish.jersey.examples.jsonp.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.glassfish.jersey.examples.jsonp.service.DocumentStorage;

/**
 * Document Resource.
 *
 * @author Michal Gajdos
 */
@Path("document")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @GET
    public JsonArray getAll() {
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (final JsonObject document : DocumentStorage.getAll()) {
            arrayBuilder.add(document);
        }
        return arrayBuilder.build();
    }

    @GET
    @Path("{id: \\d+}")
    public JsonObject get(@PathParam("id") final int id) {
        return DocumentStorage.get(id);
    }

    @DELETE
    @Path("{id: \\d+}")
    public JsonObject remove(@PathParam("id") final int id) {
        return DocumentStorage.remove(id);
    }

    @DELETE
    public void removeAll() {
        DocumentStorage.removeAll();
    }

    @POST
    public JsonArray store(final JsonObject document) {
        return Json.createArrayBuilder().add(DocumentStorage.store(document)).build();
    }

    @POST
    @Path("multiple")
    public JsonArray store(final JsonArray documents) {
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (final JsonObject document : documents.getValuesAs(JsonObject.class)) {
            arrayBuilder.add(DocumentStorage.store(document));
        }
        return arrayBuilder.build();
    }

    @Path("filter")
    public Class<DocumentFilteringResource> getFilteringResource() {
        return DocumentFilteringResource.class;
    }
}
