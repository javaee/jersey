/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey2704;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.core.Application;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.tests.integration.jersey2704.services.HappyService;
import org.glassfish.jersey.tests.integration.jersey2704.services.SadService;
import org.junit.Assert;
import org.junit.Test;


/**
 * This test case is to cover enhancement implemented in JERSEY-2704. The goal of this enhancement
 * is to give users possibility to register main {@link ServiceLocator} in the servlet context, so
 * it can be later used by Jersey. This creates the opportunity to wire Jersey-specific classes with
 * the services created outside the Jersey context.
 *
 * @author Bartosz Firyn (bartoszfiryn at gmail.com)
 */
public class Jersey2704ITCase extends JerseyTest {

    @Override
    protected Application configure() {
        return new TestApplication();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    /**
     * Invokes REST endpoint to check whether specific class service is registered in the
     * {@link ServiceLocator}.
     *
     * @param service the service class
     * @return HTTP status code, 200 when service is available and 600 otherwise
     * @throws IOException in case of problems with HTTP communication
     */
    private int test(Class<?> service) throws IOException {

        String name = service.getCanonicalName();
        String path = getBaseUri().toString() + "test/" + name;

        HttpURLConnection connection = (HttpURLConnection) new URL(path).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        connection.disconnect();

        return connection.getResponseCode();
    }

    /**
     * Test to cover sunny day scenario, i.e. specific service has been registered in the parent
     * {@link ServiceLocator} so it will be available in the one that is used in Jersey context.
     *
     * @throws IOException
     */
    @Test
    public void testCorrectInjection() throws IOException {
        Assert.assertEquals(200, test(HappyService.class));
    }

    /**
     * Test to cover rainy day scenario, i.e. specific service has <b>not</b> been registered in the
     * parent {@link ServiceLocator} so it cannot be used to wire Jersey classes.
     *
     * @throws IOException
     */
    @Test
    public void testMisingInjection() throws IOException {
        Assert.assertEquals(600, test(SadService.class));
    }
}
