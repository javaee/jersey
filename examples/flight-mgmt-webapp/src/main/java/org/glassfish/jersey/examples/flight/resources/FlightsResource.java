/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.flight.resources;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
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
import org.glassfish.jersey.examples.flight.model.Aircraft;
import org.glassfish.jersey.examples.flight.model.Flight;
import org.glassfish.jersey.examples.flight.validation.ValidAircraftId;
import org.glassfish.jersey.examples.flight.validation.ValidFlightId;
import org.glassfish.jersey.server.mvc.ErrorTemplate;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 * JAX-RS resource for accessing & manipulating flight information.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path("flights")
@Produces({APPLICATION_XML, APPLICATION_JSON})
public class FlightsResource {
    @GET
    public Collection<Flight> list() {
        return DataStore.selectAllFlights();
    }

    @GET
    @Produces(TEXT_HTML)
    @Template(name = "/flight/list")
    public Collection<Flight> listAsHtml() {
        return list();
    }


    @GET
    @Produces("text/csv")
    public Viewable listAsCsv() {
        return new Viewable("/flight/list-csv", list());
    }


    @GET
    @Produces("application/x-yaml")
    @Template(name = "/flight/list-yaml")
    public Collection<Flight> listAsYaml() {
        return list();
    }

    @GET
    @Produces("text/plain;qs=0.5")
    public String listAsString() {
        StringBuilder sb = new StringBuilder();
        for (Flight flight : list()) {
            sb.append(flight).append('\n');
        }
        return sb.toString();
    }

    @GET
    @Path("{id}")
    @Detail
    public Flight get(@ValidFlightId @PathParam("id") String flightId) {
        return DataStore.selectFlight(flightId);
    }

    @GET
    @Path("{id}")
    @Produces(TEXT_HTML)
    @Template(name = "/flight/detail")
    @ErrorTemplate(name = "/errors/404")
    public Flight getAsHtml(@ValidFlightId @PathParam("id") String flightId) {
        return get(flightId);
    }

    @POST
    @Path("{id}/new-booking")
    @Produces(TEXT_PLAIN)
    public String book(@ValidFlightId @PathParam("id") String flightId) {
        final Flight flight = DataStore.selectFlight(flightId);

        if (!flight.isOpen()) {
            throw new BadRequestException("Flight closed.");
        }

        String ridString = "FAILED";
        final int nextRid = flight.nextReservationNumber();
        if (nextRid > 0) {
            ridString = String.format("%s-%03d", flight.getId(), nextRid);
        }
        return ridString;
    }

    @POST
    @Path("{id}/new-booking")
    @Produces(TEXT_HTML)
    public Viewable bookAsHtml(@PathParam("id") String flightId) {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("flightId", flightId);
        model.put("reservationId", book(flightId));
        return new Viewable("/flight/reservation", model);
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @RolesAllowed("admin")
    @Detail
    public Flight create(@ValidAircraftId @FormParam("aircraftId") Integer aircraftId) {
        final Aircraft aircraft = DataStore.selectAircraft(aircraftId);

        if (!aircraft.marAssigned()) {
            throw new BadRequestException("Aircraft already assigned.");
        }

        Flight flight = new Flight(null, aircraft);
        if (!DataStore.addFlight(flight)) {
            aircraft.marAvailable();
            throw new BadRequestException("Flight already exists.");
        }

        return flight;
    }

    @DELETE
    @Path("{id}")
    @Produces(TEXT_PLAIN)
    @RolesAllowed("admin")
    public String delete(@ValidFlightId @PathParam("id") String flightId) {
        Flight flight = DataStore.removeFlight(flightId);
        flight.getAircraft().marAvailable();
        return flight.getId();
    }

    @POST
    @Path("{id}/status")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    @RolesAllowed("admin")
    public String updateStatus(
            @ValidFlightId @PathParam("id") String flightId,
            @FormParam("status") String newStatus) {

        Flight.Status status;
        try {
            status = Flight.Status.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown status.");
        }

        final Flight flight = DataStore.selectFlight(flightId);

        flight.setStatus(status);

        return status.name();
    }

    @GET
    @Path("open")
    public Collection<Flight> listOpen() {
        return DataStore.selectOpenFlights();
    }

    @GET
    @Produces("text/plain;qs=0.5")
    @Path("open")
    public String listOpenAsString() {
        StringBuilder sb = new StringBuilder();
        for (Flight flight : listOpen()) {
            sb.append(flight).append('\n');
        }
        return sb.toString();
    }
}
