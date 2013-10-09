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
package org.glassfish.jersey.examples.flight;

import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.examples.flight.internal.DataStore;
import org.glassfish.jersey.examples.flight.model.Aircraft;
import org.glassfish.jersey.examples.flight.model.Flight;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Flight Management Demo Application test suite.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class FlightsDemoAppTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new FlightDemoApp();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        // XML
        config.register(MoxyXmlFeature.class);
        config.property(MessageProperties.XML_FORMAT_OUTPUT, true);
        // JSON
        config.register(FlightDemoApp.createMoxyJsonResolver());
    }

    @BeforeClass
    public static void setup() {
        DataStore.generateData();
    }

    private void _testAllAircrafts(String acceptType) {
        final List<Aircraft> aircrafts = target("aircrafts")
                .request(acceptType)
                .get(new GenericType<List<Aircraft>>() {});

        for (Aircraft aircraft : aircrafts) {
            assertNotNull("Aircraft id", aircraft.getId());
            assertNotNull("Aircraft type", aircraft.getType());
        }
    }

    @Test
    public void testAllAircrafts() {
        _testAllAircrafts("application/json");
        _testAllAircrafts("application/xml");
    }

    public void _testAvailableAircrafts(String acceptType) {
        final List<Aircraft> aircrafts = target("aircrafts/available")
                .request(acceptType)
                .get(new GenericType<List<Aircraft>>() {});

        for (Aircraft aircraft : aircrafts) {
            assertNotNull("Aircraft id", aircraft.getId());
            assertNotNull("Aircraft type", aircraft.getType());
            assertTrue("Aircraft not available", aircraft.isAvailable());
        }
    }

    @Test
    public void testAvailableAircrafts() {
        _testAvailableAircrafts("application/json");
        _testAvailableAircrafts("application/xml");
    }

    private void _testAllFlights(String acceptType) {
        final List<Flight> flights = target("flights")
                .request(acceptType)
                .get(new GenericType<List<Flight>>() {});

        for (Flight flight : flights) {
            assertNotNull("Flight id", flight.getId());
            assertFalse("Flight id empty", flight.getId().isEmpty());
            assertFalse("Aircraft not assigned to flight", flight.getAircraft().isAvailable());
        }
    }

    @Test
    public void testAllFlights() {
        _testAllFlights("application/json");
        _testAllFlights("application/xml");
    }

    public void _testOpenFlights(String acceptType) {
        final List<Flight> flights = target("flights/open")
                .request(acceptType)
                .get(new GenericType<List<Flight>>() {});

        for (Flight flight : flights) {
            assertNotNull("Flight id", flight.getId());
            assertFalse("Flight id empty", flight.getId().isEmpty());
            assertFalse("Aircraft not assigned to flight", flight.getAircraft().isAvailable());
            assertTrue("Flight not open", flight.isOpen());
        }
    }

    @Test
    public void testOpenFlights() {
        _testOpenFlights("application/json");
        _testOpenFlights("application/xml");
    }

    public void _testCreateFlight(String acceptType) {
        final List<Aircraft> availableAircrafts = target("aircrafts/available")
                .request(acceptType)
                .get(new GenericType<List<Aircraft>>() {});

        final Aircraft aircraft = availableAircrafts.get(0);
        final Form flightForm = new Form("aircraftId", aircraft.getId().toString());
        Flight flight = target("flights").queryParam("user", "admin")
                .request(acceptType)
                .post(Entity.form(flightForm), Flight.class);

        assertNotNull("Flight", flight);
        assertNotNull("Flight.id", flight.getId());
        assertNotNull("Flight.aircraft", flight.getAircraft());
        assertEquals("Aircraft IDs do not match", aircraft.getId(), flight.getAircraft().getId());
        assertFalse("Aircraft not assigned", flight.getAircraft().isAvailable());
    }

    @Test
    public void testCreateFlight() {
        _testCreateFlight("application/json");
        _testCreateFlight("application/xml");
    }

    public void _testCreateAircraft(String acceptType) {
        Form form = new Form("manufacturer", "Cesna")
                .param("type", "680")
                .param("capacity", "9");

        Aircraft aircraft = target("aircrafts").queryParam("user", "admin")
                .request(acceptType)
                .post(Entity.form(form), Aircraft.class);
        assertNotNull("Aircraft", aircraft);
        assertNotNull("Aircraft id", aircraft.getId());
        assertNotNull("Aircraft type", aircraft.getType());
        assertNotNull("Aircraft location", aircraft.getLocation());
        assertEquals("Aircraft location x pos.", 0, aircraft.getLocation().getX());
        assertEquals("Aircraft location y pos.", 0, aircraft.getLocation().getY());
        assertTrue("Aircraft not available", aircraft.isAvailable());

        final List<Aircraft> availableAircrafts = target("aircrafts/available")
                .request(acceptType)
                .get(new GenericType<List<Aircraft>>() {});

        for (Aircraft a : availableAircrafts) {
            if (aircraft.getId().equals(a.getId())) {
                // passed
                return;
            }
        }
        fail("New aircraft not found in the list of available aircrafts.");
    }

    public void _testCreateAircraftWithLocation(String acceptType) {
        Form form = new Form("manufacturer", "Cesna")
                .param("type", "750")
                .param("capacity", "12")
                .param("x-pos", "100")
                .param("y-pos", "200");

        Aircraft aircraft = target("aircrafts").queryParam("user", "admin")
                .request(acceptType)
                .post(Entity.form(form), Aircraft.class);
        assertNotNull("Aircraft", aircraft);
        assertNotNull("Aircraft id", aircraft.getId());
        assertNotNull("Aircraft type", aircraft.getType());
        assertNotNull("Aircraft location", aircraft.getLocation());
        assertEquals("Aircraft location x pos.", 100, aircraft.getLocation().getX());
        assertEquals("Aircraft location y pos.", 200, aircraft.getLocation().getY());
        assertTrue("Aircraft not available", aircraft.isAvailable());

        final List<Aircraft> availableAircrafts = target("aircrafts/available")
                .request(acceptType)
                .get(new GenericType<List<Aircraft>>() {});

        for (Aircraft a : availableAircrafts) {
            if (aircraft.getId().equals(a.getId())) {
                // passed
                return;
            }
        }
        fail("New aircraft not found in the list of available aircrafts.");
    }

    @Test
    public void testCreateAircraft() {
        _testCreateAircraft("application/json");
        _testCreateAircraft("application/xml");

        _testCreateAircraftWithLocation("application/json");
        _testCreateAircraftWithLocation("application/xml");
    }
}
