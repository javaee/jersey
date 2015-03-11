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

package org.glassfish.jersey.tests.integration.jersey2637;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Reproducer tests for JERSEY-2637 - Query params cannot be injected using {@link javax.ws.rs.FormParam}.
 */
public class Jersey2637DisabledITCase extends JerseyTest {

    @Override
    protected Application configure() {
        return new Jersey2637();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testFormParams() throws Exception {
        final Form form = new Form()
                .param("username", "user")
                .param("password", "pass");

        final Response response = target("disabled").request().post(Entity.form(form));

        assertThat(response.readEntity(String.class), is("user_pass"));
    }

    @Test
    public void testQueryParams() throws Exception {
        final Response response = target("disabled")
                .queryParam("username", "user").queryParam("password", "pass")
                .request()
                .post(Entity.form(new Form()));

        assertThat(response.readEntity(String.class), is("ko_ko"));
    }

    @Test
    public void testDoubleQueryParams() throws Exception {
        final Response response = target("disabled")
                .queryParam("username", "user").queryParam("password", "pass")
                .queryParam("username", "user").queryParam("password", "pass")
                .request()
                .post(Entity.form(new Form()));

        assertThat(response.readEntity(String.class), is("ko_ko"));
    }

    @Test
    public void testEncodedQueryParams() throws Exception {
        final Response response = target("disabled")
                .queryParam("username", "us%20er").queryParam("password", "pass")
                .request()
                .post(Entity.form(new Form()));

        assertThat(response.readEntity(String.class), is("ko_ko"));
    }

    @Test
    public void testFormAndQueryParams() throws Exception {
        final Form form = new Form()
                .param("username", "user")
                .param("password", "pass");

        final Response response = target("disabled")
                .queryParam("username", "user").queryParam("password", "pass")
                .request()
                .post(Entity.form(form));

        assertThat(response.readEntity(String.class), is("user_pass"));
    }

    @Test
    public void testFormAndDoubleQueryParams() throws Exception {
        final Form form = new Form()
                .param("username", "user")
                .param("password", "pass");

        final Response response = target("disabled")
                .queryParam("username", "user").queryParam("password", "pass")
                .queryParam("username", "user").queryParam("password", "pass")
                .request()
                .post(Entity.form(form));

        assertThat(response.readEntity(String.class), is("user_pass"));
    }
}
