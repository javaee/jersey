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

package org.glassfish.jersey.examples.flight.resources;

import java.util.Collection;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.annotation.security.RolesAllowed;

import org.glassfish.jersey.examples.flight.filtering.Detail;
import org.glassfish.jersey.examples.flight.internal.DataStore;
import org.glassfish.jersey.examples.flight.internal.SimEngine;
import org.glassfish.jersey.examples.flight.model.Aircraft;
import org.glassfish.jersey.examples.flight.model.AircraftType;
import org.glassfish.jersey.examples.flight.model.Flight;
import org.glassfish.jersey.examples.flight.model.Location;
import org.glassfish.jersey.examples.flight.validation.ValidAircraftId;
import org.glassfish.jersey.server.mvc.ErrorTemplate;
import org.glassfish.jersey.server.mvc.Template;

/**
 * JAX-RS resource for accessing & manipulating flight information.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path("aircrafts")
@Produces({APPLICATION_XML, APPLICATION_JSON})
public class AircraftsResource {
    @GET
    public Collection<Aircraft> list() {
        return DataStore.selectAllAircrafts();
    }

    @GET
    @Produces(TEXT_HTML)
    @Template(name = "/aircraft/list")
    public Collection<Aircraft> listAsHtml() {
        return list();
    }

    @GET
    @Produces("text/plain;qs=0.5")
    public String listAsString() {
        StringBuilder sb = new StringBuilder();
        for (Aircraft aircraft : list()) {
            sb.append(aircraft).append('\n');
        }
        return sb.toString();
    }

    @GET
    @Path("{id}")
    @Detail
    public Aircraft get(@ValidAircraftId @PathParam("id") Integer aircraftId) {
        return DataStore.selectAircraft(aircraftId);
    }

    @GET
    @Path("{id}")
    @Produces(TEXT_HTML)
    @Template(name = "/aircraft/detail")
    @ErrorTemplate(name = "/errors/404")
    public Aircraft getAsHtml(@ValidAircraftId @PathParam("id") Integer id) {
        return get(id);
    }

    @DELETE
    @Path("{id}")
    @Produces(TEXT_PLAIN)
    @RolesAllowed("admin")
    public String delete(@ValidAircraftId @PathParam("id") Integer id) {
        Flight flight = DataStore.selectFlightByAircraft(id);
        if (flight != null) {
            throw new BadRequestException("Aircraft assigned to a flight.");
        }

        Aircraft aircraft = DataStore.removeAircraft(id);
        return String.format("%03d", aircraft.getId());
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @RolesAllowed("admin")
    @Detail
    public Aircraft create(
            @FormParam("manufacturer") String manufacturer,
            @FormParam("type") String type,
            @FormParam("capacity") Integer capacity,
            @DefaultValue("0") @FormParam("x-pos") Integer x,
            @DefaultValue("0") @FormParam("y-pos") Integer y) {
        if (manufacturer == null || type == null || capacity == null) {
            throw new BadRequestException("Incomplete data.");
        }

        Aircraft aircraft = new Aircraft();
        aircraft.setType(new AircraftType(manufacturer, type, capacity));
        aircraft.setLocation(SimEngine.bound(new Location(x, y)));

        if (!DataStore.addAircraft(aircraft)) {
            throw new InternalServerErrorException("Unable to add new aircraft.");
        }

        return aircraft;
    }

    @GET
    @Path("available")
    public Collection<Aircraft> listAvailable() {
        return DataStore.selectAvailableAircrafts();
    }

    @GET
    @Path("available")
    @Produces("text/plain;qs=0.5")
    public String listAvailableAsString() {
        StringBuilder sb = new StringBuilder();
        for (Aircraft aircraft : listAvailable()) {
            sb.append(aircraft).append('\n');
        }
        return sb.toString();
    }
}
