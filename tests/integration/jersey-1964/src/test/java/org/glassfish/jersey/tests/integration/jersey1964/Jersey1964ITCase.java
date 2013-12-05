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
package org.glassfish.jersey.tests.integration.jersey1964;

import java.net.ConnectException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Reproducer tests for JERSEY-1964.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class Jersey1964ITCase extends JerseyTest {

    @Override
    protected Application configure() {
        return new Jersey1964();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testJackson2JsonPut() throws Exception {
        final Response response = target().request().put(Entity.json(new Issue1964Resource.JsonStringWrapper("foo")));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(Issue1964Resource.JsonStringWrapper.class).getValue(), equalTo("foo"));
    }

    @Test(expected = ConnectException.class)
    public void testJackson2JsonGetInvalidEndpoint() throws Throwable {
        try {
            ClientBuilder.newClient()
                    .target("http://localhost:1234")
                    .request()
                    .get();

            fail("End-point shouldn't exist.");
        } catch (final ProcessingException pe) {
            throw pe.getCause();
        }
    }

    @Test(expected = ConnectException.class)
    public void testJackson2JsonPutInvalidEndpoint() throws Throwable {
        try {
            ClientBuilder.newClient()
                    .target("http://localhost:1234")
                    .request()
                    .put(Entity.json(new Issue1964Resource.JsonStringWrapper("foo")));

            fail("End-point shouldn't exist.");
        } catch (final ProcessingException pe) {
            throw pe.getCause();
        }
    }
}