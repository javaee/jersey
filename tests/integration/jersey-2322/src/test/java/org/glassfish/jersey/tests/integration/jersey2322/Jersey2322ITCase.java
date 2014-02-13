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
package org.glassfish.jersey.tests.integration.jersey2322;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Reproducer tests for JERSEY-2322.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class Jersey2322ITCase extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new Jersey2322();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    /**
     * Server side response is wrapped, needs to be read to wrapper class.
     */
    @Test
    public void testJackson2JsonPut1() {
        final Response response = target("1").request().put(Entity.json(new Issue2322Resource.JsonString1("foo")));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(Wrapper1.class).getJsonString1().getValue(), equalTo("Hello foo"));
    }

    /**
     * Server side response is returned as orig class.
     */
    @Test
    public void testJackson2JsonPut2() {
        final Response response = target("2").request().put(Entity.json(new Issue2322Resource.JsonString2("foo")));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(Issue2322Resource.JsonString2.class).getValue(), equalTo("Hi foo"));
    }


    public static class Wrapper1 {
        @JsonProperty("JsonString1")
        Issue2322Resource.JsonString1 jsonString1;

        public Issue2322Resource.JsonString1 getJsonString1() {
            return jsonString1;
        }

        public void setJsonString1(Issue2322Resource.JsonString1 jsonString1) {
            this.jsonString1 = jsonString1;
        }
    }

}