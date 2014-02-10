/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.entityfiltering.selectable;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.examples.entityfiltering.selectable.domain.Person;
import org.glassfish.jersey.examples.entityfiltering.selectable.resource.PersonResource;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link PersonResource} unit tests.
 * 
 * @author Andy Pemberton (pembertona at gmail.com)
 */
public class PersonResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new SelectableEntityFilteringApplication();
    }

    @Test
    public void testNoFilter() throws Exception {
        final Person entity = target("people").path("1234").request()
                .get(Person.class);

        // Not null values.
        assertThat(entity.getFamilyName(), notNullValue());
        assertThat(entity.getGivenName(), notNullValue());
        assertThat(entity.getAddresses(), notNullValue());
        assertThat(entity.getPhoneNumbers(), notNullValue());

    }

    /**
     * Test first level filters
     * 
     * @throws Exception
     */
    @Test
    public void testFilters() throws Exception {
        final Person entity = target("people").path("1234")
                .queryParam("select", "familyName,givenName").request()
                .get(Person.class);

        // Not null values.
        assertThat(entity.getFamilyName(), notNullValue());
        assertThat(entity.getGivenName(), notNullValue());

        // Null values.
        assertThat(entity.getAddresses(), nullValue());
        assertThat(entity.getPhoneNumbers(), nullValue());
        assertThat(entity.getRegion(), nullValue());
    }

    /**
     * Test 2nd and 3rd level filters
     * 
     * @throws Exception
     */
    @Test
    public void testSubFilters() throws Exception {
        final Person entity = target("people")
                .path("1234")
                .queryParam("select",
                        "familyName,givenName,addresses.streetAddress,addresses.phoneNumber.areaCode")
                .request().get(Person.class);

        // Not null values.
        assertThat(entity.getFamilyName(), notNullValue());
        assertThat(entity.getGivenName(), notNullValue());
        assertThat(entity.getAddresses().get(0).getStreetAddress(),
                notNullValue());
        assertThat(entity.getAddresses().get(0).getPhoneNumber().getAreaCode(),
                notNullValue());

        // Null values.
        assertThat(entity.getRegion(), nullValue());
        assertThat(entity.getAddresses().get(0).getPhoneNumber().getNumber(),
                nullValue());
    }

    /**
     * Test that 1st and 2nd level filters with the same name act as expected
     * 
     * @throws Exception
     */
    @Test
    public void testFiltersSameName() throws Exception {
        final Person firstLevel = target("people").path("1234")
                .queryParam("select", "familyName,region").request()
                .get(Person.class);
        final Person secondLevel = target("people").path("1234")
                .queryParam("select", "familyName,addresses.region").request()
                .get(Person.class);

        // Not null values.
        assertThat(firstLevel.getRegion(), notNullValue());
        assertThat(secondLevel.getAddresses().get(0).getRegion(),
                notNullValue());

        // Null values.
        assertThat(firstLevel.getAddresses(), nullValue()); //confirms 2nd level region on addresses is null
        assertThat(secondLevel.getRegion(), nullValue());
    }

}
