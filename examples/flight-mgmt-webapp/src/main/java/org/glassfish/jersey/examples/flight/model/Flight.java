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
package org.glassfish.jersey.examples.flight.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Flight data model representation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Flight {

    private String id;
    private Status status = Status.OPEN;
    private int reservationsCount;
    private Aircraft aircraft;


    public static enum Status {
        OPEN, CLOSED
    }

    public Flight() {
    }

    public Flight(String id, Aircraft aircraft) {
        this.id = id;
        this.aircraft = aircraft;
    }

    public synchronized String getId() {
        return id;
    }

    public synchronized void setId(String id) {
        this.id = id;
    }

    public synchronized Aircraft getAircraft() {
        return aircraft;
    }

    public synchronized void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized boolean isOpen() {
        return status == Status.OPEN;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public synchronized void closeReservations() {
        status = Status.CLOSED;
    }

    public synchronized int getReservationsCount() {
        return reservationsCount;
    }

    public synchronized void setReservationsCount(int reservationsCount) {
        this.reservationsCount = reservationsCount;
    }

    public synchronized int nextReservationNumber() {
        if (reservationsCount < aircraft.getCapacity()) {
            return ++reservationsCount;
        }

        return -1;
    }

    public synchronized int getAvailableSeats() {
        return aircraft.getCapacity() - reservationsCount;
    }

    @Override
    public synchronized String toString() {
        return String.format("%s (%s) {%s, seats: %3d / %3d}",
                id,
                status,
                aircraft,
                getAvailableSeats(),
                aircraft.getCapacity());
    }
}
