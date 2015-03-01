/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.flight.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.jersey.examples.flight.model.Aircraft;
import org.glassfish.jersey.examples.flight.model.AircraftType;
import org.glassfish.jersey.examples.flight.model.Flight;
import org.glassfish.jersey.examples.flight.model.Location;

/**
 * Data store for the runtime object model of the application.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class DataStore {

    public static final int CONCURRENCY_LEVEL =
            ceilingNextPowerOfTwo(Runtime.getRuntime().availableProcessors());

    private static int ceilingNextPowerOfTwo(int x) {
        // Hacker's Delight, Chapter 3, Harry S. Warren Jr.
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(x - 1));
    }

    private static final ConcurrentMap<String, Flight> flights =
            new ConcurrentHashMap<>(16, 0.75f, CONCURRENCY_LEVEL);
    private static final Comparator<Flight> FLIGHT_COMPARATOR = new Comparator<Flight>() {
        @Override
        public int compare(Flight f1, Flight f2) {
            return f1.getId().compareTo(f2.getId());
        }
    };

    private static final ConcurrentMap<Integer, Aircraft> aircrafts =
            new ConcurrentHashMap<>(16, 0.75f, CONCURRENCY_LEVEL);
    private static final AtomicInteger nextAircraftId = new AtomicInteger(1);
    private static final Comparator<Aircraft> AIRCRAFT_COMPARATOR = new Comparator<Aircraft>() {
        @Override
        public int compare(Aircraft a1, Aircraft a2) {
            return a1.getId().compareTo(a2.getId());
        }
    };

    public static List<Flight> selectAllFlights() {
        final List<Flight> result = new ArrayList<>(DataStore.flights.values());
        Collections.sort(result, FLIGHT_COMPARATOR);
        return result;
    }

    public static List<Flight> selectOpenFlights() {
        final List<Flight> result = new ArrayList<>(DataStore.flights.values());
        final Iterator<Flight> it = result.iterator();
        while (it.hasNext()) {
            final Flight flight = it.next();
            if (!flight.isOpen()) {
                it.remove();
            }
        }
        Collections.sort(result, FLIGHT_COMPARATOR);
        return result;
    }

    public static Flight selectFlight(String id) {
        return (id == null) ? null : flights.get(id);
    }

    public static Flight selectFlightByAircraft(Integer id) {
        if (id == null) {
            return null;
        }

        final Aircraft aircraft = selectAircraft(id);
        if (aircraft == null || aircraft.isAvailable()) {
            return null;
        }

        for (Flight flight : DataStore.flights.values()) {
            if (flight.getAircraft().getId().equals(id)) {
                return flight;
            }
        }

        return null;
    }

    public static boolean addFlight(Flight flight) {
        if (flight.getId() != null) {
            return flights.putIfAbsent(flight.getId(), flight) == null;
        }

        do {
            flight.setId(String.format("FL-%04d", rnd.nextInt(10000)));
        } while (flights.putIfAbsent(flight.getId(), flight) != null);

        return true;
    }

    public static Flight removeFlight(String id) {
        return (id == null) ? null : flights.remove(id);
    }

    public static List<Aircraft> selectAllAircrafts() {
        final List<Aircraft> result = new ArrayList<>(aircrafts.values());
        Collections.sort(result, AIRCRAFT_COMPARATOR);
        return result;
    }

    public static List<Aircraft> selectAvailableAircrafts() {
        final ArrayList<Aircraft> result = new ArrayList<>(aircrafts.values());
        final Iterator<Aircraft> it = result.iterator();
        while (it.hasNext()) {
            Aircraft a = it.next();
            if (!a.isAvailable()) {
                it.remove();
            }
        }
        Collections.sort(result, AIRCRAFT_COMPARATOR);
        return result;
    }

    public static Aircraft selectAircraft(Integer id) {
        return (id == null) ? null : aircrafts.get(id);
    }

    public static boolean addAircraft(Aircraft aircraft) {
        final int id = nextAircraftId.getAndIncrement();
        boolean result = aircrafts.putIfAbsent(
                id, aircraft) == null;

        if (result) {
            aircraft.setId(id);
        }

        return result;
    }

    public static Aircraft removeAircraft(Integer id) {
        return (id == null) ? null : aircrafts.remove(id);
    }

    /*
     * Data generation fields and methods.
     */

    private static final Random rnd = new Random();

    private static Flight generateFlight() {
        final Flight flight = new Flight();
        flight.setId(String.format("FL-%04d", rnd.nextInt(10000)));

        return flight;
    }

    private static Aircraft generateAircraft() {
        final Aircraft aircraft = new Aircraft();

        final Iterator<AircraftType> iterator = aircraftTypes.iterator();
        int i = rnd.nextInt(aircraftTypes.size());
        AircraftType type = null;
        while (iterator.hasNext()) {
            type = iterator.next();
            if (--i < 0) {
                break;
            }
        }

        aircraft.setType(type);
        aircraft.setLocation(generateLocation(SimEngine.X_BOUND, SimEngine.Y_BOUND));

        return aircraft;
    }

    public static Location generateLocation(int xBound, int yBound) {
        return new Location(
                rnd.nextInt(xBound), rnd.nextInt(yBound));
    }

    public static final int MAX_GEN_AIRCRAFTS = 20;
    public static final int MAX_GEN_FLIGHTS = 10;

    public static void generateData() {
        flights.clear();
        aircrafts.clear();

        LinkedList<Aircraft> planes = new LinkedList<>();
        while (planes.size() < MAX_GEN_AIRCRAFTS) {
            Aircraft a = generateAircraft();
            if (addAircraft(a)) {
                planes.add(a);
            }
        }

        int count = 0;
        while (count < MAX_GEN_FLIGHTS) {
            final Flight flight = generateFlight();
            if (addFlight(flight)) {
                count++;
                final Aircraft aircraft = planes.remove(rnd.nextInt(planes.size()));
                aircraft.marAssigned();
                flight.setAircraft(aircraft);
            }
        }
    }

    private static final Set<AircraftType> aircraftTypes = initAircraftTypes();

    private static Set<AircraftType> initAircraftTypes() {
        Set<AircraftType> ats = new LinkedHashSet<>();

        // Airbus

        // Short-range
        ats.add(new AircraftType("Airbus", "A318-100", 107));
        ats.add(new AircraftType("Airbus", "A319-100", 124));
        ats.add(new AircraftType("Airbus", "A320-200", 150));
        ats.add(new AircraftType("Airbus", "A321-200", 185));

        ats.add(new AircraftType("Airbus", "A330-200", 253));
        ats.add(new AircraftType("Airbus", "A330-300", 295));
        ats.add(new AircraftType("Airbus", "A330-500", 222));
        ats.add(new AircraftType("Airbus", "A330-500", 222));

        // Medium-range
        ats.add(new AircraftType("Airbus", "A340-200", 240));
        ats.add(new AircraftType("Airbus", "A340-300", 295));
        ats.add(new AircraftType("Airbus", "A340-500", 313));
        ats.add(new AircraftType("Airbus", "A340-600", 380));

        // Long-range
        ats.add(new AircraftType("Airbus", "A350-800", 270));
        ats.add(new AircraftType("Airbus", "A350-900", 314));
        ats.add(new AircraftType("Airbus", "A350-1000", 350));

        ats.add(new AircraftType("Airbus", "A380-800", 525));

        // Boeing

        // Short-range
        ats.add(new AircraftType("Boeing", "737-200", 97));
        ats.add(new AircraftType("Boeing", "737-500", 146));
        ats.add(new AircraftType("Boeing", "737-900", 177));

        // Medium-range
        ats.add(new AircraftType("Boeing", "767-200", 181));
        ats.add(new AircraftType("Boeing", "767-300", 218));
        ats.add(new AircraftType("Boeing", "767-400", 245));

        // Long-range
        ats.add(new AircraftType("Boeing", "777-200", 314));
        ats.add(new AircraftType("Boeing", "777-300", 386));

        ats.add(new AircraftType("Boeing", "787-8", 242));
        ats.add(new AircraftType("Boeing", "787-9", 280));
        ats.add(new AircraftType("Boeing", "787-10", 323));

        return Collections.unmodifiableSet(ats);
    }
}
