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

package org.glassfish.jersey.examples.jettison;

import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JsonJettisonTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);

        return App.createApp();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new JettisonFeature()).register(JaxbContextResolver.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // reset static flights list
        target().path("flights/init").request("application/json").post(null);
    }

    /**
     * Test checks that the application.wadl is reachable.
     * <p/>
     */
    @Test
    public void testApplicationWadl() {
        String applicationWadl = target().path("application.wadl").request().get(String.class);
        assertTrue("Something wrong. Returned wadl length is not > 0", applicationWadl.length() > 0);
    }

    /**
     * Test check GET on the "flights" resource in "application/json" format.
     */
    @Test
    public void testGetOnFlightsJSONFormat() {
        // get the initial representation
        Flights flights = target().path("flights").request("application/json").get(Flights.class);
        // check that there are two flight entries
        assertEquals("Expected number of initial entries not found", 2, flights.getFlight().size());
    }

    /**
     * Test checks PUT on the "flights" resource in "application/json" format.
     */
    @Test
    public void testPutOnFlightsJSONFormat() {
        // get the initial representation
        Flights flights = target().path("flights")
                .request("application/json").get(Flights.class);
        // check that there are two flight entries
        assertEquals("Expected number of initial entries not found", 2, flights.getFlight().size());

        // remove the second flight entry
        if (flights.getFlight().size() > 1) {
            flights.getFlight().remove(1);
        }

        // update the first entry
        flights.getFlight().get(0).setNumber(125);
        flights.getFlight().get(0).setFlightId("OK125");

        // and send the updated list back to the server
        target().path("flights").request().put(Entity.json(flights));

        // get the updated list out from the server:
        Flights updatedFlights = target().path("flights").request("application/json").get(Flights.class);
        //check that there is only one flight entry
        assertEquals("Remaining number of flight entries do not match the expected value", 1, updatedFlights.getFlight().size());
        // check that the flight entry in retrieved list has FlightID OK!@%
        assertEquals("Retrieved flight ID doesn't match the expected value", "OK125",
                updatedFlights.getFlight().get(0).getFlightId());
    }

    /**
     * Test checks GET on "flights" resource with mime-type "application/xml".
     */
    @Test
    public void testGetOnFlightsXMLFormat() {
        // get the initial representation
        Flights flights = target().path("flights").request("application/xml").get(Flights.class);
        // check that there are two flight entries
        assertEquals("Expected number of initial entries not found", 2, flights.getFlight().size());
    }

    /**
     * Test checks PUT on "flights" resource with mime-type "application/xml".
     */
    @Test
    public void testPutOnFlightsXMLFormat() {
        // get the initial representation
        Flights flights = target().path("flights").request("application/XML").get(Flights.class);
        // check that there are two flight entries
        assertEquals("Expected number of initial entries not found", 2, flights.getFlight().size());

        // remove the second flight entry
        if (flights.getFlight().size() > 1) {
            flights.getFlight().remove(1);
        }

        // update the first entry
        flights.getFlight().get(0).setNumber(125);
        flights.getFlight().get(0).setFlightId("OK125");

        // and send the updated list back to the server
        target().path("flights").request().put(Entity.xml(flights));

        // get the updated list out from the server:
        Flights updatedFlights = target().path("flights").request("application/XML").get(Flights.class);
        //check that there is only one flight entry
        assertEquals("Remaining number of flight entries do not match the expected value", 1, updatedFlights.getFlight().size());
        // check that the flight entry in retrieved list has FlightID OK!@%
        assertEquals("Retrieved flight ID doesn't match the expected value", "OK125",
                updatedFlights.getFlight().get(0).getFlightId());
    }

    /**
     * Test check GET on the "aircrafts" resource in "application/json" format.
     */
    @Test
    public void testGetOnAircraftsJSONFormat() {
        GenericType<List<AircraftType>> listOfAircrafts = new GenericType<List<AircraftType>>() {
        };
        // get the initial representation
        List<AircraftType> aircraftTypes = target().path("aircrafts").request("application/json").get(listOfAircrafts);
        // check that there are two aircraft type entries
        assertEquals("Expected number of initial aircraft types not found", 2, aircraftTypes.size());
    }
}
