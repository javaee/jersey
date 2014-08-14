/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.examples.flight.model.Flight;
import org.glassfish.jersey.examples.flight.model.FlightLocation;
import org.glassfish.jersey.examples.flight.model.Location;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;

/**
 * Flight simulation engine.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class SimEngine {
    /**
     * X-axis coordinate boundary.
     */
    public static final int X_BOUND = 800;
    /**
     * Y-axis coordinate boundary.
     */
    public static final int Y_BOUND = 350;
    /**
     * Simulation step executor.
     */
    private static final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();
    /**
     * SSE broadcaster.
     */
    private static final SseBroadcaster broadcaster = new SseBroadcaster();
    /**
     * Simulation engine status.
     */
    private static final AtomicBoolean started = new AtomicBoolean(false);

    public static boolean register(final EventOutput output) {
        return broadcaster.add(output);
    }

    public static boolean start() {
        if (!started.compareAndSet(false, true)) {
            return false;
        }

        executor.submit(new SimRunner());

        return true;
    }

    public static boolean stop() {
        return started.compareAndSet(true, false);
    }

    public static Location bound(final Location location) {
        int x = location.getX();
        int y = location.getY();
        if (x >= X_BOUND) {
            x = x % X_BOUND;
        } else if (x < 0) {
            x = X_BOUND + (x % X_BOUND);
        }
        if (y >= Y_BOUND) {
            y = y % Y_BOUND;
        } else if (y < 0) {
            y = Y_BOUND + (y % Y_BOUND);
        }
        return (x != location.getX() || y != location.getY())
                ? new Location(x, y) : location;
    }

    private static class SimRunner implements Runnable {
        private final List<Flight> flights;
        private final List<Location> vectors;

        private SimRunner() {
            flights = DataStore.selectAllFlights();

            vectors = new ArrayList<Location>(flights.size());
            final int boundSpeedX = X_BOUND / 30;
            final int boundSpeedY = Y_BOUND / 30;
            final int count = 0;
            for (final Flight flight : flights) {
                flight.setStatus(Flight.Status.CLOSED);
                final Location vector = DataStore.generateLocation(boundSpeedX, boundSpeedY);
                switch (count / 4) {
                    case 0:
                        vector.setX(-vector.getX());
                        break;
                    case 1:
                        vector.setY(-vector.getY());
                        break;
                    case 2:
                        vector.setX(-vector.getX());
                        vector.setY(-vector.getY());
                        break;
                    case 3:
                        // no change
                        break;
                }
                vectors.add(vector);
            }
        }

        @Override
        public void run() {
            final Thread currentThread = Thread.currentThread();
            for (int i = 0; i < flights.size(); i++) {
                if (!started.get() || currentThread.isInterrupted()) {
                    cleanup();
                    return;
                }

                final Flight flight = flights.get(i);
                final Location vector = vectors.get(i);
                Location newLocation = new Location(
                        flight.getAircraft().getLocation().getX() + vector.getX(),
                        flight.getAircraft().getLocation().getY() + vector.getY()
                );
                newLocation = bound(newLocation);
                flight.getAircraft().setLocation(newLocation);

                final OutboundEvent flightMovedEvent = new OutboundEvent.Builder()
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(new FlightLocation(flight.getId(), newLocation))
                        .build();

                broadcaster.broadcast(flightMovedEvent);
            }

            if (started.get() && !currentThread.isInterrupted()) {
                executor.schedule(this, 500, TimeUnit.MILLISECONDS); // re-schedule
            } else {
                cleanup();
            }
        }

        private void cleanup() {
            for (final Flight flight : flights) {
                flight.setStatus(Flight.Status.OPEN);
            }
        }
    }
}
