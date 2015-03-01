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
package org.glassfish.jersey.examples.flight.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.examples.flight.filtering.Detail;

/**
 * Aircraft data model representation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Aircraft {

    private Integer id;
    private Status status = Status.AVAILABLE;
    private AircraftType type;
    @Detail
    private Location location;

    public static enum Status {
        AVAILABLE,
        ASSIGNED,
    }

    public synchronized Integer getId() {
        return id;
    }

    public synchronized void setId(Integer id) {
        this.id = id;
    }

    public synchronized AircraftType getType() {
        return type;
    }

    public synchronized void setType(AircraftType type) {
        this.type = type;
    }

    public synchronized int getCapacity() {
        return type.getCapacity();
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public synchronized boolean isAvailable() {
        return status == Status.AVAILABLE;
    }

    public synchronized void marAvailable() {
        status = Status.AVAILABLE;
    }

    public synchronized boolean marAssigned() {
        if (status == Status.AVAILABLE) {
            status = Status.ASSIGNED;
            return true;
        }
        return false;
    }

    public synchronized Location getLocation() {
        return location;
    }

    public synchronized void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public synchronized String toString() {
        return String.format("A[%03d] { %s, %9s, located at %s}",
                id, type, status, location);
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Aircraft aircraft = (Aircraft) o;

        if (id != null ? !id.equals(aircraft.id) : aircraft.id != null) {
            return false;
        }
        if (location != null ? !location.equals(aircraft.location) : aircraft.location != null) {
            return false;
        }
        if (status != aircraft.status) {
            return false;
        }
        if (type != null ? !type.equals(aircraft.type) : aircraft.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public synchronized int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + status.hashCode();
        return result;
    }
}
