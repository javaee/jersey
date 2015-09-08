/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey2892;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import javax.xml.bind.annotation.XmlTransient;

/**
 * A resource that provides a means to test whether repeating classes in object graph are correctly filtered out.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {

    @GET
    @Path("pointer")
    public Pointer pointer() {
        return new Pointer();
    }

    public static class Pointer {

        public final Persons persons = new Persons();
    }

    @GET
    @Path("test")
    public Persons issue() {
        return new Persons();
    }

    public static class Persons {

        public final Person first = new Person("Larry", "Amphitheatre Pkwy", 1600, "Mountain View");
        public final Person second = new Person("Bill", "Microsoft Way", 1, "Redmond");
    }

    public static class Person {

        public Person() {
        }

        public Person(final String name, final String streetName, final int streetNumber, final String city) {
            this.name = name;
            address = new Address(streetName, streetNumber, city);
        }

        public Address address;
        public String name;
    }

    public static class Address {

        public Address() {
        }

        public Address(final String name, final int number, final String city) {
            this.city = city;
            street = new Street(name, number);
        }

        public Street street;
        public String city;
    }

    public static class Street {

        public Street() {
        }

        public Street(final String name, final int number) {
            this.name = name;
            this.number = number;
        }

        public String name;
        public int number;
    }

    @GET
    @Path("recursive")
    public Recursive recursive() {
        return new Recursive();
    }

    public static class Recursive {

        public String idRecursive = "a";
        public SubField subField = new SubField();
    }

    public static class SubField {

        public final String idSubField = "b";
        public final SubSubField subSubField;

        public SubField() {
            subSubField = new SubSubField(this);
        }
    }

    public static class SubSubField {

        public final String idSubSubField = "c";

        @XmlTransient
        public SubField subField;

        public SubSubField() {
        }

        public SubSubField(final SubField subField) {
            this.subField = subField;
        }
    }
}
