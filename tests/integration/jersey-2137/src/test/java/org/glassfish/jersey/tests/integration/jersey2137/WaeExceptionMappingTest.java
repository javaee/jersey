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
package org.glassfish.jersey.tests.integration.jersey2137;

import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Reproducer for JERSEY-2137.
 * Ensure that generated {@link WebApplicationException} is propagated
 * via transactional CDI call and mapped to response according to JAX-RS spec.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class WaeExceptionMappingTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new TestApplication();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("jersey-2137").build();
    }

    /**
     * Test all {@javax.transaction.Transactional}
     * annotated CDI beans. The test scenario is as follows.
     * Set two accounts via the CDI bean that avoids rollback.
     * Should any rollback happen there, we would not be able
     * to store any data in JPA. Next try to make two transactions,
     * first of them should be finished without errors,
     * during the other one, a rollback is expected.
     * The rollback should avoid partial data to be written
     * to the first account.
     */
    @Test
    public void testTransactions() {

        final WebTarget cdiResource = target().path("cdi-transactional");
        final WebTarget cdiResourceNoRollback = target().path("cdi-transactional-no-rollback");

        Response response;
        String responseBody;

        // account 12 -> insert 1000:
        response = cdiResourceNoRollback.path("12").request().put(Entity.text("1000"));
        assertThat(response.getStatus(), equalTo(200));

        // account 13 -> insert 1000:
        response = cdiResourceNoRollback.path("13").request().put(Entity.text("1000"));
        assertThat(response.getStatus(), equalTo(200));

        // transfer 1000 from 13 to 12:
        response = cdiResource.queryParam("from", "13").queryParam("to", "12").request().post(Entity.text("1000"));
        assertThat(response.getStatus(), equalTo(200));

        // ensure 12 has balance 2000:
        response = cdiResource.path("12").request().get();
        assertThat(response.getStatus(), equalTo(200));
        responseBody = response.readEntity(String.class);
        assertThat(responseBody, equalTo("2000"));

        // try to transfer 1000 from non-existing 8 to 12, this time the transaction should fail:
        response = cdiResource.queryParam("from", "8").queryParam("to", "12").request().post(Entity.text("1000"));
        assertThat(response.getStatus(), equalTo(400));

        // ensure 12 balance has not changed:
        response = cdiResource.path("12").request().get();
        assertThat(response.getStatus(), equalTo(200));
        responseBody = response.readEntity(String.class);
        assertThat(responseBody, equalTo("2000"));
    }
}
