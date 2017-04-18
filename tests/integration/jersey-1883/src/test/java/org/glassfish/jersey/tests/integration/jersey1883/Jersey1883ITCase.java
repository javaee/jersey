/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey1883;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class Jersey1883ITCase extends JerseyTest {

    @Before
    public void setup() {
        Assume.assumeTrue(Hk2InjectionManagerFactory.isImmediateStrategy());
    }

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testGetRestNoLife() throws Exception {
        Response response = target("rest1").path("no-life").request().get();
        assertThat(response.readEntity(String.class), equalTo("ciao #1"));

        response = target("rest1").path("no-life").request().get();
        assertThat(response.readEntity(String.class), equalTo("ciao #1"));

        response = target("rest1").path("no-life").request().get();
        assertThat(response.readEntity(String.class), equalTo("ciao #1"));

        response = target("rest1").path("no-life").request().get();
        assertThat(response.readEntity(String.class), equalTo("ciao #1"));
    }

    @Test
    public void testGetRestSingletonLife() throws Exception {
        Response response = target("rest2").path("singleton-life").request().get();
        assertThat(response.readEntity(String.class), equalTo("hello #1"));

        response = target("rest2").path("singleton-life").request().get();
        assertThat(response.readEntity(String.class), equalTo("hello #1"));

        response = target("rest2").path("singleton-life").request().get();
        assertThat(response.readEntity(String.class), equalTo("hello #1"));

        response = target("rest2").path("singleton-life").request().get();
        assertThat(response.readEntity(String.class), equalTo("hello #1"));
    }

    @Test
    public void testGetRestLife() throws Exception {
        Response response = target("rest3").path("life").request().get();
        assertThat(response.readEntity(String.class), equalTo("hi #2"));

        response = target("rest3").path("life").request().get();
        assertThat(response.readEntity(String.class), equalTo("hi #3"));

        response = target("rest3").path("life").request().get();
        assertThat(response.readEntity(String.class), equalTo("hi #4"));

        response = target("rest3").path("life").request().get();
        assertThat(response.readEntity(String.class), equalTo("hi #5"));
    }

}
