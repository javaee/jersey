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

import org.glassfish.jersey.examples.flight.filtering.Detail;

/**
 * Aircraft type.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AircraftType {

    private String manufacturer;
    private String model;
    @Detail
    private int capacity;

    public AircraftType() {
    }

    public AircraftType(String manufacturer, String model, int capacity) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.capacity = capacity;
    }

    public synchronized String getManufacturer() {
        return manufacturer;
    }

    public synchronized void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public synchronized String getModel() {
        return model;
    }

    public synchronized void setModel(String model) {
        this.model = model;
    }

    public synchronized int getCapacity() {
        return capacity;
    }

    public synchronized void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AircraftType at = (AircraftType) o;

        if (capacity != at.capacity) {
            return false;
        }
        if (manufacturer != null ? !manufacturer.equals(at.manufacturer)
                : at.manufacturer != null) {
            return false;
        }

        return !(model != null ? !model.equals(at.model) : at.model != null);
    }

    @Override
    public synchronized int hashCode() {
        int result = manufacturer != null ? manufacturer.hashCode() : 0;
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + capacity;
        return result;
    }

    @Override
    public synchronized String toString() {
        return String.format("%6s %-9s [%3d]", manufacturer, model, capacity);
    }
}
