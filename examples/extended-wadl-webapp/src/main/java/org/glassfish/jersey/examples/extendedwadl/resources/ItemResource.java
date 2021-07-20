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

package org.glassfish.jersey.examples.extendedwadl.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.examples.extendedwadl.Item;

import org.codehaus.jettison.json.JSONArray;

/**
 * This resource is used to manage a single item.
 *
 * @author Martin Grotzke (martin.grotzke@freiheit.com)
 */
public class ItemResource {

    private final Item _item;

    public ItemResource(Item item) {
        _item = item;
    }

    /**
     * Typically returns the item if it exists. Please be aware that this method is extremely
     * expensive, so we can't guarantee that getting items is all the time possible.
     *
     * @response.representation.200.qname {http://www.example.com}item
     * @response.representation.200.mediaType application/xml
     * @response.representation.200.doc This is the representation returned by default
     *                                  (if we have an even number of millis since 1970...:)
     * @response.representation.200.example {@link org.glassfish.jersey.examples.extendedwadl.util.Examples#SAMPLE_ITEM}
     *
     * @response.representation.503.mediaType text/plain
     * @response.representation.503.example You'll get some explanation why this service is not available
     *
     * @return the requested item if this service is available, otherwise a 503.
     */
    @GET
    @Produces({"application/xml", "text/plain"})
    public Response getItem() {
        if (System.currentTimeMillis() % 2 == 0) {
            return Response.status(Status.SERVICE_UNAVAILABLE)
                    .entity("Sorry, but right now we can't process this request,"
                            + " try again an odd number of milliseconds later, please :)")
                    .type(MediaType.TEXT_PLAIN).build();
        }
        return Response.ok(_item).build();
    }

    /**
     * Tries hard to return the item if it exists. If "Try-Hard" header is set to "true", the method is guaranteed to always
     * complete successfully if the item exists.
     *
     * @request.param {@name Try-Hard}
     *                  {@style header}
     *                  {@type {http://www.w3.org/2001/XMLSchema}string}
     *                  {@doc If set to "true", the call will always succeed provided the item exists.}
     *
     * @response.representation.200.qname {http://www.example.com}item
     * @response.representation.200.mediaType application/xml
     * @response.representation.200.doc This is the representation returned by default
     *                                  (if we have an even number of millis since 1970...:)
     * @response.representation.200.example {@link org.glassfish.jersey.examples.extendedwadl.util.Examples#SAMPLE_ITEM}
     *
     * @response.representation.503.mediaType text/plain
     * @response.representation.503.example You'll get some explanation why this service is not available.
     *
     * @return the requested item if it exists and the "Try-Hard" header is set to "true", otherwise a 503.
     */
    // Method added to reproduce OWLS-24243 issue.
    @GET
    @Produces({"application/xml", "text/plain"})
    @Path("try-hard")
    public Response getItem(@HeaderParam("Try-Hard") boolean tryHard) {
        if (!tryHard) {
            return Response.status(Status.SERVICE_UNAVAILABLE)
                    .entity("Sorry, but right now we can't process this request,"
                            + " try again and set the \"Try-Hard\" header to \"true\"")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        return Response.ok(_item).build();
    }


    /**
     * Returns the item if existing.
     *
     * @response.representation.200.mediaType application/json
     * @response.representation.200.example ["myValue"]
     *
     * @return the requested item.
     */
    @GET
    @Produces({"application/json"})
    public JSONArray getItemAsJSON() {
        final JSONArray result = new JSONArray();
        result.put(_item.getValue());
        return result;
    }

    /**
     * Update the value property of the current item.
     *
     * @param value the new value to set
     */
    @Path("value/{value}")
    @PUT
    @Consumes({"application/xml"})
    public void updateItemValue(@PathParam("value") String value) {
        _item.setValue(value);
    }

}
